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

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond.id = :bondId"
                    + " ORDER BY b.calculationDate DESC LIMIT 1")
    Optional<BondInterestCalculation> findLatestByBondId(@Param("bondId") Integer bondId);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond.id = :bondId"
                    + " ORDER BY b.calculationDate DESC")
    List<BondInterestCalculation> findByBondIdOrderByCalculationDateDesc(
            @Param("bondId") Integer bondId);

    void deleteByBondIdAndCalculationDate(
            @Param("bondId") Integer bondId, @Param("calculationDate") String calculationDate);

    // Métodos para histórico mensal
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
                    + " ORDER BY b.referenceMonth DESC")
    List<BondInterestCalculation> findByBondOrderByReferenceMonthDesc(@Param("bond") Bond bond);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " AND b.referenceMonth >= :startMonth AND b.referenceMonth <= :endMonth"
                    + " ORDER BY b.referenceMonth ASC")
    List<BondInterestCalculation> findByBondAndReferenceMonthBetween(
            @Param("bond") Bond bond,
            @Param("startMonth") String startMonth,
            @Param("endMonth") String endMonth);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " AND b.referenceMonth = :referenceMonth")
    boolean existsByBondAndReferenceMonth(
            @Param("bond") Bond bond, @Param("referenceMonth") String referenceMonth);

    @Query(
            "SELECT b FROM BondInterestCalculation b WHERE b.bond = :bond"
                    + " ORDER BY b.referenceMonth DESC LIMIT 1")
    Optional<BondInterestCalculation> findLastCalculatedMonth(@Param("bond") Bond bond);
}
