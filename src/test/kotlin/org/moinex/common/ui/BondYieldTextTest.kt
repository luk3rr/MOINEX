package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.util.Locale

class BondYieldTextTest :
    BehaviorSpec({
        val amount: (BigDecimal) -> String = { value ->
            (if (value.signum() > 0) "+ " else "") + "R$ " +
                value.toPlainString()
        }
        val brazil = Locale.forLanguageTag("pt-BR")
        val us = Locale.US

        Given("a gain of R$ 150,20 that is 3.76% of the invested") {
            Then("the text shows the amount and the signed percentage with the decimal mark of the locale") {
                BondYieldText.format(BigDecimal("150.20"), BigDecimal("3.76"), amount, brazil) shouldBe
                    "+ R$ 150.20 (+3,76%)"
                BondYieldText.format(BigDecimal("150.20"), BigDecimal("3.76"), amount, us) shouldBe
                    "+ R$ 150.20 (+3.76%)"
            }
        }

        Given("a loss of R$ 10,00 that is -1.00%") {
            Then("the percentage keeps its minus sign") {
                BondYieldText.format(BigDecimal("-10.00"), BigDecimal("-1.00"), amount, brazil) shouldBe
                    "R$ -10.00 (-1,00%)"
            }
        }

        Given("a bond with nothing invested") {
            Then("only the amount is shown") {
                BondYieldText.format(BigDecimal.ZERO, null, amount, brazil) shouldBe "R$ 0"
            }
        }
    })
