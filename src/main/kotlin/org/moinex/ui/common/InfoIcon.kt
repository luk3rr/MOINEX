/*
 * Filename: InfoIcon.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import javafx.scene.Cursor
import javafx.scene.control.Label
import org.kordamp.ikonli.javafx.FontIcon
import org.moinex.common.ui.Icons
import org.moinex.common.util.UIUtils

/**
 * A discreet "i" that shows [text] as a tooltip when the mouse rests on it, so that explanations do not pop up all over
 * a card.
 */
object InfoIcon {
    private const val STYLE_CLASS = "info-icon"
    private const val SIZE_CLASS = "icon-size-16"

    fun create(text: String): Label =
        Label().apply {
            styleClass.add(STYLE_CLASS)
            graphic =
                FontIcon(Icons.DETAILS).apply {
                    styleClass.addAll(Icons.ICON_STYLE_CLASS, SIZE_CLASS)
                }
            cursor = Cursor.HAND
            UIUtils.addTooltipToNode(this, text)
        }
}
