package org.moinex.ui.common.search

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.icon

/**
 * Overlay over the main window that filters [actions] as the user types and runs the chosen one.
 */
class GlobalSearch(
    private val rootPane: AnchorPane,
    private val actions: () -> List<SearchAction>,
    private val translate: (String) -> String,
) {
    private val input =
        TextField().apply {
            promptText = translate(TranslationKeys.SEARCH_PLACEHOLDER)
            styleClass.add("search-input")
        }

    private val results =
        ListView<SearchAction>().apply {
            styleClass.add("search-results")
            prefHeight = RESULTS_HEIGHT
            maxHeight = RESULTS_HEIGHT
            placeholder = Label(translate(TranslationKeys.SEARCH_EMPTY))
            setCellFactory { SearchCell() }
            setOnMouseClicked { if (it.clickCount >= 1) runSelected() }
        }

    private val panel =
        VBox(
            input,
            results,
            Label(
                listOf(
                    TranslationKeys.SEARCH_HINT_NAVIGATE,
                    TranslationKeys.SEARCH_HINT_EXECUTE,
                    TranslationKeys.SEARCH_HINT_CLOSE,
                ).joinToString("    ") { translate(it) },
            ).apply { styleClass.add("search-hint") },
        ).apply {
            styleClass.add("search-overlay")
            maxWidth = PANEL_WIDTH
            maxHeight = Region.USE_PREF_SIZE
            spacing = PANEL_SPACING
        }

    private val overlay =
        StackPane(panel).apply {
            styleClass.add("search-scrim")
            alignment = Pos.TOP_CENTER
            padding = Insets(TOP_OFFSET, 0.0, 0.0, 0.0)
            isVisible = false
            setOnMouseClicked { if (it.target === this) close() }
        }

    val isOpen: Boolean get() = overlay.isVisible

    init {
        rootPane.children.add(overlay)
        AnchorPane.setTopAnchor(overlay, 0.0)
        AnchorPane.setBottomAnchor(overlay, 0.0)
        AnchorPane.setLeftAnchor(overlay, 0.0)
        AnchorPane.setRightAnchor(overlay, 0.0)

        input.textProperty().addListener { _, _, _ -> refresh() }
        input.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            when (event.code) {
                KeyCode.DOWN -> move(1, event)
                KeyCode.UP -> move(-1, event)
                KeyCode.ENTER -> {
                    event.consume()
                    runSelected()
                }
                KeyCode.ESCAPE -> {
                    event.consume()
                    close()
                }
                else -> Unit
            }
        }
    }

    fun toggle() {
        if (isOpen) close() else open()
    }

    fun open() {
        input.clear()
        refresh()
        overlay.isVisible = true
        overlay.toFront()
        Platform.runLater { input.requestFocus() }
    }

    fun close() {
        overlay.isVisible = false
    }

    private fun refresh() {
        results.items.setAll(SearchMatcher.rank(input.text.orEmpty(), actions(), translate))
        results.selectionModel.selectFirst()
    }

    private fun move(
        step: Int,
        event: KeyEvent,
    ) {
        event.consume()
        val size = results.items.size
        if (size == 0) return
        val next = (results.selectionModel.selectedIndex + step + size) % size
        results.selectionModel.select(next)
        results.scrollTo(next)
    }

    private fun runSelected() {
        val action = results.selectionModel.selectedItem ?: return
        close()
        Platform.runLater { action.run() }
    }

    private inner class SearchCell : ListCell<SearchAction>() {
        init {
            styleClass.add("search-item")
        }

        override fun updateItem(
            item: SearchAction?,
            empty: Boolean,
        ) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                graphic = null
                text = null
                return
            }
            val spacer = Region().also { HBox.setHgrow(it, Priority.ALWAYS) }
            graphic =
                HBox(
                    icon(item.icon, ICON_SIZE),
                    Label(item.label ?: translate(item.titleKey)),
                    spacer,
                    Label(translate(item.group.titleKey)).apply { styleClass.add("search-group") },
                ).apply {
                    alignment = Pos.CENTER_LEFT
                    spacing = ITEM_SPACING
                }
        }
    }

    companion object {
        private const val PANEL_WIDTH = 560.0
        private const val PANEL_SPACING = 8.0
        private const val RESULTS_HEIGHT = 360.0
        private const val TOP_OFFSET = 110.0
        private const val ICON_SIZE = 20
        private const val ITEM_SPACING = 12.0
    }
}
