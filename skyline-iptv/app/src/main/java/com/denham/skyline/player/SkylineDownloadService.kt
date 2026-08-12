package com.denham.skyline.player

import android.app.Notification
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.NotificationUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.denham.skyline.R
import com.denham.skyline.SkylineApp

private const val CHANNEL_ID = "skyline_downloads"
private const val FOREGROUND_ID = 2001
private const val JOB_ID = 3001

@UnstableApi
class SkylineDownloadService : DownloadService(
    FOREGROUND_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    R.string.download_channel_name,
    0,
) {

    override fun getDownloadManager(): DownloadManager =
        (application as SkylineApp).container.downloadsCenter.downloadManager

    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        NotificationUtil.createNotificationChannel(
            this, CHANNEL_ID, R.string.download_channel_name, 0,
            NotificationUtil.IMPORTANCE_LOW,
        )
        val active = downloads.count { it.state == Download.STATE_DOWNLOADING }
        val percent = downloads
            .filter { it.state == Download.STATE_DOWNLOADING }
            .map { it.percentDownloaded.toInt().coerceIn(0, 100) }
            .average().takeIf { !it.isNaN() }?.toInt()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.download_channel_name))
            .setContentText(
                when {
                    active > 0 && percent != null -> "Downloading $active item(s) — $percent%"
                    downloads.isNotEmpty() -> "Waiting to download"
                    else -> "Downloads"
                }
            )
            .apply { if (percent != null) setProgress(100, percent, false) }
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
