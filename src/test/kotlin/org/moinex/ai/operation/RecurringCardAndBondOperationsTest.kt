/*
 * Filename: RecurringCardAndBondOperationsTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.json.JSONObject
import org.moinex.ai.operation.creditcard.CreateRecurringCardDebtOperation
import org.moinex.ai.operation.creditcard.UpdateRecurringCardDebtOperation
import org.moinex.ai.operation.investments.CreateBondOperation
import org.moinex.ai.operation.investments.UpdateBondOperation
import org.moinex.factory.CategoryFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.RecurringCreditCardDebtFactory
import org.moinex.factory.investment.BondFactory
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.enums.BondType
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.model.investment.Bond
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.moinex.service.investment.BondService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class RecurringCardAndBondOperationsTest :
    BehaviorSpec({
        val creditCardService = mockk<CreditCardService>()
        val recurringService = mockk<RecurringCreditCardDebtService>()
        val bondService = mockk<BondService>()
        val resolver = mockk<CategoryResolver>()
        val card = CreditCardFactory.create(id = 20, name = "Main Card")
        val streaming = CategoryFactory.create(id = 5, name = "Streaming")
        val catalog = EntityCatalog(creditCards = listOf(card), categories = listOf(streaming))

        every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns listOf(card)
        every { resolver.resolveOrCreate(any()) } answers { CategoryFactory.create(id = 99, name = firstArg()) }

        Given("a request for a monthly charge on a card") {
            val operation = CreateRecurringCardDebtOperation(creditCardService, recurringService, resolver)
            val payload =
                operation.fromModelArguments(
                    JSONObject(
                        """{"creditCardName":"main card","categoryName":"streaming","amount":39.9,
                            "date":"2026-12-10","description":"Streaming","cardFrequency":"monthly"}""",
                    ),
                    catalog,
                )

            Then("the day defaults to the first charge, the names resolve and the first invoice is the start month") {
                payload.dayOfMonth shouldBe 10
                payload.categoryName shouldBe "Streaming"
                operation.validate(payload).isValid shouldBe true

                val saved = slot<RecurringCreditCardDebt>()
                every { recurringService.createRecurring(capture(saved)) } returns 3
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.nextInvoiceMonth shouldBe YearMonth.of(2026, 12)
                saved.captured.frequency shouldBe CreditCardRecurringFrequency.MONTHLY
            }

            Then("a day out of range, an unknown card or an end date too close are refused") {
                operation.validate(payload.copy(dayOfMonth = 45)).isValid shouldBe false
                operation.validate(payload.copy(cardId = null)).isValid shouldBe false
                operation.validate(payload.copy(endDate = LocalDate.of(2026, 12, 11))).isValid shouldBe false
            }
        }

        Given("a stored recurring charge and a request that changes only the amount") {
            val stored =
                RecurringCreditCardDebtFactory.create(
                    id = 3,
                    creditCard = card,
                    category = streaming,
                    amount = BigDecimal("30.00"),
                    startDate = LocalDate.of(2026, 1, 10),
                    nextInvoiceMonth = YearMonth.of(2026, 11),
                )
            every { recurringService.getAllRecurringDebts() } returns listOf(stored)
            val operation = UpdateRecurringCardDebtOperation(creditCardService, recurringService, resolver)
            val payload =
                operation.fromModelArguments(JSONObject("""{"cardRecurringId":3,"amount":45}"""), catalog)

            Then("the start date, status and next invoice are kept and the category is not recreated") {
                operation.categoryNeeded(payload) shouldBe null
                val saved = slot<RecurringCreditCardDebt>()
                every { recurringService.updateRecurring(capture(saved)) } returns Unit
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.amount shouldBe BigDecimal("45.00")
                saved.captured.startDate shouldBe LocalDate.of(2026, 1, 10)
                saved.captured.nextInvoiceMonth shouldBe YearMonth.of(2026, 11)
            }

            Then("an unknown id is an error for the model") {
                runCatching {
                    operation.fromModelArguments(JSONObject("""{"cardRecurringId":404}"""), catalog)
                }.isFailure shouldBe true
            }
        }

        Given("a request to register a bond") {
            val operation = CreateBondOperation(bondService)
            val bond = BondFactory.create(id = 40, name = "CDB Example", symbol = "CDBX")
            every { bondService.getAllNonArchivedBonds() } returns listOf(bond)
            every { bondService.getAllArchivedBonds() } returns emptyList()
            val payload =
                operation.fromModelArguments(
                    JSONObject(
                        """{"name":"CDB Banco Z","symbol":"cdbz","bondType":"cdb","issuer":"Banco Z",
                            "maturityDate":"2028-05-01","interestType":"floating","interestIndex":"cdi",
                            "interestRate":110}""",
                    ),
                    catalog,
                )

            Then("the enums are parsed and the bond is created through the service") {
                payload.bondType shouldBe BondType.CDB
                operation.validate(payload).isValid shouldBe true
                val saved = slot<Bond>()
                every { bondService.createBond(capture(saved)) } returns Unit
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.interestRate shouldBe BigDecimal("110")
                saved.captured.symbol shouldBe "CDBZ"
            }

            Then("an unknown type, a negative rate or a symbol already stored are refused") {
                operation.validate(payload.copy(bondType = null)).isValid shouldBe false
                operation.validate(payload.copy(interestRate = BigDecimal("-1"))).isValid shouldBe false
                operation.validate(payload.copy(symbol = "cdbx")).isValid shouldBe false
            }
        }

        Given("a stored bond and a request that changes only the maturity date") {
            val bond = BondFactory.create(id = 40, name = "CDB Example", symbol = "CDBX")
            val operation = UpdateBondOperation(bondService)
            val bondCatalog = EntityCatalog(bonds = listOf(bond))
            val payload =
                operation.fromModelArguments(
                    JSONObject("""{"assetName":"cdbx","maturityDate":"2030-01-01"}"""),
                    bondCatalog,
                )

            Then("everything else keeps its stored value") {
                payload.maturityDate shouldBe LocalDate.of(2030, 1, 1)
                payload.issuer shouldBe bond.issuer
                payload.interestRate shouldBe bond.interestRate
                val saved = slot<Bond>()
                every { bondService.updateBond(capture(saved)) } returns Unit
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.id shouldBe 40
                saved.captured.type shouldBe bond.type
            }
        }
    })
