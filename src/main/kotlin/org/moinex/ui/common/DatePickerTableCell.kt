/*
 * Filename: DatePickerTableCell.kt
 * Created on: September 21, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import javafx.scene.control.ContentDisplay
import javafx.scene.control.DatePicker
import javafx.scene.control.TableCell
import java.time.LocalDate

/** Edits a [LocalDate] cell with a real [DatePicker] instead of free text. */
class DatePickerTableCell<S> : TableCell<S, LocalDate>() {
    private val datePicker =
        DatePicker().apply {
            setOnAction { commitEdit(value) }
            focusedProperty().addListener { _, _, focused -> if (!focused && isEditing) commitEdit(value) }
        }

    override fun startEdit() {
        super.startEdit()
        datePicker.value = item
        contentDisplay = ContentDisplay.GRAPHIC_ONLY
        graphic = datePicker
    }

    override fun cancelEdit() {
        super.cancelEdit()
        contentDisplay = ContentDisplay.TEXT_ONLY
        text = item?.toString()
        graphic = null
    }

    override fun updateItem(
        item: LocalDate?,
        empty: Boolean,
    ) {
        super.updateItem(item, empty)
        when {
            empty -> {
                text = null
                graphic = null
            }
            isEditing -> {
                datePicker.value = item
                contentDisplay = ContentDisplay.GRAPHIC_ONLY
                graphic = datePicker
            }
            else -> {
                contentDisplay = ContentDisplay.TEXT_ONLY
                text = item?.toString()
                graphic = null
            }
        }
    }
}
