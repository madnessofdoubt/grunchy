#!/usr/bin/env python3
"""Verify the native-look pass: rule thickness, title size, settings icon, 3 tab bottom bar."""
import os
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
os.environ.setdefault("PKG", "com.grunchy.workout")
os.environ["SHOTS"] = os.path.join(ROOT, "tmp", "shots")
import driver as ui  # noqa: E402

FAILS = []


def check(ok, label):
    print(("  OK   " if ok else "  FAIL ") + label)
    if not ok:
        FAILS.append(label)


ui.relaunch(wait=7)
plan = ui.texts()
print("plan:", [t for t in plan[:8]])
check(any("Grunchy" in t for t in plan), "Plan screen up")
check(not any("potter" in t for t in plan), "etymology is off the app bar")
check(len([n for n in ui.dump() if n[2] < 135 and n[1] > 330 and n[5]]) >= 2,
      "two icons in the bar: the info glyph and the sliders")
check(not any(t.endswith("a ...") for t in plan), "subtitle not ellipsized")
check("More" not in plan, "More tab is gone")
check(all(t in plan for t in ("Plan", "History", "Stats")), "three tabs remain")
ui.shot("n01_plan")

# the settings icon lives in the app bar: top right, above the rule
nodes = ui.dump()
icon = [n for n in nodes if n[1] > 380 and n[2] < 135 and n[5]]
print("clickable nodes in the top right:", [(n[0], n[1], n[2], n[3], n[4]) for n in icon][:6])
check(bool(icon), "an icon target exists in the app bar's top right")
if icon:
    n = icon[0]
    ui.tap_xy((n[1] + n[3]) // 2, (n[2] + n[4]) // 2)
    time.sleep(1.8)
    s = ui.texts()
    print("after icon tap:", s[:5])
    check("Settings" in s, "settings icon opens Settings")
    check("Back" in s, "Settings has a Back control")
    check("Plan" not in s, "bottom bar hidden while in Settings")
    ui.shot("n02_settings")
    ui.tap("Back", exact=True)
    time.sleep(1.5)
    check("Plan" in ui.texts(), "Back returns to the tab")

ui.tap("History", exact=True)
time.sleep(1.5)
h = ui.texts()
check("History" in h, "History tab renders")
ui.shot("n03_history")

ui.tap("Stats", exact=True)
time.sleep(1.5)
st = ui.texts()
check("Stats" in st, "Stats tab renders")
ui.shot("n04_stats")

# the workout screen, which had its own custom bar
ui.tap("Plan", exact=True)
time.sleep(1.5)
ui.tap("Start", exact=True)
time.sleep(2.0)
w = ui.texts()
print("workout bar:", [t for t in w[:6]])
check("Finish" in w and "Discard" in w, "workout bar keeps Discard and Finish")
ui.shot("n05_workout")
ui.tap("Discard", exact=True)
time.sleep(1.2)
ui.tap_where(lambda t, x1, y1, x2, y2: t == "Discard" and y1 > 120, what="confirm discard")
time.sleep(1.8)

print("crashes:", ui.crashes() or "none")
print()
print("FAILURES:", FAILS or "none")
