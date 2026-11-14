/*
 * Filename: DialogHost.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.StackPane
import org.moinex.common.constant.Files

/**
 * Shows dialogs as overlays over the main window, stacked one above the other, each on its own scrim.
 *
 * [show] blocks the caller until the dialog closes, like `showAndWait` of a window, by running a nested event loop, so
 * callers keep their sequential code. [present] opens a dialog without blocking.
 */
class DialogHost(
    private val rootPane: AnchorPane,
) {
    private class Level(
        val card: DialogCard,
        val scrim: StackPane,
        val previousFocus: Node?,
        val onHidden: List<Runnable>,
    ) {
        var blocking: Boolean = false
    }

    private val stack = DialogStack<Level>()
    private val alwaysOnTop = mutableListOf<Node>()
    private val activeLoops = ArrayDeque<Level>()
    private val pendingExit = mutableSetOf<Level>()
    private var leavingLoop: Level? = null

    val depth: Int get() = stack.depth

    val isOpen: Boolean get() = !stack.isEmpty()

    /**
     * Keeps [node], such as the toast stack, above every dialog.
     */
    fun keepOnTop(node: Node) {
        alwaysOnTop.add(node)
    }

    /**
     * Opens [card] and returns only after it, and every dialog opened above it, has closed.
     */
    fun show(
        card: DialogCard,
        onHidden: List<Runnable> = emptyList(),
    ) {
        val level = open(card, onHidden)
        level.blocking = true
        activeLoops.addLast(level)
        try {
            Platform.enterNestedEventLoop(level)
        } catch (e: RuntimeException) {
            level.blocking = false
            activeLoops.remove(level)
            close(card)
            throw e
        }
        activeLoops.remove(level)
        pendingExit.remove(level)
        leavingLoop = null
        releaseInnermostLoop()
    }

    /**
     * Opens [card] and returns at once.
     */
    fun present(
        card: DialogCard,
        onHidden: List<Runnable> = emptyList(),
    ) {
        open(card, onHidden)
    }

    /**
     * Closes every open dialog, from the top down.
     */
    fun closeAll() {
        rootPane.children
            .mapNotNull { it.properties[LEVEL_KEY] as? Level }
            .firstOrNull()
            ?.let { close(it.card) }
    }

    /**
     * Closes [card] and the dialogs above it, top first. Does nothing when it is not open.
     */
    fun close(card: DialogCard) {
        val target = findLevel(card)
        if (target == null) return
        val removed = stack.closeFrom(target)

        removed.forEach { level ->
            rootPane.children.remove(level.scrim)
            level.card.properties.remove(DIALOG_CLOSE_KEY)
        }

        target.previousFocus?.takeIf { it.scene != null }?.requestFocus()

        removed.forEach { level ->
            level.onHidden.forEach(Runnable::run)
            if (level.blocking) pendingExit.add(level)
        }
        releaseInnermostLoop()
    }

    /**
     * Nested loops can only be left from the innermost one, and once at a time. A close marks the loops of the dialogs
     * it removed; this leaves the innermost marked loop, and each [show] that returns calls it again for the next.
     */
    private fun releaseInnermostLoop() {
        val innermost = activeLoops.lastOrNull() ?: return
        if (leavingLoop != null || innermost !in pendingExit) return

        leavingLoop = innermost
        Platform.exitNestedEventLoop(innermost, null)
    }

    private fun findLevel(card: DialogCard): Level? =
        rootPane.children.mapNotNull { it.properties[LEVEL_KEY] as? Level }.firstOrNull { it.card === card }

    private fun open(
        card: DialogCard,
        onHidden: List<Runnable>,
    ): Level {
        val scrim =
            StackPane(card).apply {
                styleClass.add(if (stack.isEmpty()) SCRIM_STYLE else SCRIM_STACKED_STYLE)
                alignment = Pos.CENTER
                padding = Insets(SCRIM_PADDING)
            }
        scrim.stylesheets.add(DialogHost::class.java.getResource(Files.DIALOG_CSS)!!.toExternalForm())
        val level = Level(card, scrim, rootPane.scene?.focusOwner, onHidden)
        scrim.properties[LEVEL_KEY] = level
        card.properties[DIALOG_CLOSE_KEY] = Runnable { close(card) }

        scrim.addEventFilter(KeyEvent.KEY_PRESSED) { event -> handleKey(level, event) }

        stack.push(level)
        rootPane.children.add(scrim)
        AnchorPane.setTopAnchor(scrim, 0.0)
        AnchorPane.setBottomAnchor(scrim, 0.0)
        AnchorPane.setLeftAnchor(scrim, 0.0)
        AnchorPane.setRightAnchor(scrim, 0.0)
        scrim.toFront()
        alwaysOnTop.forEach(Node::toFront)

        Platform.runLater { focusInitial(level) }
        return level
    }

    private fun handleKey(
        level: Level,
        event: KeyEvent,
    ) {
        if (!stack.isTop(level)) return

        when (event.code) {
            KeyCode.ESCAPE -> {
                event.consume()
                level.card.onEscape?.invoke() ?: close(level.card)
            }
            KeyCode.TAB -> {
                event.consume()
                moveFocus(level, if (event.isShiftDown) -1 else 1)
            }
            else -> Unit
        }
    }

    private fun focusInitial(level: Level) {
        if (!stack.contains(level)) return
        val target = level.card.initialFocus ?: focusables(level.card).firstOrNull()
        target?.requestFocus()
    }

    private fun moveFocus(
        level: Level,
        step: Int,
    ) {
        val nodes = focusables(level.card)
        if (nodes.isEmpty()) return

        val current =
            nodes.indexOfFirst {
                it.isFocused ||
                    (it is Parent && it.childrenUnmodifiable.any(Node::isFocused))
            }
        val next = if (current < 0) 0 else (current + step + nodes.size) % nodes.size
        nodes[next].requestFocus()
    }

    private fun focusables(root: Node): List<Node> {
        if (!root.isVisible || root.isDisabled) return emptyList()
        val own = if (root.isFocusTraversable) listOf(root) else emptyList()
        val inner = (root as? Parent)?.childrenUnmodifiable?.flatMap(::focusables).orEmpty()
        return own + inner
    }

    companion object {
        const val SCRIM_STYLE = "dialog-scrim"
        const val SCRIM_STACKED_STYLE = "dialog-scrim-stacked"
        private const val LEVEL_KEY = "moinex.dialog.level"
        private const val SCRIM_PADDING = 16.0
    }
}
