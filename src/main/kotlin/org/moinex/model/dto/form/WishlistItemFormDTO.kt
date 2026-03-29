/*
 * Filename: WishlistItemFormDTO.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto.form

import org.moinex.model.Category
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.model.wishlist.WishlistItemLink
import java.time.LocalDate

data class WishlistItemFormDTO(
    val title: String,
    val estimatedPriceStr: String?,
    val targetDate: LocalDate?,
    val category: Category?,
    val priority: WishlistItemPriority?,
    val notes: String?,
    val links: List<WishlistItemLink>,
) {
    fun isValid(): Boolean =
        title.isNotBlank() &&
            !estimatedPriceStr.isNullOrBlank() &&
            category != null &&
            priority != null
}
