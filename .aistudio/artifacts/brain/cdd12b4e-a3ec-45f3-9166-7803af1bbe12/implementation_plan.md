# UniDeck — Offline-First College Academic Suite

UniDeck is a 100% offline native Android college companion built with Kotlin, Jetpack Compose, Material 3 Expressive, Room, and Jetpack Glance. It unifies daily attendance tracking, weekly timetables with offline PDF viewing, exam score breakdown, SGPA/CGPA simulation, and interactive Canvas-based academic analytics under seamless multi-semester separation.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> All four product questions from our initial alignment are locked in below. Review the architecture and visual direction, then click **Proceed** to begin implementation or share any adjustments.

- **Grading Scale (Confirmed):** 10-point scale with customizable grade cutoffs in Settings. Defaults to `O ≥ 90 (10)`, `A+ ≥ 80 (9)`, `A ≥ 70 (8)`, `B+ ≥ 60 (7)`, `B ≥ 50 (6)`, `C ≥ 40 (5)`, `F < 40 (0)`, with instant recalculation across SGPA, CGPA, Equivalent %, and Grade Target matrices.
- **First-Launch Experience (Confirmed):** Clean empty state with quick-start actions (`+ Add Course`, `Set Up Timetable`, `Import JSON Backup`). Zero mock or placeholder courses are pre-populated; a single empty `Semester 1` is initialized automatically so the user can begin immediately.
- **Attendance Weighting (Confirmed):** Equal weight — every logged `PRESENT` or `ABSENT` session counts as `1` toward attendance percentages and bunk/recovery math (`CANCELLED` sessions are excluded from the denominator). Schedule slot weights remain stored as metadata and surfaced in the component breakdown.
- **Theme & Surface Modes (Confirmed):** Full theme switcher in Settings supporting **Dark (Default)**, **Light**, and **System**, plus the **Pitch Black** OLED toggle (applies pure `#000000` canvas in dark mode), **Dynamic Monet** toggle (Android 12+), and **10 vibrant high-chroma palettes**.
- **CI / APK Workflow Default:** The GitHub Actions workflow will run on JDK 17 to assemble the debug APK on pushes to `main` and publish GitHub Releases with the `.apk` attached on `v*` tags, ensuring wrapper compatibility cleanly in CI.

---

## 1. Overview & Core Concept

- **What It Does:** Replaces scattered spreadsheets, timetable screenshots, and GPA calculators with one fast, privacy-preserving academic hub. Students switch semesters from a persistent top pill to isolate Attendance, Timetable, Scores, and SGPA while automatically aggregating cumulative CGPA across all semesters.
- **Target Audience / Persona:** College and university students who need zero-latency daily attendance logging in lecture halls (often with poor indoor connectivity), quick bunk/recovery planning, and clear visibility into exam targets without sign-ups, ads, or cloud tracking.
- **Key Value:** 100% offline reliability (no `INTERNET` permission), one-tap daily class marking from both the in-app calendar and home screen Glance widgets, and visual academic forecasting built natively with Compose Canvas.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Launch & Entry:** Every launch opens a 1.35s Material You overlay matching the active theme surface with a centered Material 3 Expressive `ContainedLoadingIndicator` (morphing polygon in `onPrimary` inside a vivid `primaryContainer` circle), smoothly cross-fading into the main shell.
2. **Persistent Navigation Shell:**
   - **Top Bar:** Two-tone wordmark (**Uni** in high-contrast foreground + **Deck** in active `primary`), right-aligned **Semester Switcher Pill** (e.g., `Sem 1 ⌄`), **Timetable** button, and **Settings** button.
   - **3-Tab Bottom Navigation:** `Attendance`, `Scores`, `Grades`. Active tab renders a filled icon inside a `primaryContainer` pill with its text label underneath; inactive tabs show clean outlined icons only (no label). On wide/tablet screens, adapts cleanly with centered max-width content rails and side `NavigationRail`.
