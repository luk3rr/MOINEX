/*
 * Filename: CalendarController.kt (original filename: CalendarController.java)
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import javafx.fxml.FXML
import javafx.geometry.HPos
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.text.Text
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.CalendarEvent
import org.moinex.service.CalendarService
import org.moinex.service.PreferencesService
import org.moinex.ui.dialog.AddCalendarEventController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.time.LocalDate
import java.time.Month
import kotlin.math.ceil

@Controller
class CalendarController(
    private val calendarService: CalendarService,
    private val preferencesService: PreferencesService,
    private val springContext: ConfigurableApplicationContext,
) {
    @FXML
    private lateinit var currentMonth: Label

    @FXML
    private lateinit var calendar: GridPane

    private var dateFocus = LocalDate.now()
    private val today = LocalDate.now()
    private var calendarEvents = emptyList<CalendarEvent>()

    @FXML
    private fun initialize() {
        dateFocus = LocalDate.now()
        loadCalendarEventsFromDatabase()
        drawCalendar()
    }

    @FXML
    private fun handleBackOneMonth() {
        dateFocus = dateFocus.minusMonths(1)
        calendar.children.clear()
        drawCalendar()
    }

    @FXML
    private fun handleForwardOneMonth() {
        dateFocus = dateFocus.plusMonths(1)
        calendar.children.clear()
        drawCalendar()
    }

    @FXML
    private fun handleAddEvent() {
        WindowUtils.openModalWindow(
            Files.ADD_CALENDAR_EVENT_FXML,
            preferencesService.translate(TranslationKeys.COMMON_CALENDAR_MODAL_ADD_EVENT),
            springContext,
            { _: AddCalendarEventController -> },
            listOf(Runnable { drawCalendar() }),
        )
    }

    private fun loadCalendarEventsFromDatabase() {
        calendarEvents = calendarService.getAllEvents()
    }

    private fun drawCalendar() {
        val formatter = UIUtils.getShortMonthYearFormatter(preferencesService.locale)
        currentMonth.text = dateFocus.format(formatter)

        val monthMaxDate =
            if (!dateFocus.isLeapYear && dateFocus.month == Month.FEBRUARY) {
                Constants.NON_LEAP_YEAR_FEBRUARY_DAYS
            } else {
                dateFocus.month.maxLength()
            }

        var dateOffset =
            LocalDate
                .of(dateFocus.year, dateFocus.monthValue, 1)
                .dayOfWeek
                .value

        if (dateOffset == Constants.WEEK_DAYS) {
            dateOffset = 0
        }

        val totalGridCells = dateOffset + monthMaxDate
        val totalRows = ceil(totalGridCells.toDouble() / Constants.WEEK_DAYS).toInt()

        val calendarEventMap = getCalendarEventsMonth(dateFocus)

        calendar.children.clear()

        val calendarWidth = calendar.prefWidth
        val calendarHeight = calendar.prefHeight

        createWeekdayLabels(calendarWidth, calendarHeight, totalRows)
        createCalendarCells(
            calendarWidth,
            calendarHeight,
            totalRows,
            dateOffset,
            monthMaxDate,
            calendarEventMap,
        )
    }

    private fun createWeekdayLabels(
        calendarWidth: Double,
        calendarHeight: Double,
        totalRows: Int,
    ) {
        UIUtils.getWeekdayAbbreviations().forEachIndexed { j, weekday ->
            val dayLabel =
                Text(weekday).apply {
                    font = Constants.CALENDAR_WEEKDAY_FONT_CONFIG
                }

            val dayContainer =
                StackPane(dayLabel).apply {
                    prefWidth = calendarWidth / Constants.WEEK_DAYS
                    prefHeight = calendarHeight / (totalRows + 1)
                }

            GridPane.setHalignment(dayLabel, HPos.CENTER)
            GridPane.setValignment(dayLabel, VPos.CENTER)
            calendar.add(dayContainer, j, 0)
        }
    }

    private fun createCalendarCells(
        calendarWidth: Double,
        calendarHeight: Double,
        totalRows: Int,
        dateOffset: Int,
        monthMaxDate: Int,
        calendarEventMap: Map<Int, List<CalendarEvent>>,
    ) {
        var currentDate = 1

        repeat(totalRows) { i ->
            repeat(Constants.WEEK_DAYS) { j ->
                val cell = createCell(calendarWidth, calendarHeight, totalRows, i, j)

                if ((i == 0 && j >= dateOffset) || (i > 0 && currentDate <= monthMaxDate)) {
                    populateCell(cell, currentDate, calendarEventMap[currentDate])
                    currentDate++
                }

                calendar.add(cell, j, i + 1)
            }
        }
    }

    private fun createCell(
        calendarWidth: Double,
        calendarHeight: Double,
        totalRows: Int,
        row: Int,
        col: Int,
    ): VBox =
        VBox().apply {
            minWidth = calendarWidth / Constants.WEEK_DAYS
            minHeight = calendarHeight / (totalRows + 1)
            alignment = Pos.TOP_CENTER

            val borderWidths = calculateBorderWidths(row, col, totalRows)
            style =
                "-fx-border-color: black; " +
                "-fx-border-width: %.1fpx %.1fpx %.1fpx %.1fpx;".format(
                    borderWidths.top,
                    borderWidths.right,
                    borderWidths.bottom,
                    borderWidths.left,
                )
        }

    private fun calculateBorderWidths(
        row: Int,
        col: Int,
        totalRows: Int,
    ): BorderWidths =
        BorderWidths(
            top =
                if (row == 0) {
                    Constants.CALENDAR_CELL_EXTERNAL_BORDER_WIDTH
                } else {
                    Constants.CALENDAR_CELL_BORDER_WIDTH
                },
            bottom =
                if (row == totalRows - 1) {
                    Constants.CALENDAR_CELL_EXTERNAL_BORDER_WIDTH
                } else {
                    Constants.CALENDAR_CELL_BORDER_WIDTH
                },
            left =
                if (col == 0) {
                    Constants.CALENDAR_CELL_EXTERNAL_BORDER_WIDTH
                } else {
                    Constants.CALENDAR_CELL_BORDER_WIDTH
                },
            right =
                if (col == Constants.WEEK_DAYS - 1) {
                    Constants.CALENDAR_CELL_EXTERNAL_BORDER_WIDTH
                } else {
                    Constants.CALENDAR_CELL_BORDER_WIDTH
                },
        )

    private fun populateCell(
        cell: VBox,
        date: Int,
        events: List<CalendarEvent>?,
    ) {
        val dateText =
            Text(date.toString()).apply {
                font = Constants.CALENDAR_DATE_FONT_CONFIG
            }

        val eventIndicators =
            HBox(3.0).apply {
                alignment = Pos.CENTER
                events?.forEach { event ->
                    children.add(Circle(4.0, Color.web(event.eventType.colorHex)))
                }
            }

        val spacer =
            Region().apply {
                VBox.setVgrow(this, Priority.ALWAYS)
            }

        eventIndicators.padding = Insets(0.0, 0.0, 5.0, 0.0)
        cell.children.addAll(dateText, spacer, eventIndicators)

        if (isToday(date)) {
            cell.style = "-fx-border-color: blue; -fx-border-width: 2px; -fx-background-color: lightblue;"
        }
    }

    private fun isToday(date: Int): Boolean =
        today.year == dateFocus.year &&
            today.month == dateFocus.month &&
            today.dayOfMonth == date

    private fun createCalendarMap(calendarEvents: List<CalendarEvent>): Map<Int, List<CalendarEvent>> =
        calendarEvents.groupBy { it.date.dayOfMonth }

    private fun getCalendarEventsMonth(dateFocus: LocalDate): Map<Int, List<CalendarEvent>> {
        val calendarEventsMonth =
            calendarEvents.filter {
                it.date.month == dateFocus.month && it.date.year == dateFocus.year
            }

        return createCalendarMap(calendarEventsMonth)
    }

    private data class BorderWidths(
        val top: Double,
        val right: Double,
        val bottom: Double,
        val left: Double,
    )
}
