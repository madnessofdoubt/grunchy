#!/usr/bin/env python3
"""Seed the emulator with months of history, so the graph's timespans actually differ.

Only ever pointed at the emulator: the phone's data is the user's real log.
"""
ADB = os.environ.get("ADB", os.path.join(os.environ.get("ANDROID_HOME", os.path.expanduser("~/Android/Sdk")), "platform-tools", "adb"))
import json
import os
import tempfile
import subprocess
import sys
from datetime import datetime, timezone

SERIAL = "emulator-5554"
PKG = "com.grunchy.workout"
LOCAL = os.path.join(tempfile.gettempdir(), "grunchy_seed.json")
SEEDED = os.path.join(tempfile.gettempdir(), "grunchy_seeded.json")

# (date, weight kg, reps) — a plausible year of squatting, ending where the real log picks up.
HISTORY = [
    ("2025-08-15", 60.0, 5),
    ("2025-09-19", 62.5, 5),
    ("2025-10-17", 65.0, 5),
    ("2025-11-14", 67.5, 5),
    ("2025-12-12", 70.0, 5),
    ("2026-01-16", 72.5, 5),
    ("2026-02-13", 72.5, 6),
    ("2026-03-20", 75.0, 5),
    ("2026-04-17", 77.5, 4),
    ("2026-05-15", 77.5, 5),
    # Every set over 12 reps: no estimate, so this one must be reported as skipped, not plotted.
    ("2026-06-19", 40.0, 20),
    ("2026-07-17", 80.0, 3),
]


def adb(*args):
    return subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, text=True)


def main():
    data = json.load(open(LOCAL))
    known = {s["id"] for s in data["sessions"]}
    for date, weight, reps in HISTORY:
        sid = f"seed-squat-{date}"
        if sid in known:
            continue
        ts = int(datetime.fromisoformat(date + "T12:00:00+00:00").timestamp() * 1000)
        data["sessions"].append({
            "id": sid,
            "title": "Back Squat day",
            "startedAt": ts,
            "finishedAt": ts + 45 * 60 * 1000,
            "logs": [{"exerciseId": "back_squat",
                      "sets": [{"weightKg": weight, "reps": reps},
                               {"weightKg": weight, "reps": reps + 1}]}],
        })
    data["sessions"].sort(key=lambda s: s["finishedAt"])
    json.dump(data, open(SEEDED, "w"), indent=2)

    adb("shell", "am", "force-stop", PKG)
    adb("push", SEEDED, "/data/local/tmp/g_seeded.json")
    print(adb("shell", "run-as", PKG, "cp", "/data/local/tmp/g_seeded.json",
              f"/data/data/{PKG}/files/grunchy.json").stderr.strip() or "copied")
    adb("shell", "am", "start", "-n", f"{PKG}/.MainActivity")
    print(f"sessions now: {len(data['sessions'])}")


if __name__ == "__main__":
    sys.exit(main())
