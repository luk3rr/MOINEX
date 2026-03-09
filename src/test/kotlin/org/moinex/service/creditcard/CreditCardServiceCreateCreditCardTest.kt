package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.CreditCardOperatorFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.service.CreditCardService
import java.math.BigDecimal

class CreditCardServiceCreateCreditCardTest :
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

        Given("a valid credit card with all required fields") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard =
                CreditCardFactory.create(
                    name = "My Visa Card",
                    billingDueDay = 15,
                    closingDay = 5,
                    maxDebt = BigDecimal("10000.00"),
                    lastFourDigits = "1234",
                    operator = operator,
                    defaultBillingWallet = null,
                )
            val savedCreditCard =
                CreditCardFactory.create(
                    id = 1,
                    name = "My Visa Card",
                    billingDueDay = 15,
                    closingDay = 5,
                    maxDebt = BigDecimal("10000.00"),
                    lastFourDigits = "1234",
                    operator = operator,
                    defaultBillingWallet = null,
                )

            When("creating the credit card") {
                every { creditCardRepository.existsByName("My Visa Card") } returns false
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.save(any()) } returns savedCreditCard

                val result = service.createCreditCard(creditCard)

                Then("should return the created credit card id") {
                    result shouldBe 1
                }

                Then("should call repository save method") {
                    verify { creditCardRepository.save(any()) }
                }
            }
        }

        Given("a valid credit card without default billing wallet") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Mastercard")
            val creditCard =
                CreditCardFactory.create(
                    name = "My Mastercard",
                    billingDueDay = 20,
                    closingDay = 10,
                    maxDebt = BigDecimal("5000.00"),
                    lastFourDigits = "5678",
                    operator = operator,
                    defaultBillingWallet = null,
                )
            val savedCreditCard =
                CreditCardFactory.create(
                    id = 2,
                    name = "My Mastercard",
                    billingDueDay = 20,
                    closingDay = 10,
                    maxDebt = BigDecimal("5000.00"),
                    lastFourDigits = "5678",
                    operator = operator,
                    defaultBillingWallet = null,
                )

            When("creating the credit card") {
                every { creditCardRepository.existsByName("My Mastercard") } returns false
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.save(any()) } returns savedCreditCard

                val result = service.createCreditCard(creditCard)

                Then("should return the created credit card id") {
                    result shouldBe 2
                }

                Then("should not validate wallet existence") {
                    verify(exactly = 0) { walletRepository.existsById(any()) }
                }
            }
        }

        Given("a credit card with a name that already exists") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Amex")
            val creditCard =
                CreditCardFactory.create(
                    name = "Existing Card",
                    billingDueDay = 25,
                    closingDay = 15,
                    maxDebt = BigDecimal("8000.00"),
                    lastFourDigits = "9999",
                    operator = operator,
                    defaultBillingWallet = null,
                )

            When("creating the credit card") {
                every { creditCardRepository.existsByName("Existing Card") } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createCreditCard(creditCard)
                    }
                }
            }
        }

        Given("a credit card with a non-existent operator") {
            val operator = CreditCardOperatorFactory.create(id = 999, name = "Non-existent Operator")
            val creditCard =
                CreditCardFactory.create(
                    name = "Card with Invalid Operator",
                    billingDueDay = 10,
                    closingDay = 5,
                    maxDebt = BigDecimal("3000.00"),
                    lastFourDigits = "1111",
                    operator = operator,
                    defaultBillingWallet = null,
                )

            When("creating the credit card") {
                every { creditCardRepository.existsByName("Card with Invalid Operator") } returns false
                every { creditCardOperatorRepository.existsById(999) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createCreditCard(creditCard)
                    }
                }
            }
        }

        Given("a credit card with a non-existent default billing wallet") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val wallet = WalletFactory.create(id = 999, name = "Invalid Wallet")
            val creditCard =
                CreditCardFactory.create(
                    name = "Card with Invalid Wallet",
                    billingDueDay = 10,
                    closingDay = 5,
                    maxDebt = BigDecimal("3000.00"),
                    lastFourDigits = "2222",
                    operator = operator,
                    defaultBillingWallet = wallet,
                )

            When("creating the credit card") {
                every { creditCardRepository.existsByName("Card with Invalid Wallet") } returns false
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { walletRepository.existsById(999) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createCreditCard(creditCard)
                    }
                }
            }
        }

        Given("multiple valid credit cards") {
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")

            When("creating multiple credit cards sequentially") {
                val creditCard1 =
                    CreditCardFactory.create(
                        name = "Card 1",
                        billingDueDay = 10,
                        closingDay = 5,
                        maxDebt = BigDecimal("5000.00"),
                        lastFourDigits = "1111",
                        operator = operator,
                        defaultBillingWallet = WalletFactory.create(id = 1, name = "My Wallet"),
                    )
                val creditCard2 =
                    CreditCardFactory.create(
                        name = "Card 2",
                        billingDueDay = 15,
                        closingDay = 10,
                        maxDebt = BigDecimal("8000.00"),
                        lastFourDigits = "2222",
                        operator = operator,
                        defaultBillingWallet = null,
                    )

                val savedCard1 =
                    CreditCardFactory.create(
                        id = 1,
                        name = "Card 1",
                        billingDueDay = 10,
                        closingDay = 5,
                        maxDebt = BigDecimal("5000.00"),
                        lastFourDigits = "1111",
                        operator = operator,
                        defaultBillingWallet = null,
                    )
                val savedCard2 =
                    CreditCardFactory.create(
                        id = 2,
                        name = "Card 2",
                        billingDueDay = 15,
                        closingDay = 10,
                        maxDebt = BigDecimal("8000.00"),
                        lastFourDigits = "2222",
                        operator = operator,
                        defaultBillingWallet = null,
                    )

                every { creditCardRepository.existsByName("Card 1") } returns false
                every { creditCardRepository.existsByName("Card 2") } returns false
                every { walletRepository.existsById(creditCard1.defaultBillingWallet?.id!!) } returns true
                every { creditCardOperatorRepository.existsById(1) } returns true
                every { creditCardRepository.save(any()) } returnsMany
                    listOf(
                        savedCard1,
                        savedCard2,
                    )

                val result1 = service.createCreditCard(creditCard1)
                val result2 = service.createCreditCard(creditCard2)

                Then("should return different ids for each card") {
                    result1 shouldBe 1
                    result2 shouldBe 2
                    result1 shouldNotBe result2
                }

                Then("should call save twice") {
                    verify(exactly = 2) { creditCardRepository.save(any()) }
                }
            }
        }
    })
