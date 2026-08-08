package com.inscopelabs.abx.ironmark

import com.inscopelabs.abx.ironmark.model.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Test

class IronMarkUnitTest {
    @Test
    fun testFormatBytes() {
        assertEquals("500 B", formatBytes(500L))
        assertEquals("1.00 KB", formatBytes(1024L))
        assertEquals("10.00 MB", formatBytes(10 * 1024 * 1024L))
        assertEquals("1.50 GB", formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }
}
