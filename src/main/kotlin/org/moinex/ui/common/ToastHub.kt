/*
 * Filename: ToastHub.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common

import org.moinex.model.enums.NotificationType

/**
 * Where the toasts of a [ToastHub] are drawn.
 */
interface ToastSurface {
    fun addToast(
        id: Int,
        spec: ToastSpec,
    )

    fun updateToastCount(
        id: Int,
        count: Int,
    )

    fun removeToast(id: Int)
}

/**
 * Shows the messages that used to be dialogs the user had to close: warnings and successes as toasts that go away by
 * themselves, errors as toasts that stay until closed. Nothing here is saved to the notification center.
 */
class ToastHub(
    private val surface: ToastSurface,
    maxVisible: Int = MAX_VISIBLE,
) {
    private val board = ToastBoard(maxVisible)

    fun show(
        type: NotificationType,
        title: String,
        message: String,
        sticky: Boolean,
    ) {
        when (val offer = board.offer(ToastSpec(type, title, message, sticky))) {
            is ToastBoard.Offer.Added -> {
                offer.evicted?.let { surface.removeToast(it.id) }
                surface.addToast(offer.entry.id, offer.entry.spec)
            }
            is ToastBoard.Offer.Repeated -> surface.updateToastCount(offer.entry.id, offer.entry.count)
            ToastBoard.Offer.Queued -> Unit
        }
    }

    /**
     * Closes every toast on screen.
     */
    fun clear() {
        board.visibleIds.forEach { surface.removeToast(it) }
    }

    /**
     * Called by the surface when the toast [id] is gone, so a toast that was waiting for room can take its place.
     */
    fun dismissed(id: Int) {
        board.remove(id)?.let { surface.addToast(it.id, it.spec) }
    }

    companion object {
        const val MAX_VISIBLE = 5
    }
}
