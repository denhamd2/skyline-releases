package com.denham.skyline.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.Executors

/**
 * Owns the download machinery: the SimpleCache holding downloaded bytes, the
 * DownloadManager, and the CacheDataSource factory the player uses so that
 * downloaded content plays offline. Only direct-file VOD is downloadable
 * (progressive mp4/mkv); live streams are not.
 */
@UnstableApi
class DownloadsCenter(
    private val context: Context,
    okHttpClient: OkHttpClient,
) {

    private val databaseProvider by lazy { StandaloneDatabaseProvider(context) }

    val downloadDirectory: File by lazy {
        File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads")
    }

    val cache: SimpleCache by lazy {
        SimpleCache(downloadDirectory, NoOpCacheEvictor(), databaseProvider)
    }

    private val upstreamFactory by lazy { OkHttpDataSource.Factory(okHttpClient) }

    /** Read-through factory for the player: local bytes when downloaded. */
    val playbackDataSourceFactory: CacheDataSource.Factory by lazy {
        CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(null) // only DownloadManager writes
    }

    val downloadManager: DownloadManager by lazy {
        DownloadManager(
            context,
            databaseProvider,
            cache,
            upstreamFactory,
            Executors.newFixedThreadPool(2),
        ).apply {
            maxParallelDownloads = 1 // each download consumes a provider connection
        }
    }

    fun startDownload(url: String) {
        val request = DownloadRequest.Builder(url, android.net.Uri.parse(url)).build()
        DownloadService.sendAddDownload(context, SkylineDownloadService::class.java, request, false)
    }

    fun removeDownload(url: String) {
        DownloadService.sendRemoveDownload(context, SkylineDownloadService::class.java, url, false)
    }

    fun currentDownloads(): List<Download> {
        val result = mutableListOf<Download>()
        downloadManager.downloadIndex.getDownloads().use { cursor ->
            while (cursor.moveToNext()) result.add(cursor.download)
        }
        return result
    }

    fun isDownloaded(url: String): Boolean =
        downloadManager.downloadIndex.getDownload(url)?.state == Download.STATE_COMPLETED

    fun totalDownloadedBytes(): Long = cache.cacheSpace
}
