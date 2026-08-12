package com.denham.skyline.core

/**
 * Maps Xtream's HTTP error semantics to messages a viewer can act on.
 * Codes per the community API reference: 401 auth, 403 IP/token mismatch,
 * 405 extension not allowed, 406 not in bouquet.
 */
object XtreamError {

    fun messageFor(httpCode: Int): String = when (httpCode) {
        401 -> "Sign-in failed. Check your username and password — or your subscription may have expired."
        403 -> "The provider refused the connection (IP or token mismatch). Try again in a minute; if it persists, contact your provider."
        404 -> "This stream wasn't found on the server."
        405 -> "This stream format isn't allowed on your account. Try switching between TS and HLS in Settings."
        406 -> "This channel isn't included in your subscription package."
        429 -> "Too many requests — the server is rate-limiting. Wait a moment and try again."
        in 500..599 -> "The provider's server had a problem (HTTP $httpCode). Try again shortly."
        else -> "Playback failed (HTTP $httpCode)."
    }

    /** Shown when a stream fails and the account is already at its connection cap. */
    const val CONNECTION_LIMIT =
        "Your account's connection limit is in use. Stop playback on other devices (or wait a moment) and try again."
}
