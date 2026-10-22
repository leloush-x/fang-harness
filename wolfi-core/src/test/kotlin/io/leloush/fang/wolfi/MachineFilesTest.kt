package io.leloush.fang.wolfi

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The files the rootfs archive leaves out, because they belong to the machine: the hosts file, the hostname and the
 * resolver. The runtime writes them, so the agent can resolve `localhost`.
 */
class MachineFilesTest {
  private fun tempEtc(): File {
    val dir = Files.createTempDirectory("fang-machine-files").toFile()
    dir.deleteOnExit()
    return File(dir, "etc")
  }

  @Test
  fun writesTheFilesTheArchiveLeavesOut() {
    val etc = tempEtc()

    val written = MachineFiles.ensure(etc)

    assertEquals(3, written.size)
    assertTrue(File(etc, "hosts").exists())
    assertTrue(File(etc, "hostname").exists())
    assertTrue(File(etc, "resolv.conf").exists())
  }

  @Test
  fun theHostsFileNamesLocalhost() {
    val etc = tempEtc()

    MachineFiles.ensure(etc)

    val hosts = File(etc, "hosts").readText()
    assertTrue(hosts, hosts.contains("127.0.0.1\tlocalhost"))
    assertTrue(hosts, hosts.contains("::1\tlocalhost"))
  }

  @Test
  fun leavesAHostsFileTheUserChangedAlone() {
    val etc = tempEtc()
    etc.mkdirs()
    File(etc, "hosts").writeText("127.0.0.1\tmy-host\n")

    MachineFiles.ensure(etc)

    assertEquals("127.0.0.1\tmy-host\n", File(etc, "hosts").readText())
  }

  @Test
  fun replacesAResolverThatIsThereButEmpty() {
    val etc = tempEtc()
    etc.mkdirs()
    File(etc, "resolv.conf").writeText("")

    MachineFiles.ensure(etc)

    assertEquals(MachineFiles.RESOLVERS, File(etc, "resolv.conf").readText())
  }

  @Test
  fun writesNothingTheSecondTime() {
    val etc = tempEtc()
    MachineFiles.ensure(etc)

    assertTrue(MachineFiles.ensure(etc).isEmpty())
  }

  @Test
  fun createsTheFolderWhenItIsNotThere() {
    val dir = Files.createTempDirectory("fang-machine-files").toFile()
    dir.deleteOnExit()
    val etc = File(dir, "etc")

    MachineFiles.ensure(etc)

    assertTrue(etc.isDirectory)
    assertFalse(File(etc, "hosts").readText().isEmpty())
  }
}
