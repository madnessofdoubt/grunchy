#!/usr/bin/env python3
"""The etymology is off the app bar, behind the ⓘ — and comes back when asked."""
import os
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.environ["ANDROID_SERIAL"] = "emulator-5554"
os.environ["PKG"] = "com.grunchy.workout"
os.environ["SHOTS"] = os.path.join(ROOT, "tmp", "shots")
sys.path.insert(0, os.path.join(ROOT, "tools"))
import driver as ui  # noqa: E402

FAILS = []


def check(ok, label):
    print(("  OK   " if ok else "  FAIL ") + label)
    if not ok:
        FAILS.append(label)


ui.relaunch(wait=8)

plan = ui.texts()
print("plan:", [t for t in plan[:5]])
check("Grunchy" in plan, "the title is still there")
check(not any("potter" in t or "грънчар" in t for t in plan), "etymology no longer on the plan screen")
ui.shot("i01_plan_no_subtitle")

# Both bar icons are clickable nodes with no text: the ⓘ is the left-hand one.
nodes = ui.dump()
icons = sorted([n for n in nodes if n[2] < 135 and n[1] > 330 and n[5]], key=lambda n: n[1])
print("clickable app-bar targets (text, x1, y1, x2, y2):",
      [(n[0], n[1], n[2], n[3], n[4]) for n in icons])
check(len(icons) >= 2, "two icon targets in the app bar (ⓘ and settings)")

if icons:
    info = icons[0]
    ui.tap_xy((info[1] + info[3]) // 2, (info[2] + info[4]) // 2)
    time.sleep(1.8)
    shown = ui.texts()
    print("after tapping the left icon:", shown[:8])
    check(any("potter" in t for t in shown), "the ⓘ shows the etymology")
    check(any("грънчар" in t for t in shown), "the Cyrillic is there (system font, not tofu)")
    check("Close" in shown, "there is a way to dismiss it")
    ui.shot("i02_etymology_card")

    ui.tap_where(lambda t, x1, y1, x2, y2: t == "Close")
    time.sleep(1.6)
    after = ui.texts()
    print("after Close:", after[:5])
    check("Grunchy" in after and not any("potter" in t for t in after), "Close puts the page back")

    # The right-hand icon must still be the settings one.
    ui.tap_xy((icons[1][1] + icons[1][3]) // 2, (icons[1][2] + icons[1][4]) // 2)
    time.sleep(2.6)
    settings = ui.texts()
    print("after tapping the right icon:", settings[:5])
    check("Settings" in settings, "the right-hand icon still opens Settings")

print("crashes:", ui.crashes() or "none")
print("FAILURES:", FAILS or "none")
