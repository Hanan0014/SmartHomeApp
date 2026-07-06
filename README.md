# Smart Home Monitoring & Control System

Native Android (Kotlin + Jetpack Compose) mobile client for the SCS 3311
mini-project, backed by Firebase Realtime Database and Cloud Functions.

## Architecture

- **Mobile app** (`app/`): Kotlin + Jetpack Compose. Dashboard → Floor Plan
  (grid overlay) → Device Detail, with type-specific UI for outlets,
  multi-switch gang-boxes, scheduled fire-hazard appliances, light schedules,
  and mock cameras.
- **Realtime sync**: `SmartHomeRepository` wraps Firebase Realtime Database
  with `addValueEventListener`, exposed as Kotlin `Flow`s. Any change —
  from this app, another phone, or the backend — pushes to every connected
  screen instantly. No polling or manual refresh.
- **Backend safety cutoff** (`functions/`): Firebase Cloud Functions.
  - `safetyCutoffWatcher` — fires on every device write; if a safety-critical
    appliance has been ON past its `maxOnDurationSeconds`, force-flips it to
    OFF and sends an FCM push alert.
  - `sweepOverdueDevices` — scheduled sweep (every minute) as a backstop.
  - `lightScheduleSweep` — scheduled sweep that turns `LIGHT_SCHEDULE`
    devices on/off per their configured time window.

## Data model (Firebase RTDB)

```
/floors/{floorId}
    name, planImageName, gridCols, gridRows, order
    /devices/{deviceId}
        name, type, status, gridX, gridY
        subSwitches: [{id, label, status}]        // MULTI_SWITCH
        maxOnDurationSeconds, turnedOnAtEpochMs    // SCHEDULED_APPLIANCE
        scheduleStart, scheduleEnd, scheduleEnabled // LIGHT_SCHEDULE
        snapshotUrl, streamUri                     // CAMERA
        lastToggledAtEpochMs, totalOnTimeSeconds   // reporting
```

## Setup

### 1. Firebase project
1. Create a project at https://console.firebase.google.com
2. Add an Android app with package name `com.smarthome.app`
3. Download `google-services.json` and place it in `app/` (it's
   git-ignored on purpose — never commit real Firebase credentials)
4. Enable **Realtime Database** and **Cloud Messaging**
5. Deploy the backend:
   ```
   cd functions && npm install
   firebase deploy --only functions,database
   ```

### 2. Android app
Open the project root in Android Studio (Koala or newer), let Gradle sync,
then run on an emulator or device (minSdk 26).

## Pushing this project to GitHub

This folder is already a git repository with an initial commit. To publish it:

```bash
# 1. Create a new EMPTY repo on github.com (no README/license/gitignore)
#    e.g. https://github.com/new -> name it "smart-home-android"

# 2. Point this local repo at it and push
git remote add origin https://github.com/<your-username>/smart-home-android.git
git branch -M main
git push -u origin main
```

If you use two-factor auth on GitHub, use a Personal Access Token instead
of your password when prompted, or push via GitHub Desktop / Android
Studio's built-in Git integration (VCS menu → Share Project on GitHub).

## Deliverables checklist (per spec)

- [x] Source code (this repo) — add final APK link after building
- [ ] Technical documentation — see `docs/TECHNICAL_DOCUMENTATION.md` (fill in team-specific details)
- [ ] Demo video (≤ 25 min, all members present + contributions)
