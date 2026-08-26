package com.sxdbsm.cookbook.android.util

import android.app.Application

object MainProcessLoggingPolicy {
    fun isMainProcess(application: Application): Boolean =
        isMainProcessName(application.applicationInfo.processName, Application.getProcessName())

    internal fun isMainProcessName(applicationProcessName: String, currentProcessName: String): Boolean =
        applicationProcessName == currentProcessName

    fun install(application: Application) {
        if (!isMainProcess(application)) return
        AppLogger.init(application)
        AppLogger.installCrashHandler()
    }
}
