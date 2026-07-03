@echo off
rem =====================================================================
rem [AI] CLI build entry (Windows). Doc: .ai-context/rules/tong-yong-gui-ze.md sec.8
rem Forces JDK 17 for Gradle daemon (required by AGP 8.2.2), independent
rem from global ~/.gradle/gradle.properties and IDE settings.
rem Usage: scripts\build-cli.bat :androidApp:assembleDebug
rem        scripts\build-cli.bat :shared:testDebugUnitTest
rem On a new machine, edit CLI_JDK_HOME below to the local JDK 17 path.
rem =====================================================================
setlocal

set "CLI_JDK_HOME=C:\Program Files\Java\jdk-17"

if exist "%CLI_JDK_HOME%\bin\java.exe" (
    set "JAVA_HOME=%CLI_JDK_HOME%"
) else (
    echo [build-cli] WARN: %CLI_JDK_HOME% not found, fallback to JAVA_HOME=%JAVA_HOME%
)

pushd "%~dp0.."
call "%~dp0..\gradlew.bat" %*
set "EXIT_CODE=%ERRORLEVEL%"
popd
endlocal & exit /b %EXIT_CODE%
