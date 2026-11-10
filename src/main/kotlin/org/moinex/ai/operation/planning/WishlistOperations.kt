/*
 * Filename: WishlistOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.planning

import org.json.JSONArray
import org.json.JSONObject
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.BaseOperation
import org.moinex.ai.operation.CardInfo
import org.moinex.ai.operation.CategoryResolver
import org.moinex.ai.operation.DraftChoices
import org.moinex.ai.operation.DraftField
import org.moinex.ai.operation.DraftFieldType
import org.moinex.ai.operation.DraftValues
import org.moinex.ai.operation.EntityCatalog
import org.moinex.ai.operation.OperationDomain
import org.moinex.ai.operation.OperationRisk
import org.moinex.ai.operation.Preview
import org.moinex.ai.operation.ValidationResult
import org.moinex.ai.operation.cardMoney
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.model.wishlist.WishlistItem
import org.moinex.service.wishlist.WishlistService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/** A wishlist item to create or, when [itemId] is set, to change; its links and status are kept. */
data class WishlistItemPayload(
    val itemId: Int?,
    val title: String,
    val estimatedPrice: BigDecimal,
    val targetDate: LocalDate?,
    val priority: WishlistItemPriority,
    val notes: String,
    val categoryName: String,
    val before: String? = null,
    val keptCategory: String? = null,
)

private fun String?.toPriority(default: WishlistItemPriority) =
    WishlistItemPriority.entries.firstOrNull { it.name.equals(this, ignoreCase = true) } ?: default

/** Shared by the wishlist creation and change operations. */
abstract class WishlistOperation(
    protected val wishlistService: WishlistService,
    protected val categoryResolver: CategoryResolver,
) : BaseOperation<WishlistItemPayload>() {
    override val domain = OperationDomain.PLANNING

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "title" to property("string", "For wishlist items: what the user wants."),
            "priority" to
                property("string", "For wishlist items.")
                    .put("enum", JSONArray(WishlistItemPriority.entries.map { it.name.lowercase() })),
            "notes" to property("string", "For wishlist items: free notes."),
            "itemTitle" to property("string", "For update_wishlist_item: the current title of the item to change."),
        )

    override fun validate(payload: WishlistItemPayload): ValidationResult =
        when {
            payload.title.isBlank() -> ValidationResult.error("title is required")
            payload.estimatedPrice.signum() < 0 -> ValidationResult.error("price cannot be negative")
            payload.categoryName.isBlank() -> ValidationResult.error("categoryName is required")
            else -> ValidationResult.OK
        }

    override fun categoryNeeded(payload: WishlistItemPayload) =
        payload.categoryName.takeIf { it.isNotBlank() && !it.equals(payload.keptCategory, ignoreCase = true) }

    override fun card(payload: WishlistItemPayload) = CardInfo(payload.title, listOf(cardMoney(payload.estimatedPrice)))

    override fun summary(payload: WishlistItemPayload) =
        "${payload.title}, ${payload.estimatedPrice.toPlainString()}, ${payload.priority.name.lowercase()}"

    override fun draftSpec() =
        listOf(
            DraftField("title", "aidraft.field.title", DraftFieldType.TEXT),
            DraftField("estimatedPrice", "aidraft.field.amount", DraftFieldType.DECIMAL),
            DraftField("targetDate", "aidraft.field.targetDate", DraftFieldType.DATE, required = false),
            DraftField("priority", "aidraft.field.priority", DraftFieldType.TEXT),
            DraftField("notes", "aidraft.field.notes", DraftFieldType.TEXT, required = false),
            DraftField("categoryName", "aidraft.field.category", DraftFieldType.CHOICE, DraftChoices.CATEGORIES),
        )

    override fun toDraft(payload: WishlistItemPayload) =
        DraftValues(
            name,
            mapOf(
                "itemId" to payload.itemId,
                "title" to payload.title,
                "estimatedPrice" to payload.estimatedPrice,
                "targetDate" to payload.targetDate,
                "priority" to payload.priority.name.lowercase(),
                "notes" to payload.notes,
                "categoryName" to payload.categoryName,
            ),
        )

    override fun fromDraft(draft: DraftValues): WishlistItemPayload? {
        val fields = draft.fields
        return WishlistItemPayload(
            itemId = fields["itemId"].asInt(),
            title = (fields["title"] as? String)?.trim() ?: return null,
            estimatedPrice = fields["estimatedPrice"].asDecimal() ?: return null,
            targetDate = fields["targetDate"].asDate(),
            priority = (fields["priority"] as? String).toPriority(WishlistItemPriority.MEDIUM),
            notes = fields["notes"] as? String ?: "",
            categoryName = fields["categoryName"] as? String ?: "",
        )
    }
}

