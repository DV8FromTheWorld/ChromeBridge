plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "net.dv8tion.chromebridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.dv8tion.chromebridge"
        minSdk = 30
        targetSdk = 34
        versionCode = 4
        versionName = "1.3"
    }

    // Signing is opt-in and secrets are supplied only by the trusted release job.
    val releaseKeystore = providers.environmentVariable("CHROMEBRIDGE_KEYSTORE_PATH").orNull
    if (releaseKeystore != null) {
        signingConfigs.create("stableRelease") {
            storeFile = file(releaseKeystore)
            storePassword = providers.environmentVariable("CHROMEBRIDGE_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("CHROMEBRIDGE_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("CHROMEBRIDGE_KEY_PASSWORD").get()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("stableRelease")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
