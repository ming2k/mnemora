#!/usr/bin/env python3
"""
Package Quiz — validate and bundle a Mnemora question bank into .mnemorapkg

Usage:
    python3 scripts/package-quiz.py <source_dir> [output_file]

Example:
    python3 scripts/package-quiz.py ~/my-quiz-bank/ ~/my-quiz-bank.mnemorapkg

The script will:
1. Validate data.json exists and is valid JSON
2. Check that all image references resolve to actual files
3. Warn about unknown question_type values
4. Produce a .mnemorapkg (or .zip) archive
"""

import json
import os
import re
import sys
import zipfile
from pathlib import Path


SUPPORTED_EXTENSIONS = (".zip", ".quizpkg", ".mnemorapkg")
SUPPORTED_QUESTION_TYPES = {
    "multiple_choice",
    "true_false",
    "fill_blank",
    "cloze",
    "flashcard",
    "passage",
}
REQUIRED_DATA_FILES = ("data.json",)


def find_data_json(src_dir: Path) -> Path:
    """Locate data.json in src_dir or its immediate subdirectories."""
    for name in REQUIRED_DATA_FILES:
        direct = src_dir / name
        if direct.exists():
            return direct
        for sub in src_dir.iterdir():
            if sub.is_dir():
                nested = sub / name
                if nested.exists():
                    return nested
    raise FileNotFoundError(
        f"No data.json found in {src_dir} or its immediate subdirectories."
    )


def extract_image_refs(text: str) -> list[str]:
    """Find all Markdown image paths: ![alt](path)"""
    if not text:
        return []
    return re.findall(r"!\[.*?\]\((.*?)\)", text)


def collect_all_image_refs(data: dict) -> list[str]:
    """Recursively collect image references from the entire data tree."""
    refs = []

    def scan(obj):
        if isinstance(obj, dict):
            for k, v in obj.items():
                if isinstance(v, str) and k in (
                    "content",
                    "explanation",
                    "front_template",
                    "back_template",
                ):
                    refs.extend(extract_image_refs(v))
                elif k == "choices" and isinstance(v, list):
                    for choice in v:
                        if isinstance(choice, dict):
                            refs.extend(extract_image_refs(choice.get("content", "")))
                            refs.extend(extract_image_refs(choice.get("html", "")))
                            refs.extend(extract_image_refs(choice.get("text", "")))
                else:
                    scan(v)
        elif isinstance(obj, list):
            for item in obj:
                scan(item)

    scan(data)
    return refs


def validate(src_dir: Path, data_path: Path) -> list[str]:
    """Run validations and return a list of warnings/errors."""
    issues = []

    try:
        with open(data_path, "r", encoding="utf-8") as f:
            data = json.load(f)
    except json.JSONDecodeError as e:
        issues.append(f"[ERROR] Invalid JSON in {data_path}: {e}")
        return issues

    if not isinstance(data, dict):
        issues.append("[ERROR] Root of data.json must be a JSON object.")
        return issues

    # Basic field presence
    if "nodes" not in data:
        issues.append("[WARN] Root object is missing 'nodes'; package will import with 0 questions.")
    elif not isinstance(data.get("nodes"), list):
        issues.append("[WARN] 'nodes' must be an array.")

    # Recursively scan question types and image refs
    image_refs = collect_all_image_refs(data)
    image_refs_seen = set()
    for ref in image_refs:
        if ref.startswith("http://") or ref.startswith("https://") or ref.startswith("file://"):
            continue
        image_refs_seen.add(ref)

    # Resolve images relative to src_dir (or the directory containing data.json)
    base_dir = src_dir if data_path.parent == src_dir else data_path.parent
    for ref in sorted(image_refs_seen):
        resolved = base_dir / ref
        if not resolved.exists():
            issues.append(f"[ERROR] Referenced image not found: {ref} (looked in {resolved})")
        else:
            print(f"  [OK] Image resolved: {ref}")

    # Check question types
    def scan_types(obj):
        if isinstance(obj, dict):
            qtype = obj.get("question_type")
            if qtype and qtype not in SUPPORTED_QUESTION_TYPES:
                issues.append(f"[WARN] Unknown question_type: '{qtype}'")
            for v in obj.values():
                scan_types(v)
        elif isinstance(obj, list):
            for item in obj:
                scan_types(item)

    scan_types(data)

    return issues


def build_package(src_dir: Path, output_path: Path) -> None:
    """Create the zip archive."""
    data_path = find_data_json(src_dir)
    base_dir = src_dir if data_path.parent == src_dir else data_path.parent

    with zipfile.ZipFile(output_path, "w", zipfile.ZIP_DEFLATED) as zf:
        for root, _, files in os.walk(base_dir):
            for filename in files:
                if filename.lower().endswith(SUPPORTED_EXTENSIONS):
                    continue
                file_path = Path(root) / filename
                arcname = str(file_path.relative_to(base_dir))
                zf.write(file_path, arcname)
                print(f"  [ADDED] {arcname}")

    print(f"\nPackage created: {output_path}")
    size_kb = output_path.stat().st_size / 1024
    print(f"Size: {size_kb:.1f} KB")


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 1

    src_dir = Path(sys.argv[1]).resolve()
    if not src_dir.is_dir():
        print(f"[ERROR] Source directory does not exist: {src_dir}")
        return 1

    if len(sys.argv) >= 3:
        output_path = Path(sys.argv[2]).resolve()
    else:
        default_name = src_dir.name + ".mnemorapkg"
        output_path = src_dir.parent / default_name

    if not str(output_path).lower().endswith(SUPPORTED_EXTENSIONS):
        output_path = output_path.with_suffix(".mnemorapkg")

    print(f"Source : {src_dir}")
    print(f"Output : {output_path}")
    print()

    data_path = find_data_json(src_dir)
    print(f"Found data.json at: {data_path.relative_to(src_dir) if data_path.is_relative_to(src_dir) else data_path}")
    print()

    print("Validating...")
    issues = validate(src_dir, data_path)
    if issues:
        print()
        for issue in issues:
            print(f"  {issue}")
    else:
        print("  No issues found.")

    errors = [i for i in issues if i.startswith("[ERROR]")]
    if errors:
        print(f"\nValidation failed with {len(errors)} error(s). Package was NOT created.")
        return 1

    print("\nBuilding package...")
    build_package(src_dir, output_path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
