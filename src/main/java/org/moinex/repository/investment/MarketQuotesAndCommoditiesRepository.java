/*
 * Filename: MarketQuotesAndCommoditiesRepository.java
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import org.moinex.model.investment.MarketQuotesAndCommodities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketQuotesAndCommoditiesRepository
        extends JpaRepository<MarketQuotesAndCommodities, Integer> {}
