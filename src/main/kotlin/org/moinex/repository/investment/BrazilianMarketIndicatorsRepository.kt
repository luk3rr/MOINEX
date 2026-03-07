/*
 * Filename: BrazilianMarketIndicatorsRepository.kt (original filename: BrazilianMarketIndicatorsRepository.java)
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.investment.BrazilianMarketIndicators
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BrazilianMarketIndicatorsRepository : JpaRepository<BrazilianMarketIndicators, Int>
