/*
 * Filename: NetWorthService.kt
 * Created on: March 16, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Service responsible for transactional persistence operations of NetWorthSnapshot entities.
 * Separated from NetWorthCalculationService to avoid self-invocation issues with @Transactional.
 */

package org.moinex.service.networth

import org.moinex.common.ClockProvider
import org.moinex.model.NetWorthSnapshot
import org.moinex.repository.NetWorthSnapshotRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class NetWorthService(
    private val netWorthSnapshotRepository: NetWorthSnapshotRepository,
    private val clockProvider: ClockProvider,
) {
    private val logger = LoggerFactory.getLogger(NetWorthService::class.java)

    @Transactional
    fun deleteSnapshotsOutsideRange(
        startMonth: YearMonth,
        endMonth: YearMonth,
    ) {
        logger.info("Deleting snapshots outside range {} to {}", startMonth, endMonth)
        netWorthSnapshotRepository.deleteSnapshotsOutsideRange(startMonth, endMonth)
    }

    @Transactional
    fun saveBatch(snapshots: List<NetWorthSnapshot>) {
        logger.debug("Saving batch of {} snapshots", snapshots.size)
        snapshots.forEach { snapshot ->
            save(snapshot)
        }
    }

    @Transactional
    fun save(snapshot: NetWorthSnapshot): NetWorthSnapshot {
        val existing = netWorthSnapshotRepository.findByReferenceMonth(snapshot.referenceMonth)

        val entity =
            existing?.apply {
                referenceMonth = snapshot.referenceMonth
                assets = snapshot.assets
                liabilities = snapshot.liabilities
                netWorth = snapshot.netWorth
                walletBalances = snapshot.walletBalances
                investments = snapshot.investments
                creditCardDebt = snapshot.creditCardDebt
                negativeWalletBalances = snapshot.negativeWalletBalances
                calculatedAt = clockProvider.now()
            } ?: snapshot

        return netWorthSnapshotRepository.save(entity)
    }

    fun findByReferenceMonth(referenceMonth: YearMonth): NetWorthSnapshot? =
        netWorthSnapshotRepository.findByReferenceMonth(referenceMonth)

    fun getSnapshot(referenceMonth: YearMonth): NetWorthSnapshot? =
        netWorthSnapshotRepository.findByReferenceMonth(referenceMonth)
}
