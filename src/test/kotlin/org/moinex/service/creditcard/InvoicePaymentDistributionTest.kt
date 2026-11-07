package org.moinex.service.creditcard

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class InvoicePaymentDistributionTest :
    BehaviorSpec({
        fun values(vararg text: String) = text.map(::BigDecimal)

        fun List<BigDecimal>.sum() = fold(BigDecimal.ZERO, BigDecimal::add)

        fun BigDecimal.eq(text: String) = compareTo(BigDecimal(text)) == 0

        Given("installments owing R$ 1.000, R$ 1 and R$ 1 and a payment of R$ 1.001") {
            val shares = InvoicePaymentDistribution.distribute(values("1000", "1", "1"), BigDecimal("1001"))

            Then("no installment receives more than it owes and the shares add up to R$ 1.001") {
                shares.forEachIndexed { index, share ->
                    (share <= values("1000", "1", "1")[index]) shouldBe true
                }
                shares.sum().eq("1001") shouldBe true
            }

            Then("the shares follow what each one owes, to the cent: R$ 999.00, R$ 1.00 and R$ 1.00") {
                shares[0].eq("999.00") shouldBe true
                shares[1].eq("1.00") shouldBe true
                shares[2].eq("1.00") shouldBe true
            }
        }

        Given("three equal installments of R$ 100 and a payment of R$ 100") {
            val shares = InvoicePaymentDistribution.distribute(values("100", "100", "100"), BigDecimal("100"))

            Then("the cent that does not divide goes to the first installment and the total is exact") {
                shares.sum().eq("100") shouldBe true
                shares.map { it.setScale(2).toPlainString() } shouldBe listOf("33.34", "33.33", "33.33")
            }
        }

        Given("a payment equal to everything owed") {
            val shares = InvoicePaymentDistribution.distribute(values("40.10", "59.90"), BigDecimal("100.00"))

            Then("each installment receives exactly what it owes") {
                shares[0].eq("40.10") shouldBe true
                shares[1].eq("59.90") shouldBe true
            }
        }

        Given("a payment larger than what is owed") {
            val shares = InvoicePaymentDistribution.distribute(values("10", "20"), BigDecimal("500"))

            Then("it is limited to what is owed") {
                shares.sum().eq("30") shouldBe true
            }
        }

        Given("a rebate of R$ 10 over installments owing R$ 70 and R$ 30") {
            val shares = InvoicePaymentDistribution.distribute(values("70", "30"), BigDecimal("10"))

            Then("it is shared in proportion: R$ 7 and R$ 3") {
                shares[0].eq("7.00") shouldBe true
                shares[1].eq("3.00") shouldBe true
            }
        }

        Given("nothing owed or nothing paid") {
            Then("every share is zero") {
                InvoicePaymentDistribution.distribute(emptyList(), BigDecimal("10")) shouldBe emptyList()
                InvoicePaymentDistribution.distribute(values("10"), BigDecimal.ZERO).sum().signum() shouldBe 0
            }
        }
    })
