package com.denham.skyline.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun clear(type: String)

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder")
    fun observe(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories WHERE type = :type")
    suspend fun count(type: String): Int

    @Query("SELECT name FROM categories WHERE type = :type AND categoryId = :categoryId")
    suspend fun nameFor(type: String, categoryId: String): String?
}

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(items: List<ChannelEntity>)

    @Query("DELETE FROM channels")
    suspend fun clear()

    @Query("SELECT * FROM channels WHERE categoryId = :categoryId ORDER BY num")
    fun pagingByCategory(categoryId: String): PagingSource<Int, ChannelEntity>

    @Query("SELECT * FROM channels ORDER BY num")
    fun pagingAll(): PagingSource<Int, ChannelEntity>

    @Query(
        """SELECT channels.* FROM channels
           JOIN favorites ON favorites.refId = channels.streamId AND favorites.type = 'live'
           ORDER BY num"""
    )
    fun pagingByFavorites(): PagingSource<Int, ChannelEntity>

    @Query(
        """SELECT channels.* FROM channels
           JOIN channels_fts ON channels.rowid = channels_fts.rowid
           WHERE channels_fts MATCH :query ORDER BY num LIMIT 100"""
    )
    suspend fun search(query: String): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE streamId = :id")
    suspend fun byId(id: Int): ChannelEntity?

    /**
     * Resolves pinned-channel ids to channels. Ids no longer in the catalogue
     * simply do not come back, which is how a pin to a channel the provider
     * has dropped disappears instead of rendering as a broken card.
     */
    @Query("SELECT * FROM channels WHERE streamId IN (:ids) ORDER BY num")
    suspend fun byIds(ids: List<Int>): List<ChannelEntity>

    @Query(
        """SELECT channels.* FROM channels
           JOIN favorites ON favorites.refId = channels.streamId AND favorites.type = 'live'
           ORDER BY favorites.addedAt DESC"""
    )
    fun observeFavorites(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY num LIMIT :limit")
    fun observeFirst(limit: Int): Flow<List<ChannelEntity>>

    /** Channels that carry a catch-up/archive window (Recordings source). */
    @Query("SELECT * FROM channels WHERE tvArchive = 1 ORDER BY num")
    suspend fun archiveChannels(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE categoryId = :categoryId ORDER BY num LIMIT :limit")
    fun observeByCategory(categoryId: String, limit: Int): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels ORDER BY streamId LIMIT :limit OFFSET :offset")
    suspend fun pageForEpgMap(limit: Int, offset: Int): List<ChannelEntity>

    /** Guide display order: the provider's channel numbering (what users expect). */
    @Query("SELECT * FROM channels ORDER BY num, name LIMIT :limit OFFSET :offset")
    suspend fun pageByNumber(limit: Int, offset: Int): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE categoryId = :categoryId ORDER BY num LIMIT :limit OFFSET :offset")
    suspend fun pageByCategory(categoryId: String, limit: Int, offset: Int): List<ChannelEntity>

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun count(): Int

    /** Neighbours for channel zapping (previous/next by number within category). */
    @Query(
        """SELECT * FROM channels WHERE categoryId = :categoryId AND num > :num
           ORDER BY num LIMIT 1"""
    )
    suspend fun nextInCategory(categoryId: String, num: Int): ChannelEntity?

    @Query(
        """SELECT * FROM channels WHERE categoryId = :categoryId AND num < :num
           ORDER BY num DESC LIMIT 1"""
    )
    suspend fun previousInCategory(categoryId: String, num: Int): ChannelEntity?
}

@Dao
interface MovieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(items: List<MovieEntity>)

    @Query("DELETE FROM movies")
    suspend fun clear()

    @Query("SELECT * FROM movies WHERE categoryId = :categoryId ORDER BY name")
    fun pagingByCategory(categoryId: String): PagingSource<Int, MovieEntity>

    @Query("SELECT * FROM movies ORDER BY added DESC")
    fun pagingAll(): PagingSource<Int, MovieEntity>

    @Query(
        """SELECT movies.* FROM movies
           JOIN favorites ON favorites.refId = movies.streamId AND favorites.type = 'vod'
           ORDER BY added DESC"""
    )
    fun pagingByFavorites(): PagingSource<Int, MovieEntity>

    @Query(
        """SELECT movies.* FROM movies
           JOIN movies_fts ON movies.rowid = movies_fts.rowid
           WHERE movies_fts MATCH :query ORDER BY name LIMIT 100"""
    )
    suspend fun search(query: String): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE streamId = :id")
    suspend fun byId(id: Int): MovieEntity?

    @Query("SELECT * FROM movies ORDER BY added DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<MovieEntity>>

    @Query(
        """SELECT movies.* FROM movies
           JOIN favorites ON favorites.refId = movies.streamId AND favorites.type = 'vod'
           ORDER BY favorites.addedAt DESC"""
    )
    fun observeFavorites(): Flow<List<MovieEntity>>

    @Query("SELECT COUNT(*) FROM movies")
    suspend fun count(): Int

    /** Exact (case-insensitive) title match, for reusing a film's poster as
     *  programme artwork. */
    @Query("SELECT * FROM movies WHERE name = :title COLLATE NOCASE LIMIT 1")
    suspend fun findByName(title: String): MovieEntity?
}

@Dao
interface SeriesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(items: List<SeriesEntity>)

    @Query("DELETE FROM series")
    suspend fun clear()

    @Query("SELECT * FROM series WHERE categoryId = :categoryId ORDER BY name")
    fun pagingByCategory(categoryId: String): PagingSource<Int, SeriesEntity>

    @Query("SELECT * FROM series ORDER BY lastModified DESC")
    fun pagingAll(): PagingSource<Int, SeriesEntity>

    @Query(
        """SELECT series.* FROM series
           JOIN favorites ON favorites.refId = series.seriesId AND favorites.type = 'series'
           ORDER BY lastModified DESC"""
    )
    fun pagingByFavorites(): PagingSource<Int, SeriesEntity>

    @Query(
        """SELECT series.* FROM series
           JOIN series_fts ON series.rowid = series_fts.rowid
           WHERE series_fts MATCH :query ORDER BY name LIMIT 100"""
    )
    suspend fun search(query: String): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE seriesId = :id")
    suspend fun byId(id: Int): SeriesEntity?

    @Query("SELECT * FROM series ORDER BY lastModified DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int): Flow<List<SeriesEntity>>

    @Query(
        """SELECT series.* FROM series
           JOIN favorites ON favorites.refId = series.seriesId AND favorites.type = 'series'
           ORDER BY favorites.addedAt DESC"""
    )
    fun observeFavorites(): Flow<List<SeriesEntity>>

    @Query("SELECT COUNT(*) FROM series")
    suspend fun count(): Int

    @Query("SELECT * FROM series WHERE name = :title COLLATE NOCASE LIMIT 1")
    suspend fun findByName(title: String): SeriesEntity?
}

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE type = :type AND refId = :refId")
    suspend fun delete(type: String, refId: Int)

    @Query("SELECT COUNT(*) FROM favorites WHERE type = :type AND refId = :refId")
    suspend fun exists(type: String, refId: Int): Int

    @Query("SELECT refId FROM favorites WHERE type = :type")
    fun observeIds(type: String): Flow<List<Int>>

    @Query("DELETE FROM favorites")
    suspend fun clear()
}

