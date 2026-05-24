enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
pluginManagement {
    repositories {
        // 优先使用阿里云镜像以提高下载速度和稳定性
        maven(url = uri("https://maven.aliyun.com/nexus/content/repositories/google"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/groups/public"))
        maven(url = uri("https://maven.aliyun.com/repository/jcenter"))
        maven(url = uri("https://maven.aliyun.com/repository/gradle-plugin"))
        maven(url = uri("https://maven.aliyun.com/repository/public/"))
        maven(url = uri("https://jitpack.io"))

        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        // 优先使用阿里云镜像以提高下载速度和稳定性
        maven(url = uri("https://maven.aliyun.com/nexus/content/repositories/google"))
        maven(url = uri("https://maven.aliyun.com/nexus/content/groups/public"))
        maven(url = uri("https://maven.aliyun.com/repository/jcenter"))
        maven(url = uri("https://maven.aliyun.com/repository/public/"))
        maven(url = uri("https://jitpack.io"))

        google()
        mavenCentral()
    }
}

rootProject.name = "Cookbook"
include(":androidApp")
include(":shared")