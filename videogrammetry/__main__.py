"""User-invocable v0.1.0 command-line preview and guided testing."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
from uuid import uuid4

from . import __version__
from .capture import ENGINE_NAMES, EventLog, extract_frames, export_diagnostics, tool_status


def print_json(item: object) -> None:
    print(json.dumps(item, indent=2))


def test_version(project: Path, recording: str) -> int:
    """Practical guide with objective checks and explicit manual failure control."""
    print("TEST THIS VERSION — v" + __version__)
    print("1. Dependency preflight: ffmpeg must be available.")
    environment = tool_status()
    if not environment["ffmpeg"]["available"]:
        print("FAIL: FFmpeg is missing. Install FFmpeg and ensure it is in PATH.")
        return 1
    print("2. Recorded-video extraction: verify actual image files and manifest.")
    result = extract_frames(project, "recording", recording, interval=1.0, max_frames=24)
    run = project.expanduser().resolve() / "runs" / result["runId"]
    audit = EventLog(project.expanduser().resolve(), run, result["runId"])
    test_session_id = uuid4().hex
    audit.emit("TEST_STARTED", testId="v0.1.0_recorded_video", testSessionId=test_session_id)
    steps = [{"stepId": "dependency", "status": "PASS", "source": "AUTO_VERIFIED"}]
    if result["status"] != "PASS":
        steps.append({"stepId": "recording", "status": "FAIL", "source": "AUTO_FAIL",
                      "reason": result.get("error", "unknown")})
        verdict = "FAIL"
    else:
        manifest = json.loads((run / "frames_manifest.json").read_text(encoding="utf-8"))
        frames = list((run / "frames").glob("frame_*.png"))
        actual = len(frames)
        ok = bool(actual) and actual == result["frameCount"] == len(manifest["frames"])
        steps.append({"stepId": "recording", "status": "PASS" if ok else "FAIL",
                      "source": "AUTO_VERIFIED" if ok else "AUTO_FAIL",
                      "observedFrames": actual})
        if not ok:
            verdict = "FAIL"
        else:
            print(f"PASS: {actual} nonempty complete PNG frames recorded.")
            print("3. Inspect the extracted frames in:", run / "frames")
            print("Do the images show useful views of the intended object? [y/n]")
            print("Type n if anything visibly failed, even when files exist.")
            try:
                answer = input("Your result (y/n): ").strip().lower()
            except EOFError:
                answer = ""
            manual_pass = answer in {"y", "yes"}
            steps.append({
                "stepId": "visual_review",
                "status": "PASS" if manual_pass else ("FAIL" if answer else "UNTESTED"),
                "source": "MANUAL_PASS" if manual_pass else ("MANUAL_FAIL" if answer else "UNTESTED"),
            })
            verdict = "PASS" if manual_pass else ("FAIL" if answer else "PARTIAL")
    for step in steps:
        audit.emit("TEST_VERIFICATION", testSessionId=test_session_id, testStepId=step["stepId"],
                   result=step["status"], resultSource=step["source"])
    audit.emit("TEST_RESULT", testSessionId=test_session_id, result=verdict,
               testId="v0.1.0_recorded_video")
    test_result = {
        "testSessionId": test_session_id,
        "testId": "v0.1.0_recorded_video", "runId": result["runId"],
        "appVersion": __version__, "status": verdict, "steps": steps,
        "note": "Test PASS is not a USER VERIFIED BASELINE until user explicitly confirms it.",
    }
    (run / "test_results.json").write_text(json.dumps(test_result, indent=2) + "\n", encoding="utf-8")
    (run / "test_report.txt").write_text(
        "v0.1.0 Test This Version\n" + "\n".join(
            f"{step['stepId']}: {step['status']} ({step['source']})" for step in steps
        ) + "\nOverall: " + verdict + "\n", encoding="utf-8",
    )
    try:
        archived = export_diagnostics(project, result["runId"])
        print("Export Test + Diagnostics:", archived)
    except Exception as exc:
        print("DIAGNOSTICS EXPORT FAILED:", type(exc).__name__, file=sys.stderr)
        return 1
    print("Test result:", verdict)
    return 0 if verdict == "PASS" else 1


def main() -> int:
    parser = argparse.ArgumentParser(prog="videogrammetry", description="Local-first multi-engine photogrammetry lab — v0.1.0 capture candidate")
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("check", help="Check local acquisition dependency availability")
    sub.add_parser("engines", help="List PLANNED adapters (not operational yet)")
    capture = sub.add_parser("extract", help="Extract PNG frames from a recording or bounded live camera/stream")
    capture.add_argument("--project", type=Path, required=True)
    sources = capture.add_mutually_exclusive_group(required=True)
    sources.add_argument("--recording", help="Existing video recording")
    sources.add_argument("--stream", help="Reachable RTSP/HTTP live video URL")
    sources.add_argument("--camera", help="Windows DirectShow camera name, or Linux /dev/videoN")
    capture.add_argument("--interval", type=float, default=1.0, help="Seconds between selected video frames")
    capture.add_argument("--max-frames", type=int, default=120)
    capture.add_argument("--duration", type=float, help="Bounded capture duration (required for live inputs)")
    capture.add_argument("--timeout", type=float, default=300.0)
    guide = sub.add_parser("test-this-version", help="Guided recorded-video test with manual failure control")
    guide.add_argument("--project", type=Path, required=True)
    guide.add_argument("--recording", required=True)
    exporter = sub.add_parser("export-diagnostics", help="Export redacted diagnostics without original images")
    exporter.add_argument("--project", type=Path, required=True)
    exporter.add_argument("--run-id", required=True)
    opts = parser.parse_args()
    if opts.command == "check":
        print_json(tool_status())
        return 0
    if opts.command == "engines":
        print_json({name: {"integrationStatus": "NOT_IMPLEMENTED",
                           "runStatus": "NOT_TESTED"} for name in ENGINE_NAMES})
        return 0
    if opts.command == "test-this-version":
        return test_version(opts.project, opts.recording)
    if opts.command == "export-diagnostics":
        try:
            print(export_diagnostics(opts.project, opts.run_id))
            return 0
        except Exception as exc:
            print(f"FAIL: {type(exc).__name__}: {exc}", file=sys.stderr)
            return 1
    if opts.command == "extract":
        kind, value = next(
            (kind, value) for kind, value in (
                ("recording", opts.recording), ("stream", opts.stream),
                ("camera", opts.camera),
            ) if value is not None
        )
        try:
            result = extract_frames(
                opts.project, kind, value, opts.interval,
                opts.max_frames, opts.duration, opts.timeout,
            )
        except (ValueError, OSError) as exc:
            print(f"FAIL: {exc}", file=sys.stderr)
            return 2
        print_json(result)
        return 0 if result["status"] == "PASS" else 1
    return 2


if __name__ == "__main__":
    sys.exit(main())
