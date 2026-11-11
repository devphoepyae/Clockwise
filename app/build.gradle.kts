import org.gradle.api.tasks.testing.Test
import java.util.Properties

/**
 * Clockwise's version sequence, as specified:
 *
 *   v1.0.0 … v1.0.100, v1.1.0 … v1.1.100, … v1.100.0, v2.0.0 …
 *
 * Each minor line carries 101 patch releases (0..100), except the terminal
 * `x.100.0` line, which carries only patch 0 before the next major. The code is
 * therefore the release's zero-based position in that sequence, so it is
 * strictly increasing and independent of how the name is parsed elsewhere.
 *
 * `v1.0.0` is the first release. Android and Play require a positive version
 * code, so the zero-based sequence position is shifted by one: v1.0.0 maps to
 * 1, v1.0.1 to 2, … v1.0.100 to 101, v1.1.0 to 102, and v2.0.0 to 10_102.
 */
fun clockwiseVersionCode(versionName: String): Int {
    val parts = versionName.trim().removePrefix("v").split('.')
    require(parts.size == 3) { "versionName must be MAJOR.MINOR.PATCH: $versionName" }
    val major = parts[0].toIntOrNull()
    val minor = parts[1].toIntOrNull()
    val patch = parts[2].toIntOrNull()
    require(major != null && major >= 1) { "major must be >= 1: $versionName" }
    require(minor != null && minor in 0..100) { "minor must be 0..100: $versionName" }
    require(patch != null && patch in 0..100) { "patch must be 0..100: $versionName" }
    require(minor != 100 || patch == 0) { "x.100.0 is the last release of a major: $versionName" }
    val position = (major - 1) * 10_101 + if (minor == 100) 10_100 else minor * 101 + patch
    return position + 1
}

val clockwiseVersionName = "1.0.10"

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Release signing. Credentials live in `keystore.properties` (git-ignored); when
// that file is absent — a fresh checkout without the keystore, for example —
// the release build simply falls back to being unsigned.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.pj95.clockwise"
    // Glance 1.2.0 requires compileSdk 35. targetSdk stays at 34 for this
    // release: compileSdk is a build-time API surface, not a runtime policy.
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.pj95.clockwise"
        minSdk = 26
        targetSdk = 34
        versionCode = clockwiseVersionCode(clockwiseVersionName)
        versionName = clockwiseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Strict R8: shrink code and resources so no unused library class,
            // metadata or drawable reaches the shipped APK.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    }

    testOptions {
        unitTests {
            // Robolectric renders real resources (fonts, vectors) in unit tests.
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        // `MigrationTestHelper` reads the exported schemas from the assets of the
        // instrumentation context. Under Robolectric that is the app context, so
        // debug builds carry the folder as assets; the release APK never does.
        // The schemas themselves are owned by `:core:data`.
        getByName("debug").assets.srcDir("$rootDir/core/data/schemas")
        getByName("androidTest").assets.srcDir("$rootDir/core/data/schemas")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Roborazzi writes screenshots when recording is enabled. Keeping it on for all
// test tasks means `testDebugUnitTest` refreshes the layout snapshots.
tasks.withType<Test>().configureEach {
    systemProperty("roborazzi.test.record", "true")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:alarm"))
    implementation(project(":feature:map"))
    implementation(project(":feature:timer"))

    // Compose BOM keeps all Compose / Material 3 artifacts on one aligned
    // version. 2025.02.00 maps to Compose 1.7.8, the runtime Glance 1.2.0
    // compiles against.
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.navigation:navigation-compose:2.8.4")

    // The composition root still builds the DataStore and the Room database, so
    // it needs their runtime types on its own compile classpath.
    implementation("androidx.datastore:datastore:1.1.1")
    implementation("com.google.protobuf:protobuf-javalite:3.25.5")
    implementation("androidx.room:room-runtime:2.6.1")

    // Home-screen widgets are hosted by the app module: the receivers, the
    // Glance designs and their configuration activity all live here.
    implementation("androidx.glance:glance-appwidget:1.2.0")
    implementation("androidx.glance:glance-material3:1.2.0")

    // Tooling (debug only).
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")

    // JVM screenshot tests: renders the real Compose UI at a fixed device size
    // without needing an emulator.
    testImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.26.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.26.0")
    testImplementation("androidx.room:room-runtime:2.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("androidx.datastore:datastore:1.1.1")
    testImplementation("com.google.protobuf:protobuf-javalite:3.25.5")
    testImplementation("androidx.glance:glance-appwidget-testing:1.2.0")

    // On-device smoke test, run with `connectedDebugAndroidTest`. Deliberately
    // minimal: everything assertable on the JVM is already covered above, and
    // interaction tests are flaky across OEM animation settings.
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
