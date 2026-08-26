import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

/**
 * Release signing comes from environment variables in CI, or a local
 * `keystore.properties` that is never committed. When neither is present the
 * release build falls back to the debug key so a plain `assembleRelease` still
 * works for local testing.
 */
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

// A skipped CI step still exports its output as an empty string rather than
// leaving the variable unset, so blank has to count as absent here — `file("")`
// throws rather than returning something non-existent.
fun secret(key: String, envName: String): String? =
    (keystoreProps.getProperty(key) ?: System.getenv(envName))?.takeIf { it.isNotBlank() }

val storeFilePath = secret("storeFile", "KEYSTORE_FILE")
val hasReleaseKey = storeFilePath != null && file(storeFilePath).exists()

// Overridable from CI so a release can be cut from the Actions page without
// editing this file: -PversionName=1.1 -PversionCode=7
val appVersionName = (project.findProperty("versionName") as String?)
    ?.takeIf { it.isNotBlank() } ?: "1.0"
val appVersionCode = (project.findProperty("versionCode") as String?)
    ?.trim()?.toIntOrNull() ?: 1

android {
    namespace = "ir.tivan.controller"
    compileSdk = 34

    defaultConfig {
        applicationId = "ir.tivan.controller"
        minSdk = 24
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
        resourceConfigurations += listOf("fa", "en")

        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("release") {
                storeFile = file(storeFilePath!!)
                storePassword = secret("storePassword", "KEYSTORE_PASSWORD")
                keyAlias = secret("keyAlias", "KEY_ALIAS")
                keyPassword = secret("keyPassword", "KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // R8 shrinks the APK and strips unused Compose/Room code, which
            // also keeps the app light on low-end devices.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseKey) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
