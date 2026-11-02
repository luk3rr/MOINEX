package org.moinex.ui.common

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.moinex.common.constant.TranslationKeys
import org.moinex.model.dto.QualityDimensionType
import org.moinex.model.dto.QualityLabel
import org.moinex.model.dto.ValuationLevel
import java.util.Properties

class FundamentalInsightTextTest :
    BehaviorSpec({
        fun bundle(name: String): Properties =
            Properties().apply {
                FundamentalInsightTextTest::class.java
                    .getResourceAsStream("/i18n/$name.properties")!!
                    .reader(Charsets.UTF_8)
                    .use { load(it) }
            }

        val keys =
            ValuationLevel.entries.map { FundamentalInsightText.valuationKey(it) } +
                QualityLabel.entries.map { FundamentalInsightText.qualityKey(it) } +
                QualityDimensionType.entries.map { FundamentalInsightText.dimensionKey(it) } +
                QualityDimensionType.entries.map { FundamentalInsightText.dimensionTooltipKey(it) } +
                listOf(
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_TITLE,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_EPS_FROM_STATEMENT,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_VALUATION_TOOLTIP,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_TITLE,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_UNAVAILABLE,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_INSIGHTS_QUALITY_TOOLTIP,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_SECTOR_MEDIAN,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_SECTOR_FEW_PEERS,
                    TranslationKeys.FUNDAMENTAL_ANALYSIS_SECTOR_UNKNOWN,
                )

        Given("the texts of the valuation and quality summary") {
            Then("each valuation level and quality label has its own key and style") {
                ValuationLevel.entries
                    .map { FundamentalInsightText.valuationKey(it) }
                    .toSet()
                    .size shouldBe
                    ValuationLevel.entries.size
                ValuationLevel.entries
                    .map { FundamentalInsightText.valuationStyleClass(it) }
                    .toSet()
                    .size shouldBe
                    ValuationLevel.entries.size
                QualityLabel.entries
                    .map { FundamentalInsightText.qualityKey(it) }
                    .toSet()
                    .size shouldBe
                    QualityLabel.entries.size
                QualityLabel.entries
                    .map { FundamentalInsightText.qualityStyleClass(it) }
                    .toSet()
                    .size shouldBe
                    QualityLabel.entries.size
            }

            listOf("messages_pt_BR", "messages_en").forEach { name ->
                Then("the $name bundle has a text for every key") {
                    val properties = bundle(name)
                    properties.stringPropertyNames() shouldContainAll keys
                    keys.forEach { properties.getProperty(it).shouldNotBeBlank() }
                }
            }
        }
    })
