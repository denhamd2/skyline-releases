package com.denham.skyline.core

/**
 * The Xtream API never returns playable URLs — clients construct them from
 * well-known path patterns. All patterns per the community API reference
 * (worldofiptvcom/xtream-codes-api-documentation).
 */
class StreamUrlBuilder(private val account: XtreamAccount) {

    private val base get() = account.portal.baseUrl
    private val creds get() = "${account.username}/${account.password}"

    /** Live: `/live/{user}/{pass}/{stream_id}.{ts|m3u8}` */
    fun live(streamId: Int, container: LiveContainer): String =
        "$base/live/$creds/$streamId.${container.extension}"

    /** VOD: `/movie/{user}/{pass}/{stream_id}.{container_extension}` */
    fun movie(streamId: Int, containerExtension: String): String =
        "$base/movie/$creds/$streamId.${containerExtension.ifBlank { "mp4" }}"

    /** Series episode: `/series/{user}/{pass}/{episode_id}.{container_extension}` */
    fun seriesEpisode(episodeId: Int, containerExtension: String): String =
        "$base/series/$creds/$episodeId.${containerExtension.ifBlank { "mp4" }}"

    /** Catch-up: `/timeshift/{user}/{pass}/{duration_mins}/{start}/{stream_id}` */
    fun timeshift(streamId: Int, durationMinutes: Int, start: String): String =
        "$base/timeshift/$creds/$durationMinutes/$start/$streamId"
}

enum class LiveContainer(val extension: String) {
    TS("ts"),
    HLS("m3u8");

    fun other(): LiveContainer = if (this == TS) HLS else TS

    companion object {
        /**
         * Picks the preferred live container, honouring the account's
         * `allowed_output_formats` when the server provides it.
         */
        fun pick(allowedFormats: List<String>, preferred: LiveContainer): LiveContainer {
            if (allowedFormats.isEmpty()) return preferred
            val allowed = allowedFormats.map { it.lowercase().trim() }
            return when {
                preferred.extension in allowed -> preferred
                preferred.other().extension in allowed -> preferred.other()
                else -> preferred
            }
        }
    }
}
