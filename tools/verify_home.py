#!/usr/bin/env python3
"""The rearranged home page: no recent sessions, and an about box behind the ⓘ."""
import os
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.environ["ANDROID_SERIAL"] = "emulator-5554"
os.environ["PKG"] = "com.grunchy.workout"
os.environ["SHOTS"] = os.path.join(ROOT, "tmp", "shots")
sys.path.insert(0, os.path.join(ROOT, "tools"))
import driver as ui  # noqa: E402

ABOUT = ("Grunchy, from the bulgarian \"грънчар\" [ɡrɤnt͡ʃar] - a potter, is a workout "
         "tracker app built for the Mudita Kompakt.")
FAILS = []


def check(ok, label):
    print(("  OK   " if ok else "  FAIL ") + label)
    if not ok:
        FAILS.append(label)


ui.relaunch(wait=8)

plan = ui.texts()
print("plan:", plan)
check("Grunchy" in plan, "Plan screen up")
check("MY ROUTINES" in plan, "routines are still listed")
check("Recent sessions" not in plan, "recent sessions are gone from Plan")
check("You haven't finished any routines yet" not in plan, "its empty-state note went with it")
ui.shot("r01_plan_no_recent")

# ⓘ is the left-hand of the two app-bar icons.
icons = sorted([n for n in ui.dump() if n[2] < 135 and n[1] > 330 and n[5]], key=lambda n: n[1])
check(len(icons) >= 2, "two app-bar icons present")
if icons:
    ui.tap_xy((icons[0][1] + icons[0][3]) // 2, (icons[0][2] + icons[0][4]) // 2)
    time.sleep(2.0)
    shown = ui.texts()
    print("after ⓘ:", shown)
    check(ABOUT in shown, f"the ⓘ shows exactly: {ABOUT!r}")
    check(any("грънчар" in t for t in shown), "the Cyrillic renders as letters")
    # Compare against the expected string itself, never a second copy of its wording: this check
    # broke the day the romanisation became IPA, because it hard-coded the old spelling.
    check(not any("potter" in t and t != ABOUT for t in shown), "only the one sentence in the box")
    ui.shot("r02_about_box")
    ui.tap_where(lambda t, x1, y1, x2, y2: t == "Close")
    time.sleep(1.8)
    check("MY ROUTINES" in ui.texts(), "Close puts the page back")

    # Settings must no longer carry an About section.
    ui.tap_xy((icons[-1][1] + icons[-1][3]) // 2, (icons[-1][2] + icons[-1][4]) // 2)
    time.sleep(2.6)
    settings = ui.texts()
    check("Settings" in settings, "the right-hand icon still opens Settings")
    check("About" not in settings, "the About section left Settings")
    for _ in range(6):
        if "Heaviest weight in the list" in ui.texts():
            break
        ui.swipe(240, 520, 240, 300, ms=250)
        time.sleep(1.2)
    tail = ui.texts()
    print("settings tail:", tail[-6:])
    check("About" not in tail, "About is gone from the bottom of Settings too")
    check("UI library" not in tail, "the UI library row went with it")
    ui.shot("r03_settings_no_about")

# History still has the sessions that Plan no longer shows.
ui.tap("Back", exact=True)
time.sleep(1.5)
ui.tap_where(lambda t, x1, y1, x2, y2: t == "History")
time.sleep(2.0)
hist = ui.texts()
print("history:", hist[:8])
check("History" in hist and len(hist) > 4, "History still lists finished sessions")
ui.shot("r04_history")

print("crashes:", ui.crashes() or "none")
print("FAILURES:", FAILS or "none")
