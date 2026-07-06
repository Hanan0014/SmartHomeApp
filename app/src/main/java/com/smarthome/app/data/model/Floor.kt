package com.smarthome.app.data.model

/**
 * A house floor. `planImageRes` points to a bundled sample floor-plan drawable;
 * the abstract grid (gridCols x gridRows) is overlaid on top of it and devices
 * are placed at (gridX, gridY) cells within that grid.
 *
 * Firebase RTDB path: /floors/{floorId}
 */
data class Floor(
    val id: String = "",
    val name: String = "",
    val planImageName: String = "",   // drawable resource name, e.g. "floor_plan_1"
    val gridCols: Int = 8,
    val gridRows: Int = 6,
    val order: Int = 0
)
