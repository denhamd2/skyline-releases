package com.denham.skyline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamUrlBuilderTest {

    private val account = XtreamAccount(
        portal = PortalAddress("http", "iptv.example.com", 25461),
        username = "dave",
        password = "s3cret",
    )
    private val urls = StreamUrlBuilder(account)

    @Test
    fun `live ts url`() {
        assertEquals(
            "http://iptv.example.com:25461/live/dave/s3cret/5121.ts",
            urls.live(5121, LiveContainer.TS)
        )
    }

    @Test
    fun `live hls url`() {
        assertEquals(
            "http://iptv.example.com:25461/live/dave/s3cret/5121.m3u8",
            urls.live(5121, LiveContainer.HLS)
        )
    }

    @Test
    fun `vod url uses container extension and defaults to mp4`() {
        assertEquals(
            "http://iptv.example.com:25461/movie/dave/s3cret/42.mkv",
            urls.movie(42, "mkv")
        )
        assertEquals(
            "http://iptv.example.com:25461/movie/dave/s3cret/42.mp4",
            urls.movie(42, "")
        )
    }

    @Test
    fun `series episode url`() {
        assertEquals(
            "http://iptv.example.com:25461/series/dave/s3cret/900.mp4",
            urls.seriesEpisode(900, "mp4")
        )
    }

    @Test
    fun `timeshift url`() {
        assertEquals(
            "http://iptv.example.com:25461/timeshift/dave/s3cret/60/2026-07-16:20-00/5121",
            urls.timeshift(5121, 60, "2026-07-16:20-00")
        )
    }

    @Test
    fun `container pick honours allowed_output_formats`() {
        assertEquals(LiveContainer.TS, LiveContainer.pick(listOf("ts", "m3u8"), LiveContainer.TS))
        assertEquals(LiveContainer.HLS, LiveContainer.pick(listOf("m3u8"), LiveContainer.TS))
        assertEquals(LiveContainer.TS, LiveContainer.pick(emptyList(), LiveContainer.TS))
        // Neither allowed (weird server data): keep the preference.
        assertEquals(LiveContainer.HLS, LiveContainer.pick(listOf("rtmp"), LiveContainer.HLS))
    }

    @Test
    fun `portal parse handles the shapes users actually type`() {
        assertEquals(
            PortalAddress("http", "example.com", 8080),
            PortalAddress.parse("example.com:8080")
        )
        assertEquals(
            PortalAddress("http", "example.com", 80),
            PortalAddress.parse("http://example.com/")
        )
        assertEquals(
            PortalAddress("https", "example.com", 443),
            PortalAddress.parse("https://example.com")
        )
        assertEquals(
            PortalAddress("https", "example.com", 25463),
            PortalAddress.parse("HTTPS://example.com:25463/player_api.php?username=a&password=b")
        )
        assertNull(PortalAddress.parse(""))
        assertNull(PortalAddress.parse("   "))
        assertNull(PortalAddress.parse("http://"))
    }
}
