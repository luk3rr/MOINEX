/*
 * Filename: AddWishlistItemController.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wishlist

import javafx.fxml.FXML
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.model.wishlist.WishlistItem
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.wishlist.WishlistService
import org.moinex.ui.dialog.wishlist.base.BaseWishlistManagement
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
class AddWishlistItemController(
    categoryService: CategoryService,
    preferencesService: PreferencesService,
    private val wishlistService: WishlistService,
) : BaseWishlistManagement(categoryService, preferencesService) {
    @FXML
    override fun initialize() {
        super.initialize()
        links.clear()
        updateTableView()
    }

    @FXML
    override fun handleSave() {
        val formData = getFieldsFromInterface()

        if (!formData.isValid()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_DIALOG_EMPTY_FIELDS_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_DIALOG_EMPTY_FIELDS_MESSAGE),
            )
            return
        }

        runCatching {
            val estimatedPrice = formData.estimatedPriceStr!!.toBigDecimal()

            val newItem =
                WishlistItem(
                    title = formData.title,
                    estimatedPrice = estimatedPrice,
                    targetDate = formData.targetDate,
                    category = formData.category!!,
                    priority = formData.priority!!,
                    notes = formData.notes,
                    createdAt = LocalDateTime.now(),
                )

            wishlistService.createItem(newItem, formData.links)

            WindowUtils.showSuccessDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_DIALOG_CREATED_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_DIALOG_CREATED_MESSAGE),
            )

            (titleField.scene.window as Stage).close()
        }.onFailure { e ->
            when (e) {
                is NumberFormatException -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.WISHLIST_DIALOG_INVALID_PRICE_TITLE),
                        preferencesService.translate(TranslationKeys.WISHLIST_DIALOG_INVALID_PRICE_MESSAGE),
                    )
                }
                else -> {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(TranslationKeys.WISHLIST_DIALOG_ERROR_CREATING_TITLE),
                        e.message ?: "Unknown error",
                    )
                }
            }
        }
    }
}
