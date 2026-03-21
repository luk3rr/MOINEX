/*
 * Filename: AddCalendarEventController.kt (original filename: AddCalendarEventController.java)
 * Created on: March  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog

import javafx.fxml.FXML
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.CalendarEvent
import org.moinex.model.enums.CalendarEventType
import org.moinex.service.CalendarService
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Controller

@Controller
class AddCalendarEventController(
    private val calendarService: CalendarService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var titleField: TextField

    @FXML
    private lateinit var typeComboBox: ComboBox<CalendarEventType>

    @FXML
    private lateinit var datePicker: DatePicker

    @FXML
    private lateinit var descriptionTextArea: TextArea

    @FXML
    private fun initialize() {
        configureComboBoxes()
        UIUtils.setDatePickerFormat(datePicker)
        populateComboBoxes()
    }

    @FXML
    private fun handleCancel() {
        (titleField.scene.window as Stage).close()
    }

    @FXML
    private fun handleSave() {
        val eventTitle = titleField.text.trim()
        val eventType = typeComboBox.value
        val eventDate = datePicker.value
        val description = descriptionTextArea.text

        if (eventTitle.isEmpty() || eventDate == null || eventType == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            calendarService.createEvent(
                CalendarEvent(null, eventDate, eventTitle, description, eventType),
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.CALENDAR_DIALOG_EVENT_CREATED_TITLE),
                preferencesService.translate(TranslationKeys.CALENDAR_DIALOG_EVENT_CREATED_MESSAGE),
            )

            (titleField.scene.window as Stage).close()
        }.onFailure { e ->
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.CALENDAR_DIALOG_ERROR_CREATING_EVENT_TITLE),
                e.message ?: "Unknown error",
            )
        }
    }

    private fun populateComboBoxes() {
        typeComboBox.items.setAll(*CalendarEventType.entries.toTypedArray())
    }

    private fun configureComboBoxes() {
        UIUtils.configureComboBox(typeComboBox, UIUtils::translateCalendarEventType)
    }
}
