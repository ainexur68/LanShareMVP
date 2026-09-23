#!/usr/bin/env python3
"""Report an APK's installed package composition and enforce the release size cap."""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import re
import sys
import zipfile

MIB = 1024 * 1024
CATEGORIES = ("DEX", "Resources", "Assets", "Native libraries", "Other")


def category_for(path: str) -> str:
    name = Path(path).name
    if re.fullmatch(r"classes(?:\d+)?\.dex", name):
        return "DEX"
    if path == "resources.arsc" or path.startswith("res/"):
        return "Resources"
    if path.startswith("assets/"):
        return "Assets"
    if path.startswith("lib/") and path.endswith(".so"):
        return "Native libraries"
    return "Other"


def build_report(apk: Path, max_mib: float) -> tuple[str, int, int]:
    if not apk.is_file():
        raise ValueError(f"APK not found: {apk}")
    if not zipfile.is_zipfile(apk):
        raise ValueError(f"Not a valid APK/ZIP archive: {apk}")

    totals = {category: 0 for category in CATEGORIES}
    abis: set[str] = set()
    with zipfile.ZipFile(apk) as archive:
        names = [info.filename for info in archive.infolist() if not info.is_dir()]
        if not any(re.fullmatch(r"classes(?:\d+)?\.dex", name) for name in names):
            raise ValueError("APK contains no DEX bytecode")
        for info in archive.infolist():
            if info.is_dir():
                continue
            totals[category_for(info.filename)] += info.compress_size
            if info.filename.startswith("lib/"):
                parts = info.filename.split("/")
                if len(parts) > 2 and parts[1]:
                    abis.add(parts[1])

    size_bytes = apk.stat().st_size
    payload_bytes = sum(totals.values())
    overhead_bytes = max(0, size_bytes - payload_bytes)
    limit_bytes = int(max_mib * MIB)
    rows = [
        "# Release APK size report",
        "",
        f"- APK: `{apk}`",
        f"- Total: **{size_bytes / MIB:.2f} MiB** ({size_bytes:,} bytes)",
        f"- Limit: **{max_mib:g} MiB** ({limit_bytes:,} bytes)",
        f"- Native ABIs: {', '.join(sorted(abis)) if abis else 'none'}",
        "",
        "| Component | Compressed APK payload | Share of APK |",
        "|---|---:|---:|",
    ]
    for category in CATEGORIES:
        amount = totals[category]
        share = amount / size_bytes * 100 if size_bytes else 0
        rows.append(f"| {category} | {amount / MIB:.2f} MiB | {share:.1f}% |")
    overhead_share = overhead_bytes / size_bytes * 100 if size_bytes else 0
    rows.append(
        f"| ZIP/signing overhead | {overhead_bytes / MIB:.2f} MiB | {overhead_share:.1f}% |"
    )
    rows.extend(
        [
            "",
            "Component sizes sum compressed ZIP-entry payloads. ZIP metadata, alignment, and APK signing block are reported as overhead.",
            "",
            f"**Gate: {'PASS' if size_bytes <= limit_bytes else 'FAIL'}**",
        ]
    )
    return "\n".join(rows) + "\n", size_bytes, limit_bytes


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path, help="Release APK to inspect")
    parser.add_argument("--max-mib", type=float, default=15, help="Maximum APK size in MiB (default: 15)")
    parser.add_argument("--report-file", type=Path, help="Also write the report to this path")
    args = parser.parse_args()
    if args.max_mib <= 0:
        parser.error("--max-mib must be positive")

    try:
        report, size_bytes, limit_bytes = build_report(args.apk, args.max_mib)
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        print(f"APK size report failed: {error}", file=sys.stderr)
        return 2

    print(report, end="")
    if args.report_file:
        args.report_file.parent.mkdir(parents=True, exist_ok=True)
        args.report_file.write_text(report, encoding="utf-8")
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with Path(summary).open("a", encoding="utf-8") as output:
            output.write(report)
    return 0 if size_bytes <= limit_bytes else 1


if __name__ == "__main__":
    raise SystemExit(main())
