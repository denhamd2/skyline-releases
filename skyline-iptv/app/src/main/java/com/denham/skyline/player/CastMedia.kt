package com.denham.skyline.player

import androidx.media3.common.C
import androidx.media3.common.MimeTypes

/**
 * What the default Chromecast receiver (CC1AD845) can be asked to load.
 *
 * Kept free of Android and ExoPlayer types so the decisions that can actually
 * be *wrong* are unit-testable on the JVM; what is left in PlayerManager is
 * wiring. Three invariants, each one a failure that looked like "casting is
 * just broken":
 *
 * 1. A cast load MUST carry a contentType. Media3's DefaultMediaItemConverter
 *    passes MediaItem.mimeType straight into MediaInfo.setContentType() and
 *    neither it nor the Cast SDK rejects null -- it only logs "Converting
 *    MediaItem with null MIME type". The receiver then has nothing to pick a
 *    pipeline from, so the cast dies silently with no error anywhere.
 * 2. Live channels cast as HLS, never as the TS variant. The receiver is
 *    Chrome-based; MP2T support on Cast comes from HLS segment transmuxing,
 *    so a progressive .ts URL is not something we can name a content type for
 *    that the receiver will understand. HLS is the one live variant we can.
 * 3. Live casts start at the default position. The local position is an offset
 *    into the live window -- often tens of minutes -- and means nothing to the
 *    receiver.
 */
object CastMedia {

    /** Shown when the receiver cannot play a container at all. */
    const val UNSUPPORTED_MESSAGE: String =
        "This can't be sent to a Chromecast — it only plays MP4/WebM video and " +
            "HLS streams. Still playing here."

    /** Extension of a stream URL, query and fragment stripped. "" if none. */
    fun extensionOf(url: String): String =
        url.substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('/')
            .substringAfterLast('.', "")
            .lowercase()

    /** Content type for [url], or null when the default receiver can't play it. */
    fun mimeTypeFor(url: String): String? = when (extensionOf(url)) {
        "m3u8" -> MimeTypes.APPLICATION_M3U8
        "mp4", "m4v", "mov" -> MimeTypes.VIDEO_MP4
        "webm" -> MimeTypes.VIDEO_WEBM
        "mp3" -> MimeTypes.AUDIO_MPEG
        // mkv, avi, ts, flv, wmv and extensionless (timeshift) are unsupported.
        else -> null
    }

    /** A load the receiver will accept. */
    data class Plan(val url: String, val mimeType: String, val startPositionMs: Long)

    /**
     * The load to send to the receiver, or null when this stream cannot be cast
     * at all -- the caller must then stay local and say so rather than handing
     * the receiver something it will drop.
     */
    fun plan(url: String, castUrl: String?, isLive: Boolean, localPositionMs: Long): Plan? {
        val target = if (isLive) castUrl ?: return null else url
        val mimeType = mimeTypeFor(target) ?: return null
        return Plan(
            url = target,
            mimeType = mimeType,
            startPositionMs = if (isLive) C.TIME_UNSET else localPositionMs,
        )
    }
}
