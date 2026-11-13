package io.leloush.fang.wolfi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.system.Os
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * The one Linux rootfs Fang Harness runs on: Wolfi OS, unpacked into the app's private files and started with the
 * bundled PRoot. Nothing here needs Termux or another installed app.
 *
 * [install] is idempotent and safe to call from a setup screen or before a host launch. It downloads the pinned
 * Wolfi release only when the rootfs is missing, then installs its tools and Node.js once inside it.
 */
class WolfiRuntime(context: Context) {
  private val app = context.applicationContext
  private val baseDir = File(app.filesDir, "wolfi")
  private val rootfs = File(baseDir, "rootfs")
  private val hostDir = File(baseDir, "runtime")
  private val configDir = File(baseDir, "pi")
  private val hostLog = File(baseDir, "host.log")
  private val archive = File(baseDir, "rootfs.tar.gz")

  /** The host script and Pi's pinned runtime; inside the rootfs this is [hostDir]'s Android path. */
  val root: File get() = hostDir

  /** Pi's config, sessions and packages. */
  val piConfig: File get() = configDir

  /**
   * The files [command] binds over the `/proc` entries Android denies an app (`htop`'s `Cannot open /proc/stat`).
   * The host rewrites them while it runs; these are only what the bind needs to find, and what a process sees before
   * its host writes its own.
   */
  private val syntheticProc = File(hostDir, SYNTHETIC_PROC_DIR)

  data class Status(val installed: Boolean, val version: String?)

  fun status(): Status = Status(installed = rootfsReady() && toolsReady(), version = rootfsVersion())

  /**
   * Makes the runtime usable: Wolfi OS, Node.js and the directories the host needs. [onLine] receives short progress
   * lines for the setup screen; it may be called from a background thread.
   */
  @Synchronized
  fun install(onLine: (String) -> Unit = {}) {
    baseDir.mkdirs()
    if (!rootfsReady()) {
      val downloaded = download(onLine)
      onLine("Unpacking Wolfi OS…")
      val staging = File(baseDir, "rootfs.staging")
      staging.deleteRecursively()
      try {
        RootfsExtractor.install(downloaded, staging)
        if (!File(staging, "usr/bin/bash").exists()) throw IOException("The Wolfi archive is incomplete")
        rootfs.deleteRecursively()
        if (!staging.renameTo(rootfs)) throw IOException("Could not move the Wolfi rootfs into place")
      } finally {
        staging.deleteRecursively()
        downloaded.delete()
      }
    }
    prepareRootfs()
    ensureNode(onLine)
    hostDir.mkdirs()
    configDir.mkdirs()
  }

  /** Writes [host] to the app's runtime folder, replacing it when the bundled copy changed. */
  @Synchronized
  fun installHost(host: InputStream) {
    hostDir.mkdirs()
    val target = File(hostDir, "host.mjs")
    val bytes = host.use { it.readBytes() }
    if (target.exists() && target.readBytes().contentEquals(bytes)) return
    val temporary = File.createTempFile("host.mjs", ".tmp", hostDir).apply { writeBytes(bytes) }
    if (!temporary.renameTo(target)) {
      target.delete()
      if (!temporary.renameTo(target)) {
        temporary.delete()
        throw IOException("Could not write the Wolfi host")
      }
    }
  }

  /** The full PRoot command for a program inside the rootfs. */
  fun command(vararg command: String): List<String> {
    ensureMachineFiles()
    val libraryDir = app.applicationInfo.nativeLibraryDir
    val proot = File(libraryDir, "libproot.so")
    if (!proot.canExecute()) throw IOException("libproot.so is missing")
    val args = mutableListOf(proot.absolutePath, "--kill-on-exit", "-w", "/")
    for (path in SYSTEM_MOUNTS) {
      val file = File(path)
      if (file.exists()) args += listOf("-b", path)
    }
    for (path in sharedMounts()) {
      val file = File(path)
      if (file.exists()) args += listOf("-b", path)
    }
    // After /proc, whose entries these replace: PRoot gives the later binding the path.
    for ((name, file) in syntheticProcFiles()) args += listOf("-b", "${file.absolutePath}:/proc/$name")
    args += listOf("-b", app.filesDir.absolutePath)
    args += listOf("-b", "/dev/urandom:/dev/random")
    args += listOf("-r", rootfs.absolutePath, "-0", "--link2symlink", "--sysvipc", "-L")
    args.addAll(command)
    return args
  }

