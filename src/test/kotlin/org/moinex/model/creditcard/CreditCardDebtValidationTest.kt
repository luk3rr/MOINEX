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

class CreditCardDebtValidationTest :
    BehaviorSpec({
        val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
        val creditCard = CreditCardFactory.create(operator = operator)
        val date = LocalDateTime.now()

        Given("a credit card debt with valid installments") {
            When("creating the debt") {
                val debt =
                    CreditCardDebt(
                        category = CategoryFactory.create(),
                        installments = 5,
                        creditCard = creditCard,
                        date = date,
                        amount = BigDecimal("500.00"),
                        description = "Valid debt",
                    )

                Then("should create successfully") {
                    debt.installments shouldBe 5
                    debt.amount shouldBe BigDecimal("500.00")
                }
            }
        }

        Given("a credit card debt with installments below minimum") {
            When("creating the debt") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardDebt(
                            category =
                                CategoryFactory
                                    .create(),
                            installments = 0,
                            creditCard = creditCard,
                            date = date,
                            amount = BigDecimal("500.00"),
                            description = "Invalid debt",
                        )
                    }
                }
            }
        }

        Given("a credit card debt with negative installments") {
            When("creating the debt") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardDebt(
                            category =
                                CategoryFactory
                                    .create(),
                            installments = -1,
                            creditCard = creditCard,
                            date = date,
                            amount = BigDecimal("500.00"),
                            description = "Invalid debt",
                        )
                    }
                }
            }
        }

        Given("a credit card debt with installments at minimum boundary") {
            When("creating the debt") {
                val debt =
                    CreditCardDebt(
                        category =
                            CategoryFactory
                                .create(),
                        installments = 1,
                        creditCard = creditCard,
                        date = date,
                        amount = BigDecimal("100.00"),
                        description = "Single installment debt",
                    )

                Then("should create successfully") {
                    debt.installments shouldBe 1
                }
            }
        }

        Given("a credit card debt with installments at maximum boundary") {
            When("creating the debt") {
                val debt =
                    CreditCardDebt(
                        category =
                            CategoryFactory
                                .create(),
                        installments = Constants.MAX_INSTALLMENTS,
                        creditCard = creditCard,
                        date = date,
                        amount = BigDecimal("1000.00"),
                        description = "Maximum installments debt",
                    )

                Then("should create successfully") {
                    debt.installments shouldBe Constants.MAX_INSTALLMENTS
                }
            }
        }

        Given("a credit card debt with installments above maximum") {
            When("creating the debt") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardDebt(
                            category =
                                CategoryFactory
                                    .create(),
                            installments = Constants.MAX_INSTALLMENTS + 1,
                            creditCard = creditCard,
                            date = date,
                            amount = BigDecimal("1000.00"),
                            description = "Invalid debt",
                        )
                    }
                }
            }
        }

        Given("a credit card debt with zero amount") {
            When("creating the debt") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardDebt(
                            category =
                                CategoryFactory
                                    .create(),
                            installments = 3,
                            creditCard = creditCard,
                            date = date,
                            amount = BigDecimal.ZERO,
                            description = "Invalid debt",
                        )
                    }
                }
            }
        }

        Given("a credit card debt with negative amount") {
            When("creating the debt") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardDebt(
                            category =
                                CategoryFactory
                                    .create(),
                            installments = 3,
                            creditCard = creditCard,
                            date = date,
                            amount = BigDecimal("-100.00"),
                            description = "Invalid debt",
                        )
                    }
                }
            }
        }

        Given("a credit card debt with positive amount") {
            When("creating the debt") {
                val debt =
                    CreditCardDebt(
                        category =
                            CategoryFactory
                                .create(),
                        installments = 2,
                        creditCard = creditCard,
                        date = date,
                        amount = BigDecimal("0.01"),
                        description = "Small debt",
                    )

                Then("should create successfully") {
                    debt.amount shouldBe BigDecimal("0.01")
                }
            }
        }

        Given("a credit card debt with null description") {
            When("creating the debt") {
                val debt =
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
                    debt.description shouldBe null
                }
            }
        }
    })
