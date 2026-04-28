#!/usr/bin/env python3
"""
Convert Chuanyuanyi markdown exports into Mnemora quiz package source trees.

The source format is:
    book + chapters + sections + questions

The Mnemora package format is:
    name + description + icon + nodes
"""

import argparse
import json
import re
import shutil
from collections import defaultdict
from pathlib import Path


IMAGE_RE = re.compile(r"!\[([^\]]*)\]\(([^)]*)\)")
DEFAULT_SOURCE = Path("/home/ming/projects/chuanyuanyi-data/data/07a-markdown")
DEFAULT_ASSET_ROOT = Path("/home/ming/projects/chuanyuanyi-data/data/02-asset-localized")
DEFAULT_OUTPUT = Path("build/chuanyuanyi-mnemora/sources")


def clean_markdown(text: str | None) -> str:
    if not text:
        return ""

    def replace_empty_image(match: re.Match[str]) -> str:
        alt, path = match.group(1).strip(), match.group(2).strip()
        if path:
            return match.group(0)
        return f"（图片：{alt}）" if alt else ""

    cleaned = IMAGE_RE.sub(replace_empty_image, text)
    cleaned = IMAGE_RE.sub(lambda match: f"\n\n{match.group(0)}\n\n", cleaned)
    cleaned = re.sub(r"[ \t]+\n", "\n", cleaned)
    cleaned = re.sub(r"\n{3,}", "\n\n", cleaned)
    return cleaned.strip()


def collect_image_refs(value) -> set[str]:
    refs: set[str] = set()

    def scan(item) -> None:
        if isinstance(item, dict):
            for child in item.values():
                scan(child)
        elif isinstance(item, list):
            for child in item:
                scan(child)
        elif isinstance(item, str):
            for _, ref in IMAGE_RE.findall(item):
                ref = ref.strip()
                if ref and not ref.startswith(("http://", "https://", "file://")):
                    refs.add(ref)

    scan(value)
    return refs


def convert_choice(choice: dict) -> dict:
    html_content = choice.get("html")
    if html_content:
        # HTML choices pass through without markdown cleaning
        return {
            "key": str(choice.get("key", "")).strip(),
            "content": html_content,
            "format": "html",
        }
    content = choice.get("content") or choice.get("text") or ""
    return {
        "key": str(choice.get("key", "")).strip(),
        "content": clean_markdown(content),
    }


def convert_question(
    question: dict,
    *,
    is_sub_question: bool = False,
) -> dict:
    children = question.get("children") or []
    has_html = bool(question.get("html"))
    if children and not is_sub_question:
        converted = {
            "content": clean_markdown(question.get("content")),
            "explanation": clean_markdown(question.get("explanation")),
            "question_type": "passage",
            "sub_questions": [
                convert_question(child, is_sub_question=True)
                for child in children
            ],
        }
        if has_html:
            converted["format"] = "html"
        return converted

    content_raw = question.get("html") if has_html else question.get("content")
    explanation_raw = question.get("html") if has_html else question.get("explanation")
    converted = {
        "content": content_raw if has_html else clean_markdown(content_raw),
        "choices": [convert_choice(choice) for choice in question.get("choices", [])],
        "answer": str(question.get("answer", "")).strip(),
        "explanation": explanation_raw if has_html else clean_markdown(explanation_raw),
        "question_type": "multiple_choice",
    }
    if has_html:
        converted["format"] = "html"
    return converted


