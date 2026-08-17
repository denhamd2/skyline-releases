package com.denham.skyline.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CastMediaTest {

    private val base = "http://iptv.example.com:25461"

    @Test
    fun `extension strips query and fragment`() {
        assertEquals("mp4", CastMedia.extensionOf("$base/movie/dave/s3cret/42.mp4"))
        assertEquals("mp4", CastMedia.extensionOf("$base/movie/dave/s3cret/42.mp4?token=abc"))
        assertEquals("m3u8", CastMedia.extensionOf("$base/live/dave/s3cret/5121.m3u8#frag"))
        assertEquals("ts", CastMedia.extensionOf("$base/live/dave/s3cret/5121.TS"))
    }

    @Test
    fun `extensionless timeshift url has no extension`() {
        // The dot in the host must not be mistaken for an extension.
        assertEquals("", CastMedia.extensionOf("$base/timeshift/dave/s3cret/60/2026-07-16:20-00/5121"))
    }

    @Test
    fun `mime types the default receiver understands`() {
        assertEquals("application/x-mpegURL", CastMedia.mimeTypeFor("$base/live/d/p/1.m3u8"))
        assertEquals("video/mp4", CastMedia.mimeTypeFor("$base/movie/d/p/1.mp4"))
        assertEquals("video/mp4", CastMedia.mimeTypeFor("$base/movie/d/p/1.m4v"))
        assertEquals("video/webm", CastMedia.mimeTypeFor("$base/movie/d/p/1.webm"))
    }

    @Test
    fun `containers the default receiver cannot play`() {
        // Progressive TS is the app's default live container -- casting it is
        // exactly the case that used to fail silently.
        assertNull(CastMedia.mimeTypeFor("$base/live/d/p/5121.ts"))
        assertNull(CastMedia.mimeTypeFor("$base/movie/d/p/42.mkv"))
        assertNull(CastMedia.mimeTypeFor("$base/movie/d/p/42.avi"))
        assertNull(CastMedia.mimeTypeFor("$base/timeshift/d/p/60/2026-07-16:20-00/5121"))
    }

    @Test
    fun `live casts the hls variant at the default position`() {
        // The regression test for both live defects at once: the TS URL local
        // playback is using must not reach the receiver, and the local live-window
        // offset must not be sent as a start position.
        val plan = CastMedia.plan(
            url = "$base/live/dave/s3cret/5121.ts",
            castUrl = "$base/live/dave/s3cret/5121.m3u8",
            isLive = true,
            localPositionMs = 1_800_000,
        )
        assertNotNull(plan)
        assertEquals("$base/live/dave/s3cret/5121.m3u8", plan!!.url)
        assertEquals("application/x-mpegURL", plan.mimeType)
        assertEquals(C.TIME_UNSET, plan.startPositionMs)
    }

    @Test
    fun `live without an hls variant cannot be cast`() {
        assertNull(
            CastMedia.plan(
                url = "$base/live/dave/s3cret/5121.ts",
                castUrl = null,
                isLive = true,
                localPositionMs = 0,
            )
        )
    }

    @Test
    fun `vod keeps its url and resume position`() {
        val plan = CastMedia.plan(
            url = "$base/movie/dave/s3cret/42.mp4",
            castUrl = "$base/movie/dave/s3cret/42.mp4",
            isLive = false,
            localPositionMs = 90_000,
        )
        assertNotNull(plan)
        assertEquals("$base/movie/dave/s3cret/42.mp4", plan!!.url)
        assertEquals("video/mp4", plan.mimeType)
        assertEquals(90_000L, plan.startPositionMs)
    }

    @Test
    fun `mkv vod cannot be cast`() {
        assertNull(
            CastMedia.plan(
                url = "$base/movie/dave/s3cret/42.mkv",
                castUrl = "$base/movie/dave/s3cret/42.mkv",
                isLive = false,
                localPositionMs = 0,
            )
        )
    }
}
