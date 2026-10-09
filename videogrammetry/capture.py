"""Bounded FFmpeg acquisition with independently verifiable image outputs.

No reconstruction-engine adapter is implemented in v0.1.0.
"""
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import time
from datetime import datetime, timezone
from urllib.parse import urlsplit
from uuid import uuid4
import zipfile

from . import __version__

ENGINE_NAMES = (
    "COLMAP", "OpenMVG", "OpenMVS", "AliceVision", "Meshroom", "MicMac",
    "OpenDroneMap", "Manifold", "Assimp", "trimesh", "meshoptimizer", "MeshLab",
)


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def tool_status() -> dict:
    """Probe acquisition tools. This does NOT mean an engine is integrated."""
    result = {}
    for name in ("ffmpeg", "ffprobe"):
        path = shutil.which(name)
        result[name] = {"available": bool(path), "version": None}
        if path:
            try:
                proc = subprocess.run(
                    [path, "-version"], capture_output=True, text=True,
                    timeout=8, check=False,
                )
                if proc.returncode == 0:
                    result[name]["version"] = (proc.stdout.splitlines() or [""])[0][:160]
            except (OSError, subprocess.TimeoutExpired):
                pass
    return result


def _source_args(kind: str, value: str) -> list[str]:
    if kind == "recording":
        source = Path(value).expanduser()
        if not source.is_file():
            raise ValueError("Recording does not exist or is not a file")
        return ["-i", str(source.resolve())]
    if kind == "stream":
        parsed = urlsplit(value)
        if parsed.scheme.lower() not in {"rtsp", "rtsps", "http", "https"} or not parsed.hostname:
            raise ValueError("Stream must use a valid rtsp, rtsps, http or https URL")
        extra = ["-rtsp_transport", "tcp"] if parsed.scheme.lower() in {"rtsp", "rtsps"} else []
        return extra + ["-i", value]
    if kind == "camera":
        if not value.strip():
            raise ValueError("Camera device/name must not be blank")
        if sys.platform == "win32":
            return ["-f", "dshow", "-i", "video=" + value]
        if sys.platform.startswith("linux") and re.fullmatch(r"/dev/video[0-9]+", value):
            return ["-f", "v4l2", "-i", value]
        raise ValueError("For Windows provide a DirectShow camera name; on Linux use /dev/videoN")
    raise ValueError("Unknown source kind")


def _public_source_metadata(kind: str, value: str) -> dict:
    # No exact URLs, paths, login credentials or filenames in diagnostics.
    fingerprint = hashlib.sha256(value.encode("utf-8", errors="replace")).hexdigest()[:16]
    data = {"kind": kind, "sourceFingerprint": fingerprint}
    if kind == "recording" and Path(value).is_file():
        data["sourceBytes"] = Path(value).stat().st_size
    return data


def _safe_error(stderr: str, source: str) -> str:
    # FFmpeg can print input URLs or file paths. Never persist raw output.
    stderr = stderr.replace(source, "[SOURCE_REDACTED]")
    stderr = re.sub(r"(?i)(rtsp|rtsps|https?)://[^\s\"']+", "[URL_REDACTED]", stderr)
    stderr = re.sub(r"(?i)(password|token|key)=\S+", r"\1=[REDACTED]", stderr)
    return stderr[-1000:]


def _valid_png(path: Path) -> tuple[int, int]:
    with path.open("rb") as file:
        header = file.read(24)
        if len(header) != 24 or header[:8] != b"\x89PNG\r\n\x1a\n" or header[12:16] != b"IHDR":
            raise ValueError("Not a valid PNG header")
        width, height = struct.unpack(">II", header[16:24])
        if width == 0 or height == 0 or width > 65535 or height > 65535:
            raise ValueError("Implausible PNG dimensions")
        file.seek(-12, os.SEEK_END)
        if file.read(12)[4:] != b"IEND\xaeB\x60\x82":
            raise ValueError("PNG is not complete")
    return width, height


def _hash_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as file:
        for part in iter(lambda: file.read(1024 * 1024), b""):
            h.update(part)
    return h.hexdigest()


