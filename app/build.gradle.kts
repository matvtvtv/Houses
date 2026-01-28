plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.example.houses"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.houses"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
        // desugaring + Java 11 compatibility
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.hilt.android)
    implementation(libs.lombok)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)
    implementation(libs.viewpager2)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.flexbox)

    // core library desugaring dependency (правильный вызов)
    coreLibraryDesugaring(libs.coreLibraryDesugaring)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // annotation processors (если проект на Java)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.hilt.compiler)
    implementation("com.google.android.material:material:1.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.lombok)
    implementation(libs.flexbox)
    implementation(libs.work)
    implementation(libs.okhttp)
    implementation(libs.retrofit)

    // Lombok
    annotationProcessor(libs.lombok)

    // JUnit для юнит-тестов
    testImplementation(libs.junit)

    // Android Instrumented Tests
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
