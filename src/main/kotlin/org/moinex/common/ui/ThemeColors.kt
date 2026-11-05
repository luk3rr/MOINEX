/*
 * Filename: ThemeColors.kt
 * Created on: October 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import javafx.scene.paint.Color

/**
 * Colors that code draws directly (canvas charts, WebView bubbles) and so cannot read from the CSS tokens.
 * ThemeColorsTest keeps every value here equal to its `theme-*` token or inside the contrast minimum.
 */
object ThemeColors {
    const val DARK_TEXT_PRIMARY = "#F8F8F2"
    const val LIGHT_TEXT_PRIMARY = "#1F2328"
    const val DARK_BG_CARD = "#282A36"
    const val LIGHT_BG_CARD = "#FFFFFF"
    const val DARK_BG_CARD_ALT = "#21222C"
    const val LIGHT_BG_CARD_ALT = "#F5F5F5"
    const val DARK_BORDER = "#44475A"
    const val LIGHT_BORDER = "#D1D9E0"
    const val DARK_BG_SIDEBAR = "#0A614E"
    const val LIGHT_BG_SIDEBAR = "#1ABC9C"
    const val DARK_ON_SIDEBAR = "#F8F8F2"
    const val LIGHT_ON_SIDEBAR = "#1F2328"
    const val DARK_WARNING = "#FFB86C"
    const val LIGHT_WARNING = "#9A6700"
    const val DARK_ALLOC_TRACK = "#44475A"
    const val LIGHT_ALLOC_TRACK = "#E5E7EB"
    const val DARK_POSITIVE = "#50FA7B"
    const val LIGHT_POSITIVE = "#1A7F37"
    const val DARK_NEGATIVE = "#FF5555"
    const val LIGHT_NEGATIVE = "#C62F3E"
    const val DARK_ACCENT = "#BD93F9"
    const val LIGHT_ACCENT_TEXT = "#6D4FC2"

    val DARK_SANKEY_PALETTE =
        listOf(
            "#FF6B6B",
            "#4ECDC4",
            "#45B7D1",
            "#FFA07A",
            "#98D8C8",
            "#F7DC6F",
            "#BB8FCE",
            "#85C1E2",
            "#F06292",
            "#AED581",
        )

    val LIGHT_SANKEY_PALETTE =
        listOf(
            "#FF5757",
            "#2C9D95",
            "#2D9DB6",
            "#FF5714",
            "#3B9C84",
            "#AF8E0A",
            "#AE7AC5",
            "#3397CE",
            "#EF598C",
            "#6C9D34",
        )

    fun textPrimary(isDarkMode: Boolean): Color = Color.web(if (isDarkMode) DARK_TEXT_PRIMARY else LIGHT_TEXT_PRIMARY)

    fun positive(isDarkMode: Boolean): Color = Color.web(if (isDarkMode) DARK_POSITIVE else LIGHT_POSITIVE)

    fun negative(isDarkMode: Boolean): Color = Color.web(if (isDarkMode) DARK_NEGATIVE else LIGHT_NEGATIVE)

    fun allocTrack(isDarkMode: Boolean): Color = Color.web(if (isDarkMode) DARK_ALLOC_TRACK else LIGHT_ALLOC_TRACK)

    fun progress(isDarkMode: Boolean): Color = Color.web(if (isDarkMode) DARK_ACCENT else LIGHT_ACCENT_TEXT)

    fun sankeyPalette(isDarkMode: Boolean): List<Color> =
        (if (isDarkMode) DARK_SANKEY_PALETTE else LIGHT_SANKEY_PALETTE).map(Color::web)
}
