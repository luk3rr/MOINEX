/*
 * Filename: WishlistItemLink.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.wishlist

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "wishlist_item_link")
class WishlistItemLink(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "url", nullable = false, length = 500)
    var url: String,
    @Column(name = "label", length = 100)
    var label: String? = null,
    @ManyToOne
    @JoinColumn(name = "wishlist_item_id", nullable = false)
    var wishlistItem: WishlistItem? = null,
) {
    init {
        url = url.trim()
        require(url.isNotEmpty()) {
            "Link URL cannot be empty"
        }

        label = label?.trim()
    }

    override fun toString(): String = "WishlistItemLink [id=$id, url='$url', label='$label']"
}
