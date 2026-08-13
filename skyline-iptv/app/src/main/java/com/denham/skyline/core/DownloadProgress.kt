package com.denham.skyline.core

/**
 * Pure helper for turning a byte count into a download percentage.
 *
 * Extracted out of [com.denham.skyline.data.repo.UpdateRepository] so the
 * arithmetic can be unit-tested on the JVM without a network stack.
 */
object DownloadProgress {

    /**
     * Returns the percentage complete for [bytesRead] out of [totalBytes],
     * or `null` when [totalBytes] is unknown (absent/invalid `Content-Length`,
     * e.g. chunked transfer encoding) so callers can fall back to an
     * indeterminate state instead of dividing by zero or reporting a bogus
     * percentage.
     *
     * The result is clamped to `0..100` in case a server reports a
     * `Content-Length` smaller than what actually arrives.
     */
    fun percentOf(bytesRead: Long, totalBytes: Long): Int? {
        if (totalBytes <= 0) return null
        val pct = (bytesRead * 100 / totalBytes).toInt()
        return pct.coerceIn(0, 100)
    }
}
