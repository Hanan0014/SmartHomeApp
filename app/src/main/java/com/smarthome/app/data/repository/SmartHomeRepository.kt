package com.smarthome.app.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.Floor
import com.smarthome.app.data.model.Room
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Single source of truth for all Firebase Realtime Database reads/writes.
 *
 * Bidirectional sync design:
 *  - Writes (toggleDevice, addFloor, ...) push directly to RTDB.
 *  - Reads use `addValueEventListener`, exposed as Kotlin Flows, so any
 *    change made anywhere (this app, another device, or the Cloud Function
 *    safety-cutoff worker) is pushed to the UI automatically — no manual
 *    refresh/polling needed.
 */
/** Pairs a Device with its parent floor's name — used by the Reports screen
 *  which needs to show devices from across every floor at once. */
data class DeviceWithFloor(
    val device: Device,
    val floorName: String
)

class SmartHomeRepository(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://smart-home-app-68cf4-default-rtdb.asia-southeast1.firebasedatabase.app"
    )
) {
    private val floorsRef = db.getReference("floors")

    /** Live stream of all floors, ordered by `order`. */
    fun observeFloors(): Flow<List<Floor>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val floors = snapshot.children.mapNotNull { it.getValue(Floor::class.java) }
                    .sortedBy { it.order }
                trySend(floors)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        floorsRef.addValueEventListener(listener)
        awaitClose { floorsRef.removeEventListener(listener) }
    }

    /** Live stream of every device belonging to one floor. */
    fun observeDevices(floorId: String): Flow<List<Device>> = callbackFlow {
        val ref = floorsRef.child(floorId).child("devices")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val devices = snapshot.children.mapNotNull { it.getValue(Device::class.java) }
                trySend(devices)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Live stream of a single device (used for the device detail screen). */
    fun observeDevice(floorId: String, deviceId: String): Flow<Device?> = callbackFlow {
        val ref = floorsRef.child(floorId).child("devices").child(deviceId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Device::class.java))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addFloor(floor: Floor) {
        floorsRef.child(floor.id).setValue(floor).await()
    }

    /** Phase 2: rename a floor in place (used by the Dashboard's rename menu action). */
    suspend fun renameFloor(floorId: String, newName: String) {
        floorsRef.child(floorId).child("name").setValue(newName).await()
    }

    /** Phase 2: delete a floor and everything under it (its devices go too). */
    suspend fun deleteFloor(floorId: String) {
        floorsRef.child(floorId).removeValue().await()
    }

    suspend fun addDevice(floorId: String, device: Device) {
        floorsRef.child(floorId).child("devices").child(device.id).setValue(device).await()
    }

    /** Live stream of every room defined on a floor. */
    fun observeRooms(floorId: String): Flow<List<Room>> = callbackFlow {
        val ref = floorsRef.child(floorId).child("rooms")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rooms = snapshot.children.mapNotNull { it.getValue(Room::class.java) }
                trySend(rooms)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun addRoom(floorId: String, room: Room) {
        floorsRef.child(floorId).child("rooms").child(room.id).setValue(room).await()
    }

    suspend fun deleteRoom(floorId: String, roomId: String) {
        floorsRef.child(floorId).child("rooms").child(roomId).removeValue().await()
    }

    /** Simple ON/OFF toggle for outlets and lights. Stamps turnedOnAtEpochMs
     *  whenever a device turns ON (used both for safety cutoff and for
     *  Phase 6 reporting), and accumulates totalOnTimeSeconds whenever it
     *  turns OFF, based on how long it was actually ON for. */
    suspend fun toggleDevice(floorId: String, device: Device) {
        val ref = floorsRef.child(floorId).child("devices").child(device.id)
        val turningOn = device.status != com.smarthome.app.data.model.DeviceStatus.ON
        val newStatus = if (turningOn) com.smarthome.app.data.model.DeviceStatus.ON
        else com.smarthome.app.data.model.DeviceStatus.OFF
        val now = System.currentTimeMillis()

        val updates = mutableMapOf<String, Any?>(
            "status" to newStatus.name,
            "lastToggledAtEpochMs" to now
        )

        if (turningOn) {
            updates["turnedOnAtEpochMs"] = now
        } else {
            updates["turnedOnAtEpochMs"] = null
            val sessionStart = device.turnedOnAtEpochMs
            if (sessionStart != null) {
                val elapsedSeconds = (now - sessionStart) / 1000
                updates["totalOnTimeSeconds"] = device.totalOnTimeSeconds + elapsedSeconds
            }
        }
        ref.updateChildren(updates).await()
    }

    /**
     * Live stream of every device across every floor, paired with its
     * floor's name for display. Powers the Phase 6 Reports screen, which
     * needs a global view rather than one floor at a time like
     * observeDevices() above.
     */
    fun observeAllDevices(): Flow<List<DeviceWithFloor>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val result = mutableListOf<DeviceWithFloor>()
                for (floorSnap in snapshot.children) {
                    val floorName = floorSnap.child("name").getValue(String::class.java) ?: "Unknown Floor"
                    val devicesSnap = floorSnap.child("devices")
                    for (deviceSnap in devicesSnap.children) {
                        val device = deviceSnap.getValue(Device::class.java) ?: continue
                        result.add(DeviceWithFloor(device, floorName))
                    }
                }
                trySend(result)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        floorsRef.addValueEventListener(listener)
        awaitClose { floorsRef.removeEventListener(listener) }
    }

    /** Toggle one sub-switch inside a multi-switch gang-box unit. */
    suspend fun toggleSubSwitch(floorId: String, deviceId: String, subSwitchId: String, newStatus: com.smarthome.app.data.model.DeviceStatus) {
        val ref = floorsRef.child(floorId).child("devices").child(deviceId)
            .child("subSwitches")
        // Sub-switches are stored as a list; we fetch, mutate, and rewrite the changed index.
        // For simplicity/atomicity in a mini-project this uses a transaction-free read-modify-write.
        ref.get().await().children.forEachIndexed { index, snap ->
            if (snap.child("id").getValue(String::class.java) == subSwitchId) {
                ref.child(index.toString()).child("status").setValue(newStatus.name).await()
            }
        }
    }

    suspend fun updateSchedule(floorId: String, deviceId: String, start: String, end: String, enabled: Boolean) {
        floorsRef.child(floorId).child("devices").child(deviceId).updateChildren(
            mapOf(
                "scheduleStart" to start,
                "scheduleEnd" to end,
                "scheduleEnabled" to enabled
            )
        ).await()
    }

    /**
     * Client-side safety-cutoff fallback (see FloorPlanViewModel) — force a
     * device OFF and record why, mirroring what functions/index.js does on
     * the backend. Only used because our Firebase project is on the Spark
     * tier and cannot run the real Cloud Function. Also accumulates
     * totalOnTimeSeconds, same as a normal toggle-off, so Reports stays
     * accurate even when a device is cut off automatically rather than by
     * the user.
     */
    suspend fun forceOff(floorId: String, deviceId: String, reason: String, sessionStartEpochMs: Long? = null) {
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any?>(
            "status" to com.smarthome.app.data.model.DeviceStatus.OFF.name,
            "turnedOnAtEpochMs" to null,
            "lastToggledAtEpochMs" to now,
            "lastCutoffReason" to reason
        )
        if (sessionStartEpochMs != null) {
            val elapsedSeconds = (now - sessionStartEpochMs) / 1000
            floorsRef.child(floorId).child("devices").child(deviceId).child("totalOnTimeSeconds").get().await()
                .getValue(Long::class.java)?.let { current ->
                    updates["totalOnTimeSeconds"] = current + elapsedSeconds
                } ?: run { updates["totalOnTimeSeconds"] = elapsedSeconds }
        }
        floorsRef.child(floorId).child("devices").child(deviceId).updateChildren(updates).await()
    }

    /** Client-side light-schedule fallback — same idea as forceOff above. */
    suspend fun setScheduledStatus(floorId: String, deviceId: String, status: com.smarthome.app.data.model.DeviceStatus) {
        floorsRef.child(floorId).child("devices").child(deviceId).updateChildren(
            mapOf(
                "status" to status.name,
                "lastToggledAtEpochMs" to System.currentTimeMillis()
            )
        ).await()
    }
}