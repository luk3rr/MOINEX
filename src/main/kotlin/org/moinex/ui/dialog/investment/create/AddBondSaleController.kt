/*
 * Filename: AddBondSaleController.kt (original filename: AddBondSaleController.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.investment.create

import jakarta.persistence.EntityNotFoundException
import javafx.fxml.FXML
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.extension.toRounded
import org.moinex.common.util.UIUtils
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
class AddBondSaleController(
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

    init {
        walletTransactionType = WalletTransactionType.INCOME
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

            bondService.createBondOperation(
                BondOperation(
                    bond = bond,
                    operationType = OperationType.SELL,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    fees = fees,
                    taxes = taxes,
                    netProfit = netProfit,
                ),
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
                preferencesService.translate(TranslationKeys.BOND_DIALOG_SALE_ADDED_TITLE),
                preferencesService.translate(TranslationKeys.BOND_DIALOG_SALE_ADDED_MESSAGE),
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
                        preferencesService.translate(TranslationKeys.BOND_DIALOG_ERROR_SELLING_TITLE),
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
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel, Constants.NEGATIVE_BALANCE_STYLE)
            } else {
                UIUtils.setLabelStyle(walletAfterBalanceValueLabel, Constants.NEUTRAL_BALANCE_STYLE)
            }

            walletAfterBalanceValueLabel.text = UIUtils.formatCurrency(walletAfterBalanceValue)
        }.onFailure {
            UIUtils.resetLabel(walletAfterBalanceValueLabel)
        }
    }

    private fun updateNetProfitColor(valueStr: String) {
        if (valueStr.isBlank() || valueStr == "-") {
            netProfitField.styleClass.removeAll(
                Constants.NEGATIVE_BALANCE_STYLE,
                Constants.POSITIVE_BALANCE_STYLE,
                Constants.NEUTRAL_BALANCE_STYLE,
            )
            return
        }

        runCatching {
            val value = valueStr.toBigDecimal()

            netProfitField.styleClass.removeAll(
                Constants.NEGATIVE_BALANCE_STYLE,
                Constants.POSITIVE_BALANCE_STYLE,
                Constants.NEUTRAL_BALANCE_STYLE,
            )

            when {
                value > BigDecimal.ZERO -> netProfitField.styleClass.add(Constants.POSITIVE_BALANCE_STYLE)
                value < BigDecimal.ZERO -> netProfitField.styleClass.add(Constants.NEGATIVE_BALANCE_STYLE)
                else -> netProfitField.styleClass.add(Constants.NEUTRAL_BALANCE_STYLE)
            }
        }.onFailure {
            netProfitField.styleClass.removeAll(
                Constants.NEGATIVE_BALANCE_STYLE,
                Constants.POSITIVE_BALANCE_STYLE,
                Constants.NEUTRAL_BALANCE_STYLE,
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
                    .toRounded()

            netProfitField.text = profit.toString()
        }.onFailure {
            netProfitField.text = ""
        }
    }
}
