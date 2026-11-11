plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pj95.clockwise.core.ui"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 26
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
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:model"))
    api(project(":core:common"))

    // Compose is part of the module's public surface: every feature renders
    // these components, so the artifacts are `api`.
    api(platform("androidx.compose:compose-bom:2025.02.00"))
    api("androidx.compose.ui:ui")
    api("androidx.compose.ui:ui-graphics")
    api("androidx.compose.foundation:foundation")
    api("androidx.compose.material3:material3")
    api("androidx.compose.runtime:runtime")

    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.core:core-ktx:1.13.1")

    // This module owns the widget-provider XML, which references Glance's
    // initial loading layout. The dependency is required for resource linking
    // even though the widget composables themselves live in :app.
    implementation("androidx.glance:glance-appwidget:1.2.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
