package com.denham.skyline.core

/**
 * Xtream puts the username and password in the query string of every API call
 * and in the path of every stream URL. Anything that logs a URL MUST pass it
 * through here first, so credentials never reach Logcat or crash reports.
 */
object Redact {

    private val QUERY_CREDS = Regex("(?i)((?:username|password)=)[^&\\s]*")
    private val PATH_CREDS = Regex("(?i)(/(?:live|movie|series|timeshift)/)[^/]+/[^/]+/")

    fun url(url: String): String = url
        .replace(QUERY_CREDS) { "${it.groupValues[1]}•••" }
        .replace(PATH_CREDS) { "${it.groupValues[1]}•••/•••/" }
}
