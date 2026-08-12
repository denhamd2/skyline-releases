package com.denham.skyline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadProgressTest {

    @Test
    fun `zero bytes read is zero percent`() {
        assertEquals(0, DownloadProgress.percentOf(bytesRead = 0, totalBytes = 1000))
    }

    @Test
    fun `partial download is proportional`() {
        assertEquals(25, DownloadProgress.percentOf(bytesRead = 250, totalBytes = 1000))
        assertEquals(50, DownloadProgress.percentOf(bytesRead = 500, totalBytes = 1000))
        assertEquals(99, DownloadProgress.percentOf(bytesRead = 999, totalBytes = 1000))
    }

    @Test
    fun `full download is one hundred percent`() {
        assertEquals(100, DownloadProgress.percentOf(bytesRead = 1000, totalBytes = 1000))
    }

    @Test
    fun `unknown total returns null instead of dividing by zero`() {
        assertNull(DownloadProgress.percentOf(bytesRead = 500, totalBytes = -1))
        assertNull(DownloadProgress.percentOf(bytesRead = 500, totalBytes = 0))
    }

    @Test
    fun `result is clamped even if more bytes arrive than the reported total`() {
        // Some servers report a Content-Length smaller than what actually
        // arrives; don't let the percentage overshoot 100.
        assertEquals(100, DownloadProgress.percentOf(bytesRead = 1500, totalBytes = 1000))
    }

    @Test
    fun `large apk sized transfer does not overflow`() {
        val totalBytes = 250L * 1024 * 1024 // ~250MB APK
        val bytesRead = 125L * 1024 * 1024
        assertEquals(50, DownloadProgress.percentOf(bytesRead, totalBytes))
    }
}
