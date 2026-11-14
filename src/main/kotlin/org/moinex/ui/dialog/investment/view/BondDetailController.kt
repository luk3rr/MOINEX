/*
 * Filename: BondDetailController.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment.view

import atlantafx.base.controls.SegmentedControl
import javafx.fxml.FXML
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.BondRateText
import org.moinex.common.ui.ChartCrosshair
import org.moinex.common.ui.SegmentedFilter
import org.moinex.common.ui.closeDialog
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.dto.BondDetailDTO
import org.moinex.model.dto.MonthBalanceDTO
import org.moinex.model.investment.Bond
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondService
import org.moinex.ui.dialog.investment.create.AddBondPurchaseController
import org.moinex.ui.dialog.investment.create.AddBondSaleController
import org.moinex.ui.dialog.investment.update.EditBondController
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.text.MessageFormat

/**
 * The window of one bond: its data and position, its balance over the last months, its interest history and its
 * operations, with the actions of buying, selling, editing and archiving it.
 */
@Controller
class BondDetailController(
    private val bondService: BondService,
    private val preferencesService: PreferencesService,
    private val springContext: ConfigurableApplicationContext,
) {
    @FXML
    private lateinit var titleLabel: Label

    @FXML
    private lateinit var subtitleLabel: Label

    @FXML
    private lateinit var dataPane: HBox

    @FXML
    private lateinit var positionPane: HBox

    @FXML
    private lateinit var balancePeriodSegment: SegmentedControl

    @FXML
    private lateinit var yearComboBox: ComboBox<Int?>

    @FXML
    private lateinit var balanceHolder: StackPane

    @FXML
    private lateinit var tabs: TabPane

    @FXML
    private lateinit var historyTab: Tab

    @FXML
    private lateinit var operationsTab: Tab

    @FXML
    private lateinit var archiveButton: Button

    private lateinit var bond: Bond
    private var historyController: BondInterestHistoryController? = null
    private var operationsController: BondTransactionsController? = null
    private var balancePeriod: SegmentedFilter<Int?>? = null
    private var monthlyBalance: List<MonthBalanceDTO> = emptyList()

    companion object {
        private val logger = LoggerFactory.getLogger(BondDetailController::class.java)
        private const val BALANCE_MONTHS = 12
        private const val CARD_WIDTH = 180.0
    }

    @FXML
    private fun initialize() {
        balancePeriod =
            SegmentedFilter(
                balancePeriodSegment,
                listOf(
                    "12M" to BALANCE_MONTHS,
                    preferencesService.translate(TranslationKeys.BOND_DETAIL_BALANCE_ALL) to null,
                ),
            ).also { filter ->
                filter.value = BALANCE_MONTHS
                filter.onChange { showBalance() }
            }
        yearComboBox.converter =
            object : StringConverter<Int?>() {
                override fun toString(value: Int?): String =
                    value?.toString() ?: preferencesService.translate(TranslationKeys.BOND_DETAIL_YEAR_ALL)

                override fun fromString(string: String): Int? = string.toIntOrNull()
            }
        yearComboBox.valueProperty().addListener { _, _, year ->
            historyController?.showYear(year)
            operationsController?.showYear(year)
        }
    }

    fun setBond(selected: Bond) {
        bond = selected

        val (historyRoot, history) =
            WindowUtils.loadEmbedded<BondInterestHistoryController>(Files.BOND_INTEREST_HISTORY_FXML, springContext)
        history.setEmbedded()
        history.setBond(bond)
        historyController = history
        historyTab.content = historyRoot

        val (operationsRoot, operations) =
            WindowUtils.loadEmbedded<BondTransactionsController>(Files.BOND_TRANSACTIONS_FXML, springContext)
        operations.setBond(bond)
        operationsController = operations
        operationsTab.content = operationsRoot

        showPosition()
        loadYears()
    }

    private fun loadYears() {
        val years =
            ((historyController?.years() ?: emptyList()) + (operationsController?.years() ?: emptyList()))
                .distinct()
                .sortedDescending()

        yearComboBox.items.setAll(listOf<Int?>(null) + years)
        yearComboBox.value = null
    }

    private fun showPosition() {
        val detail = bondService.detail(bond)

        titleLabel.text = bond.name
        subtitleLabel.text = bond.symbol.orEmpty()
        subtitleLabel.isVisible = !bond.symbol.isNullOrBlank()
        subtitleLabel.isManaged = !bond.symbol.isNullOrBlank()

        dataPane.children.setAll(dataCards())
        positionPane.children.setAll(positionCards(detail))
        monthlyBalance = detail.monthlyBalance
        showBalance()
    }

    private fun dataCards(): List<VBox> =
        listOf(
            card(TranslationKeys.BOND_DETAIL_FIELD_TYPE, UIUtils.translateBondType(bond.type)),
            card(TranslationKeys.BOND_DETAIL_FIELD_ISSUER, bond.issuer?.takeIf { it.isNotBlank() } ?: "-"),
            card(TranslationKeys.BOND_DETAIL_FIELD_RATE, rateText()),
            card(
                TranslationKeys.BOND_DETAIL_FIELD_INDEX,
                bond.interestIndex?.let { UIUtils.translateInterestIndex(it) } ?: "-",
            ),
            card(
                TranslationKeys.BOND_DETAIL_FIELD_MATURITY,
                bond.maturityDate?.let { UIUtils.formatDateForDisplay(it) } ?: "-",
            ),
        )

    private fun positionCards(detail: BondDetailDTO): List<VBox> {
        val net = detail.netInterest

        return listOf(
            card(TranslationKeys.BOND_DETAIL_FIELD_INVESTED, UIUtils.formatCurrency(detail.invested)),
            card(TranslationKeys.BOND_DETAIL_FIELD_CURRENT, UIUtils.formatCurrency(detail.currentValue)),
            card(
                TranslationKeys.BOND_DETAIL_FIELD_YIELD,
                UIUtils.formatCurrencySigned(detail.accumulatedInterest),
                detail.accumulatedInterest,
            ),
            card(
                TranslationKeys.BOND_DETAIL_FIELD_MONTH,
                UIUtils.formatCurrencySigned(detail.monthInterest),
                detail.monthInterest,
            ),
            card(
                TranslationKeys.BOND_DETAIL_FIELD_NET,
                if (net.taxApplicable) {
                    UIUtils.formatCurrency(net.netInterest)
                } else {
                    preferencesService.translate(TranslationKeys.BOND_DETAIL_NET_NOT_APPLICABLE)
                },
            ),
        )
    }

    private fun card(
        titleKey: String,
        value: String,
        signed: BigDecimal? = null,
    ): VBox {
        val valueLabel = Label(value).apply { styleClass.add("bond-detail-value") }

        if (signed != null && signed.signum() != 0) {
            valueLabel.styleClass.add(
                if (signed.signum() >
                    0
                ) {
                    Styles.POSITIVE_BALANCE_STYLE
                } else {
                    Styles.NEGATIVE_BALANCE_STYLE
                },
            )
        }

        return VBox(
            2.0,
            Label(preferencesService.translate(titleKey)).apply { styleClass.add("bond-detail-caption") },
            valueLabel,
        ).apply {
            styleClass.add("bond-detail-card")
            minWidth = CARD_WIDTH
            prefWidth = CARD_WIDTH
            maxWidth = Double.MAX_VALUE
            HBox.setHgrow(this, Priority.ALWAYS)
        }
    }

    private fun rateText(): String =
        BondRateText.format(
            bond.interestType,
            bond.interestIndex,
            bond.interestRate,
            preferencesService.translate(TranslationKeys.BOND_DETAIL_RATE_PERCENT_OF_INDEX),
            preferencesService.translate(TranslationKeys.BOND_DETAIL_RATE_INDEX_PLUS),
            preferencesService.translate(TranslationKeys.BOND_DETAIL_RATE_YEARLY),
        )

    private fun showBalance() {
        balanceHolder.children.clear()

        val months = balancePeriod?.value
        val shown = if (months == null) monthlyBalance else monthlyBalance.takeLast(months)

        if (shown.isEmpty()) {
            balanceHolder.children.add(Label(preferencesService.translate(TranslationKeys.BOND_DETAIL_BALANCE_EMPTY)))
            return
        }

        val series =
            XYChart.Series<String, Number>().apply {
                name = preferencesService.translate(TranslationKeys.BOND_DETAIL_BALANCE_HEADING)
                data.addAll(shown.map { XYChart.Data<String, Number>(it.month.toString(), it.value) })
            }
        val chart =
            LineChart(
                CategoryAxis(),
                NumberAxis().apply {
                    isForceZeroInRange = false
                    tickLabelFormatter =
                        object : StringConverter<Number>() {
                            override fun toString(value: Number): String =
                                UIUtils.formatCurrency(BigDecimal(value.toDouble()))

                            override fun fromString(string: String): Number = 0
                        }
                },
            ).apply {
                data.add(series)
                isLegendVisible = false
                animated = false
                createSymbols = false
                UIUtils.applyDefaultChartStyle(this, "bond-balance-chart")
            }

        balanceHolder.children.add(
            ChartCrosshair.wrapCategories(chart, "bond-balance-chart", { UIUtils.formatCurrency(BigDecimal(it)) }),
        )
    }

    private fun refresh() {
        bond = bondService.getBondById(bond.id!!)
        historyController?.setBond(bond)
        operationsController?.setBond(bond)
        showPosition()
        loadYears()
    }

    @FXML
    private fun handleBuy() {
        WindowUtils.openModalWindow(
            Files.BUY_BOND_FXML,
            preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_BUY_BOND_TITLE),
            springContext,
            { controller: AddBondPurchaseController -> controller.initializeBond(bond) },
            listOf(Runnable { refresh() }),
        )
    }

    @FXML
    private fun handleSell() {
        WindowUtils.openModalWindow(
            Files.SALE_BOND_FXML,
            preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_SELL_BOND_TITLE),
            springContext,
            { controller: AddBondSaleController -> controller.initializeBond(bond) },
            listOf(Runnable { refresh() }),
        )
    }

    @FXML
    private fun handleEdit() {
        WindowUtils.openModalWindow(
            Files.EDIT_BOND_FXML,
            preferencesService.translate(TranslationKeys.SAVINGS_BONDS_DIALOG_EDIT_BOND_TITLE),
            springContext,
            { controller: EditBondController -> controller.setBond(bond) },
            listOf(Runnable { refresh() }),
        )
    }

    @FXML
    private fun handleArchive() {
        val confirmed =
            WindowUtils.showConfirmationDialog(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.BOND_DETAIL_ARCHIVE_CONFIRM_TITLE),
                    bond.name,
                ),
                preferencesService.translate(TranslationKeys.BOND_DETAIL_ARCHIVE_CONFIRM_MESSAGE),
                preferencesService.bundle,
            )
        if (!confirmed) return

        runCatching {
            bondService.archiveBond(bond.id!!)
            handleClose()
        }.onFailure { e ->
            logger.error("Could not archive {}", bond, e)
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.DIALOG_ERROR_TITLE),
                e.message ?: "",
            )
        }
    }

    @FXML
    private fun handleClose() {
        archiveButton.closeDialog()
    }
}
