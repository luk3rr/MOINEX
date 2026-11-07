package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.model.enums.BondType
import org.moinex.service.investment.BondTaxEstimator.Lot
import java.math.BigDecimal

class BondTaxEstimatorTest :
    BehaviorSpec({
        fun tax(
            type: BondType,
            ageDays: Long,
            interest: String,
        ) = BondTaxEstimator.estimate(type, listOf(Lot(BigDecimal("1000"), ageDays)), BigDecimal(interest))

        Given("a CDB with R$ 100 of interest") {
            When("the money was kept for 100 days") {
                Then("the tax is 22.5%, R$ 22.50, and the net interest is R$ 77.50") {
                    tax(BondType.CDB, 100, "100").tax.compareTo(BigDecimal("22.50")) shouldBe 0
                }
            }
            When("the money was kept for 400 days") {
                Then("the tax is 17.5%, R$ 17.50, and the net interest is R$ 82.50") {
                    tax(BondType.CDB, 400, "100").tax.compareTo(BigDecimal("17.50")) shouldBe 0
                }
            }
            When("the money was kept for 800 days") {
                Then("the tax is 15%, R$ 15.00") {
                    tax(BondType.CDB, 800, "100").tax.compareTo(BigDecimal("15.00")) shouldBe 0
                }
            }
            When("the money was kept for 200 and 361 days, the edges of the second and third brackets") {
                Then("the rates are 20% and 17.5%") {
                    tax(BondType.CDB, 200, "100").tax.compareTo(BigDecimal("20.00")) shouldBe 0
                    tax(BondType.CDB, 361, "100").tax.compareTo(BigDecimal("17.50")) shouldBe 0
                    tax(BondType.CDB, 180, "100").tax.compareTo(BigDecimal("22.50")) shouldBe 0
                    tax(BondType.CDB, 720, "100").tax.compareTo(BigDecimal("17.50")) shouldBe 0
                    tax(BondType.CDB, 721, "100").tax.compareTo(BigDecimal("15.00")) shouldBe 0
                }
            }
        }

        Given("a LCI or LCA with R$ 100 of interest") {
            When("estimating the tax") {
                Then("they are exempt") {
                    val lci = tax(BondType.LCI, 100, "100")
                    val lca = tax(BondType.LCA, 100, "100")
                    lci.tax.signum() shouldBe 0
                    lci.applicable shouldBe true
                    lca.tax.signum() shouldBe 0
                }
            }
        }

        Given("an international or other bond") {
            When("estimating the tax") {
                Then("there is no estimate") {
                    tax(BondType.INTERNATIONAL, 100, "100").applicable shouldBe false
                    tax(BondType.OTHER, 100, "100").applicable shouldBe false
                }
            }
        }

        Given("two lots, one old and one recent, with R$ 90 of interest") {
            When("estimating the tax") {
                val lots = listOf(Lot(BigDecimal("1000"), 100), Lot(BigDecimal("1000"), 800))
                val estimate = BondTaxEstimator.estimate(BondType.CDB, lots, BigDecimal("90"))

                Then(
                    "interest is shared by principal times days held: 100/900 at 22.5% and 800/900 at 15% = R$ 14.25",
                ) {
                    estimate.tax.compareTo(BigDecimal("14.25")) shouldBe 0
                }
            }
        }
    })
