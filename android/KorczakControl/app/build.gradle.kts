plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.korczak.control"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.korczak.control"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("VERSION_CODE") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("VERSION_NAME") as String?) ?: "0.3.0"
        val apiUrl = (project.findProperty("CONTROL_API_URL") as String?) ?: ""
        buildConfigField("String", "CONTROL_API_URL", "\"${apiUrl.trim().trimEnd('/')}\"")
    }

    val releaseStoreFile = System.getenv("ANDROID_KEYSTORE_FILE")
    val releaseAlias = System.getenv("ANDROID_KEY_ALIAS")
    val releaseStorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")

    signingConfigs {
        if (!releaseStoreFile.isNullOrBlank() && !releaseAlias.isNullOrBlank() && !releaseStorePassword.isNullOrBlank() && !releaseKeyPassword.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