  private fun canBindSharedStorage(): Boolean {
    val api = Build.VERSION.SDK_INT
    return when {
      api == 29 -> false
      api >= 30 -> @Suppress("NewApi") Environment.isExternalStorageManager()
      api in 26..28 ->
        app.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
          app.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
      else -> false
    }
  }

  private fun sharedMounts(): List<String> = buildList {
    if (canBindSharedStorage()) {
      add(SHARED_STORAGE)
      add("/storage")
    }
    add("/data")
    add("/dev")
    add("/proc")
    add("/sys")
  }

  /**
   * Termux-style fake /proc files: generated via [FakeProcfs] and bound only when the real proc
   * entry is blocked/unreadable (bind_if_blocked). Fixes htop/top "Cannot open /proc/stat: Permission denied".
   */
  @Synchronized
  private fun syntheticProcFiles(): Map<String, File> {
    val sysdataDir = File(syntheticProc.absolutePath).apply { mkdirs() }
    FakeProcfs.writeSysdata(sysdataDir)
    val procFiles = listOf("stat", "vmstat", "meminfo", "uptime", "loadavg", "cpuinfo", "version")
    val binds = linkedMapOf<String, File>()
    for (name in procFiles) {
      val real = File("/proc", name)
      val fake = File(sysdataDir, name)
      val realReadable = try { real.canRead() && real.readBytes().isNotEmpty() } catch (_: Exception) { false }
      if (!realReadable && fake.exists() && fake.length() > 0) {
        if (FakeProcfs.isRegularFile(fake) &&
          (FakeProcfs.linkCount(fake) == 1 || FakeProcfs.linkCount(fake) == -1) &&
          fake.absolutePath.startsWith(sysdataDir.absolutePath)
        ) {
          binds[name] = fake
        }
      }
    }
    return binds
  }

  /**
   * The environment for a process in the rootfs; includes the host's [token] and paths when given.
   *
   * [FANG_HOME] is the Wolfi OS root: the folder the app opens a chat in, expands `~` with and shows as home. It is
   * the runtime's own filesystem, which is native storage, rather than Android's shared storage, which is a slow
   * FUSE mount; shared storage is still reachable, at [SHARED_STORAGE], and the folder browser offers it.
   */
  fun environment(token: String? = null): Map<String, String> {
    val libraryDir = app.applicationInfo.nativeLibraryDir
    return buildMap {
      put("HOME", "/root")
      put("SHELL", "/bin/bash")
      put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin")
      put("TERM", "xterm-256color")
      put("COLORTERM", "truecolor")
      put("LANG", "C.UTF-8")
      put("PREFIX", "/usr")
      put("EXTERNAL_STORAGE", SHARED_STORAGE)
      put("FANG_HOME", WOLFI_ROOT)
      put("TMPDIR", "/tmp")
      put("PROOT_TMP_DIR", File(baseDir, "proot-tmp").apply { mkdirs() }.absolutePath)
      put("PROOT_LOADER", File(libraryDir, "libloader.so").absolutePath)
      File(libraryDir, "libloader32.so").takeIf { it.exists() }?.let { put("PROOT_LOADER_32", it.absolutePath) }
      if (token != null) {
        put("FANG_TOKEN", token)
        put("FANG_ROOT", hostDir.absolutePath)
        put("PI_CODING_AGENT_DIR", configDir.absolutePath)
      }
    }
  }

  /** Starts a mode of the bundled host and returns its process; its output goes to [hostLog]. */
  fun startHost(mode: String, port: Int, token: String): Process {
    hostLog.delete()
    val builder = ProcessBuilder(command("/usr/bin/node", File(hostDir, "host.mjs").absolutePath, mode, port.toString()))
    builder.directory(baseDir)
    builder.redirectErrorStream(true)
    builder.redirectOutput(ProcessBuilder.Redirect.to(hostLog))
    builder.environment().clear()
    builder.environment().putAll(environment(token))
    return builder.start()
  }

