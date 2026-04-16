package com.strangeparticle.springboard.app.ui.gridnav

sealed interface GridZoomSelection {
    data class FixedZoom(val percent: Int) : GridZoomSelection

    companion object {
        val presets: List<GridZoomSelection> = listOf(
            FixedZoom(100),
            FixedZoom(125),
            FixedZoom(150),
            FixedZoom(175),
            FixedZoom(200),
        )

        fun default(): GridZoomSelection = FixedZoom(100)

        fun fromPercent(percent: Int): GridZoomSelection = FixedZoom(percent)

        /**
         * Maps a calculated fit percentage to a conservative preset: finds the
         * nearest preset at or below [fitPercent], then steps down one more level.
         * For example, fit=210% → nearest is 200% → returns 175%.
         * Falls back to the lowest preset if already at the bottom.
         */
        fun conservativePresetFor(fitPercent: Int): FixedZoom {
            val sorted = presets.filterIsInstance<FixedZoom>().sortedBy { it.percent }
            val floorIndex = sorted.indexOfLast { it.percent <= fitPercent }
            val conservativeIndex = (floorIndex - 1).coerceAtLeast(0)
            return sorted[conservativeIndex]
        }
    }
}

val GridZoomSelection.percent: Int
    get() = when (this) {
        is GridZoomSelection.FixedZoom -> percent
    }

fun GridZoomSelection.displayLabel(): String = when (this) {
    is GridZoomSelection.FixedZoom -> "$percent%"
}
