package org.moinex.model.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.CreditCardOperatorFactory
import org.moinex.util.Constants
import java.math.BigDecimal

class CreditCardValidationTest :
    BehaviorSpec({
        val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")

        Given("a credit card with valid data") {
            When("creating the credit card") {
                val creditCard =
                    CreditCardFactory.create(
                        name = "My Valid Card",
                        billingDueDay = 15,
                        closingDay = 5,
                        maxDebt = BigDecimal("10000.00"),
                        lastFourDigits = "1234",
                        operator = operator,
                    )

                Then("should create successfully") {
                    creditCard.name shouldBe "My Valid Card"
                    creditCard.billingDueDay shouldBe 15
                    creditCard.closingDay shouldBe 5
                }
            }
        }

        Given("a credit card with blank name") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "   ",
                            billingDueDay = 15,
                            closingDay = 5,
                            maxDebt = BigDecimal("10000.00"),
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with empty name") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "",
                            billingDueDay = 15,
                            closingDay = 5,
                            maxDebt = BigDecimal("10000.00"),
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with name containing leading/trailing spaces") {
            When("creating the credit card") {
                val creditCard =
                    CreditCardFactory.create(
                        name = "  My Card  ",
                        billingDueDay = 15,
                        closingDay = 5,
                        maxDebt = BigDecimal("10000.00"),
                        operator = operator,
                    )

                Then("should trim the name") {
                    creditCard.name shouldBe "My Card"
                }
            }
        }

        Given("a credit card with billing due day below minimum") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 0,
                            closingDay = 5,
                            maxDebt = BigDecimal("10000.00"),
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with billing due day above maximum") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = Constants.MAX_BILLING_DUE_DAY + 1,
                            closingDay = 5,
                            maxDebt = BigDecimal("10000.00"),
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with billing due day at minimum boundary") {
            When("creating the credit card") {
                val creditCard =
                    CreditCardFactory.create(
                        name = "Valid Card",
                        billingDueDay = 1,
                        closingDay = 5,
                        maxDebt = BigDecimal("10000.00"),
                        operator = operator,
                    )

                Then("should create successfully") {
                    creditCard.billingDueDay shouldBe 1
                }
            }
        }

        Given("a credit card with billing due day at maximum boundary") {
            When("creating the credit card") {
                val creditCard =
                    CreditCardFactory.create(
                        name = "Valid Card",
                        billingDueDay = Constants.MAX_BILLING_DUE_DAY,
                        closingDay = 5,
                        maxDebt = BigDecimal("10000.00"),
                        operator = operator,
                    )

                Then("should create successfully") {
                    creditCard.billingDueDay shouldBe Constants.MAX_BILLING_DUE_DAY
                }
            }
        }

        Given("a credit card with closing day below minimum") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 15,
                            closingDay = 0,
                            maxDebt = BigDecimal("10000.00"),
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with closing day above maximum") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 15,
                            closingDay = Constants.MAX_BILLING_DUE_DAY + 1,
                            maxDebt = BigDecimal("10000.00"),
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with closing day at minimum boundary") {
            When("creating the credit card") {
                val creditCard =
                    CreditCardFactory.create(
                        name = "Valid Card",
                        billingDueDay = 15,
                        closingDay = 1,
                        maxDebt = BigDecimal("10000.00"),
                        operator = operator,
                    )

                Then("should create successfully") {
                    creditCard.closingDay shouldBe 1
                }
            }
        }

        Given("a credit card with closing day at maximum boundary") {
            When("creating the credit card") {
                val creditCard =
                    CreditCardFactory.create(
                        name = "Valid Card",
                        billingDueDay = 15,
                        closingDay = Constants.MAX_BILLING_DUE_DAY,
                        maxDebt = BigDecimal("10000.00"),
                        operator = operator,
                    )

                Then("should create successfully") {
                    creditCard.closingDay shouldBe Constants.MAX_BILLING_DUE_DAY
                }
            }
        }

        Given("a credit card with zero max debt") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 15,
                            closingDay = 5,
                            maxDebt = BigDecimal.ZERO,
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with negative max debt") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 15,
                            closingDay = 5,
                            maxDebt = BigDecimal("-1000.00"),
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with positive max debt") {
            When("creating the credit card") {
                val creditCard =
                    CreditCardFactory.create(
                        name = "Valid Card",
                        billingDueDay = 15,
                        closingDay = 5,
                        maxDebt = BigDecimal("0.01"),
                        operator = operator,
                    )

                Then("should create successfully") {
                    creditCard.maxDebt shouldBe BigDecimal("0.01")
                }
            }
        }

        Given("a credit card with last four digits null") {
            When("creating the credit card") {
                val creditCard =
                    CreditCardFactory.create(
                        name = "Valid Card",
                        billingDueDay = 15,
                        closingDay = 5,
                        maxDebt = BigDecimal("10000.00"),
                        lastFourDigits = null,
                        operator = operator,
                    )

                Then("should create successfully") {
                    creditCard.lastFourDigits shouldBe null
                }
            }
        }

        Given("a credit card with valid last four digits") {
            When("creating the credit card") {
                val creditCard =
                    CreditCardFactory.create(
                        name = "Valid Card",
                        billingDueDay = 15,
                        closingDay = 5,
                        maxDebt = BigDecimal("10000.00"),
                        lastFourDigits = "1234",
                        operator = operator,
                    )

                Then("should create successfully") {
                    creditCard.lastFourDigits shouldBe "1234"
                }
            }
        }

        Given("a credit card with last four digits blank") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 15,
                            closingDay = 5,
                            maxDebt = BigDecimal("10000.00"),
                            lastFourDigits = "   ",
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with last four digits too short") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 15,
                            closingDay = 5,
                            maxDebt = BigDecimal("10000.00"),
                            lastFourDigits = "123",
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with last four digits too long") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 15,
                            closingDay = 5,
                            maxDebt = BigDecimal("10000.00"),
                            lastFourDigits = "12345",
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with last four digits non-numeric") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 15,
                            closingDay = 5,
                            maxDebt = BigDecimal("10000.00"),
                            lastFourDigits = "12ab",
                            operator = operator,
                        )
                    }
                }
            }
        }

        Given("a credit card with last four digits containing spaces") {
            When("creating the credit card") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardFactory.create(
                            name = "Invalid Card",
                            billingDueDay = 15,
                            closingDay = 5,
                            maxDebt = BigDecimal("10000.00"),
                            lastFourDigits = "12 34",
                            operator = operator,
                        )
                    }
                }
            }
        }
    })
