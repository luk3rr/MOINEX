/*
 * Filename: CalculatorController.kt (original filename: CalculatorController.java)
 * Created on: January  4, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import javafx.fxml.FXML
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.stage.Stage
import net.objecthunter.exp4j.ExpressionBuilder
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.toRounded
import org.moinex.common.util.WindowUtils
import org.moinex.service.CalculationEntry
import org.moinex.service.CalculatorService
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Controller
import java.math.BigDecimal

private const val RESULT_SCALE = 8

@Controller
class CalculatorController(
    private val calculatorService: CalculatorService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var mainDisplay: TextField

    @FXML
    private lateinit var expressionDisplay: TextField

    @FXML
    private lateinit var historyList: ListView<CalculationEntry>

    private var expression = StringBuilder()

    private var justCalculated = false

    @FXML
    private fun initialize() {
        calculatorService.result = null
        expression.clear()
        expressionDisplay.text = ""
        mainDisplay.text = "0"

        historyList.items = calculatorService.history
        historyList.setOnMouseClicked {
            historyList.selectionModel.selectedItem?.let { restore(it) }
        }

        mainDisplay.sceneProperty().addListener { _, _, scene ->
            scene?.addEventFilter(KeyEvent.KEY_PRESSED, ::handleKeyPressed)
        }
    }

    private fun handleKeyPressed(event: KeyEvent) {
        if (event.isControlDown || event.isAltDown || event.isMetaDown) return

        when (event.code) {
            KeyCode.ENTER -> calculate()
            KeyCode.BACK_SPACE -> backspace()
            KeyCode.DELETE, KeyCode.ESCAPE -> clearTextField()
            else -> {
                val char = event.text.singleOrNull() ?: return
                when {
                    char.isDigit() -> addNumber(char.toString())
                    char in "+-*/" -> addOperator(char.toString())
                    char == '.' || char == ',' -> buttonDotClicked()
                    char == '(' -> openParenthesis()
                    char == ')' -> closeParenthesis()
                    char == '=' -> calculate()
                    else -> return
                }
            }
        }
        event.consume()
    }

    private fun restore(entry: CalculationEntry) {
        expression = StringBuilder(entry.expression)
        justCalculated = false
        updateExpressionDisplay()
        updateMainDisplay()
    }

    @FXML
    private fun backspace() {
        if (expression.isEmpty()) return

        justCalculated = false
        expression.deleteCharAt(expression.lastIndex)
        updateExpressionDisplay()
        mainDisplay.text = currentToken().ifEmpty { "0" }
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
            val missing = expression.count { it == '(' } - expression.count { it == ')' }
            if (missing > 0) expression.append(")".repeat(missing))

            val result = evaluateExpression(expression.toString())
            val resultText =
                BigDecimal(result)
                    .toRounded(RESULT_SCALE)
                    .stripTrailingZeros()
                    .toPlainString()
            val formattedExpression = formatExpression(expression.toString()) + " = " + resultText

            if (!justCalculated) calculatorService.addToHistory(expression.toString(), resultText)

            expressionDisplay.text = formattedExpression
            mainDisplay.text = resultText
            expression = StringBuilder(resultText)
            calculatorService.result = resultText
            justCalculated = true
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
        justCalculated = false
        expressionDisplay.text = ""
        mainDisplay.text = "0"
    }

    @FXML
    private fun finish() {
        calculate()
        (mainDisplay.scene.window as Stage).close()
    }

    @FXML
    private fun buttonDotClicked() {
        startNewIfCalculated()
        if (currentToken().contains(".")) return

        when {
            expression.isEmpty() || isOperator(expression.last()) || expression.last() == '(' -> expression.append("0.")
            expression.last() == ')' -> expression.append("*0.")
            else -> expression.append(".")
        }
        updateExpressionDisplay()
        updateMainDisplay()
    }

    @FXML
    private fun openParenthesis() {
        startNewIfCalculated()
        if (expression.isNotEmpty() && (expression.last().isDigit() || expression.last() in ".)")) {
            expression.append("*")
        }
        expression.append("(")
        updateExpressionDisplay()
        mainDisplay.text = "0"
    }

    @FXML
    private fun closeParenthesis() {
        justCalculated = false
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        if (openCount > closeCount &&
            expression.isNotEmpty() &&
            (expression.last().isDigit() || expression.last() == ')')
        ) {
            expression.append(")")
            updateExpressionDisplay()
        }
    }

    private fun startNewIfCalculated() {
        if (justCalculated) {
            expression.clear()
            justCalculated = false
        }
    }

    private fun formatExpression(raw: String): String = raw.replace(Regex("([+\\-*/])"), " $1 ")

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
        expressionDisplay.text = formatExpression(expression.toString())
    }

    fun updateMainDisplay() {
        mainDisplay.text = currentToken()
    }

    fun addNumber(number: String) {
        startNewIfCalculated()
        if (expression.isNotEmpty() && expression.last() == ')') expression.append("*")
        expression.append(number)
        updateExpressionDisplay()
        updateMainDisplay()
    }

    fun addOperator(operator: String) {
        justCalculated = false
        val canAppend =
            when {
                expression.isEmpty() || expression.last() == '(' -> operator == "-"
                else -> !isOperator(expression.last())
            }
        if (canAppend) {
            expression.append(operator)
            updateExpressionDisplay()
        }
    }

    private fun isOperator(c: Char): Boolean = c in setOf('+', '-', '*', '/')

    private fun currentToken(): String = expression.toString().takeLastWhile { it.isDigit() || it == '.' }

    private fun evaluateExpression(expression: String): Double = ExpressionBuilder(expression).build().evaluate()
}
