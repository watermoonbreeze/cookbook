# P5-A Evidence

- Final acceptance: **ARCH_ACCEPTED / AUTOMATED_GATES_PASS**. Runtime remains **PENDING_DEVICE_VERIFICATION** for `DEV-P5-01` through `DEV-P5-05`.

- T-P5-A-01/02/03: `MealRecordUseCaseTest` proves an empty day returns no token, and a two-dish delete/restore round trip preserves date, meal type, time, note and dish order without increasing either dish preference.
- T-P5-A-04: the same test covers `<0` (0.0), legal (0.5) and `>1` (1.0) clamp read-back for both single-dish and whole-meal ratio APIs.
- T-P5-A-05: `DayAutoGeneratorMealBoundaryTest` proves preview zero-write and, for an existing same-meal record, `MERGE` retains the existing dish while the new dish's semantic `eatenRatio=0.5` is persisted and read back through `MealRecordUseCase`.
- T-P5-A-06: `HomeMealMutationBoundaryTest`, `TimelineMealMutationBoundaryTest` and `WeekPlanMealMutationBoundaryTest` drive the public ViewModel delete/delete-with-undo flows with a counting mutation port. They prove null/delete failure shows no undo, success exposes one undo and restores once; Timeline also asserts delete/restore failure state. WeekPlan/Home retain their existing silent/log restore failure behavior.
- Forced gates: `:shared:testDebugUnitTest --rerun-tasks` BUILD SUCCESSFUL (1m56s); `:androidApp:testDebugUnitTest --rerun-tasks` BUILD SUCCESSFUL (3m10s); `:shared:compileDebugKotlinAndroid --rerun-tasks` BUILD SUCCESSFUL (53s); `:androidApp:assembleDebug --rerun-tasks` BUILD SUCCESSFUL (3m20s).
- Release: first `:androidApp:assembleRelease --rerun-tasks` attempt timed out at 304.1s without a final Gradle result; this is retained as environment history. A subsequent fresh `scripts\build-cli.bat :androidApp:assembleRelease --rerun-tasks` reached **BUILD SUCCESSFUL in 7m53s** (83 tasks executed), so the forced release gate is now passing. The interim non-rerun `:androidApp:assembleRelease` also exited 0 / BUILD SUCCESSFUL.
- Quality: `python .ai-context/tools/test_architecture_quality_check.py` passed 8/8; `python .ai-context/tools/architecture_quality_check.py --root .` passed.
- Runtime: `DEV-P5-01` through `DEV-P5-05` remain `PENDING_DEVICE_VERIFICATION`; no device claim is made. P5-B must start with a new Reality Verification; Phase 6 is not authorized by this acceptance.