  /** The last bytes the host wrote before it exited, for a failure message. */
  fun hostLogTail(): String {
    if (!hostLog.exists()) return ""
    val length = hostLog.length()
    val start = (length - 4096).coerceAtLeast(0)
    return runCatching {
      java.io.RandomAccessFile(hostLog, "r").use { file ->
        file.seek(start)
        val bytes = ByteArray((length - start).toInt())
        file.readFully(bytes)
        bytes.toString(Charsets.UTF_8).trim()
      }
    }.getOrDefault("")
  }

  /**
   * Writes the files the rootfs leaves out because they are the machine's own, when they are missing.
   *
   * The archive excludes them, as an image excludes them, so without this the runtime has no hosts file and anything
   * that resolves `localhost` fails. It is done for every process rather than at setup, so a runtime that was
   * unpacked before this was true is fixed the first time it is used, and a file the user changed is left alone.
   */
  @Synchronized
  private fun ensureMachineFiles() {
    if (!rootfsReady()) return
    MachineFiles.ensure(File(rootfs, "etc"))
  }

  private fun rootfsReady(): Boolean = File(rootfs, "usr/bin/bash").exists() && File(rootfs, "etc/os-release").exists()

  private fun toolsReady(): Boolean =
    File(rootfs, "usr/bin/node").canExecute() && File(rootfs, "usr/bin/script").canExecute()

  private fun rootfsVersion(): String? {
    if (!rootfsReady()) return null
    return runCatching {
      val fields = File(rootfs, "etc/os-release").readLines().map { it.split('=', limit = 2) }
        .filter { it.size == 2 }.associate { it[0] to it[1].trim('"') }
      fields["VERSION_ID"] ?: fields["PRETTY_NAME"]
    }.getOrNull()
  }

  private fun prepareRootfs() {
    File(rootfs, "tmp").mkdirs()
    runCatching { Os.chmod(File(rootfs, "tmp").absolutePath, 0x3FF) }
    MachineFiles.ensure(File(rootfs, "etc"))
    // The terminal and Pi both read these; an empty root has neither, and Bash's own defaults are enough.
    val bashrc = File(rootfs, "root/.bashrc")
    if (!bashrc.exists()) {
      bashrc.parentFile?.mkdirs()
      bashrc.writeText(
        "export PS1='\\[\\033[01;32m\\]\\u@wolfi\\[\\033[00m\\]:\\[\\033[01;34m\\]\\w\\[\\033[00m\\]\\$ '\n" +
          "export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\n" +
          "export LANG=C.UTF-8\n",
      )
    }
    val profile = File(rootfs, "root/.bash_profile")
    if (!profile.exists()) {
      profile.parentFile?.mkdirs()
      profile.writeText("[ -f ~/.bashrc ] && . ~/.bashrc\n")
    }

    // Termux-style storage links: ~/storage/* points at Android's shared storage so the agent and the
    // user can reach phone files without guessing /sdcard paths. Created once; a user who removed one
    // keeps it gone.
    val storageDir = File(rootfs, "root/storage")
    if (!storageDir.exists()) {
      storageDir.mkdirs()
      val links = mapOf(
        "shared" to "/sdcard",
        "downloads" to "/sdcard/Download",
        "documents" to "/sdcard/Documents",
        "pictures" to "/sdcard/Pictures",
        "music" to "/sdcard/Music",
        "movies" to "/sdcard/Movies",
        "dcim" to "/sdcard/DCIM",
      )
      for ((name, target) in links) {
        val link = File(storageDir, name)
        if (!link.exists()) {
          runCatching { Os.symlink(target, link.absolutePath) }
        }
      }
    }

  }

  private fun ensureNode(onLine: (String) -> Unit) {
    val node = File(rootfs, "usr/bin/node")
    if (toolsReady() && nodeVersionAtLeastMinimum()) return
    val apk = listOf("/sbin/apk", "/usr/bin/apk", "/bin/apk").firstOrNull { File(rootfs, it).exists() }
      ?: throw IOException("Wolfi OS has no apk")
    onLine(if (node.exists()) "Updating Wolfi OS tools…" else "Installing Wolfi OS tools…")
    val code = run(
      listOf(apk, "add", "--no-cache", "--upgrade", "bash", "busybox", "git", "nodejs", "npm", "script"),
      onLine,
    )
    if (code != 0 || !toolsReady()) throw IOException("Wolfi OS could not install its tools")
    if (!nodeVersionAtLeastMinimum()) throw IOException("Wolfi OS installed an old Node.js")
  }

