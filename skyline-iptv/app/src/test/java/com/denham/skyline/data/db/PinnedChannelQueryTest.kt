package com.denham.skyline.data.db

import androidx.room.Room
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers [ChannelDao.byIds], which resolves a member's pinned channel ids to
 * channels.
 *
 * The case worth pinning down is a pin to a channel the provider has since
 * dropped: the id stays in DataStore but no longer exists in the catalogue.
 * That must quietly disappear from the rail rather than render a broken card,
 * and the query gets that for free by simply not matching -- which is exactly
 * the kind of behaviour that is true by accident until someone asserts it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class PinnedChannelQueryTest {

    private lateinit var db: SkylineDatabase

    @Before
    fun setUp() {
        // RuntimeEnvironment rather than androidx.test's ApplicationProvider:
        // androidx.test.core is not a declared test dependency here, and
        // relying on it arriving transitively through Robolectric is the kind
        // of thing that breaks on a version bump.
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            SkylineDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun channel(id: Int, num: Int, name: String) = ChannelEntity(
        streamId = id,
        num = num,
        name = name,
        icon = null,
        epgChannelId = null,
        categoryId = "sport",
        tvArchive = false,
        added = null,
    )

    @Test
    fun `resolves pinned ids to channels`() = runTest {
        db.channelDao().insertChunk(
            listOf(
                channel(101, 1, "MUTV"),
                channel(202, 2, "Sky Sports Main Event"),
                channel(303, 3, "BT Sport 1"),
            )
        )

        val pinned = db.channelDao().byIds(listOf(101, 303))

        assertEquals(listOf("MUTV", "BT Sport 1"), pinned.map { it.name })
    }

    @Test
    fun `an id no longer in the catalogue is omitted, not broken`() = runTest {
        db.channelDao().insertChunk(listOf(channel(101, 1, "MUTV")))

        // 999 was pinned before a sync removed it from the provider's lineup.
        val pinned = db.channelDao().byIds(listOf(101, 999))

        assertEquals(listOf("MUTV"), pinned.map { it.name })
    }

    @Test
    fun `every pinned id missing yields an empty rail`() = runTest {
        db.channelDao().insertChunk(listOf(channel(101, 1, "MUTV")))

        assertTrue(db.channelDao().byIds(listOf(998, 999)).isEmpty())
    }

    @Test
    fun `no pins yields no channels`() = runTest {
        db.channelDao().insertChunk(listOf(channel(101, 1, "MUTV")))

        assertTrue(db.channelDao().byIds(emptyList()).isEmpty())
    }

    @Test
    fun `results are ordered by channel number, not by pin order`() = runTest {
        db.channelDao().insertChunk(
            listOf(
                channel(101, 9, "MUTV"),
                channel(202, 2, "Sky Sports Main Event"),
            )
        )

        val pinned = db.channelDao().byIds(listOf(101, 202))

        assertEquals(listOf("Sky Sports Main Event", "MUTV"), pinned.map { it.name })
    }
}
