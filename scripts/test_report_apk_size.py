import tempfile
import unittest
import zipfile
from pathlib import Path

from report_apk_size import build_report


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
        report, size_bytes, limit_bytes = build_report(self.apk, max_mib=15)

        self.assertLessEqual(size_bytes, limit_bytes)
        self.assertIn("Native ABIs: arm64-v8a", report)
        self.assertIn("| DEX |", report)
        self.assertIn("| Resources |", report)
        self.assertIn("| Assets |", report)
        self.assertIn("| Native libraries |", report)
        self.assertIn("Gate: PASS", report)

    def test_report_fails_when_apk_exceeds_limit(self):
        report, size_bytes, limit_bytes = build_report(self.apk, max_mib=0.0005)

        self.assertGreater(size_bytes, limit_bytes)
        self.assertIn("Gate: FAIL", report)


if __name__ == "__main__":
    unittest.main()
