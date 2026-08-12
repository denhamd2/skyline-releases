package com.denham.skyline.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XtreamParsingTest {

    @Test
    fun `auth response with string booleans and numeric strings parses`() {
        val json = """
            {
              "user_info": {
                "username": "dave",
                "auth": 1,
                "status": "Active",
                "exp_date": "1789600000",
                "is_trial": "0",
                "active_cons": "1",
                "max_connections": "2",
                "allowed_output_formats": ["m3u8", "ts"]
              },
              "server_info": {
                "url": "example.com",
                "port": 8080,
                "https_port": "25463",
                "server_protocol": "http",
                "timezone": "Europe/London",
                "timestamp_now": 1752624000
              }
            }
        """.trimIndent()

        val resp = XtreamJson.decodeFromString(PlayerApiResponse.serializer(), json)
        val user = resp.userInfo!!
        assertTrue(user.auth)
        assertEquals("Active", user.status)
        assertEquals(1789600000L, user.expDate)
        assertFalse(user.isTrial)
        assertEquals(1, user.activeConnections)
        assertEquals(2, user.maxConnections)
        assertEquals(listOf("m3u8", "ts"), user.allowedOutputFormats)
        assertEquals("8080", resp.serverInfo!!.port)
        assertEquals("Europe/London", resp.serverInfo!!.timezone)
    }

    @Test
    fun `auth failure with auth=0 and null exp_date parses`() {
        val json = """{"user_info":{"auth":0,"status":"Banned","exp_date":null}}"""
        val resp = XtreamJson.decodeFromString(PlayerApiResponse.serializer(), json)
        assertFalse(resp.userInfo!!.auth)
        assertNull(resp.userInfo!!.expDate)
        assertNull(resp.serverInfo)
    }

    @Test
    fun `live stream with mixed types parses`() {
        val json = """
            [{"num":"3","name":"Channel One HD","stream_id":"5121",
              "stream_icon":"http://x/icon.png","epg_channel_id":"ch1.uk",
              "category_id":7,"tv_archive":1,"tv_archive_duration":"3","added":"1700000000"}]
        """.trimIndent()
        val list = XtreamJson.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(LiveStreamDto.serializer()), json
        )
        val ch = list.single()
        assertEquals(5121, ch.streamId)
        assertEquals("7", ch.categoryId)
        assertTrue(ch.tvArchive)
        assertEquals(1700000000L, ch.added)
    }

    @Test
    fun `vod info with empty-array info does not crash`() {
        val json = """{"info":[],"movie_data":{"stream_id":42,"name":"Film","container_extension":"mkv"}}"""
        val resp = VodInfoResponse.parse(json)
        assertNull(resp.info)
        assertEquals(42, resp.movieData!!.streamId)
        assertEquals("mkv", resp.movieData!!.containerExtension)
    }

    @Test
    fun `vod info with object info parses backdrops from string or array`() {
        val asArray = """{"info":{"plot":"p","backdrop_path":["http://x/1.jpg"]}}"""
        val asString = """{"info":{"plot":"p","backdrop_path":"http://x/1.jpg"}}"""
        assertEquals(
            listOf("http://x/1.jpg"),
            VodInfoResponse.parse(asArray).info!!.backdropPath
        )
        assertEquals(
            listOf("http://x/1.jpg"),
            VodInfoResponse.parse(asString).info!!.backdropPath
        )
    }

    @Test
    fun `series info with map episodes parses`() {
        val json = """
            {"info":{"series_id":9,"name":"Show"},
             "episodes":{"1":[{"id":"100","episode_num":1,"title":"Pilot","container_extension":"mp4","season":1,"info":{"plot":"x"}}],
                         "2":[{"id":"200","episode_num":"1","title":"S2E1","container_extension":"mkv","season":2,"info":[]}]}}
        """.trimIndent()
        val resp = SeriesInfoResponse.parse(json)
        assertEquals("Show", resp.info!!.name)
        assertEquals(2, resp.episodes.size)
        assertEquals(100, resp.episodes["1"]!!.single().id)
        assertEquals("x", resp.episodes["1"]!!.single().info!!.plot)
        // info: [] tolerated on episodes too
        assertNull(resp.episodes["2"]!!.single().info)
    }

    @Test
    fun `series info with array-of-arrays episodes parses`() {
        val json = """
            {"episodes":[[{"id":1,"episode_num":1,"title":"a","season":1}],
                         [{"id":2,"episode_num":1,"title":"b","season":2}]]}
        """.trimIndent()
        val resp = SeriesInfoResponse.parse(json)
        assertEquals(setOf("1", "2"), resp.episodes.keys)
        assertEquals(2, resp.episodes["2"]!!.single().id)
    }

    @Test
    fun `series info with info as empty array does not crash`() {
        val resp = SeriesInfoResponse.parse("""{"info":[],"episodes":{}}""")
        assertNull(resp.info)
        assertTrue(resp.episodes.isEmpty())
    }

    @Test
    fun `short epg parses base64 titles`() {
        val json = """
            {"epg_listings":[{"id":"1","title":"TmluZSBPJ0Nsb2NrIE5ld3M=",
              "description":"VGhlIGxhdGVzdCBoZWFkbGluZXMu",
              "start_timestamp":"1752624000","stop_timestamp":"1752627600","now_playing":1}]}
        """.trimIndent()
        val epg = XtreamJson.decodeFromString(ShortEpgResponse.serializer(), json)
        val entry = epg.epgListings.single()
        assertEquals("Nine O'Clock News", EpgText.decode(entry.title))
        assertEquals("The latest headlines.", EpgText.decode(entry.description))
        assertEquals(1752624000L, entry.startTimestamp)
        assertTrue(entry.nowPlaying)
    }

    @Test
    fun `category ids normalise to strings`() {
        val json = """[{"category_id":5,"category_name":"Sports","parent_id":"0"}]"""
        val cats = XtreamJson.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(CategoryDto.serializer()), json
        )
        assertEquals("5", cats.single().categoryId)
        assertEquals(0, cats.single().parentId)
    }

    @Test
    fun `unknown keys are ignored everywhere`() {
        val json = """{"user_info":{"auth":true,"some_new_panel_field":{"x":1}}}"""
        assertNotNull(XtreamJson.decodeFromString(PlayerApiResponse.serializer(), json).userInfo)
    }
}
