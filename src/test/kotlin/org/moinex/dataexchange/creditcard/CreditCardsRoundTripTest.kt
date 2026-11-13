/*
 * Filename: CreditCardsRoundTripTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.creditcard

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.moinex.common.util.FxUtils
import org.moinex.dataexchange.export.ExportService
import org.moinex.dataexchange.imports.ImportExecutor
import org.moinex.dataexchange.imports.ImportPlanner
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.dataexchange.wallet.WalletTestDatabase
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardCredit
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.creditcard.CreditCardOperator
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.enums.CreditCardCreditType
import org.moinex.model.enums.CreditCardRecurringFrequency
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import org.moinex.repository.CategoryRepository
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.CategoryService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class CreditCardsRoundTripTest :
    BehaviorSpec({
        beforeSpec {
            mockkObject(FxUtils)
            every { FxUtils.launchOnFxThread(any()) } returns mockk<Job>(relaxed = true)
        }

        afterSpec { unmockkObject(FxUtils) }

        fun seed(db: WalletTestDatabase) {
            val checking = db.get<WalletTypeRepository>().save(WalletType(name = "Checking"))
            val categories = db.get<CategoryService>()
            categories.createCategory(Category(name = "Food"))
            categories.createCategory(Category(name = "Travel"))
            db.get<WalletService>().createWallet(
                Wallet(type = checking, name = "Main", balance = BigDecimal("5000.00")),
            )
            val main = db.get<WalletRepository>().findByName("Main").get()
            val food = db.get<CategoryRepository>().findByName("Food")!!
            val travel = db.get<CategoryRepository>().findByName("Travel")!!

            val operator = db.get<CreditCardOperatorRepository>().save(CreditCardOperator(name = "Visa", icon = "visa"))
            val cards = db.get<CreditCardService>()
            cards.createCreditCard(
                CreditCard(
                    operator = operator,
                    defaultBillingWallet = main,
                    name = "Gold",
                    billingDueDay = 10,
                    closingDay = 3,
                    maxDebt = BigDecimal("5000.00"),
                    lastFourDigits = "1234",
                ),
            )
            val gold = db.get<CreditCardRepository>().findAllByOrderByNameAsc().first()
            val recurring =
                db.get<RecurringCreditCardDebtRepository>().save(
                    RecurringCreditCardDebt(
                        creditCard = gold,
                        category = food,
                        amount = BigDecimal("29.90"),
                        description = "streaming",
                        dayOfMonth = 5,
                        frequency = CreditCardRecurringFrequency.MONTHLY,
                        startDate = LocalDate.of(2026, 9, 5),
                        nextInvoiceMonth = YearMonth.of(2026, 11),
                    ),
                )
            cards.createDebt(
                CreditCardDebt(
                    category = travel,
                    installments = 3,
                    creditCard = gold,
                    date = LocalDateTime.of(2026, 9, 20, 12, 0, 0),
                    amount = BigDecimal("300.01"),
                    description = "flight, \"promo\"",
                ),
                YearMonth.of(2026, 10),
            )
            cards.createDebt(
                CreditCardDebt(
                    category = food,
                    installments = 1,
                    recurringSource = recurring,
                    creditCard = gold,
                    date = LocalDateTime.of(2026, 10, 5, 8, 0, 0),
                    amount = BigDecimal("29.90"),
                    description = "streaming",
                ),
                YearMonth.of(2026, 10),
            )
            cards.addRebate(
                CreditCardCredit(
                    type = CreditCardCreditType.CASHBACK,
                    creditCard = gold,
                    date = LocalDateTime.of(2026, 10, 1, 0, 0, 0),
                    amount = BigDecimal("25.50"),
                    description = "promo",
                ),
            )
            val payments = db.get<CreditCardPaymentRepository>()
            val october = payments.findAll().filter { it.date.monthValue == 10 }
            october.forEach {
                it.wallet = main
                it.paidAmount = it.amount
                it.rebateUsed =
                    if (it.creditCardDebt.description == "streaming") BigDecimal("10.00") else BigDecimal.ZERO
                payments.save(it)
            }
            val paid = october.sumOf { it.amount }
            val walletRepository = db.get<WalletRepository>()
            walletRepository.save(main.apply { balance -= paid - BigDecimal("10.00") })
            val cardRepository = db.get<CreditCardRepository>()
            cardRepository.save(
                cardRepository.findById(gold.id!!).get().apply { availableRebate -= BigDecimal("10.00") },
            )
        }

        fun snapshot(db: WalletTestDatabase): Map<String, Any> =
            mapOf(
                "wallets" to
                    db.get<WalletRepository>().findAll().sortedBy { it.name }.map {
                        listOf(
                            it.name,
                            it.balance.toPlainString(),
                        )
                    },
                "operators" to db.get<CreditCardOperatorRepository>().findAll().map { listOf(it.name, it.icon) },
                "cards" to
                    db.get<CreditCardRepository>().findAll().map {
                        listOf(
                            it.name,
                            it.operator.name,
                            it.defaultBillingWallet?.name,
                            it.billingDueDay,
                            it.closingDay,
                            it.maxDebt.toPlainString(),
                            it.availableRebate.toPlainString(),
                            it.lastFourDigits,
                            it.isArchived,
                        )
                    },
                "recurring" to
                    db.get<RecurringCreditCardDebtRepository>().findAll().map {
                        listOf(
                            it.creditCard.name,
                            it.amount.toPlainString(),
                            it.description,
                            it.startDate,
                            it.nextInvoiceMonth,
                            it.status,
                        )
                    },
                "debts" to
                    db
                        .get<CreditCardDebtRepository>()
                        .findAll()
                        .map {
                            listOf(
                                it.creditCard.name,
                                it.category.name,
                                it.date,
                                it.amount.toPlainString(),
                                it.description,
                                it.installments,
                                it.recurringSource?.description,
                            )
                        }.sortedBy { it.joinToString("|") },
                "payments" to
                    db
                        .get<CreditCardPaymentRepository>()
                        .findAll()
                        .map {
                            listOf(
                                it.creditCardDebt.description,
                                it.installment,
                                it.amount.toPlainString(),
                                it.paidAmount.toPlainString(),
                                it.rebateUsed.toPlainString(),
                                it.wallet?.name,
                                it.refunded,
                                it.date,
                            )
                        }.sortedBy { it.joinToString("|") },
                "credits" to
                    db.get<CreditCardCreditRepository>().findAll().map {
                        listOf(it.creditCard.name, it.type, it.date, it.amount.toPlainString(), it.description)
                    },
            )

        listOf(ExportFormat.XLSX, ExportFormat.CSV_ZIP).forEach { format ->
            Given("a database with a card, debts, a paid invoice and a rebate exported as $format") {
                WalletTestDatabase().use { a ->
                    seed(a)
                    val expected = snapshot(a)
                    val file = Files.createTempFile("cards", if (format == ExportFormat.XLSX) ".xlsx" else ".zip")
                    runBlocking { a.get<ExportService>().export(a.get<ExportService>().counts().keys, format, file) }

                    When("importing into an empty database") {
                        WalletTestDatabase().use { b ->
                            val plan = runBlocking { b.get<ImportPlanner>().plan(file) }
                            (plan.fileProblems + plan.entities.flatMap { it.problems }) shouldBe emptyList()
                            b.get<ImportExecutor>().execute(plan)

                            Then("the plan is clean") {
                                plan.hasErrors shouldBe false
                            }

                            Then("cards, debts, installments, payments, rebates and wallet balances are identical") {
                                snapshot(b) shouldBe expected
                            }

                            Then("importing again adds nothing") {
                                val second = runBlocking { b.get<ImportPlanner>().plan(file) }
                                second.hasErrors shouldBe false
                                second.entities.sumOf { it.toAdd } shouldBe 0
                            }
                        }
                    }
                }
            }
        }

        Given("a payment whose rebate_used is negative in the database (legacy data)") {
            WalletTestDatabase().use { a ->
                seed(a)
                val payments = a.get<CreditCardPaymentRepository>()
                payments.findAll().first().also {
                    it.rebateUsed = BigDecimal("-0.01")
                    payments.save(it)
                }
                val file = Files.createTempFile("cards-negative", ".xlsx")
                runBlocking {
                    a.get<ExportService>().export(
                        a.get<ExportService>().counts().keys,
                        ExportFormat.XLSX,
                        file,
                    )
                }

                When("importing into an empty database") {
                    WalletTestDatabase().use { b ->
                        val plan = runBlocking { b.get<ImportPlanner>().plan(file) }

                        Then("the value is corrected to zero and reported as an adjustment, not an error") {
                            plan.hasErrors shouldBe false
                            plan.entities.flatMap { it.problems }.map { it.code } shouldBe
                                listOf(ProblemCodes.ADJUSTED_VALUE)
                            b.get<ImportExecutor>().execute(plan)
                            b.get<CreditCardPaymentRepository>().findAll().all { it.rebateUsed.signum() >= 0 } shouldBe
                                true
                        }
                    }
                }
            }
        }
    })