@Component
class CreateWishlistItemOperation(
    wishlistService: WishlistService,
    categoryResolver: CategoryResolver,
) : WishlistOperation(wishlistService, categoryResolver) {
    override val name = "create_wishlist_item"
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("title", "amount", "categoryName")

    override fun modelDescription() =
        "create_wishlist_item: something the user wants to buy later (title, amount as the estimated price, " +
            "categoryName, optional priority, targetDate and notes)."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): WishlistItemPayload {
        val category = arguments.getString("categoryName")
        return WishlistItemPayload(
            itemId = null,
            title = arguments.getString("title").trim(),
            estimatedPrice = arguments.getBigDecimal("amount"),
            targetDate = arguments.optString("targetDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
            priority = arguments.optString("priority").toPriority(WishlistItemPriority.MEDIUM),
            notes = arguments.optString("notes"),
            categoryName = catalog.category(category)?.name ?: category,
        )
    }

    override fun execute(payload: WishlistItemPayload): ApplyResult.Success {
        val id =
            wishlistService.createItem(
                WishlistItem(
                    title = payload.title,
                    estimatedPrice = payload.estimatedPrice,
                    targetDate = payload.targetDate,
                    priority = payload.priority,
                    notes = payload.notes.takeIf { it.isNotBlank() },
                    category = categoryResolver.resolveOrCreate(payload.categoryName),
                    createdAt = LocalDateTime.now(),
                ),
                emptyList(),
            )
        return ApplyResult.Success(listOf(id), summary(payload))
    }
}

@Component
class UpdateWishlistItemOperation(
    wishlistService: WishlistService,
    categoryResolver: CategoryResolver,
) : WishlistOperation(wishlistService, categoryResolver) {
    override val name = "update_wishlist_item"
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("itemTitle")

    override fun modelDescription() =
        "update_wishlist_item: change a wishlist item (itemTitle is the current title). Send only the fields to " +
            "change; its links and status are kept."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): WishlistItemPayload {
        val current = arguments.getString("itemTitle")
        val item =
            wishlistService.getAllItems().firstOrNull { it.title.equals(current.trim(), ignoreCase = true) }
                ?: error("wishlist item '$current' not found")
        val stored =
            WishlistItemPayload(
                item.id,
                item.title,
                item.estimatedPrice,
                item.targetDate,
                item.priority,
                item.notes.orEmpty(),
                item.category.name,
            )
        val category = arguments.optString("categoryName")
        return stored.copy(
            title = arguments.optString("title").trim().ifBlank { stored.title },
            estimatedPrice = if (arguments.has("amount")) arguments.getBigDecimal("amount") else stored.estimatedPrice,
            targetDate =
                arguments.optString("targetDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    ?: stored.targetDate,
            priority = arguments.optString("priority").toPriority(stored.priority),
            notes = if (arguments.has("notes")) arguments.getString("notes") else stored.notes,
            categoryName =
                if (category.isBlank()) stored.categoryName else catalog.category(category)?.name ?: category,
            before = summary(stored),
            keptCategory = stored.categoryName,
        )
    }

    override fun preview(payload: WishlistItemPayload) =
        payload.before?.let { Preview(listOf(it, "→ " + summary(payload))) }

    override fun execute(payload: WishlistItemPayload): ApplyResult.Success {
        val stored = wishlistService.getAllItems().first { it.id == payload.itemId }
        wishlistService.updateItem(
            WishlistItem(
                id = stored.id,
                title = payload.title,
                estimatedPrice = payload.estimatedPrice,
                targetDate = payload.targetDate,
                status = stored.status,
                priority = payload.priority,
                notes = payload.notes.takeIf { it.isNotBlank() },
                category =
                    stored.category.takeIf { it.name.equals(payload.categoryName, ignoreCase = true) }
                        ?: categoryResolver.resolveOrCreate(payload.categoryName),
                createdAt = stored.createdAt,
            ),
            wishlistService.getLinksForItem(stored.id!!),
        )
        return ApplyResult.Success(listOf(payload.itemId!!), summary(payload))
    }
}
