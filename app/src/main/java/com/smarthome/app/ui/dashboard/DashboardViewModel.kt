package com.smarthome.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.data.model.Floor
import com.smarthome.app.data.repository.SmartHomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: SmartHomeRepository = SmartHomeRepository()
) : ViewModel() {

    private val _floors = MutableStateFlow<List<Floor>>(emptyList())
    val floors: StateFlow<List<Floor>> = _floors.asStateFlow()

    init {
        // Real-time: any floor added/removed anywhere updates this list instantly.
        viewModelScope.launch {
            repository.observeFloors().collect { _floors.value = it }
        }
    }

    fun addFloor(floor: Floor) {
        viewModelScope.launch { repository.addFloor(floor) }
    }
}
