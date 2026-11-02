/*
 * Filename: TitledValue.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox

/**
 * The "Title: value" pattern of the app: the title and its colon in bold and the value in regular text. [valueLabel]
 * is exposed so a caller can color the value.
 */
class TitledValue : HBox(SPACING) {
    val valueLabel = Label()
    private val titleLabel = Label().apply { styleClass.add(TITLE_CLASS) }

    var title: String
        get() = titleLabel.text.removeSuffix(":")
        set(value) {
            titleLabel.text = "$value:"
        }

    var value: String
        get() = valueLabel.text
        set(text) {
            valueLabel.text = text
        }

    init {
        alignment = Pos.CENTER_LEFT
        children.addAll(titleLabel, valueLabel)
    }

    companion object {
        private const val SPACING = 4.0
        private const val TITLE_CLASS = "titled-value-title"
    }
}
