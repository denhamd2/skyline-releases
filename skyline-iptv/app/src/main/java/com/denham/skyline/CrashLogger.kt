package com.denham.skyline

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * On-device crash log, for diagnosing startup crashes without a computer/adb.
 *
 * Two destinations, because they cover different failure windows:
 *  - internal filesDir: read back by MainActivity to show the crash on screen.
 *  - public Downloads:  the only one that helps when the crash happens before
 *    any Activity can start (e.g. during ContentProvider init, which Android
 *    runs before Application.onCreate) -- in that case no in-app screen is
 *    ever reachable, so the file has to be openable from the Files app.
 */
object CrashLogger {
    private const val FILE_NAME = "last_crash.txt"
    private const val PUBLIC_FILE_NAME = "skyline-crash.txt"

    /** Matches the header written by [stackTraceOf]. */
    private val BUILD_LINE = Regex("""^Skyline build:\s*(\S+)""")

    /**
     * Install from Application.attachBaseContext, not onCreate: ContentProviders
     * are initialised between the two, so a handler installed in onCreate misses
     * any provider-init crash entirely.
     */
    fun install(context: Context) {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { persist(context, stackTraceOf(throwable)) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    /** Persist a caught throwable that would otherwise not reach the handler. */
    fun record(context: Context, throwable: Throwable) {
        runCatching { persist(context, stackTraceOf(throwable)) }
    }

    fun readLastCrash(context: Context): String? = runCatching {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return@runCatching null
        val text = file.readText()
        if (isFromOtherBuild(text, BuildConfig.VERSION_NAME)) {
            file.delete()
            return@runCatching null
        }
        text
    }.getOrNull()

    /**
     * True when the stored crash was recorded by a build other than the one
     * now running.
     *
     * The log is only cleared by tapping "Dismiss and try again", and it
     * survives app updates. So a crash fixed by an update kept greeting the
     * user on every launch of the *fixed* build, looking exactly like the
     * fix had not worked -- the version shown is baked into the trace when
     * it is written, so it kept reading as the old build too. A crash from a
     * build that is no longer installed is history, not a live fault, quite
     * possibly the reason the user updated in the first place.
     *
     * The copy in Downloads (skyline-crash.txt) is deliberately left alone,
     * so the trace is still retrievable after this clears the in-app one.
     *
     * An unparseable trace is treated as current and still shown: better a
     * stale report than silently swallowing a real one.
     */
    internal fun isFromOtherBuild(crashText: String, currentVersion: String): Boolean {
        val recorded = BUILD_LINE.find(crashText)?.groupValues?.get(1) ?: return false
        return recorded != currentVersion
    }

    fun clearLastCrash(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }

    private fun stackTraceOf(throwable: Throwable): String {
        val writer = StringWriter()
        writer.write("Skyline build: ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TIME})\n")
        writer.write("Android ${Build.VERSION.SDK_INT} on ${Build.MANUFACTURER} ${Build.MODEL}\n\n")
        throwable.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private fun persist(context: Context, text: String) {
        runCatching { File(context.filesDir, FILE_NAME).writeText(text) }
        runCatching { writeToDownloads(context, text) }
    }

    /**
     * Drops the trace into the public Downloads folder so it can be opened from
     * the Files/Downloads app with no cable and no permission prompt.
     */
    private fun writeToDownloads(context: Context, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, PUBLIC_FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return
            resolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
        } else {
            val dir = context.getExternalFilesDir(null) ?: return
            File(dir, PUBLIC_FILE_NAME).writeText(text)
        }
    }
}
