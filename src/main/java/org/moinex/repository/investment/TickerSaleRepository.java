/*
 * Filename: TickerSaleRepository.java
 * Created on: January  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import org.moinex.model.investment.TickerSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerSaleRepository extends JpaRepository<TickerSale, Integer> {
    @Query(
            "SELECT ts FROM TickerSale ts "
                    + "LEFT JOIN ts.walletTransaction wt "
                    + "WHERE wt.date <= :date "
                    + "ORDER BY wt.date ASC")
    List<TickerSale> findAllByDateBefore(@Param("date") String date);
}
