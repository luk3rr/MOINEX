/*
 * Filename: CalculatorService.kt (original filename: CalculatorService.java)
 * Created on: January  4, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/08/2026
 */

package org.moinex.service

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.TextField
import org.moinex.common.extension.toRounded
import org.moinex.common.util.WindowUtils
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class CalculatorService {
    var result: String? = null

    val history: ObservableList<CalculationEntry> = FXCollections.observableArrayList()

    fun addToHistory(
        expression: String,
        result: String,
    ) {
        history.add(0, CalculationEntry(expression, result))
        if (history.size > MAX_HISTORY_SIZE) history.removeAt(history.lastIndex)
    }

    fun updateComponentWithResult(component: TextField) {
        if (result != null) {
            try {
                val resultValue = BigDecimal(result)

                if (resultValue < BigDecimal.ZERO) {
                    WindowUtils.showInformationDialog(
                        "Invalid value",
                        "The value must be positive",
                    )
                    return
                }

                result = resultValue.toRounded().toString()

                component.text = result
            } catch (_: NumberFormatException) {
                WindowUtils.showErrorDialog("Invalid value", "The value must be a number")
            }
        }
    }
}

private const val MAX_HISTORY_SIZE = 50

data class CalculationEntry(
    val expression: String,
    val result: String,
) {
    override fun toString(): String = "${expression.replace(Regex("([+\\-*/])"), " $1 ")} = $result"
}
