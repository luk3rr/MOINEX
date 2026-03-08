/*
 * Filename: CalculatorService.kt (original filename: CalculatorService.java)
 * Created on: January  4, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/08/2026
 */

package org.moinex.service

import javafx.scene.control.TextField
import org.moinex.common.toRounded
import org.moinex.util.WindowUtils
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class CalculatorService {
    var result: String? = null

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
