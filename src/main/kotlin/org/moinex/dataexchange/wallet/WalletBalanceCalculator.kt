/*
 * Filename: WalletBalanceCalculator.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.wallet

import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import java.math.BigDecimal

object WalletBalanceCalculator {
    /**
     * The balance each wallet had before any confirmed transaction or transfer, keyed by wallet id.
     * A master wallet also loses the effect of its virtual wallets, since those move the master balance.
     */
    fun initialBalances(
        wallets: List<Wallet>,
        transactions: List<WalletTransaction>,
        transfers: List<Transfer>,
    ): Map<Int, BigDecimal> {
        val own = mutableMapOf<Int, BigDecimal>()

        fun add(
            wallet: Wallet,
            delta: BigDecimal,
        ) {
            own.merge(wallet.id!!, delta, BigDecimal::add)
        }

        transactions
            .filter { it.status == WalletTransactionStatus.CONFIRMED }
            .forEach { add(it.wallet, if (it.type == WalletTransactionType.INCOME) it.amount else it.amount.negate()) }
        transfers.forEach {
            add(it.senderWallet, it.amount.negate())
            add(it.receiverWallet, it.amount)
        }

        val virtualEffectByMaster = mutableMapOf<Int, BigDecimal>()
        wallets.filter { it.masterWallet != null }.forEach { virtual ->
            virtualEffectByMaster.merge(
                virtual.masterWallet!!.id!!,
                own[virtual.id!!] ?: BigDecimal.ZERO,
                BigDecimal::add,
            )
        }

        return wallets.associate { wallet ->
            val id = wallet.id!!
            id to
                wallet.balance
                    .subtract(own[id] ?: BigDecimal.ZERO)
                    .subtract(virtualEffectByMaster[id] ?: BigDecimal.ZERO)
        }
    }
}
