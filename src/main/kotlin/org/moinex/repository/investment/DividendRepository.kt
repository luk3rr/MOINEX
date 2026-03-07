/*
 * Filename: DividendRepository.kt (original filename: DividendRepository.java)
 * Created on: January  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.investment.Dividend
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DividendRepository : JpaRepository<Dividend, Int>
