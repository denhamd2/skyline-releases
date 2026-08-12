package com.denham.skyline.data.prefs

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the [CategoryPreferences] model that the per-member store reads and
 * writes: per-member isolation, and that preferences saved before a field
 * existed still decode.
 *
 * The store's own accessors need a real DataStore and Android Context, so
 * this exercises the serialisation and per-member map handling those
 * accessors are built on -- which is where the interesting failures live.
 */
class CategoryPreferencesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `pinned channels round-trip through json`() {
        val prefs = CategoryPreferences(pinnedChannels = mapOf("David" to listOf(101, 202)))
        val decoded = json.decodeFromString<CategoryPreferences>(json.encodeToString(prefs))
        assertEquals(listOf(101, 202), decoded.pinnedChannels["David"])
    }

    @Test
    fun `pinning for one member does not affect another`() {
        val prefs = CategoryPreferences(
            pinnedChannels = mapOf("David" to listOf(101), "Ava" to listOf(303)),
        )
        assertEquals(listOf(101), prefs.pinnedChannels["David"])
        assertEquals(listOf(303), prefs.pinnedChannels["Ava"])
    }

    @Test
    fun `a member with no pins reads as empty, not null`() {
        val prefs = CategoryPreferences(pinnedChannels = mapOf("David" to listOf(101)))
        // Mirrors getPinnedChannels' `?: emptyList()`.
        assertEquals(emptyList<Int>(), prefs.pinnedChannels["Sophie"] ?: emptyList<Int>())
    }

    @Test
    fun `preferences saved before pinnedChannels existed still decode`() {
        // Exactly what is already sitting in DataStore on the family's phones:
        // an object with no pinnedChannels key at all. It must decode to an
        // empty map rather than throwing, or existing installs lose their
        // saved categories and subscriptions on first read after the update.
        val legacy = """
            {"preferences":{"David":["sport","news"]},
             "youtubeSubscriptions":{"David":["UC123"]}}
        """.trimIndent()
        val decoded = json.decodeFromString<CategoryPreferences>(legacy)
        assertEquals(listOf("sport", "news"), decoded.preferences["David"])
        assertEquals(listOf("UC123"), decoded.youtubeSubscriptions["David"])
        assertTrue(decoded.pinnedChannels.isEmpty())
    }

    @Test
    fun `withMemberTab sets categories and pins together`() {
        // The regression this guards: the editor used to save these as two
        // separate writes, each doing its own read-modify-write of the same
        // stored value. The later write dropped the earlier one's change --
        // pins landed, categories vanished, and the home tab fell back to
        // keyword defaults as though nothing had been configured. Merging
        // them into one transform is what makes that impossible.
        val updated = CategoryPreferences().withMemberTab("David", listOf("epl"), listOf(101))

        assertEquals(listOf("epl"), updated.preferences["David"])
        assertEquals(listOf(101), updated.pinnedChannels["David"])
    }

    @Test
    fun `withMemberTab leaves other members untouched`() {
        val existing = CategoryPreferences(
            preferences = mapOf("Anne" to listOf("drama")),
            youtubeSubscriptions = mapOf("Anne" to listOf("UC-anne")),
            pinnedChannels = mapOf("Anne" to listOf(777)),
        )

        val updated = existing.withMemberTab("David", listOf("epl"), listOf(101))

        assertEquals(listOf("drama"), updated.preferences["Anne"])
        assertEquals(listOf(777), updated.pinnedChannels["Anne"])
        assertEquals(listOf("UC-anne"), updated.youtubeSubscriptions["Anne"])
    }

    @Test
    fun `withMemberTab does not disturb the member's own youtube subscriptions`() {
        // Subscriptions are edited on a different screen; saving the tab must
        // not be the thing that wipes them.
        val existing = CategoryPreferences(youtubeSubscriptions = mapOf("David" to listOf("UC-wwe")))

        val updated = existing.withMemberTab("David", listOf("epl"), listOf(101))

        assertEquals(listOf("UC-wwe"), updated.youtubeSubscriptions["David"])
    }

    @Test
    fun `withMemberTab can clear a member's selections`() {
        // Unticking everything and saving must actually empty them, not be
        // read as "no change".
        val existing = CategoryPreferences(
            preferences = mapOf("David" to listOf("epl")),
            pinnedChannels = mapOf("David" to listOf(101)),
        )

        val updated = existing.withMemberTab("David", emptyList(), emptyList())

        assertTrue(updated.preferences["David"]!!.isEmpty())
        assertTrue(updated.pinnedChannels["David"]!!.isEmpty())
    }

    @Test
    fun `categories and pins are stored independently`() {
        // A pinned channel does not have to live in a selected category --
        // that independence is the whole point of pinning.
        val prefs = CategoryPreferences(
            preferences = mapOf("David" to listOf("epl")),
            pinnedChannels = mapOf("David" to listOf(101)),
        )
        assertEquals(listOf("epl"), prefs.preferences["David"])
        assertEquals(listOf(101), prefs.pinnedChannels["David"])
    }
}
