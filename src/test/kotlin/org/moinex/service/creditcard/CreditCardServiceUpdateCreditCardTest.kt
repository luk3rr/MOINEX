package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.CreditCardOperatorFactory
import org.moinex.factory.creditcard.CreditCardPaymentFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.service.creditcard.CreditCardService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class CreditCardServiceUpdateCreditCardTest :
    BehaviorSpec({
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardOperatorRepository = mockk<CreditCardOperatorRepository>()
        val walletRepository = mockk<WalletRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardPaymentRepository = mockk<CreditCardPaymentRepository>()
        val creditCardCreditRepository = mockk<CreditCardCreditRepository>()

        val service =
            CreditCardService(
                creditCardDebtRepository,
                creditCardPaymentRepository,
                creditCardRepository,
                creditCardOperatorRepository,
                walletRepository,
                creditCardCreditRepository,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a credit card with valid update data") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Old Card Name",
                    billingDueDay = 10,
                    closingDay = 5,
                    maxDebt = BigDecimal("5000.00"),
                    lastFourDigits = "1111",
                    operator = operator,
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "New Card Name",
                    billingDueDay = 15,
                    closingDay = 8,
                    maxDebt = BigDecimal("10000.00"),
                    lastFourDigits = "2222",
                    operator = operator,
                )

            When("updating the credit card") {
                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.existsByNameAndIdNot("New Card Name", 1) } returns false
                every { creditCardPaymentRepository.getAllPendingCreditCardPayments(1) } returns emptyList()

                service.updateCreditCard(updatedCreditCard)

                Then("should update all credit card fields") {
                    existingCreditCard.name shouldBe "New Card Name"
                    existingCreditCard.billingDueDay shouldBe 15
                    existingCreditCard.closingDay shouldBe 8
                    existingCreditCard.maxDebt shouldBe BigDecimal("10000.00")
                    existingCreditCard.lastFourDigits shouldBe "2222"
                }
            }
        }

        Given("a credit card with non-existent operator") {
            val operator = CreditCardOperatorFactory.create(id = 999, name = "Non-existent Operator")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    billingDueDay = 10,
                    closingDay = 5,
                    maxDebt = BigDecimal("5000.00"),
                    operator = CreditCardOperatorFactory.create(id = 1, name = "Visa"),
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    operator = operator,
                )

            When("updating the credit card") {
                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(999) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateCreditCard(updatedCreditCard)
                    }
                }
            }
        }

        Given("a credit card with a name that already exists for another card") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card 1",
                    operator = operator,
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Existing Card Name",
                    operator = operator,
                )

            When("updating the credit card") {
                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.existsByNameAndIdNot("Existing Card Name", 1) } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateCreditCard(updatedCreditCard)
                    }
                }
            }
        }

        Given("a credit card with future pending payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    billingDueDay = 10,
                    closingDay = 5,
                    operator = operator,
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    billingDueDay = 20,
                    closingDay = 5,
                    operator = operator,
                )

            When("updating billing due day") {
                val futureDate = LocalDateTime.now().plusMonths(1).withDayOfMonth(10)
                val debt = CreditCardDebtFactory.create(creditCard = existingCreditCard)
                val payment =
                    CreditCardPaymentFactory.create(
                        creditCardDebt = debt,
                        date = futureDate,
                    )

                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.existsByNameAndIdNot("Card Name", 1) } returns false
                every { creditCardPaymentRepository.getAllPendingCreditCardPayments(1) } returns
                    listOf(
                        payment,
                    )

                service.updateCreditCard(updatedCreditCard)

                Then("should update future payment dates to new billing due day") {
                    payment.date.dayOfMonth shouldBe 20
                }
            }
        }

        Given("a credit card with past pending payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    billingDueDay = 10,
                    closingDay = 5,
                    operator = operator,
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    billingDueDay = 20,
                    closingDay = 5,
                    operator = operator,
                )

            When("updating billing due day") {
                val pastDate = LocalDateTime.now().minusMonths(1).withDayOfMonth(10)
                val debt = CreditCardDebtFactory.create(creditCard = existingCreditCard)
                val payment =
                    CreditCardPaymentFactory.create(
                        creditCardDebt = debt,
                        date = pastDate,
                    )
                val originalDate = payment.date

                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.existsByNameAndIdNot("Card Name", 1) } returns false
                every { creditCardPaymentRepository.getAllPendingCreditCardPayments(1) } returns
                    listOf(
                        payment,
                    )

                service.updateCreditCard(updatedCreditCard)

                Then("should not update past payment dates") {
                    payment.date shouldBe originalDate
                }
            }
        }

        Given("a credit card that does not exist") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 999,
                    name = "Non-existent Card",
                    operator = operator,
                )

            When("updating the credit card") {
                every { creditCardRepository.findById(999) } throws
                    EntityNotFoundException("CreditCard with id 999 does not exist")

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.updateCreditCard(updatedCreditCard)
                    }
                }
            }
        }

        Given("a credit card with updated operator") {
            val oldOperator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val newOperator = CreditCardOperatorFactory.create(id = 2, name = "Mastercard")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    operator = oldOperator,
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    operator = newOperator,
                )

            When("updating the operator") {
                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(2) } returns true
                every { creditCardRepository.existsByNameAndIdNot("Card Name", 1) } returns false
                every { creditCardPaymentRepository.getAllPendingCreditCardPayments(1) } returns emptyList()

                service.updateCreditCard(updatedCreditCard)

                Then("should update the operator") {
                    existingCreditCard.operator shouldBe newOperator
                }
            }
        }

        Given("a credit card with updated max debt") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    maxDebt = BigDecimal("5000.00"),
                    operator = operator,
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    maxDebt = BigDecimal("15000.00"),
                    operator = operator,
                )

            When("updating the max debt") {
                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.existsByNameAndIdNot("Card Name", 1) } returns false
                every { creditCardPaymentRepository.getAllPendingCreditCardPayments(1) } returns emptyList()

                service.updateCreditCard(updatedCreditCard)

                Then("should update the max debt") {
                    existingCreditCard.maxDebt shouldBe BigDecimal("15000.00")
                }
            }
        }

        Given("a credit card with updated last four digits") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    lastFourDigits = "1111",
                    operator = operator,
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    lastFourDigits = "9999",
                    operator = operator,
                )

            When("updating the last four digits") {
                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.existsByNameAndIdNot("Card Name", 1) } returns false
                every { creditCardPaymentRepository.getAllPendingCreditCardPayments(1) } returns emptyList()

                service.updateCreditCard(updatedCreditCard)

                Then("should update the last four digits") {
                    existingCreditCard.lastFourDigits shouldBe "9999"
                }
            }
        }

        Given("a credit card with updated default billing wallet") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val wallet = WalletFactory.create(id = 1, name = "My Wallet")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    operator = operator,
                    defaultBillingWallet = null,
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    operator = operator,
                    defaultBillingWallet = wallet,
                )

            When("updating the default billing wallet") {
                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.existsByNameAndIdNot("Card Name", 1) } returns false
                every { creditCardPaymentRepository.getAllPendingCreditCardPayments(1) } returns emptyList()

                service.updateCreditCard(updatedCreditCard)

                Then("should update the default billing wallet") {
                    existingCreditCard.defaultBillingWallet shouldBe wallet
                }
            }
        }

        Given("a credit card with multiple future pending payments") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val existingCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    billingDueDay = 10,
                    closingDay = 5,
                    operator = operator,
                )
            val updatedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "Card Name",
                    billingDueDay = 25,
                    closingDay = 5,
                    operator = operator,
                )

            When("updating billing due day with multiple future payments") {
                val futureDate1 = LocalDateTime.now().plusMonths(1).withDayOfMonth(10)
                val futureDate2 = LocalDateTime.now().plusMonths(2).withDayOfMonth(10)

                val debt = CreditCardDebtFactory.create(creditCard = existingCreditCard)
                val payment1 =
                    CreditCardPaymentFactory.create(
                        creditCardDebt = debt,
                        date = futureDate1,
                    )
                val payment2 =
                    CreditCardPaymentFactory.create(
                        creditCardDebt = debt,
                        date = futureDate2,
                    )

                every { creditCardRepository.findById(1) } returns Optional.of(existingCreditCard)
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.existsByNameAndIdNot("Card Name", 1) } returns false
                every { creditCardPaymentRepository.getAllPendingCreditCardPayments(1) } returns
                    listOf(
                        payment1,
                        payment2,
                    )

                service.updateCreditCard(updatedCreditCard)

                Then("should update all future payment dates") {
                    payment1.date.dayOfMonth shouldBe 25
                    payment2.date.dayOfMonth shouldBe 25
                }
            }
        }
    })
