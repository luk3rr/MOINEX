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
import javafx.scene.control.CheckBox
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import org.moinex.common.constant.Constants
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.toRounded
import org.moinex.common.ui.FieldValidation
import org.moinex.common.ui.closeDialog
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.dialog.investment.base.BaseBondTransactionManagement
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalDate

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
    private val validation = FieldValidation(preferencesService::translate)

    @FXML
    private lateinit var netProfitField: TextField

    @FXML
    private lateinit var grossYieldField: TextField

    @FXML
    private lateinit var autoCalculateCheck: CheckBox

    @FXML
    private lateinit var grossYieldGroup: HBox

    init {
        walletTransactionType = WalletTransactionType.INCOME
    }

    @FXML
    override fun initialize() {
        super.initialize()

        unitPriceField.textProperty().addListener { _, _, _ ->
            suggestRedemption()
            if (!netProfitField.isFocused) {
                calculateProfit()
            }
        }

        quantityField.textProperty().addListener { _, _, _ ->
            suggestRedemption()
            if (!netProfitField.isFocused) {
                calculateProfit()
            }
        }

        transactionDatePicker.valueProperty().addListener { _, _, _ ->
            suggestRedemption()
        }

        autoCalculateCheck.selectedProperty().addListener { _, _, _ ->
            applyAutoCalculation()
        }
        applyAutoCalculation()

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

        if (!validation.check {
                required(walletComboBox)
                required(descriptionField)
                required(statusComboBox)
                required(categoryComboBox)
                required(unitPriceField)
                number(unitPriceField)
                required(quantityField)
                number(quantityField)
                required(transactionDatePicker)
            }
        ) {
            return
        }

        runCatching {
            val unitPrice = unitPriceStr.toBigDecimal()
            val quantity = principalQuantity(quantityStr)
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

            bondNameLabel.closeDialog()
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
            val quantity = principalQuantity(quantityStr)
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
            val quantity = principalQuantity(quantityStr)
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

    override fun initializeBond(bond: Bond) {
        super.initializeBond(bond)

        if (isHeldInReais(bond)) {
            quantityLabel?.text = preferencesService.translate(TranslationKeys.BOND_LABEL_GROSS_REDEEMED)

            val row = amountRow
            if (row != null && grossYieldGroup.parent != row) {
                (grossYieldGroup.parent as? Pane)?.children?.remove(grossYieldGroup)
                row.children.add(grossYieldGroup)
            }
        }
    }

    /**
     * With the automatic calculation on, the interest and the tax follow the amount and cannot be typed; turning it
     * off leaves the values as they are and lets the user change them, and turning it on calculates them again.
     */
    private fun applyAutoCalculation() {
        val automatic = autoCalculateCheck.isSelected

        grossYieldField.isEditable = !automatic
        taxesField.isEditable = !automatic

        if (automatic) {
            suggestRedemption()
        }
    }

    /**
     * What leaves the bond as principal: for a bond held in reais the field holds the gross amount redeemed, and the
     * principal is that amount less the interest taken out in it; for any other bond it is the quantity typed.
     */
    private fun principalQuantity(quantityText: String): BigDecimal {
        val typed = quantityText.toBigDecimal()
        val heldInReais = runCatching { isHeldInReais(bond) }.getOrDefault(false)

        if (!heldInReais) return typed

        val interestText = grossYieldField.text
        val interest = if (interestText.isNullOrBlank()) BigDecimal.ZERO else interestText.toBigDecimal()

        return typed.subtract(interest).max(BigDecimal.ZERO)
    }

    private fun suggestRedemption() {
        if (!::grossYieldField.isInitialized || !::autoCalculateCheck.isInitialized) return
        if (!autoCalculateCheck.isSelected) return

        val selectedBond = runCatching { bond }.getOrNull()
        val quantity = quantityField.text?.toBigDecimalOrNull()
        val unitPrice = unitPriceField.text?.toBigDecimalOrNull()

        if (selectedBond == null || quantity == null || unitPrice == null) {
            grossYieldField.text = ""
            taxesField.text = ""
            return
        }

        val date = transactionDatePicker.value ?: LocalDate.now()

        val estimate =
            if (isHeldInReais(selectedBond)) {
                bondService.estimateRedemptionByGross(selectedBond, quantity, date)
            } else {
                bondService.estimateRedemption(selectedBond, quantity, unitPrice, date)
            }

        grossYieldField.text = estimate.interestShare.toPlainString()
        taxesField.text = if (estimate.taxApplicable) estimate.estimatedTax.toPlainString() else ""
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
            val quantity = principalQuantity(quantityStr)
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
