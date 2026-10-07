import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

val supabasePublishableKey =
    System.getenv("SUPABASE_PUBLISHABLE_KEY")
        ?: localProperties.getProperty("SUPABASE_PUBLISHABLE_KEY")
        ?: "sb_publishable_mGcmejtGmoASLKFqWpSXLw_xGBeYTAA"

val kanisaKeystorePath =
    System.getenv("KANISA_KEYSTORE_PATH")
        ?: localProperties.getProperty("KANISA_KEYSTORE_PATH")

val kanisaKeystorePassword =
    System.getenv("KANISA_KEYSTORE_PASSWORD")
        ?: localProperties.getProperty("KANISA_KEYSTORE_PASSWORD")

val kanisaKeyAlias =
    System.getenv("KANISA_KEY_ALIAS")
        ?: localProperties.getProperty("KANISA_KEY_ALIAS")

val kanisaKeyPassword =
    System.getenv("KANISA_KEY_PASSWORD")
        ?: localProperties.getProperty("KANISA_KEY_PASSWORD")

android {
    namespace = "com.example.helloworld"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kfcc.mobile"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${supabasePublishableKey.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
    }

    signingConfigs {
        create("release") {
            if (!kanisaKeystorePath.isNullOrBlank()) {
                storeFile = file(kanisaKeystorePath)
            }
            if (!kanisaKeystorePassword.isNullOrBlank()) {
                storePassword = kanisaKeystorePassword
            }
            if (!kanisaKeyAlias.isNullOrBlank()) {
                keyAlias = kanisaKeyAlias
            }
            if (!kanisaKeyPassword.isNullOrBlank()) {
                keyPassword = kanisaKeyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.logging)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.youtube.player)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}