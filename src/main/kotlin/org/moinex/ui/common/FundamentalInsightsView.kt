/*
 * Filename: FundamentalInsightsView.kt
 * Created on: November 1, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.RowConstraints
import javafx.scene.layout.VBox
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.EpsSource
import org.moinex.model.dto.FundamentalSnapshot
import org.moinex.model.dto.MetricReason
import org.moinex.model.dto.QualityScore
import org.moinex.model.dto.ValuationLevel
import org.moinex.model.dto.ValuationVerdict
import java.math.BigDecimal

/**
 * Builds the two summary cards shown above the metric tabs: the valuation verdict and the quality score.
 */
class FundamentalInsightsView(
    private val translate: (String) -> String,
) {
    companion object {
        private const val CARD_CLASS = "insight-card"
        private const val TITLE_CLASS = "insight-title"
        private const val ROW_LABEL_CLASS = "insight-row-label"
        private const val NOTE_CLASS = "insight-note"
        private const val BADGE_CLASS = "valuation-badge"
        private const val SCORE_CLASS = "insight-score"
        private const val CARD_SPACING = 6.0
        private const val CARD_PADDING = 10.0
        private const val GRID_GAP = 10.0
        private const val ROW_SPACING = 6.0
        private const val SCORE_SPACING = 0.0
        private const val SCORE_GAP = 18.0
        private const val DIMENSION_VALUE_CLASS = "insight-dimension-value"
        private const val SCORE_UNAVAILABLE = "—"
    }

    fun build(
        snapshot: FundamentalSnapshot,
        verdict: ValuationVerdict,
        quality: QualityScore,
    ): List<Node> = listOf(valuationCard(snapshot, verdict), qualityCard(quality))

    fun notice(text: String): Node = card().apply { children.add(noteLabel(text)) }

    private fun valuationCard(
        snapshot: FundamentalSnapshot,
        verdict: ValuationVerdict,
    ): Node {
        val card = card()
        val badge =
            Label("● ${translate(FundamentalInsightText.valuationKey(verdict.level))}").apply {
                styleClass.addAll(BADGE_CLASS, FundamentalInsightText.valuationStyleClass(verdict.level))
            }
        card.children.add(
            headerRow(
                titleLabel(translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_TITLE)),
                badge,
                translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_TOOLTIP),
            ),
        )

        if (verdict.level == ValuationLevel.UNAVAILABLE) {
            val reason = verdict.reason ?: MetricReason.DATA_UNAVAILABLE
            card.children.add(noteLabel(translate(FundamentalReasonText.translationKey(reason))))
        } else {
            card.children.add(
                row(TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_CURRENT_PRICE, currency(verdict.price)),
            )
            card.children.add(
                row(TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_GRAHAM_FAIR_VALUE, currency(verdict.fairValue)),
            )
            card.children.add(
                row(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_METRIC_MARGIN_OF_SAFETY,
                    verdict.marginOfSafetyPct?.let { UIUtils.formatPercentageForFundamentalAnalysis(it) }
                        ?: SCORE_UNAVAILABLE,
                ),
            )
        }

        if (snapshot.epsSource == EpsSource.STATEMENT) {
            card.children.add(
                noteLabel(translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_EPS_FROM_STATEMENT)),
            )
        }

        return card
    }

    private fun qualityCard(quality: QualityScore): Node {
        val card = card()
        val title = titleLabel(translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_TITLE))
        val info = translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_TOOLTIP)

        if (quality.total == null || quality.label == null) {
            card.children.add(headerRow(title, null, info))
            card.children.add(noteLabel(translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_UNAVAILABLE)))
            return card
        }

        val badge =
            Label("● ${translate(FundamentalInsightText.qualityKey(quality.label))}").apply {
                styleClass.addAll(BADGE_CLASS, FundamentalInsightText.qualityStyleClass(quality.label))
            }
        card.children.add(headerRow(title, badge, info))

        val score =
            VBox(
                SCORE_SPACING,
                Label(quality.total.toString()).apply { styleClass.add(SCORE_CLASS) },
                Label("/ 100").apply { styleClass.add(NOTE_CLASS) },
            ).apply { alignment = Pos.CENTER }

        val grid =
            GridPane().apply {
                hgap = GRID_GAP * 2
                vgap = CARD_SPACING
                alignment = Pos.CENTER_LEFT
                columnConstraints.addAll(
                    ColumnConstraints().apply { hgrow = Priority.ALWAYS },
                    ColumnConstraints().apply { hgrow = Priority.ALWAYS },
                )
                rowConstraints.addAll(
                    RowConstraints().apply {
                        vgrow = Priority.ALWAYS
                        valignment = VPos.CENTER
                    },
                    RowConstraints().apply {
                        vgrow = Priority.ALWAYS
                        valignment = VPos.CENTER
                    },
                )
            }
        quality.dimensions.forEachIndexed { index, dimension ->
            val label = translate(FundamentalInsightText.dimensionKey(dimension.type))
            val value = dimension.score?.toString() ?: SCORE_UNAVAILABLE
            val tip = translate(FundamentalInsightText.dimensionTooltipKey(dimension.type))

            grid.add(dimensionRow(label, value, tip), index % 2, index / 2)
        }
        HBox.setHgrow(grid, Priority.ALWAYS)

        val body = HBox(SCORE_GAP, score, grid).apply { alignment = Pos.CENTER_LEFT }
        VBox.setVgrow(body, Priority.ALWAYS)
        card.children.add(body)

        return card
    }

    private fun headerRow(
        title: Label,
        badge: Label?,
        info: String,
    ): Node =
        HBox(CARD_SPACING).apply {
            alignment = Pos.CENTER_LEFT
            children.add(title)
            badge?.let { children.add(it) }
            children.add(Region().apply { HBox.setHgrow(this, Priority.ALWAYS) })
            children.add(InfoIcon.create(info))
        }

    private fun dimensionRow(
        label: String,
        value: String,
        info: String,
    ): Node =
        HBox(
            ROW_SPACING,
            Label(label).apply { styleClass.add(ROW_LABEL_CLASS) },
            InfoIcon.create(info),
            Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
            Label(value).apply { styleClass.add(DIMENSION_VALUE_CLASS) },
        ).apply { alignment = Pos.CENTER_LEFT }

    private fun card(): VBox =
        VBox(CARD_SPACING).apply {
            styleClass.add(CARD_CLASS)
            padding = Insets(CARD_PADDING)
            HBox.setHgrow(this, Priority.ALWAYS)
            prefWidth = 0.0
            maxWidth = Double.MAX_VALUE
        }

    private fun titleLabel(text: String): Label = Label(text).apply { styleClass.add(TITLE_CLASS) }

    private fun noteLabel(text: String): Label =
        Label(text).apply {
            styleClass.add(NOTE_CLASS)
            isWrapText = true
        }

    private fun row(
        labelKey: String,
        value: String,
    ): Node = rowWithText(translate(labelKey), value)

    private fun rowWithText(
        label: String,
        value: String,
    ): Node =
        HBox(
            Label(label).apply { styleClass.add(ROW_LABEL_CLASS) },
            Region().apply { HBox.setHgrow(this, Priority.ALWAYS) },
            Label(value),
        )

    private fun currency(value: BigDecimal?): String = value?.let { UIUtils.formatCurrency(it) } ?: SCORE_UNAVAILABLE
}
