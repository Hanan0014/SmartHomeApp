package com.smarthome.app.data.model

/**
 * A named zone within a floor (Hall, Kitchen, Bathroom, Bedroom, ...).
 * Each room has its own independent abstract grid — devices are placed at
 * (gridX, gridY) within THIS room's grid, not the floor's.
 *
 * Firebase RTDB path: /floors/{floorId}/rooms/{roomId}
 */
data class Room(
    val id: String = "",
    val name: String = "",
    val icon: String = "🏠",
    val gridCols: Int = 6,
    val gridRows: Int = 4,
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