import tempfile
import unittest
import zipfile
from pathlib import Path
from contextlib import redirect_stdout
from io import StringIO
from unittest.mock import patch

from report_apk_size import build_report, main


class ApkSizeReportTest(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp_dir.cleanup)
        self.apk = Path(self.temp_dir.name) / "sample.apk"
        with zipfile.ZipFile(self.apk, "w", compression=zipfile.ZIP_STORED) as archive:
            archive.writestr("classes.dex", b"dex-bytes")
            archive.writestr("resources.arsc", b"resource-table")
            archive.writestr("res/drawable/icon.xml", b"<vector />")
            archive.writestr("assets/model.bin", bytes(range(256)) * 8)
            archive.writestr("lib/arm64-v8a/libsample.so", bytes(range(256)) * 4)
            archive.writestr("META-INF/CERT.RSA", b"signature")

    def test_report_breaks_down_payload_and_detects_abi(self):
        report, size_bytes, limit_bytes, abis = build_report(self.apk, max_mib=15)

        self.assertLessEqual(size_bytes, limit_bytes)
        self.assertEqual(abis, {"arm64-v8a"})
        self.assertIn("Native ABIs: arm64-v8a", report)
        self.assertIn("| DEX |", report)
        self.assertIn("| Resources |", report)
        self.assertIn("| Assets |", report)
        self.assertIn("| Native libraries |", report)
        self.assertIn("| ZIP/signing overhead |", report)
        self.assertIn("| arm64-v8a |", report)
        self.assertIn("Gate: PASS", report)

    def test_report_fails_when_apk_exceeds_limit(self):
        report, size_bytes, limit_bytes, _ = build_report(self.apk, max_mib=0.0005)

        self.assertGreater(size_bytes, limit_bytes)
        self.assertIn("Gate: FAIL", report)

    def test_gate_accepts_one_apk_with_both_arm_abis(self):
        with zipfile.ZipFile(self.apk, "a", compression=zipfile.ZIP_STORED) as archive:
            archive.writestr("lib/armeabi-v7a/libsample.so", bytes(range(128)))

        with patch(
            "sys.argv",
            [
                "report_apk_size.py",
                str(self.apk),
                "--max-mib",
                "15",
                "--expect-abi-set",
                "arm64-v8a",
                "armeabi-v7a",
            ],
        ), redirect_stdout(StringIO()) as output:
            result = main()

        self.assertEqual(result, 0)
        self.assertIn("Single APK ABI set: PASS", output.getvalue())

    def test_gate_rejects_extra_non_arm_abi_in_release_apk(self):
        with zipfile.ZipFile(self.apk, "a", compression=zipfile.ZIP_STORED) as archive:
            archive.writestr("lib/armeabi-v7a/libsample.so", bytes(range(128)))
            archive.writestr("lib/x86_64/libsample.so", bytes(range(128)))

        with patch(
            "sys.argv",
            [
                "report_apk_size.py",
                str(self.apk),
                "--max-mib",
                "15",
                "--expect-abi-set",
                "arm64-v8a",
                "armeabi-v7a",
            ],
        ), redirect_stdout(StringIO()) as output:
            result = main()

        self.assertEqual(result, 2)
        self.assertIn("Single APK ABI set: FAIL", output.getvalue())


if __name__ == "__main__":
    unittest.main()
