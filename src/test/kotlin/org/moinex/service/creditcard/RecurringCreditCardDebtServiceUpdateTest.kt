package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import java.math.BigDecimal
import java.util.Optional

class RecurringCreditCardDebtServiceUpdateTest :
    BehaviorSpec({
        val recurringCreditCardDebtRepository = mockk<RecurringCreditCardDebtRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardService = mockk<CreditCardService>()
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferencesService = mockk<PreferencesService>(relaxed = true)

        val service =
            RecurringCreditCardDebtService(
                recurringCreditCardDebtRepository,
                creditCardDebtRepository,
                creditCardRepository,
                creditCardService,
                notificationService,
                preferencesService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("an existing recurring debt") {
            val creditCard = CreditCardFactory.create(id = 1)
            val existing = RecurringCreditCardDebtFactory.create(id = 1, creditCard = creditCard)

            When("updating with valid new values") {
                val newCategory = CategoryFactory.create(id = 2, name = "Streaming")
                val updated =
                    RecurringCreditCardDebtFactory.create(
                        id = 1,
                        creditCard = creditCard,
                        category = newCategory,
                        amount = BigDecimal("99.90"),
                        description = "Updated description",
                        dayOfMonth = 5,
                        frequency = CreditCardRecurringFrequency.YEARLY,
                    )

                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(existing)
                every { creditCardRepository.existsById(1) } returns true

                service.updateRecurring(updated)

                Then("should update mutable fields") {
                    existing.category shouldBe newCategory
                    existing.amount shouldBe BigDecimal("99.90")
                    existing.description shouldBe "Updated description"
                    existing.dayOfMonth shouldBe 5
                    existing.frequency shouldBe CreditCardRecurringFrequency.YEARLY
                }

                Then("should preserve startDate and nextInvoiceMonth") {
                    existing.startDate shouldBe RecurringCreditCardDebtFactory.create().startDate
                    existing.nextInvoiceMonth shouldBe RecurringCreditCardDebtFactory.create().nextInvoiceMonth
                }

                Then("should not call save explicitly (JPA dirty tracking)") {
                    verify(exactly = 0) { recurringCreditCardDebtRepository.save(any()) }
                }
            }
        }

        Given("a recurring debt that does not exist in the database") {
            val updated = RecurringCreditCardDebtFactory.create(id = 99)

            When("trying to update it") {
                every { recurringCreditCardDebtRepository.findById(99) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.updateRecurring(updated)
                    }
                }
            }
        }

        Given("a recurring debt update referencing a non-existent credit card") {
            val existingCard = CreditCardFactory.create(id = 1)
            val newCard = CreditCardFactory.create(id = 99)
            val existing = RecurringCreditCardDebtFactory.create(id = 1, creditCard = existingCard)
            val updated = RecurringCreditCardDebtFactory.create(id = 1, creditCard = newCard)

            When("trying to update it") {
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(existing)
                every { creditCardRepository.existsById(99) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateRecurring(updated)
                    }
                }
            }
        }
    })
