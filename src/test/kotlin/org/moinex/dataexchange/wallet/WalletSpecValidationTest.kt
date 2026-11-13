/*
 * Filename: WalletSpecValidationTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.wallet

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.moinex.dataexchange.spec.ParsedRow
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.spec.ResolveContext
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.model.enums.RecurringTransactionStatus
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class WalletSpecValidationTest :
    BehaviorSpec({
        val context =
            ResolveContext(
                fileKeys = mapOf("wallets" to setOf("Main"), "categories" to setOf("Food")),
                fileRefs = emptyMap(),
                dbKeys = { if (it == "wallets") setOf("Piggy") else emptySet() },
            )
        val transactions = WalletTransactionSpec(mockk(), mockk(), mockk(), mockk())
        val transfers = TransferSpec(mockk(), mockk(), mockk(), mockk())
        val recurring = RecurringTransactionSpec(mockk(), mockk(), mockk(), mockk())
        val wallets = WalletSpec(mockk(), mockk(), mockk(), mockk(), mockk())

        fun row(vararg pairs: Pair<String, Any?>) = ParsedRow(2, mapOf(*pairs))

        fun transaction(
            wallet: String = "Main",
            category: String = "Food",
            amount: String = "10.00",
        ) = row(
            "wallet" to wallet,
            "category" to category,
            "type" to WalletTransactionType.EXPENSE,
            "status" to WalletTransactionStatus.CONFIRMED,
            "date" to LocalDateTime.of(2026, 10, 1, 8, 0),
            "amount" to BigDecimal(amount),
        )

        Given("a transaction row") {
            Then("references found in the file or in the database are accepted") {
                transactions.validate(transaction(), context) shouldBe emptyList()
                transactions.validate(transaction(wallet = "Piggy"), context) shouldBe emptyList()
            }
            Then("an unknown wallet or category is E06 with the column") {
                val problems = transactions.validate(transaction(wallet = "Ghost", category = "Nope"), context)
                problems.map { it.column to it.code } shouldBe
                    listOf(
                        "wallet" to ProblemCodes.UNRESOLVED_REFERENCE,
                        "category" to ProblemCodes.UNRESOLVED_REFERENCE,
                    )
            }
            Then("a zero or negative amount is E08") {
                transactions.validate(transaction(amount = "0.00"), context).single().code shouldBe
                    ProblemCodes.DOMAIN_RULE
                transactions.validate(transaction(amount = "-3"), context).single().code shouldBe
                    ProblemCodes.DOMAIN_RULE
            }
            Then("the natural key ignores the scale and the seconds fraction") {
                transactions.naturalKey(transaction(amount = "10")) shouldBe
                    transactions.naturalKey(transaction(amount = "10.00"))
            }
        }

        Given("a transfer row") {
            fun transfer(
                from: String,
                to: String,
            ) = row(
                "sender_wallet" to from,
                "receiver_wallet" to to,
                "date" to LocalDateTime.of(2026, 10, 1, 8, 0),
                "amount" to BigDecimal("5.00"),
            )

            Then("the same sender and receiver is E08") {
                transfers.validate(transfer("Main", "Main"), context).single().code shouldBe ProblemCodes.DOMAIN_RULE
            }
            Then("different known wallets pass") {
                transfers.validate(transfer("Main", "Piggy"), context) shouldBe emptyList()
            }
        }

        Given("a recurring transaction row") {
            fun recurringRow(
                start: String,
                end: String?,
                next: String,
            ) = row(
                "wallet" to "Main",
                "category" to "Food",
                "type" to WalletTransactionType.EXPENSE,
                "amount" to BigDecimal("20.00"),
                "start_date" to LocalDate.parse(start),
                "end_date" to end?.let { LocalDate.parse(it) },
                "next_due_date" to LocalDate.parse(next),
                "frequency" to RecurringTransactionFrequency.MONTHLY,
                "status" to RecurringTransactionStatus.ACTIVE,
            )

            Then("a valid row passes and a missing end date is allowed") {
                recurring.validate(recurringRow("2026-01-01", "2027-01-01", "2026-02-01"), context) shouldBe emptyList()
                recurring.validate(recurringRow("2026-01-01", null, "2026-02-01"), context) shouldBe emptyList()
            }
            Then("an end date less than one period after the start is E08") {
                recurring
                    .validate(
                        recurringRow("2026-01-01", "2026-01-15", "2026-01-01"),
                        context,
                    ).single()
                    .column shouldBe
                    "end_date"
            }
            Then("a next due date before the start is E08") {
                recurring
                    .validate(
                        recurringRow("2026-03-01", "2027-01-01", "2026-02-01"),
                        context,
                    ).single()
                    .column shouldBe
                    "next_due_date"
            }
        }

        Given("a wallet row") {
            fun walletRow(
                name: String,
                master: String? = null,
            ) = row(
                "name" to name,
                "type" to "Checking",
                "initial_balance" to BigDecimal("1"),
                "master_wallet" to master,
            )
            val typeContext =
                ResolveContext(
                    mapOf("wallet_types" to setOf("Checking"), "wallets" to setOf("Main")),
                    emptyMap(),
                ) { emptySet() }

            Then("a wallet that is its own master is E08") {
                wallets.validate(walletRow("Main", "Main"), typeContext).single().code shouldBe ProblemCodes.DOMAIN_RULE
            }
            Then("a name above 50 characters is E08 and an unknown master is E06") {
                wallets.validate(walletRow("x".repeat(51)), typeContext).single().code shouldBe ProblemCodes.DOMAIN_RULE
                wallets.validate(walletRow("New", "Ghost"), typeContext).single().code shouldBe
                    ProblemCodes.UNRESOLVED_REFERENCE
            }
            Then("virtual wallets are applied after their master") {
                val rows = listOf(walletRow("V", "Main"), walletRow("Main"))
                wallets.applyOrder(rows).map { it.text("name") } shouldBe listOf("Main", "V")
            }
        }
    })
