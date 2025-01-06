/*
 * Filename: TickerRepository.java
 * Created on: January  6, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repositories;

import org.moinex.entities.investment.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, Long> {

    /**
     * Check if a ticker with the given name exists
     * @param name The name of the ticker
     * @return True if a ticker with the given name exists, false otherwise
     */
    Boolean existsByName(String name);

    /**
     * Check if a ticker with the given symbol exists
     * @param symbol The symbol of the ticker
     * @return True if a ticker with the given symbol exists, false otherwise
     */
    Boolean existsBySymbol(String symbol);
}
