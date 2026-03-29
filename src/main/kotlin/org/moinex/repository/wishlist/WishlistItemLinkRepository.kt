/*
 * Filename: WishlistItemLinkRepository.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.wishlist

import org.moinex.model.wishlist.WishlistItemLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WishlistItemLinkRepository : JpaRepository<WishlistItemLink, Int> {
    fun findAllByWishlistItemId(wishlistItemId: Int): List<WishlistItemLink>

    fun deleteAllByWishlistItemId(wishlistItemId: Int)
}
