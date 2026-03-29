package org.moinex.factory.wishlist

import org.moinex.model.wishlist.WishlistItem
import org.moinex.model.wishlist.WishlistItemLink

object WishlistItemLinkFactory {
    fun create(
        id: Int? = null,
        url: String = "https://example.com",
        label: String? = "Test Link",
        wishlistItem: WishlistItem? = null,
    ): WishlistItemLink =
        WishlistItemLink(
            id = id,
            url = url,
            label = label,
            wishlistItem = wishlistItem,
        )
}
