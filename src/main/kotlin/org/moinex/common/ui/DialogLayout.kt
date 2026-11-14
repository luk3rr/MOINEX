/*
 * Filename: DialogLayout.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import javafx.scene.control.TextInputControl
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.kordamp.ikonli.javafx.FontIcon

/**
 * Makes the content of a dialog follow the size of its card. The FXML files were drawn for a window that was exactly as
 * big as its content, so in a card that is wider or taller, rows with an icon stay stuck to the left, a root with a
 * fixed maximum size stays put, and lists keep their height.
 */
object DialogLayout {
    /**
     * Lets [root] fill the card, the text fields of the rows that start with an icon take the free width, and the
     * tables and lists take the free height.
     */
    fun stretch(root: Node) {
        if (root is Region) {
            root.maxWidth = Double.MAX_VALUE
            root.maxHeight = Double.MAX_VALUE
        }
        growIconRows(root)
        growLists(root, root)
    }

    private fun growIconRows(node: Node) {
        if (node is HBox && node.children.any(::isIconHolder)) {
            node.children.filter(::holdsTextInput).forEach { child ->
                HBox.setHgrow(child, Priority.ALWAYS)
                (child as? Region)?.maxWidth = Double.MAX_VALUE
            }
        }
        (node as? Parent)?.childrenUnmodifiable?.forEach(::growIconRows)
    }

    private fun growLists(
        node: Node,
        root: Node,
    ) {
        if (node is TableView<*> || node is ListView<*>) {
            var current: Node? = node
            while (current != null && current !== root.parent) {
                if (current.parent is VBox) VBox.setVgrow(current, Priority.ALWAYS)
                if (current is Region && current !== root) current.maxHeight = Double.MAX_VALUE
                current = current.parent
            }
        }
        (node as? Parent)?.childrenUnmodifiable?.forEach { growLists(it, root) }
    }

    private fun isIconHolder(node: Node): Boolean =
        !holdsTextInput(node) && (node is FontIcon || (node is Parent && node.childrenUnmodifiable.any(::isIconHolder)))

    private fun holdsTextInput(node: Node): Boolean =
        node is TextInputControl || (node is Parent && node.childrenUnmodifiable.any(::holdsTextInput))
}
