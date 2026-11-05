/*
 * Filename: CreditCardManagementOperationsTest.kt
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
import org.moinex.ai.operation.creditcard.AddCreditCardCreditOperation
import org.moinex.ai.operation.creditcard.ArchiveCreditCardOperation
import org.moinex.ai.operation.creditcard.CreateCreditCardOperation
import org.moinex.ai.operation.creditcard.UnarchiveCreditCardOperation
import org.moinex.ai.operation.creditcard.UpdateCreditCardOperation
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardCredit
import org.moinex.model.creditcard.CreditCardOperator
import org.moinex.model.enums.CreditCardCreditType
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal

class CreditCardManagementOperationsTest :
    BehaviorSpec({
        val creditCardService = mockk<CreditCardService>(relaxed = true)
        val walletService = mockk<WalletService>()
        val visa = CreditCardOperator(id = 1, name = "Visa")
        val nubank = WalletFactory.create(id = 10, name = "Nubank")
        val stored = CreditCardFactory.create(id = 20, name = "Main Card")
        val archived = CreditCardFactory.create(id = 21, name = "Old Card")
        val catalog = EntityCatalog(wallets = listOf(nubank), creditCards = listOf(stored))

        every { creditCardService.getAllCreditCardOperatorsOrderedByName() } returns listOf(visa)
        every { creditCardService.getAllArchivedCreditCards() } returns listOf(archived)
        every { creditCardService.getAllNonArchivedCreditCardsOrderedByName() } returns listOf(stored)
        every { walletService.getWalletById(10) } returns nubank

        Given("a request to create a card") {
            val operation = CreateCreditCardOperation(creditCardService, walletService)
            val payload =
                operation.fromModelArguments(
                    JSONObject(
                        """{"name":"Travel","operator":"visa","closingDay":5,"dueDay":12,"maxDebt":3000,
                            "billingWalletName":"nubank","lastFourDigits":"1234"}""",
                    ),
                    catalog,
                )

            Then("the operator and wallet are resolved and the card is created through the service") {
                payload.operatorName shouldBe "Visa"
                payload.billingWalletId shouldBe 10
                operation.validate(payload).isValid shouldBe true

                val saved = slot<CreditCard>()
                every { creditCardService.createCreditCard(capture(saved)) } returns 30
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.closingDay shouldBe 5
                saved.captured.defaultBillingWallet?.id shouldBe 10
            }

            Then("an unknown operator, a bad day or bad digits are refused") {
                operation.validate(payload.copy(operatorName = "Nope")).isValid shouldBe false
                operation.validate(payload.copy(closingDay = 40)).isValid shouldBe false
                operation.validate(payload.copy(lastFourDigits = "12")).isValid shouldBe false
            }
        }

        Given("a request that changes only the limit of a stored card") {
            val operation = UpdateCreditCardOperation(creditCardService, walletService)
            val payload =
                operation.fromModelArguments(
                    JSONObject("""{"creditCardName":"main card","maxDebt":9000}"""),
                    catalog,
                )

            Then("everything else keeps its stored value and the card shows before and after") {
                payload.cardId shouldBe 20
                payload.name shouldBe "Main Card"
                payload.closingDay shouldBe stored.closingDay
                payload.maxDebt shouldBe BigDecimal("9000")
                operation.preview(payload)!!.lines.size shouldBe 2
            }
        }

        Given("archive and unarchive requests") {
            val archive = ArchiveCreditCardOperation(creditCardService)
            val unarchive = UnarchiveCreditCardOperation(creditCardService)

            Then("each finds the card in its own list") {
                archive.fromModelArguments(JSONObject("""{"creditCardName":"main card"}"""), catalog).cardId shouldBe 20
                archive.fromModelArguments(JSONObject("""{"creditCardName":"old card"}"""), catalog).cardId shouldBe
                    null
                unarchive.fromModelArguments(JSONObject("""{"creditCardName":"old card"}"""), catalog).cardId shouldBe
                    21
            }
        }

        Given("a cashback credit") {
            val operation = AddCreditCardCreditOperation(creditCardService)
            val payload =
                operation.fromModelArguments(
                    JSONObject(
                        """{"creditCardName":"Main Card","date":"2026-10-01","amount":15,"creditType":"refund"}""",
                    ),
                    catalog,
                )

            Then("it is a refund credit written through addRebate") {
                payload.type shouldBe CreditCardCreditType.REFUND
                val saved = slot<CreditCardCredit>()
                every { creditCardService.addRebate(capture(saved)) } returns Unit
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.amount shouldBe BigDecimal("15.00")
            }
        }
    })
