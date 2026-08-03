package com.smarthome.app.ui.floorplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.data.model.DeviceType
import com.smarthome.app.data.model.Room
import com.smarthome.app.data.repository.SmartHomeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val MAX_ON_DURATION_DEFAULT_SECONDS = 15 * 60L
private const val SAFETY_CHECK_INTERVAL_MS = 5_000L
private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

class FloorPlanViewModel(
    private val floorId: String,
    private val repository: SmartHomeRepository = SmartHomeRepository()
) : ViewModel() {

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    init {
        // Real-time: reflects toggles made from this app or another user's phone.
        viewModelScope.launch {
            repository.observeDevices(floorId).collect { _devices.value = it }
        }

        viewModelScope.launch {
            repository.observeRooms(floorId).collect { _rooms.value = it }
        }

        // KNOWN LIMITATION: our Firebase project is on the Spark (free) tier,
        // which cannot run Cloud Functions (they require the Blaze plan). The
        // real backend safety-cutoff worker in functions/index.js is written
        // and ready, but cannot be deployed under this constraint.
        //
        // As a client-side fallback, this loop performs the same checks
        // (safety cutoff + light schedule) locally, once every 5 seconds,
        // for as long as this floor's screen is open. This is a disclosed
        // deviation from the spec's "backend cloud listener or worker
        // process" requirement — it only runs while some device has the app
        // open on this floor, not continuously in the background.
        viewModelScope.launch {
            while (true) {
                runSafetyChecks(_devices.value)
                delay(SAFETY_CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun runSafetyChecks(currentDevices: List<Device>) {
        val now = System.currentTimeMillis()
        val nowHHmm = LocalTime.now().format(TIME_FORMATTER)

        for (device in currentDevices) {
            when (device.type) {
                DeviceType.SCHEDULED_APPLIANCE -> {
                    val turnedOnAt = device.turnedOnAtEpochMs
                    val maxDuration = device.maxOnDurationSeconds ?: MAX_ON_DURATION_DEFAULT_SECONDS
                    if (device.status == DeviceStatus.ON && turnedOnAt != null) {
                        val elapsedSeconds = (now - turnedOnAt) / 1000
                        if (elapsedSeconds >= maxDuration) {
                            repository.forceOff(
                                floorId, device.id,
                                "exceeded max on-duration of ${maxDuration}s",
                                sessionStartEpochMs = turnedOnAt
                            )
                        }
                    }
                }
                DeviceType.LIGHT_SCHEDULE -> {
                    if (device.scheduleEnabled) {
                        val shouldBeOn = isWithinWindow(nowHHmm, device.scheduleStart, device.scheduleEnd)
                        val desired = if (shouldBeOn) DeviceStatus.ON else DeviceStatus.OFF
                        if (device.status != desired) {
                            repository.setScheduledStatus(floorId, device.id, desired)
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    private fun isWithinWindow(nowHHmm: String, start: String?, end: String?): Boolean {
        if (start.isNullOrBlank() || end.isNullOrBlank()) return false
        return if (start <= end) {
            nowHHmm >= start && nowHHmm < end
        } else {
            // Window crosses midnight, e.g. 22:00 -> 02:00
            nowHHmm >= start || nowHHmm < end
        }
    }

    fun toggleDevice(device: Device) {
        viewModelScope.launch { repository.toggleDevice(floorId, device) }
    }

    fun addDevice(device: Device) {
        viewModelScope.launch { repository.addDevice(floorId, device) }
    }

    fun addRoom(room: Room) {
        viewModelScope.launch { repository.addRoom(floorId, room) }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch { repository.deleteRoom(floorId, roomId) }
    }

    /** Which room (if any) a given grid cell currently belongs to. */
    fun roomForCell(x: Int, y: Int): Room? = _rooms.value.firstOrNull { it.containsCell(x, y) }
}