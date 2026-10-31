package org.moinex.common.ui

import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TableRow
import javafx.scene.control.TableView

/**
 * One entry of a row menu: [label] is shown in the menu and [run] is called with the row already selected.
 */
data class RowAction(
    val label: String,
    val run: () -> Unit,
)

/**
 * Opens the actions of a table row with a right click on it. The clicked row is selected first, so the handlers
 * that read the selected item work unchanged. Empty rows open nothing. [createRow] builds the row, so a table
 * that also styles its rows passes its own row here; a second row factory would replace the menu.
 */
object RowContextMenu {
    fun <T> install(
        table: TableView<T>,
        actions: List<RowAction>,
        createRow: () -> TableRow<T> = { TableRow() },
    ) {
        table.setRowFactory {
            createRow().apply {
                val menu =
                    ContextMenu(
                        *actions
                            .map { action ->
                                MenuItem(action.label).apply {
                                    setOnAction {
                                        table.selectionModel.select(index)
                                        action.run()
                                    }
                                }
                            }.toTypedArray(),
                    )

                setOnContextMenuRequested { event ->
                    if (!isEmpty) {
                        menu.show(this, event.screenX, event.screenY)
                    }
                    event.consume()
                }
            }
        }
    }
}
