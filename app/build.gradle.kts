import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    id("kotlin-kapt")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

// OpenCV paths within the :opencv module
val opencvModuleDir = project(":opencv").projectDir.absolutePath.replace("\\", "/")
// Point to native/jni where OpenCVConfig.cmake is located
val effectiveOpencvJniDir = "$opencvModuleDir/native/jni"
val opencvLibsDir = "$opencvModuleDir/native/libs"

android {
    namespace = "com.example.contentanalyzer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.contentanalyzer"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "HUGGING_FACE_API_KEY", "\"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\"")
        buildConfigField("String", "GOOGLE_VISION_API_KEY", "\"YOUR_GOOGLE_VISION_API_KEY_HERE\"")

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                // Pass the directory containing OpenCVConfig.cmake
                arguments("-DOpenCV_DIR=$effectiveOpencvJniDir")
            }
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    sourceSets {
        getByName("main") {
            // Append instead of replacing to ensure externalNativeBuild output is included
            jniLibs.srcDirs(opencvLibsDir)
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "HUGGING_FACE_API_KEY", "\"\"")
            buildConfigField("String", "GOOGLE_VISION_API_KEY", "\"\"")
        }

        debug {
            isDebuggable = true
            buildConfigField("String", "HUGGING_FACE_API_KEY", "\"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\"")
            buildConfigField("String", "GOOGLE_VISION_API_KEY", "\"YOUR_GOOGLE_VISION_API_KEY_HERE\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // OpenCV - Use local module
    implementation(project(":opencv"))

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:image-labeling:17.0.7")

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)

    // Gson
    implementation(libs.gson)

    // Accompanist Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Camera support
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")
}
