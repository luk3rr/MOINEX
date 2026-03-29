package org.moinex.factory.wishlist

import org.moinex.factory.CategoryFactory
import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.model.enums.WishlistItemStatus
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.model.wishlist.WishlistItem
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

object WishlistItemFactory {
    fun create(
        id: Int? = null,
        title: String = "Test Wishlist Item",
        estimatedPrice: BigDecimal = BigDecimal("100.00"),
        targetDate: LocalDate? = null,
        status: WishlistItemStatus = WishlistItemStatus.PENDING,
        priority: WishlistItemPriority = WishlistItemPriority.MEDIUM,
        notes: String? = null,
        category: Category = CategoryFactory.create(),
        createdAt: LocalDateTime = LocalDateTime.now(),
        purchasedAt: LocalDateTime? = null,
        walletTransaction: WalletTransaction? = null,
        creditCardDebt: CreditCardDebt? = null,
    ): WishlistItem =
        WishlistItem(
            id = id,
            title = title,
            estimatedPrice = estimatedPrice,
            targetDate = targetDate,
            status = status,
            priority = priority,
            notes = notes,
            category = category,
            createdAt = createdAt,
            purchasedAt = purchasedAt,
            walletTransaction = walletTransaction,
            creditCardDebt = creditCardDebt,
        )
}
