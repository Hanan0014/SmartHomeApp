package com.smarthome.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.data.repository.DeviceWithFloor
import com.smarthome.app.data.repository.SmartHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ReportSort { MOST_USED, RECENTLY_TOGGLED }

class ReportsViewModel(
    private val repository: SmartHomeRepository = SmartHomeRepository()
) : ViewModel() {

    private val _allDevices = MutableStateFlow<List<DeviceWithFloor>>(emptyList())
    private val _sortMode = MutableStateFlow(ReportSort.MOST_USED)
    val sortMode: StateFlow<ReportSort> = _sortMode.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _displayedDevices = MutableStateFlow<List<DeviceWithFloor>>(emptyList())
    val displayedDevices: StateFlow<List<DeviceWithFloor>> = _displayedDevices.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllDevices().collect {
                _allDevices.value = it
                _isLoading.value = false
                applySort()
            }
        }
    }

    fun setSortMode(mode: ReportSort) {
        _sortMode.value = mode
        applySort()
    }

    private fun applySort() {
        val devices = _allDevices.value
        _displayedDevices.value = when (_sortMode.value) {
            ReportSort.MOST_USED -> devices.sortedByDescending { it.device.totalOnTimeSeconds }
            ReportSort.RECENTLY_TOGGLED -> devices.sortedByDescending { it.device.lastToggledAtEpochMs ?: 0L }
        }
    }
}