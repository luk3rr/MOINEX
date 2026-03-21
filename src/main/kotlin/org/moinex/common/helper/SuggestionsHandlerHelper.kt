package org.moinex.common.helper

import javafx.beans.value.ChangeListener
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.Popup
import org.moinex.common.constant.Constants

class SuggestionsHandlerHelper<T>(
    private val componentField: TextField,
    private val filterFunction: (T) -> String,
    private val displayFunction: (T) -> String,
    private val onSelectCallback: (T) -> Unit,
) {
    private val suggestionListView = ListView<T>()
    private val suggestionsPopup = Popup()
    private lateinit var componentFieldListener: ChangeListener<String>

    var suggestions: List<T> = emptyList()

    init {
        configureListeners()
        configureSuggestionsListView()
        configureSuggestionsPopup()
    }

    fun disable() {
        componentField.textProperty().removeListener(componentFieldListener)
    }

    fun enable() {
        componentField.textProperty().addListener(componentFieldListener)
    }

    private fun configureListeners() {
        componentFieldListener =
            ChangeListener { _, _, newValue ->
                if (newValue.isBlank()) {
                    suggestionsPopup.hide()
                    return@ChangeListener
                }

                suggestionListView.items.clear()

                val filteredSuggestions =
                    suggestions
                        .filter { item ->
                            filterFunction(item)
                                .lowercase()
                                .contains(newValue.lowercase())
                        }.take(Constants.SUGGESTIONS_MAX_ITEMS)

                suggestionListView.items.addAll(filteredSuggestions)

                if (filteredSuggestions.isNotEmpty()) {
                    adjustPopupWidth()
                    adjustPopupHeight()
                    showPopup()
                } else {
                    suggestionsPopup.hide()
                }
            }
    }

    private fun configureSuggestionsListView() {
        suggestionListView.apply {
            setCellFactory {
                object : ListCell<T>() {
                    override fun updateItem(
                        item: T?,
                        empty: Boolean,
                    ) {
                        super.updateItem(item, empty)

                        graphic =
                            if (empty || item == null) {
                                null
                            } else {
                                VBox().apply {
                                    spacing = 2.0
                                    children.add(Label(displayFunction(item)))
                                }
                            }
                    }
                }
            }

            prefWidth = Region.USE_COMPUTED_SIZE
            prefHeight = Region.USE_COMPUTED_SIZE

            addEventFilter(KeyEvent.KEY_PRESSED) { event ->
                if (event.code == KeyCode.SPACE) {
                    event.consume()
                }
            }

            selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                newValue?.let {
                    onSelectCallback(it)
                    suggestionsPopup.hide()
                }
            }
        }
    }

    private fun configureSuggestionsPopup() {
        suggestionsPopup.apply {
            isAutoHide = true
            isHideOnEscape = true
            content.add(suggestionListView)
        }
    }

    private fun adjustPopupWidth() {
        suggestionListView.prefWidth = componentField.width
    }

    private fun adjustPopupHeight() {
        val itemCount = suggestionListView.items.size.coerceAtMost(Constants.SUGGESTIONS_MAX_ITEMS)
        suggestionListView.prefHeight = itemCount * 45.0
    }

    private fun showPopup() {
        val scene = componentField.scene ?: return
        val window = scene.window ?: return

        suggestionsPopup.show(
            componentField,
            componentField.localToScene(0.0, 0.0).x + window.x + scene.x,
            componentField.localToScene(0.0, 0.0).y + window.y + scene.y + componentField.height,
        )
    }
}
