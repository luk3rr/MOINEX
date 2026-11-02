/*
 * Filename: RebalanceController.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment.view

import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.AllocationDTO
import org.moinex.model.dto.RebalanceAction
import org.moinex.model.dto.RebalanceRowDTO
import org.moinex.service.PreferencesService
import org.moinex.service.investment.PortfolioAnalyticsService
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller
import java.math.BigDecimal

/**
 * Suggests how much to buy or sell of each asset type to reach the allocation targets, optionally with a contribution.
 */
@Controller
@Scope("prototype")
class RebalanceController(
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var contributionField: TextField

    @FXML
    private lateinit var rebalanceTable: TableView<RebalanceRowDTO>

    private var allocations: List<AllocationDTO> = emptyList()

    @FXML
    fun initialize() {
        rebalanceTable.columns.addAll(
            textColumn(TranslationKeys.SAVINGS_REBALANCE_COLUMN_TYPE, Pos.CENTER_LEFT) { it.typeName },
            textColumn(TranslationKeys.SAVINGS_REBALANCE_COLUMN_CURRENT) { UIUtils.formatPercentage(it.currentPct) },
            textColumn(TranslationKeys.SAVINGS_REBALANCE_COLUMN_TARGET) { UIUtils.formatPercentage(it.targetPct) },
            textColumn(TranslationKeys.SAVINGS_REBALANCE_COLUMN_DEVIATION) { deviation(it.deviationPp) },
            textColumn(TranslationKeys.SAVINGS_REBALANCE_COLUMN_ACTION) { actionText(it) },
        )

        contributionField.textProperty().addListener { _, _, _ -> refresh() }
    }

    fun setAllocations(allocations: List<AllocationDTO>) {
        this.allocations = allocations
        refresh()
    }

    @FXML
    fun handleClose() {
        (rebalanceTable.scene.window as Stage).close()
    }

    private fun refresh() {
        val contribution =
            contributionField.text
                ?.trim()
                ?.replace(",", ".")
                ?.toBigDecimalOrNull()

        rebalanceTable.items.setAll(PortfolioAnalyticsService.rebalance(allocations, contribution))
    }

    private fun deviation(value: BigDecimal): String {
        val sign =
            when {
                value.signum() > 0 -> "+"
                value.signum() < 0 -> "−"
                else -> ""
            }

        return sign + UIUtils.formatNumWithDecimalPlaces(value.abs(), 2) + " pp"
    }

    private fun actionText(row: RebalanceRowDTO): String =
        when (row.action) {
            RebalanceAction.BUY ->
                "${preferencesService.translate(
                    TranslationKeys.SAVINGS_REBALANCE_BUY,
                )} ${UIUtils.formatCurrency(row.amount)}"
            RebalanceAction.SELL ->
                "${preferencesService.translate(
                    TranslationKeys.SAVINGS_REBALANCE_SELL,
                )} ${UIUtils.formatCurrency(row.amount)}"
            RebalanceAction.NONE -> preferencesService.translate(TranslationKeys.SAVINGS_REBALANCE_NONE)
        }

    private fun textColumn(
        headerKey: String,
        alignment: Pos = Pos.CENTER,
        extractor: (RebalanceRowDTO) -> String,
    ): TableColumn<RebalanceRowDTO, String> =
        TableColumn<RebalanceRowDTO, String>(preferencesService.translate(headerKey)).apply {
            setCellValueFactory { SimpleStringProperty(extractor(it.value).ifBlank { Constants.NA_DATA }) }
            cellFactory = UIUtils.createCellFactory(alignment)
        }
}
