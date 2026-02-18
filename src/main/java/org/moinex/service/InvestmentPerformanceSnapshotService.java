/*
 * Filename: InvestmentPerformanceSnapshotService.java
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.moinex.model.InvestmentPerformanceSnapshot;
import org.moinex.repository.InvestmentPerformanceSnapshotRepository;
import org.moinex.util.Constants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvestmentPerformanceSnapshotService {

    private final InvestmentPerformanceSnapshotRepository repository;

    public Optional<InvestmentPerformanceSnapshot> getSnapshot(Integer month, Integer year) {
        return repository.findByMonthAndYear(month, year);
    }

    public List<InvestmentPerformanceSnapshot> getAllSnapshots() {
        return repository.findAllOrderedByDate();
    }

    @Transactional
    public InvestmentPerformanceSnapshot saveSnapshot(
            Integer month,
            Integer year,
            BigDecimal investedValue,
            BigDecimal portfolioValue,
            BigDecimal accumulatedCapitalGains,
            BigDecimal monthlyCapitalGains) {

        Optional<InvestmentPerformanceSnapshot> existing =
                repository.findByMonthAndYear(month, year);

        InvestmentPerformanceSnapshot snapshot;
        if (existing.isPresent()) {
            snapshot = existing.get();
            snapshot.setInvestedValue(investedValue);
            snapshot.setPortfolioValue(portfolioValue);
            snapshot.setAccumulatedCapitalGains(accumulatedCapitalGains);
            snapshot.setMonthlyCapitalGains(monthlyCapitalGains);
            snapshot.setCalculatedAt(LocalDateTime.now().format(Constants.DB_DATE_FORMATTER));
        } else {
            snapshot =
                    InvestmentPerformanceSnapshot.builder()
                            .month(month)
                            .year(year)
                            .investedValue(investedValue)
                            .portfolioValue(portfolioValue)
                            .accumulatedCapitalGains(accumulatedCapitalGains)
                            .monthlyCapitalGains(monthlyCapitalGains)
                            .calculatedAt(LocalDateTime.now().format(Constants.DB_DATE_FORMATTER))
                            .build();
        }

        return repository.save(snapshot);
    }

    @Transactional
    public void deleteAllSnapshots() {
        repository.deleteAll();
    }

    public boolean hasSnapshots() {
        return repository.count() > 0;
    }
}