@Dao
interface GuideDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(items: List<EpgProgrammeEntity>)

    @Query("DELETE FROM epg_programmes")
    suspend fun clear()

    @Query("DELETE FROM epg_programmes WHERE stopMs < :beforeMs")
    suspend fun deleteEndedBefore(beforeMs: Long)

    @Query(
        """SELECT * FROM epg_programmes
           WHERE channelStreamId IN (:channelIds)
             AND stopMs > :fromMs AND startMs < :toMs
           ORDER BY channelStreamId, startMs"""
    )
    suspend fun forChannelsBetween(
        channelIds: List<Int>,
        fromMs: Long,
        toMs: Long,
    ): List<EpgProgrammeEntity>

    @Query("SELECT COUNT(*) FROM epg_programmes")
    suspend fun count(): Int

    /** Current-programme artwork (when the feed carries it) for a set of channels. */
    @Query(
        """SELECT channelStreamId, imageUrl FROM epg_programmes
           WHERE channelStreamId IN (:channelIds)
             AND startMs <= :nowMs AND stopMs > :nowMs
             AND imageUrl IS NOT NULL
           GROUP BY channelStreamId"""
    )
    suspend fun nowImages(channelIds: List<Int>, nowMs: Long): List<NowImage>

    /**
     * Channels whose guide carries a programme mentioning both team names,
     * within a kickoff-relative window (typically kickoff +/- 90 min). Scans
     * across ALL channels rather than a pre-filtered id list -- unlike
     * [forChannelsBetween]/[nowImages], which are scoped to
     * favourites+popular channel ids for Home's tiles, a fixture's carrier
     * channel could be anywhere in the provider's catalogue (a dedicated
     * sports channel that never shows up in "popular").
     *
     * `%homeTeam%` / `%awayTeam%` are supplied pre-wrapped in `%` wildcards
     * by the caller so this stays a plain LIKE, matching guide titles like
     * "Man United v Man City" or "Manchester United - Manchester City".
     */
    @Query(
        """SELECT DISTINCT channelStreamId FROM epg_programmes
           WHERE stopMs > :fromMs AND startMs < :toMs
             AND (title LIKE :homeTeamLike OR title LIKE :awayTeamLike)"""
    )
    suspend fun channelsForFixture(
        homeTeamLike: String,
        awayTeamLike: String,
        fromMs: Long,
        toMs: Long,
    ): List<Int>
}

