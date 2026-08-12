# Skyline — Sky-inspired IPTV player (Android)

A personal, sideloaded Android phone app that plays your own **Xtream Codes**
IPTV subscription with a Sky-Glass-inspired look: dark navy canvas, hero
spotlight, horizontal rails, Sky-blue accents, Manrope typography.

**Skyline ships with no channels.** You sign in with the server/username/
password from your own IPTV provider; the app is a neutral player, exactly
like VLC or Televizo.

## Feature set (v1)

- **Sign in** with any Xtream portal (`http://host:port`, https and custom
  ports supported; you can paste the full URL from your provider e-mail).
- **Live TV**: categories, favourites, now/next EPG on each channel, fast
  channel zapping (prev/next in category from the player).
- **Player**: Media3/ExoPlayer with the NextLib **FFmpeg** extension, so
  AC3/EAC3/DTS/MP2 audio plays even when the phone lacks the hardware
  decoder (the #1 IPTV pain). Automatic retry with backoff, then automatic
  TS↔HLS container fallback. Clear error messages for provider HTTP codes
  (401 auth, 403 IP block, 406 not in bouquet, connection cap).
- **Films & Series**: poster grids by category, detail pages (plot, cast,
  rating), season/episode lists, playback via each item's
  `container_extension`.
- **Search**: full-text (FTS4) across channels, films and series.
- **Home**: hero spotlight + rails (Favourites, Live TV, New films, New
  series), shimmer loading states.
- **PiP** (press Home while watching), background audio via MediaSession,
  landscape fullscreen player.
- **Settings**: account status/expiry/connections, TS/HLS preference,
  configurable User-Agent (some providers filter players by UA), library
  refresh, sign out.

## Architecture

Kotlin + Jetpack Compose (Material 3, always-dark custom scheme), MVVM with
plain `StateFlow`, manual DI (`di/AppContainer.kt`), single activity.

| Layer | What's in it |
|---|---|
| `core/` | Pure Kotlin, no Android imports: Xtream DTOs with defensive serializers (0/1/"true" booleans, string-or-number ids, `info: []` tolerance), stream-URL builder, Base64 EPG decoding, HTTP error mapping, credential redaction. Unit-tested on the JVM. |
| `data/api` | Retrofit + OkHttp; credentials appended by interceptor; UA read live from Settings on every request. |
| `data/db` | Room: categories, channels, movies, series (+FTS4 mirrors), favourites, now/next EPG cache. Paging 3 with bounded memory for 10k+ channel providers. |
| `data/repo` | Auth; catalogue sync (streaming JSON parse via `decodeToSequence`, chunked 500-row transactions); short-EPG cache with TTL. |
| `player/` | One shared ExoPlayer (fast zapping), NextLib FFmpeg renderers, tuned LoadControl (~1s start buffer), `MediaSessionService`. |
| `ui/` | Compose screens: Login, Home, Live, Films, Series, Search, Detail, Player, Settings. |

## Build & install

Requirements: Android Studio (or Android SDK platform 36), JDK 17+.
Device: Android 8.0+ (minSdk 26; PiP needs 26+).

```sh
cd skyline-iptv
./gradlew assembleDebug          # APK at app/build/outputs/apk/debug/
./gradlew testDebugUnitTest      # JVM tests for the core layer
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open the `skyline-iptv` folder in Android Studio and press Run.

> Version pins: everything resolvable from Maven Central (Kotlin 2.2.21,
> KSP, kotlinx-serialization, Retrofit 3, OkHttp, Coil, NextLib
> 1.10.1-0.13.0) was verified against the repository. Google-hosted
> artifacts (AGP 8.11.1, Compose BOM 2025.06.00, Media3 1.10.1, Room,
> Paging, DataStore) are pinned to known releases; if Gradle sync flags one,
> accept Android Studio's suggested patch version — nothing in the code
> depends on exotic APIs.

### First-run checklist (on your phone)

1. Sign in with your real portal URL + credentials — expect your account
   status on the Settings screen afterwards.
2. Play a `.ts` live channel; confirm video **and audio**.
3. Specifically test a channel with **AC3/Dolby audio** (sports/movie
   channels often are) — this exercises the FFmpeg decoder path.
4. Rotate to landscape; press Home while playing → PiP.
5. Start playback on a second device to see the connection-cap error
   message handled gracefully.
6. If a provider blocks streams, try the other container (Settings →
   TS/HLS) and/or change the User-Agent.

## Security notes

- Credentials are stored in **EncryptedSharedPreferences** (AES-256,
  Android Keystore).
- The Xtream protocol itself puts credentials in the **query string of
  every request** — that's a protocol limitation no client can fix. Skyline
  never logs URLs unredacted (`core/Redact.kt`), but be aware plain-HTTP
  providers expose credentials on the network path.
- Cleartext HTTP is enabled because many providers only serve HTTP on
  custom ports; the user supplies the URL.

## Legal

Skyline is a generic player for content **you** are entitled to access. It
bundles no channels, playlists or provider endpoints. The UI is
Sky-*inspired* (rail/hero layout, navy + blue palette) but uses its own
name, icon and the open-licence **Manrope** typeface — not Sky's marks or
proprietary Sky Text font. Built for personal sideloading; it is not
distributed on any app store. See `THIRD_PARTY_LICENSES.md`.
