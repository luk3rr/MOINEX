/*
 * Filename: DialogExtensions.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.scene.Node
import javafx.stage.Stage

/**
 * Key under which a hosted dialog card publishes the action that closes it.
 */
const val DIALOG_CLOSE_KEY: String = "moinex.dialog.close"

/**
 * Closes the dialog that contains this node. Inside an overlay it closes the [DialogCard] (and any dialog opened above
 * it); in a window of its own, such as the calculator, it closes that window.
 */
fun Node.closeDialog() {
    var node: Node? = this
    while (node != null) {
        val action = node.properties[DIALOG_CLOSE_KEY] as? Runnable
        if (action != null) {
            action.run()
            return
        }
        node = node.parent
    }

    (scene?.window as? Stage)?.close()
}

/**
 * Makes the window of a dialog fit its content again after the content grew or shrank. A dialog shown as an overlay
 * resizes with its content by itself, and its window is the main one, so there is nothing to do for it.
 */
fun Node.refitDialogWindow() {
    var node: Node? = this
    while (node != null) {
        if (node.properties.containsKey(DIALOG_CLOSE_KEY)) return
        node = node.parent
    }

    scene?.window?.sizeToScene()
}
