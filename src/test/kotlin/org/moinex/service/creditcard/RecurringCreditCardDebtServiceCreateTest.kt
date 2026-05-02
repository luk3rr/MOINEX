package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.common.ClockProvider
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService

class RecurringCreditCardDebtServiceCreateTest :
    BehaviorSpec({
        val recurringCreditCardDebtRepository = mockk<RecurringCreditCardDebtRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardService = mockk<CreditCardService>()
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferencesService = mockk<PreferencesService>(relaxed = true)
        val clockProvider = ClockProvider()

        val service =
            RecurringCreditCardDebtService(
                recurringCreditCardDebtRepository,
                creditCardDebtRepository,
                creditCardRepository,
                creditCardService,
                notificationService,
                preferencesService,
                clockProvider,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid recurring debt with an existing credit card") {
            val creditCard = CreditCardFactory.create(id = 1)
            val recurring = RecurringCreditCardDebtFactory.create(id = null, creditCard = creditCard)

            When("creating the recurring debt") {
                every { creditCardRepository.existsById(1) } returns true
                every { recurringCreditCardDebtRepository.save(recurring) } returns
                    recurring.also { it.id = 1 }

                val id = service.createRecurring(recurring)

                Then("should persist the recurring debt") {
                    verify { recurringCreditCardDebtRepository.save(recurring) }
                }

                Then("should return the generated id") {
                    assert(id == 1)
                }
            }
        }

        Given("a recurring debt referencing a non-existent credit card") {
            val creditCard = CreditCardFactory.create(id = 99)
            val recurring = RecurringCreditCardDebtFactory.create(creditCard = creditCard)

            When("trying to create it") {
                every { creditCardRepository.existsById(99) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createRecurring(recurring)
                    }
                }

                Then("should not save anything") {
                    verify(exactly = 0) { recurringCreditCardDebtRepository.save(any()) }
                }
            }
        }
    })
