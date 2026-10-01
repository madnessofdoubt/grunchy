# Verification harnesses

The app was built and checked against a real device, not just compiled. These scripts drive the
running app over `adb` by reading Compose's accessibility tree, so they assert on what the screen
actually says rather than on pixels.

They were written against the **Mudita Kompakt** and its emulator AVD (480x800 @ 213 dpi). Any
device will do for most of them, but the layout-sensitive ones assume that viewport.

## Setup

```bash
export ANDROID_SERIAL=emulator-5554     # or the phone's serial
export ANDROID_HOME=$HOME/Android/Sdk   # driver looks here for adb
```

`driver.py` finds `adb` at `$ANDROID_HOME/platform-tools/adb`, prefers the emulator when several
devices are attached, and prints which serial it chose.

## Running

```bash
python3 tools/verify_graph.py          # the Stats graph: timespans, captions, pixel ink
python3 tools/verify_graph_single.py   # the one-session and no-estimable-set edge cases
python3 tools/verify_native.py         # the measured Mudita chrome: rule, app bar, title
python3 tools/verify_home.py           # the Plan tab and the About card
python3 tools/verify_info.py           # the info card's contents
```

Each prints `FAILURES: none` on success and exits non-zero otherwise. They are not unit tests:
they need the app installed and running, and they tap and swipe the real UI.

`seed_history.py` writes a synthetic multi-month log into the app's data file so the trend and
graph screens have something to draw. **It overwrites the app's history** — point it at an
emulator, never at a phone holding a log you care about:

```bash
python3 tools/seed_history.py
```

## A note on the numbers they assert

Expectations are derived from the app's own data file, not typed into the test. The graph checks
read `files/grunchy.json` out of the running app and recompute what the screen should show, so the
suite keeps working as the log grows and the unit changes - and it doubles as an independent check
of the app's own rule.
