plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/** Set these to produce a signed release build; leave unset and the release is unsigned. */
val releaseKeystorePath: String? = System.getenv("GRUNCHY_KEYSTORE")

android {
    namespace = "com.grunchy.workout"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.grunchy.workout"
        // Mudita Kompakt runs MuditaOS K, an AOSP based system without Google services.
        // 26 is the floor that gives us java.time without desugaring.
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Personal app on a phone with 8 system languages: ship English only.
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        // Optional release signing, driven entirely by environment variables (see README).
        // Nothing secret lives in the project, and a plain clone still builds.
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("GRUNCHY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("GRUNCHY_KEY_ALIAS")
                keyPassword = System.getenv("GRUNCHY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            isMinifyEnabled = false
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // E Ink optimised component library (Mudita Mindful Design).
    implementation(libs.mudita.mmd)

    implementation(libs.kotlinx.serialization.json)

    // Progress charts. compose-m3 supplies the Material 3 theming module; the chart itself is
    // configured black-on-white by hand, so nothing of Vico's default palette reaches the panel.
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
