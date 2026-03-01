package org.moinex.model.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.factory.CategoryFactory
import org.moinex.factory.CreditCardFactory
import org.moinex.factory.CreditCardOperatorFactory
import java.math.BigDecimal
import java.time.LocalDateTime

class CreditCardTransactionValidationTest :
    BehaviorSpec({
        val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
        val creditCard = CreditCardFactory.create(operator = operator)
        val date = LocalDateTime.now()

        Given("a credit card transaction with valid data") {
            When("creating the transaction") {
                val transaction =
                    CreditCardDebt(
                        category =
                            CategoryFactory
                                .create(),
                        installments = 1,
                        creditCard = creditCard,
                        date = date,
                        amount = BigDecimal("100.00"),
                        description = "Test transaction",
                    )

                Then("should create successfully") {
                    transaction.amount shouldBe BigDecimal("100.00")
                    transaction.description shouldBe "Test transaction"
                }
            }
        }

        Given("a credit card transaction with zero amount") {
            When("creating the transaction") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardDebt(
                            category =
                                CategoryFactory
                                    .create(),
                            installments = 1,
                            creditCard = creditCard,
                            date = date,
                            amount = BigDecimal.ZERO,
                            description = "Invalid transaction",
                        )
                    }
                }
            }
        }

        Given("a credit card transaction with negative amount") {
            When("creating the transaction") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardDebt(
                            category =
                                CategoryFactory
                                    .create(),
                            installments = 1,
                            creditCard = creditCard,
                            date = date,
                            amount = BigDecimal("-50.00"),
                            description = "Invalid transaction",
                        )
                    }
                }
            }
        }

        Given("a credit card transaction with positive amount") {
            When("creating the transaction") {
                val transaction =
                    CreditCardDebt(
                        category =
                            CategoryFactory
                                .create(),
                        installments = 1,
                        creditCard = creditCard,
                        date = date,
                        amount = BigDecimal("0.01"),
                        description = "Valid transaction",
                    )

                Then("should create successfully") {
                    transaction.amount shouldBe BigDecimal("0.01")
                }
            }
        }

        Given("a credit card transaction with null description") {
            When("creating the transaction") {
                val transaction =
                    CreditCardDebt(
                        category =
                            CategoryFactory
                                .create(),
                        installments = 1,
                        creditCard = creditCard,
                        date = date,
                        amount = BigDecimal("100.00"),
                        description = null,
                    )

                Then("should create successfully") {
                    transaction.description shouldBe null
                }
            }
        }

        Given("a credit card transaction with empty description") {
            When("creating the transaction") {
                val transaction =
                    CreditCardDebt(
                        category =
                            CategoryFactory
                                .create(),
                        installments = 1,
                        creditCard = creditCard,
                        date = date,
                        amount = BigDecimal("100.00"),
                        description = "",
                    )

                Then("should create successfully") {
                    transaction.description shouldBe ""
                }
            }
        }

        Given("a credit card transaction with large amount") {
            When("creating the transaction") {
                val transaction =
                    CreditCardDebt(
                        category =
                            CategoryFactory
                                .create(),
                        installments = 1,
                        creditCard = creditCard,
                        date = date,
                        amount = BigDecimal("999999.99"),
                        description = "Large transaction",
                    )

                Then("should create successfully") {
                    transaction.amount shouldBe BigDecimal("999999.99")
                }
            }
        }
    })
