# UniDeck

A 100% offline, privacy-first personal academic manager for Android built with Kotlin, Jetpack Compose, and Material 3. Designed to track multi-semester attendance, recurring timetables, internal/end-semester exam scores, and cumulative SGPA/CGPA.

---

## Core Features

### Attendance & Timetable
* **Daily Timeline & Course Views:** Switch between a date-based vertical class timeline (`Today`) and an overall course health dashboard (`Courses`) with safe-bunk and recovery lecture math.
* **Semester Date Boundaries:** Configure optional start and end dates per semester so recurring schedules automatically pause outside active term windows.
* **One-Click Logging & Bulk Actions:** Mark individual classes as *Present*, *Absent*, or *Cancelled*, or batch-update an entire day with *Mark All Present* and *Holiday* toggles.
* **Professor Autocomplete:** Offline prefix/substring autocomplete populated from previously entered faculty names across all saved semesters.
* **Glance Widget & Reminders:** 4×2 Jetpack Glance home-screen widget displaying today's schedule alongside local `AlarmManager` class and deadline alerts.

### Exam Scores & Grade Simulator
* **Theory & Lab Breakdowns:** Track Mid-Sem/Internal, Quizzes, Assignments, and End-Sem marks across `50/50` Theory and `60/40` Lab evaluation schemes.
* **Target Grade Calculator:** Live end-semester score targets showing exact marks needed for `A+` through `D` (or flagging unreachable cutoffs).
* **Interactive What-If SGPA/CGPA Simulator:** Tap grade pills (`A+` to `E`) to simulate live semester SGPA, cumulative CGPA, and equivalent percentage across custom decimal credits (`0`, `1.5`, `2`, `3`, `4`, or custom values).
* **Multi-Semester History & Archive Lock:** Automatically protects completed semesters in read-only mode while aggregating credit-weighted CGPA across all saved terms.

### Appearance & Customization
* **Material 3 / Material You:** Supports System, Dark, and Light themes, Pure `#000000` AMOLED dark mode, Android 12+ Dynamic Wallpaper colors (Monet), and `System` / `Plus Jakarta Sans` typography.
* **16 Accent Swatches:** Clean 4×4 color grid generating balanced tonal palettes.
* **16 Adaptive Launcher Icons:** Switchable `<activity-alias>` monochrome and minimal vector icons directly from Settings.

### Offline Data & Automated Backups
* **Zero Network Permissions:** No analytics, no cloud sync, and no external API dependencies—all records stay locally in a Room SQLite database.
* **Manual & Daily Auto-Backups:** Export/import complete academic records as portable `.json` files, or schedule silent daily rolling backups to a user-selected local folder via `WorkManager` and Storage Access Framework (SAF).

---

## Tech Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose, Material 3, Jetpack Glance (App Widgets)
* **Local Persistence:** Room (SQLite with non-destructive migrations), Jetpack DataStore (Preferences)
* **Background Work:** WorkManager, `AlarmManager`, Storage Access Framework (`DocumentFile`)
* **CI/CD:** GitHub Actions automated APK compilation

---

## Building from Source

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/r4hullit69/UniDeck-v2.git](https://github.com/r4hullit69/UniDeck-v2.git)
   cd UniDeck-v2
