/*
 * Filename: WishlistService.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.wishlist

import org.moinex.common.ClockProvider
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isPending
import org.moinex.common.extension.isPurchased
import org.moinex.model.enums.WishlistItemStatus
import org.moinex.model.wishlist.WishlistItem
import org.moinex.model.wishlist.WishlistItemLink
import org.moinex.repository.wishlist.WishlistItemLinkRepository
import org.moinex.repository.wishlist.WishlistItemRepository
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WishlistService(
    private val wishlistItemRepository: WishlistItemRepository,
    private val wishlistItemLinkRepository: WishlistItemLinkRepository,
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val clockProvider: ClockProvider = ClockProvider(),
) {
    private val logger = LoggerFactory.getLogger(WishlistService::class.java)

    @Transactional
    fun createItem(
        item: WishlistItem,
        links: List<WishlistItemLink>,
    ): Int {
        val newItem = wishlistItemRepository.save(item)

        links.forEach { link ->
            link.wishlistItem = newItem
            wishlistItemLinkRepository.save(link)
        }

        logger.info("$newItem created successfully")

        return newItem.id!!
    }

    @Transactional
    fun updateItem(
        item: WishlistItem,
        links: List<WishlistItemLink>,
    ) {
        val itemFromDatabase = wishlistItemRepository.findByIdOrThrow(item.id!!)

        itemFromDatabase.apply {
            title = item.title
            estimatedPrice = item.estimatedPrice
            targetDate = item.targetDate
            priority = item.priority
            notes = item.notes
            category = item.category
        }

        wishlistItemRepository.save(itemFromDatabase)

        val existingLinks = wishlistItemLinkRepository.findAllByWishlistItemId(item.id!!)
        val existingLinksMap = existingLinks.associateBy { it.id }
        val newLinksMap = links.filter { it.id != null }.associateBy { it.id }

        val linksToDelete = existingLinks.filter { it.id !in newLinksMap.keys }
        linksToDelete.forEach { wishlistItemLinkRepository.delete(it) }

        links.forEach { link ->
            link.wishlistItem = itemFromDatabase
            if (link.id != null && link.id in existingLinksMap.keys) {
                val existingLink = existingLinksMap[link.id]!!
                existingLink.url = link.url
                existingLink.label = link.label
                wishlistItemLinkRepository.save(existingLink)
            } else {
                wishlistItemLinkRepository.save(link)
            }
        }

        logger.info("$itemFromDatabase updated successfully")
    }

    @Transactional
    fun deleteItem(id: Int) {
        val itemFromDatabase = wishlistItemRepository.findByIdOrThrow(id)

        itemFromDatabase.walletTransaction?.let { transaction ->
            walletService.deleteWalletTransaction(transaction.id!!)
            logger.info("Deleted associated $transaction for $itemFromDatabase")
        }

        itemFromDatabase.creditCardDebt?.let { debt ->
            creditCardService.deleteDebt(debt.id!!)
            logger.info("Deleted associated $debt for $itemFromDatabase")
        }

        wishlistItemRepository.delete(itemFromDatabase)

        logger.info("$itemFromDatabase deleted successfully")
    }

    @Transactional
    fun markAsPurchasedWithWallet(
        id: Int,
        walletTransactionId: Int,
    ): WishlistItem {
        val itemFromDatabase = wishlistItemRepository.findByIdOrThrow(id)

        check(itemFromDatabase.isPending()) {
            "Wishlist item is already marked as purchased"
        }

        val transaction = walletService.getWalletTransactionById(walletTransactionId)

        itemFromDatabase.apply {
            status = WishlistItemStatus.PURCHASED
            purchasedAt = clockProvider.now()
            walletTransaction = transaction
            creditCardDebt = null
        }

        logger.info("$itemFromDatabase marked as purchased with wallet successfully")

        return itemFromDatabase
    }

    @Transactional
    fun markAsPurchasedWithCreditCard(
        id: Int,
        creditCardDebtId: Int,
    ): WishlistItem {
        val itemFromDatabase = wishlistItemRepository.findByIdOrThrow(id)

        check(itemFromDatabase.isPending()) {
            "Wishlist item is already marked as purchased"
        }

        val debt = creditCardService.getCreditCardDebtById(creditCardDebtId)

        itemFromDatabase.apply {
            status = WishlistItemStatus.PURCHASED
            purchasedAt = clockProvider.now()
            creditCardDebt = debt
            walletTransaction = null
        }

        logger.info("$itemFromDatabase marked as purchased with credit card successfully")

        return itemFromDatabase
    }

    @Transactional
    fun markAsPending(id: Int) {
        val itemFromDatabase = wishlistItemRepository.findByIdOrThrow(id)

        check(itemFromDatabase.isPurchased()) {
            "Wishlist item is already marked as pending"
        }

        itemFromDatabase.walletTransaction?.let { transaction ->
            walletService.deleteWalletTransaction(transaction.id!!)
            logger.info("Deleted and refunded $transaction for $itemFromDatabase")
        }

        itemFromDatabase.creditCardDebt?.let { debt ->
            creditCardService.deleteDebt(debt.id!!)
            logger.info("Deleted $debt for $itemFromDatabase")
        }

        itemFromDatabase.apply {
            status = WishlistItemStatus.PENDING
            purchasedAt = null
            walletTransaction = null
            creditCardDebt = null
        }

        logger.info("$itemFromDatabase marked as pending successfully")
    }

    fun getAllItems(): List<WishlistItem> = wishlistItemRepository.findAllByOrderByStatusAscPriorityDescTargetDateAsc()

    fun getLinksForItem(itemId: Int): List<WishlistItemLink> =
        wishlistItemLinkRepository.findAllByWishlistItemId(itemId)

    fun getItemCountByCategory(categoryId: Int): Int = wishlistItemRepository.countByCategoryId(categoryId)
}
