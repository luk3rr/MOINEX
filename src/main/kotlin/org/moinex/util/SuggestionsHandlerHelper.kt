/*
 * Filename: SuggestionsHandlerHelper.kt (original filename: SuggestionsHandlerHelper.java)
 * Created on: March 15, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.util

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
import org.moinex.constants.Constants
import java.util.function.Consumer
import java.util.function.Function

class SuggestionsHandlerHelper<T>(
    private val componentField: TextField,
    private val filterFunction: Function<T, String>, // TODO: Após migração para kotlin, utilizar (T) -> String
    private val displayFunction: Function<T, String>, // TODO: Após migração para kotlin, utilizar (T) -> String
    private val onSelectCallback: Consumer<T>, // TODO: Após migração para kotlin, utilizar (T) -> Unit
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
                            filterFunction
                                .apply(item)
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
                                    children.add(Label(displayFunction.apply(item)))
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
                    onSelectCallback.accept(it)
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
