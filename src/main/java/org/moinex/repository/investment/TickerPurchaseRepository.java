/*
 * Filename: TickerPurchaseRepository.java
 * Created on: January  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import org.moinex.model.investment.TickerPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerPurchaseRepository extends JpaRepository<TickerPurchase, Integer> {
    @Query(
            "SELECT tp FROM TickerPurchase tp "
                    + "LEFT JOIN tp.walletTransaction wt "
                    + "WHERE wt.date <= :date "
                    + "ORDER BY wt.date ASC")
    List<TickerPurchase> findAllByDateBefore(@Param("date") String date);
}
