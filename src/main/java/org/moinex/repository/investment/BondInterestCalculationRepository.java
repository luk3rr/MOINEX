/*
 * Filename: BondInterestCalculationRepository.java
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import java.util.Optional;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BondInterestCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface BondInterestCalculationRepository
        extends JpaRepository<BondInterestCalculation, Integer> {

    @Modifying
    @Transactional
    @Query("DELETE FROM BondInterestCalculation b WHERE b.bond = :bond")
    void deleteByBond(@Param("bond") Bond bond);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " AND b.referenceMonth = :referenceMonth")
    Optional<BondInterestCalculation> findByBondAndReferenceMonth(
            @Param("bond") Bond bond, @Param("referenceMonth") String referenceMonth);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " ORDER BY b.referenceMonth ASC")
    List<BondInterestCalculation> findByBondOrderByReferenceMonthAsc(@Param("bond") Bond bond);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " ORDER BY b.referenceMonth DESC LIMIT 1")
    Optional<BondInterestCalculation> findLastCalculatedMonth(@Param("bond") Bond bond);
}
