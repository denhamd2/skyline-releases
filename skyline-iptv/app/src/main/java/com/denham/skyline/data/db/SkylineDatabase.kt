package com.denham.skyline.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ChannelEntity::class, ChannelFts::class,
        MovieEntity::class, MovieFts::class,
        SeriesEntity::class, SeriesFts::class,
        FavoriteEntity::class,
        EpgNowNextEntity::class,
        EpgProgrammeEntity::class,
        ProgrammeImageEntity::class,
        DownloadMetaEntity::class,
        YouTubeChannelEntity::class,
        YouTubeVideoEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class SkylineDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun epgDao(): EpgDao
    abstract fun guideDao(): GuideDao
    abstract fun downloadMetaDao(): DownloadMetaDao
    abstract fun programmeImageDao(): ProgrammeImageDao
    abstract fun youtubeChannelDao(): YouTubeChannelDao
    abstract fun youtubeVideoDao(): YouTubeVideoDao

    companion object {
        /**
         * v3 → v4 adds the programme artwork column. A real migration (not a
         * destructive rebuild) so user-owned data — favourites especially —
         * survives the update.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE epg_programmes ADD COLUMN imageUrl TEXT")
            }
        }

        /** v4 → v5 adds the resolved-artwork cache. Additive, preserves data. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS programme_images (" +
                        "titleKey TEXT NOT NULL PRIMARY KEY, " +
                        "imageUrl TEXT, " +
                        "resolvedAt INTEGER NOT NULL)"
                )
            }
        }

        /** v5 → v6 adds YouTube channel and video tables. Additive, preserves data. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS youtube_channels (" +
                        "channelId TEXT NOT NULL PRIMARY KEY, " +
                        "title TEXT NOT NULL, " +
                        "lastFetchedAt INTEGER NOT NULL DEFAULT 0)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_youtube_channels_channelId " +
                        "ON youtube_channels(channelId)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS youtube_videos (" +
                        "videoId TEXT NOT NULL PRIMARY KEY, " +
                        "channelId TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "description TEXT, " +
                        "thumbnailUrl TEXT, " +
                        "publishedAt INTEGER NOT NULL, " +
                        "FOREIGN KEY(channelId) REFERENCES youtube_channels(channelId) ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_youtube_videos_channelId " +
                        "ON youtube_videos(channelId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_youtube_videos_publishedAt " +
                        "ON youtube_videos(publishedAt)"
                )
            }
        }

        /** v6 → v7 extends FTS indexing to include description fields. Additive + schema change. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add description columns to content tables (non-destructive)
                db.execSQL("ALTER TABLE channels ADD COLUMN description TEXT")
                db.execSQL("ALTER TABLE movies ADD COLUMN description TEXT")

                // Recreate FTS tables with expanded schema
                // Drop old FTS tables (virtual tables)
                db.execSQL("DROP TABLE IF EXISTS channels_fts")
                db.execSQL("DROP TABLE IF EXISTS movies_fts")
                db.execSQL("DROP TABLE IF EXISTS series_fts")

                // Recreate FTS tables with new schema including description/plot/genre
                db.execSQL(
                    "CREATE VIRTUAL TABLE channels_fts USING fts4(" +
                        "name, description, content=channels)"
                )
                db.execSQL(
                    "CREATE VIRTUAL TABLE movies_fts USING fts4(" +
                        "name, description, content=movies)"
                )
                db.execSQL(
                    "CREATE VIRTUAL TABLE series_fts USING fts4(" +
                        "name, plot, genre, content=series)"
                )

                // Populate FTS tables from content tables
                db.execSQL(
                    "INSERT INTO channels_fts(rowid, name, description) " +
                        "SELECT rowid, name, description FROM channels"
                )
                db.execSQL(
                    "INSERT INTO movies_fts(rowid, name, description) " +
                        "SELECT rowid, name, description FROM movies"
                )
                db.execSQL(
                    "INSERT INTO series_fts(rowid, name, plot, genre) " +
                        "SELECT rowid, name, plot, genre FROM series"
                )
            }
        }

        fun build(context: Context): SkylineDatabase =
            Room.databaseBuilder(context, SkylineDatabase::class.java, "skyline.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                // Content tables are a server cache; only fall back to a rebuild
                // for schema jumps we haven't written a migration for.
                .fallbackToDestructiveMigration()
                .build()
    }
}
