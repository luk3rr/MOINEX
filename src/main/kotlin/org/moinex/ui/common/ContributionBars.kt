/*
 * Filename: ContributionBars.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.moinex.common.constant.Styles
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.ContributionDTO
import java.math.BigDecimal

/**
 * What each ticker added to the portfolio, as a list of rows with a bar proportional to the profit (green) or loss (red),
 * followed by the total.
 */
class ContributionBars(
    private val translate: (String) -> String,
) {
    companion object {
        private const val BAR_AREA_WIDTH = 100.0
        private const val BAR_HEIGHT = 12.0
        private const val MIN_BAR_WIDTH = 2.0
        private const val SYMBOL_WIDTH = 85.0
        private const val ROW_SPACING = 8.0
        private const val LIST_SPACING = 6.0
        private val NEGLIGIBLE = BigDecimal("0.005")
    }

    fun build(contributions: List<ContributionDTO>): Node {
        val visible = contributions.filter { it.amount.abs() >= NEGLIGIBLE }
        if (visible.isEmpty()) {
            return Label(translate(TranslationKeys.SAVINGS_PERFORMANCE_CONTRIBUTION_EMPTY))
        }

        val largest = visible.maxOf { it.amount.abs() }
        val rows = VBox(LIST_SPACING).apply { padding = Insets(0.0, 8.0, 0.0, 0.0) }
        visible.forEach { rows.children.add(row(it, largest)) }

        val scroll =
            ScrollPane(rows).apply {
                isFitToWidth = true
                styleClass.add("transparent-scroll-pane")
                VBox.setVgrow(this, Priority.ALWAYS)
            }

        return VBox(
            LIST_SPACING,
            scroll,
            totalRow(contributions.fold(BigDecimal.ZERO) { acc, it -> acc.add(it.amount) }),
        )
    }

    private fun row(
        contribution: ContributionDTO,
        largest: BigDecimal,
    ): Node {
        val width =
            contribution.amount
                .abs()
                .divide(largest, 4, java.math.RoundingMode.HALF_UP)
                .toDouble() * BAR_AREA_WIDTH
        val bar =
            Region().apply {
                prefWidth = width.coerceAtLeast(MIN_BAR_WIDTH)
                prefHeight = BAR_HEIGHT
                maxWidth = prefWidth
                styleClass.add(
                    if (contribution.amount.signum() >= 0) "contribution-bar-positive" else "contribution-bar-negative",
                )
            }
        val barArea =
            Pane(bar).apply {
                prefWidth = BAR_AREA_WIDTH
                minWidth = BAR_AREA_WIDTH
                prefHeight = BAR_HEIGHT
            }

        return HBox(
            ROW_SPACING,
            Label(contribution.ticker.symbol).apply {
                prefWidth = SYMBOL_WIDTH
                minWidth = SYMBOL_WIDTH
            },
            barArea,
            Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
            amountLabel(contribution.amount),
        ).apply { alignment = Pos.CENTER_LEFT }
    }

    private fun totalRow(total: BigDecimal): Node =
        HBox(
            ROW_SPACING,
            Label(translate(TranslationKeys.SAVINGS_PERFORMANCE_CONTRIBUTION_TOTAL))
                .apply { styleClass.add("contribution-total-label") },
            Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
            amountLabel(total).apply { styleClass.add("contribution-total-label") },
        ).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add("contribution-total-row")
        }

    private fun amountLabel(amount: BigDecimal): Label {
        val sign =
            when {
                amount.signum() > 0 -> "+ "
                amount.signum() < 0 -> "− "
                else -> ""
            }

        return Label(sign + UIUtils.formatCurrency(amount.abs())).apply {
            minWidth = Region.USE_PREF_SIZE
            when {
                amount.signum() > 0 -> styleClass.add(Styles.POSITIVE_BALANCE_STYLE)
                amount.signum() < 0 -> styleClass.add(Styles.NEGATIVE_BALANCE_STYLE)
            }
        }
    }
}
