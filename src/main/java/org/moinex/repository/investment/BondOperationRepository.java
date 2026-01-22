/*
 * Filename: BondOperationRepository.java
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import org.moinex.model.enums.OperationType;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BondOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BondOperationRepository extends JpaRepository<BondOperation, Integer> {
    @Query(
            "SELECT bo FROM BondOperation bo LEFT JOIN bo.walletTransaction wt ORDER BY wt.date"
                    + " DESC")
    List<BondOperation> findAllByOrderByOperationDateDesc();

    @Query(
            "SELECT bo FROM BondOperation bo LEFT JOIN bo.walletTransaction wt WHERE bo.bond ="
                    + " :bond ORDER BY wt.date ASC")
    List<BondOperation> findByBondOrderByOperationDateAsc(@Param("bond") Bond bond);

    @Query(
            "SELECT bo FROM BondOperation bo LEFT JOIN bo.walletTransaction wt WHERE bo.bond ="
                    + " :bond AND bo.operationType = :operationType ORDER BY wt.date ASC")
    List<BondOperation> findByBondAndOperationTypeOrderByOperationDateAsc(
            @Param("bond") Bond bond, @Param("operationType") OperationType operationType);

    @Query(
            "SELECT bo FROM BondOperation bo "
                    + "LEFT JOIN bo.walletTransaction wt "
                    + "WHERE wt.date <= :date "
                    + "ORDER BY wt.date ASC")
    List<BondOperation> findAllByDateBefore(@Param("date") String date);
}
