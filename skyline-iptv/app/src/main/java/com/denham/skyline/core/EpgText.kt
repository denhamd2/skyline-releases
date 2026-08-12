package com.denham.skyline.core

import java.util.Base64

/**
 * Titles/descriptions in the JSON EPG endpoints (`get_short_epg`,
 * `get_simple_data_table`) are Base64-encoded. Some panels don't encode them
 * (or return already-decoded strings), so fall back to the raw value when
 * decoding fails or produces garbage.
 */
object EpgText {

    fun decode(raw: String): String {
        if (raw.isBlank()) return raw
        val candidate = raw.trim()
        // Base64 alphabet check first: avoids "decoding" plain words like
        // "News" that happen to be valid Base64 into binary garbage.
        if (!candidate.matches(BASE64_SHAPE)) return raw
        return try {
            val bytes = Base64.getDecoder().decode(candidate)
            val text = String(bytes, Charsets.UTF_8)
            if (text.any { it.isISOControl() && it != '\n' && it != '\r' && it != '\t' }) raw else text
        } catch (_: IllegalArgumentException) {
            raw
        }
    }

    // Base64 strings from the panel are padded blocks of 4.
    private val BASE64_SHAPE =
        Regex("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)$")
}
