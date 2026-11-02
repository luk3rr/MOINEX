/*
 * Filename: ChartCrosshair.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.chart.LineChart
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Polygon
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A line of a chart that the crosshair reads: its [label], the CSS class that colors its dot and its swatch, the points
 * (sorted by date) and how a value is written.
 */
class CrosshairSeries(
    val label: String,
    val styleClass: String,
    val points: List<Pair<LocalDate, Double>>,
    val format: (Double) -> String,
)

/**
 * A triangle drawn over the chart at a date and a value, such as a buy or a sell.
 */
class ChartMarker(
    val date: LocalDate,
    val value: Double,
    val styleClass: String,
    val pointsUp: Boolean,
    val tooltip: String,
)

/**
 * Makes a line chart follow the mouse: a vertical line snaps to the closest day, a dot marks each series on it and a box
 * lists their values. Markers are drawn in the same layer, positioned from the chart axes.
 */
object ChartCrosshair {
    private const val STYLESHEET = "/css/component/chart-crosshair.css"
    private const val BOX_OFFSET = 14.0
    private const val BOX_MARGIN = 6.0
    private const val DOT_RADIUS = 4.5
    private const val MARKER_SIZE = 6.0
    private const val PLOT_SELECTOR = ".chart-plot-background"
    private const val ROW_SPACING = 6.0
    private const val BOX_SPACING = 3.0
    private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Wraps [chart] with the interactive layer. [chartStyleClass] goes on the wrapper so the chart stylesheet reaches the
     * layer too. [notes] are lines added to the box on the day they fall on, such as the trades of that day.
     */
    fun wrap(
        chart: LineChart<Number, Number>,
        chartStyleClass: String,
        series: List<CrosshairSeries>,
        markers: List<ChartMarker> = emptyList(),
        notes: List<Pair<LocalDate, String>> = emptyList(),
    ): StackPane {
        val overlay = Overlay(chart, markers)
        val root = StackPane(chart, overlay)
        root.styleClass.add(chartStyleClass)
        root.stylesheets.addAll(chart.stylesheets)
        root.stylesheets.add(
            ChartCrosshair::class.java.getResource(STYLESHEET)?.toExternalForm()
                ?: error("Crosshair stylesheet not found"),
        )

        val days =
            series
                .maxByOrNull { it.points.size }
                ?.points
                ?.map { it.first }
                .orEmpty()
        val line =
            Line().apply {
                styleClass.add("crosshair-line")
                isVisible = false
                isMouseTransparent = true
            }
        val dots =
            series.map {
                Circle(DOT_RADIUS).apply {
                    styleClass.addAll("crosshair-dot", it.styleClass)
                    isVisible = false
                    isMouseTransparent = true
                }
            }
        val box =
            VBox(BOX_SPACING).apply {
                styleClass.add("crosshair-box")
                isVisible = false
                isMouseTransparent = true
            }
        overlay.children.addAll(line, box)
        overlay.children.addAll(dots)

        fun hide() {
            line.isVisible = false
            box.isVisible = false
            dots.forEach { it.isVisible = false }
        }

        fun update(event: MouseEvent) {
            val plot = chart.lookup(PLOT_SELECTOR) as? Region
            if (plot == null || days.isEmpty()) return hide()

            val plotBounds = root.sceneToLocal(plot.localToScene(plot.boundsInLocal))
            val mouse = root.sceneToLocal(event.sceneX, event.sceneY)
            if (mouse.x < plotBounds.minX ||
                mouse.x > plotBounds.maxX ||
                mouse.y < plotBounds.minY ||
                mouse.y > plotBounds.maxY
            ) {
                return hide()
            }

            val axisX = chart.xAxis.sceneToLocal(event.sceneX, event.sceneY).x
            val day = CrosshairMath.nearestDay(days, chart.xAxis.getValueForDisplay(axisX).toDouble()) ?: return hide()
            val lineX =
                root
                    .sceneToLocal(
                        chart.xAxis.localToScene(chart.xAxis.getDisplayPosition(day.toEpochDay()), 0.0),
                    ).x

            line.startX = lineX
            line.endX = lineX
            line.startY = plotBounds.minY
            line.endY = plotBounds.maxY
            line.isVisible = true

            box.children.setAll(header(day))
            series.forEachIndexed { index, item ->
                val value = CrosshairMath.valueOn(item.points, day)
                val dot = dots[index]
                if (value == null) {
                    dot.isVisible = false
                } else {
                    val y = root.sceneToLocal(chart.yAxis.localToScene(0.0, chart.yAxis.getDisplayPosition(value))).y
                    dot.centerX = lineX
                    dot.centerY = y
                    dot.isVisible = true
                    box.children.add(row(item, value))
                }
            }
            CrosshairMath.notesFor(notes, CrosshairMath.previousDay(days, day), day).forEach {
                box.children.add(Label(it).apply { styleClass.add("crosshair-note") })
            }

            box.isVisible = true
            box.applyCss()
            val boxWidth = box.prefWidth(-1.0)
            val boxHeight = box.prefHeight(boxWidth)
            val rightSide = lineX + BOX_OFFSET
            val x = if (rightSide + boxWidth > plotBounds.maxX) lineX - BOX_OFFSET - boxWidth else rightSide
            val y =
                (mouse.y - boxHeight / 2).coerceIn(
                    plotBounds.minY + BOX_MARGIN,
                    plotBounds.maxY - boxHeight - BOX_MARGIN,
                )
            box.resizeRelocate(x.coerceAtLeast(plotBounds.minX + BOX_MARGIN), y, boxWidth, boxHeight)
        }

        root.addEventFilter(MouseEvent.MOUSE_MOVED) { update(it) }
        root.addEventFilter(MouseEvent.MOUSE_DRAGGED) { update(it) }
        root.addEventFilter(MouseEvent.MOUSE_EXITED) { hide() }

        return root
    }

