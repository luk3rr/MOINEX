/*
 * Filename: InvestmentPerformanceSnapshotRepository.java
 * Created on: February 17, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository;

import java.util.List;
import java.util.Optional;
import org.moinex.model.InvestmentPerformanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InvestmentPerformanceSnapshotRepository
        extends JpaRepository<InvestmentPerformanceSnapshot, Integer> {

    Optional<InvestmentPerformanceSnapshot> findByMonthAndYear(Integer month, Integer year);

    @Query(
            "SELECT ips FROM InvestmentPerformanceSnapshot ips ORDER BY ips.year ASC, ips.month"
                    + " ASC")
    List<InvestmentPerformanceSnapshot> findAllOrderedByDate();

    @Modifying
    @Transactional
    @Query("DELETE FROM InvestmentPerformanceSnapshot")
    void deleteAll();

    boolean existsByMonthAndYear(Integer month, Integer year);
}
