/*
 * Filename: AddCalendarEventController.java
 * Created on: March  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.service.CalendarService;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.CalendarEventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Calendar Event dialog
 */
@Controller
@NoArgsConstructor
public class AddCalendarEventController
{
    @FXML
    private TextField titleField;

    @FXML
    private ComboBox<CalendarEventType> typeComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextArea descriptionTextArea;

    private CalendarService calendarService;

    /**
     * Constructor
     * @param calendarService The CalendarService instance
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddCalendarEventController(CalendarService calendarService)
    {
        this.calendarService = calendarService;
    }

    @FXML
    private void initialize()
    {
        configureComboBoxes();
        UIUtils.setDatePickerFormat(datePicker);
        populateComboBoxes();
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)titleField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        String eventTitle = titleField.getText();
        eventTitle = eventTitle.strip(); // Remove leading and trailing whitespaces

        CalendarEventType eventType   = typeComboBox.getValue();
        LocalDate         eventDate   = datePicker.getValue();
        String            description = descriptionTextArea.getText();

        if (eventTitle.isEmpty() || eventDate == null || eventType == null)
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");

            return;
        }

        try
        {
            calendarService.addEvent(eventTitle,
                                     description,
                                     eventDate.atStartOfDay(),
                                     eventType);

            WindowUtils.showSuccessDialog("Event created",
                                          "The event was successfully created.");

            Stage stage = (Stage)titleField.getScene().getWindow();
            stage.close();
        }
        catch (IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog("Error creating event", e.getMessage());
        }
    }

    private void populateComboBoxes()
    {
        typeComboBox.getItems().setAll(CalendarEventType.values());
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(typeComboBox, CalendarEventType::getDescription);
    }
}
