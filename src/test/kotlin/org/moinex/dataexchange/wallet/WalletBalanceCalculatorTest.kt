/*
 * Filename: WalletBalanceCalculatorTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.wallet

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.model.Category
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.model.wallettransaction.WalletType
import java.math.BigDecimal
import java.time.LocalDateTime

class WalletBalanceCalculatorTest :
    BehaviorSpec({
        val type = WalletType(1, "Checking")
        val category = Category(1, "Food")
        val now = LocalDateTime.of(2026, 11, 12, 10, 0)

        fun tx(
            wallet: Wallet,
            kind: WalletTransactionType,
            amount: String,
            status: WalletTransactionStatus = WalletTransactionStatus.CONFIRMED,
        ) = WalletTransaction(null, now, status, null, true, wallet, category, kind, BigDecimal(amount))

        Given("a master wallet with a virtual wallet, transactions and a transfer") {
            val master = Wallet(1, type, "Master", BigDecimal("1050.00"))
            val virtual = Wallet(2, type, "Virtual", BigDecimal("250.00"), masterWallet = master)
            val other = Wallet(3, type, "Other", BigDecimal("40.00"))
            val transactions =
                listOf(
                    tx(virtual, WalletTransactionType.INCOME, "50.00"),
                    tx(master, WalletTransactionType.EXPENSE, "30.00"),
                    tx(other, WalletTransactionType.INCOME, "999.00", WalletTransactionStatus.PENDING),
                )
            val transfers = listOf(Transfer(1, master, other, now, BigDecimal("10.00"), null, null))

            When("computing the initial balances") {
                val initial =
                    WalletBalanceCalculator.initialBalances(
                        listOf(master, virtual, other),
                        transactions,
                        transfers,
                    )

                Then(
                    "pending is ignored, the virtual effect is removed from the master, and the references match a hand calculation",
                ) {
                    initial.getValue(2) shouldBe BigDecimal("200.00")
                    initial.getValue(1) shouldBe BigDecimal("1040.00")
                    initial.getValue(3) shouldBe BigDecimal("30.00")
                }
            }
        }
    })
