/*
 * Filename: CalendarController.java
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.moinex.entities.CalendarEvent;
import org.moinex.services.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the calendar view
 */
@Controller
public class CalendarController
{
    @FXML
    private Text year;

    @FXML
    private Text month;

    @FXML
    private GridPane calendar;

    private LocalDate dateFocus;

    private LocalDate today;

    private List<CalendarEvent> calendarEvents;

    private CalendarService calendarService;

    public CalendarController() { }

    /**
     * Constructor for CalendarController
     * @param calendarService The service for the calendar
     */
    @Autowired
    public CalendarController(CalendarService calendarService)
    {
        this.calendarService = calendarService;
    }

    @FXML
    public void initialize()
    {
        dateFocus = LocalDate.now();
        today     = LocalDate.now();

        LoadCalendarEventsFromDatabase();

        DrawCalendar();
    }

    @FXML
    private void handleBackOneMonth()
    {
        dateFocus = dateFocus.minusMonths(1);
        calendar.getChildren().clear();
        DrawCalendar();
    }

    @FXML
    private void handleForwardOneMonth()
    {
        dateFocus = dateFocus.plusMonths(1);
        calendar.getChildren().clear();
        DrawCalendar();
    }

    @FXML
    private void handleAddEvent()
    { }

    private void LoadCalendarEventsFromDatabase()
    {
        calendarEvents = calendarService.GetAllEvents();
    }

    private void DrawCalendar()
    {
        year.setText(String.valueOf(dateFocus.getYear()));
        month.setText(String.valueOf(dateFocus.getMonth()));

        Integer monthMaxDate = dateFocus.getMonth().maxLength();

        if (!dateFocus.isLeapYear() && dateFocus.getMonth() == Month.FEBRUARY)
        {
            monthMaxDate = 28;
        }

        Integer dateOffset =
            LocalDate.of(dateFocus.getYear(), dateFocus.getMonthValue(), 1)
                .getDayOfWeek()
                .getValue();

        if (dateOffset == 7)
        {
            dateOffset = 0;
        }

        Integer currentDate    = 1;
        Integer totalGridCells = dateOffset + monthMaxDate;
        Integer totalRows      = (int)Math.ceil(totalGridCells / 7.0);

        Map<Integer, List<CalendarEvent>> calendarEventMap =
            GetCalendarEventsMonth(dateFocus);

        calendar.getChildren().clear();

        Double calendarWidth  = calendar.getPrefWidth();
        Double calendarHeight = calendar.getPrefHeight();

        String[] weekDays = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };

        for (Integer j = 0; j < 7; j++)
        {
            Text dayLabel = new Text(weekDays[j]);
            dayLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

            StackPane dayContainer = new StackPane(dayLabel);
            dayContainer.setPrefWidth(calendarWidth / 7);
            dayContainer.setPrefHeight(calendarHeight / (totalRows + 1));
            GridPane.setHalignment(dayLabel, HPos.CENTER);
            GridPane.setValignment(dayLabel, VPos.CENTER);
            calendar.add(dayContainer, j, 0);
        }

        for (Integer i = 0; i < totalRows; i++)
        {
            for (Integer j = 0; j < 7; j++)
            {
                VBox cell = new VBox();
                cell.setMinSize(calendarWidth / 7, calendarHeight / (totalRows + 1));
                cell.setAlignment(Pos.TOP_CENTER);

                // Define the border style for the cell
                Double borderWidth         = 0.5;
                Double externalBorderWidth = 2.0;

                Double top = (i == 0) ? externalBorderWidth : borderWidth;
                Double bottom =
                    (i == totalRows - 1) ? externalBorderWidth : borderWidth;
                Double left  = (j == 0) ? externalBorderWidth : borderWidth;
                Double right = (j == 6) ? externalBorderWidth : borderWidth;

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
                    dateText.setFont(Font.font("Arial", FontWeight.BOLD, 14));

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
                                Color.web(event.GetEventType().GetColorHex()));
                            eventIndicators.getChildren().add(indicator);
                        }
                    }

                    // Make the cell clickable to open the event details
                    // TODO: Implement the event details view
                    final Integer selectedDate = currentDate;
                    cell.setOnMouseClicked(event -> {
                        System.out.println("Selected date: " + selectedDate);
                    });

                    // Align the event indicators to the bottom of the cell
                    Region spacer = new Region();
                    VBox.setVgrow(spacer, Priority.ALWAYS);
                    eventIndicators.setPadding(new Insets(0, 0, 5, 0));

                    cell.getChildren().addAll(dateText, spacer, eventIndicators);

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

    private Map<Integer, List<CalendarEvent>>
    CreateCalendarMap(List<CalendarEvent> calendarEvents)
    {
        Map<Integer, List<CalendarEvent>> calendarEventMap = new HashMap<>();

        for (CalendarEvent event : calendarEvents)
        {
            Integer eventDate = event.GetDate().getDayOfMonth();
            if (!calendarEventMap.containsKey(eventDate))
            {
                calendarEventMap.put(eventDate, List.of(event));
            }
            else
            {
                List<CalendarEvent> OldListByDate = calendarEventMap.get(eventDate);

                List<CalendarEvent> newList = new ArrayList<>(OldListByDate);
                newList.add(event);
                calendarEventMap.put(eventDate, newList);
            }
        }

        return calendarEventMap;
    }

    private Map<Integer, List<CalendarEvent>>
    GetCalendarEventsMonth(LocalDate dateFocus)
    {
        List<CalendarEvent> calendarEventsMonth = new ArrayList<>();

        for (CalendarEvent event : calendarEvents)
        {
            if (event.GetDate().getMonth() == dateFocus.getMonth() &&
                event.GetDate().getYear() == dateFocus.getYear())
            {
                calendarEventsMonth.add(event);
            }
        }

        return CreateCalendarMap(calendarEventsMonth);
    }
}