3. **Attendance Tab (Home) & Smart Subject Detail:**
   - **Zero-State:** Welcoming card prompting the student to add their first course, configure a timetable, or import a JSON backup.
   - **Expandable Calendar:** Weekly date strip (`S M T W T F S`) with circular `primary` selection highlight, a Week/Month grid toggle revealing color-coded status dots (`Present`/`Absent`/`Cancelled`) under logged dates, a Jump-to-Today button, and tapping the month title opens an M3 `DatePickerDialog`. Selecting any date displays that day's scheduled sessions with one-tap `Present` / `Absent` / `Cancelled` chips, or a clean `"No classes today"` card.
   - **Overall Summary & Course List:** Overall attendance % hero card (subtle `12%` top-left primary radial glow, circular progress ring, warning count for courses below threshold), followed by M3 Expressive grouped course cards displaying live bunk buffer / classes-needed math, circular ring, and inline quick-mark controls. Extended FAB (`+ Add Course`) manages adding/editing courses.
   - **Smart Subject Detail (Tap Course):** Split-decimal hero percentage (`97` in 44sp ExtraBold + `.14%` in 20sp Bold), thick rounded progress bar with required threshold subtitle, swipeable `HorizontalPager` component breakdown (`LECTURE` / `LAB` / `TUTORIAL` counts split by timetable vs. manual logs + slot weight metadata), an **Attendance Trend** preview card with a Canvas sparkline and dotted 75% target line (tapping launches the full interactive Analytics view), and Upcoming Classes list.
4. **Interactive Attendance Analytics (Full-Screen Canvas View):**
   - Touch-to-scrub cumulative attendance line chart with exact date/percentage callout pill and dotted coral-red target threshold line.
   - **This Month vs. Overall** delta cards to catch recent slumps immediately.
   - **Day-of-Week Breakdown** horizontal bar chart (`Mon–Sat`).
   - **Smart Bunk / Recovery Simulator:** Interactive slider (`"What if I attend / miss the next N classes?"`) updating projected percentage in real time alongside current attendance streak and safe-bunk buffer.
5. **Scores Tab & Insights Sheet:**
   - Filter chips (`All`, `Theory (50/50)`, `Lab (60/40)`), plus a top Insights button opening the **Scores & Exam Insights** bottom sheet.
   - Expandable course cards with a 5-segment vibrant stacked bar (`#4D8EFF` Mid, `#22E58B` Q1, `#FFB020` Q2, `#A67BFF` Asgn, `#26D9FF` End), editable numeric inputs (`Mid-Sem /25`, `Quiz 1 /10`, `Quiz 2 /10`, `Assignment /5`, `End-Sem /50` for Theory; `Internal /60`, `End-Sem /40` for Lab), live `"Need X/50 in End-Sem for A+"` helper, and per-course **Relative Grading** switch (enabling custom grade override).
   - **Scores Insights Sheet:** Subject Performance horizontal bar comparison (Theory vs. Lab color-coded), Internal vs. End-Sem split averages, and auto-calculated Grade Target Matrix (`O`, `A+`, `A`, `B+` marks needed per course).
6. **Grades Tab:**
   - 3-column hero card with `12%` top-left primary radial tint: Credit-weighted **SGPA**, Cumulative **CGPA**, and **Equivalent %** (`CGPA * 10`).
   - **GPA Trajectory & Credit Impact:** Dual-line Compose Canvas chart plotting semester-by-semester SGPA vs. cumulative CGPA, high-credit (`4–5 cr`) impact badges, and a compact stacked **Grade Distribution** bar (`O`, `A+`, `A`, `B+`, `B`).
   - **What-If Simulator:** Per-course interactive grade pills (`O`, `A+`, `A`, `B+`, `B`) with a Reset button to preview hypothetical SGPA/CGPA live without mutating saved marks.
   - **Semester History:** Expandable list of all semesters showing earned credits and SGPA.
7. **Timetable Screen (Top Bar Icon):**
   - **Tab A — In-App Timetable:** Mon–Sat day tabs and time-ordered slot cards to add, edit, or delete sessions (Course, Day, Start/End Time, Professor, Room, Component Type, Slot Weight).
   - **Tab B — Timetable PDF:** Zero-permission Storage Access Framework (`OpenDocument`) importer that copies the chosen `.pdf` into app-internal storage scoped per semester, rendered completely offline via `android.graphics.pdf.PdfRenderer` with pinch-to-zoom and pan gestures.
