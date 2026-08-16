/**
 * Smart Home — Hardware Simulator
 * ---------------------------------------------------------------------------
 * Companion web dashboard for the SmartHomeApp Android client. Connects to
 * the SAME Firebase Realtime Database, at the same schema, and represents
 * the physical hardware the app controls (per the mini-project spec):
 * floors -> rooms -> devices, plus a live "CCTV wall" for camera devices.
 *
 * This file intentionally mirrors app/src/.../data/repository/
 * SmartHomeRepository.kt and ui/floorplan/FloorPlanViewModel.kt field-for-
 * field, so a write made here is indistinguishable from one made by the
 * phone app, and vice versa.
 *
 * Scope (see project chat): VIEW + SIMULATE only. Floors/rooms/devices are
 * created from the Android app; this page reads them, lets you flip
 * switches like real hardware would, and adds simulator-only controls
 * (force ERROR/DISCONNECTED, fast-forward a timer, fake a camera snapshot)
 * that the app has no way to trigger on itself.
 * ---------------------------------------------------------------------------
 */

import { initializeApp } from "https://www.gstatic.com/firebasejs/10.7.1/firebase-app.js";
import {
  getDatabase, ref, onValue, update,
} from "https://www.gstatic.com/firebasejs/10.7.1/firebase-database.js";

/* ============================================================================
   Firebase — same project as the Android app (values from google-services.json)
   ============================================================================ */
const firebaseConfig = {
  apiKey: "AIzaSyDngUiQZm4_5QqTu5Cft7hpXBmYC1U2i_E",
  authDomain: "smart-home-app-68cf4.firebaseapp.com",
  databaseURL: "https://smart-home-app-68cf4-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "smart-home-app-68cf4",
  storageBucket: "smart-home-app-68cf4.firebasestorage.app",
  messagingSenderId: "725106868666",
  appId: "1:725106868666:android:9d8d942f68e0dd84441d83",
};
// NOTE: this is the Android app's key, reused here. Fine for now because the
// RTDB rules are wide open (read/write: true) — see PROJECT_OVERVIEW.md's
// documentation note. For a cleaner setup later: Firebase Console -> Project
// Settings -> Your apps -> Add app -> Web, and swap the config above.

const firebaseApp = initializeApp(firebaseConfig);
const db = getDatabase(firebaseApp, firebaseConfig.databaseURL);

/* ============================================================================
   Constants — mirror DeviceEnums.kt / Room.kt / FloorPlanScreen.kt exactly
   ============================================================================ */
const STATUSES = ["ON", "OFF", "ERROR", "DISCONNECTED"];
const TYPE_META = {
  OUTLET: { icon: "🔌", label: "Outlet", heroImg: "assets/icons/outlet.png" },
  MULTI_SWITCH: { icon: "🎚️", label: "Multi-Switch", heroImg: "assets/icons/switch_board.png" },
  SCHEDULED_APPLIANCE: { icon: "⏲️", label: "Scheduled Appliance", heroImg: "assets/icons/iron.png" },
  LIGHT_SCHEDULE: { icon: "💡", label: "Light Schedule", heroImg: null },
  CAMERA: { icon: "📷", label: "Camera", heroImg: null },
};
// Room.kt's ROOM_COLOR_PALETTE, in the same cycling order.
const ROOM_COLORS = ["#B3E5FC", "#C8E6C9", "#FFE0B2", "#F8BBD0", "#D1C4E9", "#FFF9C4"];
// FloorPlanViewModel.kt
const MAX_ON_DURATION_DEFAULT_SECONDS = 15 * 60;
const WARNING_THRESHOLD_SECONDS = 60;
const SAFETY_CHECK_INTERVAL_MS = 5000;

/* ============================================================================
   DOM refs
   ============================================================================ */
const viewRoot = document.getElementById("viewRoot");
const breadcrumbEl = document.getElementById("breadcrumb");
const connDot = document.getElementById("connDot");
const connLabel = document.getElementById("connLabel");
const clockEl = document.getElementById("clock");
const toastEl = document.getElementById("toast");
const deviceModalBackdrop = document.getElementById("deviceModalBackdrop");
const deviceModalPanel = document.getElementById("deviceModalPanel");

/* ============================================================================
   State
   ============================================================================ */
let floorsData = {};                    // raw snapshot of /floors
let nav = { view: "home" };             // {view:'home'} | {view:'floor',floorId} | {view:'room',floorId,roomId}
let openDeviceRef = null;               // {floorId, deviceId} while the modal is open
let toastTimer = null;

/* ============================================================================
   Small helpers — mirror Kotlin equivalents 1:1
   ============================================================================ */
