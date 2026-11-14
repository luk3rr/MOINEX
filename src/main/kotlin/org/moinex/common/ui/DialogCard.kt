/*
 * Filename: DialogCard.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.geometry.Pos
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.Tooltip
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.moinex.common.constant.Files

/**
 * Widths of a dialog card.
 */
enum class DialogSize(
    val width: Double,
    val styleClass: String,
) {
    SMALL(SMALL_WIDTH, "dialog-size-small"),
    DEFAULT(DEFAULT_WIDTH, "dialog-size-default"),
    WIDE(WIDE_WIDTH, "dialog-size-wide"),
}

private const val SMALL_WIDTH = 340.0
private const val DEFAULT_WIDTH = 420.0
private const val WIDE_WIDTH = 560.0

/**
 * The standard dialog card: a header with the title and a close button, a scrollable body and an optional footer. The
 * header and the footer stay in place while the body scrolls.
 *
 * @param title text of the header
 * @param content body, usually the root of an FXML
 * @param size width of the card
 * @param closable whether the header shows the close button
 * @param closeTooltip tooltip of the close button
 * @param footer optional node fixed under the body, such as the action buttons of a confirmation
 * @param width width of the card when the content has its own natural width; defaults to the one of [size]
 * @param sizeKey names this dialog in [sizeStore], which remembers the size the user gave it by dragging the border
 * @param sizeStore where the size is kept; without it the size lasts only while the dialog is open
 */
class DialogCard(
    title: String,
    content: Node,
    val size: DialogSize = DialogSize.DEFAULT,
    closable: Boolean = true,
    closeTooltip: String? = null,
    footer: Node? = null,
    width: Double? = null,
    private val sizeKey: String? = null,
    private val sizeStore: DialogSizeStore? = null,
) : VBox() {
    /**
     * Node that receives the focus when the card opens; when null the first focusable node of the body does.
     */
    var initialFocus: Node? = null

    /**
     * What ESC does; when null it closes the card.
     */
    var onEscape: (() -> Unit)? = null

    private val closeButton: Button? =
        if (closable) {
            Button().apply {
                graphic = icon(Icons.REMOVE, CLOSE_ICON_SIZE)
                styleClass.add(CLOSE_STYLE)
                closeTooltip?.let { tooltip = Tooltip(it) }
                isFocusTraversable = false
                setOnAction { closeDialog() }
            }
        } else {
            null
        }

    private val defaultWidth: Double = width ?: size.width

    init {
        styleClass.addAll(CARD_STYLE, size.styleClass)
        stylesheets.add(DialogCard::class.java.getResource(Files.DIALOG_CSS)!!.toExternalForm())
        prefWidth = width ?: size.width
        maxWidth = width ?: size.width
        maxHeight = USE_PREF_SIZE

        val header =
            HBox(
                Label(title).apply {
                    styleClass.add(TITLE_STYLE)
                    isWrapText = true
                    maxWidth = Double.MAX_VALUE
                    HBox.setHgrow(this, Priority.ALWAYS)
                },
                *listOfNotNull(closeButton).toTypedArray(),
            ).apply {
                styleClass.add(HEADER_STYLE)
                alignment = Pos.CENTER_LEFT
            }

        DialogLayout.stretch(content)

        val body =
            ScrollPane(content).apply {
                styleClass.add(SCROLL_STYLE)
                isFitToWidth = true
                isFitToHeight = true
                hbarPolicy = ScrollPane.ScrollBarPolicy.AS_NEEDED
                isFocusTraversable = false
                VBox.setVgrow(this, Priority.ALWAYS)
            }

        children.addAll(header, body)

        footer?.let {
            children.add(
                HBox(it).apply {
                    styleClass.add(FOOTER_STYLE)
                    alignment = Pos.CENTER
                },
            )
        }

        minHeight = MIN_HEIGHT

        loadStoredSize()
        installResizing()
    }

    private var dragStart: DragStart? = null

    private class DragStart(
        val screenX: Double,
        val screenY: Double,
        val size: DialogResize.Size,
        val zone: DialogResize.Zone,
    )

    private fun applySize(size: DialogResize.Size) {
        prefWidth = size.width
        maxWidth = size.width
        prefHeight = size.height
        maxHeight = size.height
    }

    private fun loadStoredSize() {
        val key = sizeKey ?: return
        sizeStore?.load(key)?.let { applySize(it) }
    }

    private fun resetSize() {
        prefWidth = defaultWidth
        maxWidth = defaultWidth
        prefHeight = USE_COMPUTED_SIZE
        maxHeight = USE_PREF_SIZE
        sizeKey?.let { sizeStore?.clear(it) }
    }

    private fun availableArea(): DialogResize.Size {
        val area = parent as? Region ?: return DialogResize.Size(Double.MAX_VALUE, Double.MAX_VALUE)
        return DialogResize.Size(
            area.width - area.padding.left - area.padding.right,
            area.height - area.padding.top - area.padding.bottom,
        )
    }

    private fun installResizing() {
        addEventFilter(MouseEvent.MOUSE_MOVED) { event ->
            if (dragStart == null) {
                cursor = cursorFor(DialogResize.zoneAt(event.x, event.y, width, height))
            }
        }
        addEventFilter(MouseEvent.MOUSE_EXITED) { if (dragStart == null) cursor = null }
        addEventFilter(MouseEvent.MOUSE_PRESSED) { event ->
            val zone = DialogResize.zoneAt(event.x, event.y, width, height)
            if (zone != DialogResize.Zone.NONE && event.isPrimaryButtonDown) {
                event.consume()
                if (event.clickCount >= 2) {
                    resetSize()
                } else {
                    dragStart = DragStart(event.screenX, event.screenY, DialogResize.Size(width, height), zone)
                }
            }
        }
        addEventFilter(MouseEvent.MOUSE_DRAGGED) { event ->
            val start = dragStart ?: return@addEventFilter
            event.consume()
            applySize(
                DialogResize.resize(
                    start.size,
                    start.zone,
                    event.screenX - start.screenX,
                    event.screenY - start.screenY,
                    max = availableArea(),
                ),
            )
        }
        addEventFilter(MouseEvent.MOUSE_RELEASED) { event ->
            if (dragStart == null) return@addEventFilter
            event.consume()
            dragStart = null
            sizeKey?.let { sizeStore?.save(it, DialogResize.Size(width, height)) }
        }
    }

    private fun cursorFor(zone: DialogResize.Zone): Cursor? =
        when (zone) {
            DialogResize.Zone.NONE -> null
            DialogResize.Zone.N, DialogResize.Zone.S -> Cursor.N_RESIZE
            DialogResize.Zone.E, DialogResize.Zone.W -> Cursor.E_RESIZE
            DialogResize.Zone.NE, DialogResize.Zone.SW -> Cursor.NE_RESIZE
            DialogResize.Zone.NW, DialogResize.Zone.SE -> Cursor.NW_RESIZE
        }

    companion object {
        const val CARD_STYLE = "dialog-card"
        const val HEADER_STYLE = "dialog-header"
        const val TITLE_STYLE = "dialog-title"
        const val CLOSE_STYLE = "dialog-close"
        const val SCROLL_STYLE = "dialog-scroll"
        const val FOOTER_STYLE = "dialog-footer"
        const val SCROLLBAR_ALLOWANCE = 18.0
        private const val CLOSE_ICON_SIZE = 16
        private const val MIN_HEIGHT = 120.0
    }
}
