/*
 * Filename: Category.kt (original filename: Category.java)
 * Created on: March 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "category")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "name", nullable = false, length = 50)
    var name: String,
    @Column(name = "archived", nullable = false)
    var isArchived: Boolean = false,
) {
    init {
        name = name.trim()
        require(name.isNotEmpty()) {
            "Category name cannot be empty"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val category = other as Category
        return id != null && id == category.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: javaClass.hashCode()

    override fun toString(): String = "Category [id=$id, name='$name']"
}
