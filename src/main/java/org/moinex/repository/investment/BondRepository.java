/*
 * Filename: BondRepository.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.moinex.model.investment.Bond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BondRepository extends JpaRepository<Bond, Integer> {
    List<Bond> findByArchivedFalseOrderByNameAsc();

    List<Bond> findByArchivedTrueOrderByNameAsc();

    Optional<Bond> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);

    /**
     * Get total invested value for a bond (sum of all purchases minus sales)
     * @param bondId The id of the bond
     * @return Total invested value
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN bo.operationType = 'PURCHASE' THEN (bo.unitPrice * bo.quantity) " +
           "WHEN bo.operationType = 'SALE' THEN -(bo.unitPrice * bo.quantity) ELSE 0 END), 0) " +
           "FROM BondOperation bo WHERE bo.bond.id = :bondId")
    BigDecimal getTotalInvestedValue(@Param("bondId") Integer bondId);

    /**
     * Get total quantity for a bond (sum of all purchases minus sales)
     * @param bondId The id of the bond
     * @return Total quantity
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN bo.operationType = 'PURCHASE' THEN bo.quantity " +
           "WHEN bo.operationType = 'SALE' THEN -bo.quantity ELSE 0 END), 0) " +
           "FROM BondOperation bo WHERE bo.bond.id = :bondId")
    BigDecimal getTotalQuantity(@Param("bondId") Integer bondId);
}
