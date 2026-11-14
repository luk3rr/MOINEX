/*
 * Filename: ToastBoard.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import org.moinex.model.enums.NotificationType

/**
 * What a toast shows. [sticky] toasts stay until the user closes them; the others go away by themselves.
 */
data class ToastSpec(
    val type: NotificationType,
    val title: String,
    val message: String,
    val sticky: Boolean,
) {
    internal val key: Triple<NotificationType, String, String> get() = Triple(type, title, message)
}

/**
 * Pure bookkeeping of the toasts on screen, kept apart from JavaFX so the grouping and the limit can be tested: the
 * same toast shown again while visible is counted instead of stacked, and when the board is full the oldest toast that
 * goes away by itself makes room, never a sticky one.
 */
class ToastBoard(
    private val maxVisible: Int,
) {
    class Entry(
        val id: Int,
        val spec: ToastSpec,
    ) {
        var count: Int = 1
            internal set
    }

    sealed interface Offer {
        data class Added(
            val entry: Entry,
            val evicted: Entry?,
        ) : Offer

        data class Repeated(
            val entry: Entry,
        ) : Offer

        data object Queued : Offer
    }

    private val visible = mutableListOf<Entry>()
    private val waiting = ArrayDeque<ToastSpec>()
    private var nextId = 0

    val visibleCount: Int get() = visible.size

    val waitingCount: Int get() = waiting.size

    val visibleIds: List<Int> get() = visible.map { it.id }

    fun offer(spec: ToastSpec): Offer {
        visible.firstOrNull { it.spec.key == spec.key }?.let {
            it.count++
            return Offer.Repeated(it)
        }

        var evicted: Entry? = null
        if (visible.size >= maxVisible) {
            evicted = visible.firstOrNull { !it.spec.sticky }
            if (evicted == null) {
                waiting.addLast(spec)
                return Offer.Queued
            }
            visible.remove(evicted)
        }

        val entry = Entry(nextId++, spec)
        visible.add(entry)
        return Offer.Added(entry, evicted)
    }

    /**
     * Removes the toast [id] and returns the one that was waiting for room, already added to the board, if any.
     */
    fun remove(id: Int): Entry? {
        if (!visible.removeIf { it.id == id }) return null
        val next = waiting.removeFirstOrNull() ?: return null
        val entry = Entry(nextId++, next)
        visible.add(entry)
        return entry
    }
}
