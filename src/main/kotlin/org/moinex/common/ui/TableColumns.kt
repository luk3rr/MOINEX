package org.moinex.common.ui

import javafx.collections.ListChangeListener
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView

/**
 * Header layout shared by every table: the "ID" column header is centred, the other headers keep their alignment.
 */
object TableColumns {
    private const val ID_HEADER = "ID"

    /**
     * Centres the header of the "ID" columns of [table], now and for columns added later.
     */
    fun centerIdHeaders(table: TableView<*>) {
        table.columns.forEach { centerIfId(it) }
        table.columns.addListener(
            ListChangeListener { change ->
                while (change.next()) {
                    if (change.wasAdded()) change.addedSubList.forEach { centerIfId(it) }
                }
            },
        )
    }

    private fun centerIfId(column: TableColumn<*, *>) {
        if (column.text != ID_HEADER) return
        column.text = ""
        column.graphic =
            Label(ID_HEADER).apply {
                alignment = Pos.CENTER
                maxWidth = Double.MAX_VALUE
            }
    }
}
