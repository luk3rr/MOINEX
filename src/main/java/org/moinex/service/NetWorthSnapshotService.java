/*
 * Filename: NetWorthSnapshotService.java
 * Created on: January 22, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.moinex.model.NetWorthSnapshot;
import org.moinex.repository.NetWorthSnapshotRepository;
import org.moinex.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NetWorthSnapshotService {

    private final NetWorthSnapshotRepository netWorthSnapshotRepository;

    /**
     * Get snapshot for a specific month and year
     * @param month The month
     * @param year The year
     * @return Optional snapshot
     */
    public Optional<NetWorthSnapshot> getSnapshot(Integer month, Integer year) {
        return netWorthSnapshotRepository.findByMonthAndYear(month, year);
    }

    /**
     * Get all snapshots ordered by date
     * @return List of snapshots
     */
    public List<NetWorthSnapshot> getAllSnapshots() {
        return netWorthSnapshotRepository.findAllOrderedByDate();
    }

    /**
     * Save or update snapshot
     * @param month The month
     * @param year The year
     * @param assets Total assets
     * @param liabilities Total liabilities
     * @param netWorth Net worth
     * @param walletBalances Wallet balances
     * @param investments Investments
     * @param creditCardDebt Credit card debt
     * @param negativeWalletBalances Negative wallet balances
     * @return Saved snapshot
     */
    @Transactional
    public NetWorthSnapshot saveSnapshot(
            Integer month,
            Integer year,
            BigDecimal assets,
            BigDecimal liabilities,
            BigDecimal netWorth,
            BigDecimal walletBalances,
            BigDecimal investments,
            BigDecimal creditCardDebt,
            BigDecimal negativeWalletBalances) {

        Optional<NetWorthSnapshot> existing =
                netWorthSnapshotRepository.findByMonthAndYear(month, year);

        NetWorthSnapshot snapshot;
        if (existing.isPresent()) {
            snapshot = existing.get();
            snapshot.setAssets(assets);
            snapshot.setLiabilities(liabilities);
            snapshot.setNetWorth(netWorth);
            snapshot.setWalletBalances(walletBalances);
            snapshot.setInvestments(investments);
            snapshot.setCreditCardDebt(creditCardDebt);
            snapshot.setNegativeWalletBalances(negativeWalletBalances);
            snapshot.setCalculatedAt(LocalDateTime.now().format(Constants.DB_DATE_FORMATTER));
        } else {
            snapshot =
                    NetWorthSnapshot.builder()
                            .month(month)
                            .year(year)
                            .assets(assets)
                            .liabilities(liabilities)
                            .netWorth(netWorth)
                            .walletBalances(walletBalances)
                            .investments(investments)
                            .creditCardDebt(creditCardDebt)
                            .negativeWalletBalances(negativeWalletBalances)
                            .calculatedAt(LocalDateTime.now().format(Constants.DB_DATE_FORMATTER))
                            .build();
        }

        return netWorthSnapshotRepository.save(snapshot);
    }

    /**
     * Delete all snapshots
     */
    @Transactional
    public void deleteAllSnapshots() {
        netWorthSnapshotRepository.deleteAll();
    }

    /**
     * Check if snapshot exists
     * @param month The month
     * @param year The year
     * @return True if exists
     */
    public boolean snapshotExists(Integer month, Integer year) {
        return netWorthSnapshotRepository.existsByMonthAndYear(month, year);
    }
}
