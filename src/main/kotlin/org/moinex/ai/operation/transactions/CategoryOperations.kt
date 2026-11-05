/*
 * Filename: CategoryOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.transactions

import org.json.JSONObject
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.BaseOperation
import org.moinex.ai.operation.DraftField
import org.moinex.ai.operation.DraftFieldType
import org.moinex.ai.operation.DraftValues
import org.moinex.ai.operation.EntityCatalog
import org.moinex.ai.operation.OperationDomain
import org.moinex.ai.operation.OperationRisk
import org.moinex.ai.operation.Preview
import org.moinex.ai.operation.ValidationResult
import org.moinex.model.Category
import org.moinex.service.CategoryService
import org.springframework.stereotype.Component

data class CategoryPayload(
    val name: String,
)

/** [categoryId] is null while the model's category name matched no category. */
data class UpdateCategoryPayload(
    val categoryId: Int?,
    val currentName: String,
    val name: String,
)

data class ArchiveCategoryPayload(
    val categoryId: Int?,
    val currentName: String,
)

private const val NAME_MAX_LENGTH = 50

@Component
class CreateCategoryOperation(
    private val categoryService: CategoryService,
) : BaseOperation<CategoryPayload>() {
    override val name = "create_category"
    override val domain = OperationDomain.MAINTENANCE
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("name")

    override fun modelDescription() =
        "create_category: a new transaction category, only when the user asks for one; proposing a transaction " +
            "with an unknown categoryName already creates the category card for you."

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf("name" to property("string", "The new name, for create_category, update_category and create_wallet."))

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ) = CategoryPayload(arguments.getString("name").trim())

    override fun validate(payload: CategoryPayload): ValidationResult =
        when {
            payload.name.isBlank() -> ValidationResult.error("name is required")
            payload.name.length > NAME_MAX_LENGTH -> ValidationResult.error("name is too long")
            categoryService.getCategories().any { it.name.equals(payload.name, ignoreCase = true) } ->
                ValidationResult.error("category '${payload.name}' already exists")
            else -> ValidationResult.OK
        }

    override fun summary(payload: CategoryPayload) = payload.name

    override fun draftSpec() = listOf(DraftField("name", "aidraft.field.name", DraftFieldType.TEXT))

    override fun toDraft(payload: CategoryPayload) = DraftValues(name, mapOf("name" to payload.name))

    override fun fromDraft(draft: DraftValues) = (draft.fields["name"] as? String)?.let { CategoryPayload(it.trim()) }

    override fun execute(payload: CategoryPayload): ApplyResult.Success {
        val id = categoryService.createCategory(Category(name = payload.name))
        return ApplyResult.Success(listOf(id), payload.name)
    }
}

@Component
class UpdateCategoryOperation(
    private val categoryService: CategoryService,
) : BaseOperation<UpdateCategoryPayload>() {
    override val name = "update_category"
    override val domain = OperationDomain.MAINTENANCE
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("categoryName", "name")

    override fun modelDescription() =
        "update_category: rename an existing category (categoryName is the current name, name the new one)."

    override fun modelProperties(): Map<String, JSONObject> = emptyMap()

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): UpdateCategoryPayload {
        val current = arguments.getString("categoryName")
        val category = catalog.category(current)
        return UpdateCategoryPayload(category?.id, category?.name ?: current, arguments.getString("name").trim())
    }

    override fun validate(payload: UpdateCategoryPayload): ValidationResult =
        when {
            payload.categoryId == null -> ValidationResult.error("category '${payload.currentName}' not found")
            payload.name.isBlank() -> ValidationResult.error("name is required")
            payload.name.length > NAME_MAX_LENGTH -> ValidationResult.error("name is too long")
            else -> ValidationResult.OK
        }

    override fun summary(payload: UpdateCategoryPayload) = "${payload.currentName} → ${payload.name}"

    override fun preview(payload: UpdateCategoryPayload) = Preview(listOf(summary(payload)))

    override fun draftSpec() = listOf(DraftField("name", "aidraft.field.name", DraftFieldType.TEXT))

    override fun toDraft(payload: UpdateCategoryPayload) =
        DraftValues(
            name,
            mapOf("name" to payload.name, "categoryId" to payload.categoryId, "currentName" to payload.currentName),
        )

    override fun fromDraft(draft: DraftValues): UpdateCategoryPayload? {
        val fields = draft.fields
        return UpdateCategoryPayload(
            fields["categoryId"].asInt(),
            fields["currentName"] as? String ?: "",
            (fields["name"] as? String)?.trim() ?: return null,
        )
    }

    override fun execute(payload: UpdateCategoryPayload): ApplyResult.Success {
        val category = categoryService.getCategories().first { it.id == payload.categoryId }
        categoryService.renameCategory(Category(id = category.id, name = payload.name))
        return ApplyResult.Success(listOf(category.id!!), summary(payload))
    }
}

@Component
class ArchiveCategoryOperation(
    private val categoryService: CategoryService,
) : BaseOperation<ArchiveCategoryPayload>() {
    override val name = "archive_category"
    override val domain = OperationDomain.MAINTENANCE
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("categoryName")

    override fun modelDescription() =
        "archive_category: archive an existing category (categoryName); nothing is deleted."

    override fun modelProperties(): Map<String, JSONObject> = emptyMap()

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): ArchiveCategoryPayload {
        val current = arguments.getString("categoryName")
        val category = catalog.category(current)
        return ArchiveCategoryPayload(category?.id, category?.name ?: current)
    }

    override fun validate(payload: ArchiveCategoryPayload): ValidationResult =
        if (payload.categoryId == null) {
            ValidationResult.error("category '${payload.currentName}' not found")
        } else {
            ValidationResult.OK
        }

    override fun summary(payload: ArchiveCategoryPayload) = payload.currentName

    override fun execute(payload: ArchiveCategoryPayload): ApplyResult.Success {
        categoryService.archiveCategory(payload.categoryId!!)
        return ApplyResult.Success(listOf(payload.categoryId), payload.currentName)
    }
}