def build_subject_node(source_file: Path) -> tuple[dict, dict]:
    data = json.loads(source_file.read_text(encoding="utf-8"))
    book = data.get("book", {})
    subject_name = book.get("subject_name_zh") or source_file.stem

    chapters_by_id = {str(chapter["id"]): chapter for chapter in data.get("chapters", [])}
    sections_by_chapter: dict[str, list[dict]] = defaultdict(list)
    for section in data.get("sections", []):
        sections_by_chapter[str(section.get("chapter_id"))].append(section)

    questions_by_section: dict[str, list[dict]] = defaultdict(list)
    uncategorized: list[dict] = []
    for question in data.get("questions", []):
        section_id = question.get("section_id")
        if section_id is None:
            uncategorized.append(question)
        else:
            questions_by_section[str(section_id)].append(question)

    chapter_nodes: list[dict] = []
    known_section_ids = {str(section.get("id")) for section in data.get("sections", [])}
    for chapter in data.get("chapters", []):
        chapter_id = str(chapter.get("id"))
        chapter_title = chapter.get("title") or f"章节 {chapter_id}"
        section_nodes: list[dict] = []
        for section in sections_by_chapter.get(chapter_id, []):
            section_id = str(section.get("id"))
            section_title = section.get("title") or f"小节 {section_id}"
            questions = [
                convert_question(question)
                for question in questions_by_section.get(section_id, [])
            ]
            if questions:
                section_nodes.append({"title": section_title, "questions": questions})

        direct_questions = [
            question
            for question in questions_by_section.get(chapter_id, [])
            if str(question.get("section_id")) == chapter_id
        ]
        if direct_questions:
            section_nodes.append(
                {
                    "title": "章节题目",
                    "questions": [convert_question(question) for question in direct_questions],
                }
            )

        if section_nodes:
            chapter_nodes.append({"title": chapter_title, "children": section_nodes})

    unmatched_section_nodes: list[dict] = []
    for section_id, questions in sorted(questions_by_section.items()):
        if section_id in known_section_ids:
            continue
        unmatched_section_nodes.append(
            {
                "title": f"未匹配小节 {section_id}",
                "questions": [convert_question(question) for question in questions],
            }
        )
    if unmatched_section_nodes:
        chapter_nodes.append({"title": "未匹配小节", "children": unmatched_section_nodes})

    if uncategorized:
        chapter_nodes.append(
            {
                "title": "未分类",
                "questions": [convert_question(question) for question in uncategorized],
            }
        )

    referenced_section_ids = {
        str(section.get("id"))
        for chapter_id in chapters_by_id
        for section in sections_by_chapter.get(chapter_id, [])
    }
    orphan_sections = [
        section
        for section in data.get("sections", [])
        if str(section.get("id")) not in referenced_section_ids
    ]

    subject_node = {"title": subject_name, "children": chapter_nodes}
    stats = {
        "source": source_file.name,
        "subject": subject_name,
        "questions": len(data.get("questions", [])),
        "passages": sum(1 for q in data.get("questions", []) if q.get("children")),
        "chapters": len(chapter_nodes),
        "orphan_sections": len(orphan_sections),
    }
    return subject_node, stats


def copy_referenced_images(package_data: dict, asset_root: Path, output_dir: Path) -> tuple[int, list[str]]:
    image_refs = collect_image_refs(package_data)
    copied = 0
    missing: list[str] = []
    for ref in sorted(image_refs):
        source_asset = asset_root / ref
        if not source_asset.exists():
            missing.append(ref)
            continue
        target_asset = output_dir / ref
        target_asset.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source_asset, target_asset)
        copied += 1
    return copied, missing


def write_one_package_source(
    output_dir: Path,
    asset_root: Path,
    package_data: dict,
    stats: dict,
) -> dict:
    output_dir.mkdir(parents=True, exist_ok=True)
    data_path = output_dir / "data.json"
    data_path.write_text(
        json.dumps(package_data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    copied, missing = copy_referenced_images(package_data, asset_root, output_dir)
    return {
        "output_dir": str(output_dir),
        "data_json": str(data_path),
        "subject": stats,
        "image_refs": len(collect_image_refs(package_data)),
        "copied_images": copied,
        "missing_images": missing,
    }


def build_subject_package_data(source_file: Path) -> tuple[dict, dict]:
    node, stats = build_subject_node(source_file)
    package_data = {
        "name": stats["subject"],
        "description": f"由 07a-markdown/{source_file.name} 转换的航海题库，包含选择题和阅读理解题。",
        "icon": "school",
        "nodes": [node],
    }
    return package_data, stats


def write_split_package_sources(source_dir: Path, asset_root: Path, output_dir: Path) -> dict:
    output_dir.mkdir(parents=True, exist_ok=True)
    packages = []
    for data_file in sorted(source_dir.glob("*.json")):
        package_data, stats = build_subject_package_data(data_file)
        packages.append(
            write_one_package_source(
                output_dir / data_file.stem,
                asset_root,
                package_data,
                stats,
            )
        )

    return {
        "output_dir": str(output_dir),
        "packages": packages,
        "missing_images": [
            ref
            for package in packages
            for ref in package["missing_images"]
        ],
    }


def write_combined_package_source(source_dir: Path, asset_root: Path, output_dir: Path) -> dict:
    output_dir.mkdir(parents=True, exist_ok=True)
    nodes: list[dict] = []
    stats: list[dict] = []
    for data_file in sorted(source_dir.glob("*.json")):
        node, file_stats = build_subject_node(data_file)
        nodes.append(node)
        stats.append(file_stats)

    package_data = {
        "name": "船员易题库",
        "description": "由 07a-markdown 数据转换的航海题库，包含选择题和阅读理解题。",
        "icon": "school",
        "nodes": nodes,
    }

    summary = write_one_package_source(
        output_dir,
        asset_root,
        package_data,
        {"source": "combined", "subject": "船员易题库", "subjects": stats},
    )
    summary["subjects"] = stats
    return summary


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--asset-root", type=Path, default=DEFAULT_ASSET_ROOT)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument(
        "--combined",
        action="store_true",
        help="Write one combined package source instead of one package source per subject.",
    )
    args = parser.parse_args()

    if args.combined:
        summary = write_combined_package_source(
            args.source.resolve(),
            args.asset_root.resolve(),
            args.output.resolve(),
        )
    else:
        summary = write_split_package_sources(
            args.source.resolve(),
            args.asset_root.resolve(),
            args.output.resolve(),
        )
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 1 if summary["missing_images"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