8. **Settings Screen (Top Bar Icon):**
   - **Appearance:** Theme Mode (`Dark`, `Light`, `System`), **Pitch Black** OLED toggle, **Dynamic Monet** switch, 10 circular high-chroma theme swatches, and **App Icon Switcher** (4-column M3 `ModalBottomSheet` displaying all 8 adaptive vector icons: Dark [default], White, Wooden, Glass, Kitty, Neon, Minimal Mono, Retro Pixel).
   - **Academic & Display:** Minimum Attendance Threshold stepper/slider (default 75%), **Customizable 10-Point Grade Cutoffs** editor, **12-hour / 24-hour Time Format** toggle, and Manage Semesters dialog (add, rename, switch, delete).
   - **Offline Notifications:** Lead-time selector (`Off`, `5 min`, `10 min`, `15 min`, `30 min`) requesting `POST_NOTIFICATIONS` on Android 13+ only when enabled; schedules local alarms via `AlarmManager` + `BOOT_COMPLETED` receiver across `class_reminders` (High) and `attendance_alerts` (Default) channels.
   - **Data Portability:** Local JSON Export (`CreateDocument`) and Import (`OpenDocument`) via SAF covering all semesters, courses, schedules, logs, and academic preferences.

### Visual Identity & Theme

- **Aesthetic Direction:** Precision academic utility inspired by Material 3 Expressive and LibreTube's uncluttered tool-first chrome — high-contrast dark surfaces by default, crisp hairline borders, expressive grouped card corners (`22dp` outer, `6dp` inner, `2dp` gap), and zero visual clutter.
- **Color Palette & Mood:**
  - *Standard Dark (Default):* Canvas `#101218`, Cards `#181B24`, Hairline Borders `#262A36`, subtly tinted by the active palette's `primary`.
  - *Pitch Black (OLED Toggle):* Canvas `#000000`, Cards `#111318`, Borders `#1F222C`.
  - *Light Mode:* Canvas `#F4F6FA`, Cards `#FFFFFF`, Hairline Borders `#DCE1EC`, preserving high-chroma accents adjusted for legibility.
  - *10 High-Chroma Built-In Palettes:* Electric Blue (`#4D8EFF`, default), Emerald Pop (`#22E58B`), Vivid Violet (`#A67BFF`), Crimson Rose (`#FF5C8A`), Solar Amber (`#FFB020`), Cyan Neon (`#26D9FF`), Teal (`#2DD4BF`), Coral (`#FF6B6B`), Rose (`#FF7EB6`), Monochrome (`#E2E2E6`).
  - *Semantic Attendance Tones:* Safe Emerald `#22E58B` (`#0D2E20` fill), Margin Amber `#FFB020`, Danger Coral-Red `#FF4D6A` (`#33121A` fill), Cancelled Slate `#D0D5E2` (`#1A1D26` fill).
- **Typography & Hierarchy (Bundled Inter):**
  - Local `Inter` font bundled via `font-util` in `res/font/` (zero runtime font downloads).
  - Strictly bold weights: `ExtraBold` (800, `-0.75.sp` tracking) for hero metrics (`97.14%`, SGPA/CGPA), `Bold` (700) for screen/course titles, `SemiBold` (600) for body/chips/buttons, `Medium` (500) for metadata. Never `Light` or `Thin`.
  - `fontFeatureSettings = "tnum"` enforced across every percentage, ratio, score, GPA, and chart axis so numbers never jitter during animations or scrubbing.
- **Interactive Feedback & Motion:** Edge-to-edge layout (`enableEdgeToEdge()`) respecting status and navigation bar insets, spring-based card expansions (`AnimatedVisibility`), animated circular rings and Canvas path sweeps, touch-scrub haptic/visual indicators, and snackbars for undoable attendance actions or backup imports.

---

## 3. Key Product Decisions & Trade-Offs

- **100% Local Room + DataStore Architecture (Zero Network):**
  - *Chosen Approach:* Store all relational academic data in Room (SQLite via KSP) and user preferences (theme, Monet, Pitch Black, time format, threshold, grade cutoffs, reminder lead time, active icon) in Jetpack DataStore Preferences. Omit `INTERNET` permission completely and comment out unused network/Firebase libraries.
  - *Why:* Guarantees instant offline performance in campus dead zones, honors strict privacy, and minimizes APK footprint.
  - *Alternatives Considered:* Cloud sync (Firebase/Firestore) was explicitly excluded by the offline-first requirement; local JSON Export/Import via SAF provides full user-owned backup and device migration instead.
