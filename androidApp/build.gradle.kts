import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
}

// [AI生成] 阶段3-d：从 local.properties 读友盟 AppKey(不进 git)，注入 BuildConfig 供 UMConfigure.init 使用。
val umengAppKey: String = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}.getProperty("UMENG_APP_KEY", "")

android {
    namespace = "com.sxdbsm.cookbook.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.sxdbsm.cookbook.android"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        // [AI生成] 阶段3-d：友盟 AppKey(空串=未配置·UmengAnalyticsSink 会跳过 init)。
        buildConfigField("String", "UMENG_APP_KEY", "\"$umengAppKey\"")
    }
    buildFeatures {
        compose = true
        buildConfig = true // [AI生成] 阶段3-d：启用 BuildConfig(承载友盟 AppKey)。
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

    // [AI生成] 阶段3-d 友盟 U-App 匿名统计(common+asms·不加 uyumao 避免多采集)。
    //   版本用 latest.release 保证可解析(headless 无法核对确切版本号)；发布前建议钉具体版本(可复现构建)。
    //   合规:UMConfigure.init 延迟到用户同意后(见 UmengAnalyticsSink)。
    implementation("com.umeng.umsdk:common:latest.release")
    implementation("com.umeng.umsdk:asms:latest.release")
}