class EventLog:
    """Persistent structured per-run events plus a bounded rolling action trace."""
    def __init__(self, project: Path, run_dir: Path, run_id: str) -> None:
        self.project, self.run_dir, self.run_id = project, run_dir, run_id
        self.session_id = uuid4().hex
        self.started = time.monotonic()
        self.seq = 0
        self.per_run = run_dir / "events.jsonl"
        global_dir = project / "diagnostics"
        global_dir.mkdir(parents=True, exist_ok=True)
        self.global_log = global_dir / "actions.jsonl"

    def emit(self, category: str, **fields: object) -> None:
        self.seq += 1
        item = {
            "eventId": uuid4().hex,
            "sequenceNumber": self.seq,
            "timestampUtc": utc_now(),
            "monotonicTimeMs": round((time.monotonic() - self.started) * 1000),
            "appVersion": __version__,
            "appSessionId": self.session_id,
            "correlationId": self.run_id,
            "operationId": self.run_id,
            "category": category,
            **fields,
        }
        line = json.dumps(item, sort_keys=True, ensure_ascii=False) + "\n"
        for path in (self.per_run, self.global_log):
            if path == self.global_log and path.exists() and path.stat().st_size + len(line) > 2_000_000:
                previous = path.with_name("actions.previous.jsonl")
                path.replace(previous)
            with path.open("a", encoding="utf-8") as stream:
                stream.write(line)
                stream.flush()
                if category in {"OPERATION_START", "ERROR", "OPERATION_RESULT", "TEST_RESULT"}:
                    os.fsync(stream.fileno())


