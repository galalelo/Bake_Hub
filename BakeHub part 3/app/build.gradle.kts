import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

// Release signing: read from keystore.properties (git-ignored) or environment
// variables (used by CI). If neither is present the release build is simply
// left unsigned, so contributors can still build without the upload key.
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? =
    keystoreProps.getProperty(key) ?: System.getenv(env)

val hasReleaseKey = signingValue("storeFile", "BAKEHUB_STORE_FILE") != null

android {
    namespace = "com.bakehub.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bakehub.app"
        minSdk = 26
        targetSdk = 36          // Google Play requires API 36 for new apps/updates since 31 Aug 2026
        versionCode = 3
        versionName = "1.0.0"
    }

    // Only ship the languages BakeHub is translated into (see res/values-*).
    androidResources {
        localeFilters += listOf("en", "es", "fr", "ar")
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = rootProject.file(signingValue("storeFile", "BAKEHUB_STORE_FILE")!!)
                storePassword = signingValue("storePassword", "BAKEHUB_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "BAKEHUB_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "BAKEHUB_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        abortOnError = false      // report issues without blocking CI; see lint-results in CI artifacts
        checkReleaseBuilds = false
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")   // real-time push notifications

    // Google Sign-In (SSO) through Credential Manager -> Firebase Auth
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // TheMealDB API connection
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Recipe images for XML ImageViews
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Unit tests (JVM)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
