/*
 * Filename: CategoryResolver.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import org.moinex.model.Category
import org.moinex.service.CategoryService
import org.springframework.stereotype.Component

/** Finds a category by name and creates it when it does not exist yet, as the review screens always did. */
@Component
class CategoryResolver(
    private val categoryService: CategoryService,
) {
    fun resolveOrCreate(name: String): Category {
        categoryService
            .getNonArchivedCategoriesOrderedByName()
            .firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.let { return it }

        val newId = categoryService.createCategory(Category(name = name))
        return categoryService.getCategories().first { it.id == newId }
    }
}
