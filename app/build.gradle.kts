import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.plugin.serialization)
}

val radioPropertiesFile = file("radio.properties")
check(radioPropertiesFile.isFile) {
    "Missing app/radio.properties. Copy app/radio.properties.example and set radio.ip."
}
val radioProperties = Properties().apply {
    radioPropertiesFile.inputStream().use { load(it) }
}
val radioIp = radioProperties.getProperty("radio.ip")
    ?: error("Missing radio.ip in app/radio.properties")

android {
    namespace = "pl.krzysztofwojciechowski.yamaharadio"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pl.krzysztofwojciechowski.yamaharadio"
        minSdk = 31
        targetSdk = 37
        versionCode = 2
        versionName = "2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "RADIO_IP", "\"$radioIp\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)

    // Retrofit
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
