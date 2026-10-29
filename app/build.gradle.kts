plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zixora.calories"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.zixora.calories"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        // Stable test signing key (CI secrets) so every build shares one signature
        // and installs as an update over previous builds. Local builds without the
        // secrets fall back to the default debug key.
        create("calorieTest") {
            storeFile = file("calorie-test.p12")
            storeType = "PKCS12"
            storePassword = System.getenv("CALORIE_STORE_PASSWORD")
            keyAlias = System.getenv("CALORIE_KEY_ALIAS") ?: "calorie-test"
            keyPassword = System.getenv("CALORIE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("calorieTest")
        }
        release {
            isMinifyEnabled = false
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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
