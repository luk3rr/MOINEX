/*
 * Filename: CryptoExchangeRepository.kt (original filename: CryptoExchangeRepository.java)
 * Created on: January 28, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.investment.CryptoExchange
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CryptoExchangeRepository : JpaRepository<CryptoExchange, Int>
