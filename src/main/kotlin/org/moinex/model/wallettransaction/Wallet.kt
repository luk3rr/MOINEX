/*
 * Filename: Wallet.kt (original filename: Wallet.java)
 * Created on: March 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.model.wallettransaction

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.common.extension.toRounded
import java.math.BigDecimal

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "wallet")
class Wallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @ManyToOne
    @JoinColumn(name = "type_id", referencedColumnName = "id", nullable = false)
    var type: WalletType,
    @Column(name = "name", nullable = false, length = 50, unique = true)
    var name: String,
    @Column(name = "balance", nullable = false, scale = 2)
    var balance: BigDecimal,
    @Column(name = "archived", nullable = false)
    var isArchived: Boolean = false,
    @ManyToOne
    @JoinColumn(name = "master_wallet_id", referencedColumnName = "id")
    var masterWallet: Wallet? = null,
) {
    init {
        balance = balance.toRounded()
        name = name.trim()
        require(name.isNotEmpty()) {
            "Wallet name cannot be empty"
        }
    }

    fun isVirtual(): Boolean = !isMaster()

    fun isMaster(): Boolean = masterWallet == null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val wallet = other as Wallet
        return id != null && id == wallet.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: javaClass.hashCode()

    override fun toString(): String = "Wallet [id=$id, name='$name']"
}
