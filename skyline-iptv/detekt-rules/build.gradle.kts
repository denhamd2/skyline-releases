// Custom detekt rules enforcing the Sky design system.
//
// A plain JVM module, deliberately not an Android one: this is a library
// loaded by the detekt tool itself, not code that ships in the app. The app
// consumes it via `detektPlugins(project(":detekt-rules"))`.
//
// The rules previously lived at gradle/detekt/ referenced by nothing, so they
// were never compiled and never ran.

plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // compileOnly: detekt supplies its own API at runtime. Bundling it would
    // risk two copies on the tool's classpath.
    compileOnly(libs.detekt.api)
}

// Match the app's Java target rather than declaring a toolchain: CI runs a
// single JDK (21) and a toolchain request for a different JDK would need
// provisioning that isn't set up here.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
