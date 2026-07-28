package com.smarthome.app.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.model.DeviceStatus
import com.smarthome.app.data.repository.SmartHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Phase 4 fix: the previous DeviceDetailScreen was handed a static Device
 * snapshot and every control only mutated local remember{} state — nothing
 * was ever written to Firebase, and nothing reflected live changes (e.g. the
 * Cloud Function safety-cutoff force-flipping status to OFF). This ViewModel
 * observes the device live and exposes writes that go through the repository,
 * same pattern as FloorPlanViewModel/DashboardViewModel.
 */
class DeviceDetailViewModel(
    private val floorId: String,
    private val deviceId: String,
    private val repository: SmartHomeRepository = SmartHomeRepository()
) : ViewModel() {

    private val _device = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _device.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeDevice(floorId, deviceId).collect { _device.value = it }
        }
    }

    fun toggleDevice() {
        val current = _device.value ?: return
        viewModelScope.launch { repository.toggleDevice(floorId, current) }
    }

    fun toggleSubSwitch(subSwitchId: String, newStatus: DeviceStatus) {
        viewModelScope.launch { repository.toggleSubSwitch(floorId, deviceId, subSwitchId, newStatus) }
    }

    fun updateSchedule(start: String, end: String, enabled: Boolean) {
        viewModelScope.launch { repository.updateSchedule(floorId, deviceId, start, end, enabled) }
    }
}