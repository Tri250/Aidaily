plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.livecompose.livecapture"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.livecompose.livecapture"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Bugly AppID（需在 bugly.qq.com 注册获取，通过 -PBUGLY_APP_ID=xxx 传入）
        buildConfigField("String", "BUGLY_APP_ID", "\"${project.findProperty("BUGLY_APP_ID") as? String ?: ""}\"")
        // 微信 AppID（需在 open.weixin.qq.com 注册获取，通过 -PWECHAT_APP_ID=xxx 传入）
        buildConfigField("String", "WECHAT_APP_ID", "\"${project.findProperty("WECHAT_APP_ID") as? String ?: ""}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/release.jks")
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as? String
                ?: System.getenv("RELEASE_STORE_PASSWORD")
                ?: ""
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as? String
                ?: System.getenv("RELEASE_KEY_ALIAS")
                ?: "livecapture"
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as? String
                ?: System.getenv("RELEASE_KEY_PASSWORD")
                ?: ""
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            isCrunchPngs = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
        }
    }

    flavorDimensions += "channel"
    productFlavors {
        create("huawei") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"huawei\"")
        }
        create("xiaomi") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"xiaomi\"")
        }
        create("oppo") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"oppo\"")
        }
        create("vivo") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"vivo\"")
        }
        create("tencent") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"tencent\"")
        }
        create("official") {
            dimension = "channel"
            buildConfigField("String", "CHANNEL", "\"official\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.2")

    // CameraX
    val cameraxVersion = "1.4.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ML Kit
    implementation("com.google.mlkit:face-detection:16.1.6")
    implementation("com.google.mlkit:object-detection:17.0.1")
    implementation("com.google.mlkit:image-labeling:17.0.7")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.16.1")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ExifInterface
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Bugly 崩溃上报（国内合规）
    implementation("com.tencent.bugly:crashreport:4.1.9.3")
    implementation("com.tencent.bugly:nativecrashreport:3.9.1")

    // 微信分享 SDK
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.0")

    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
