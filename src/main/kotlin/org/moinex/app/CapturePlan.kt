/*
 * Filename: CapturePlan.kt
 * Created on: October 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.app

import org.moinex.common.constant.Files
import org.moinex.service.PreferencesService
import java.nio.file.Path
import java.nio.file.Paths

enum class CaptureKind {
    SCREEN,
    NOTIFICATIONS,
    DIALOG,
}

/**
 * One screenshot to take. [index] is fixed per id so that the "before" and "after" captures of the same
 * screen always share a file name.
 */
data class CaptureTarget(
    val index: Int,
    val id: String,
    val kind: CaptureKind,
    val fxml: String,
    val styleSheet: String? = null,
    val settleMillis: Long = CapturePlan.DEFAULT_SETTLE_MILLIS,
)

/**
 * Ordered list of screens captured by [ScreenshotRunner], and the naming rules of the output files.
 * Kept free of JavaFX types so it can be unit tested.
 */
object CapturePlan {
    const val PROPERTY_CAPTURE_DIR = "moinex.capture.dir"
    const val WINDOW_WIDTH = 1440.0
    const val WINDOW_HEIGHT = 900.0
    const val DEFAULT_SETTLE_MILLIS = 2500L
    const val HEAVY_SETTLE_MILLIS = 4000L

    val themes: List<String> = listOf(PreferencesService.THEME_LIGHT, PreferencesService.THEME_DARK)

    val targets: List<CaptureTarget> =
        listOf(
            screen(1, "home", Files.HOME_FXML, Files.HOME_STYLE_SHEET, heavy = true),
            screen(2, "annual-summary", Files.ANNUAL_SUMMARY_FXML, Files.ANNUAL_SUMMARY_STYLE_SHEET, heavy = true),
            screen(3, "wallets", Files.WALLET_FXML, Files.WALLET_STYLE_SHEET, heavy = true),
            screen(4, "credit-cards", Files.CREDIT_CARD_FXML, Files.CREDIT_CARD_STYLE_SHEET, heavy = true),
            screen(5, "transactions", Files.TRANSACTION_FXML, Files.TRANSACTION_STYLE_SHEET, heavy = true),
            screen(6, "goals", Files.GOALS_FXML, Files.GOALS_STYLE_SHEET),
            screen(7, "plans", Files.PLANS_FXML, Files.PLANS_STYLE_SHEET, heavy = true),
            screen(8, "fire", Files.FIRE_CALCULATOR_FXML, Files.FIRE_CALCULATOR_STYLE_SHEET, heavy = true),
            screen(9, "wishlist", Files.WISHLIST_FXML, Files.WISHLIST_STYLE_SHEET, heavy = true),
            screen(
                10,
                "savings-overview",
                Files.SAVINGS_OVERVIEW_FXML,
                Files.SAVINGS_OVERVIEW_STYLE_SHEET,
                heavy = true,
            ),
            screen(
                11,
                "savings-stocks-funds",
                Files.SAVINGS_STOCKS_FUNDS_FXML,
                Files.SAVINGS_STOCKS_FUNDS_STYLE_SHEET,
            ),
            screen(12, "savings-bonds", Files.SAVINGS_BONDS_FXML, Files.SAVINGS_BONDS_STYLE_SHEET),
            screen(13, "ai-chat", Files.AI_CHAT_FXML, Files.AI_CHAT_STYLE_SHEET, heavy = true),
            screen(14, "settings", Files.SETTINGS_FXML, Files.SETTINGS_STYLE_SHEET),
            CaptureTarget(15, "notifications", CaptureKind.NOTIFICATIONS, Files.HOME_FXML, Files.HOME_STYLE_SHEET),
            CaptureTarget(16, "dialog-add-expense-error", CaptureKind.DIALOG, Files.ADD_EXPENSE_FXML),
        )

    fun fileName(target: CaptureTarget): String = "%02d-%s.png".format(target.index, target.id)

    fun relativePath(
        theme: String,
        target: CaptureTarget,
    ): Path = Paths.get(theme, fileName(target))

    /**
     * True when [userHome] is the real home of the person running the app, which means the capture would touch the
     * real database and preferences.
     */
    fun isRealUserHome(
        userHome: String?,
        environmentHome: String?,
    ): Boolean {
        if (userHome.isNullOrBlank() || environmentHome.isNullOrBlank()) return false
        return Paths.get(userHome).normalize() == Paths.get(environmentHome).normalize()
    }

    private fun screen(
        index: Int,
        id: String,
        fxml: String,
        styleSheet: String,
        heavy: Boolean = false,
    ): CaptureTarget =
        CaptureTarget(
            index = index,
            id = id,
            kind = CaptureKind.SCREEN,
            fxml = fxml,
            styleSheet = styleSheet,
            settleMillis = if (heavy) HEAVY_SETTLE_MILLIS else DEFAULT_SETTLE_MILLIS,
        )
}
