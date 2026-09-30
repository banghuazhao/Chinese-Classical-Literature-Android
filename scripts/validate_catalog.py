#!/usr/bin/env python3
"""Check the bundled bilingual catalog without starting Android or Gradle."""

import json
import struct
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


def read_json(path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError) as exc:
        errors.append(f"{path.relative_to(ROOT)}: {exc}")
        return None


catalogs = {
    script: read_json(ASSETS / f"bookInfo-{script}.json")
    for script in ("simplified", "traditional")
}
copy = {
    (script, kind): read_json(
        ASSETS / "literary_copy" / (
            f"{kind}{'-traditional' if script == 'traditional' else ''}.json"
        )
    )
    for script in catalogs
    for kind in ("books", "authors")
}
index_map = read_json(ASSETS / "chapter-index-map.json")
counts = {}

for script, catalog in catalogs.items():
    if not isinstance(catalog, list):
        errors.append(f"{script}: catalog must be a list")
        continue
    ids = [item.get("id") for item in catalog]
    require(len(ids) == len(set(ids)), f"{script}: duplicate work IDs")
    for book in catalog:
        name = book.get("name", "")
        label = f"{script}: {name or '<unnamed>'}"
        for key in ("id", "name", "author", "fileName", "bookCover", "bookType"):
            require(bool(str(book.get(key, "")).strip()), f"{label}: missing {key}")
        expected_suffix = "_Fan" if script == "traditional" else ""
        require(book.get("bookType", "").endswith("_Fan") == bool(expected_suffix),
                f"{label}: wrong script bookType")

        for field, value in (("books", name), ("authors", book.get("author"))):
            entries = copy.get((script, field)) or {}
            require(bool(str(entries.get(value, "")).strip()),
                    f"{label}: missing {field} introduction")

        cover = ASSETS / "covers" / f"{book.get('bookCover', '')}.png"
        if not cover.is_file():
            errors.append(f"{label}: missing cover {cover.name}")
        else:
            with cover.open("rb") as stream:
                header = stream.read(24)
            if len(header) < 24 or header[:8] != b"\x89PNG\r\n\x1a\n":
                errors.append(f"{label}: invalid PNG cover")
            else:
                width, height = struct.unpack(">II", header[16:24])
                require(width >= 300 and height >= 450,
                        f"{label}: cover too small ({width}x{height})")
                require(cover.stat().st_size <= 250_000,
                        f"{label}: cover exceeds 250 KB")

        content = read_json(ASSETS / "files" / f"{book.get('fileName', '')}.json")
        if not isinstance(content, dict) or not content:
            errors.append(f"{label}: missing or malformed chapter file")
            continue
        chapters = next(iter(content.values()))
        if not isinstance(chapters, list) or not chapters:
            errors.append(f"{label}: no chapters")
            continue
        counts[(script, book.get("id"))] = len(chapters)
        for index, chapter in enumerate(chapters):
            title = chapter.get("章节名称", chapter.get("章節名稱", ""))
            text = chapter.get("章节内容", chapter.get("章節內容", ""))
            require(bool(str(title).strip()), f"{label}: chapter {index + 1} has no title")
            require(bool(str(text).strip()), f"{label}: chapter {index + 1} is empty")

simple = catalogs.get("simplified") or []
traditional = catalogs.get("traditional") or []
require(len(simple) == len(traditional), "catalogs have different book counts")
for left, right in zip(simple, traditional):
    work_id = left.get("id")
    require(work_id == right.get("id"),
            f"mispaired catalog entries: {left.get('name')} / {right.get('name')}")
    require(left.get("bookCover") == right.get("bookCover"),
            f"{work_id}: script variants must share a text-free cover")
    simple_count = counts.get(("simplified", work_id))
    traditional_count = counts.get(("traditional", work_id))
    if simple_count is None or traditional_count is None:
        continue
    omitted = (index_map or {}).get(work_id, [])
    require(simple_count - traditional_count == len(omitted),
            f"{work_id}: chapter count gap needs an index map")
    require(omitted == sorted(set(omitted)) and all(
        isinstance(index, int) and 0 <= index < simple_count for index in omitted
    ), f"{work_id}: invalid omitted chapter indexes")

known_ids = {book.get("id") for book in simple}
for work_id in index_map or {}:
    require(work_id in known_ids, f"unknown work in chapter index map: {work_id}")

referenced_covers = {
    book.get("bookCover") for catalog in catalogs.values() for book in catalog
}
for cover in (ASSETS / "covers").glob("*.png"):
    require(cover.stem in referenced_covers, f"unreferenced cover: {cover.name}")

if errors:
    print("Catalog validation failed:", file=sys.stderr)
    for error in errors:
        print(f"  - {error}", file=sys.stderr)
    sys.exit(1)

print(f"Catalog OK: {len(simple)} paired works, {sum(counts.values())} chapters, "
      f"{len(referenced_covers)} covers")
