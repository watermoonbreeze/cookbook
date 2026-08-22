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
            errors = MODULE.check_root(root)
            self.assertTrue(any("shared platform boundary" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
