package com.denham.skyline.core

import org.junit.Assert.assertEquals
import org.junit.Test

class EpgTextAndRedactTest {

    @Test
    fun `decodes padded base64`() {
        assertEquals("Match of the Day", EpgText.decode("TWF0Y2ggb2YgdGhlIERheQ=="))
    }

    @Test
    fun `plain text passes through unchanged`() {
        assertEquals("Evening News at 9", EpgText.decode("Evening News at 9"))
        assertEquals("", EpgText.decode(""))
        assertEquals("  ", EpgText.decode("  "))
    }

    @Test
    fun `text with spaces is never treated as base64`() {
        // Contains spaces -> not base64 shape -> raw.
        assertEquals("ABCD EFGH", EpgText.decode("ABCD EFGH"))
    }

    @Test
    fun `binary-looking decode falls back to raw`() {
        // "abcd" is valid base64 but decodes to bytes with control chars.
        assertEquals("abcd", EpgText.decode("abcd"))
    }

    @Test
    fun `utf8 survives decoding`() {
        // "Fútbol: Perú vs Chile" in Base64.
        assertEquals(
            "Fútbol: Perú vs Chile",
            EpgText.decode("RsO6dGJvbDogUGVyw7ogdnMgQ2hpbGU=")
        )
    }

    @Test
    fun `redacts credentials in query strings`() {
        assertEquals(
            "http://h:80/player_api.php?username=•••&password=•••&action=get_live_streams",
            Redact.url("http://h:80/player_api.php?username=dave&password=s3cret&action=get_live_streams")
        )
    }

    @Test
    fun `redacts credentials in stream paths`() {
        assertEquals(
            "http://h:80/live/•••/•••/5121.ts",
            Redact.url("http://h:80/live/dave/s3cret/5121.ts")
        )
        assertEquals(
            "http://h:80/timeshift/•••/•••/60/2026-07-16:20-00/5121",
            Redact.url("http://h:80/timeshift/dave/s3cret/60/2026-07-16:20-00/5121")
        )
    }

    @Test
    fun `error mapping covers the xtream codes`() {
        listOf(401, 403, 404, 405, 406, 429, 500, 418).forEach { code ->
            assert(XtreamError.messageFor(code).isNotBlank())
        }
    }
}
