/*
 * Filename: CalendarController.java
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import lombok.NoArgsConstructor;
import org.moinex.entities.CalendarEvent;
import org.moinex.services.CalendarService;
import org.moinex.ui.dialog.AddCalendarEventController;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the calendar view
 */
@Controller
@NoArgsConstructor
public class CalendarController
{
    @FXML
    private Label currentMonth;

    @FXML
    private GridPane calendar;

    private ConfigurableApplicationContext springContext;

    private LocalDate dateFocus;

    private LocalDate today;

    private List<CalendarEvent> calendarEvents;

    private CalendarService calendarService;

    /**
     * Constructor for CalendarController
     * @param calendarService The service for the calendar
     */
    @Autowired
    public CalendarController(CalendarService calendarService, ConfigurableApplicationContext springContext)
    {
        this.calendarService = calendarService;
        this.springContext = springContext;
    }

    @FXML
    public void initialize()
    {
        dateFocus = LocalDate.now();
        today     = LocalDate.now();

        loadCalendarEventsFromDatabase();

        drawCalendar();
    }

    @FXML
    private void handleBackOneMonth()
    {
        dateFocus = dateFocus.minusMonths(1);
        calendar.getChildren().clear();
        drawCalendar();
    }

    @FXML
    private void handleForwardOneMonth()
    {
        dateFocus = dateFocus.plusMonths(1);
        calendar.getChildren().clear();
        drawCalendar();
    }

    @FXML
    private void handleAddEvent()
    {
        WindowUtils.openModalWindow(Constants.ADD_CALENDAR_EVENT_FXML,
                                    "Add Calendar Event",
                                    springContext,
                                    (AddCalendarEventController controller)
                                        -> {},
                                    List.of(this::drawCalendar));
    }

    /**
     * Load the calendar events from the database
     */
    private void loadCalendarEventsFromDatabase()
    {
        calendarEvents = calendarService.getAllEvents();
    }

