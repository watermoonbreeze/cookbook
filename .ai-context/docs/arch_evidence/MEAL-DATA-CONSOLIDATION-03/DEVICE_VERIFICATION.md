# MEAL-DATA-CONSOLIDATION-03 R2 Device Verification

## Build evidence

- Device: HUAWEI TAS-AN00
- Android: 12 / API 31
- Package: `com.sxdbsm.cookbook.android`
- Build: `scripts\build-cli.bat :shared:testDebugUnitTest :androidApp:assembleDebug`
- Result: `BUILD SUCCESSFUL`
- Unit tests: 674 completed, 0 failed

## Trace implementation

The Debug-only trace is implemented in:

- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/MealDataTraceLogger.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/repository/MealRecordRepository.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/home/HomeViewModel.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/weekplan/WeekPlanViewModel.kt`

Expected filter: `adb logcat -s CB/MDC3:D *:S`

Expected markers:

- `[MDC3][Revision] meal_record_dish changed`
- `[MDC3][Revision] dish_ingredient changed`
- `[MDC3][Revision] dish changed`
- `[MDC3][Projection] generated`
- `[MDC3][UI] feature=home` or `feature=weekplan`

## Device cases

| Case | Operation | Result | Evidence |
|---|---|---|---|
| 1 | Change meal eaten ratio | BLOCKED: device remained on system lock screen | No app log captured |
| 2 | Change dish information | BLOCKED: device remained on system lock screen | No app log captured |
| 3 | Change dish ingredient | BLOCKED: device remained on system lock screen | No app log captured |

ADB was connected and the debug APK was installed. `MainActivity` started successfully, but Keyguard kept the app window covered; no unlock credential was available to the agent. These three cases must be rerun after manual unlock, with the expected log markers above recorded here before ARCH acceptance.

## Gate status

- Unit test gate: PASS
- Android debug build gate: PASS
- Schema diff: NONE in this batch
- API deletion: NONE in this batch
- Repository redesign: NONE in this batch
- Device evidence gate: BLOCKED pending manual unlock and three UI cases