function esc(str) {
  return String(str ?? "").replace(/[&<>"']/g, (c) => (
    { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]
  ));
}
// DeviceStatus.fromString — unknown/missing status defaults to DISCONNECTED, not OFF.
function fromStatus(v) {
  const s = String(v || "").toUpperCase();
  return STATUSES.includes(s) ? s : "DISCONNECTED";
}
// DeviceType.fromString — unknown/missing type defaults to OUTLET.
function fromType(v) {
  const s = String(v || "").toUpperCase();
  return TYPE_META[s] ? s : "OUTLET";
}
function statusClass(status) { return status.toLowerCase(); }
// Room.cellKey(x, y)
function cellKey(x, y) { return `${x},${y}`; }
// ScheduledApplianceControl.kt's formatTime — mm:ss, no hours field.
function formatTime(totalSeconds) {
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}
// LightScheduleControl.kt's isValidTime (HH:mm, 24h).
function isValidTime(str) { return /^([01]\d|2[0-3]):([0-5]\d)$/.test(str || ""); }
// FloorPlanViewModel.kt's isWithinWindow — string compare works because HH:mm is zero-padded.
function isWithinWindow(nowHHmm, start, end) {
  if (!start || !end) return false;
  return start <= end ? (nowHHmm >= start && nowHHmm < end) : (nowHHmm >= start || nowHHmm < end);
}
// LightScheduleControl.kt's nextTransitionText.
function nextTransitionText(start, end, enabled) {
  if (!enabled || !start || !end || !isValidTime(start) || !isValidTime(end)) return "Schedule not active";
  const nowHHmm = new Date().toTimeString().slice(0, 5);
  if (nowHHmm < start) return `Turns on at ${start}`;
  if (nowHHmm < end || end < start) return `Turns off at ${end}`;
  return `Turns on at ${start}`;
}
function hexToRgba(hex, alpha) {
  const h = hex.replace("#", "");
  const r = parseInt(h.substring(0, 2), 16), g = parseInt(h.substring(2, 4), 16), b = parseInt(h.substring(4, 6), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}
// subSwitches can arrive as an array (sequential keys) or an object (sparse
// keys) depending on how RTDB serialized it — this normalizes to
// [rtdbKey, value] pairs either way, so writes can target the right path.
function subSwitchEntries(device) {
  const raw = device.subSwitches;
  if (!raw) return [];
  return Object.entries(raw).filter(([, v]) => v != null);
}

/* ============================================================================
   Data access helpers (read from the in-memory floorsData snapshot)
   ============================================================================ */
function floorsArr() {
  return Object.entries(floorsData)
    .map(([id, f]) => ({ id, ...f }))
    .sort((a, b) => (a.order ?? 0) - (b.order ?? 0));
}
function getFloor(floorId) {
  const f = floorsData[floorId];
  return f ? { id: floorId, ...f } : null;
}
function devicesArr(floor) {
  return Object.entries(floor.devices || {}).map(([id, d]) => ({ id, ...d }));
}
function roomsArr(floor) {
  return Object.entries(floor.rooms || {}).map(([id, r]) => ({ id, ...r }));
}
function getDevice(floorId, deviceId) {
  const f = floorsData[floorId];
  const d = f && f.devices && f.devices[deviceId];
  return d ? { id: deviceId, ...d } : null;
}
function roomForCellXY(rooms, x, y) {
  const key = cellKey(x, y);
  return rooms.find((r) => (r.cells || []).includes(key));
}
function roomColorFor(room, rooms) {
  const idx = Math.max(0, rooms.findIndex((r) => r.id === room.id));
  return ROOM_COLORS[idx % ROOM_COLORS.length];
}
function allCameras() {
  const out = [];
  for (const floor of floorsArr()) {
    const rooms = roomsArr(floor);
    for (const device of devicesArr(floor)) {
      if (fromType(device.type) === "CAMERA") {
        const room = rooms.find((r) => r.id === device.roomId);
        out.push({ device, floorId: floor.id, floorName: floor.name || "Untitled floor", roomName: room ? room.name : "Unassigned" });
      }
    }
  }
  return out;
}

/* ============================================================================
   Firebase writes — mirror SmartHomeRepository.kt function-for-function
   ============================================================================ */

// toggleDevice()
async function writeToggleDevice(floorId, device) {
  const turningOn = fromStatus(device.status) !== "ON";
  const now = Date.now();
  const updates = { status: turningOn ? "ON" : "OFF", lastToggledAtEpochMs: now };
  if (turningOn) {
    updates.turnedOnAtEpochMs = now;
  } else {
    updates.turnedOnAtEpochMs = null;
    if (device.turnedOnAtEpochMs) {
      const elapsed = Math.floor((now - device.turnedOnAtEpochMs) / 1000);
      updates.totalOnTimeSeconds = (device.totalOnTimeSeconds || 0) + elapsed;
    }
  }
  await update(ref(db, `floors/${floorId}/devices/${device.id}`), updates);
}

// toggleSubSwitch() — subKey is the actual RTDB key (array index or object key).
async function writeToggleSubSwitch(floorId, deviceId, subKey, newStatus) {
  await update(ref(db, `floors/${floorId}/devices/${deviceId}/subSwitches/${subKey}`), { status: newStatus });
}

// updateSchedule()
async function writeSchedule(floorId, deviceId, start, end, enabled) {
  await update(ref(db, `floors/${floorId}/devices/${deviceId}`), {
    scheduleStart: start, scheduleEnd: end, scheduleEnabled: enabled,
  });
}

// forceOff() — the safety-cutoff worker's write. Used by the sweep below.
async function writeForceOff(floorId, deviceId, reason, sessionStartEpochMs, currentTotal) {
  const now = Date.now();
  const updates = { status: "OFF", turnedOnAtEpochMs: null, lastToggledAtEpochMs: now, lastCutoffReason: reason };
  if (sessionStartEpochMs) {
    const elapsed = Math.floor((now - sessionStartEpochMs) / 1000);
    updates.totalOnTimeSeconds = (currentTotal || 0) + elapsed;
  }
  await update(ref(db, `floors/${floorId}/devices/${deviceId}`), updates);
}

// setScheduledStatus() — the light-schedule sweep's write. Deliberately
// touches only these two fields, same as the Kotlin version.
async function writeSetScheduledStatus(floorId, deviceId, status) {
  await update(ref(db, `floors/${floorId}/devices/${deviceId}`), { status, lastToggledAtEpochMs: Date.now() });
}

// --- Simulator-only writes (no Kotlin equivalent — this is the "hardware" talking back) ---
async function writeSnapshotUrl(floorId, deviceId, url) {
  await update(ref(db, `floors/${floorId}/devices/${deviceId}`), { snapshotUrl: url || null });
}
async function writeForceStatus(floorId, deviceId, status) {
  const updates = { status, lastToggledAtEpochMs: Date.now() };
  if (status !== "ON") updates.turnedOnAtEpochMs = null;
  await update(ref(db, `floors/${floorId}/devices/${deviceId}`), updates);
}
async function writeReconnect(floorId, deviceId) {
  await update(ref(db, `floors/${floorId}/devices/${deviceId}`), {
    status: "OFF", turnedOnAtEpochMs: null, lastToggledAtEpochMs: Date.now(),
  });
}
async function writeSkipAhead(floorId, device) {
  const maxDuration = device.maxOnDurationSeconds ?? MAX_ON_DURATION_DEFAULT_SECONDS;
  const now = Date.now();
  // Lands 3s from cutoff so the 5s safety sweep catches it almost immediately —
  // enough to demo the auto-shutoff without waiting out the real duration.
  const newTurnedOnAt = now - Math.max(0, maxDuration - 3) * 1000;
  const updates = { turnedOnAtEpochMs: newTurnedOnAt };
  if (fromStatus(device.status) !== "ON") {
    updates.status = "ON";
    updates.lastToggledAtEpochMs = now;
  }
  await update(ref(db, `floors/${floorId}/devices/${device.id}`), updates);
}

/* ============================================================================
   Client-side safety sweep — mirrors FloorPlanViewModel.runSafetyChecks().
   Runs globally across every floor (not just one open screen), every 5s, for
   as long as this tab stays open. Same disclosed Spark-tier workaround as
   the app; harmless if both the app and this tab are open at once, since
   both writes are idempotent.
   ============================================================================ */
function runSafetyChecks() {
  const nowHHmm = new Date().toTimeString().slice(0, 5);
  for (const floor of floorsArr()) {
    for (const device of devicesArr(floor)) {
      const type = fromType(device.type);
      const status = fromStatus(device.status);
      if (type === "SCHEDULED_APPLIANCE") {
        const maxDuration = device.maxOnDurationSeconds ?? MAX_ON_DURATION_DEFAULT_SECONDS;
        if (status === "ON" && device.turnedOnAtEpochMs) {
          const elapsed = Math.floor((Date.now() - device.turnedOnAtEpochMs) / 1000);
          if (elapsed >= maxDuration) {
            writeForceOff(floor.id, device.id, `exceeded max on-duration of ${maxDuration}s`, device.turnedOnAtEpochMs, device.totalOnTimeSeconds);
          }
        }
      } else if (type === "LIGHT_SCHEDULE") {
        if (device.scheduleEnabled) {
          const desired = isWithinWindow(nowHHmm, device.scheduleStart, device.scheduleEnd) ? "ON" : "OFF";
          if (status !== desired) writeSetScheduledStatus(floor.id, device.id, desired);
        }
      }
    }
  }
}
setInterval(runSafetyChecks, SAFETY_CHECK_INTERVAL_MS);

/* ============================================================================
   Rendering — Home
   ============================================================================ */
function renderHome() {
  const floors = floorsArr();
  let html = `
    <div class="view-header">
      <div class="view-heading">
        <div class="view-heading__icon">🏠</div>
        <div>
          <div class="view-title">Floors</div>
          <div class="view-subtitle">${floors.length} floor${floors.length === 1 ? "" : "s"} synced from the app</div>
        </div>
      </div>
    </div>`;

  if (!floors.length) {
    html += `<div class="empty-state"><strong>No floors yet.</strong><br/>Add a floor from the Android app — it appears here instantly.</div>`;
  } else {
    html += `<div class="floor-grid">${floors.map(floorCardHtml).join("")}</div>`;
  }

  const cams = allCameras();
  if (cams.length) {
    html += `<div class="section-label">🎥 Live cameras</div><div class="cctv-wall">${cams.map(cctvTileHtml).join("")}</div>`;
  }

  viewRoot.innerHTML = html;
}

function floorCardHtml(floor) {
  const devices = devicesArr(floor);
  const counts = { ON: 0, ERROR: 0, DISCONNECTED: 0 };
  devices.forEach((d) => { const s = fromStatus(d.status); if (s in counts) counts[s]++; });
  const pills = [];
  if (counts.ON) pills.push(`<span class="pill on">${counts.ON} ON</span>`);
  if (counts.ERROR) pills.push(`<span class="pill error">${counts.ERROR} ERROR</span>`);
  if (counts.DISCONNECTED) pills.push(`<span class="pill disc">${counts.DISCONNECTED} OFFLINE</span>`);
  if (!pills.length) pills.push(`<span class="pill">all quiet</span>`);

  return `
    <div class="floor-card" data-action="go-floor" data-floor-id="${floor.id}" role="button" tabindex="0">
      <div class="floor-card__top">
        <div class="floor-card__icon">${esc(floor.iconEmoji || "🏠")}</div>
        <div>
          <div class="floor-card__name">${esc(floor.name || "Untitled floor")}</div>
          <div class="floor-card__meta">${devices.length} device${devices.length === 1 ? "" : "s"} · ${floor.gridCols || 8}×${floor.gridRows || 6} grid</div>
        </div>
      </div>
      <div class="floor-card__pills">${pills.join("")}</div>
    </div>`;
}

/* ============================================================================
   Rendering — CCTV wall (shared by Home + inside device modal)
   ============================================================================ */
function cameraFeedState(device) {
  const status = fromStatus(device.status);
  if (status === "ERROR") return "error";
  if (status === "DISCONNECTED") return "offline";
  if (device.snapshotUrl && device.snapshotUrl.trim()) return "live";
  return "no-stream";
}
function buildCameraVisual(device) {
  const state = cameraFeedState(device);
  if (state === "live") {
    return `<img class="cctv-tile__feed" src="${esc(device.snapshotUrl)}" alt="${esc(device.name)} snapshot" onerror="this.style.display='none'" /><div class="cctv-tile__scan"></div>`;
  }
  const isError = state === "error";
  const icon = isError ? "⚠️" : state === "offline" ? "🔌" : "🚫";
  const style = isError ? ' style="background:linear-gradient(160deg,#2a1010,#140505 70%);"' : "";
  return `<div class="cctv-tile__noise"${style}></div><div class="cctv-empty-icon">${icon}</div>`;
}
function cameraRecBadge(device) {
  const state = cameraFeedState(device);
  if (state === "live") return `<div class="cctv-tile__rec"><span class="dot"></span>LIVE</div>`;
  const label = state === "error" ? "SIGNAL ERROR" : state === "offline" ? "OFFLINE" : "NO STREAM";
  const dotStyle = state === "error" ? ' style="background:var(--error);animation:pulse-red 1.2s infinite;"' : "";
  return `<div class="cctv-tile__rec offline"><span class="dot"${dotStyle}></span>${label}</div>`;
}
function cctvTileHtml(cam) {
  return `
    <div class="cctv-tile" data-action="open-device" data-floor-id="${cam.floorId}" data-device-id="${cam.device.id}" role="button" tabindex="0">
      ${buildCameraVisual(cam.device)}
      <div class="cctv-tile__hud">
        ${cameraRecBadge(cam.device)}
        <div class="cctv-tile__bottom">
          <div>
            <div class="cctv-tile__label">${esc(cam.device.name)}</div>
            <div class="cctv-tile__loc">${esc(cam.floorName)} · ${esc(cam.roomName)}</div>
          </div>
        </div>
      </div>
    </div>`;
}

/* ============================================================================
   Rendering — Floor (grid + rooms)
   ============================================================================ */
function renderFloor() {
  const floor = getFloor(nav.floorId);
  if (!floor) { nav = { view: "home" }; renderHome(); return; }

  const rooms = roomsArr(floor);
  const devices = devicesArr(floor);
  const cols = floor.gridCols || 8;
  const rows = floor.gridRows || 6;

  let cells = "";
  for (let y = 0; y < rows; y++) {
    for (let x = 0; x < cols; x++) {
      const room = roomForCellXY(rooms, x, y);
      // Soft constraint, same as the app: first device found at this cell wins.
      const device = devices.find((d) => d.gridX === x && d.gridY === y);
      const bg = room ? hexToRgba(roomColorFor(room, rooms), 0.22) : "transparent";
      let inner = "";
      let attrs = "";
      if (device) {
        inner = `<span class="device-dot ${statusClass(fromStatus(device.status))}"></span>`;
        attrs = `data-clickable="1" data-action="open-device" data-floor-id="${floor.id}" data-device-id="${device.id}" role="button" tabindex="0"`;
      } else if (room) {
        attrs = `data-clickable="1" data-action="go-room" data-floor-id="${floor.id}" data-room-id="${room.id}" role="button" tabindex="0"`;
      }
      cells += `<div class="grid-cell" style="background:${bg};" ${attrs}>${inner}</div>`;
    }
  }

  let html = `
    <div class="view-header"><button class="back-btn" data-action="go-home">← Floors</button></div>
    <div class="view-heading" style="margin-bottom:16px;">
      <div class="view-heading__icon">${esc(floor.iconEmoji || "🏠")}</div>
      <div>
        <div class="view-title">${esc(floor.name || "Untitled floor")}</div>
        <div class="view-subtitle">${cols}×${rows} grid · ${rooms.length} room${rooms.length === 1 ? "" : "s"} · ${devices.length} device${devices.length === 1 ? "" : "s"}</div>
      </div>
    </div>
    <div class="floor-plan-wrap">
      <div class="floor-grid-inner" style="grid-template-columns:repeat(${cols},1fr);">${cells}</div>
    </div>`;

  if (rooms.length) {
    html += `<div class="room-legend">${rooms.map((r) => `
      <div class="room-legend__item"><span class="room-legend__swatch" style="background:${roomColorFor(r, rooms)}"></span>${esc(r.icon || "🏠")} ${esc(r.name)}</div>
    `).join("")}</div>`;
  }

  html += `<div class="section-label">Rooms</div>`;
  html += !rooms.length
    ? `<div class="empty-state"><strong>No rooms defined yet.</strong><br/>Draw a room on this floor from the app to see it here.</div>`
    : `<div class="room-list">${rooms.map((r) => roomCardHtml(floor, r, devices, rooms)).join("")}</div>`;

  viewRoot.innerHTML = html;
}

function roomCardHtml(floor, room, allDevices, rooms) {
  const roomDevices = allDevices.filter((d) => d.roomId === room.id);
  const cams = roomDevices.filter((d) => fromType(d.type) === "CAMERA").length;
  return `
    <div class="room-card" data-action="go-room" data-floor-id="${floor.id}" data-room-id="${room.id}" role="button" tabindex="0">
      <div class="room-card__icon" style="background:${hexToRgba(roomColorFor(room, rooms), 0.25)};">${esc(room.icon || "🏠")}</div>
      <div>
        <div class="room-card__name">${esc(room.name || "Room")}</div>
        <div class="room-card__meta">${roomDevices.length} device${roomDevices.length === 1 ? "" : "s"}${cams ? ` · ${cams} camera${cams === 1 ? "" : "s"}` : ""}</div>
      </div>
      <div class="room-card__chevron">→</div>
    </div>`;
}

/* ============================================================================
   Rendering — Room (the "switch view")
   ============================================================================ */
function renderRoom() {
  const floor = getFloor(nav.floorId);
  if (!floor) { nav = { view: "home" }; renderHome(); return; }
  const rooms = roomsArr(floor);
  const room = rooms.find((r) => r.id === nav.roomId);
  if (!room) { nav = { view: "floor", floorId: floor.id }; renderFloor(); return; }

  const devices = devicesArr(floor).filter((d) => d.roomId === room.id);

  let html = `
    <div class="view-header"><button class="back-btn" data-action="go-floor" data-floor-id="${floor.id}">← ${esc(floor.name || "Floor")}</button></div>
    <div class="view-heading" style="margin-bottom:18px;">
      <div class="view-heading__icon">${esc(room.icon || "🏠")}</div>
      <div>
        <div class="view-title">${esc(room.name)}</div>
        <div class="view-subtitle">${devices.length} device${devices.length === 1 ? "" : "s"} in this room</div>
      </div>
    </div>`;

  html += !devices.length
    ? `<div class="empty-state"><strong>No devices in ${esc(room.name)} yet.</strong><br/>Add one from the app's + button on this room.</div>`
    : `<div class="switch-list">${devices.map((d) => switchCardHtml(floor, d)).join("")}</div>`;

  viewRoot.innerHTML = html;
}

function switchCardHtml(floor, device) {
  const type = fromType(device.type);
  const status = fromStatus(device.status);
  const meta = TYPE_META[type];
  const isOn = status === "ON";
  const toggleable = status === "ON" || status === "OFF";

  // Matches RoomDeviceRow in RoomScreen.kt: every type except Camera gets a
  // single device-level switch here (not per-sub-switch — that's only in
  // the full Device Panel for Multi-Switch).
  const rightControl = type === "CAMERA"
    ? `<button class="toggle-view-btn" data-action="open-device" data-floor-id="${floor.id}" data-device-id="${device.id}">View feed</button>`
    : `<div class="toggle ${isOn ? "on" : ""} ${toggleable ? "" : "disabled"}" data-action="toggle-device" data-floor-id="${floor.id}" data-device-id="${device.id}"></div>`;

  return `
    <div class="switch-card">
      <div class="switch-card__icon" data-action="open-device" data-floor-id="${floor.id}" data-device-id="${device.id}" role="button" tabindex="0">${meta.icon}</div>
      <div class="switch-card__body" data-action="open-device" data-floor-id="${floor.id}" data-device-id="${device.id}" role="button" tabindex="0">
        <div class="switch-card__name">${esc(device.name)}</div>
        <div class="switch-card__meta">
          <span class="status-chip ${statusClass(status)}"><span class="dot"></span>${status}</span>
          <span>${meta.label}</span>
        </div>
      </div>
      ${rightControl}
    </div>`;
}

/* ============================================================================
   Rendering — Breadcrumb
   ============================================================================ */
function renderBreadcrumb() {
  const parts = [`<button class="crumb-btn" data-action="go-home">Home</button>`];
  if (nav.view === "floor" || nav.view === "room") {
    const floor = getFloor(nav.floorId);
    if (floor) parts.push(`<span class="crumb-sep">/</span><button class="crumb-btn" data-action="go-floor" data-floor-id="${floor.id}">${esc(floor.name || "Floor")}</button>`);
  }
  if (nav.view === "room") {
    const floor = getFloor(nav.floorId);
    const room = floor ? roomsArr(floor).find((r) => r.id === nav.roomId) : null;
    if (room) parts.push(`<span class="crumb-sep">/</span><span class="crumb-current">${esc(room.name)}</span>`);
  }
  breadcrumbEl.innerHTML = parts.join("");
}

/* ============================================================================
   Rendering — Device modal (one layout per DeviceType)
   ============================================================================ */
function heroBlockHtml(type, status) {
  const meta = TYPE_META[type];
  const visual = meta.heroImg
    ? `<img src="${meta.heroImg}" alt="${meta.label}" style="width:104px;height:auto;object-fit:contain;margin-bottom:6px;" />`
    : `<div class="device-hero__icon">${meta.icon}</div>`;
  return `<div class="device-hero">${visual}<div class="status-chip ${statusClass(status)}" style="margin-top:8px;"><span class="dot"></span>${status}</div></div>`;
}

function outletPanelHtml(device, floorId) {
  const status = fromStatus(device.status);
  const isOn = status === "ON";
  const toggleable = status === "ON" || status === "OFF";
  const hint = status === "DISCONNECTED" ? "Offline — reconnect below to restore control."
    : status === "ERROR" ? "Reporting a fault — reconnect below to clear it."
      : "Simple single-node ON/OFF power outlet.";
  return `
    ${heroBlockHtml("OUTLET", status)}
    <div class="field-row">
      <span class="field-row__label">Power</span>
      <div class="toggle ${isOn ? "on" : ""} ${toggleable ? "" : "disabled"}" data-action="toggle-device" data-floor-id="${floorId}" data-device-id="${device.id}"></div>
    </div>
    <p class="field-hint">${hint}</p>`;
}

function multiSwitchPanelHtml(device, floorId) {
  const status = fromStatus(device.status);
  const subs = subSwitchEntries(device);
  const rows = !subs.length
    ? `<div class="empty-state">No sub-switches configured for this unit.</div>`
    : subs.map(([key, s]) => {
      const sOn = fromStatus(s.status) === "ON";
      return `
        <div class="subswitch-row">
          <div>
            <div style="font-size:13.5px;font-weight:600;">${esc(s.label || `Switch ${s.id || key}`)}</div>
            <div class="field-hint" style="margin-top:2px;">${fromStatus(s.status)}</div>
          </div>
          <div class="toggle ${sOn ? "on" : ""}" data-action="toggle-subswitch" data-floor-id="${floorId}" data-device-id="${device.id}" data-sub-key="${esc(key)}" data-sub-id="${esc(s.id || "")}"></div>
        </div>`;
    }).join("");
  return `
    ${heroBlockHtml("MULTI_SWITCH", status)}
    <p class="field-hint" style="text-align:center;margin-bottom:14px;">${subs.length} sub-switch${subs.length === 1 ? "" : "es"} on this gang-box — each toggles independently.</p>
    ${rows}`;
}

function scheduledAppliancePanelHtml(device, floorId) {
  const status = fromStatus(device.status);
  const isOn = status === "ON";
  const toggleable = status === "ON" || status === "OFF";
  const maxDuration = device.maxOnDurationSeconds ?? MAX_ON_DURATION_DEFAULT_SECONDS;
  const circumference = 2 * Math.PI * 52;
  return `
    ${heroBlockHtml("SCHEDULED_APPLIANCE", status)}
    <div class="countdown-ring-wrap">
      <svg viewBox="0 0 120 120">
        <circle class="countdown-ring-bg" cx="60" cy="60" r="52"></circle>
        <circle class="countdown-ring-fg" id="countdownRingFg" cx="60" cy="60" r="52" stroke-dasharray="${circumference}" stroke-dashoffset="0"></circle>
      </svg>
      <div class="countdown-ring-label">
        <div class="countdown-ring-label__time" id="countdownTimeLabel">--:--</div>
        <div class="countdown-ring-label__caption" id="countdownCaption">not running</div>
      </div>
    </div>
    <div class="field-row"><span class="field-row__label">Max ON duration</span><span class="field-hint">${maxDuration}s</span></div>
    <div class="field-row">
      <span class="field-row__label">Power</span>
      <div class="toggle ${isOn ? "on" : ""} ${toggleable ? "" : "disabled"}" data-action="toggle-device" data-floor-id="${floorId}" data-device-id="${device.id}"></div>
    </div>
    <p class="field-hint" id="cutoffWarning"></p>`;
}

function lightSchedulePanelHtml(device, floorId) {
  const status = fromStatus(device.status);
  const isOn = status === "ON";
  const start = device.scheduleStart || "18:00";
  const end = device.scheduleEnd || "06:00";
  const enabled = !!device.scheduleEnabled;
  return `
    <div class="device-hero">
      <div class="bulb-visual ${isOn ? "on" : ""}">💡</div>
      <div class="status-chip ${statusClass(status)}" style="margin-top:10px;"><span class="dot"></span>${status}</div>
      <div class="field-hint" style="margin-top:8px;font-size:12.5px;">${esc(nextTransitionText(device.scheduleStart, device.scheduleEnd, device.scheduleEnabled))}</div>
    </div>
    <div class="field-row">
      <span class="field-row__label">Schedule enabled</span>
      <div class="toggle ${enabled ? "on" : ""}" id="scheduleEnabledToggle" data-action="toggle-schedule-enabled-local"></div>
    </div>
    <div class="field-block">
      <label class="field-label">On time (24h HH:mm)</label>
      <input class="field-input" id="scheduleStartInput" value="${esc(start)}" placeholder="18:00" />
    </div>
    <div class="field-block">
      <label class="field-label">Off time (24h HH:mm)</label>
      <input class="field-input" id="scheduleEndInput" value="${esc(end)}" placeholder="06:00" />
    </div>
    <button class="btn primary full" data-action="save-schedule" data-floor-id="${floorId}" data-device-id="${device.id}">Save schedule</button>
    <p class="field-hint">While enabled, the on-page safety sweep flips this ON/OFF to match the window automatically — no need for the app to be open.</p>`;
}

function cameraPanelHtml(device, floorId) {
  const status = fromStatus(device.status);
  return `
    <div class="cctv-modal-feed">${buildCameraVisual(device)}<div class="cctv-tile__hud">${cameraRecBadge(device)}</div></div>
    <div class="status-chip ${statusClass(status)}" style="margin-bottom:14px;"><span class="dot"></span>${status}</div>
    <div class="field-block">
      <label class="field-label">Mock snapshot URL</label>
      <input class="field-input" id="snapshotUrlInput" value="${esc(device.snapshotUrl || "")}" placeholder="https://…" />
      <p class="field-hint">Paste any public image URL to fake a live feed — the app's Camera screen reads this exact field.</p>
    </div>
    <button class="btn primary full" data-action="save-snapshot" data-floor-id="${floorId}" data-device-id="${device.id}">Save snapshot URL</button>`;
}

function simControlsFooterHtml(device, floorId, deviceId) {
  const type = fromType(device.type);
  const status = fromStatus(device.status);
  const skipBtn = (type === "SCHEDULED_APPLIANCE" && status === "ON")
    ? `<button class="btn small" data-action="skip-ahead" data-floor-id="${floorId}" data-device-id="${deviceId}">⏩ Skip to near-cutoff</button>`
    : "";
  return `
    <div class="sim-controls">
      <div class="sim-controls__label">⚙ Simulator controls — not part of the app</div>
      <div class="btn-row">
        <button class="btn warn small" data-action="force-status" data-status="ERROR" data-floor-id="${floorId}" data-device-id="${deviceId}">Force ERROR</button>
        <button class="btn ghost-disc small" data-action="force-status" data-status="DISCONNECTED" data-floor-id="${floorId}" data-device-id="${deviceId}">Force DISCONNECTED</button>
        <button class="btn small" data-action="reconnect" data-floor-id="${floorId}" data-device-id="${deviceId}">Reconnect</button>
        ${skipBtn}
      </div>
    </div>`;
}

function buildDeviceModal(floorId, deviceId) {
  const device = getDevice(floorId, deviceId);
  if (!device) { closeModal(); return; }
  const type = fromType(device.type);
  const meta = TYPE_META[type];

  let body = "";
  if (type === "OUTLET") body = outletPanelHtml(device, floorId);
  else if (type === "MULTI_SWITCH") body = multiSwitchPanelHtml(device, floorId);
  else if (type === "SCHEDULED_APPLIANCE") body = scheduledAppliancePanelHtml(device, floorId);
  else if (type === "LIGHT_SCHEDULE") body = lightSchedulePanelHtml(device, floorId);
  else if (type === "CAMERA") body = cameraPanelHtml(device, floorId);

  deviceModalPanel.innerHTML = `
    <div class="modal-header">
      <div>
        <div class="modal-header__title">${esc(device.name)}</div>
        <div class="modal-header__type">${meta.icon} ${meta.label} · grid (${device.gridX ?? 0},${device.gridY ?? 0})</div>
      </div>
      <button class="modal-close" data-action="close-modal" aria-label="Close">✕</button>
    </div>
    ${body}
    ${simControlsFooterHtml(device, floorId, deviceId)}`;

  if (type === "SCHEDULED_APPLIANCE") updateCountdownDom(device);
}

function openDeviceModal(floorId, deviceId) {
  openDeviceRef = { floorId, deviceId };
  buildDeviceModal(floorId, deviceId);
  deviceModalBackdrop.classList.remove("hidden");
}
function closeModal() {
  openDeviceRef = null;
  deviceModalBackdrop.classList.add("hidden");
}

// Called whenever fresh Firebase data arrives while a modal is open. Skips
// the rebuild if the user is actively typing (schedule times / snapshot
// URL), so a live update from elsewhere can't wipe out in-progress input.
function refreshOpenModalIfSafe() {
  if (!openDeviceRef) return;
  const device = getDevice(openDeviceRef.floorId, openDeviceRef.deviceId);
  if (!device) { closeModal(); return; }
  const active = document.activeElement;
  const typingInModal = active && deviceModalPanel.contains(active) && active.tagName === "INPUT";
  if (typingInModal) return;
  buildDeviceModal(openDeviceRef.floorId, openDeviceRef.deviceId);
}

// Ticks every second purely client-side (mirrors ScheduledApplianceControl's
// LaunchedEffect + delay(1000) loop) — recomputes from the last known
// turnedOnAtEpochMs, no Firebase read needed for the tick itself.
function updateCountdownDom(device) {
  const ringFg = document.getElementById("countdownRingFg");
  if (!ringFg) return;
  const timeLabel = document.getElementById("countdownTimeLabel");
  const caption = document.getElementById("countdownCaption");
  const warnEl = document.getElementById("cutoffWarning");
  const status = fromStatus(device.status);
  const maxDuration = device.maxOnDurationSeconds ?? MAX_ON_DURATION_DEFAULT_SECONDS;
  const isOn = status === "ON";

  let remaining = null;
  if (isOn && device.turnedOnAtEpochMs) {
    const elapsed = Math.floor((Date.now() - device.turnedOnAtEpochMs) / 1000);
    remaining = Math.max(0, maxDuration - elapsed);
  }

  if (remaining == null) {
    timeLabel.textContent = "--:--";
    caption.textContent = "not running";
    ringFg.style.strokeDashoffset = "0";
    ringFg.style.stroke = "var(--border)";
    warnEl.textContent = "";
    return;
  }

  timeLabel.textContent = formatTime(remaining);
  caption.textContent = "remaining";
  const circumference = 2 * Math.PI * 52;
  const fraction = maxDuration > 0 ? remaining / maxDuration : 0;
  ringFg.style.strokeDashoffset = String(circumference * (1 - fraction));

  const isNearCutoff = remaining > 0 && remaining <= WARNING_THRESHOLD_SECONDS;
  const isCutoff = remaining === 0;
  ringFg.style.stroke = (isNearCutoff || isCutoff) ? "var(--error)" : "var(--on)";
  warnEl.style.color = (isNearCutoff || isCutoff) ? "var(--error)" : "var(--text-faint)";
  warnEl.textContent = isNearCutoff ? "⚠ Approaching maximum ON duration — will auto shut off soon."
    : isCutoff ? "⚠ Maximum ON duration reached — safety sweep shuts it off within 5s."
      : "";
}

function tickCountdownIfOpen() {
  if (!openDeviceRef) return;
  const device = getDevice(openDeviceRef.floorId, openDeviceRef.deviceId);
  if (device && fromType(device.type) === "SCHEDULED_APPLIANCE") updateCountdownDom(device);
}

/* ============================================================================
   Toast + clock
   ============================================================================ */
function showToast(msg) {
  toastEl.textContent = msg;
  toastEl.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toastEl.classList.remove("show"), 2600);
}
function updateClock() { clockEl.textContent = new Date().toTimeString().slice(0, 8); }

