package org.moinex.common.extension

import javafx.scene.Node
import javafx.scene.layout.AnchorPane

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
