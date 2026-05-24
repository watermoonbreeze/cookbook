plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    // MVP 不开发 iOS。一期再启用 iOS targets。
    // 已保留 iosMain 源码（不参与编译），等启用 target 时可立即接入。
    // listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
    //     it.binaries.framework { baseName = "shared"; isStatic = true }
    // }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

sqldelight {
    databases {
        create("CookbookDatabase") {
            packageName.set("com.sxdbsm.cookbook.db")
        }
    }
}

android {
    namespace = "com.sxdbsm.cookbook"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
