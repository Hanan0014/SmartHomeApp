package com.smarthome.app.data.model

/**
 * A named zone within a floor (Hall, Kitchen, Bathroom, Bedroom, ...),
 * defined by the set of grid cells it occupies. Devices placed inside those
 * cells belong to this room (see Device.roomId).
 *
 * Firebase RTDB path: /floors/{floorId}/rooms/{roomId}
 */
data class Room(
    val id: String = "",
    val name: String = "",
    val icon: String = "🏠",
    // Stored as "x,y" strings since RTDB doesn't support nested arrays of
    // pairs cleanly via the default POJO mapper — parsed back into
    // Pair<Int, Int> by RoomCell helpers below.
    val cells: List<String> = emptyList()
) {
    fun containsCell(x: Int, y: Int): Boolean = cells.contains(cellKey(x, y))

    companion object {
        fun cellKey(x: Int, y: Int): String = "$x,$y"
    }
}

// A small curated set of room types with sensible default icons — user can
// still type any custom name, this just speeds up the common case.
val ROOM_TYPE_PRESETS = listOf(
    "Hall" to "🛋️",
    "Kitchen" to "🍳",
    "Bedroom" to "🛏️",
    "Bathroom" to "🛁",
    "Garage" to "🚗",
    "Living Room" to "📺",
    "Dining Room" to "🍽️",
    "Office" to "💼"
)