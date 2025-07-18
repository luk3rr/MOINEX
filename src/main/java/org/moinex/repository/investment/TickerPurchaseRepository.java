/*
 * Filename: TickerPurchaseRepository.java
 * Created on: January  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import org.moinex.model.investment.TickerPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TickerPurchaseRepository extends JpaRepository<TickerPurchase, Integer> {}
