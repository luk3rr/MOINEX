/*
 * Filename: LancamentosOperationsTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.moinex.ai.operation.transactions.ArchiveCategoryOperation
import org.moinex.ai.operation.transactions.ArchiveWalletOperation
import org.moinex.ai.operation.transactions.CreateCategoryOperation
import org.moinex.ai.operation.transactions.CreateRecurringTransactionOperation
import org.moinex.ai.operation.transactions.CreateTransferOperation
import org.moinex.ai.operation.transactions.CreateWalletOperation
import org.moinex.ai.operation.transactions.UnarchiveWalletOperation
import org.moinex.ai.operation.transactions.UpdateCategoryOperation
import org.moinex.ai.operation.transactions.UpdateRecurringTransactionOperation
import org.moinex.ai.operation.transactions.UpdateTransferOperation
import org.moinex.ai.operation.transactions.UpdateWalletOperation
import org.moinex.ai.operation.transactions.UpdateWalletTransactionOperation
import org.moinex.factory.CategoryFactory
import org.moinex.factory.wallet.RecurringTransactionFactory
import org.moinex.factory.wallet.TransferFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.factory.wallet.WalletTypeFactory
import org.moinex.model.Category
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.service.CategoryService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class LancamentosOperationsTest :
    BehaviorSpec({
        val walletService = mockk<WalletService>(relaxed = true)
        val categoryService = mockk<CategoryService>(relaxed = true)
        val recurringService = mockk<RecurringTransactionService>(relaxed = true)
        val resolver = mockk<CategoryResolver>()

        val groceries = CategoryFactory.create(id = 1, name = "Groceries")
        val rent = CategoryFactory.create(id = 2, name = "Rent")
        val nubank = WalletFactory.create(id = 10, name = "Nubank")
        val inter = WalletFactory.create(id = 11, name = "Inter")
        val savings = WalletFactory.create(id = 12, name = "Old Savings", isArchived = true)
        val checking = WalletTypeFactory.create(5, "Checking")
        val catalog = EntityCatalog(wallets = listOf(nubank, inter), categories = listOf(groceries, rent))

        every { walletService.getWalletById(10) } returns nubank
        every { walletService.getWalletById(11) } returns inter
        every { walletService.getAllWalletTypes() } returns listOf(checking)
        every { walletService.getAllArchivedWallets() } returns listOf(savings)
        every { resolver.resolveOrCreate(any()) } answers
            { Category(id = 99, name = firstArg()) }

        Given("a recorded wallet transaction and a request that only changes the amount") {
            val stored =
                WalletTransactionFactory.create(
                    id = 7,
                    date = LocalDateTime.of(2026, 10, 3, 0, 0),
                    status = WalletTransactionStatus.CONFIRMED,
                    description = "Market",
                    wallet = nubank,
                    category = groceries,
                    amount = BigDecimal("50.00"),
                )
            every { walletService.getWalletTransactionById(7) } returns stored
            val operation = UpdateWalletTransactionOperation(walletService, resolver)

            val payload =
                operation.fromModelArguments(JSONObject("""{"transactionId":7,"amount":65.5}"""), catalog)

            Then("the other fields keep their stored value and the card shows before and after") {
                payload.description shouldBe "Market"
                payload.walletId shouldBe 10
                payload.categoryName shouldBe "Groceries"
                payload.amount shouldBe BigDecimal("65.5")
                operation.preview(payload)!!.lines shouldBe listOf("amount: 50.00 → 65.5")
            }

            Then("approving keeps the status and updates through the service") {
                val saved = slot<WalletTransaction>()
                every { walletService.updateWalletTransaction(capture(saved)) } returns Unit

                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()

                saved.captured.id shouldBe 7
                saved.captured.status shouldBe WalletTransactionStatus.CONFIRMED
                saved.captured.amount shouldBe BigDecimal("65.50")
            }

            Then("an unknown wallet name is a validation error, not a silent change") {
                val moved =
                    operation.fromModelArguments(JSONObject("""{"transactionId":7,"walletName":"Nowhere"}"""), catalog)
                operation.validate(moved).errors.size shouldBe 1
            }
        }

        Given("a transfer request") {
            val operation = CreateTransferOperation(walletService, resolver)

            When("origin and destination are existing wallets") {
                val payload =
                    operation.fromModelArguments(
                        JSONObject(
                            """{"date":"2026-11-01","amount":200,"fromWalletName":"nubank","toWalletName":"inter"}""",
                        ),
                        catalog,
                    )

                Then("it is valid, has no category and is written through createTransfer") {
                    payload.senderWalletId shouldBe 10
                    payload.receiverWalletId shouldBe 11
                    operation.validate(payload).isValid shouldBe true
                    operation.categoryNeeded(payload) shouldBe null

                    val saved = slot<Transfer>()
                    every { walletService.createTransfer(capture(saved)) } returns 5
                    operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                    saved.captured.category shouldBe null
                    saved.captured.amount shouldBe BigDecimal("200.00")
                }
            }

            When("origin and destination are the same wallet") {
                val payload =
                    operation.fromModelArguments(
                        JSONObject(
                            """{"date":"2026-11-01","amount":200,"fromWalletName":"nubank","toWalletName":"nubank"}""",
                        ),
                        catalog,
                    )

                Then("it is refused before reaching the service") {
                    operation.validate(payload).isValid shouldBe false
                }
            }

            When("the amount is missing") {
                val payload =
                    operation.fromModelArguments(
                        JSONObject("""{"date":"2026-11-01","amount":0,"fromWalletName":"nubank"}"""),
                        catalog,
                    )

                Then("the model is told to resend it") {
                    operation.missingFromModel(payload).size shouldBe 1
                }
            }
        }

        Given("a stored transfer and a request that changes the date only") {
            val stored =
                TransferFactory.create(
                    id = 3,
                    senderWallet = nubank,
                    receiverWallet = inter,
                    date = LocalDateTime.of(2026, 9, 1, 0, 0),
                    amount = BigDecimal("80.00"),
                    category = rent,
                )
            every { walletService.getAllTransfers() } returns listOf(stored)
            val operation = UpdateTransferOperation(walletService, resolver)

            val payload =
                operation.fromModelArguments(JSONObject("""{"transactionId":3,"date":"2026-09-02"}"""), catalog)

            Then("wallets, amount and category are kept") {
                payload.senderWalletId shouldBe 10
                payload.receiverWalletId shouldBe 11
                payload.amount shouldBe BigDecimal("80.00")
                payload.categoryName shouldBe "Rent"
                payload.date shouldBe LocalDate.of(2026, 9, 2)
            }

            Then("an unknown transfer id is an error for the model") {
                runCatching {
                    operation.fromModelArguments(JSONObject("""{"transactionId":404}"""), catalog)
                }.isFailure shouldBe true
            }
        }

        Given("a recurring transaction request") {
            val operation = CreateRecurringTransactionOperation(walletService, recurringService, resolver)
            val payload =
                operation.fromModelArguments(
                    JSONObject(
                        """{"date":"2026-12-05","description":"Rent","amount":1800,"walletName":"Inter",
                            "categoryName":"rent","movementType":"expense","frequency":"monthly"}""",
                    ),
                    catalog,
                )

            Then("it resolves names, defaults the end date and is written as an active recurrence") {
                payload.frequency shouldBe RecurringTransactionFrequency.MONTHLY
                payload.categoryName shouldBe "Rent"
                operation.validate(payload).isValid shouldBe true

                val saved = slot<RecurringTransaction>()
                every { recurringService.createRecurringTransaction(capture(saved)) } returns 8
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.nextDueDate shouldBe LocalDate.of(2026, 12, 5)
                saved.captured.type shouldBe WalletTransactionType.EXPENSE
            }

            Then("an end date before the start is invalid") {
                operation
                    .validate(payload.copy(endDate = LocalDate.of(2026, 1, 1)))
                    .isValid shouldBe false
            }
        }

        Given("a stored recurring transaction and a request that changes only the frequency") {
            val stored =
                RecurringTransactionFactory.create(
                    id = 4,
                    startDate = LocalDate.of(2026, 1, 5),
                    endDate = LocalDate.of(2027, 1, 5),
                    nextDueDate = LocalDate.of(2026, 12, 5),
                    wallet = nubank,
                    category = rent,
                    amount = BigDecimal("900.00"),
                )
            every { recurringService.getAllRecurringTransactions() } returns listOf(stored)
            val operation = UpdateRecurringTransactionOperation(walletService, recurringService, resolver)

            val payload =
                operation.fromModelArguments(JSONObject("""{"recurringId":4,"frequency":"yearly"}"""), catalog)

            Then("the rest is kept and the stored start and next due date are preserved on save") {
                payload.amount shouldBe BigDecimal("900.00")
                payload.walletId shouldBe 10
                payload.frequency shouldBe RecurringTransactionFrequency.YEARLY

                val saved = slot<RecurringTransaction>()
                every { recurringService.updateRecurringTransaction(capture(saved)) } returns Unit
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.startDate shouldBe LocalDate.of(2026, 1, 5)
                saved.captured.nextDueDate shouldBe LocalDate.of(2026, 12, 5)
            }
        }

        Given("wallet management requests") {
            When("creating a wallet with an existing type") {
                val operation = CreateWalletOperation(walletService)
                every { walletService.existsByName("Cash") } returns false
                val payload =
                    operation.fromModelArguments(
                        JSONObject("""{"name":"Cash","walletType":"checking","amount":25}"""),
                        catalog,
                    )

                Then("the type is resolved and the wallet created with the initial balance") {
                    payload.typeName shouldBe "Checking"
                    operation.validate(payload).isValid shouldBe true

                    val saved = slot<Wallet>()
                    every { walletService.createWallet(capture(saved)) } returns 20
                    operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                    saved.captured.balance shouldBe BigDecimal("25.00")
                }
            }

            When("creating a wallet whose name is taken") {
                val operation = CreateWalletOperation(walletService)
                every { walletService.existsByName("Nubank") } returns true
                val payload =
                    operation.fromModelArguments(JSONObject("""{"name":"Nubank","walletType":"Checking"}"""), catalog)

                Then("it is invalid") {
                    operation.validate(payload).isValid shouldBe false
                }
            }

            When("renaming a wallet") {
                val operation = UpdateWalletOperation(walletService)
                val payload =
                    operation.fromModelArguments(
                        JSONObject("""{"walletName":"Nubank","name":"Nu"}"""),
                        catalog,
                    )

                Then("it renames through the service and never touches the balance") {
                    payload.walletId shouldBe 10
                    val saved = slot<Wallet>()
                    every { walletService.renameWallet(capture(saved)) } returns Unit
                    operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                    saved.captured.name shouldBe "Nu"
                    verify(exactly = 0) { walletService.updateWalletBalance(any()) }
                }
            }

            When("archiving and unarchiving") {
                val archive = ArchiveWalletOperation(walletService)
                val unarchive = UnarchiveWalletOperation(walletService)

                Then("archive finds active wallets and unarchive finds archived ones") {
                    archive.fromModelArguments(JSONObject("""{"walletName":"nubank"}"""), catalog).walletId shouldBe 10
                    archive
                        .fromModelArguments(
                            JSONObject("""{"walletName":"Old Savings"}"""),
                            catalog,
                        ).walletId shouldBe
                        null
                    unarchive
                        .fromModelArguments(JSONObject("""{"walletName":"old savings"}"""), catalog)
                        .walletId shouldBe 12
                }
            }
        }

        Given("category requests") {
            every { categoryService.getCategories() } returns listOf(groceries, rent)

            When("creating a category that already exists with another case") {
                val operation = CreateCategoryOperation(categoryService)
                val payload = operation.fromModelArguments(JSONObject("""{"name":"groceries"}"""), catalog)

                Then("it is invalid") {
                    operation.validate(payload).isValid shouldBe false
                }
            }

            When("creating a new category") {
                val operation = CreateCategoryOperation(categoryService)
                val payload = operation.fromModelArguments(JSONObject("""{"name":"Pets"}"""), catalog)

                Then("it is created through the service") {
                    every { categoryService.createCategory(any()) } returns 30
                    operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                    verify { categoryService.createCategory(match { it.name == "Pets" }) }
                }
            }

            When("renaming and archiving an existing category") {
                val update = UpdateCategoryOperation(categoryService)
                val archive = ArchiveCategoryOperation(categoryService)

                Then("they resolve the category by its current name") {
                    val renamed =
                        update.fromModelArguments(JSONObject("""{"categoryName":"rent","name":"Housing"}"""), catalog)
                    renamed.categoryId shouldBe 2
                    update.summary(renamed) shouldBe "Rent → Housing"

                    val archived = archive.fromModelArguments(JSONObject("""{"categoryName":"Nope"}"""), catalog)
                    archive.validate(archived).isValid shouldBe false
                    listOf(archived.currentName) shouldContain "Nope"
                }
            }
        }
    })
