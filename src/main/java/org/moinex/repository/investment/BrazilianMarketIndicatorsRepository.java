/*
 * Filename: BrazilianMarketIndicatorsRepository.java
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import org.moinex.model.investment.BrazilianMarketIndicators;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrazilianMarketIndicatorsRepository
    extends JpaRepository<BrazilianMarketIndicators, Long> { }
