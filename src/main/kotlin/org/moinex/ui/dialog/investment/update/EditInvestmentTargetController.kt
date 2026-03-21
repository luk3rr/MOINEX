/*
 * Filename: EditInvestmentTargetController.kt (original filename: EditInvestmentTargetController.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.extension.isNotEqual
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.AssetType
import org.moinex.service.PreferencesService
import org.moinex.service.investment.InvestmentTargetService
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class EditInvestmentTargetController(
    private val investmentTargetService: InvestmentTargetService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var targetsContainer: VBox

    @FXML
    private lateinit var totalPercentageLabel: Label

    @FXML
    private lateinit var validationLabel: Label

    private val targetRows = mutableListOf<TargetRow>()

    companion object {
        private data class TargetRow(
            val assetType: AssetType,
            val percentageField: TextField,
        )

        private const val VALID_TARGET_STYLE = "-fx-font-weight: bold; -fx-text-fill: green;"
        private const val INVALID_TARGET_STYLE = "-fx-font-weight: bold; -fx-text-fill: red;"
        private const val TYPE_LABEL_STYLE = "-fx-font-weight: bold;"
    }

    @FXML
    fun initialize() {
        loadInvestmentTargets()
        setupListeners()
    }

    private fun loadInvestmentTargets() {
        targetsContainer.children.clear()
        targetRows.clear()

        AssetType.entries.forEach { assetType ->
            val targetPercentage =
                runCatching {
                    investmentTargetService.getTargetByType(assetType).targetPercentage
                }.getOrElse {
                    if (it is EntityNotFoundException) BigDecimal.ZERO else throw it
                }

            addTargetRow(assetType, targetPercentage)
        }

        updateTotalPercentage()
    }

    private fun addTargetRow(
        assetType: AssetType,
        targetPercentage: BigDecimal,
    ) {
        val row =
            HBox(10.0).apply {
                alignment = Pos.CENTER_LEFT
            }

        val typeLabel =
            Label(UIUtils.translateAssetType(assetType)).apply {
                minWidth = 200.0
                style = TYPE_LABEL_STYLE
            }

        val percentageField =
            TextField().apply {
                text = targetPercentage.toString()
                promptText = "0.00"
                prefWidth = 100.0
            }

        val percentLabel = Label("%")

        row.children.addAll(typeLabel, percentageField, percentLabel)
        targetsContainer.children.add(row)

        val targetRow = TargetRow(assetType, percentageField)
        targetRows.add(targetRow)

        percentageField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.PERCENTAGE_REGEX))) {
                percentageField.text = oldValue
            } else {
                updateTotalPercentage()
            }
        }
    }

    private fun setupListeners() {
        updateTotalPercentage()
    }

    private fun updateTotalPercentage() {
        val targets = mutableMapOf<AssetType, BigDecimal>()

        targetRows.forEach { row ->
            runCatching {
                val text = row.percentageField.text.trim()
                val value = if (text.isEmpty()) BigDecimal.ZERO else text.toBigDecimal()
                targets[row.assetType] = value
            }
        }

        val validationResult = investmentTargetService.validate(targets)

        totalPercentageLabel.text = UIUtils.formatPercentage(validationResult.total)

        if (validationResult.isValid && validationResult.total.isEqual(100)) {
            totalPercentageLabel.style = VALID_TARGET_STYLE
            validationLabel.isVisible = false
        } else {
            totalPercentageLabel.style = INVALID_TARGET_STYLE

            validationLabel.text =
                when {
                    !validationResult.isValid -> validationResult.errors.first()
                    validationResult.total.isNotEqual(100) ->
                        preferencesService.translate(
                            TranslationKeys.INVESTMENT_DIALOG_TOTAL_PERCENTAGE_VALIDATION,
                        )
                    else -> ""
                }
            validationLabel.isVisible = true
        }
    }

    @FXML
    private fun handleSave() {
        val targets = mutableMapOf<AssetType, BigDecimal>()

        for (row in targetRows) {
            runCatching {
                val text = row.percentageField.text.trim()
                val percentage = if (text.isEmpty()) BigDecimal.ZERO else text.toBigDecimal()
                targets[row.assetType] = percentage
            }.onFailure {
                if (it is NumberFormatException) {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_PERCENTAGE_TITLE),
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_INVALID_PERCENTAGE_MESSAGE),
                    )
                    return
                }
            }
        }

        runCatching {
            investmentTargetService.saveTargets(targets)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_TARGET_UPDATED_TITLE),
                preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_TARGET_UPDATED_MESSAGE),
            )

            targetsContainer.scene.window.hide()
        }.onFailure { e ->
            when (e) {
                is IllegalStateException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.INVESTMENT_DIALOG_TOTAL_PERCENTAGE_VALIDATION_TITLE,
                        ),
                        e.message ?: "",
                    )
                }
                else -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.INVESTMENT_DIALOG_ERROR_UPDATING_TARGET_TITLE),
                        e.message ?: "Unknown error",
                    )
                }
            }
        }
    }

    @FXML
    private fun handleCancel() {
        targetsContainer.scene.window.hide()
    }
}
