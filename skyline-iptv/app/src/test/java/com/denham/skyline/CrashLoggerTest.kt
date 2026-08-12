package com.denham.skyline

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the "is this crash still relevant" decision.
 *
 * The bug this exists for: the crash log is only cleared by tapping
 * "Dismiss and try again" and survives app updates, so a crash that an
 * update had already fixed kept greeting the user on every launch of the
 * fixed build -- and because the build number is baked into the trace when
 * it is written, it kept reporting the old version too, looking exactly
 * like the fix had failed.
 */
class CrashLoggerTest {

    private fun trace(version: String) =
        "Skyline build: $version (10 Aug 2026, 13:12 UTC)\n" +
            "Android 36 on HONOR NLA-NX1\n\n" +
            "java.lang.IllegalArgumentException: background can not be translucent: #0\n" +
            "  at androidx.core.graphics.ColorUtils.calculateContrast(ColorUtils.java:175)\n"

    @Test
    fun `crash from an older build is not from the current one`() {
        assertTrue(CrashLogger.isFromOtherBuild(trace("1.1.83"), currentVersion = "1.1.86"))
    }

    @Test
    fun `crash from the running build is kept`() {
        assertFalse(CrashLogger.isFromOtherBuild(trace("1.1.86"), currentVersion = "1.1.86"))
    }

    @Test
    fun `a newer recorded build also counts as other`() {
        // Downgrades are possible via the public releases repo, and a trace
        // from a build that is not the one running is stale either way.
        assertTrue(CrashLogger.isFromOtherBuild(trace("1.1.90"), currentVersion = "1.1.86"))
    }

    @Test
    fun `unparseable trace is treated as current and still shown`() {
        // Better a stale report than silently swallowing a real one.
        val noHeader = "java.lang.NullPointerException\n  at com.denham.skyline.Foo.bar(Foo.kt:1)\n"
        assertFalse(CrashLogger.isFromOtherBuild(noHeader, currentVersion = "1.1.86"))
    }

    @Test
    fun `local builds report the local version name`() {
        // versionName is "1.1.local" when GITHUB_RUN_NUMBER is absent, so a
        // sideloaded local build must not inherit a CI build's crash.
        assertTrue(CrashLogger.isFromOtherBuild(trace("1.1.86"), currentVersion = "1.1.local"))
    }
}
