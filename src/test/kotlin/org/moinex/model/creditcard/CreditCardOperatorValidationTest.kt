package org.moinex.model.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class CreditCardOperatorValidationTest :
    BehaviorSpec({
        Given("a credit card operator with valid name") {
            When("creating the operator") {
                val operator = CreditCardOperator(id = 1, name = "Visa")

                Then("should create successfully") {
                    operator.name shouldBe "Visa"
                    operator.id shouldBe 1
                }
            }
        }

        Given("a credit card operator with empty name") {
            When("creating the operator") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardOperator(id = 1, name = "")
                    }
                }
            }
        }

        Given("a credit card operator with blank name") {
            When("creating the operator") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardOperator(id = 1, name = "   ")
                    }
                }
            }
        }

        Given("a credit card operator with name containing leading/trailing spaces") {
            When("creating the operator") {
                val operator = CreditCardOperator(id = 1, name = "  Mastercard  ")

                Then("should trim the name") {
                    operator.name shouldBe "Mastercard"
                }
            }
        }

        Given("a credit card operator with name containing only spaces") {
            When("creating the operator") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> {
                        CreditCardOperator(id = 1, name = "\t\n ")
                    }
                }
            }
        }

        Given("a credit card operator with valid icon") {
            When("creating the operator") {
                val operator = CreditCardOperator(id = 1, name = "Visa", icon = "visa-icon")

                Then("should create successfully") {
                    operator.icon shouldBe "visa-icon"
                }
            }
        }

        Given("a credit card operator with null icon") {
            When("creating the operator") {
                val operator = CreditCardOperator(id = 1, name = "Visa", icon = null)

                Then("should create successfully") {
                    operator.icon shouldBe null
                }
            }
        }
    })
