/*
 * Filename: CryptoExchangeRepository.java
 * Created on: January 28, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import org.moinex.model.investment.CryptoExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CryptoExchangeRepository extends JpaRepository<CryptoExchange, Integer> {}
