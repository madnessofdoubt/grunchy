## Grunchy — workout tracker for the Mudita Kompakt.

Designed to minimize typing while training. Built with the Mudita Kompakt in mind but might work on other e-ink and minimal mobile devices.

The name comes from the Bulgarian "грънчар", which means a potter.

The UI is built on Mudita Mindful Design (MMD) (https://github.com/mudita/MMD) and the app uses graphs with the help of Vico (https://github.com/patrykandpatrick/vico).

## Disclaimer

I have not written a single line of code manually as I do not know how and I did not wish to learn to code to just create to an app I needed. Deepseek has taken care of this, quite well actually. I am sharing this app with others who might be looking for a decent e-ink workout tracker.

## Download

Grab `grunchy-1.0.apk` from [Releases](https://github.com/madnessofdoubt/grunchy/releases/latest)
and install it on the phone (`adb install -r grunchy-1.0.apk`), or open it on the device itself.

## Flow

Choose your desired rep-ranges, weight pickers and whether you prefer kg or lbs in settings. Then, either start a freestyle workout (unplanned) and start logging your exercises and sets, or create a lasting routine which you can start over and over again. Once you complete your workout, it is saved under History. Your best sets are calculated and an estimated 1RM is inferred from them, which is then used to draw a graph to give you a visual idea of your progress!

## Layout

```
app/src/main/java/com/grunchy/workout/
├── MainActivity.kt            single activity, ThemeMMD, no transitions
├── model/Models.kt            Exercise, Routine, Session, Settings (serializable state)
├── data/Store.kt              atomic JSON persistence
├── data/ExerciseLibrary.kt    120 preset exercises, grouped Push/Pull/Legs/Core
├── util/Format.kt             short number/date formatting (fewest glyphs)
├── util/Stats.kt              estimated 1RM, progression points, last top set
├── util/RepRanges.kt          rep range labels, prefill and editing rules
├── util/Units.kt              kilos <-> pounds, per-unit steps and rounding
└── ui/
    ├── AppRoot.kt             screen state + back stack + bottom bar
    ├── AppState.kt            state holder, writes through to disk on every change
    ├── ActiveWorkout.kt       the session in progress, pre-fill and set propagation
    ├── Controls.kt            ValueDropdown, WeightPicker, StepButton, …
    ├── ScrollList.kt          the scrolling list + scrollbar (MMD's look, finer gestures)
    └── *Screen.kt             Plan, RoutineEditor, Workout, History, SessionDetail,
                               Progress (Stats), Graph, Settings

```

## Licence

Apache-2.0 — see LICENSE and NOTICE.

Copyright 2026 Stan Dinev.
