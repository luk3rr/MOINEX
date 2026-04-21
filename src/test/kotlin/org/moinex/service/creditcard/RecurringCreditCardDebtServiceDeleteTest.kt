package org.moinex.service.creditcard

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import java.util.Optional

class RecurringCreditCardDebtServiceDeleteTest :
    BehaviorSpec({
        val recurringCreditCardDebtRepository = mockk<RecurringCreditCardDebtRepository>()
        val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
        val creditCardRepository = mockk<CreditCardRepository>()
        val creditCardService = mockk<CreditCardService>()

        val service =
            RecurringCreditCardDebtService(
                recurringCreditCardDebtRepository,
                creditCardDebtRepository,
                creditCardRepository,
                creditCardService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("an existing recurring debt with no materialized debts") {
            val recurring = RecurringCreditCardDebtFactory.create(id = 1)

            When("deleting it") {
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(recurring)
                every { creditCardDebtRepository.existsByRecurringSourceId(1) } returns false
                every { recurringCreditCardDebtRepository.delete(recurring) } returns Unit

                service.deleteRecurring(1)

                Then("should delete permanently") {
                    verify { recurringCreditCardDebtRepository.delete(recurring) }
                }
            }
        }

        Given("an existing recurring debt that has materialized debts") {
            val recurring = RecurringCreditCardDebtFactory.create(id = 1)

            When("trying to delete it") {
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(recurring)
                every { creditCardDebtRepository.existsByRecurringSourceId(1) } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.deleteRecurring(1)
                    }
                }

                Then("should not delete from repository") {
                    verify(exactly = 0) { recurringCreditCardDebtRepository.delete(any()) }
                }
            }
        }

        Given("a recurring debt that does not exist") {
            When("trying to delete it") {
                every { recurringCreditCardDebtRepository.findById(99) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.deleteRecurring(99)
                    }
                }
            }
        }

        Given("an active recurring debt") {
            val recurring =
                RecurringCreditCardDebtFactory.create(
                    id = 1,
                    status = RecurringTransactionStatus.ACTIVE,
                )

            When("deactivating it") {
                every { recurringCreditCardDebtRepository.findById(1) } returns Optional.of(recurring)

                service.deactivateRecurring(1)

                Then("should set status to INACTIVE") {
                    recurring.status shouldBe RecurringTransactionStatus.INACTIVE
                }
            }
        }
    })
