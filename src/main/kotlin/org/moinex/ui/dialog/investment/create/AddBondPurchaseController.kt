/*
 * Filename: AddBondPurchaseController.kt (original filename: AddBondPurchaseController.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.create

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.OperationType
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
class AddBondPurchaseController(
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
    init {
        walletTransactionType = WalletTransactionType.EXPENSE
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

            bondService.createBondOperation(
                BondOperation(
                    bond = bond,
                    operationType = OperationType.BUY,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    fees = fees,
                    taxes = taxes,
                    netProfit = BigDecimal.ZERO,
                ),
                WalletTransactionContextDTO(
                    wallet,
                    buyDate.atStartOfDay(),
                    category,
                    description,
                    status,
                    includeInAnalysisCheckBox.isSelected,
                ),
            )

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_PURCHASE_ADDED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_PURCHASE_ADDED_MESSAGE),
            )

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
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_ERROR_BUYING_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }
}
