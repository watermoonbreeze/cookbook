"""Small, dependency-free architecture boundary check for CI/local review."""

from __future__ import annotations

import argparse
import re
from pathlib import Path


REQUIRED_RECOMMEND_ENTRY_MARKERS = {
    "meal_edit": "Meal Edit AI Recommend entry marker",
    "record_meal_manual": "Record Meal manual AI Recommend entry marker",
}

REQUIRED_MEAL_FLOW_CODES = (
    "ai_recommend",
    "food_search",
    "inventory_select",
    "new_dish",
    "edit_meal",
)


def _read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""


def check_trace_governance(root: Path) -> list[str]:
    """Check static observability wiring and device-evidence registration."""
    errors: list[str] = []
    trace_model = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/TraceModel.kt"
    trace_contract = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/TraceEventContract.kt"
    logger_test = root / "shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/platform/LoggerTest.kt"
    nav_source = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav/MainScaffold.kt"
    routes_source = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav/Destinations.kt"
    recommend_source = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiRecommendViewModel.kt"
    add_meal_source = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal/AddMealViewModel.kt"

    model_text = _read_text(trace_model)
    contract_text = _read_text(trace_contract)
    test_text = _read_text(logger_test)
    nav_text = _read_text(nav_source)
    routes_text = _read_text(routes_source)
    recommend_text = _read_text(recommend_source)
    add_meal_text = _read_text(add_meal_source)

    if "traceId={traceId}" not in routes_text:
        errors.append("AI Recommend route must require traceId")
    if "aiRecommend()" in nav_text or "aiRecommendForMeal(slotCode)" in nav_text:
        errors.append("AI Recommend navigation must pass traceId")
    if "BusinessTrace.current" in model_text:
        errors.append("TraceModel must not use a global current trace")
    if "emitMealSaveTrace" in add_meal_text or '"recommend.started"' in recommend_text or '"recommend.finished"' in recommend_text:
        errors.append("operation lifecycle must be emitted only by OperationTrace")
    if re.search(r"fun\s+\w+\([^)]*traceId:\s*TraceId\?\s*=\s*null", model_text, re.DOTALL):
        errors.append("Trace helpers must require an explicit traceId")
    if "?: nextRecommendTrace()" in recommend_text:
        errors.append("AI recommendation must not drop its entry trace")
    if '"recommend.started"' in contract_text or '"recommend.finished"' in contract_text:
        errors.append("TraceEventContract must not register deprecated manual recommendation events")

    for marker, description in REQUIRED_RECOMMEND_ENTRY_MARKERS.items():
        for label, text in (("navigation source", nav_text), ("LoggerTest", test_text)):
            if marker not in text:
                errors.append(f"missing {description} ({marker}) in {label}")

    for marker in ("recommendRoute", "recommend.route", "traceId"):
        if marker not in model_text:
            errors.append(f"missing trace governance marker in TraceModel.kt: {marker}")
    if "recommend.route" not in contract_text:
        errors.append("TraceEventContract missing recommend.route")

    device_files = sorted((root / ".ai-context/docs/真机验证").glob("真机待验证清单_*.md"))
    if not device_files:
        errors.append("missing unique device verification checklist")
    else:
        evidence_text = _read_text(device_files[-1])
        for evidence_id in ("E-OVN-04", "E-OVN-05"):
            if evidence_id not in evidence_text:
                errors.append(f"latest device checklist missing {evidence_id}")
        if "meal_edit" not in evidence_text or "record_meal_manual" not in evidence_text:
            errors.append("latest device checklist does not distinguish both AI Recommend entry points")
        if "PENDING_DEVICE_VERIFICATION" not in evidence_text:
            errors.append("AI Recommend device evidence must remain pending until real-device execution")
    return errors


