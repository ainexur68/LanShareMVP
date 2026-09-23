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


def build_report(apk: Path, max_mib: float) -> tuple[str, int, int, set[str]]:
    if not apk.is_file():
        raise ValueError(f"APK not found: {apk}")
    if not zipfile.is_zipfile(apk):
        raise ValueError(f"Not a valid APK/ZIP archive: {apk}")

    totals = {category: 0 for category in CATEGORIES}
    abis: set[str] = set()
    native_by_abi: dict[str, list[int]] = {}
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
                    sizes = native_by_abi.setdefault(parts[1], [0, 0])
                    sizes[0] += info.compress_size
                    sizes[1] += info.file_size

    size_bytes = apk.stat().st_size
    payload_bytes = sum(totals.values())
    overhead_bytes = max(0, size_bytes - payload_bytes)
    limit_bytes = int(max_mib * MIB)
    rows = [
        f"## APK: `{apk.name}`",
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
    if native_by_abi:
        rows.extend(
            [
                "",
                "| Native ABI | Compressed libraries | Uncompressed libraries |",
                "|---|---:|---:|",
            ]
        )
        for abi, (compressed_bytes, uncompressed_bytes) in sorted(native_by_abi.items()):
            rows.append(
                f"| {abi} | {compressed_bytes / MIB:.2f} MiB | {uncompressed_bytes / MIB:.2f} MiB |"
            )
    rows.extend(
        [
            "",
            "Component sizes sum compressed ZIP-entry payloads. ZIP metadata, alignment, and APK signing block are reported as overhead.",
            "",
            f"**Gate: {'PASS' if size_bytes <= limit_bytes else 'FAIL'}**",
        ]
    )
    return "\n".join(rows) + "\n", size_bytes, limit_bytes, abis


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apks", type=Path, nargs="+", help="Release APKs to inspect")
    parser.add_argument("--max-mib", type=float, default=15, help="Maximum APK size in MiB (default: 15)")
    parser.add_argument(
        "--expect-abis",
        nargs="+",
        help="Require one APK per listed native ABI and no universal APK",
    )
    parser.add_argument("--report-file", type=Path, help="Also write the report to this path")
    args = parser.parse_args()
    if args.max_mib <= 0:
        parser.error("--max-mib must be positive")

    try:
        reports = []
        size_ok = True
        abi_counts: dict[str, int] = {}
        for apk in args.apks:
            report, size_bytes, limit_bytes, abis = build_report(apk, args.max_mib)
            reports.append(report)
            size_ok = size_ok and size_bytes <= limit_bytes
            if args.expect_abis:
                if len(abis) != 1:
                    abi_counts[f"invalid:{apk.name}"] = len(abis)
                else:
                    abi = next(iter(abis))
                    abi_counts[abi] = abi_counts.get(abi, 0) + 1
    except (OSError, ValueError, zipfile.BadZipFile) as error:
        print(f"APK size report failed: {error}", file=sys.stderr)
        return 2

    overall = ["# Release APK size report", "", *[item for report in reports for item in [report, "---", ""]]]
    coverage_ok = True
    if args.expect_abis:
        expected = set(args.expect_abis)
        found = {abi for abi in abi_counts if not abi.startswith("invalid:")}
        duplicates = {abi for abi, count in abi_counts.items() if not abi.startswith("invalid:") and count != 1}
        invalid = [name.removeprefix("invalid:") for name in abi_counts if name.startswith("invalid:")]
        missing = expected - found
        unexpected = found - expected
        coverage_ok = not (duplicates or invalid or missing or unexpected)
        overall.extend(
            [
                f"**Split ABI coverage: {'PASS' if coverage_ok else 'FAIL'}**",
                f"- Expected: {', '.join(sorted(expected))}",
                f"- Found: {', '.join(sorted(found)) if found else 'none'}",
            ]
        )
        if duplicates:
            overall.append(f"- Duplicate ABI APKs: {', '.join(sorted(duplicates))}")
        if invalid:
            overall.append(f"- APKs without exactly one native ABI: {', '.join(sorted(invalid))}")
        if missing:
            overall.append(f"- Missing ABI APKs: {', '.join(sorted(missing))}")
        if unexpected:
            overall.append(f"- Unexpected ABI APKs: {', '.join(sorted(unexpected))}")
    overall.append(f"\n**Overall gate: {'PASS' if size_ok and coverage_ok else 'FAIL'}**\n")
    combined_report = "\n".join(overall)
    print(combined_report, end="")
    if args.report_file:
        args.report_file.parent.mkdir(parents=True, exist_ok=True)
        args.report_file.write_text(combined_report, encoding="utf-8")
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with Path(summary).open("a", encoding="utf-8") as output:
            output.write(combined_report)
    if not size_ok:
        return 1
    return 0 if coverage_ok else 2


if __name__ == "__main__":
    raise SystemExit(main())