- **Native Compose Canvas for All Charts:**
  - *Chosen Approach:* Build custom animated Canvas components for the Attendance Trend chart (with touch-to-scrub), Day-of-Week horizontal bars, Subject Performance comparison, SGPA vs. CGPA dual-line trajectory, and Grade Distribution bar.
  - *Why:* Eliminates third-party chart dependency bloat, ensures pixel-perfect alignment with our 10 high-chroma palettes and `tnum` Inter typography, and supports custom gestures cleanly.
  - *Alternatives Considered:* Vico/MPAndroidChart — rejected because the specification explicitly calls for zero third-party charting libraries.
- **8 Switchable Adaptive Icons via `<activity-alias>`:**
  - *Chosen Approach:* Define the launcher entry via 8 `<activity-alias>` declarations pointing to `MainActivity` (with `Dark` enabled by default and 7 disabled), toggling them atomically via `PackageManager.setComponentEnabledSetting`. Each variant uses its own vector adaptive icon drawable set.
  - *Why:* Standard Android mechanism allowing users to switch home-screen icons dynamically offline without restarting or external assets.
- **Offline Exact/Inexact Class Reminders via `AlarmManager`:**
  - *Chosen Approach:* Compute the next upcoming scheduled class occurrence minus the user's lead time (`5/10/15/30 min`), schedule a local `BroadcastReceiver` alarm via `AlarmManager` (using `setExactAndAllowWhileIdle` when permitted, gracefully falling back to `setAndAllowWhileIdle` so no intrusive special-access barrier blocks the user), and re-arm alarms after device reboot via `BOOT_COMPLETED`.
  - *Why:* Reliable local notifications with zero background battery drain or WorkManager timing drift, fully compliant with Google Play permissions minimization.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│                         JETPACK COMPOSE UI SHELL                             │
│  ┌────────────────────┐  ┌────────────────────────────────────────────────┐  │
│  │ 1.5s M3 Expressive │  │ Persistent Top Bar (UniDeck + Sem Pill + Nav)  │  │
│  │ Launch Overlay     │  └────────────────────────────────────────────────┘  │
│  └────────────────────┘                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ Attendance   │  │ Scores       │  │ Grades       │  │ Full-Screen      │  │
│  │ Tab & Detail │  │ Tab & Sheet  │  │ Tab & WhatIf │  │ Timetable / PDF  │  │
│  │ + Analytics  │  │ + Canvas     │  │ + Dual-Line  │  │ + Settings Modal │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘  │
└─────────┼─────────────────┼─────────────────┼───────────────────┼────────────┘
          └─────────────────┴────────┬────────┴───────────────────┘
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                       MVVM VIEWMODEL & DOMAIN LAYER                          │
│  • UniDeckViewModel (StateFlows per tab, What-If & Bunk simulation state)    │
│  • AcademicCalculator (Equal-weight attendance math, customizable 10-pt GPA, │
│    End-Sem target matrix, monthly vs. overall delta, streak & recovery math) │
│  • BackupManager (Offline JSON serializer/deserializer via SAF URIs)         │
│  • ReminderScheduler (AlarmManager + NotificationChannels + BootReceiver)    │
│  • AppIconManager (PackageManager <activity-alias> switcher)                 │
└────────────────────────────┬───────────────────────────┬─────────────────────┘
                             ▼                           ▼
