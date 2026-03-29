/*
 * Filename: WishlistItemRepository.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.wishlist

import org.moinex.model.enums.WishlistItemStatus
import org.moinex.model.wishlist.WishlistItem
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface WishlistItemRepository : JpaRepository<WishlistItem, Int> {
    fun findAllByOrderByStatusAscPriorityDescTargetDateAsc(): List<WishlistItem>

    fun findAllByStatusOrderByPriorityDescTargetDateAsc(status: WishlistItemStatus): List<WishlistItem>

    @Query(
        "SELECT w " +
            "FROM WishlistItem w " +
            "WHERE w.category.id = :categoryId " +
            "ORDER BY w.priority DESC, w.targetDate ASC",
    )
    fun findAllByCategoryOrderByPriorityDesc(categoryId: Int): List<WishlistItem>

    fun countByCategoryId(categoryId: Int): Int
}
