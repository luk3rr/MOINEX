/*
 * Filename: WalletTypeRepository.kt (original filename: WalletTypeRepository.java)
 * Created on: September 29, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.repository.wallettransaction

import org.moinex.model.wallettransaction.WalletType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WalletTypeRepository : JpaRepository<WalletType, Int> {
    fun findAllByOrderByNameAsc(): List<WalletType>

    fun findByName(name: String): Optional<WalletType>

    fun existsByName(name: String): Boolean
}
