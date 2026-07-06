# Technical Documentation — Smart Home Monitoring & Control System

*(Fill in team names, module code, and date before submission.)*

## 1. Synchronizing Mechanism

The mobile app never polls. `SmartHomeRepository` attaches Firebase Realtime
Database `ValueEventListener`s to `/floors` and `/floors/{id}/devices`,
wrapped in Kotlin `callbackFlow`s and collected by ViewModels
(`DashboardViewModel`, `FloorPlanViewModel`). Firebase's RTDB client keeps a
persistent connection and pushes deltas to every listener the instant the
underlying data changes — whether the change came from:

- this device (toggling a switch → `repository.toggleDevice()` writes to RTDB),
- another user's phone (same listener path, same push), or
- the backend Cloud Function (safety cutoff / schedule sweep writing directly
  to RTDB).

Because the UI is driven by `StateFlow`s collected with
`collectAsState()`, Compose recomposes automatically on any of these paths —
this satisfies the "quickly reflect externally-driven updates without manual
refresh" requirement.

## 2. Floor Representation

Each floor (`Floor` model) stores a `gridCols x gridRows` abstract grid size.
Devices store their own `gridX`/`gridY` cell coordinates. `FloorPlanScreen`
renders this as a bordered grid of cells and places a colored dot (green/
grey/red/light-grey for ON/OFF/ERROR/DISCONNECTED) at each device's cell,
overlaid conceptually on top of a sample floor-plan image
(`planImageName`). A scrollable list view below the grid gives a more
practical way to toggle devices than tapping small grid cells.

## 3. Safety Cutoff / Server-Side Enforcement

`SCHEDULED_APPLIANCE` devices (e.g. irons) record `turnedOnAtEpochMs` when
switched on and carry a configured `maxOnDurationSeconds`. Two Cloud
Functions enforce the cutoff:

1. `safetyCutoffWatcher` — an `onValueWritten` trigger on
   `/floors/{floorId}/devices/{deviceId}` that checks elapsed time on every
   write to that path.
2. `sweepOverdueDevices` — a scheduled function (every minute) that scans
   all devices as a backstop for the case where no further writes happen
   while a device sits ON unattended.

Both force the device's `status` to `OFF`, clear `turnedOnAtEpochMs`, and
publish an FCM notification to the `safety_alerts` topic, which
`SmartHomeMessagingService` displays as a high-priority Android notification.

## 4. Device Profiles

| Type | Key fields | Notes |
|---|---|---|
| OUTLET | status | simple binary |
| MULTI_SWITCH | subSwitches[] | N independently toggled switches under one entity |
| SCHEDULED_APPLIANCE | maxOnDurationSeconds, turnedOnAtEpochMs | fire-hazard safety cutoff |
| LIGHT_SCHEDULE | scheduleStart/End, scheduleEnabled | backend sweep auto-toggles |
| CAMERA | snapshotUrl, streamUri | mock feed, no on/off toggle shown |

## 5. Companion Hardware Simulator

*(Not implemented in this milestone — team chose to focus on the mobile
client and backend first. The RTDB schema above is intentionally
simulator-agnostic: a future web dashboard can attach the same listeners
used by `SmartHomeRepository` to visually reflect and drive device state.)*

## 6. Reporting

`lastToggledAtEpochMs` and `totalOnTimeSeconds` are tracked per device,
giving a basis for usage reporting in the app (e.g. a simple bar chart of
on-time per device) — extend `DeviceDetailScreen` to visualize this as
needed for your team's submission.