/* ============================================================================
   Top-level render dispatch
   ============================================================================ */
function render() {
  renderBreadcrumb();
  if (nav.view === "floor") renderFloor();
  else if (nav.view === "room") renderRoom();
  else renderHome();
}

/* ============================================================================
   Event delegation — every interactive element uses data-action
   ============================================================================ */
document.addEventListener("click", async (e) => {
  const el = e.target.closest("[data-action]");
  if (!el) return;
  const action = el.dataset.action;
  const floorId = el.dataset.floorId;
  const deviceId = el.dataset.deviceId;

  try {
    switch (action) {
      case "go-home":
        nav = { view: "home" }; render(); break;
      case "go-floor":
        nav = { view: "floor", floorId }; render(); break;
      case "go-room":
        nav = { view: "room", floorId, roomId: el.dataset.roomId }; render(); break;
      case "open-device":
        openDeviceModal(floorId, deviceId); break;
      case "close-modal":
        closeModal(); break;

      case "toggle-device": {
        const device = getDevice(floorId, deviceId);
        if (!device) return;
        const status = fromStatus(device.status);
        if (status !== "ON" && status !== "OFF") { showToast(`Device is ${status} — reconnect first.`); return; }
        await writeToggleDevice(floorId, device);
        break;
      }
      case "toggle-subswitch": {
        const subKey = el.dataset.subKey;
        const device = getDevice(floorId, deviceId);
        if (!device) return;
        const entry = subSwitchEntries(device).find(([key]) => key === subKey);
        if (!entry) return;
        const newStatus = fromStatus(entry[1].status) === "ON" ? "OFF" : "ON";
        await writeToggleSubSwitch(floorId, deviceId, subKey, newStatus);
        break;
      }
      case "toggle-schedule-enabled-local":
        el.classList.toggle("on"); break;
      case "save-schedule": {
        const start = document.getElementById("scheduleStartInput").value.trim();
        const end = document.getElementById("scheduleEndInput").value.trim();
        const enabled = document.getElementById("scheduleEnabledToggle").classList.contains("on");
        if (!isValidTime(start) || !isValidTime(end)) { showToast("Use 24-hour HH:mm, e.g. 18:00"); return; }
        await writeSchedule(floorId, deviceId, start, end, enabled);
        showToast("Schedule saved");
        break;
      }
      case "save-snapshot": {
        const url = document.getElementById("snapshotUrlInput").value.trim();
        await writeSnapshotUrl(floorId, deviceId, url);
        showToast(url ? "Snapshot URL saved" : "Snapshot cleared");
        break;
      }
      case "force-status":
        await writeForceStatus(floorId, deviceId, el.dataset.status);
        showToast(`Forced ${el.dataset.status}`);
        break;
      case "reconnect":
        await writeReconnect(floorId, deviceId);
        showToast("Reconnected");
        break;
      case "skip-ahead": {
        const device = getDevice(floorId, deviceId);
        if (!device) return;
        await writeSkipAhead(floorId, device);
        showToast("Fast-forwarded — safety sweep runs within 5s");
        break;
      }
      default: break;
    }
  } catch (err) {
    console.error(err);
    showToast("Write failed — check the console / Firebase rules.");
  }
});

document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") { closeModal(); return; }
  if (e.key === "Enter" || e.key === " ") {
    const el = e.target.closest('[data-action][role="button"]');
    if (el) { e.preventDefault(); el.click(); }
  }
});

/* ============================================================================
   Firebase subscriptions + init
   ============================================================================ */
onValue(ref(db, ".info/connected"), (snap) => {
  const connected = snap.val() === true;
  connDot.classList.toggle("on", connected);
  connDot.classList.toggle("off-line", !connected);
  connLabel.textContent = connected ? "connected" : "reconnecting…";
});

onValue(ref(db, "floors"), (snap) => {
  floorsData = snap.val() || {};
  render();
  refreshOpenModalIfSafe();
}, (err) => {
  console.error(err);
  showToast("Firebase read failed — check firebaseConfig / DB rules.");
});

updateClock();
setInterval(() => { updateClock(); tickCountdownIfOpen(); }, 1000);
render();
