package org.moinex.common.extension

import javafx.scene.Node
import javafx.scene.effect.ColorAdjust
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane

fun ImageView.applyIconTheme(isDarkMode: Boolean) {
    effect = if (isDarkMode) ColorAdjust().apply { brightness = 1.0 } else null
}

fun Node.setAnchorPaneConstraints(
    top: Double = 0.0,
    bottom: Double = 0.0,
    left: Double = 0.0,
    right: Double = 0.0,
) {
    AnchorPane.setTopAnchor(this, top)
    AnchorPane.setBottomAnchor(this, bottom)
    AnchorPane.setLeftAnchor(this, left)
    AnchorPane.setRightAnchor(this, right)
}
