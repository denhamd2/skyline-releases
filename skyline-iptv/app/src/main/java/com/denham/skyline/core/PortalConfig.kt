package com.denham.skyline.core

/**
 * Where an Xtream portal lives. Scheme, host and port are stored separately —
 * providers use arbitrary ports (80/443/25461/8080/…) and mixing them up is a
 * classic source of "works in one app, not another" bugs.
 */
data class PortalAddress(
    val scheme: String,
    val host: String,
    val port: Int,
) {
    val baseUrl: String get() = "$scheme://$host:$port"

    companion object {
        /**
         * Parses whatever the user typed into the server field:
         * "example.com:8080", "http://example.com", "https://example.com:25463/",
         * even a full player_api URL pasted from the provider e-mail.
         * Returns null only when no host can be extracted.
         */
        fun parse(raw: String): PortalAddress? {
            var s = raw.trim()
            if (s.isEmpty()) return null

            val scheme: String
            when {
                s.startsWith("https://", ignoreCase = true) -> {
                    scheme = "https"; s = s.substring(8)
                }
                s.startsWith("http://", ignoreCase = true) -> {
                    scheme = "http"; s = s.substring(7)
                }
                else -> scheme = "http"
            }

            // Drop any path/query the user pasted along (e.g. /player_api.php?...)
            s = s.substringBefore('/').substringBefore('?')
            if (s.isEmpty()) return null

            val host: String
            val port: Int
            val colon = s.lastIndexOf(':')
            if (colon > 0 && colon < s.length - 1 && s.substring(colon + 1).all { it.isDigit() }) {
                host = s.substring(0, colon)
                port = s.substring(colon + 1).toIntOrNull() ?: return null
            } else {
                host = s.substringBefore(':')
                port = if (scheme == "https") 443 else 80
            }
            if (host.isEmpty() || port !in 1..65535) return null
            return PortalAddress(scheme, host, port)
        }
    }
}

/** A full Xtream account: portal + credentials. */
data class XtreamAccount(
    val portal: PortalAddress,
    val username: String,
    val password: String,
)