┌────────────────────────────────────┐   ┌─────────────────────────────────────┐
│   LOCAL PERSISTENCE LAYER          │   │   JETPACK GLANCE HOME WIDGETS       │
│  ┌──────────────────────────────┐  │   │  ┌───────────────────────────────┐  │
│  │ Room Database (KSP)          │◄─┼───┼──┤ Today's Classes Widget (4x2)  │  │
│  │ • SemesterEntity             │  │   │  └───────────────────────────────┘  │
│  │ • CourseEntity               │  │   │  ┌───────────────────────────────┐  │
│  │ • ClassScheduleEntity        │◄─┼───┼──┤ Attendance & GPA Widget (2x2) │  │
│  │ • AttendanceLogEntity        │  │   │  └───────────────────────────────┘  │
│  └──────────────────────────────┘  │   │  (GlanceTheme Monet + 24dp corners, │
│  ┌──────────────────────────────┐  │   │   refreshed automatically on DB     │
│  │ DataStore Preferences        │  │   │   writes & semester switches)       │
│  │ (Theme, Monet, Pitch Black,  │  │   └─────────────────────────────────────┘
│  │  Cutoffs, Threshold, Icon)   │  │
│  └──────────────────────────────┘  │
│  ┌──────────────────────────────┐  │
│  │ Internal File Storage        │  │
│  │ (Per-Semester Timetable PDFs)│  │
│  └──────────────────────────────┘  │
└────────────────────────────────────┘
```

### Data Model & State

- **Room Entities (Relational with Cascading Deletes):**
  - `SemesterEntity`: `id`, `name`, `isCurrent` — isolates courses, schedules, logs, and PDFs per semester.
  - `CourseEntity`: `id`, `semesterId` (FK), `code`, `name`, `professorName`, `credits`, `isLab`, `midSem`, `quiz1`, `quiz2`, `assignment`, `endSem`, `relativeGradingEnabled`, `customGradeOverride`.
  - `ClassScheduleEntity`: `id`, `courseId` (FK), `semesterId` (FK), `dayOfWeek` (`1..7`), `startTime` (minutes from midnight), `endTime`, `roomNumber`, `componentType` (`LECTURE`/`LAB`/`TUTORIAL`), `weight`.
  - `AttendanceLogEntity`: `id`, `courseId` (FK), `dateEpochDay`, `status` (`PRESENT`/`ABSENT`/`CANCELLED`), `componentType`, `isManualEntry`.
- **Preferences State (`DataStore`):**
  - `themeMode` (`DARK`/`LIGHT`/`SYSTEM`), `pitchBlackEnabled`, `dynamicMonetEnabled`, `paletteIndex` (`0..9`), `minAttendanceThreshold` (default `75`), `use24HourFormat`, `reminderLeadMinutes` (`0`/`5`/`10`/`15`/`30`), `activeIconKey`, and customizable grade cutoffs (`oMin`, `aPlusMin`, `aMin`, `bPlusMin`, `bMin`, `cMin`).

### Interactive Component & State Mapping

- **Calendar & Daily Marking:** Tapping a date updates `selectedEpochDay` in `UniDeckViewModel`, dynamically filtering `ClassScheduleEntity` by `dayOfWeek` and pairing each slot with existing `AttendanceLogEntity` rows. Tapping `Present` / `Absent` / `Cancelled` upserts or toggles the log in Room, recalculates attendance/bunk math via reactive `Flow`, triggers an `attendance_alerts` notification if a course newly crosses below threshold, and updates Glance widgets.
- **Course CRUD & Manual Attendance:** Adding/editing a course validates required fields (code, name, credits) and persists via Room; inline `+Present`/`+Absent`/`+Cancelled` buttons on course cards log manual entries (`isManualEntry = true`) for the selected date.
- **Subject Detail & Interactive Analytics:** Tapping a course navigates via type-safe Navigation Compose key (`SubjectDetail(courseId)`). Tapping the Trend card navigates to `AttendanceAnalytics(courseId)` where drag/tap gestures on the Canvas update a local `scrubbedIndex` state to show exact date & cumulative percentage tooltips, and the recovery slider mutates `simulatedDelta` (`-15..+15` classes) in real time.
- **Scores & Grade Cutoffs:** Inline numeric editors clamp marks to valid ceilings (`Theory: 25/10/10/5/50`; `Lab: 60/40`), persist debounced/immediate updates to Room, and reactively recompute total percentage, required End-Sem marks, SGPA, CGPA, and Grade Target Matrix rows.
- **What-If Simulator:** Stores an in-memory map `Map<Long, GradeLetter>` overlaid atop computed course grades in the Grades tab; tapping pills previews live SGPA/CGPA delta without altering Room data, and `Reset` clears the overlay map.
- **Timetable & PDF Viewer:** Adding/editing timetable slots updates Room and automatically re-arms `ReminderScheduler` + refreshes the 4x2 Glance widget. Importing a PDF copies the SAF stream into internal app storage keyed by `semesterId`, rendering pages via `PdfRenderer` into memory-safe bitmaps with pinch-to-zoom (`detectTransformGestures`).
- **JSON Backup & Icon Switcher:** Export serializes all 4 Room tables + preferences into a versioned JSON schema via `CreateDocument`; Import validates schema integrity, performs a transactional Room replacement, and refreshes widgets/alarms. Selecting an icon in Settings enables the chosen `<activity-alias>` and disables the remaining 7 aliases while storing `activeIconKey`.
