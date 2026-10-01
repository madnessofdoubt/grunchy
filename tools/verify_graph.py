#!/usr/bin/env python3
"""The graph page: reachable from Stats, actually drawn, and the timespan changes what it shows.

Every expected number is counted from the app's own log rather than written down here, so the test
does not rot when the log grows a session or the unit changes — which is exactly how it started
lying the first time it was run twice.
"""
import json
import os
import re
import subprocess
import sys
import time
from datetime import date, datetime, timedelta

SERIAL = os.environ.get("ANDROID_SERIAL", "emulator-5554")
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.environ["ANDROID_SERIAL"] = SERIAL
os.environ["PKG"] = "com.grunchy.workout"
SHOTS = os.path.join(ROOT, "tmp", "shots")
os.environ["SHOTS"] = SHOTS
sys.path.insert(0, os.path.join(ROOT, "tools"))
import driver as ui  # noqa: E402
ADB = ui.ADB

LIBRARY = os.path.join(ROOT, "app", "src", "main", "java", "com", "grunchy", "workout", "data",
                       "ExerciseLibrary.kt")
MAX_REPS = 12  # mirrors GraphSeries.MaxRepsForE1rm
WINDOWS = [("Last 3 months", 92), ("Last year", 366), ("All time", None)]

FAILS = []


def check(ok, label):
    print(("  OK   " if ok else "  FAIL ") + label)
    if not ok:
        FAILS.append(label)


def dark_pixels(png, x0, y0, x1, y1, threshold=128):
    """Count ink in a rectangle: a chart is a picture, and the node dump cannot see it."""
    raw = subprocess.run(
        ["ffmpeg", "-v", "quiet", "-i", png, "-f", "rawvideo", "-pix_fmt", "gray", "-"],
        capture_output=True,
    ).stdout
    return sum(1 for y in range(y0, y1) for x in range(x0, x1) if raw[y * 480 + x] < threshold)


def library_ids():
    """name -> id, straight out of the app's own exercise library.

    Stats opens on whatever was trained last, which any other script on this emulator can change.
    Reading the exercise off the screen and looking its id up here keeps this suite honest about
    *that* exercise instead of assuming one — the same state assumption that made the pounds probe
    report a phantom failure once its world moved under it.
    """
    src = open(LIBRARY).read()
    return {name: eid for eid, name in re.findall(r'Exercise\("([^"]+)", "([^"]+)"', src)}


def app_log():
    out = subprocess.run(
        [ADB, "-s", SERIAL, "shell", "run-as", "com.grunchy.workout", "cat", "files/grunchy.json"],
        capture_output=True,
        text=True,
    ).stdout
    return json.loads(out)


def expect(log, days, exercise_id):
    """What a window should hold, counted straight from the stored log.

    Mirrors graphPoints(): one point per session that trained the exercise (a session counts if any
    of its logs for that exercise has sets), and a point carries an estimate only when some set was
    light enough in reps and heavy enough to estimate from. Returns (plotted, skipped).
    """
    cutoff = None
    if days is not None:
        cutoff = datetime.combine(date.today() - timedelta(days=days - 1), datetime.min.time())
    plotted = skipped = 0
    for s in log["sessions"]:
        logs = [x for x in s["logs"] if x["exerciseId"] == exercise_id and x["sets"]]
        if not logs:
            continue
        if cutoff is not None and datetime.fromtimestamp(s["finishedAt"] / 1000) < cutoff:
            continue
        if any(st["reps"] <= MAX_REPS and st["weightKg"] > 0 for st in logs[0]["sets"]):
            plotted += 1
        else:
            skipped += 1
    return plotted, skipped


def caption(texts):
    """Find the caption by its words, never by its separator: the copy review replaced the "·"
    with a comma, and a test that matched on punctuation reported an app failure instead."""
    return next((t for t in texts if "Estimated 1RM from your" in t), "")


def pick(text):
    ui.tap_where(lambda t, x1, y1, x2, y2: t == text)
    time.sleep(2.4)


