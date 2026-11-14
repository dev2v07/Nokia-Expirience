from __future__ import annotations

import subprocess
import os
import shutil
from pathlib import Path


ADB_DEFAULT = Path(os.environ.get("NOKIA_ADB", shutil.which("adb") or "adb"))
GRADLE_DEFAULT = Path(os.environ.get("NOKIA_GRADLE", shutil.which("gradle") or "gradle"))
DEFAULT_SERIAL = os.environ.get("NOKIA_B_SERIAL", "")


def run_command(args: list[str], cwd: Path | None = None, timeout: int = 120) -> tuple[int, str]:
    try:
        process = subprocess.run(args, cwd=cwd, capture_output=True, text=True, timeout=timeout, encoding="utf-8", errors="replace")
        return process.returncode, (process.stdout + process.stderr).strip()
    except Exception as exc:
        return 1, str(exc)


def adb_args(adb: Path, serial: str, *args: str) -> list[str]:
    return [str(adb)] + (["-s", serial] if serial else []) + list(args)


def probe_device(serial: str = DEFAULT_SERIAL, adb: Path = ADB_DEFAULT) -> str:
    code, output = run_command(adb_args(adb, serial, "shell", "getprop", "ro.product.model"))
    if code:
        return f"ADB error: {output}"
    _, version = run_command(adb_args(adb, serial, "shell", "getprop", "ro.build.version.release"))
    _, api = run_command(adb_args(adb, serial, "shell", "getprop", "ro.build.version.sdk"))
    return f"Connected: {output} · Android {version} · API {api} · {serial or 'auto-selected device'}"


def device_info(serial: str = DEFAULT_SERIAL, adb: Path = ADB_DEFAULT) -> dict[str, str]:
    result = {"serial": serial, "model": "", "android": "", "api": "", "size": "", "density": "", "online": "false"}
    for key, prop in (("model", "ro.product.model"), ("android", "ro.build.version.release"), ("api", "ro.build.version.sdk")):
        code, value = run_command(adb_args(adb, serial, "shell", "getprop", prop))
        if code:
            return result
        result[key] = value.strip()
    for key, command in (("size", ["wm", "size"]), ("density", ["wm", "density"])):
        code, value = run_command(adb_args(adb, serial, "shell", *command))
        if not code:
            result[key] = value.splitlines()[-1].strip()
    result["online"] = "true"
    return result


def export_project(project, android_root: Path) -> tuple[bool, str]:
    targets = [android_root / "studio" / "project.json", android_root / "app" / "src" / "main" / "assets" / "studio" / "project.json"]
    for target in targets:
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            target.with_suffix(target.suffix + ".bak").write_bytes(target.read_bytes())
        target.write_text(project.to_json(), encoding="utf-8")
    project.path = targets[0]
    return True, f"Exported managed config to {targets[0]} and runtime asset"


def build_apk(android_root: Path) -> tuple[int, str]:
    gradle = android_root / "gradlew.bat"
    if not gradle.exists():
        gradle = GRADLE_DEFAULT
    if not gradle.exists():
        return 1, "Neither gradlew.bat nor configured Gradle 9.5 was found"
    return run_command([str(gradle), ":app:assembleDebug"], cwd=android_root, timeout=600)


def install_apk(android_root: Path, serial: str) -> tuple[int, str]:
    apk = android_root / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    if not apk.exists():
        return 1, f"APK not found: {apk}"
    return run_command(adb_args(ADB_DEFAULT, serial, "install", "-r", "--no-streaming", str(apk)), timeout=180)


def capture_screenshot(serial: str, output: Path, adb: Path = ADB_DEFAULT) -> tuple[int, str]:
    try:
        result = subprocess.run(adb_args(adb, serial, "exec-out", "screencap", "-p"), capture_output=True, timeout=30)
        if result.returncode:
            return result.returncode, result.stderr.decode("utf-8", "replace")
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(result.stdout)
        return 0, str(output)
    except Exception as exc:
        return 1, str(exc)
