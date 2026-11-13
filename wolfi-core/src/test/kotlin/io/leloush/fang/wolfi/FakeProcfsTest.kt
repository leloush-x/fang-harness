package io.leloush.fang.wolfi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeProcfsTest {
    @Test
    fun statContentHasOneCpuRowPerProcessor() {
        val content = FakeProcfs.statContent(4, 1000L)
        val lines = content.lines()
        val cpuRows = lines.filter { it.startsWith("cpu") && it.getOrNull(3)?.isDigit() == true }
        assertEquals(4, cpuRows.size)
    }

    @Test
    fun statContentAggregateIsColumnSum() {
        val content = FakeProcfs.statContent(4, 1000L)
        val lines = content.lines()
        val aggregate = lines.first { it.startsWith("cpu ") }.split(Regex("\\s+")).drop(1).map { it.toLong() }
        val firstRow = lines.first { it.startsWith("cpu0 ") }.split(Regex("\\s+")).drop(1).map { it.toLong() }
        assertEquals(firstRow.map { it * 4 }, aggregate)
    }

    @Test
    fun statContentIncludesBtime() {
        val content = FakeProcfs.statContent(2, 12345L)
        assertTrue(content.contains("btime 12345"))
    }

    @Test
    fun uptimeContentFormatsSeconds() {
        val content = FakeProcfs.uptimeContent(100.5)
        assertTrue(content.startsWith("100.50"))
    }

    @Test
    fun cpuCountParsesProcCpuinfo() {
        val cpuinfo = "processor\t: 0\nprocessor\t: 1\nprocessor\t: 2\n"
        assertEquals(3, FakeProcfs.cpuCount(cpuinfo))
    }

    @Test
    fun cpuCountFallsBackWhenEmpty() {
        assertTrue(FakeProcfs.cpuCount("") >= 1)
    }

    @Test
    fun meminfoAndLoadavgContainExpectedFields() {
        assertTrue(FakeProcfs.meminfoContent().contains("MemTotal:"))
        assertTrue(FakeProcfs.loadavgContent().isNotBlank())
        assertTrue(FakeProcfs.vmstatContent().contains("nr_free_pages"))
        assertTrue(FakeProcfs.versionContent().contains("Linux version"))
    }
}
