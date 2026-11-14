/*
 * Filename: DialogStack.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

/**
 * Pure stack of open dialogs, kept apart from JavaFX so the open and close rules can be tested without a display.
 */
class DialogStack<T : Any> {
    private val items = mutableListOf<T>()

    val depth: Int get() = items.size

    val top: T? get() = items.lastOrNull()

    fun isEmpty(): Boolean = items.isEmpty()

    fun contains(item: T): Boolean = items.any { it === item }

    fun isTop(item: T): Boolean = items.lastOrNull() === item

    fun push(item: T) {
        check(!contains(item)) { "Dialog is already open" }
        items.add(item)
    }

    /**
     * Removes [item] and every dialog opened above it, and returns them from the top down, the order in which they
     * must be torn down. Returns an empty list when [item] is not open, so closing twice does nothing.
     */
    fun closeFrom(item: T): List<T> {
        val index = items.indexOfFirst { it === item }
        if (index < 0) return emptyList()

        val removed = items.subList(index, items.size).reversed()
        items.subList(index, items.size).clear()
        return removed
    }
}
