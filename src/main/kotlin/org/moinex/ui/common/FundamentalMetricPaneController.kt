/*
 * Filename: FundamentalMetricPaneController.kt (original filename: FundamentalMetricPaneController.java)
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 19/03/2026
 */

package org.moinex.ui.common

import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Text
import org.json.JSONObject
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.service.PreferencesService
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalDateTime

@Controller
@Scope("prototype")
class FundamentalMetricPaneController(
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var rootVBox: VBox

    @FXML
    private lateinit var metricNameText: Text

    @FXML
    private lateinit var metricValueLabel: Label

    @FXML
    private lateinit var metadataBox: HBox

    companion object {
        private val THRESHOLD_DECREASE_FONT_SIZE = BigDecimal("1E12")
        private const val LARGE_VALUE_FONT_SIZE = "-fx-font-size: 20px;"
        private const val NORMAL_FONT_SIZE = "-fx-font-size: 22px;"
        private const val METRIC_METADATA_LABEL_STYLE = "metric-metadata-label"
        private const val VALUE_FIELD = "value"
        private const val TYPE_FIELD = "type"
        private const val PERCENT_TYPE = "percent"
        private const val CURRENCY_TYPE = "currency"
        private const val REFERENCE_DATE_FIELD = "reference_date"
        private const val DATA_TEMPORALITY_FIELD = "data_temporality"
        private const val REAL_TIME = "real_time"
    }

    fun getRoot(): Node = rootVBox

    fun updateMetricPane(
        metricName: String,
        metricData: Any?,
        lastUpdateDate: String?,
    ) {
        metricNameText.text = metricName

        when (metricData) {
            is JSONObject -> {
                metricValueLabel.text = formatMetricValue(metricData)
                metadataBox.children.clear()

                val dateToShow = determineDateToShow(metricData, lastUpdateDate)
                dateToShow?.let { addDateLabel(it) }
            }
            else -> {
                metricValueLabel.text = UIUtils.getOrDefault(metricData, Constants.NA_DATA).toString()
                metadataBox.children.clear()
            }
        }

        updateFontSize(extractNumericValue(metricData))
    }

    private fun determineDateToShow(
        metric: JSONObject,
        lastUpdateDate: String?,
    ): String? =
        when {
            metric.has(REFERENCE_DATE_FIELD) -> metric.getString(REFERENCE_DATE_FIELD)
            metric.has(DATA_TEMPORALITY_FIELD) &&
                metric.getString(DATA_TEMPORALITY_FIELD) == REAL_TIME &&
                lastUpdateDate != null -> lastUpdateDate
            else -> null
        }

    private fun addDateLabel(dateStr: String) {
        val dateLabel =
            Label(
                "${preferencesService.translate(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_REFERENCE_DATE,
                )}: ${formatDate(dateStr)}",
            ).apply {
                styleClass.add(METRIC_METADATA_LABEL_STYLE)
            }
        metadataBox.children.add(dateLabel)
    }

    private fun formatMetricValue(metric: JSONObject): String {
        val value = metric.opt(VALUE_FIELD)
        val type = metric.optString(TYPE_FIELD, "number")

        if (value == null || value.toString() == "null") {
            return Constants.NA_DATA
        }

        return runCatching {
            val numValue = BigDecimal(value.toString())
            when (type) {
                PERCENT_TYPE -> UIUtils.formatPercentageForFundamentalAnalysis(numValue)
                CURRENCY_TYPE -> UIUtils.formatCurrency(numValue)
                else -> UIUtils.formatNumWithDecimalPlaces(numValue, 2)
            }
        }.getOrElse {
            UIUtils.getOrDefault(value, Constants.NA_DATA).toString()
        }
    }

    private fun formatDate(dateStr: String): String =
        runCatching {
            val date = LocalDateTime.parse(dateStr)
            UIUtils.formatDateForDisplay(date)
        }.getOrDefault(dateStr)

    private fun updateFontSize(value: BigDecimal) {
        metricValueLabel.style =
            if (value.abs() > THRESHOLD_DECREASE_FONT_SIZE) {
                LARGE_VALUE_FONT_SIZE
            } else {
                NORMAL_FONT_SIZE
            }
    }

    private fun extractNumericValue(metricData: Any?): BigDecimal =
        when (metricData) {
            null -> BigDecimal.ZERO
            is JSONObject -> {
                val value = metricData.opt(VALUE_FIELD)
                when {
                    value == null || value == JSONObject.NULL -> BigDecimal.ZERO
                    else ->
                        runCatching {
                            BigDecimal(value.toString())
                        }.getOrDefault(BigDecimal.ZERO)
                }
            }
            is Number -> BigDecimal(metricData.toString())
            else -> BigDecimal.ZERO
        }
}
