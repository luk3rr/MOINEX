package org.moinex.factory

import org.moinex.model.Category

object CategoryFactory {
    fun create(
        id: Int? = null,
        name: String = "Test Category",
        isArchived: Boolean = false,
    ): Category =
        Category(
            id,
            name,
            isArchived,
        )
}
