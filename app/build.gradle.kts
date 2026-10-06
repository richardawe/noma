plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
providers.gradleProperty("nomaBuildRoot").orNull?.let { layout.buildDirectory.set(file("$it/app")) }
android {
    namespace = "org.noma"
    compileSdk = 35
    ndkVersion = "27.2.12479018"
    externalNativeBuild { ndkBuild { path = file("../vendor/hev-socks5-tunnel/Android.mk") } }
    defaultConfig {
        applicationId = "org.noma"
        minSdk = 29
        targetSdk = 35
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        externalNativeBuild { ndkBuild { targets += "hev-socks5-tunnel"; arguments += listOf("NDK_APPLICATION_MK=${rootDir}/vendor/hev-socks5-tunnel/Application.mk", "REV_ID=2.17.1") } }
        versionCode = 7
        versionName = "0.4.2-evidence-scene"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
