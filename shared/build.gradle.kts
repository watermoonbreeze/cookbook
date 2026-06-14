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
        getByName("androidMain").resources.srcDir("src/commonMain/resources") // [AI生成] Android 运行时可通过 ClassLoader 读取基础数据 JSON。
        getByName("androidUnitTest").resources.srcDir("src/commonMain/resources") // [AI生成] 单元测试同步读取基础数据 JSON，防止 seed 资源漏打包。
        getByName("androidUnitTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver) // [AI生成] 单元测试使用内存 SQLite，验证 SQLDelight 查询和 Repository 逻辑。
        }
    }
}

sqldelight {
    databases {
        create("CookbookDatabase") {
            packageName.set("com.sxdbsm.cookbook.db")
            version = 7 // [AI修改] v7 新增烹饪计时模板表；数据库版本只能升不能降。
        }
    }
}

android {
    namespace = "com.sxdbsm.cookbook"
    compileSdk = 34
    sourceSets["main"].resources.srcDir("src/commonMain/resources") // [AI生成] Android library 打包 seed JSON，供 ClassLoader 读取。
    sourceSets["test"].resources.srcDir("src/commonMain/resources") // [AI生成] JVM 单元测试打包同一份 seed JSON，避免测试与运行时数据分叉。
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