def check_meal_flow_contract(root: Path) -> list[str]:
    """Require one shared SAVE/RESTORE/MERGE contract for all meal subflows."""
    errors: list[str] = []
    contract = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/MealFlowStateContract.kt"
    diagnostic = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/TraceDiagnostic.kt"
    capability = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/RecommendationCapability.kt"
    governance = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/ArchitectureGovernance.kt"
    contract_text = _read_text(contract)
    diagnostic_text = _read_text(diagnostic)
    capability_text = _read_text(capability)
    governance_text = _read_text(governance)
    for marker in ("SAVE_STATE", "RESTORE_STATE", "MERGE_RESULT"):
        if marker not in contract_text:
            errors.append(f"meal flow contract missing {marker}")
    for flow in REQUIRED_MEAL_FLOW_CODES:
        if flow not in contract_text:
            errors.append(f"meal flow contract missing flow {flow}")
    for marker in ("diagnose", "FLOW_COMPLETE", "FLOW_INCOMPLETE", "NAVIGATION_FAILURE", "STATE_RESTORE_FAILURE", "STATE_MERGE_FAILURE", "OPERATION_FAILURE"):
        if marker not in diagnostic_text:
            errors.append(f"trace diagnostic missing {marker}")
    for marker in ("RecommendationTrace", "RecommendationReason", "RecommendationFeedback"):
        if marker not in capability_text + governance_text:
            errors.append(f"AI capability preparation missing {marker}")
    for marker in ("DomainState", "PageState", "NavigationState", "NavigationContract", "ResultContract", "RecommendationContext", "FeedbackModel"):
        if marker not in governance_text:
            errors.append(f"architecture governance missing {marker}")

    app_sources = root / "androidApp/src/main/java"
    app_text = "\n".join(path.read_text(encoding="utf-8") for path in app_sources.rglob("*.kt")) if app_sources.exists() else ""
    if "stateSnapshotBeforeNavigation" not in app_text:
        errors.append("navigation contract has no SAVE_STATE wiring")
    if "stateRestore" not in app_text:
        errors.append("navigation contract has no RESTORE_STATE wiring")
    if "stateMergeResult" not in app_text:
        errors.append("navigation contract has no MERGE_RESULT wiring")
    return errors


def check_privacy_log_boundary(root: Path) -> list[str]:
    """Reject known raw-content logging regressions in the B privacy allowlist."""
    errors: list[str] = []
    sources = (
        "androidApp/src/main/java/com/sxdbsm/cookbook/android/ai/VoiceRecognizer.kt",
        "androidApp/src/main/java/com/sxdbsm/cookbook/android/analytics/UmengAnalyticsSink.kt",
        "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/analytics/LogSink.kt",
        "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal/AddMealViewModel.kt",
        "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal/AddDayFoodScreen.kt",
        "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav/MainScaffold.kt",
        "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/dishes/DishesViewModel.kt",
        "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/dishes/DishesScreen.kt",
        "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/newdish/NewDishViewModel.kt",
        "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/newdish/NewDishScreen.kt",
    )
    forbidden = (
        "${it.message}", "throwable.message", "throwable.stackTrace", "params=${event.params}",
        "onResults: $text", "onPartialResults: $text", "currentName=${current.name}",
        "route=${", "date=${", "name=${", "dishIds=${", "dishes.map { it.id }", "error=${state.errorMessage}",
    )
    for relative in sources:
        text = _read_text(root / relative)
        lines = text.splitlines()
        log_context = "\n".join(
            "\n".join(lines[index:index + 6])
            for index, line in enumerate(lines)
            if "AppLogger." in line or "CookbookLog." in line
        )
        if any(marker in log_context for marker in forbidden):
            errors.append(f"privacy log boundary violated: {relative}")
    return errors


def check_root(root: Path) -> list[str]:
    errors: list[str] = []
    shared = root / "shared" / "src" / "commonMain"
    android_app = root / "androidApp" / "src" / "main"
    if not (shared / "kotlin").exists():
        errors.append("missing shared commonMain")
    if not (android_app / "java").exists():
        errors.append("missing androidApp main")

    for path in shared.rglob("*.kt") if shared.exists() else []:
        text = path.read_text(encoding="utf-8")
        if "import android." in text or "import com.sxdbsm.cookbook.android" in text:
            errors.append(f"shared platform boundary: {path.relative_to(root)}")

    for required in (
        shared / "kotlin/com/sxdbsm/cookbook/platform/TraceModel.kt",
        shared / "kotlin/com/sxdbsm/cookbook/platform/TraceEventContract.kt",
        shared / "kotlin/com/sxdbsm/cookbook/platform/Logger.kt",
    ):
        if not required.exists():
            errors.append(f"missing trace contract: {required.relative_to(root)}")

    for gradle in (root / "shared/build.gradle.kts", root / "androidApp/build.gradle.kts"):
        if gradle.exists() and 'project(":androidApp")' in gradle.read_text(encoding="utf-8"):
            errors.append(f"forbidden reverse module dependency: {gradle.relative_to(root)}")
    errors.extend(check_trace_governance(root))
    errors.extend(check_meal_flow_contract(root))
    errors.extend(check_privacy_log_boundary(root))
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path.cwd())
    args = parser.parse_args()
    errors = check_root(args.root.resolve())
    if errors:
        for error in errors:
            print(f"[FAIL] {error}")
        return 1
    print("ARCHITECTURE_QUALITY: PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
