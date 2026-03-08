/*
 * Filename: InvestmentTargetRepository.kt (original filename: InvestmentTargetRepository.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.enums.AssetType
import org.moinex.model.investment.InvestmentTarget
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InvestmentTargetRepository : JpaRepository<InvestmentTarget, Int> {
    fun findAllByIsActiveTrueOrderByAssetTypeAsc(): List<InvestmentTarget>

    fun findByAssetTypeAndIsActiveTrue(assetType: AssetType): InvestmentTarget?

    fun findByAssetType(assetType: AssetType): InvestmentTarget?
}
