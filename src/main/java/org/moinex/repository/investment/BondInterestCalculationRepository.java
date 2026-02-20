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
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BondInterestCalculationRepository
        extends JpaRepository<BondInterestCalculation, Integer> {

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " AND b.calculationDate = :calculationDate")
    Optional<BondInterestCalculation> findByBondAndCalculationDate(
            @Param("bond") Bond bond, @Param("calculationDate") String calculationDate);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " ORDER BY b.calculationDate DESC")
    List<BondInterestCalculation> findByBondOrderByCalculationDateDesc(@Param("bond") Bond bond);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " ORDER BY b.calculationDate ASC")
    List<BondInterestCalculation> findByBondOrderByCalculationDateAsc(@Param("bond") Bond bond);

    @Query("DELETE FROM BondInterestCalculation b WHERE b.bond = :bond")
    void deleteByBond(@Param("bond") Bond bond);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " ORDER BY b.calculationDate DESC LIMIT 1")
    Optional<BondInterestCalculation> findLatestByBond(@Param("bond") Bond bond);
}
