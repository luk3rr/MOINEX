/*
 * Filename: DialogResize.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

/**
 * Pure geometry of resizing a dialog card by dragging its border, kept apart from JavaFX so it can be tested. The card
 * stays centered, so a drag of the cursor by one pixel changes the size by two.
 */
object DialogResize {
    const val EDGE = 6.0
    const val CORNER = 14.0
    const val MIN_WIDTH = 320.0
    const val MIN_HEIGHT = 160.0

    data class Size(
        val width: Double,
        val height: Double,
    )

    enum class Zone(
        val west: Boolean = false,
        val east: Boolean = false,
        val north: Boolean = false,
        val south: Boolean = false,
    ) {
        NONE,
        N(north = true),
        S(south = true),
        E(east = true),
        W(west = true),
        NE(north = true, east = true),
        NW(north = true, west = true),
        SE(south = true, east = true),
        SW(south = true, west = true),
    }

    /**
     * The part of the border under the point ([x], [y]) of a card of [width] by [height], or [Zone.NONE] inside.
     */
    fun zoneAt(
        x: Double,
        y: Double,
        width: Double,
        height: Double,
    ): Zone {
        if (x < 0 || y < 0 || x > width || y > height) return Zone.NONE

        val west = x <= EDGE
        val east = x >= width - EDGE
        val north = y <= EDGE
        val south = y >= height - EDGE
        val nearHorizontalCorner = x <= CORNER || x >= width - CORNER
        val nearVerticalCorner = y <= CORNER || y >= height - CORNER

        return when {
            (north || south) && nearHorizontalCorner -> corner(north, west || x <= CORNER)
            (west || east) && nearVerticalCorner -> corner(y <= CORNER, west)
            north -> Zone.N
            south -> Zone.S
            west -> Zone.W
            east -> Zone.E
            else -> Zone.NONE
        }
    }

    private fun corner(
        north: Boolean,
        west: Boolean,
    ): Zone =
        when {
            north && west -> Zone.NW
            north -> Zone.NE
            west -> Zone.SW
            else -> Zone.SE
        }

    /**
     * Size after dragging the [zone] by ([dx], [dy]) pixels from the [start] size, kept between [min] and [max].
     */
    fun resize(
        start: Size,
        zone: Zone,
        dx: Double,
        dy: Double,
        min: Size = Size(MIN_WIDTH, MIN_HEIGHT),
        max: Size,
    ): Size {
        val width =
            when {
                zone.east -> start.width + 2 * dx
                zone.west -> start.width - 2 * dx
                else -> start.width
            }
        val height =
            when {
                zone.south -> start.height + 2 * dy
                zone.north -> start.height - 2 * dy
                else -> start.height
            }

        return Size(
            width.coerceIn(min.width, maxOf(min.width, max.width)),
            height.coerceIn(min.height, maxOf(min.height, max.height)),
        )
    }
}
