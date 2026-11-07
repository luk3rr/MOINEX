/*
 * Filename: ChartCrosshair.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.chart.LineChart
import javafx.scene.chart.XYChart
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Polygon
import javafx.scene.shape.Shape
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
 * Makes a line chart follow the mouse: a vertical line snaps to the closest column of the chart, a dot marks each
 * series on it and a box lists their values. Two kinds of chart are supported: the ones whose x axis is a number of days
 * ([wrap]) and the ones whose x axis lists categories, such as months or years ([wrapCategories]). Markers are drawn in
 * the same layer, positioned from the chart axes.
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
     * What the crosshair shows for the column under the mouse: the [header], where the column is on the x axis, a [Row]
     * for every series that has a value there and the [notes] of that column.
     */
    private class Snapshot(
        val header: String,
        val axisX: Double,
        val rows: List<Row>,
        val notes: List<String>,
    )

    /**
     * A series on a column. The swatch and the dot are painted by [styleClass] when there is one, or else by [paint].
     */
    private class Row(
        val label: String,
        val styleClass: String?,
        val paint: Paint?,
        val value: Double,
        val text: String,
    )

    /**
     * Wraps [chart], whose x axis is a number of days, with the interactive layer. [chartStyleClass] goes on the wrapper
     * so the chart stylesheet reaches the layer too. [notes] are lines added to the box on the day they fall on, such as
     * the trades of that day.
     */
    fun wrap(
        chart: LineChart<Number, Number>,
        chartStyleClass: String,
        series: List<CrosshairSeries>,
        markers: List<ChartMarker> = emptyList(),
        notes: List<Pair<LocalDate, String>> = emptyList(),
    ): StackPane {
        val days =
            series
                .maxByOrNull { it.points.size }
                ?.points
                ?.map { it.first }
                .orEmpty()

        val lookup = { axisX: Double ->
            CrosshairMath
                .nearestDay(days, chart.xAxis.getValueForDisplay(axisX).toDouble())
                ?.let { day ->
                    Snapshot(
                        header = day.format(DATE_FORMAT),
                        axisX = chart.xAxis.getDisplayPosition(day.toEpochDay()),
                        rows =
                            series.mapNotNull { item ->
                                CrosshairMath.valueOn(item.points, day)?.let {
                                    Row(item.label, item.styleClass, null, it, item.format(it))
                                }
                            },
                        notes = CrosshairMath.notesFor(notes, CrosshairMath.previousDay(days, day), day),
                    )
                }
        }

        val overlay =
            Overlay(chart, markers) { date -> chart.xAxis.getDisplayPosition(date.toEpochDay()) }

        return install(chart, chartStyleClass, overlay, series.size, lookup)
    }

    /**
     * Wraps [chart], whose x axis lists categories such as months or years, with the interactive layer. The series, the
     * categories and the values are read from the chart each time the mouse moves, so the chart can change its data
     * after it is wrapped. The dot and the swatch of a series take the color of its line. [header] turns the category
     * under the mouse into the first line of the box, and [format] writes a value.
     */
    fun wrapCategories(
        chart: LineChart<String, Number>,
        chartStyleClass: String,
        format: (Double) -> String,
        header: (String) -> String = { it },
    ): StackPane {
        val lookup = { axisX: Double ->
            val categories =
                chart.data
                    .maxByOrNull { it.data.size }
                    ?.data
                    ?.map { it.xValue }
                    .orEmpty()
            val positions = categories.map { chart.xAxis.getDisplayPosition(it) }

            CrosshairMath.nearestIndex(positions, axisX)?.let { index ->
                val category = categories[index]
                Snapshot(
                    header = header(category),
                    axisX = positions[index],
                    rows =
                        chart.data.mapNotNull { series ->
                            series.data
                                .firstOrNull { it.xValue == category }
                                ?.yValue
                                ?.toDouble()
                                ?.let {
                                    Row(series.name.orEmpty(), null, (series.node as? Shape)?.stroke, it, format(it))
                                }
                        },
                    notes = emptyList(),
                )
            }
        }

        return install(chart, chartStyleClass, Overlay(chart, emptyList()) { 0.0 }, 0, lookup)
    }

    /**
     * Puts [overlay] over [chart] and makes it follow the mouse with what [lookup] finds under it.
     */
    private fun install(
        chart: XYChart<*, Number>,
        chartStyleClass: String,
        overlay: Overlay,
        expectedRows: Int,
        lookup: (Double) -> Snapshot?,
    ): StackPane {
        val root = StackPane(chart, overlay)
        root.styleClass.add(chartStyleClass)
        root.stylesheets.addAll(chart.stylesheets)
        root.stylesheets.add(
            ChartCrosshair::class.java.getResource(STYLESHEET)?.toExternalForm()
                ?: error("Crosshair stylesheet not found"),
        )

        val line =
            Line().apply {
                styleClass.add("crosshair-line")
                isVisible = false
                isMouseTransparent = true
            }
        val box =
            VBox(BOX_SPACING).apply {
                styleClass.add("crosshair-box")
                isVisible = false
                isMouseTransparent = true
            }
        val dots = mutableListOf<Circle>()

        overlay.children.addAll(line, box)

        fun dot(index: Int): Circle {
            while (dots.size <= index) {
                dots.add(
                    Circle(DOT_RADIUS).apply {
                        styleClass.add("crosshair-dot")
                        isVisible = false
                        isMouseTransparent = true
                    },
                )
                overlay.children.add(dots.last())
            }
            return dots[index]
        }

        repeat(expectedRows) { dot(it) }

        fun hide() {
            line.isVisible = false
            box.isVisible = false
            dots.forEach { it.isVisible = false }
        }

        fun update(event: MouseEvent) {
            val plot = chart.lookup(PLOT_SELECTOR) as? Region ?: return hide()

            val plotBounds = root.sceneToLocal(plot.localToScene(plot.boundsInLocal))
            val mouse = root.sceneToLocal(event.sceneX, event.sceneY)
            if (mouse.x < plotBounds.minX ||
                mouse.x > plotBounds.maxX ||
                mouse.y < plotBounds.minY ||
                mouse.y > plotBounds.maxY
            ) {
                return hide()
            }

            val snapshot = lookup(chart.xAxis.sceneToLocal(event.sceneX, event.sceneY).x) ?: return hide()
            val lineX = root.sceneToLocal(chart.xAxis.localToScene(snapshot.axisX, 0.0)).x

            line.startX = lineX
            line.endX = lineX
            line.startY = plotBounds.minY
            line.endY = plotBounds.maxY
            line.isVisible = true

            box.children.setAll(Label(snapshot.header).apply { styleClass.add("crosshair-header") })
            dots.forEach { it.isVisible = false }
            snapshot.rows.forEachIndexed { index, row ->
                val y = root.sceneToLocal(chart.yAxis.localToScene(0.0, chart.yAxis.getDisplayPosition(row.value))).y
                dot(index).apply {
                    styleClass.setAll("crosshair-dot")
                    row.styleClass?.let { styleClass.add(it) }
                    row.paint?.let { fill = it }
                    centerX = lineX
                    centerY = y
                    isVisible = true
                }
                box.children.add(row(row))
            }
            snapshot.notes.forEach {
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

    private fun row(row: Row): Node =
        HBox(
            ROW_SPACING,
            Label("●").apply {
                row.styleClass?.let { styleClass.add(it) }
                row.paint?.let { textFill = it }
            },
            Label(row.label).apply { styleClass.add("crosshair-label") },
            Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
            Label(row.text).apply { styleClass.add("crosshair-value") },
        )

    /**
     * Pane over the chart where the markers are drawn. It is laid out after the chart, so the axes already have their
     * final size when it places them; it lets the mouse through everywhere except on the markers. [xOf] gives the
     * position of a date on the x axis.
     */
    private class Overlay(
        private val chart: XYChart<*, Number>,
        markers: List<ChartMarker>,
        private val xOf: (LocalDate) -> Double,
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
                val x = sceneToLocal(chart.xAxis.localToScene(xOf(marker.date), 0.0)).x
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
