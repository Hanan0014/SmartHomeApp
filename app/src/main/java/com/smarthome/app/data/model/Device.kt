package com.smarthome.app.data.model

/**
 * A single addressable switch inside a Multi-Switch gang-box unit.
 */
data class SubSwitch(
    val id: String = "",
    val label: String = "",
    val status: DeviceStatus = DeviceStatus.OFF
)

/**
 * Unified device model. Not every field applies to every DeviceType —
 * fields are nullable/defaulted so a single flat schema can represent
 * outlets, multi-switches, scheduled appliances, light schedules and cameras.
 *
 * Firebase RTDB path: /floors/{floorId}/devices/{deviceId}
 */
data class Device(
    val id: String = "",
    val name: String = "",
    val type: DeviceType = DeviceType.OUTLET,
    val status: DeviceStatus = DeviceStatus.OFF,

    // Grid placement on the abstract floor-plan overlay
    val gridX: Int = 0,
    val gridY: Int = 0,

    // Multi-switch unit: list of individually addressable sub-switches
    val subSwitches: List<SubSwitch> = emptyList(),

    // Scheduled / fire-hazard appliances (irons, etc.)
    val maxOnDurationSeconds: Long? = null,
    val turnedOnAtEpochMs: Long? = null,
    val power: Int?,

    // Light schedule window (HH:mm 24h strings)
    val scheduleStart: String? = null,
    val scheduleEnd: String? = null,
    val scheduleEnabled: Boolean = false,

    // Camera
    val snapshotUrl: String? = null,
    val streamUri: String? = null,

    // Reporting
    val lastToggledAtEpochMs: Long? = null,
    val totalOnTimeSeconds: Long = 0L
) {
    // Note: all properties have default values, so Kotlin already generates the
    // no-arg constructor Firebase's deserializer needs.

    fun isSafetyCritical(): Boolean =
        type == DeviceType.SCHEDULED_APPLIANCE && maxOnDurationSeconds != null
}
