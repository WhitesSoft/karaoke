import org.gradle.kotlin.dsl.coreLibraryDesugaring

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.gvtlaiko.tengokaraoke"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gvtlaiko.tengokaraoke"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {

        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.okhttp3)
    implementation(libs.retrofit.okhttp)

    implementation(libs.coroutines)
    implementation(libs.androidx.lifecycle)

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    implementation(libs.android.youtube)

    implementation(libs.chromecast)

    implementation(libs.newpipe)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

}