    /**
     * Draw the calendar grid
     */
    private void drawCalendar()
    {
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");
        currentMonth.setText(dateFocus.format(formatter));

        Integer monthMaxDate = dateFocus.getMonth().maxLength();

        if (!dateFocus.isLeapYear() && dateFocus.getMonth() == Month.FEBRUARY)
        {
            monthMaxDate = Constants.NON_LEAP_YEAR_FEBRUARY_DAYS;
        }

        Integer dateOffset =
            LocalDate.of(dateFocus.getYear(), dateFocus.getMonthValue(), 1)
                .getDayOfWeek()
                .getValue();

        // Adjust the date offset to start from Sunday at the first line of the calendar
        if (dateOffset.equals(Constants.WEEK_DAYS))
        {
            dateOffset = 0;
        }

        Integer currentDate    = 1;
        int totalGridCells = dateOffset + monthMaxDate;
        int totalRows =
            (int)Math.ceil(totalGridCells / Constants.WEEK_DAYS.doubleValue());

        Map<Integer, List<CalendarEvent>> calendarEventMap =
            getCalendarEventsMonth(dateFocus);

        calendar.getChildren().clear();

        Double calendarWidth  = calendar.getPrefWidth();
        double calendarHeight = calendar.getPrefHeight();

        // Create the weekday labels
        for (int j = 0; j < Constants.WEEKDAY_ABBREVIATIONS.length; j++)
        {
            Text dayLabel = new Text(Constants.WEEKDAY_ABBREVIATIONS[j]);
            dayLabel.setFont(Constants.CALENDAR_WEEKDAY_FONT_CONFIG);

            StackPane dayContainer = new StackPane(dayLabel);
            dayContainer.setPrefWidth(calendarWidth / Constants.WEEK_DAYS);
            dayContainer.setPrefHeight(calendarHeight / (totalRows + 1));
            GridPane.setHalignment(dayLabel, HPos.CENTER);
            GridPane.setValignment(dayLabel, VPos.CENTER);
            calendar.add(dayContainer, j, 0);
        }

        // Create the calendar cells for each day of the month
        for (int i = 0; i < totalRows; i++)
        {
            for (int j = 0; j < Constants.WEEK_DAYS; j++)
            {
                VBox cell = new VBox();
                cell.setMinSize(calendarWidth / Constants.WEEK_DAYS,
                                calendarHeight / (totalRows + 1));

                cell.setAlignment(Pos.TOP_CENTER);

                // Define the border style for the cell
                Double top    = (i == 0) ? Constants.CALENDAR_CELL_EXTERNAL_BORDER_WIDTH
                                         : Constants.CALENDAR_CELL_BORDER_WIDTH;
                Double bottom = (i == totalRows - 1)
                                    ? Constants.CALENDAR_CELL_EXTERNAL_BORDER_WIDTH
                                    : Constants.CALENDAR_CELL_BORDER_WIDTH;
                Double left   = (j == 0) ? Constants.CALENDAR_CELL_EXTERNAL_BORDER_WIDTH
                                         : Constants.CALENDAR_CELL_BORDER_WIDTH;
                Double right  = (j == Constants.WEEK_DAYS - 1)
                                    ? Constants.CALENDAR_CELL_EXTERNAL_BORDER_WIDTH
                                    : Constants.CALENDAR_CELL_BORDER_WIDTH;

                String borderStyle = String.format(
                    "-fx-border-color: black; "
                        + "-fx-border-width: %.1fpx %.1fpx %.1fpx %.1fpx;",
                    top,
                    right,
                    bottom,
                    left);

                cell.setStyle(borderStyle);

                if ((i == 0 && j >= dateOffset) ||
                    (i > 0 && currentDate <= monthMaxDate))
                {
                    Text dateText = new Text(String.valueOf(currentDate));
                    dateText.setFont(Constants.CALENDAR_DATE_FONT_CONFIG);

                    // Create the event indicators for the cell
                    HBox eventIndicators = new HBox(3);
                    eventIndicators.setAlignment(Pos.CENTER);

                    List<CalendarEvent> calendarEvents =
                        calendarEventMap.get(currentDate);
                    if (calendarEvents != null)
                    {
                        for (CalendarEvent event : calendarEvents)
                        {
                            Circle indicator = new Circle(
                                4,
                                Color.web(event.getEventType().getColorHex()));
                            eventIndicators.getChildren().add(indicator);
                        }
                    }

                    // Make the cell clickable to open the event details
                    // TODO: Implement the event details view
                    final Integer selectedDate = currentDate;
                    cell.setOnMouseClicked(
                        event -> System.out.println("Selected date: " + selectedDate));

                    // Align the event indicators to the bottom of the cell
                    Region spacer = new Region();
                    VBox.setVgrow(spacer, Priority.ALWAYS);
                    eventIndicators.setPadding(new Insets(0, 0, 5, 0));

                    cell.getChildren().addAll(dateText, spacer, eventIndicators);

                    // Highlight the current date
                    if (today.getYear() == dateFocus.getYear() &&
                        today.getMonth() == dateFocus.getMonth() &&
                        today.getDayOfMonth() == currentDate)
                    {
                        cell.setStyle("-fx-border-color: blue; -fx-border-width: "
                                      + "2px; -fx-background-color: lightblue;");
                    }

                    currentDate++;
                }

                calendar.add(cell, j, i + 1);
            }
        }
    }

    /**
     * Create a map of calendar events by date
     * @param calendarEvents The list of calendar events
     * @return A map of calendar events by date
     */
    private Map<Integer, List<CalendarEvent>>
    createCalendarMap(List<CalendarEvent> calendarEvents)
    {
        Map<Integer, List<CalendarEvent>> calendarEventMap = new HashMap<>();

        for (CalendarEvent event : calendarEvents)
        {
            Integer eventDate = event.getDate().getDayOfMonth();
            if (!calendarEventMap.containsKey(eventDate))
            {
                calendarEventMap.put(eventDate, List.of(event));
            }
            else
            {
                List<CalendarEvent> oldListByDate = calendarEventMap.get(eventDate);

                List<CalendarEvent> newList = new ArrayList<>(oldListByDate);
                newList.add(event);
                calendarEventMap.put(eventDate, newList);
            }
        }

        return calendarEventMap;
    }

    /**
     * Get the calendar events for the month
     * @param dateFocus The date to focus on
     * @return A map of calendar events by date
     */
    private Map<Integer, List<CalendarEvent>>
    getCalendarEventsMonth(LocalDate dateFocus)
    {
        List<CalendarEvent> calendarEventsMonth = new ArrayList<>();

        for (CalendarEvent event : calendarEvents)
        {
            if (event.getDate().getMonth() == dateFocus.getMonth() &&
                event.getDate().getYear() == dateFocus.getYear())
            {
                calendarEventsMonth.add(event);
            }
        }

        return createCalendarMap(calendarEventsMonth);
    }
}
