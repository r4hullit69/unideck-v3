<div align="center">

# 🎓 UniDeck

**Offline-first university academic companion for Android — built with Kotlin, Jetpack Compose, and Material You.**

[![Platform](https://img.shields.io/badge/Platform-Android_8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Offline--First_Room-000000?style=for-the-badge)](https://developer.android.com/training/data-storage/room)
[![Latest Release](https://img.shields.io/github/v/release/r4hullit69/unideck-v3?style=for-the-badge&color=6750A4&label=Download_APK)](https://github.com/r4hullit69/unideck-v3/releases/latest)
[![Build Status](https://img.shields.io/github/actions/workflow/status/r4hullit69/unideck-v3/build-apk.yml?style=for-the-badge&label=CI_Build)](https://github.com/r4hullit69/unideck-v3/actions)

[Download Latest APK](https://github.com/r4hullit69/unideck-v3/releases/latest) • [Features](#-core-features) • [Architecture](#%EF%B8%8F-architecture--tech-stack) • [Local Setup](#%EF%B8%8F-building-locally)

</div>

---

## ✨ Overview

**UniDeck** is a zero-server, privacy-first academic management app designed for university students who want instant access to their timetable, attendance projections, internal marks, and semester GPA calculations—even with zero network connectivity.

---

## 🚀 Core Features

### 📅 Smart Timetable & Daily Timeline
* **Gesture-Driven Calendar Navigation:** Smooth horizontal week/month navigation aligned to `Sun–Sat` with contextual floating actions.
* **Live Class Reminders:** Exact-alarm scheduling via `AlarmManager` with automatic reboot recovery (`BootCompletedReceiver`).
* **Home Screen Widgets:** Glanceable **Next Class**, **Today's Classes**, and **Academic Snapshot** widgets with custom preview layouts.

### 📊 Attendance Tracking & Bunk Projection
* **Subject-Level Analytics:** Visual attendance breakdown per course, professor tracking, and real-time safe-bunk / recovery-class math (`AcademicCalculator`).
* **Semester-Aware Projections:** Accurate upcoming class counts that respect semester boundaries and holiday overrides.

### 🎯 Grades, Scores & Fractional-Credit GPA
* **Internal & Exam Score Logs:** Track quizzes, mid-sems, labs, and end-semester assessments per subject.
* **Precision SGPA / CGPA Engine:** Supports fractional course credits (e.g., `1.5`, `2.5`, `3.5`) with instant target-grade simulation.

### 🎨 Material You Expressive UI & AMOLED Theme
* **Dynamic Monet Theming:** Adapts automatically to Android 12+ wallpaper palettes alongside curated static themes.
* **True Pitch-Black (`#000000`) AMOLED Mode:** Battery-friendly dark mode across all screens, charts, and widgets.
* **30+ Custom Adaptive App Icons:** Switchable launcher icons (`AppIconManager`) including minimalist, monochrome, botanical, and animal presets.

### 🔒 100% Offline-First & Portable Backups
* **Local Persistence:** Powered by **Room** (SQLite) and **Jetpack DataStore**—no accounts, cloud lock-in, or telemetry.
* **Automated & Manual Backups:** One-tap JSON/database export via Android Storage Access Framework (`BackupManager`) plus scheduled background backups via **WorkManager** (`DailyBackupWorker`).

---

## 🛠️ Architecture & Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose + Material 3 (Expressive components & custom Canvas charts) |
| **Local Database** | Room (`UniDeckDatabase`, `UniDeckDao`) |
| **Preferences** | Jetpack DataStore (`UserPreferencesRepository`) |
| **Background Work** | WorkManager (`DailyBackupWorker`) & `AlarmManager` (`ReminderScheduler`) |
| **Build System** | Gradle Kotlin DSL (`build.gradle.kts`, Version Catalog `libs.versions.toml`) |
| **CI / Delivery** | GitHub Actions (`build-apk.yml`) & GitHub Releases |

---

## 📂 Project Structure

```text
app/src/main/java/com/unideck/app/
├── backup/          # Storage Access Framework export/import & WorkManager daily backups
├── data/
│   ├── local/       # Room entities, DAOs, and database setup
│   ├── prefs/       # DataStore theme, semester, and user settings
│   └── repository/  # Single source of truth repository layer
├── domain/          # Fractional-credit GPA & attendance projection calculators
├── notifications/   # Exact class reminders and boot persistence receivers
├── system/          # Dynamic launcher icon switcher
├── ui/
│   ├── components/  # Custom Canvas charts, timeline cards, pickers, haptics
│   ├── screens/     # Attendance, Timetable, Grades, Scores, Analytics, Appearance
│   └── theme/       # Material 3 dynamic colors, pitch-black AMOLED scheme, typography
└── widget/          # Android home screen app widgets
