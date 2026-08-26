import importlib.util
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("architecture_quality_check.py")
SPEC = importlib.util.spec_from_file_location("architecture_quality_check", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC and SPEC.loader
SPEC.loader.exec_module(MODULE)


class ArchitectureQualityCheckTest(unittest.TestCase):
    def test_current_project_passes(self):
        errors = MODULE.check_root(Path(__file__).parents[2])
        self.assertEqual([], errors)

    def test_android_import_in_shared_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            common = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform"
            common.mkdir(parents=True)
            (root / "androidApp/src/main/java").mkdir(parents=True)
            (common / "TraceModel.kt").write_text("import android.util.Log\n", encoding="utf-8")
            (common / "TraceEventContract.kt").write_text("", encoding="utf-8")
            (common / "Logger.kt").write_text("", encoding="utf-8")
            (common / "MealFlowStateContract.kt").write_text(
                "SAVE_STATE RESTORE_STATE MERGE_RESULT ai_recommend food_search inventory_select new_dish edit_meal",
                encoding="utf-8",
            )
            (common / "TraceDiagnostic.kt").write_text(
                "diagnose FLOW_COMPLETE FLOW_INCOMPLETE",
                encoding="utf-8",
            )
            errors = MODULE.check_root(root)
            self.assertTrue(any("shared platform boundary" in error for error in errors))

    def test_trace_governance_requires_both_recommend_entries_and_evidence(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            common = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform"
            tests = root / "shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/platform"
            nav = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav"
            device = root / ".ai-context/docs/真机验证"
            for path in (common, tests, nav, device):
                path.mkdir(parents=True)
            (common / "TraceModel.kt").write_text(
                "recommendRoute traceId recommend.route meal_edit record_meal_manual", encoding="utf-8"
            )
            (common / "TraceEventContract.kt").write_text("recommend.route", encoding="utf-8")
            (common / "MealFlowStateContract.kt").write_text(
                "SAVE_STATE RESTORE_STATE MERGE_RESULT ai_recommend food_search inventory_select new_dish edit_meal",
                encoding="utf-8",
            )
            (common / "TraceDiagnostic.kt").write_text(
                "diagnose FLOW_COMPLETE FLOW_INCOMPLETE",
                encoding="utf-8",
            )
            (common / "Logger.kt").write_text("", encoding="utf-8")
            (tests / "LoggerTest.kt").write_text("meal_edit record_meal_manual", encoding="utf-8")
            (nav / "MainScaffold.kt").write_text("meal_edit record_meal_manual", encoding="utf-8")
            (nav / "Destinations.kt").write_text("traceId={traceId}", encoding="utf-8")
            (nav / "MealFlow.kt").write_text(
                "stateSnapshotBeforeNavigation stateRestore stateMergeResult",
                encoding="utf-8",
            )
            (device / "真机待验证清单_202608230000.md").write_text(
                "E-OVN-04 meal_edit PENDING_DEVICE_VERIFICATION\n"
                "E-OVN-05 record_meal_manual PENDING_DEVICE_VERIFICATION\n", encoding="utf-8"
            )
            self.assertEqual([], MODULE.check_trace_governance(root))

    def test_trace_governance_rejects_missing_second_entry(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            common = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform"
            tests = root / "shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/platform"
            nav = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav"
            device = root / ".ai-context/docs/真机验证"
            for path in (common, tests, nav, device):
                path.mkdir(parents=True)
            (common / "TraceModel.kt").write_text("recommendRoute traceId recommend.route meal_edit", encoding="utf-8")
            (common / "TraceEventContract.kt").write_text("recommend.route", encoding="utf-8")
            (tests / "LoggerTest.kt").write_text("meal_edit", encoding="utf-8")
            (nav / "MainScaffold.kt").write_text("meal_edit", encoding="utf-8")
            (device / "真机待验证清单_202608230000.md").write_text("E-OVN-04 meal_edit", encoding="utf-8")
            errors = MODULE.check_trace_governance(root)
            self.assertTrue(any("record_meal_manual" in error for error in errors))

    def test_ai_capability_preparation_is_required(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            common = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform"
            common.mkdir(parents=True)
            (root / "androidApp/src/main/java").mkdir(parents=True)
            for name, content in {
                "TraceModel.kt": "recommendRoute traceId recommend.route meal_edit record_meal_manual",
                "TraceEventContract.kt": "recommend.route",
                "Logger.kt": "",
                "MealFlowStateContract.kt": "SAVE_STATE RESTORE_STATE MERGE_RESULT ai_recommend food_search inventory_select new_dish edit_meal",
                "TraceDiagnostic.kt": "diagnose FLOW_COMPLETE FLOW_INCOMPLETE STATE_RESTORE_FAILURE MERGE_FAILURE",
            }.items():
                (common / name).write_text(content, encoding="utf-8")
            errors = MODULE.check_root(root)
            self.assertTrue(any("AI capability preparation" in error for error in errors))

    def test_trace_rules_reject_missing_route_and_manual_operation(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            common = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform"
            nav = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav"
            ai = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai"
            add = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal"
            for path in (common, nav, ai, add, root / "androidApp/src/main/java"):
                path.mkdir(parents=True, exist_ok=True)
            (common / "TraceModel.kt").write_text("recommendRoute traceId recommend.route", encoding="utf-8")
            (common / "TraceEventContract.kt").write_text("recommend.route", encoding="utf-8")
            (common / "Logger.kt").write_text("", encoding="utf-8")
            (nav / "Destinations.kt").write_text("ai_recommend", encoding="utf-8")
            (nav / "MainScaffold.kt").write_text("aiRecommend()", encoding="utf-8")
            (ai / "AiRecommendViewModel.kt").write_text('"recommend.started"', encoding="utf-8")
            (add / "AddMealViewModel.kt").write_text("emitMealSaveTrace", encoding="utf-8")
            errors = MODULE.check_trace_governance(root)
            self.assertTrue(any("traceId" in error or "operation lifecycle" in error for error in errors))

    def test_trace_rules_reject_default_trace_deprecated_events_and_dropped_entry(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            common = root / "shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform"
            nav = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav"
            ai = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai"
            add = root / "androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal"
            tests = root / "shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/platform"
            device = root / ".ai-context/docs/真机验证"
            for path in (common, nav, ai, add, tests, device, root / "androidApp/src/main/java"):
                path.mkdir(parents=True, exist_ok=True)
            (common / "TraceModel.kt").write_text("recommendRoute traceId recommend.route fun state(traceId: TraceId? = null)", encoding="utf-8")
            (common / "TraceEventContract.kt").write_text('recommend.route "recommend.started"', encoding="utf-8")
            (common / "Logger.kt").write_text("", encoding="utf-8")
            (nav / "Destinations.kt").write_text("traceId={traceId}", encoding="utf-8")
            (nav / "MainScaffold.kt").write_text("meal_edit record_meal_manual", encoding="utf-8")
            (ai / "AiRecommendViewModel.kt").write_text("?: nextRecommendTrace()", encoding="utf-8")
            (add / "AddMealViewModel.kt").write_text("", encoding="utf-8")
            (tests / "LoggerTest.kt").write_text("meal_edit record_meal_manual", encoding="utf-8")
            (device / "真机待验证清单_202608230000.md").write_text(
                "E-OVN-04 meal_edit PENDING_DEVICE_VERIFICATION\n"
                "E-OVN-05 record_meal_manual PENDING_DEVICE_VERIFICATION\n", encoding="utf-8"
            )
            errors = MODULE.check_trace_governance(root)
            self.assertTrue(any("explicit traceId" in error for error in errors))
            self.assertTrue(any("deprecated manual" in error for error in errors))
            self.assertTrue(any("drop its entry" in error for error in errors))

    def test_privacy_log_boundary_rejects_raw_voice_and_analytics_parameters(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            cases = {
                "throwable_message": ("androidApp/src/main/java/com/sxdbsm/cookbook/android/analytics/UmengAnalyticsSink.kt", "CookbookLog.d(\"x\", throwable.message)"),
                "throwable_stack": ("androidApp/src/main/java/com/sxdbsm/cookbook/android/analytics/UmengAnalyticsSink.kt", "CookbookLog.d(\"x\", throwable.stackTrace)"),
                "analytics_params": ("shared/src/commonMain/kotlin/com/sxdbsm/cookbook/analytics/LogSink.kt", "CookbookLog.d(\"x\", \"params=${event.params}\")"),
                "raw_route": ("androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav/MainScaffold.kt", "AppLogger.d(\"x\", \"route=${value}\")"),
                "raw_date": ("androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal/AddMealViewModel.kt", "AppLogger.d(\"x\", \"date=${value}\")"),
                "dish_name": ("androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/dishes/DishesViewModel.kt", "AppLogger.d(\"x\", \"name=${dish.name}\")"),
                "dish_ids": ("androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal/AddDayFoodScreen.kt", "AppLogger.d(\"x\", \"${dishes.map { it.id }}\")"),
                "ui_error": ("androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/newdish/NewDishScreen.kt", "AppLogger.d(\"x\", \"error=${state.errorMessage}\")"),
            }
            for label, (relative, marker) in cases.items():
                with self.subTest(label=label):
                    target = root / relative
                    target.parent.mkdir(parents=True, exist_ok=True)
                    target.write_text(marker, encoding="utf-8")
                    errors = MODULE.check_privacy_log_boundary(root)
                    self.assertTrue(any(relative in error for error in errors))
                    target.unlink()


if __name__ == "__main__":
    unittest.main()