    private fun header(day: LocalDate): Node =
        Label(day.format(DATE_FORMAT)).apply { styleClass.add("crosshair-header") }

    private fun row(
        series: CrosshairSeries,
        value: Double,
    ): Node =
        HBox(
            ROW_SPACING,
            Label("●").apply { styleClass.add(series.styleClass) },
            Label(series.label).apply { styleClass.add("crosshair-label") },
            Region().apply { HBox.setHgrow(this, javafx.scene.layout.Priority.ALWAYS) },
            Label(series.format(value)).apply { styleClass.add("crosshair-value") },
        )

    /**
     * Pane over the chart where the markers are drawn. It is laid out after the chart, so the axes already have their
     * final size when it places them; it lets the mouse through everywhere except on the markers.
     */
    private class Overlay(
        private val chart: LineChart<Number, Number>,
        private val markers: List<ChartMarker>,
    ) : Pane() {
        private val nodes: List<Pair<ChartMarker, Group>> =
            markers.map { marker ->
                marker to
                    Group(
                        Polygon(*triangle(marker.pointsUp)).apply { styleClass.add(marker.styleClass) },
                    ).also { Tooltip.install(it, Tooltip(marker.tooltip)) }
            }

        init {
            isPickOnBounds = false
            children.addAll(nodes.map { it.second })
        }

        override fun layoutChildren() {
            super.layoutChildren()

            nodes.forEach { (marker, node) ->
                val x =
                    sceneToLocal(
                        chart.xAxis.localToScene(chart.xAxis.getDisplayPosition(marker.date.toEpochDay()), 0.0),
                    ).x
                val y = sceneToLocal(chart.yAxis.localToScene(0.0, chart.yAxis.getDisplayPosition(marker.value))).y
                node.relocate(x, y)
            }
        }

        private fun triangle(up: Boolean): DoubleArray =
            if (up) {
                doubleArrayOf(0.0, -MARKER_SIZE, MARKER_SIZE, MARKER_SIZE, -MARKER_SIZE, MARKER_SIZE)
            } else {
                doubleArrayOf(0.0, MARKER_SIZE, MARKER_SIZE, -MARKER_SIZE, -MARKER_SIZE, -MARKER_SIZE)
            }
    }
}
