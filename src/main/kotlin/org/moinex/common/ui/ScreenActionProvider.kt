package org.moinex.common.ui

/**
 * Implemented by the controller of a screen that offers actions through the "+" button of the footer.
 */
interface ScreenActionProvider {
    fun screenActions(): List<RowAction>
}
