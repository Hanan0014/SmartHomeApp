package com.smarthome.app.ui.floorplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.data.model.Device
import com.smarthome.app.data.repository.SmartHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FloorPlanViewModel(
    private val floorId: String,
    private val repository: SmartHomeRepository = SmartHomeRepository()
) : ViewModel() {

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    init {
        // Real-time: reflects toggles made from this app, another user's phone,
        // or the backend safety-cutoff worker — no manual refresh needed.
        viewModelScope.launch {
            repository.observeDevices(floorId).collect { _devices.value = it }
        }
    }

    fun toggleDevice(device: Device) {
        viewModelScope.launch { repository.toggleDevice(floorId, device) }
    }

    fun addDevice(device: Device) {
        viewModelScope.launch { repository.addDevice(floorId, device) }
    }
}
