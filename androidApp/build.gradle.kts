plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

android {
    namespace = "com.sxdbsm.cookbook.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.sxdbsm.cookbook.android"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // [AI生成] 统一签名：debug 和 release 都用项目根目录的 cookbook.jks(便于覆盖安装、双端一致)。
    signingConfigs {
        create("cookbook") {
            storeFile = rootProject.file("cookbook.jks")
            storePassword = "123456"
            keyAlias = "cookbook"
            keyPassword = "123456"
        }
    }
    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("cookbook")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("cookbook")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(projects.shared)

    // Compose
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material.icons)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.exifinterface) // [AI生成] 读取拍照 EXIF 方向,修部分设备(如小米8)拍照90°旋转
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel + Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // kotlinx
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Paging (Compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // 二维码（双设备同传：发送端生成、接收端扫描）
    implementation(libs.zxing.android.embedded)
}
