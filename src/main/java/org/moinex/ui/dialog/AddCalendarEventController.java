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
import org.moinex.model.enums.CalendarEventType;
import org.moinex.service.CalendarService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Calendar Event dialog
 */
@Controller
@NoArgsConstructor
public class AddCalendarEventController {
    @FXML private TextField titleField;

    @FXML private ComboBox<CalendarEventType> typeComboBox;

    @FXML private DatePicker datePicker;

    @FXML private TextArea descriptionTextArea;

    private CalendarService calendarService;

    private I18nService i18nService;

    /**
     * Constructor
     * @param calendarService The CalendarService instance
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddCalendarEventController(CalendarService calendarService, I18nService i18nService) {
        this.calendarService = calendarService;
        this.i18nService = i18nService;
    }

    @FXML
    private void initialize() {
        configureComboBoxes();
        UIUtils.setDatePickerFormat(datePicker, i18nService);
        populateComboBoxes();
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave() {
        String eventTitle = titleField.getText();
        eventTitle = eventTitle.strip(); // Remove leading and trailing whitespaces

        CalendarEventType eventType = typeComboBox.getValue();
        LocalDate eventDate = datePicker.getValue();
        String description = descriptionTextArea.getText();

        if (eventTitle.isEmpty() || eventDate == null || eventType == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE));

            return;
        }

        try {
            calendarService.addEvent(eventTitle, description, eventDate.atStartOfDay(), eventType);

            WindowUtils.showSuccessDialog(
                    i18nService.tr(Constants.TranslationKeys.CALENDAR_DIALOG_EVENT_CREATED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.CALENDAR_DIALOG_EVENT_CREATED_MESSAGE));

            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.close();
        } catch (IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.CALENDAR_DIALOG_ERROR_CREATING_EVENT_TITLE),
                    e.getMessage());
        }
    }

    private void populateComboBoxes() {
        typeComboBox.getItems().setAll(CalendarEventType.values());
    }

    private void configureComboBoxes() {
        UIUtils.configureComboBox(
                typeComboBox, t -> UIUtils.translateCalendarEventType(t, i18nService));
    }
}
