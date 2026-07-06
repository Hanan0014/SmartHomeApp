/**
 * Smart Home Monitoring & Control System — Cloud Functions
 *
 * 1. safetyCutoffWatcher
 *    Fires on every device write. If a safety-critical device (type =
 *    SCHEDULED_APPLIANCE, e.g. an iron) has been ON longer than its
 *    maxOnDurationSeconds, this force-flips it to OFF in the Realtime
 *    Database and sends an FCM push alert. This is the backend
 *    "server-side safety cutoff" required by the spec.
 *
 * 2. sweepOverdueDevices (scheduled, every minute)
 *    A belt-and-suspenders sweep across ALL floors/devices, in case a
 *    device was left ON and no further writes happened to trigger the
 *    per-write watcher above (e.g. the app was closed).
 *
 * 3. lightScheduleSweep (scheduled, every minute)
 *    Turns LIGHT_SCHEDULE devices ON/OFF automatically based on their
 *    configured scheduleStart / scheduleEnd time window.
 */

const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");
const { onValueWritten } = require("firebase-functions/v2/database");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const logger = require("firebase-functions/logger");

initializeApp();

const MAX_ON_DURATION_DEFAULT_SECONDS = 15 * 60; // fallback safety net: 15 min

/** Force a device OFF and record why, then push an alert. */
async function cutOffDevice(floorId, deviceId, deviceName, reason) {
  const db = getDatabase();
  const deviceRef = db.ref(`floors/${floorId}/devices/${deviceId}`);

  await deviceRef.update({
    status: "OFF",
    turnedOnAtEpochMs: null,
    lastToggledAtEpochMs: Date.now(),
    lastCutoffReason: reason
  });

  logger.warn(`Safety cutoff: ${deviceName} (${floorId}/${deviceId}) — ${reason}`);

  try {
    await getMessaging().send({
      topic: "safety_alerts",
      notification: {
        title: "Safety Cutoff Triggered",
        body: `${deviceName} was automatically turned off (${reason}).`
      }
    });
  } catch (err) {
    logger.error("Failed to send safety alert push", err);
  }
}

/** Per-write trigger: reacts immediately when a device node changes. */
exports.safetyCutoffWatcher = onValueWritten(
  "/floors/{floorId}/devices/{deviceId}",
  async (event) => {
    const after = event.data.after.val();
    if (!after) return;

    const isScheduledAppliance = after.type === "SCHEDULED_APPLIANCE";
    const isOn = after.status === "ON";
    const maxDuration = after.maxOnDurationSeconds || MAX_ON_DURATION_DEFAULT_SECONDS;

    if (!isScheduledAppliance || !isOn || !after.turnedOnAtEpochMs) return;

    const elapsedSeconds = (Date.now() - after.turnedOnAtEpochMs) / 1000;
    if (elapsedSeconds >= maxDuration) {
      await cutOffDevice(
        event.params.floorId,
        event.params.deviceId,
        after.name || "Device",
        `exceeded max on-duration of ${maxDuration}s`
      );
    }
  }
);

/** Scheduled sweep every minute — catches devices left on with no new writes. */
exports.sweepOverdueDevices = onSchedule("every 1 minutes", async () => {
  const db = getDatabase();
  const snapshot = await db.ref("floors").get();
  if (!snapshot.exists()) return;

  const floors = snapshot.val();
  const tasks = [];

  for (const floorId of Object.keys(floors)) {
    const devices = floors[floorId].devices || {};
    for (const deviceId of Object.keys(devices)) {
      const device = devices[deviceId];
      const maxDuration = device.maxOnDurationSeconds || MAX_ON_DURATION_DEFAULT_SECONDS;

      if (
        device.type === "SCHEDULED_APPLIANCE" &&
        device.status === "ON" &&
        device.turnedOnAtEpochMs &&
        (Date.now() - device.turnedOnAtEpochMs) / 1000 >= maxDuration
      ) {
        tasks.push(cutOffDevice(floorId, deviceId, device.name || "Device", "overdue sweep"));
      }
    }
  }

  await Promise.all(tasks);
});

/** Scheduled sweep every minute — applies light on/off schedules. */
exports.lightScheduleSweep = onSchedule("every 1 minutes", async () => {
  const db = getDatabase();
  const snapshot = await db.ref("floors").get();
  if (!snapshot.exists()) return;

  const now = new Date();
  const nowHHmm = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
  const floors = snapshot.val();
  const updates = {};

  for (const floorId of Object.keys(floors)) {
    const devices = floors[floorId].devices || {};
    for (const deviceId of Object.keys(devices)) {
      const device = devices[deviceId];
      if (device.type !== "LIGHT_SCHEDULE" || !device.scheduleEnabled) continue;

      const shouldBeOn = isWithinWindow(nowHHmm, device.scheduleStart, device.scheduleEnd);
      const desiredStatus = shouldBeOn ? "ON" : "OFF";

      if (device.status !== desiredStatus) {
        updates[`floors/${floorId}/devices/${deviceId}/status`] = desiredStatus;
        updates[`floors/${floorId}/devices/${deviceId}/lastToggledAtEpochMs`] = Date.now();
      }
    }
  }

  if (Object.keys(updates).length > 0) {
    await db.ref().update(updates);
  }
});

function isWithinWindow(nowHHmm, start, end) {
  if (!start || !end) return false;
  // Handles windows that cross midnight (e.g. 22:00 -> 02:00)
  if (start <= end) {
    return nowHHmm >= start && nowHHmm < end;
  }
  return nowHHmm >= start || nowHHmm < end;
}
