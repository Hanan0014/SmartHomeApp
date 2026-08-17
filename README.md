# Smart Home Monitoring & Control System

**SCS 3311 — Mobile Application Design & Development — Mini-Project**

A mobile Smart Home Monitoring and Control system built with **Kotlin + Jetpack Compose**, backed by **Firebase Realtime Database**, with a companion **web-based Hardware Simulator**. Users manage multiple house floors, define rooms within each floor (Hall, Kitchen, Bathroom, etc.), and place five different types of smart devices inside those rooms — all synchronized in real time across every connected client.

---

## ✨ Features

- **Multi-floor management** — add, rename, and delete floors, each with a customizable abstract grid.
- **Room definition** — mark out a room's shape and position by selecting cells on a floor's grid; the room automatically gets its own device-placement grid sized to match.
- **Five device profiles**, each with type-specific controls:
  | Type | Behavior |
  |---|---|
  | 🔌 Outlet | Simple ON/OFF toggle |
  | 🎛️ Multi-Switch | 2, 3, or 5 independently addressable sub-switches |
  | 👕 Scheduled Appliance | Configurable max on-duration, live countdown, safety cutoff |
  | 💡 Light Schedule | Automatic ON/OFF within a configured daily time window |
  | 📷 Camera | Mock snapshot rendering, or a clear "no stream configured" state |
- **Real-time bidirectional sync** — any change made on any client (another phone, or the web simulator) reflects everywhere else within 1–2 seconds, no manual refresh.
- **Safety cutoff** — automatically turns off a Scheduled Appliance that exceeds its configured max duration (see [Known Limitations](#-known-limitations) for how this is currently enforced).
- **Usage reporting** — a Reports screen showing devices sorted by most-used or most-recently-toggled.
- **Companion web Hardware Simulator** — visualizes the same floors/rooms/devices live, with simulator-only controls for forcing ERROR/DISCONNECTED states and fast-forwarding the safety cutoff for testing.
- **Robust error/edge-case handling** — distinct ERROR/DISCONNECTED states, graceful offline behavior, empty states throughout, and input validation on every creation form.

---

## 🏗️ Architecture

MVVM + Repository pattern.

```
Compose UI  →  ViewModel  →  Repository (SmartHomeRepository)  →  Firebase Realtime Database
```

Data hierarchy: **Floor → Room → Device**, mirroring the physical structure of a house.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Mobile client | Kotlin, Jetpack Compose (Material 3) |
| Navigation | Jetpack Navigation Compose |
| Async / reactive | Kotlin Coroutines & Flow |
| Image loading | Coil |
| Backend | Firebase Realtime Database (`asia-southeast1`) |
| Push notifications | Firebase Cloud Messaging |
| Backend automation (written, undeployed) | Firebase Cloud Functions |
| Hardware simulator | HTML / CSS / JavaScript, Firebase JS SDK v10 |
| Version control | Git & GitHub |

---

## 📁 Project Structure

```
SmartHomeApp/
├── app/                          # Android app module
│   └── src/main/java/com/smarthome/app/
│       ├── data/
│       │   ├── model/            # Floor, Room, Device, DeviceEnums
│       │   └── repository/       # SmartHomeRepository (all Firebase access)
│       └── ui/
│           ├── dashboard/        # Floor list
│           ├── floorplan/        # Floor grid, room definition, Room screen
│           ├── device/           # Per-device-type detail screens
│           ├── reports/          # Usage reporting
│           ├── navigation/       # NavGraph
│           └── theme/            # Material 3 theme
├── functions/                    # Firebase Cloud Functions (written, undeployed — see below)
├── simulator/                    # Web Hardware Simulator
│   ├── index.html
│   ├── style.css
│   └── app.js
└── docs/
    └── TECHNICAL_DOCUMENTATION.md
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (Koala or later), JDK 17
- An Android emulator or physical device, API 26+
- A Firebase project with **Realtime Database** and **Cloud Messaging** enabled

### Setup
1. Clone this repository.
2. Open the project root in Android Studio and let Gradle sync.
3. Download your Firebase project's `google-services.json` and place it in `app/` (this file is intentionally **not** committed — see `.gitignore`).
4. Confirm the database region in `SmartHomeRepository.kt` matches your Firebase project's actual Realtime Database URL:
   ```kotlin
   FirebaseDatabase.getInstance("https://<your-project>-default-rtdb.<your-region>.firebasedatabase.app")
   ```
   ⚠️ Omitting the explicit region URL causes the app to hang silently on every read/write if your database isn't in Firebase's default `us-central1` region.
5. Run the app on an emulator or device.

### Running the Hardware Simulator
```bash
cd simulator
python -m http.server 8000
```
Then open `http://localhost:8000`. Before first use, fill in your Firebase web app config in `simulator/app.js` (get it from Firebase Console → Project Settings → Your apps → Web app, or copy from `google-services.json`).

---

## ⚠️ Known Limitations

- **Safety cutoff runs client-side, not as a true backend process.** Our Firebase project is on the free **Spark** tier, which does not support deploying Cloud Functions without enabling billing — a deliberate team decision for this student project. The intended backend implementation is fully written in `functions/index.js` (`safetyCutoffWatcher`, `sweepOverdueDevices`, `lightScheduleSweep`) but is not deployed. As a disclosed fallback, `FloorPlanViewModel` performs the same checks locally every 5 seconds while a relevant screen is open. Full details in [`docs/TECHNICAL_DOCUMENTATION.md`](docs/TECHNICAL_DOCUMENTATION.md).
- **No user authentication.** Not required by the project spec; the app is designed as a single shared household system rather than multi-tenant.
- **Firebase security rules remain open** (`read`/`write: true`) for development and testing convenience.

---

## 🧪 Testing

Manual test coverage includes: floor/room/device CRUD, per-device-type toggles, cross-device real-time sync, cold-start state reload, safety cutoff (via manual timestamp manipulation and the simulator's "skip to near-cutoff" control), offline behavior, and input validation. See [`docs/TECHNICAL_DOCUMENTATION.md`](docs/TECHNICAL_DOCUMENTATION.md) Section 12 for the full test case table.

---

## 📄 Documentation

Full technical report — architecture diagram, data model, Firebase schema, feature-by-feature implementation notes, and known limitations — is in [[Project Report]](https://docs.google.com/document/d/1OqBvKyQPS9zzWicKQ6DJYb-BJLdQPwPpPpvWmE8Az-c/edit?usp=sharing)

---

## 👥 Team

| Member | Contribution |
|---|---|
| **Hanan** | Real-Time Sync Verification, Reporting/Usage screen, Error & Edge-Case Handling, Firebase configuration and region-fix debugging |
| **Santhosh** | Initial Floor Plan scaffold, Floor Plan UI, testing the whole app, report documentation, UI contributions |
| **Sivajan** | Initial Device Detail scaffold, overall system UI, Web Hardware Simulator |

---

## 📦 Deliverables

- **Source code:** [Project Repo](https://github.com/Hanan0014/SmartHomeApp)
- **APK:** Mobile Application APK