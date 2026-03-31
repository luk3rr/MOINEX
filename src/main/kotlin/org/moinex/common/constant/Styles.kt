package org.moinex.common.constant

object Styles {
    // Component styles
    const val CHARTS_COLORS_COUNT: Int = 20
    const val CHARTS_COLORS_PREFIX: String = "chart-color-"
    const val CHARTS_LEGEND_RECT_STYLE: String = "legend-color"

    const val PROGRESS_BAR_RED_COLOR_STYLE: String = "progress-bar-red"
    const val PROGRESS_BAR_YELLOW_COLOR_STYLE: String = "progress-bar-yellow"
    const val PROGRESS_BAR_GREEN_COLOR_STYLE: String = "progress-bar-green"

    // Info styles from common-styles.css
    const val INFO_LABEL_RED_STYLE: String = "info-label-red"
    const val INFO_LABEL_GREEN_STYLE: String = "info-label-green"
    const val INFO_LABEL_YELLOW_STYLE: String = "info-label-yellow"
    const val INFO_LABEL_NEUTRAL_STYLE: String = "info-label-neutral"

    // Main pane styles
    const val SIDEBAR_SELECTED_BUTTON_STYLE: String = "sidebar-button-selected"
    const val NEGATIVE_BALANCE_STYLE: String = "negative-balance"
    const val POSITIVE_BALANCE_STYLE: String = "positive-balance"
    const val NEUTRAL_BALANCE_STYLE: String = "neutral-balance"

    // Home pane styles
    const val HOME_LAST_TRANSACTIONS_INCOME_ITEM_STYLE: String = "income-item"
    const val HOME_LAST_TRANSACTIONS_EXPENSE_ITEM_STYLE: String = "expense-item"

    const val HOME_CREDIT_CARD_ITEM_STYLE: String = "credit-card-item"

    const val CUSTOM_TABLE_TITLE_STYLE: String = "custom-table-title"
    const val CUSTOM_TABLE_HEADER_STYLE: String = "custom-table-header"
    const val CUSTOM_TABLE_CELL_STYLE: String = "custom-table-cell"

    // Allocation Panel Styles
    const val ALLOCATION_TYPE_LABEL_STYLE: String = "allocation-type-label"
    const val ALLOCATION_PROGRESS_BAR_STYLE: String = "allocation-progress-bar"
    const val ALLOCATION_FILLED_BAR_CRITICAL_LOW_STYLE: String = "allocation-filled-bar-critical-low"
    const val ALLOCATION_FILLED_BAR_WARNING_LOW_STYLE: String = "allocation-filled-bar-warning-low"
    const val ALLOCATION_FILLED_BAR_ON_TARGET_STYLE: String = "allocation-filled-bar-on-target"
    const val ALLOCATION_FILLED_BAR_WARNING_HIGH_STYLE: String = "allocation-filled-bar-warning-high"
    const val ALLOCATION_FILLED_BAR_CRITICAL_HIGH_STYLE: String = "allocation-filled-bar-critical-high"
    const val ALLOCATION_INFO_LABEL_STYLE: String = "allocation-info-label"
    const val ALLOCATION_DIFF_LABEL_STYLE: String = "allocation-diff-label"
    const val ALLOCATION_DIFF_CRITICAL_LOW_STYLE: String = "allocation-diff-critical-low"
    const val ALLOCATION_DIFF_WARNING_LOW_STYLE: String = "allocation-diff-warning-low"
    const val ALLOCATION_DIFF_ON_TARGET_STYLE: String = "allocation-diff-on-target"
    const val ALLOCATION_DIFF_WARNING_HIGH_STYLE: String = "allocation-diff-warning-high"
    const val ALLOCATION_DIFF_CRITICAL_HIGH_STYLE: String = "allocation-diff-critical-high"

    // Credit Card Item Styles
    const val HOME_CREDIT_CARD_ITEM_NAME_STYLE: String = "credit-card-item-name"
    const val HOME_CREDIT_CARD_ITEM_BALANCE_STYLE: String = "credit-card-item-balance"
    const val HOME_CREDIT_CARD_ITEM_DIGITS_STYLE: String = "credit-card-item-digits"
    const val HOME_CREDIT_CARD_ITEM_OPERATOR_STYLE: String = "credit-card-item-operator"

    const val HOME_WALLET_ITEM_STYLE: String = "wallet-item"
    const val HOME_WALLET_ITEM_NAME_STYLE: String = "wallet-item-name"
    const val HOME_WALLET_ITEM_BALANCE_STYLE: String = "wallet-item-balance"
    const val HOME_WALLET_TYPE_STYLE: String = "wallet-item-type"
    const val HOME_VIRTUAL_WALLET_INFO_STYLE: String = "virtual-wallet-info"

    const val TOOLTIP_STYLE: String = "tooltip"

    const val TOTAL_BALANCE_VALUE_LABEL_STYLE: String = "total-balance-value-label"
    const val TOTAL_BALANCE_FORESEEN_LABEL_STYLE: String = "total-balance-foreseen-label"

    // Wallet pane styles
    const val WALLET_TOTAL_BALANCE_WALLETS_LABEL_STYLE: String = "total-balance-wallets-label"
    const val WALLET_CHECK_BOX_STYLE: String = "check-box"

    // Detail
    const val DETAIL_KEY_LABEL_STYLE = "-fx-font-weight: bold;"
    const val DETAIL_VALUE_LABEL_STYLE = "-fx-font-weight: normal;"
    const val DETAIL_VALUE_COPYABLE_STYLE = "-fx-font-weight: normal; -fx-cursor: hand;"
    const val DETAIL_VALUE_COPYABLE_HOVER_STYLE = "-fx-font-weight: normal; -fx-cursor: hand; -fx-underline: true;"

    // Notification
    const val NOTIFICATION_STYLE =
        "-fx-background-color: rgba(0, 0, 0, 0.8); " +
            "-fx-text-fill: white; " +
            "-fx-padding: 5 10; " +
            "-fx-background-radius: 5; " +
            "-fx-font-size: 11px;"
}
