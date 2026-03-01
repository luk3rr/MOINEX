package org.moinex.model.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.factory.CategoryFactory
import org.moinex.factory.CreditCardFactory
import org.moinex.factory.CreditCardOperatorFactory
import org.moinex.util.Constants
import java.math.BigDecimal
import java.time.LocalDateTime

class CreditCardPaymentValidationTest :
    BehaviorSpec({
        val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
        val creditCard = CreditCardFactory.create(operator = operator)
        val date = LocalDateTime.now()
        val debt =
            CreditCardDebt(
                category = CategoryFactory.create(),
                installments = 3,
                creditCard = creditCard,
                date = date,
                amount = BigDecimal("300.00"),
                description = "Test debt",
            )

        Given("a credit card payment with valid data") {
            When("creating the payment") {
                val payment =
                    CreditCardPayment(
                        creditCardDebt = debt,
                        amount = BigDecimal("100.00"),
                        rebateUsed = BigDecimal.ZERO,
                        installment = 1,
                        date = date,
                    )

                Then("should create successfully") {
                    payment.amount shouldBe BigDecimal("100.00")
                    payment.rebateUsed shouldBe BigDecimal.ZERO
                    payment.installment shouldBe 1
                }
            }
        }

        Given("a credit card payment with zero amount") {
            When("creating the payment") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardPayment(
                            creditCardDebt = debt,
                            amount = BigDecimal.ZERO,
                            rebateUsed = BigDecimal.ZERO,
                            installment = 1,
                            date = date,
                        )
                    }
                }
            }
        }

        Given("a credit card payment with negative amount") {
            When("creating the payment") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardPayment(
                            creditCardDebt = debt,
                            amount = BigDecimal("-50.00"),
                            rebateUsed = BigDecimal.ZERO,
                            installment = 1,
                            date = date,
                        )
                    }
                }
            }
        }

        Given("a credit card payment with positive amount") {
            When("creating the payment") {
                val payment =
                    CreditCardPayment(
                        creditCardDebt = debt,
                        amount = BigDecimal("0.01"),
                        rebateUsed = BigDecimal.ZERO,
                        installment = 1,
                        date = date,
                    )

                Then("should create successfully") {
                    payment.amount shouldBe BigDecimal("0.01")
                }
            }
        }

        Given("a credit card payment with zero rebate used") {
            When("creating the payment") {
                val payment =
                    CreditCardPayment(
                        creditCardDebt = debt,
                        amount = BigDecimal("100.00"),
                        rebateUsed = BigDecimal.ZERO,
                        installment = 1,
                        date = date,
                    )

                Then("should create successfully") {
                    payment.rebateUsed shouldBe BigDecimal.ZERO
                }
            }
        }

        Given("a credit card payment with positive rebate used") {
            When("creating the payment") {
                val payment =
                    CreditCardPayment(
                        creditCardDebt = debt,
                        amount = BigDecimal("100.00"),
                        rebateUsed = BigDecimal("10.00"),
                        installment = 1,
                        date = date,
                    )

                Then("should create successfully") {
                    payment.rebateUsed shouldBe BigDecimal("10.00")
                }
            }
        }

        Given("a credit card payment with negative rebate used") {
            When("creating the payment") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardPayment(
                            creditCardDebt = debt,
                            amount = BigDecimal("100.00"),
                            rebateUsed = BigDecimal("-5.00"),
                            installment = 1,
                            date = date,
                        )
                    }
                }
            }
        }

        Given("a credit card payment with installment at minimum boundary") {
            When("creating the payment") {
                val payment =
                    CreditCardPayment(
                        creditCardDebt = debt,
                        amount = BigDecimal("100.00"),
                        rebateUsed = BigDecimal.ZERO,
                        installment = 1,
                        date = date,
                    )

                Then("should create successfully") {
                    payment.installment shouldBe 1
                }
            }
        }

        Given("a credit card payment with installment at maximum boundary") {
            When("creating the payment") {
                val payment =
                    CreditCardPayment(
                        creditCardDebt = debt,
                        amount = BigDecimal("100.00"),
                        rebateUsed = BigDecimal.ZERO,
                        installment = Constants.MAX_INSTALLMENTS,
                        date = date,
                    )

                Then("should create successfully") {
                    payment.installment shouldBe Constants.MAX_INSTALLMENTS
                }
            }
        }

        Given("a credit card payment with installment below minimum") {
            When("creating the payment") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardPayment(
                            creditCardDebt = debt,
                            amount = BigDecimal("100.00"),
                            rebateUsed = BigDecimal.ZERO,
                            installment = 0,
                            date = date,
                        )
                    }
                }
            }
        }

        Given("a credit card payment with installment above maximum") {
            When("creating the payment") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardPayment(
                            creditCardDebt = debt,
                            amount = BigDecimal("100.00"),
                            rebateUsed = BigDecimal.ZERO,
                            installment = Constants.MAX_INSTALLMENTS + 1,
                            date = date,
                        )
                    }
                }
            }
        }

        Given("a credit card payment with negative installment") {
            When("creating the payment") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardPayment(
                            creditCardDebt = debt,
                            amount = BigDecimal("100.00"),
                            rebateUsed = BigDecimal.ZERO,
                            installment = -1,
                            date = date,
                        )
                    }
                }
            }
        }

        Given("a credit card payment with null wallet") {
            When("creating the payment") {
                val payment =
                    CreditCardPayment(
                        creditCardDebt = debt,
                        amount = BigDecimal("100.00"),
                        rebateUsed = BigDecimal.ZERO,
                        installment = 1,
                        date = date,
                        wallet = null,
                    )

                Then("should create successfully") {
                    payment.wallet shouldBe null
                    payment.isPaid() shouldBe false
                }
            }
        }

        Given("a credit card payment with refunded flag false") {
            When("creating the payment") {
                val payment =
                    CreditCardPayment(
                        creditCardDebt = debt,
                        amount = BigDecimal("100.00"),
                        rebateUsed = BigDecimal.ZERO,
                        installment = 1,
                        date = date,
                        refunded = false,
                    )

                Then("should create successfully") {
                    payment.isRefunded() shouldBe false
                }
            }
        }

        Given("a credit card payment with refunded flag true") {
            When("creating the payment") {
                val payment =
                    CreditCardPayment(
                        creditCardDebt = debt,
                        amount = BigDecimal("100.00"),
                        rebateUsed = BigDecimal.ZERO,
                        installment = 1,
                        date = date,
                        refunded = true,
                    )

                Then("should create successfully") {
                    payment.isRefunded() shouldBe true
                }
            }
        }
    })
