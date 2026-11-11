plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.protobuf")
}

android {
    namespace = "com.pj95.clockwise.core.data"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.5"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:model"))
    api(project(":core:common"))

    implementation("androidx.core:core-ktx:1.13.1")

    // Room is the queryable source of truth for the city catalog, alarms,
    // timer, solar cache and Studio licences.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Proto DataStore holds user preferences; the generated `SavedCities` type
    // is part of the repository API, so the runtime is `api`.
    api("androidx.datastore:datastore:1.1.1")
    api("com.google.protobuf:protobuf-javalite:3.25.5")

    // Play Billing is the only dependency that talks to a Google service, and
    // only for the one-time Studio unlock. It is part of the public API of
    // `StudioBilling`, so the composition root sees the types it returns.
    api("com.android.billingclient:billing-ktx:7.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
