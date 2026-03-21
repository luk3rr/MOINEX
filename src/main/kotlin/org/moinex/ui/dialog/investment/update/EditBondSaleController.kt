/*
 * Filename: EditBondSaleController.kt (original filename: EditBondSaleController.java)
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.update

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isEqual
import org.moinex.common.util.UIUtils
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
import java.math.RoundingMode

@Controller
class EditBondSaleController(
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
    @FXML
    private lateinit var netProfitField: TextField

    @FXML
    private lateinit var grossYieldField: TextField

    private lateinit var operation: BondOperation

    init {
        walletTransactionType = WalletTransactionType.INCOME
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
        netProfitField.text = operation.netProfit.toString()

        statusComboBox.value = operation.walletTransaction!!.status
        categoryComboBox.value = operation.walletTransaction!!.category
        transactionDatePicker.value = operation.walletTransaction!!.date.toLocalDate()
        includeInAnalysisCheckBox.isSelected = operation.walletTransaction!!.includeInAnalysis

        updateTotalPrice()
    }

    @FXML
    override fun initialize() {
        super.initialize()

        unitPriceField.textProperty().addListener { _, _, _ ->
            if (!netProfitField.isFocused) {
                calculateProfit()
            }
        }

        quantityField.textProperty().addListener { _, _, _ ->
            if (!netProfitField.isFocused) {
                calculateProfit()
            }
        }

        feesField.textProperty().addListener { _, _, _ ->
            if (!netProfitField.isFocused) {
                calculateProfit()
            }
        }

        taxesField.textProperty().addListener { _, _, _ ->
            if (!netProfitField.isFocused) {
                calculateProfit()
            }
        }

        grossYieldField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                grossYieldField.text = oldValue
            } else {
                if (!netProfitField.isFocused) calculateProfit()
                updateTotalPrice()
                walletAfterBalance()
            }
        }

        netProfitField.textProperty().addListener { _, oldValue, newValue ->
            if (!newValue.matches(Regex(Constants.SIGNED_MONETARY_VALUE_REGEX))) {
                netProfitField.text = oldValue
            } else {
                updateNetProfitColor(newValue)
                updateTotalPrice()
                walletAfterBalance()
            }
        }
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
        val saleDate = transactionDatePicker.value

        if (wallet == null ||
            description.isNullOrBlank() ||
            status == null ||
            category == null ||
            unitPriceStr.isNullOrBlank() ||
            quantityStr.isNullOrBlank() ||
            saleDate == null
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

            val netProfitStr = netProfitField.text
            val netProfit = if (!netProfitStr.isNullOrBlank()) netProfitStr.toBigDecimal() else BigDecimal.ZERO

            val feesEqual = operation.fees.isEqual(fees)
            val taxesEqual = operation.taxes.isEqual(taxes)
            val netProfitEqual = operation.netProfit.isEqual(netProfit)

            if (operation.walletTransaction!!.wallet.id == wallet.id &&
                operation.walletTransaction!!.description == description &&
                operation.walletTransaction!!.status == status &&
                operation.walletTransaction!!.category.id == category.id &&
                operation.walletTransaction!!.includeInAnalysis == includeInAnalysisCheckBox.isSelected &&
                operation.unitPrice.isEqual(unitPrice) &&
                operation.quantity.isEqual(quantity) &&
                feesEqual &&
                taxesEqual &&
                netProfitEqual &&
                operation.walletTransaction!!.date.toLocalDate() == saleDate
            ) {
                WindowUtils.showInformationDialog(
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_NO_CHANGES_SALE_MESSAGE),
                )
            } else {
                operation.quantity = quantity
                operation.unitPrice = unitPrice
                operation.fees = fees
                operation.taxes = taxes
                operation.netProfit = netProfit

                bondService.updateBondOperation(
                    operation,
                    WalletTransactionContextDTO(
                        wallet,
                        saleDate.atStartOfDay(),
                        category,
                        description,
                        status,
                        includeInAnalysisCheckBox.isSelected,
                    ),
                )

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_SALE_UPDATED_TITLE),
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_SALE_UPDATED_MESSAGE),
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
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_ERROR_UPDATING_SALE_TITLE),
                        e.message ?: "",
                    )
                }
                else -> throw e
            }
        }
    }

    override fun updateTotalPrice() {
        val unitPriceStr = unitPriceField.text
        val quantityStr = quantityField.text
        val netProfitStr = netProfitField.text

        var totalPrice = BigDecimal("0.00")

        if (unitPriceStr.isNullOrBlank() || quantityStr.isNullOrBlank()) {
            totalPriceLabel.text = UIUtils.formatCurrency(totalPrice)
            return
        }

        runCatching {
            val unitPrice = unitPriceStr.toBigDecimal()
            val quantity = quantityStr.toBigDecimal()
            val netProfit = if (!netProfitStr.isNullOrBlank()) netProfitStr.toBigDecimal() else BigDecimal.ZERO

            totalPrice = unitPrice.multiply(quantity).add(netProfit)

            totalPriceLabel.text = UIUtils.formatCurrency(totalPrice)
        }.onFailure {
            totalPriceLabel.text = UIUtils.formatCurrency(totalPrice)
        }
    }

    override fun walletAfterBalance() {
        val unitPriceStr = unitPriceField.text
        val quantityStr = quantityField.text
        val netProfitStr = netProfitField.text
        val wt = walletComboBox.value

        if (unitPriceStr.isNullOrBlank() || quantityStr.isNullOrBlank() || wt == null) {
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
            return
        }

        runCatching {
            val unitPrice = unitPriceStr.toBigDecimal()
            val quantity = quantityStr.toBigDecimal()
            val netProfit = if (!netProfitStr.isNullOrBlank()) netProfitStr.toBigDecimal() else BigDecimal.ZERO

            val transactionValue = unitPrice.multiply(quantity).add(netProfit)

            if (transactionValue < BigDecimal.ZERO) {
                UIUtils.resetLabel(walletAfterBalanceValueLabel)
                return
            }

            val walletAfterBalanceValue = wt.balance.add(transactionValue)

            if (walletAfterBalanceValue < BigDecimal.ZERO) {
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel, Styles.NEGATIVE_BALANCE_STYLE)
            } else {
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel, Styles.NEUTRAL_BALANCE_STYLE)
            }

            walletAfterBalanceValueLabel.text = UIUtils.formatCurrency(walletAfterBalanceValue)
        }.onFailure {
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
        }
    }

    private fun updateNetProfitColor(valueStr: String) {
        if (valueStr.isBlank() || valueStr == "-") {
            netProfitField.styleClass.removeAll(
                Styles.NEGATIVE_BALANCE_STYLE,
                Styles.POSITIVE_BALANCE_STYLE,
                Styles.NEUTRAL_BALANCE_STYLE,
            )
            return
        }

        runCatching {
            val value = valueStr.toBigDecimal()

            netProfitField.styleClass.removeAll(
                Styles.NEGATIVE_BALANCE_STYLE,
                Styles.POSITIVE_BALANCE_STYLE,
                Styles.NEUTRAL_BALANCE_STYLE,
            )

            when {
                value > BigDecimal.ZERO -> netProfitField.styleClass.add(Styles.POSITIVE_BALANCE_STYLE)
                value < BigDecimal.ZERO -> netProfitField.styleClass.add(Styles.NEGATIVE_BALANCE_STYLE)
                else -> netProfitField.styleClass.add(Styles.NEUTRAL_BALANCE_STYLE)
            }
        }.onFailure {
            netProfitField.styleClass.removeAll(
                Styles.NEGATIVE_BALANCE_STYLE,
                Styles.POSITIVE_BALANCE_STYLE,
                Styles.NEUTRAL_BALANCE_STYLE,
            )
        }
    }

    private fun calculateProfit() {
        if (!::netProfitField.isInitialized || !::grossYieldField.isInitialized) {
            return
        }

        val unitPriceStr = unitPriceField.text
        val quantityStr = quantityField.text
        val interestStr = grossYieldField.text
        val feesStr = feesField.text
        val taxesStr = taxesField.text

        if (unitPriceStr.isNullOrBlank() || quantityStr.isNullOrBlank()) {
            netProfitField.text = ""
            return
        }

        runCatching {
            val saleUnitPrice = unitPriceStr.toBigDecimal()
            val quantity = quantityStr.toBigDecimal()
            val interest = interestStr.toBigDecimal()
            val fees = if (!feesStr.isNullOrBlank()) feesStr.toBigDecimal() else BigDecimal.ZERO
            val taxes = if (!taxesStr.isNullOrBlank()) taxesStr.toBigDecimal() else BigDecimal.ZERO

            val averageUnitPrice = bondService.getAverageUnitPrice(bond)

            val profit =
                saleUnitPrice
                    .subtract(averageUnitPrice)
                    .multiply(quantity)
                    .add(interest)
                    .subtract(fees)
                    .subtract(taxes)
                    .setScale(2, RoundingMode.HALF_UP)

            netProfitField.text = profit.toString()
        }.onFailure {
            netProfitField.text = ""
        }
    }
}
