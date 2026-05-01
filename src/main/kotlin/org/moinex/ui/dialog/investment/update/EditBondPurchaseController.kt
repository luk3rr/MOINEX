/*
 * Filename: EditBondPurchaseController.kt (original filename: EditBondPurchaseController.java)
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.util.WindowUtils
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.BondOperation
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.investment.base.BaseBondTransactionManagement
import org.springframework.stereotype.Controller
import java.math.BigDecimal

@Controller
class EditBondPurchaseController(
    walletService: WalletService,
    categoryService: CategoryService,
    bondService: BondService,
    preferencesService: PreferencesService,
) : BaseBondTransactionManagement(
        walletService,
        categoryService,
        bondService,
        preferencesService,
    ) {
    private lateinit var operation: BondOperation

    init {
        walletTransactionType = WalletTransactionType.EXPENSE
    }

    fun setOperation(op: BondOperation) {
        operation = op
        bond = op.bond

        val symbol = bond.symbol
        bondNameLabel.text = bond.name + if (!symbol.isNullOrBlank()) " ($symbol)" else ""

        setWalletComboBox(operation.walletTransaction!!.wallet)

        descriptionField.text = operation.walletTransaction!!.description
        unitPriceField.text = operation.unitPrice.toString()
        quantityField.text = operation.quantity.toString()
        feesField.text = operation.fees.toString()
        taxesField.text = operation.taxes.toString()

        statusComboBox.value = operation.walletTransaction!!.status
        categoryComboBox.value = operation.walletTransaction!!.category
        transactionDatePicker.value = operation.walletTransaction!!.date.toLocalDate()
        includeInAnalysisCheckBox.isSelected = operation.walletTransaction!!.includeInAnalysis

        updateTotalPrice()
    }

    @FXML
    override fun handleSave() {
        val wallet = walletComboBox.value
        val description = descriptionField.text
        val status = statusComboBox.value
        val category = categoryComboBox.value
        val unitPriceStr = unitPriceField.text
        val quantityStr = quantityField.text
        val feesStr = feesField.text
        val taxesStr = taxesField.text
        val buyDate = transactionDatePicker.value

        if (wallet == null ||
            description.isNullOrBlank() ||
            status == null ||
            category == null ||
            unitPriceStr.isNullOrBlank() ||
            quantityStr.isNullOrBlank() ||
            buyDate == null
        ) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val unitPrice = unitPriceStr.toBigDecimal()
            val quantity = quantityStr.toBigDecimal()
            val fees = if (!feesStr.isNullOrBlank()) feesStr.toBigDecimal() else BigDecimal.ZERO
            val taxes = if (!taxesStr.isNullOrBlank()) taxesStr.toBigDecimal() else BigDecimal.ZERO

            val feesEqual = operation.fees.isEqual(fees)
            val taxesEqual = operation.taxes.isEqual(taxes)

            if (operation.walletTransaction!!.wallet.id == wallet.id &&
                operation.walletTransaction!!.description == description &&
                operation.walletTransaction!!.status == status &&
                operation.walletTransaction!!.category.id == category.id &&
                operation.walletTransaction!!.includeInAnalysis == includeInAnalysisCheckBox.isSelected &&
                operation.unitPrice.isEqual(unitPrice) &&
                operation.quantity.isEqual(quantity) &&
                feesEqual &&
                taxesEqual &&
                operation.walletTransaction!!.date.toLocalDate() == buyDate
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_CHANGES_PURCHASE_MESSAGE),
                )
            } else {
                operation.quantity = quantity
                operation.unitPrice = unitPrice
                operation.fees = fees
                operation.taxes = taxes

                bondService.updateBondOperation(
                    operation,
                    WalletTransactionContextDTO(
                        wallet,
                        buyDate.atStartOfDay(),
                        category,
                        description,
                        status,
                        includeInAnalysisCheckBox.isSelected,
                    ),
                )
            }

            (bondNameLabel.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_INVALID_NUMBER_TITLE),
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_INVALID_NUMBER_MESSAGE),
                    )
                }
                is EntityNotFoundException, is IllegalArgumentException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_ERROR_UPDATING_PURCHASE_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
