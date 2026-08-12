package com.denham.skyline.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/** Category type discriminator — ids can collide across live/vod/series. */
object ContentType {
    const val LIVE = "live"
    const val VOD = "vod"
    const val SERIES = "series"
}

@Entity(tableName = "categories", primaryKeys = ["type", "categoryId"])
data class CategoryEntity(
    val type: String,
    val categoryId: String,
    val name: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "channels",
    indices = [Index("categoryId"), Index("name")],
)
data class ChannelEntity(
    @PrimaryKey val streamId: Int,
    val num: Int,
    val name: String,
    val icon: String?,
    val epgChannelId: String?,
    val categoryId: String,
    val tvArchive: Boolean,
    val tvArchiveDuration: Int = 0,
    val added: Long?,
    val description: String? = null,
)

@Fts4(contentEntity = ChannelEntity::class)
@Entity(tableName = "channels_fts")
data class ChannelFts(val name: String, val description: String?)

@Entity(
    tableName = "movies",
    indices = [Index("categoryId"), Index("added"), Index("name")],
)
data class MovieEntity(
    @PrimaryKey val streamId: Int,
    val name: String,
    val icon: String?,
    val rating5: Double,
    val containerExtension: String,
    val categoryId: String,
    val added: Long?,
    val description: String? = null,
)

@Fts4(contentEntity = MovieEntity::class)
@Entity(tableName = "movies_fts")
data class MovieFts(val name: String, val description: String?)

@Entity(
    tableName = "series",
    indices = [Index("categoryId"), Index("lastModified"), Index("name")],
)
data class SeriesEntity(
    @PrimaryKey val seriesId: Int,
    val name: String,
    val cover: String?,
    val plot: String?,
    val genre: String?,
    val rating5: Double,
    val backdrop: String?,
    val categoryId: String,
    val lastModified: Long?,
)

@Fts4(contentEntity = SeriesEntity::class)
@Entity(tableName = "series_fts")
data class SeriesFts(val name: String, val plot: String?, val genre: String?)

@Entity(tableName = "favorites", primaryKeys = ["type", "refId"])
data class FavoriteEntity(
    val type: String,
    val refId: Int,
    val addedAt: Long,
)

/** Full guide programme from the xmltv.php import (window-limited). */
@Entity(
    tableName = "epg_programmes",
    primaryKeys = ["channelStreamId", "startMs"],
    indices = [Index("channelStreamId", "stopMs")],
)
data class EpgProgrammeEntity(
    val channelStreamId: Int,
    val startMs: Long,
    val stopMs: Long,
    val title: String,
    val description: String?,
    /** Programme artwork from the XMLTV <icon>, when the provider supplies one. */
    val imageUrl: String? = null,
)

/** Bookkeeping for offline downloads (Media3 owns the bytes; we own the meta). */
@Entity(tableName = "download_meta")
data class DownloadMetaEntity(
    @PrimaryKey val contentId: String,   // the stream URL
    val title: String,
    val imageUrl: String?,
    val subtitle: String?,               // e.g. "Series 1, Ep 5"
    val addedAt: Long,
)

/**
 * Cache of artwork resolved for a programme title (TVmaze / own VOD posters), so
 * we look each title up once. A null [imageUrl] records "resolved, nothing found"
 * to avoid re-querying titles that have no art (news, sport, regional).
 */
@Entity(tableName = "programme_images")
data class ProgrammeImageEntity(
    @PrimaryKey val titleKey: String,   // normalised (lowercased, trimmed) title
    val imageUrl: String?,
    val resolvedAt: Long,
)

/** Cached now/next for a live channel from get_short_epg. */
@Entity(tableName = "epg_now_next")
data class EpgNowNextEntity(
    @PrimaryKey val streamId: Int,
    val nowTitle: String?,
    val nowDescription: String?,
    val nowStart: Long?,
    val nowStop: Long?,
    val nextTitle: String?,
    val nextStart: Long?,
    val nextStop: Long?,
    val fetchedAt: Long,
)

/** YouTube channel subscription (from RSS feed). */
@Entity(
    tableName = "youtube_channels",
    indices = [Index("channelId")],
)
data class YouTubeChannelEntity(
    @PrimaryKey val channelId: String,
    val title: String,
    val lastFetchedAt: Long = 0,
)

/** YouTube video from channel RSS feed. */
@Entity(
    tableName = "youtube_videos",
    indices = [Index("channelId"), Index("publishedAt")],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = YouTubeChannelEntity::class,
            parentColumns = ["channelId"],
            childColumns = ["channelId"],
            onDelete = androidx.room.ForeignKey.CASCADE,
        )
    ],
)
data class YouTubeVideoEntity(
    @PrimaryKey val videoId: String,
    val channelId: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val publishedAt: Long,  // Unix timestamp in milliseconds
)
