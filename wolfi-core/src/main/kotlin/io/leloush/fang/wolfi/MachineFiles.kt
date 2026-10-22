package io.leloush.fang.wolfi

import java.io.File

/**
 * The files the rootfs archive leaves out, because they belong to the machine and not to the image: the hosts file,
 * the resolver and the hostname.
 *
 * The archive excludes them, as an image excludes them, so the runtime has to write them. Without the hosts file
 * anything in the runtime that resolves `localhost` fails, and the failure is a message about a name rather than
 * about the network: the agent resolves `localhost` on nearly every run.
 */
internal object MachineFiles {
  /** What a machine's own hosts file says about itself. */
  const val HOSTS = "127.0.0.1\tlocalhost\n::1\tlocalhost ip6-localhost ip6-loopback\n"

  /** The name the machine calls itself. */
  const val HOSTNAME = "localhost\n"

  /** The resolvers used when the machine has none of its own. */
  const val RESOLVERS = "nameserver 1.1.1.1\nnameserver 8.8.8.8\n"

  /**
   * Writes the files the folder [etc] is missing, and returns them. A file that is there is left alone, so a change
   * the user made to the hosts file survives, and a resolver that is there but empty is replaced, since an empty one
   * resolves nothing.
   */
  fun ensure(etc: File): List<File> {
    val written = mutableListOf<File>()
    val hosts = File(etc, "hosts")
    if (!hosts.exists()) {
      etc.mkdirs()
      hosts.writeText(HOSTS)
      written += hosts
    }
    val hostname = File(etc, "hostname")
    if (!hostname.exists()) {
      etc.mkdirs()
      hostname.writeText(HOSTNAME)
      written += hostname
    }
    val resolv = File(etc, "resolv.conf")
    if (!resolv.exists() || resolv.length() == 0L) {
      etc.mkdirs()
      resolv.writeText(RESOLVERS)
      written += resolv
    }
    return written
  }
}
