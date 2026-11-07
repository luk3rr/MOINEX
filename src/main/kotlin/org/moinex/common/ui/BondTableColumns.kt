/*
 * Filename: BondTableColumns.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.collections.ListChangeListener
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView

/**
 * The quantity and unit price columns of the bond tables only say something for bonds that have a unit price. When every
 * bond listed is held in reais (CDB, LCI and LCA) the amount is already in the invested value, so both columns are
 * hidden; a list with any other bond, or an empty one, shows them.
 */
object BondTableColumns {
    const val QUANTITY_ID = "bond-quantity-column"
    const val UNIT_PRICE_ID = "bond-unit-price-column"

    fun <S, T> TableColumn<S, T>.asQuantityColumn(): TableColumn<S, T> = apply { id = QUANTITY_ID }

    fun <S, T> TableColumn<S, T>.asUnitPriceColumn(): TableColumn<S, T> = apply { id = UNIT_PRICE_ID }

    fun <S> hideWhenAllHeldInReais(
        table: TableView<S>,
        isHeldInReais: (S) -> Boolean,
    ) {
        val update = {
            val show = table.items.isEmpty() || table.items.any { !isHeldInReais(it) }

            table.columns
                .filter { it.id == QUANTITY_ID || it.id == UNIT_PRICE_ID }
                .forEach { it.isVisible = show }
        }

        table.items.addListener(ListChangeListener { update() })
    }
}
