import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.denham.skyline"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.denham.skyline"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        // Include the CI run number so the version shown in Android's App info
        // screen differs per build. With a static "1.1" there was no way to
        // confirm on-device whether a freshly downloaded APK had actually been
        // installed, which made diagnosing "it still does X" ambiguous.
        val buildNumber = System.getenv("GITHUB_RUN_NUMBER") ?: "local"
        versionName = "1.1.$buildNumber"

        // Stamped when CI configures the build so the app can show exactly which
        // build is installed (surfaced in Settings). UTC keeps it unambiguous.
        val buildTime = SimpleDateFormat("d MMM yyyy, HH:mm 'UTC'").apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")

        // Injected by CI from the YOUTUBE_API_KEY secret; empty in local builds.
        // Only enables channel/playlist search -- the RSS feeds that actually
        // fetch videos need no key, so an absent key degrades to "paste a URL"
        // rather than breaking subscriptions. Restrict it to the YouTube Data
        // API: builds are published publicly, so anything compiled in is
        // extractable, and the exposure should be capped at quota use.
        val youtubeApiKey = System.getenv("YOUTUBE_API_KEY").orEmpty()
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")

        // Injected by CI from the FOOTBALL_DATA_API_KEY secret; empty in local
        // builds. Powers the David-only "Football" section on Home
        // (football-data.org v4). Restricted to that one API, same exposure
        // model as the YouTube key above -- an absent key degrades to the
        // section simply not rendering, never an error card.
        val footballDataApiKey = System.getenv("FOOTBALL_DATA_API_KEY").orEmpty()
        buildConfigField("String", "FOOTBALL_DATA_API_KEY", "\"$footballDataApiKey\"")
    }

    signingConfigs {
        // A stable, checked-in debug key. Without this, the default debug
        // signing config auto-generates a fresh random key on any machine
        // (or CI runner) that doesn't already have ~/.android/debug.keystore
        // -- since GitHub Actions runners are ephemeral, every build would
        // get a different signature, and Android refuses to install an
        // "update" signed with a different key than what's already on the
        // device ("package conflicts with an existing package"). Pinning
        // this keystore is what makes the in-app updater able to install
        // over itself at all.
        getByName("debug") {
            storeFile = file("../keystore/skyline-debug.keystore")
            storePassword = "skylinedebug123"
            keyAlias = "skylinedebug"
            keyPassword = "skylinedebug123"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Media3's UnstableApi opt-in is annotated where used; don't let the
        // transitive-usage lint check block builds.
        disable += "UnsafeOptInUsageError"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

detekt {
    // setFrom rather than assignment: the setter is deprecated and slated for
    // removal. It is only a warning, but the Kotlin DSL prints warnings inside
    // the same "Script compilation errors" block as real errors, so leaving it
    // made a single build break read as three. Behaviour is identical.
    config.setFrom(files("../detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
}

// The design-system gate, separate from the broad `detekt` task above so that
// it can run at zero tolerance without being entangled with the ~393
// pre-existing built-in findings. See detekt-design-system.yml for why.
//
// buildUponDefaultConfig is off: the config names every ruleset it wants and
// switches the rest off, so nothing depends on what an unlisted ruleset would
// default to. The markdown report is what CI copies into the run summary, so a
// failure states the proposed fix on the run page rather than only in the log.
tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektDesignSystem") {
    description = "Fails on any Sky design-system violation."
    group = "verification"

    config.setFrom(files("../detekt-design-system.yml"))
    // Plain assignment, not `.set(...)`: on the Detekt *task* type this is a
    // Boolean, not a Gradle Property. `.set(false)` does not resolve, and
    // Kotlin then reports it against StringBuilder's `set` operator -- an
    // error message that names neither detekt nor this line's real problem.
    // It failed script compilation, so *every* Gradle task in the project
    // failed and no APK was produced.
    buildUponDefaultConfig = false

    setSource(files("src/main/java"))
    include("**/*.kt")

    reports {
        md.required.set(true)
        html.required.set(false)
        xml.required.set(false)
        sarif.required.set(false)
        txt.required.set(false)
    }
}

dependencies {
    // Registers the Sky design-system rules with detekt. Without this the
    // rules compile but never run, which is what left detekt.yml referencing
    // a ruleset detekt could not load.
    detektPlugins(project(":detekt-rules"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)
    // FFmpeg software decoders (AC3/EAC3/DTS/MP2 audio) for streams the
    // device's hardware MediaCodec cannot handle.
    implementation(libs.nextlib.media3ext)
    // Chromecast: cast live/VOD to Chromecast / Chromecast-built-in / Google TV.
    implementation(libs.media3.cast)
    implementation(libs.androidx.mediarouter)
    implementation(libs.play.services.cast.framework)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.appcompat)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Screenshot + click tests: run the UI on the JVM (Robolectric) and
    // capture PNGs (Roborazzi) that CI publishes for visual review.
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
