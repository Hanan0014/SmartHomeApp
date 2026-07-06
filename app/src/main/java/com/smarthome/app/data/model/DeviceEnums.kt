package com.smarthome.app.data.model

/**
 * Operational status of any device in the system.
 * Mirrors the "status" field stored in Firebase Realtime Database.
 */
enum class DeviceStatus {
    ON, OFF, ERROR, DISCONNECTED;

    companion object {
        fun fromString(value: String?): DeviceStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DISCONNECTED
    }
}

/**
 * The heterogeneous device profiles required by the spec.
 */
enum class DeviceType {
    OUTLET,          // simple single-node binary power supply
    MULTI_SWITCH,    // gang-box with N individually addressable switches
    SCHEDULED_APPLIANCE, // fire-hazard-prone appliances (irons) with max_on_duration
    LIGHT_SCHEDULE,  // light bulbs with an on/off preset time window
    CAMERA;          // mock camera snapshot / stream

    companion object {
        fun fromString(value: String?): DeviceType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OUTLET
    }
}
