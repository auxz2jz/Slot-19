import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import zipfile

from videogrammetry.capture import (
    _safe_error, _source_args, extract_frames, export_diagnostics, tool_status,
)


class CaptureContractTests(unittest.TestCase):
    def test_reject_unsupported_stream_and_absent_recording(self):
        with self.assertRaises(ValueError):
            _source_args("stream", "file:///etc/passwd")
        with self.assertRaises(ValueError):
            _source_args("stream", "ftp://example.org/stream")
        with self.assertRaises(ValueError):
            _source_args("recording", "/not/a/real/recording.mp4")

    def test_redacts_stream_url_credentials(self):
        secret = "rtsp://operator:secret123@example.org/live"
        err = _safe_error("Could not open " + secret, secret)
        self.assertNotIn("secret123", err)
        self.assertNotIn(secret, err)

    def test_invalid_sampling_parameters_do_not_create_run(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(ValueError):
                extract_frames(Path(tmp), "stream", "rtsp://camera/live", interval=0.0, duration=10)
            self.assertFalse((Path(tmp) / "runs").exists())

    @unittest.skipUnless(shutil.which("ffmpeg"), "ffmpeg missing")
    def test_real_file_extraction_and_diagnostics(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            video = root / "original.mp4"
            proc = subprocess.run([
                "ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "lavfi",
                "-i", "testsrc=size=128x96:rate=10:duration=3",
                "-c:v", "mpeg4", "-q:v", "3", "-y", str(video)
            ], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)
            self.assertEqual(proc.returncode, 0, proc.stderr.decode(errors="replace")[-500:])
            result = extract_frames(root / "project", "recording", str(video),
                                    interval=1, max_frames=10, timeout=30)
            self.assertEqual(result["status"], "PASS", result)
            self.assertGreater(result["frameCount"], 0)
            run = root / "project" / "runs" / result["runId"]
            manifest = json.loads((run / "frames_manifest.json").read_text())
            self.assertEqual(len(manifest["frames"]), result["frameCount"])
            self.assertTrue(all(len(f["sha256"]) == 64 and f["width"] == 128 for f in manifest["frames"]))
            events = (run / "events.jsonl").read_text()
            self.assertIn("OUTPUT_VALIDATION", events)
            self.assertIn('"success": true', events)
            z = export_diagnostics(root / "project", result["runId"])
            with zipfile.ZipFile(z) as archive:
                self.assertIn("run_result.json", archive.namelist())
                self.assertNotIn("original.mp4", archive.namelist())
            with self.assertRaises(FileExistsError):
                export_diagnostics(root / "project", result["runId"])

    @unittest.skipUnless(shutil.which("ffmpeg"), "ffmpeg missing")
    def test_bad_video_fails_with_logged_first_error(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            bad = root / "bad.mp4"
            bad.write_bytes(b"this is not a video")
            result = extract_frames(root / "project", "recording", str(bad), timeout=30)
            self.assertEqual(result["status"], "FAIL")
            log = (root / "project" / "runs" / result["runId"] / "events.jsonl").read_text()
            self.assertIn('"category": "ERROR"', log)
            self.assertNotIn(str(bad), log)
            self.assertEqual(result["frameCount"], 0)

    @unittest.skipUnless(shutil.which("ffmpeg"), "ffmpeg missing")
    def test_guided_test_reports_correlated_results(self):
        from unittest.mock import patch
        from videogrammetry.__main__ import test_version

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            video = root / "guide.mp4"
            completed = subprocess.run([
                "ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "lavfi",
                "-i", "testsrc=size=80x60:rate=10:duration=2",
                "-c:v", "mpeg4", "-q:v", "3", "-y", str(video)
            ], stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30)
            self.assertEqual(completed.returncode, 0)
            project = root / "project"
            with patch("builtins.input", return_value="y"):
                self.assertEqual(test_version(project, str(video)), 0)
            run_dir = next((project / "runs").iterdir())
            result = json.loads((run_dir / "test_results.json").read_text())
            self.assertEqual(result["status"], "PASS")
            self.assertTrue(result["testSessionId"])
            events = (run_dir / "events.jsonl").read_text()
            self.assertIn('"category": "TEST_RESULT"', events)
            self.assertIn('"category": "OPERATION_RESULT"', events)
            archive = next((project / "exports").glob("*.zip"))
            with zipfile.ZipFile(archive) as bundle:
                self.assertIn("test_results.json", bundle.namelist())
                self.assertIn("events_after_export.jsonl", bundle.namelist())
                self.assertIn('"category": "OPERATION_RESULT"',
                              bundle.read("events_after_export.jsonl").decode())


if __name__ == "__main__":
    unittest.main()
