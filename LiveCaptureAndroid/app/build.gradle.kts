plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.livecompose.livecapture"
    compileSdk = 35

    lint {
        abortOnError = false
        checkReleaseBuilds = true
    }

    defaultConfig {
        applicationId = "com.livecompose.livecapture"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Bugly AppID（需在 bugly.qq.com 注册获取，通过 -PBUGLY_APP_ID=xxx 传入）
        buildConfigField("String", "BUGLY_APP_ID", "\"${project.findProperty("BUGLY_APP_ID") as? String ?: ""}\"")
        // 微信 AppID（需在 open.weixin.qq.com 注册获取，通过 -PWECHAT_APP_ID=xxx 传入）
        buildConfigField("String", "WECHAT_APP_ID", "\"${project.findProperty("WECHAT_APP_ID") as? String ?: ""}\"")

        ndk {
            // arm64-v8a (主流设备) + x86_64 (模拟器兼容)
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/release.jks")
            storePassword = "livecapture123"
            keyAlias = "livecapture"
            keyPassword = "livecapture123"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation("androidx.compose.material3:material3-window-size-class")
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

    // ML Kit（升级至 16KB page 兼容版本）
    implementation("com.google.mlkit:face-detection:16.1.7")
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:image-labeling:17.0.9")

    // 注：TFLite 不再直接依赖。ML Kit image-labeling 已内置 com.google.ai.edge.litert
    // （16KB page 兼容），直接依赖 org.tensorflow:tensorflow-lite 会与 litert 产生重复类冲突。
    // 应用代码未直接 import org.tensorflow.*，故移除直接依赖。

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ExifInterface
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Bugly 崩溃上报（国内合规）— 仅 Java/Kotlin 崩溃上报
    // 注：4.1.9.3 的 libBugly.so 已 16KB 对齐，但 SDK 本身可能有 Android 15 运行时兼容风险
    // 如遇 Bugly 初始化崩溃，可尝试移除此依赖
    implementation("com.tencent.bugly:crashreport:4.1.9.3")

    // 微信分享 SDK — 升级至 Android 15 兼容版本（6.8.0 的 PendingIntent 不兼容 Android 15）
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.34")

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
