#!/usr/bin/env python3
"""Drive an Android app on the emulator: dump the Compose semantics tree, tap by text, screenshot.

Compose exposes each composable's text as an accessibility node, so tapping "New routine" is
reliable without hard-coded pixel coordinates. Override ADB/PKG/SHOTS with env vars.
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ADB = os.environ.get(
    "ADB",
    f"{os.environ.get('ANDROID_HOME', os.path.expanduser('~/Android/Sdk'))}/platform-tools/adb",
)
PKG = os.environ.get("PKG", "com.example.app")
SHOTS = os.environ.get("SHOTS", "./shots")


def _serial():
    """Pin one device for every adb call.

    With the physical phone AND the emulator both attached, a bare `adb shell ...` fails with
    "more than one device/emulator" — and a script that swallows that error reports the app as
    broken. Prefer an explicit ANDROID_SERIAL; otherwise use the only device, or the emulator
    when there are several, and say which one was picked.
    """
    s = os.environ.get("ANDROID_SERIAL")
    if s:
        return s
    out = subprocess.run([ADB, "devices"], capture_output=True, text=True).stdout
    devs = [l.split()[0] for l in out.splitlines()[1:] if l.strip().endswith("device")]
    if not devs:
        raise SystemExit("no device attached: adb devices is empty")
    if len(devs) == 1:
        return devs[0]
    emu = [d for d in devs if d.startswith("emulator-")]
    if not emu:
        raise SystemExit(f"more than one device attached ({', '.join(devs)}): set ANDROID_SERIAL")
    print(f"note: {len(devs)} devices attached ({', '.join(devs)}); using {emu[0]}")
    return emu[0]


SERIAL = _serial()


def adb(*args, timeout=90):
    r = subprocess.run([ADB, "-s", SERIAL, *args], capture_output=True, text=True, timeout=timeout)
    if r.returncode != 0 and (r.stderr or "").strip():
        print(f"!! adb {' '.join(args)}: {r.stderr.strip().splitlines()[0][:140]}")
    return r


def dump(retries=4):
    """Returns [(text, x1, y1, x2, y2, clickable)] for every visible node.

    `uiautomator dump` refuses while the window is not idle (a transition, a progress bar) and
    says so on stdout — leaving the PREVIOUS dump on the device. Reading that back reports the
    last screen as if it were the current one, which looks exactly like a real app bug: a script
    would swear the app was still on Settings after a relaunch when it was on Plan. Delete the
    file first so a failed dump yields nothing, and retry until a dump actually lands.
    """
    nodes = []
    for _ in range(retries):
        adb("shell", "rm", "-f", "/sdcard/ui.xml")
        out = adb("shell", "uiautomator", "dump", "/sdcard/ui.xml").stdout or ""
        xml = adb("shell", "cat", "/sdcard/ui.xml").stdout or ""
        if "ERROR" not in out and "<hierarchy" in xml:
            nodes = _nodes(xml)
            # A well-formed tree with no nodes in it is not a screen: it happens while a popup
            # menu is open, and a caller that trusts it reports "the app shows nothing" when the
            # truth is "the harness cannot see". Retry, and shout if it never resolves.
            if nodes:
                return nodes
        time.sleep(1.5)
    print(f"!! dump(): no nodes after {retries} tries — read this as 'cannot see the screen', "
          "never as 'the screen is empty'")
    return nodes
def _nodes(xml):
    """Parse a uiautomator dump into (text, x1, y1, x2, y2, clickable) rows."""
    xml = xml[xml.find("<hierarchy"):]
    nodes = []
    try:
        root = ET.fromstring(xml)
    except ET.ParseError:
        return nodes
    for n in root.iter("node"):
        b = re.findall(r"-?\d+", n.get("bounds", ""))
        if len(b) != 4:
            continue
        nodes.append((n.get("text", ""), *map(int, b), n.get("clickable") == "true"))
    return nodes


def _tidy(text):
    """MMD marks the selected dropdown option with a bullet ("• Pounds").

    Searching for the plain label then misses exactly the option the user is looking at — which
    makes a script tap nothing, leave the popup open, and read the popup as if it were the screen.
    """
    return text.lstrip("•✓* ").strip()


def find(text, nodes=None, exact=False):
    nodes = nodes if nodes is not None else dump()
    for n in nodes:
        t = _tidy(n[0])
        if (t == text) if exact else (text.lower() in t.lower() and t):
            return n
    return None


def tap(text, exact=False, delay=1.6):
    n = find(text, exact=exact)
    if not n:
        print(f"!! not found: {text!r}")
        return False
    cx, cy = (n[1] + n[3]) // 2, (n[2] + n[4]) // 2
    adb("shell", "input", "tap", str(cx), str(cy))
    time.sleep(delay)
    print(f"tapped {text!r} at ({cx},{cy})")
    return True


def tap_xy(x, y, delay=1.6):
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(delay)


def swipe(x1, y1, x2, y2, ms=300, delay=1.2):
    adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))
    time.sleep(delay)


def tap_in_menu(label, tries=8, delay=1.2):
    """Dropdown popups fit ~6 items: scroll inside the popup until the label is reachable."""
    for _ in range(tries):
        if find(label, exact=True):
            return tap(label, exact=True)
        swipe(172, 420, 172, 120, delay=delay)
    return tap(label, exact=True)


def tap_where(pred, delay=1.6, what="node"):
    """Tap the first node satisfying pred(text, x1, y1, x2, y2) — use when texts collide.

    The predicate is handed the text with any dropdown-selection bullet stripped, so
    `t == "Pounds"` matches the chosen option (`"• Pounds"`) as well as the plain one. Callers write
    their own predicates, so tolerance has to live here — not only in find().
    """
    for n in dump():
        text, x1, y1, x2, y2 = _tidy(n[0]), n[1], n[2], n[3], n[4]
        if text and pred(text, x1, y1, x2, y2):
            cx, cy = (x1 + x2) // 2, (y1 + y2) // 2
            adb("shell", "input", "tap", str(cx), str(cy))
            time.sleep(delay)
            print(f"tapped {what} {text!r} at ({cx},{cy})")
            return True
    print(f"!! no {what} matched")
    return False


def texts(nodes=None):
    return [n[0] for n in (nodes if nodes is not None else dump()) if n[0]]


def shot(name):
    os.makedirs(SHOTS, exist_ok=True)
    out = f"{SHOTS}/{name}.png"
    with open(out, "wb") as f:
        f.write(subprocess.run([ADB, "-s", SERIAL, "exec-out", "screencap", "-p"],
                               capture_output=True).stdout)
    print("shot:", out)
    return out


def crashes():
    """Crash-buffer entries for *this* package.

    `logcat -b crash` is shared: other apps' stack traces (and the device's own services) land in
    it too, so an unfiltered scan reports failures that have nothing to do with the app under
    test. Split on the buffer's separators and keep only blocks naming the package.
    """
    out = adb("logcat", "-d", "-b", "crash").stdout or ""
    blocks, current = [], []
    for line in out.splitlines():
        if line.startswith("--------- beginning of"):
            if current:
                blocks.append(current)
            current = [line]
        else:
            current.append(line)
    blocks.append(current)
    app = [b for b in blocks if any(PKG in line for line in b)]
    hits = [line for block in app for line in block if "FATAL" in line or "Exception" in line]
    return hits[-8:]


def relaunch(pkg=None, activity=".MainActivity", wipe=False, wait=6):
    """Every flow should start here: an install kills the app and a dead app dumps nothing."""
    pkg = pkg or PKG
    if wipe:
        adb("shell", "pm", "clear", pkg)
    adb("shell", "am", "force-stop", pkg)
    adb("shell", "am", "start", "-n", f"{pkg}/{activity}")
    time.sleep(wait)


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "dump"
    if cmd == "dump":
        for n in dump():
            if n[0]:
                print(f"{n[0]!r:60} box=({n[1]},{n[2]})-({n[3]},{n[4]}) click={n[5]}")
    elif cmd == "tap":
        tap(sys.argv[2])
    elif cmd == "shot":
        shot(sys.argv[2])
    elif cmd == "crashes":
        print("\n".join(crashes()) or "none")
