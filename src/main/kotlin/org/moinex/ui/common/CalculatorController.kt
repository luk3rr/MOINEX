/*
 * Filename: CalculatorController.kt (original filename: CalculatorController.java)
 * Created on: January  4, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.stage.Stage
import net.objecthunter.exp4j.ExpressionBuilder
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.service.CalculatorService
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Controller

@Controller
class CalculatorController(
    private val calculatorService: CalculatorService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var mainDisplay: TextField

    @FXML
    private lateinit var expressionDisplay: TextField

    private var expression = StringBuilder()

    @FXML
    private fun initialize() {
        calculatorService.result = null
        expression.clear()
        expressionDisplay.text = ""
        mainDisplay.text = "0"
    }

    @FXML
    private fun addAction() = addOperator("+")

    @FXML
    private fun minusAction() = addOperator("-")

    @FXML
    private fun divideAction() = addOperator("/")

    @FXML
    private fun multiplicationAction() = addOperator("*")

    @FXML
    private fun calculate() {
        if (expression.isEmpty()) return

        runCatching {
            val result = evaluateExpression(expression.toString())
            val formattedExpression = expression.toString().replace(Regex("([+\\-*/])"), " $1 ") + " = " + result

            expressionDisplay.text = formattedExpression
            mainDisplay.text = result.toString()
            expression = StringBuilder(result.toString())
            calculatorService.result = result.toString()
        }.onFailure { e ->
            expressionDisplay.text = "Error"
            mainDisplay.text = ""
            expression.clear()
            calculatorService.result = null

            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.COMMON_CALCULATOR_DIALOG_ERROR_TITLE),
                e.message ?: "Unknown error",
            )
        }
    }

    @FXML
    private fun clearTextField() {
        expression.clear()
        expressionDisplay.text = ""
        mainDisplay.text = ""
    }

    @FXML
    private fun finish() {
        calculate()
        (mainDisplay.scene.window as Stage).close()
    }

    @FXML
    private fun buttonDotClicked() {
        val currentToken = currentToken()
        if (!expression.toString().endsWith(".") && !currentToken.contains(".")) {
            if (expression.isEmpty() || isOperator(expression.last())) {
                expression.append("0.")
            } else {
                expression.append(".")
            }
            updateExpressionDisplay()
        }
    }

    @FXML
    private fun button0Clicked() = addNumber("0")

    @FXML
    private fun button1Clicked() = addNumber("1")

    @FXML
    private fun button2Clicked() = addNumber("2")

    @FXML
    private fun button3Clicked() = addNumber("3")

    @FXML
    private fun button4Clicked() = addNumber("4")

    @FXML
    private fun button5Clicked() = addNumber("5")

    @FXML
    private fun button6Clicked() = addNumber("6")

    @FXML
    private fun button7Clicked() = addNumber("7")

    @FXML
    private fun button8Clicked() = addNumber("8")

    @FXML
    private fun button9Clicked() = addNumber("9")

    fun updateExpressionDisplay() {
        expressionDisplay.text = expression.toString().replace(Regex("([+\\-*/])"), " $1 ")
    }

    fun updateMainDisplay() {
        mainDisplay.text = currentToken()
    }

    fun addNumber(number: String) {
        expression.append(number)
        updateExpressionDisplay()
        updateMainDisplay()
    }

    fun addOperator(operator: String) {
        if (expression.isNotEmpty() && !isOperator(expression.last())) {
            expression.append(operator)
            updateExpressionDisplay()
        }
    }

    private fun isOperator(c: Char): Boolean = c in setOf('+', '-', '*', '/')

    private fun currentToken(): String {
        val lastOperatorIndex =
            maxOf(
                expression.lastIndexOf("+"),
                expression.lastIndexOf("-"),
                expression.lastIndexOf("*"),
                expression.lastIndexOf("/"),
            )

        val token = expression.substring(lastOperatorIndex + 1)

        return when {
            token.startsWith("-") && lastOperatorIndex == -1 -> token
            token.startsWith("-") && expression[lastOperatorIndex] != '-' && lastOperatorIndex != -1 -> token
            else -> token.ifEmpty { "" }
        }
    }

    private fun evaluateExpression(expression: String): Double = ExpressionBuilder(expression).build().evaluate()
}
