package com.denham.skyline.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * XMLTV timestamps look like "20260716063000 +0000"; some panels omit the
 * offset (treat as UTC) or the seconds. Returns epoch millis, or null when
 * the value is unparseable.
 */
object XmltvTime {

    private val SHAPE = Regex(
        "^(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})?\\s*([+-]\\d{4})?$"
    )
    private val BASIC = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    fun parseToEpochMs(raw: String): Long? {
        val m = SHAPE.find(raw.trim()) ?: return null
        val (y, mo, d, h, min) = m.destructured
        val sec = m.groupValues[6].ifEmpty { "00" }
        val offsetRaw = m.groupValues[7]
        return try {
            val local = LocalDateTime.parse("$y$mo$d$h$min$sec", BASIC)
            val offset = if (offsetRaw.isEmpty()) ZoneOffset.UTC else {
                val sign = if (offsetRaw[0] == '-') -1 else 1
                val hours = offsetRaw.substring(1, 3).toInt()
                val mins = offsetRaw.substring(3, 5).toInt()
                ZoneOffset.ofTotalSeconds(sign * (hours * 3600 + mins * 60))
            }
            local.toInstant(offset).toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    fun epochMsToInstant(ms: Long): Instant = Instant.ofEpochMilli(ms)
}

/**
 * Layout maths for the TV Guide grid: programmes are positioned/sized in
 * minutes relative to the visible window, clamped to its edges.
 */
object GuideMath {

    /** Minutes from window start to where this block begins (>= 0). */
    fun offsetMinutes(windowStartMs: Long, programmeStartMs: Long): Int =
        (((programmeStartMs.coerceAtLeast(windowStartMs)) - windowStartMs) / 60_000L).toInt()

    /**
     * Visible width of the block in minutes after clamping both ends to the
     * window; 0 when the programme lies entirely outside it.
     */
    fun widthMinutes(
        programmeStartMs: Long,
        programmeStopMs: Long,
        windowStartMs: Long,
        windowEndMs: Long,
    ): Int {
        val start = programmeStartMs.coerceAtLeast(windowStartMs)
        val stop = programmeStopMs.coerceAtMost(windowEndMs)
        if (stop <= start) return 0
        return ((stop - start) / 60_000L).toInt()
    }

    /** 0f..1f progress of `nowMs` through a programme (for progress bars). */
    fun progress(startMs: Long?, stopMs: Long?, nowMs: Long): Float {
        if (startMs == null || stopMs == null || stopMs <= startMs) return 0f
        return ((nowMs - startMs).toFloat() / (stopMs - startMs).toFloat()).coerceIn(0f, 1f)
    }
}
