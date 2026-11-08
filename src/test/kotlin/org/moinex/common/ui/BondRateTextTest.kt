package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import java.math.BigDecimal

class BondRateTextTest :
    BehaviorSpec({
        fun text(
            type: InterestType?,
            index: InterestIndex?,
            rate: String?,
        ) = BondRateText.format(type, index, rate?.let(::BigDecimal), "{0}% of {1}", "{1} + {0}% p.y.", "{0}% p.y.")

        Given("the contracted rate of a bond") {
            Then("a floating bond is a percentage of the index") {
                text(InterestType.FLOATING, InterestIndex.CDI, "120.00") shouldBe "120% of CDI"
            }

            Then("a floating bond with a spread is the index plus a yearly rate") {
                text(InterestType.FLOATING_SPREAD, InterestIndex.IPCA, "6.50") shouldBe "IPCA + 6.5% p.y."
            }

            Then("a fixed bond is a yearly rate") {
                text(InterestType.FIXED, InterestIndex.CDI, "11.00") shouldBe "11% p.y."
            }

            Then("a bond without type or rate has no text") {
                text(null, null, "10") shouldBe "-"
                text(InterestType.FIXED, null, null) shouldBe "-"
            }
        }
    })
