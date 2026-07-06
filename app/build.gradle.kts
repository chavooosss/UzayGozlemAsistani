plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.uzaygozlem.asistan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uzaygozlem.asistan"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            // Kişisel kullanım: debug anahtarıyla imzala, R8 ile optimize et.
            // Compose release modda debug'a göre belirgin şekilde akıcıdır.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
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
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.glance:glance-appwidget:1.1.1")
    // commons-logging'in çalışma zamanı log keşfi Android'de (özellikle R8
    // sonrası) çöküyor; hariç tutulup app içinde mini stub ile karşılanıyor.
    implementation("com.github.davidmoten:predict4java:1.3.1") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
