package com.denham.skyline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XmltvTimeTest {

    @Test
    fun `parses utc timestamp with offset`() {
        // 2026-07-16 06:30:00 UTC
        assertEquals(1784183400000L, XmltvTime.parseToEpochMs("20260716063000 +0000"))
    }

    @Test
    fun `parses positive offset`() {
        // 08:30 +0200 == 06:30 UTC
        assertEquals(1784183400000L, XmltvTime.parseToEpochMs("20260716083000 +0200"))
    }

    @Test
    fun `parses negative offset`() {
        // 01:30 -0500 == 06:30 UTC
        assertEquals(1784183400000L, XmltvTime.parseToEpochMs("20260716013000 -0500"))
    }

    @Test
    fun `missing offset treated as utc`() {
        assertEquals(1784183400000L, XmltvTime.parseToEpochMs("20260716063000"))
    }

    @Test
    fun `missing seconds tolerated`() {
        assertEquals(1784183400000L, XmltvTime.parseToEpochMs("202607160630 +0000"))
    }

    @Test
    fun `garbage returns null`() {
        assertNull(XmltvTime.parseToEpochMs(""))
        assertNull(XmltvTime.parseToEpochMs("not a date"))
        assertNull(XmltvTime.parseToEpochMs("2026-07-16 06:30"))
    }

    @Test
    fun `guide block clamps to window`() {
        val windowStart = 1_000_000_000_000L
        val windowEnd = windowStart + 2 * 60 * 60_000L // 2h window

        // Fully inside: 30 min at +15 min.
        assertEquals(15, GuideMath.offsetMinutes(windowStart, windowStart + 15 * 60_000L))
        assertEquals(
            30,
            GuideMath.widthMinutes(
                windowStart + 15 * 60_000L, windowStart + 45 * 60_000L,
                windowStart, windowEnd,
            )
        )

        // Starts before the window: offset clamps to 0, width trims.
        assertEquals(0, GuideMath.offsetMinutes(windowStart, windowStart - 30 * 60_000L))
        assertEquals(
            60,
            GuideMath.widthMinutes(
                windowStart - 30 * 60_000L, windowStart + 60 * 60_000L,
                windowStart, windowEnd,
            )
        )

        // Entirely outside: zero width.
        assertEquals(
            0,
            GuideMath.widthMinutes(
                windowEnd + 60_000L, windowEnd + 30 * 60_000L,
                windowStart, windowEnd,
            )
        )
    }

    @Test
    fun `programme progress`() {
        assertEquals(0.5f, GuideMath.progress(0L, 100_000L, 50_000L))
        assertEquals(0f, GuideMath.progress(null, 100L, 50L))
        assertEquals(1f, GuideMath.progress(0L, 100L, 500L))
        assertEquals(0f, GuideMath.progress(100L, 100L, 100L))
    }
}