def extract_frames(
    project: Path,
    kind: str,
    value: str,
    interval: float = 1.0,
    max_frames: int = 120,
    duration: float | None = None,
    timeout: float = 300.0,
) -> dict:
    """Return an explicit PASS/FAIL result; never overwrite old runs."""
    if not 0.1 <= interval <= 120:
        raise ValueError("interval must be between 0.1 and 120 seconds")
    if not 1 <= max_frames <= 1000:
        raise ValueError("max_frames must be between 1 and 1000")
    if not 5 <= timeout <= 86400:
        raise ValueError("timeout must be between 5 and 86400 seconds")
    if duration is not None and not 0 < duration <= 86400:
        raise ValueError("duration must be positive and <= 86400 seconds")
    if kind != "recording" and duration is None:
        raise ValueError("live sources require an explicit --duration")
    project = Path(project).expanduser().resolve()
    project.mkdir(parents=True, exist_ok=True)
    run_id = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ") + "_" + uuid4().hex[:10]
    run_dir = project / "runs" / run_id
    frames_dir = run_dir / "frames"
    frames_dir.mkdir(parents=True, exist_ok=False)
    log = EventLog(project, run_dir, run_id)
    started = time.monotonic()
    result = {
        "runId": run_id, "status": "FAIL", "appVersion": __version__,
        "source": _public_source_metadata(kind, value), "frameCount": 0,
    }
    log.emit("USER_ACTION", requestedOperation="EXTRACT_FRAMES", sourceKind=kind)
    try:
        ffmpeg = shutil.which("ffmpeg")
        if not ffmpeg:
            raise RuntimeError("FFmpeg was not found in PATH")
        input_args = _source_args(kind, value)
        log.emit("PRECONDITION", ffmpegAvailable=True, sourceValidated=True)
        frame_rate = 1.0 / interval
        args = [
            ffmpeg, "-hide_banner", "-nostdin", "-loglevel", "error", "-n",
            *input_args,
        ]
        if duration is not None:
            args += ["-t", str(duration)]
        args += [
            "-an", "-vf", f"fps={frame_rate:.10g}", "-frames:v", str(max_frames),
            "-start_number", "1", str(frames_dir / "frame_%06d.png"),
        ]
        # Do not log the raw command; arguments can contain credentials.
        log.emit("OPERATION_START", operation="FFMPEG_EXTRACTION",
                 frameIntervalSec=interval, maxFrames=max_frames, durationLimitSec=duration,
                 timeoutSec=timeout, inputKind=kind)
        try:
            child = subprocess.Popen(
                args, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE,
            )
            try:
                _, raw_stderr = child.communicate(timeout=timeout)
            except subprocess.TimeoutExpired:
                child.kill()
                child.communicate()
                log.emit("STALL_TIMEOUT", timeoutSec=timeout)
                raise RuntimeError("Frame extraction timed out")
            except KeyboardInterrupt:
                child.kill()
                child.communicate()
                log.emit("CANCELLED", reason="KeyboardInterrupt")
                raise
        except OSError as exc:
            raise RuntimeError("Could not start FFmpeg") from exc

        if child.returncode != 0:
            detail = _safe_error(raw_stderr.decode("utf-8", "replace"), value)
            raise RuntimeError(f"FFmpeg exited with code {child.returncode}: {detail}")
        log.emit("STATE_TRANSITION", stateAfter="VALIDATING_OUTPUT")
        frames = []
        for frame in sorted(frames_dir.glob("frame_*.png")):
            w, h = _valid_png(frame)
            frames.append({
                "name": frame.name, "bytes": frame.stat().st_size,
                "sha256": _hash_file(frame), "width": w, "height": h,
            })
        if not frames:
            raise RuntimeError("FFmpeg produced zero frames")
        result["frameCount"] = len(frames)
        result["status"] = "PASS"
        result["elapsedSec"] = round(time.monotonic() - started, 3)
        manifest = {
            "schemaVersion": 1, "appVersion": __version__, "runId": run_id,
            "createdUtc": utc_now(), "source": result["source"],
            "selectionMethod": "uniform_time_sampling",
            "frameIntervalSec": interval,
            "frameTimeNote": "Sampling interval is requested, not exact per-frame source presentation timestamps",
            "frames": frames,
        }
        (run_dir / "frames_manifest.json").write_text(
            json.dumps(manifest, indent=2) + "\n", encoding="utf-8",
        )
        log.emit("OUTPUT_VALIDATION", validatedFrameCount=len(frames),
                 dimensions=[[f["width"], f["height"]] for f in frames[:3]])
        log.emit("OPERATION_RESULT", success=True, frameCount=len(frames),
                 durationMs=round((time.monotonic() - started) * 1000))
    except KeyboardInterrupt:
        result["status"] = "CANCELLED"
        result["error"] = "Capture cancelled by user"
        log.emit("OPERATION_RESULT", success=False, result="CANCELLED")
    except Exception as exc:
        result["error"] = str(exc)
        log.emit("ERROR", errorType=type(exc).__name__, errorMessage=str(exc))
        log.emit("OPERATION_RESULT", success=False, result="FAIL")
    result["elapsedSec"] = round(time.monotonic() - started, 3)
    (run_dir / "run_result.json").write_text(
        json.dumps(result, indent=2) + "\n", encoding="utf-8",
    )
    return result


def export_diagnostics(project: Path, run_id: str) -> Path:
    if not re.fullmatch(r"[0-9]{8}T[0-9]{6}Z_[a-f0-9]{10}", run_id):
        raise ValueError("Invalid run ID")
    root = Path(project).expanduser().resolve()
    run_dir = root / "runs" / run_id
    if not (run_dir / "run_result.json").is_file():
        raise FileNotFoundError("Run report does not exist")
    exports = root / "exports"
    exports.mkdir(parents=True, exist_ok=True)
    destination = exports / f"diagnostics_{run_id}.zip"
    if destination.exists():
        raise FileExistsError("Export already exists; old reports are never overwritten")
    files = ["run_result.json", "frames_manifest.json", "events.jsonl",
             "test_results.json", "test_report.txt"]
    with zipfile.ZipFile(destination, "x", compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr("README.txt", "Video Photogrammetry v0.1.0 diagnostics. No source video or frame images included.\n")
        for filename in files:
            file = run_dir / filename
            if file.is_file():
                archive.write(file, arcname=filename)
    if destination.stat().st_size == 0:
        raise RuntimeError("Empty diagnostics export")
    return destination
