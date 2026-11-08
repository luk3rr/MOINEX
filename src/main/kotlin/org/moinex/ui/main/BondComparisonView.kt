/*
 * Filename: BondComparisonView.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main

import atlantafx.base.controls.SegmentedControl
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import org.kordamp.ikonli.javafx.FontIcon
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.ChartCrosshair
import org.moinex.common.ui.CrosshairSeries
import org.moinex.common.ui.SegmentedFilter
import org.moinex.common.util.FxUtils
import org.moinex.common.util.UIUtils
import org.moinex.model.dto.BondComparisonDTO
import org.moinex.model.dto.BondComparisonRequest
import org.moinex.model.dto.BondMonthlyDTO
import org.moinex.model.dto.ComparisonPeriod
import org.moinex.model.investment.Bond
import org.moinex.service.PreferencesService
import org.moinex.service.investment.BondComparisonService
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The comparison tab: the accumulated return of each bond against the CDI and the interest each one earned per month,
 * for a period, gross or net, with archived bonds optional. A click on a legend entry hides or shows that bond.
 */
class BondComparisonView(
    private val comparisonService: BondComparisonService,
    private val preferencesService: PreferencesService,
    private val onRegisterBond: () -> Unit,
) {
    private val content = VBox(10.0)
    val root = AnchorPane()
    private val periodSegment = SegmentedControl()
    private val taxSegment = SegmentedControl()
    private val archivedCheck = CheckBox(preferencesService.translate(TranslationKeys.BOND_COMPARISON_INCLUDE_ARCHIVED))
    private val chartHolder = StackPane()
    private val barsHolder = StackPane()
    private val legend = FlowPane(12.0, 4.0)
    private val note = Label()
    private val periodFilter: SegmentedFilter<ComparisonPeriod>
    private val taxFilter: SegmentedFilter<Boolean>

    private var loadSequence = 0
    private var comparison: BondComparisonDTO? = null
    private var monthly: BondMonthlyDTO? = null

    init {
        periodFilter =
            SegmentedFilter(
                periodSegment,
                listOf(
                    "3M" to ComparisonPeriod.M3,
                    "6M" to ComparisonPeriod.M6,
                    "12M" to ComparisonPeriod.M12,
                    preferencesService.translate(TranslationKeys.BOND_COMPARISON_PERIOD_ALL) to ComparisonPeriod.ALL,
                ),
            )
        taxFilter =
            SegmentedFilter(
                taxSegment,
                listOf(
                    preferencesService.translate(TranslationKeys.BOND_COMPARISON_TAX_GROSS) to false,
                    preferencesService.translate(TranslationKeys.BOND_COMPARISON_TAX_NET) to true,
                ),
            )
        periodFilter.value = ComparisonPeriod.M12
        periodFilter.onChange { load() }
        taxFilter.onChange { load() }
        archivedCheck.selectedProperty().addListener { _, _, _ -> load() }

        chartHolder.minHeight = MIN_CHART_HEIGHT
        chartHolder.prefHeight = CHART_HEIGHT
        barsHolder.minHeight = MIN_CHART_HEIGHT
        barsHolder.prefHeight = BARS_HEIGHT
        note.isWrapText = true

        val controls =
            HBox(
                12.0,
                archivedCheck,
                periodSegment,
                taxSegment,
            ).apply { alignment = Pos.CENTER_RIGHT }

        val linesPage = VBox(10.0, legend, chartHolder, note)
        val barsPage = VBox(10.0, barsHolder)
        val pages = listOf(linesPage, barsPage)
        val titles =
            listOf(
                preferencesService.translate(TranslationKeys.BOND_COMPARISON_VIEW_ACCUMULATED),
                preferencesService.translate(TranslationKeys.BOND_COMPARISON_VIEW_MONTHLY),
            )
        val pageTitle = Label(titles[0]).apply { style = SECTION_STYLE }
        val pageHolder = StackPane(linesPage)
        var page = 0

        fun show(index: Int) {
            page = (index + pages.size) % pages.size
            pageTitle.text = titles[page]
            pageHolder.children.setAll(pages[page])
        }

        VBox.setVgrow(pageHolder, Priority.ALWAYS)
        content.children.addAll(
            controls,
            pageTitle,
            pageHolder,
        )

        val previous = navButton("mdal-arrow_back") { show(page - 1) }
        val next = navButton("mdal-arrow_forward") { show(page + 1) }

        root.styleClass.add("anchor-pane")
        root.stylesheets.add(
            BondComparisonView::class.java.getResource("/css/component/anchor-pane.css")?.toExternalForm() ?: "",
        )
        root.stylesheets.add(
            BondComparisonView::class.java.getResource(Files.BOND_COMPARISON_STYLE_SHEET)?.toExternalForm() ?: "",
        )
        root.children.addAll(content, previous, next)
        AnchorPane.setTopAnchor(content, 0.0)
        AnchorPane.setLeftAnchor(content, 0.0)
        AnchorPane.setRightAnchor(content, 0.0)
        AnchorPane.setBottomAnchor(content, NAV_HEIGHT)
        AnchorPane.setBottomAnchor(previous, 0.0)
        AnchorPane.setRightAnchor(previous, NAV_GAP)
        AnchorPane.setBottomAnchor(next, 0.0)
        AnchorPane.setRightAnchor(next, 0.0)
    }

    private fun navButton(
        icon: String,
        action: () -> Unit,
    ): Button =
        Button().apply {
            graphic = FontIcon(icon).apply { styleClass.addAll("app-icon", "icon-size-20") }
            setOnAction { action() }
        }

    fun load() {
        val request = BondComparisonRequest(periodFilter.value, taxFilter.value, archivedCheck.isSelected)
        val sequence = ++loadSequence

        chartHolder.children.setAll(Label(preferencesService.translate(TranslationKeys.BOND_COMPARISON_LOADING)))

        FxUtils.launchBackgroundThenUI(
            { comparisonService.comparison(request) to comparisonService.monthly(request) },
            { (lines, months) ->
                if (sequence != loadSequence) return@launchBackgroundThenUI

                comparison = lines
                monthly = months
                render()
            },
        )
    }

    private fun colorOf(
        bond: Bond,
        all: List<Bond>,
    ): Int = all.indexOfFirst { it.id == bond.id }.coerceAtLeast(0) % COLORS

    private fun render() {
        val data = comparison ?: return
        val months = monthly ?: return
        val all = (data.lines.map { it.bond } + data.excluded).sortedBy { it.name }

        renderLegend(data, all)
        renderNote(data)
        renderLines(data, all)
        renderBars(months, all)
    }

    private fun renderNote(data: BondComparisonDTO) {
        val parts = mutableListOf<String>()
        if (data.excluded.isNotEmpty() && data.lines.isNotEmpty()) {
            parts.add(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.BOND_COMPARISON_EXCLUDED),
                    data.excluded.size,
                ),
            )
        }
        if (taxFilter.value) parts.add(preferencesService.translate(TranslationKeys.BOND_COMPARISON_NET_NOTE))

        note.text = parts.joinToString(" ")
    }

    private fun renderLegend(
        data: BondComparisonDTO,
        all: List<Bond>,
    ) {
        legend.children.clear()

        data.lines.forEach { line ->
            legend.children.add(
                legendEntry(
                    "bond-legend-${colorOf(line.bond, all)}",
                    "${line.bond.name} ${formatSigned(line.resultPct)}",
                ),
            )
        }

        if (data.cdi.isNotEmpty()) {
            legend.children.add(
                legendEntry(
                    "bond-legend-cdi",
                    "${preferencesService.translate(TranslationKeys.BOND_COMPARISON_CDI)} " +
                        formatSigned(data.cdi.last().pct),
                ),
            )
        }
    }

    private fun legendEntry(
        dotStyleClass: String,
        text: String,
    ): HBox =
        HBox(
            LEGEND_GAP,
            Label("●").apply { styleClass.add(dotStyleClass) },
            Label(text),
        ).apply { alignment = Pos.CENTER_LEFT }

    private fun renderLines(
        data: BondComparisonDTO,
        all: List<Bond>,
    ) {
        val visible = data.lines

        if (data.lines.isEmpty()) {
            chartHolder.children.setAll(emptyState(data.excluded.isEmpty()))
            return
        }

        val days = (visible.flatMap { it.points } + data.cdi).map { it.date.toEpochDay() }
        val first = days.minOrNull()?.toDouble() ?: 0.0
        val last = days.maxOrNull()?.toDouble() ?: first + 1
        val xAxis =
            NumberAxis(first, last, ((last - first) / TICKS).coerceAtLeast(1.0)).apply {
                isMinorTickVisible = false
                tickLabelFormatter =
                    object : StringConverter<Number>() {
                        override fun toString(value: Number): String =
                            LocalDate.ofEpochDay(value.toLong()).format(DAY_FORMAT)

                        override fun fromString(string: String): Number = 0
                    }
            }
        val yAxis =
            NumberAxis().apply {
                isForceZeroInRange = true
                tickLabelFormatter =
                    object : StringConverter<Number>() {
                        override fun toString(value: Number): String =
                            UIUtils.formatPercentageForFundamentalAnalysis(value)

                        override fun fromString(string: String): Number = 0
                    }
            }

        val chart = LineChart(xAxis, yAxis)
        val classes = mutableListOf<String>()
        val crosshair = mutableListOf<CrosshairSeries>()

        visible.forEach { line ->
            val color = colorOf(line.bond, all)
            chart.data.add(seriesOf(line.points.map { it.date to it.pct }))
            classes.add("bond-line-$color")
            crosshair.add(crosshairOf(line.bond.name, "crosshair-bond-$color", line.points.map { it.date to it.pct }))
        }
        if (data.cdi.isNotEmpty()) {
            chart.data.add(seriesOf(data.cdi.map { it.date to it.pct }))
            classes.add("bond-cdi")
            crosshair.add(
                crosshairOf(
                    preferencesService.translate(TranslationKeys.BOND_COMPARISON_CDI),
                    "crosshair-series-cdi",
                    data.cdi.map { it.date to it.pct },
                ),
            )
        }

        chart.isLegendVisible = false
        chart.animated = false
        chart.createSymbols = false
        UIUtils.applyDefaultChartStyle(chart, CHART_STYLE)
        addStylesheet(chart)

        Platform.runLater {
            chart.data.forEachIndexed { index, series -> series.node?.styleClass?.add(classes[index]) }
        }

        chartHolder.children.setAll(
            if (crosshair.isEmpty()) Label() else ChartCrosshair.wrap(chart, CHART_STYLE, crosshair),
        )
    }

    private fun renderBars(
        data: BondMonthlyDTO,
        all: List<Bond>,
    ) {
        val visible = data.series

        if (visible.isEmpty()) {
            barsHolder.children.setAll(
                Label(preferencesService.translate(TranslationKeys.BOND_COMPARISON_MONTHLY_EMPTY)),
            )
            return
        }

        val yAxis = NumberAxis().apply { tickLabelFormatter = moneyFormatter() }
        val chart = BarChart(CategoryAxis(), yAxis)
        val colors = mutableListOf<Int>()

        visible.forEach { series ->
            colors.add(colorOf(series.bond, all))
            chart.data.add(
                XYChart.Series<String, Number>().apply {
                    name = series.bond.name
                    data.months.forEach { month ->
                        val value = series.values[month] ?: BigDecimal.ZERO
                        this.data.add(XYChart.Data(month.format(MONTH_FORMAT), value))
                    }
                },
            )
        }

        chart.isLegendVisible = false
        chart.animated = false
        UIUtils.applyDefaultChartStyle(chart, BARS_STYLE)
        addStylesheet(chart)

        Platform.runLater {
            chart.data.forEachIndexed { index, series ->
                series.data.forEach { point ->
                    point.node?.styleClass?.add("bond-bar-${colors[index]}")
                    val value = UIUtils.formatCurrency(BigDecimal(point.yValue.toDouble()))
                    UIUtils.addTooltipToXYChartNode(point.node, "${series.name}\n${point.xValue}: $value")
                }
            }
        }

        barsHolder.children.setAll(chart)
    }

    private fun emptyState(noBonds: Boolean) =
        VBox(
            10.0,
            Label(
                preferencesService.translate(
                    if (noBonds) TranslationKeys.BOND_COMPARISON_EMPTY else TranslationKeys.BOND_COMPARISON_NO_DATA,
                ),
            ),
        ).apply {
            alignment = Pos.CENTER
            if (noBonds) {
                children.add(
                    Button(preferencesService.translate(TranslationKeys.BOND_COMPARISON_EMPTY_BUTTON)).apply {
                        setOnAction { onRegisterBond() }
                    },
                )
            }
        }

    private fun seriesOf(points: List<Pair<LocalDate, BigDecimal>>): XYChart.Series<Number, Number> =
        XYChart.Series<Number, Number>().apply {
            points.forEach { data.add(XYChart.Data(it.first.toEpochDay(), it.second)) }
        }

    private fun crosshairOf(
        name: String,
        styleClass: String,
        points: List<Pair<LocalDate, BigDecimal>>,
    ) = CrosshairSeries(name, styleClass, points.map { it.first to it.second.toDouble() }) { formatSigned(it) }

    private fun addStylesheet(chart: javafx.scene.chart.Chart) {
        chart.stylesheets.add(
            BondComparisonView::class.java.getResource(Files.BOND_COMPARISON_STYLE_SHEET)?.toExternalForm()
                ?: error("Bond comparison stylesheet not found"),
        )
    }

    private fun moneyFormatter() =
        object : StringConverter<Number>() {
            override fun toString(value: Number): String = UIUtils.formatCurrency(BigDecimal(value.toDouble()))

            override fun fromString(string: String): Number = 0
        }

    private fun formatSigned(pct: BigDecimal): String = formatSigned(pct.toDouble())

    private fun formatSigned(pct: Double): String {
        val sign =
            when {
                pct > 0 -> "+"
                pct < 0 -> "−"
                else -> ""
            }

        return sign + UIUtils.formatPercentageForFundamentalAnalysis(kotlin.math.abs(pct))
    }

    companion object {
        private const val SECTION_STYLE = "-fx-font-weight: bold; -fx-font-size: 15px;"
        private const val CHART_STYLE = "bond-comparison-chart"
        private const val BARS_STYLE = "bond-monthly-bars"
        private const val CHART_HEIGHT = 380.0
        private const val BARS_HEIGHT = 380.0
        private const val MIN_CHART_HEIGHT = 200.0
        private const val TICKS = 6
        private const val LEGEND_GAP = 5.0
        private const val NAV_HEIGHT = 50.0
        private const val NAV_GAP = 60.0
        private const val COLORS = 24
        private val DAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yy")
        private val MONTH_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy")
    }
}
