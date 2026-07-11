# Smart Home Monitoring & Control System

A native Android mobile client for monitoring and controlling a simulated
smart home — multiple floors, heterogeneous device types, real-time
cloud sync, and automated safety enforcement.

Built for **SCS 3311: Mobile Application Design & Development** (Mini Project).

**Team:** Hanan · Santhosh · Sivajan

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Data Model](#data-model-firebase-rtdb)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Team Workflow](#team-workflow)
- [Deliverables Checklist](#deliverables-checklist-per-spec)
- [Known Limitations](#known-limitations)

---

## Overview

The app lets a user manage multiple house floors, each with devices placed
on an abstract grid overlaid on a floor-plan image. Five distinct device
profiles are supported, each with its own behavior:

| Device Type | Behavior |
|---|---|
| **Outlet** | Simple ON/OFF power supply |
| **Multi-Switch** | A gang-box unit with several independently addressable switches |
| **Scheduled Appliance** | Fire-hazard-prone devices (e.g. irons) with a server-enforced max on-duration |
| **Light Schedule** | Bulbs that auto-toggle on a configured time window |
| **Camera** | Mock snapshot/stream display, no toggle |

State changes sync bidirectionally and instantly: a toggle from this app,
another device, or the backend safety-cutoff worker reflects everywhere
within seconds — no manual refresh required.

## Architecture

**Mobile app** (`app/`) — Kotlin + Jetpack Compose.
Dashboard → Floor Plan (grid overlay) → Device Detail, with type-specific
UI for each of the five device profiles above.

**Realtime sync** — `SmartHomeRepository` wraps Firebase Realtime Database
listeners (`addValueEventListener`) and exposes them as Kotlin `Flow`s,
collected by Compose via `collectAsState()`. Any change — from this app,
a teammate's phone, or the backend — pushes to every connected screen
automatically.

**Backend safety cutoff** (`functions/`) — Firebase Cloud Functions:

| Function | Trigger | Purpose |
|---|---|---|
| `safetyCutoffWatcher` | Fires on every device write | If a safety-critical appliance has been ON past its `maxOnDurationSeconds`, force-flips it OFF and sends an FCM push alert |
| `sweepOverdueDevices` | Scheduled, every minute | Backstop sweep in case no write triggers the watcher above |
| `lightScheduleSweep` | Scheduled, every minute | Auto-toggles `LIGHT_SCHEDULE` devices per their configured time window |

## Data Model (Firebase RTDB)

```
/floors/{floorId}
    name, planImageName, gridCols, gridRows, order
    /devices/{deviceId}
        name, type, status, gridX, gridY
        subSwitches: [{id, label, status}]           // MULTI_SWITCH
        maxOnDurationSeconds, turnedOnAtEpochMs       // SCHEDULED_APPLIANCE
        scheduleStart, scheduleEnd, scheduleEnabled   // LIGHT_SCHEDULE
        snapshotUrl, streamUri                        // CAMERA
        lastToggledAtEpochMs, totalOnTimeSeconds      // reporting
```

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile app | Kotlin, Jetpack Compose |
| Navigation | Jetpack Navigation Compose |
| Backend / database | Firebase Realtime Database (free Spark tier) |
| Server-side logic | Firebase Cloud Functions |
| Push notifications | Firebase Cloud Messaging (FCM) |
| Version control | Git + GitHub |

## Getting Started

### Prerequisites

- Android Studio (Koala 2024.1 or newer)
- JDK 17 (bundled with recent Android Studio)
- An emulator image, API level 26+

### 1. Clone the repo

```bash
git clone https://github.com/<org-or-username>/smart-home-android.git
cd smart-home-android
```

`google-services.json` is already committed to this repo (see
[Known Limitations](#known-limitations) for why), so no manual Firebase
file transfer is needed between teammates.

### 2. Open in Android Studio

Open the cloned folder as the project root, let Gradle sync complete, then
select an emulator (API 26+) and hit **Run**. The app should launch to the
Dashboard screen.

### 3. Firebase project access

The team's shared Firebase project already has Realtime Database and Cloud
Messaging enabled. If you need console access (e.g. to inspect data or
manually add test devices), ask Hanan for an invite to the Firebase project.

### 4. Deploy backend changes (only if modifying Cloud Functions)

```bash
cd functions
npm install
firebase deploy --only functions,database
```

## Project Structure

```
app/src/main/java/com/smarthome/app/
├── data/
│   ├── model/          Device, Floor, enums
│   ├── repository/      SmartHomeRepository (Firebase RTDB access)
│   └── messaging/        FCM notification handling
└── ui/
    ├── dashboard/        Floor list screen
    ├── floorplan/        Grid overlay + device placement
    ├── device/           Per-device-type detail screens
    ├── navigation/        NavGraph
    └── theme/            Colors, typography

functions/                Firebase Cloud Functions (safety cutoff, schedules)
docs/                     Technical documentation
plan.md                   Phase-by-phase development plan
PROJECT_OVERVIEW.md       Full project scope and stack rationale
```

## Team Workflow

Work is split across three parallel feature branches, one per screen area:

| Branch | Scope | Owner |
|---|---|---|
| `feature/dashboard` | Dashboard / floor list | Hanan |
| `feature/floorplan` | Floor plan grid + device placement | Santhosh |
| `feature/device-detail` | Per-device-type detail screens | Sivajan |

Each branch merges into `main` via pull request, one at a time, to keep
conflicts manageable — `ui/navigation/NavGraph.kt` is the file most likely
to be touched by more than one branch, so coordinate before merging changes
to it. Full phase breakdown, ownership, and target timeline are in
[`plan.md`](./plan.md).

## Deliverables Checklist (per module spec)

- [x] Source code (this repo)
- [ ] Final APK link (add after building a signed release)
- [ ] Technical documentation — [`docs/TECHNICAL_DOCUMENTATION.md`](./docs/TECHNICAL_DOCUMENTATION.md)
- [ ] Demo video (≤ 25 min, all members present, contributions stated)

## Known Limitations

These are deliberate, documented trade-offs for a coursework timeline —
worth restating in the technical documentation, not hiding:

- **`google-services.json` is committed to this repo**, rather than
  git-ignored, so all three team members get working Firebase access on
  clone without manual file sharing. This is acceptable here because
  Firebase access control is enforced by **Database Rules**, not by
  keeping this file private.
- **Realtime Database rules currently allow open read/write** for ease of
  development. This is fine for a coursework demo but would need proper
  authentication-based rules before any real-world deployment.
- **No companion hardware simulator is implemented in this milestone** —
  the team chose to prioritize the mobile client. The RTDB schema above is
  intentionally simulator-agnostic, so one could be added later without
  changing the app.