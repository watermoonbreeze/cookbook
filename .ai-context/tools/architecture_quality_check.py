"""Small, dependency-free architecture boundary check for CI/local review."""

from __future__ import annotations

import argparse
from pathlib import Path


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
