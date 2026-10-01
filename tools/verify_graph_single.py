#!/usr/bin/env python3
"""The reported crash path, reproduced: one session of an exercise, then "View as graph".

The user's phone had a single Incline Dumbbell Press session logged. With one point there is no
trend to draw, so the page must say so in words -- not draw a one-point chart, and certainly not
crash. Two cases are covered: one estimable session, and one whose every set is too long in reps
to estimate from (which leaves no points at all).

Emulator only. The app's log is saved and restored, and the script refuses to run against the phone.
"""
import json
import os
import subprocess
import sys
import time

SERIAL = "emulator-5554"
if "LD2024" in os.environ.get("ANDROID_SERIAL", "") or SERIAL != "emulator-5554":
    sys.exit("refusing to run: this script writes a log and must never touch the phone")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.environ["ANDROID_SERIAL"] = SERIAL
os.environ["PKG"] = "com.grunchy.workout"
os.environ["SHOTS"] = os.path.join(ROOT, "tmp", "shots")
sys.path.insert(0, os.path.join(ROOT, "tools"))
import driver as ui  # noqa: E402
ADB = ui.ADB

EXERCISE = "Incline Dumbbell Press"
LOCAL = os.path.join(ROOT, "tools", "single_session.json")
FAILS = []


def check(ok, label):
    print(("  OK   " if ok else "  FAIL ") + label)
    if not ok:
        FAILS.append(label)


def pull_log():
    out = subprocess.run(
        [ADB, "-s", SERIAL, "shell", "run-as", "com.grunchy.workout", "cat", "files/grunchy.json"],
        capture_output=True,
        text=True,
    ).stdout
    return json.loads(out)


PKG = "com.grunchy.workout"


def push_log(log):
    """Swap the app's log, then prove the swap landed.

    Two traps here, both of which made the first draft of this script report a false failure: a
    relative `cp` destination lands wherever run-as happens to start (so the file must be given by
    absolute path), and replacing the file under a *running* app is a coin toss (so force-stop
    first).
    """
    with open(LOCAL, "w") as fh:
        json.dump(log, fh)
    subprocess.run([ADB, "-s", SERIAL, "shell", "am", "force-stop", PKG], capture_output=True)
    subprocess.run([ADB, "-s", SERIAL, "push", LOCAL, "/data/local/tmp/next.json"],
                   capture_output=True)
    subprocess.run([ADB, "-s", SERIAL, "shell", "run-as", PKG, "cp", "/data/local/tmp/next.json",
                    f"/data/data/{PKG}/files/grunchy.json"], capture_output=True)
    back = pull_log()
    if [s["id"] for s in back["sessions"]] != [s["id"] for s in log["sessions"]]:
        raise SystemExit("the log swap did not take: run-as cp failed")


def one_session(reps, weight_kg):
    """A log holding nothing but a single session of the exercise, dated today."""
    now = int(time.time() * 1000)
    return {
        "routines": [],
        "sessions": [{
            "id": "s_single",
            "title": "Single",
            "startedAt": now - 3600_000,
            "finishedAt": now,
            "logs": [{"exerciseId": "incline_dumbbell_press",
                      "sets": [{"weightKg": weight_kg, "reps": reps},
                               {"weightKg": weight_kg - 2.5, "reps": reps + 2}]}],
        }],
        "settings": {},
    }


original = pull_log()
print(f"app log saved: {len(original['sessions'])} sessions")

try:
    for label, reps, weight, expected in [
        ("one estimable session", 8, 30.0, "There is one session in this timespan"),
        ("one session, every set past twelve reps", 20, 30.0, "There is no chart for this timespan"),
    ]:
        print(f"\n--- {label} ---")
        push_log(one_session(reps, weight))
        ui.relaunch(wait=9)
        ui.tap_where(lambda t, x1, y1, x2, y2: t == "Stats")
        time.sleep(2.2)
        stats = ui.texts()
        check(any(EXERCISE in t for t in stats), "Stats selects the only exercise there is")
        check("View as graph" in stats, "the button is there")
        ui.tap_where(lambda t, x1, y1, x2, y2: t == "View as graph")
        time.sleep(2.6)
        texts = ui.texts()
        print("   graph:", [t[:70] for t in texts[:5]])
        check(any(expected in t for t in texts), f"the page says {expected!r}")
        check(not any("Estimated 1RM from your" in t for t in texts), "no chart caption without a chart")
        check(not ui.crashes(), "no crash")
        ui.shot("r_g_single_" + ("plot" if reps == 8 else "empty"))
finally:
    push_log(original)
    ui.relaunch(wait=6)

print("\nFAILURES:", FAILS or "none")
