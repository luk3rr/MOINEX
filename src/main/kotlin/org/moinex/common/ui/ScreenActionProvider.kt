package org.moinex.common.ui

/**
 * Implemented by the controller of a screen that offers actions through the "+" button of the footer.
 */
interface ScreenActionProvider {
    fun screenActions(): List<RowAction>

    /**
     * Calls [callback] whenever the actions may have changed, for example when the selected tab changes.
     */
    fun onActionsChanged(callback: () -> Unit) {}
}
