# Smart Home Monitoring & Control System — Technical Documentation

**Course:** SCS 3311 — Mobile Application Design & Development
**Project:** Smart Home Monitoring & Control System (Mini-Project)

**Team Members:**
| Member | Contribution |
|---|---|
| **Hanan** | Real-Time Sync Verification, Reporting/Usage screen, Error & Edge-Case Handling, Firebase configuration and region-fix debugging |
| **Santhosh** | Initial Floor Plan scaffold, Floor Plan UI, testing the whole app, report documentation, UI contributions |
| **Sivajan** | Initial Device Detail scaffold, overall system UI, Web Hardware Simulator |

*(Update the table above with each member's actual, specific contributions before submission — this is required for individual defense grading.)*

---

## 1. Project Overview

The Smart Home Monitoring & Control System is a mobile application (Kotlin + Jetpack Compose) paired with a Firebase Realtime Database backend and a web-based Hardware Simulator Dashboard. Users manage multiple house floors, each containing user-defined rooms (Hall, Kitchen, Bathroom, etc.), and place heterogeneous smart devices — outlets, multi-switch units, fire-hazard scheduled appliances, light schedules, and security cameras — within those rooms. All state changes sync bidirectionally in near real time between the mobile app, the web simulator, and any backend automation.

---

## 2. Architecture Overview

The app follows an **MVVM (Model-View-ViewModel) architecture** with a single **Repository** as the source of truth for all Firebase Realtime Database access:

```
Compose UI (Screens)
       ↓ observes StateFlow
ViewModel (DashboardViewModel, FloorPlanViewModel, DeviceDetailViewModel, ReportsViewModel)
       ↓ calls suspend functions / Flows
SmartHomeRepository
       ↓ Firebase Realtime Database SDK (callbackFlow-wrapped listeners)
Firebase Realtime Database (asia-southeast1 region)
```

Each ViewModel exposes read data as a Kotlin `StateFlow`, populated by a `callbackFlow` wrapping Firebase's native `ValueEventListener`. Writes (toggles, renames, additions) are plain `suspend` functions on the repository using Firebase's `.setValue()` / `.updateChildren()`, awaited via the Kotlin coroutines Firebase extensions.

**Important configuration note:** the project's Realtime Database instance lives in the `asia-southeast1` region rather than the SDK default (`us-central1`). `SmartHomeRepository` explicitly constructs `FirebaseDatabase.getInstance(<region-specific URL>)` rather than the no-argument default — omitting this causes the app to silently hang on every read/write with no error surfaced to the user.

---

## 3. Floor / Room / Device Representation

The data hierarchy is: **Floor → Room → Device**.

### 3.1 Floor
- Created from the Dashboard screen with a name and a customizable grid size (`gridCols` × `gridRows`, 3–15 each).
- The floor's grid is an abstract, simple coordinate system overlaid on an optional background floor-plan image (`planImageName`, matched to a bundled drawable resource).
- This satisfies the spec's requirement for "an abstract(simple) grid mapping overlaid onto specific floor layouts."

### 3.2 Room
- Rooms are defined **on top of** the floor's grid: the user enters "Add Room" mode on the Floor Plan screen, taps a set of grid cells to mark out the room's position and shape, then names it and picks a type/icon (Hall, Kitchen, Bathroom, Bedroom, Garage, Living Room, Dining Room, Office, or custom).
- The selected floor cells are stored (`Room.cells`) and rendered as a colored, icon-labeled overlay on the floor grid, so multiple rooms are visually distinguishable on one floor map.
- Each room also gets its **own independent device-placement grid** (`Room.gridCols` / `Room.gridRows`), automatically derived from the bounding box of the cells selected on the floor (e.g. selecting a 3-wide, 2-tall area gives that room a 3×2 internal grid). This means a room's internal device layout visually matches the size and shape it occupies on the floor map.
- Rooms can be renamed and deleted (with confirmation) directly from the Floor Plan screen.

### 3.3 Device
- Devices are created from within a **Room screen** (not directly on the floor), by tapping an empty cell on that room's own grid. This enforces the design decision that every device belongs to exactly one room.
- Firebase schema path: `/floors/{floorId}/devices/{deviceId}`, with a `roomId` field linking it back to its room.
- Grid coordinates (`gridX`, `gridY`) are local to the room's own grid, not the floor's.

### 3.4 Heterogeneous Device Profiles
| Type | Key fields | Notes |
|---|---|---|
| Outlet | `status` | Simple binary ON/OFF toggle |
| Multi-Switch | `subSwitches: List<SubSwitch>` | User picks 2, 3, or 5 sub-switches at creation; each toggles independently and writes back individually |
| Scheduled Appliance | `maxOnDurationSeconds`, `turnedOnAtEpochMs` | Configurable max duration at creation; live countdown UI; pre-cutoff warning shown before, not just after, the limit is reached |
| Light Schedule | `scheduleStart`, `scheduleEnd`, `scheduleEnabled` | HH:mm-validated time window; UI shows the next scheduled transition |
| Camera | `snapshotUrl` | Renders a mock snapshot via Coil's `AsyncImage`, or a clear "no stream configured" state |

---

## 4. Synchronization Mechanism

**Bidirectional sync** is achieved entirely through Firebase Realtime Database's native listener model, wrapped in Kotlin Flows:

1. **Reads:** `SmartHomeRepository.observeDevices()`, `observeRooms()`, `observeFloors()`, and `observeAllDevices()` each open a `ValueEventListener` inside a `callbackFlow`, emitting a fresh snapshot on every remote change. ViewModels collect these into `StateFlow`s that Compose observes directly — any external write (from another phone, the web simulator, or a backend process) triggers an automatic recomposition with no manual refresh.
2. **Writes:** All writes (`toggleDevice`, `addFloor`, `addRoom`, `updateSchedule`, etc.) are `suspend` functions that call Firebase's `.setValue()`/`.updateChildren()` and `.await()` the result, ensuring writes complete before the UI proceeds.
3. **Verification:** cross-device sync was manually verified by running the app simultaneously on two physical/emulated devices against the same Firebase project — toggling a device on one updates the other within 1–2 seconds with no user action. A cold-start test (force-closing and relaunching the app) confirmed state reloads correctly from Firebase on next launch.

---

## 5. Server-Side Safety Cutoffs — Design and Known Limitation

The spec requires a **backend cloud listener or worker process** to force-flip a safety-critical device to OFF (and push an alert) if its `maxOnDurationSeconds` is exceeded.

**What was built:** `functions/index.js` contains three Cloud Functions:
- `safetyCutoffWatcher` — an `onValueWritten` trigger that reacts immediately whenever a device's data changes, cutting off a Scheduled Appliance that has exceeded its max duration.
- `sweepOverdueDevices` — a scheduled sweep (every minute) as a belt-and-suspenders check for devices left ON with no further writes.
- `lightScheduleSweep` — a scheduled sweep that turns Light Schedule devices ON/OFF based on their configured time window.

All three correctly implement the required backend behavior and are committed to the repository.

**Known limitation — disclosed deviation from the spec:** Our Firebase project uses the **Spark (free) tier**, which does not permit deploying Cloud Functions (this requires the Blaze pay-as-you-go plan). By team decision, we chose not to enable billing for this student project. As a result, `functions/index.js` cannot currently run in production.

**Client-side fallback:** `FloorPlanViewModel` runs the equivalent safety-cutoff and light-schedule logic locally, checking every 5 seconds, for as long as a floor/room screen showing the relevant devices is open in the app. This means:
- The safety cutoff and light scheduling only take effect while at least one user has the app open on a relevant screen — not continuously in the background, as a true Cloud Function would provide.
- This was deliberately tested and confirmed: with the relevant screen closed, an overdue device is *not* cut off; reopening that screen triggers the cutoff within 5 seconds.

This trade-off is disclosed here rather than hidden, per the team's decision to prioritize an honestly-documented limitation over adding billing to the shared Firebase project.

---

## 6. Reporting / Usage Tracking

The Reports screen (accessible via the 📊 icon on the Dashboard) shows every device across every floor, sortable by:
- **Most Used** — descending `totalOnTimeSeconds`
- **Recently Toggled** — descending `lastToggledAtEpochMs`

**Design decision:** `totalOnTimeSeconds` is accumulated **client-side, on toggle-off** — computed as `(now - turnedOnAtEpochMs)` at the moment a device turns off (whether by manual toggle or by the safety-cutoff fallback). No backend changes were needed since `Device.kt` already carried these fields in the original scaffold.

---

## 7. Companion Hardware Simulator

*(Fill in this section with your team's specific implementation details — the web technology used, how it's run/hosted, which device types it visualizes, and how it listens to Firebase. Include a screenshot if possible. Example structure below.)*

- **Technology:** *(e.g. HTML/CSS/JS with the Firebase JS SDK v10)*
- **How to run:** *(e.g. `python -m http.server 8000` from the `simulator/` folder, then open `http://localhost:8000`)*
- **What it shows:** *(per-device-type visual panels, simulator-only controls for forcing ERROR/DISCONNECTED states, etc.)*
- **Sync behavior:** listens to the same Firebase Realtime Database as the mobile app; any change made in the app reflects in the simulator within seconds, and vice versa.

---

## 8. Known Limitations Summary

| Limitation | Reason | Mitigation |
|---|---|---|
| Server-side safety cutoff only runs while a relevant app screen is open | Firebase Spark tier cannot deploy Cloud Functions | Client-side fallback in `FloorPlanViewModel`, checked every 5s; correct backend code exists in `functions/index.js` but is undeployed |
| No user authentication | Not a spec requirement; single-tenant use case | N/A — deliberate scope decision |

---

## 9. How to Run

1. Clone the repository, open in Android Studio.
2. Place `google-services.json` in `app/` (not committed to the repo).
3. Ensure Realtime Database and Cloud Messaging are enabled on the linked Firebase project.
4. Run on an emulator or physical device (API 26+).
5. *(Add simulator run instructions once Section 7 is completed.)*

---

## 10. Testing Summary

*(Fill in with your Phase 9 testing notes — manual test script results, any unit/instrumented tests written, screen sizes/API levels tested.)*