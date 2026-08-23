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


if __name__ == "__main__":
    unittest.main()
