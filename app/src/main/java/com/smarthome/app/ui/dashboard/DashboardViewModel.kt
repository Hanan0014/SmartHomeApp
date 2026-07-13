package com.smarthome.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.app.data.model.Floor
import com.smarthome.app.data.repository.SmartHomeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val LOAD_TIMEOUT_MS = 8_000L

class DashboardViewModel(
    private val repository: SmartHomeRepository = SmartHomeRepository()
) : ViewModel() {

    private val _floors = MutableStateFlow<List<Floor>>(emptyList())
    val floors: StateFlow<List<Floor>> = _floors.asStateFlow()

    // True until the first snapshot from Firebase arrives, OR until
    // LOAD_TIMEOUT_MS passes with nothing -- whichever comes first. Without
    // this timeout, a stalled/misconfigured Firebase connection spins the
    // loading indicator forever with no feedback to the user.
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    init {
        startObserving()
    }

    private fun startObserving() {
        _isLoading.value = true
        _loadError.value = null

        // Real-time: any floor added/removed/renamed anywhere updates this list instantly.
        viewModelScope.launch {
            repository.observeFloors()
                .collect {
                    _floors.value = it
                    _isLoading.value = false
                    _loadError.value = null
                }
        }

        // Safety-net timeout: if the collect{} above never fires (no error,
        // just silence -- e.g. Realtime Database not enabled for this
        // project, wrong region, or no network), stop the spinner and tell
        // the user instead of leaving them staring at it indefinitely.
        viewModelScope.launch {
            delay(LOAD_TIMEOUT_MS)
            if (_isLoading.value) {
                _isLoading.value = false
                _loadError.value = "Couldn't load your floors. Check your internet connection " +
                        "and that Realtime Database is enabled in the Firebase console."
            }
        }
    }

    fun retry() {
        startObserving()
    }

    fun addFloor(floor: Floor) {
        viewModelScope.launch { repository.addFloor(floor) }
    }

    fun renameFloor(floorId: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.renameFloor(floorId, trimmed) }
    }

    fun deleteFloor(floorId: String) {
        viewModelScope.launch { repository.deleteFloor(floorId) }
    }
}