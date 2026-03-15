/*
 * Filename: WalletTransaction.kt (original filename: WalletTransaction.java)
 * Created on: August 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.model.wallettransaction

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.model.Category
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "wallet_transaction")
class WalletTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "date", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var date: LocalDateTime,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: WalletTransactionStatus,
    description: String? = null,
    includeInAnalysis: Boolean,
    wallet: Wallet,
    category: Category,
    type: WalletTransactionType,
    amount: BigDecimal,
) : BaseTransaction(wallet, category, type, amount, description, includeInAnalysis) {
    override fun toString(): String = "Wallet Transaction [id=$id, type=$type, amount=$amount]"

    companion object {
        fun from(
            walletTransactionContextDTO: WalletTransactionContextDTO,
            type: WalletTransactionType,
            amount: BigDecimal,
        ): WalletTransaction =
            WalletTransaction(
                date = walletTransactionContextDTO.date,
                status = walletTransactionContextDTO.status,
                description = walletTransactionContextDTO.description,
                includeInAnalysis = walletTransactionContextDTO.includeInAnalysis,
                wallet = walletTransactionContextDTO.wallet,
                category = walletTransactionContextDTO.category,
                type = type,
                amount = amount,
            )
    }
}