log = app_log()
unit = "lbs" if log["settings"].get("weightUnit") == "LB" else "kg"
print(f"log on {SERIAL}: {len(log['sessions'])} sessions, unit {unit}")

ui.relaunch(wait=9)
ui.tap_where(lambda t, x1, y1, x2, y2: t == "Stats")
time.sleep(2.2)

stats = ui.texts()
ids = library_ids()
check("Stats" in stats, "Stats screen up")   # the bar title, not a section label
selected = next((t for t in stats if t in ids), None)
check(selected is not None, "Stats shows an exercise with history")
if selected is None:
    print("FAILURES: cannot continue without knowing which exercise is on screen")
    raise SystemExit(1)
exercise_id = ids[selected]
print(f"Stats is on {selected!r} ({exercise_id})")
check("View as graph" in stats, "the button is on the screen")

pick("View as graph")
graph = ui.texts()
check(any(selected in t for t in graph), "graph page is titled with the exercise")
check("TIMESPAN" in graph, "timespan control present")
check("Last 3 months" in graph, "it opens on a three-month window")

shot3 = ui.shot("r_g1_three_months")
plotted, skipped = expect(log, 92, exercise_id)
cap3 = caption(graph)
print(f"3 months: log says {plotted} estimable of {plotted + skipped}; caption {cap3!r}")
check(cap3.startswith(f"Estimated 1RM from your {plotted} sessions"),
      "three months plots the estimable sessions in the window")
check(cap3.rstrip(".").endswith(f"in {unit}"), f"the caption names the unit ({unit})")
if skipped:
    check(any(f"{skipped} of {plotted + skipped} sessions" in t for t in graph),
          "the unplottable sessions are counted out loud")

# The chart is a picture: assert ink inside the box, between the control and the caption.
nodes = ui.dump()
control = next((n for n in nodes if n[0] == "Last 3 months"), None)
capnode = next((n for n in nodes if "Estimated 1RM from your" in n[0]), None)
if control and capnode:
    ink = dark_pixels(shot3, 10, control[4] + 6, 470, capnode[2] - 4)
    print(f"chart band y={control[4] + 6}..{capnode[2] - 4}, ink={ink} px")
    check(ink > 400, "the chart area has a line drawn in it, not an empty box")
else:
    check(False, "could not locate the chart band from the dump")

# Widening the window has to show more, or the selector is decoration.
previous = "Last 3 months"
plotted_all = 0
for label, days in WINDOWS[1:]:
    pick(previous)
    pick(label)
    texts = ui.texts()
    cap = caption(texts)
    plotted, skipped = expect(log, days, exercise_id)
    plotted_all = max(plotted_all, plotted)
    print(f"{label}: log says {plotted} estimable + {skipped} unplottable; caption {cap!r}")
    check(cap.startswith(f"Estimated 1RM from your {plotted} "),
          f"{label} plots the estimable sessions in the window")
    if skipped:
        note = next((t for t in texts if "reps or fewer" in t), "")
        print(f"   note: {note[:100]!r}")
        check(re.search(rf"{skipped} of (?:the )?{plotted + skipped} sessions", note),
              f"{label} counts the {skipped} unplottable session(s) out loud")
    ui.shot(f"r_g_{label.replace(' ', '_').lower()}")
    previous = label

# The very first session on record must be inside the widest window: proof the range reaches back.
first = min(s["finishedAt"] for s in log["sessions"]
            if any(x["exerciseId"] == exercise_id and x["sets"] for x in s["logs"]))
oldest = datetime.fromtimestamp(first / 1000).strftime("%b %y")
check(plotted_all == expect(log, None, exercise_id)[0],
      f"all time reaches back to the first session ({oldest})")

check(not ui.crashes(), "no crashes")
ui.tap_where(lambda t, x1, y1, x2, y2: t == "Back")
time.sleep(2.0)
check(any("View as graph" in t for t in ui.texts()), "Back returns to Stats")
print("FAILURES:", FAILS or "none")