  private fun nodeVersionAtLeastMinimum(): Boolean {
    val version = capture("/usr/bin/node", "-p", "process.versions.node").trim().split('.')
    val major = version.getOrNull(0)?.toIntOrNull() ?: return false
    val minor = version.getOrNull(1)?.toIntOrNull() ?: 0
    return major > 24 || (major == 24 && minor >= 0)
  }

  private fun run(command: List<String>, onLine: (String) -> Unit): Int {
    val builder = ProcessBuilder(this.command(*command.toTypedArray()))
    builder.directory(baseDir)
    builder.redirectErrorStream(true)
    builder.environment().clear()
    builder.environment().putAll(environment())
    val process = builder.start()
    process.inputStream.bufferedReader().useLines { lines ->
      for (line in lines) {
        if (line.isNotBlank()) onLine(line.take(200))
      }
    }
    return process.waitFor()
  }

  private fun capture(vararg command: String): String {
    val builder = ProcessBuilder(this.command(*command))
    builder.directory(baseDir)
    builder.redirectErrorStream(true)
    builder.environment().clear()
    builder.environment().putAll(environment())
    val process = builder.start()
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
      process.destroyForcibly()
      return ""
    }
    val output = process.inputStream.bufferedReader().use { it.readText() }
    return if (process.exitValue() == 0) output else ""
  }

  private fun download(onLine: (String) -> Unit): File {
    val asset =
      when (Build.SUPPORTED_ABIS.firstOrNull { it == "arm64-v8a" || it == "x86_64" }) {
        "arm64-v8a" -> "wolfi-rootfs-aarch64.tar.gz"
        "x86_64" -> "wolfi-rootfs-x86_64.tar.gz"
        else -> throw IOException("Wolfi OS supports 64-bit phones only")
      }
    val url = URL("$ROOTFS_REPO/releases/latest/download/$asset")
    val connection = (url.openConnection() as HttpURLConnection).apply {
      connectTimeout = 30_000
      readTimeout = 60_000
      instanceFollowRedirects = true
      setRequestProperty("User-Agent", "FangHarness")
    }
    onLine("Downloading Wolfi OS…")
    val temporary = File(baseDir, "rootfs.tar.gz.part")
    try {
      val total = connection.contentLengthLong
      connection.inputStream.use { input ->
        temporary.outputStream().buffered().use { output ->
          val buffer = ByteArray(64 * 1024)
          var copied = 0L
          var shown = -1
          while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            copied += read
            if (total > 0) {
              val percent = (copied * 100 / total).toInt()
              if (percent / 5 > shown / 5) {
                shown = percent
                onLine("Downloading Wolfi OS… $percent%")
              }
            }
          }
        }
      }
      if (temporary.length() < 1_000_000) throw IOException("The Wolfi download is empty")
      if (!temporary.renameTo(archive)) throw IOException("Could not keep the Wolfi download")
      return archive
    } catch (failure: Throwable) {
      temporary.delete()
      throw failure
    } finally {
      connection.disconnect()
    }
  }

  companion object {
    const val ROOTFS_REPO = "https://github.com/leloush-x/wolfi-os-rootfs"

    /**
     * The root of the Wolfi OS the runtime runs: the folder a chat opens in, what `~` means, and the home the
     * folder browser offers. It is the runtime's own filesystem, which is native storage, rather than Android's
     * shared storage, which is a slow FUSE mount.
     */
    const val WOLFI_ROOT = "/"

    /** Android's shared storage, which the runtime reaches only with All files access. */
    @SuppressLint("SdCardPath")
    const val SHARED_STORAGE = "/sdcard"

    /** Under `runtime`: the files bound over the `/proc` entries Android denies an app; `host/src/synthetic-proc.ts` names the same four. */
    private const val SYNTHETIC_PROC_DIR = "proc"

    // (unused legacy constants removed; FakeProcfs now generates proc files dynamically)

    private val SYSTEM_MOUNTS = listOf("/apex", "/odm", "/product", "/system", "/system_ext", "/vendor")
    private val SHARED_MOUNTS = listOf(SHARED_STORAGE, "/storage", "/data", "/dev", "/proc", "/sys")
  }
}
