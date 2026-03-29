/*
 * Filename: WishlistItem.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.wishlist

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateStringConverter
import org.moinex.common.converter.LocalDateTimeStringConverter
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.model.enums.WishlistItemStatus
import org.moinex.model.wallettransaction.WalletTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "wishlist_item")
class WishlistItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "title", nullable = false, length = 255)
    var title: String,
    @Column(name = "estimated_price", nullable = false, scale = 2)
    var estimatedPrice: BigDecimal,
    @Column(name = "target_date")
    @Convert(converter = LocalDateStringConverter::class)
    var targetDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: WishlistItemStatus = WishlistItemStatus.PENDING,
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    var priority: WishlistItemPriority = WishlistItemPriority.MEDIUM,
    @Column(name = "notes", length = 1000)
    var notes: String? = null,
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,
    @Column(name = "created_at", nullable = false)
    @Convert(converter = LocalDateTimeStringConverter::class)
    var createdAt: LocalDateTime,
    @Column(name = "purchased_at")
    @Convert(converter = LocalDateTimeStringConverter::class)
    var purchasedAt: LocalDateTime? = null,
    @ManyToOne
    @JoinColumn(name = "wallet_transaction_id")
    var walletTransaction: WalletTransaction? = null,
    @ManyToOne
    @JoinColumn(name = "credit_card_debt_id")
    var creditCardDebt: CreditCardDebt? = null,
) {
    init {
        title = title.trim()
        require(title.isNotEmpty()) {
            "Wishlist item title cannot be empty"
        }

        require(estimatedPrice >= BigDecimal.ZERO) {
            "Estimated price must be greater than or equal to zero"
        }

        notes = notes?.trim()
    }

    override fun toString(): String = "WishlistItem [id=$id, title='$title', status=$status]"
}