/** Projection: the current programme's artwork URL for a channel. */
data class NowImage(val channelStreamId: Int, val imageUrl: String)

@Dao
interface ProgrammeImageDao {
    @Query("SELECT * FROM programme_images WHERE titleKey = :key")
    suspend fun get(key: String): ProgrammeImageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ProgrammeImageEntity)
}

@Dao
interface DownloadMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DownloadMetaEntity)

    @Query("DELETE FROM download_meta WHERE contentId = :contentId")
    suspend fun delete(contentId: String)

    @Query("SELECT * FROM download_meta")
    fun observeAll(): Flow<List<DownloadMetaEntity>>

    @Query("SELECT * FROM download_meta WHERE contentId = :contentId")
    suspend fun byId(contentId: String): DownloadMetaEntity?
}

@Dao
interface EpgDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: EpgNowNextEntity)

    @Query("SELECT * FROM epg_now_next WHERE streamId = :streamId")
    suspend fun byId(streamId: Int): EpgNowNextEntity?

    @Query("SELECT * FROM epg_now_next WHERE streamId IN (:ids)")
    fun observeForIds(ids: List<Int>): Flow<List<EpgNowNextEntity>>

    @Query("DELETE FROM epg_now_next")
    suspend fun clear()
}

@Dao
interface YouTubeChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<YouTubeChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: YouTubeChannelEntity)

    @Query("DELETE FROM youtube_channels WHERE channelId = :channelId")
    suspend fun delete(channelId: String)

    @Query("SELECT * FROM youtube_channels WHERE channelId = :channelId")
    suspend fun byId(channelId: String): YouTubeChannelEntity?

    @Query("SELECT * FROM youtube_channels ORDER BY title")
    fun observeAll(): Flow<List<YouTubeChannelEntity>>

    @Query("DELETE FROM youtube_channels")
    suspend fun clear()
}

@Dao
interface YouTubeVideoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<YouTubeVideoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: YouTubeVideoEntity)

    @Query("DELETE FROM youtube_videos WHERE channelId = :channelId")
    suspend fun deleteByChannel(channelId: String)

    @Query(
        """DELETE FROM youtube_videos WHERE channelId = :channelId
           AND videoId NOT IN (
               SELECT videoId FROM youtube_videos
               WHERE channelId = :channelId
               ORDER BY publishedAt DESC
               LIMIT 20
           )"""
    )
    suspend fun pruneOlderThan20(channelId: String)

    @Query("SELECT * FROM youtube_videos WHERE channelId = :channelId ORDER BY publishedAt DESC")
    fun observeByChannel(channelId: String): Flow<List<YouTubeVideoEntity>>

    @Query(
        """SELECT * FROM youtube_videos
           WHERE channelId IN (:channelIds)
           ORDER BY publishedAt DESC
           LIMIT :limit"""
    )
    fun observeLatestForChannels(channelIds: List<String>, limit: Int = 30): Flow<List<YouTubeVideoEntity>>

    @Query("SELECT * FROM youtube_videos WHERE videoId = :videoId")
    suspend fun byId(videoId: String): YouTubeVideoEntity?

    @Query("DELETE FROM youtube_videos")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM youtube_videos WHERE channelId = :channelId")
    suspend fun countByChannel(channelId: String): Int
}
