package org.moinex.common.ui

import atlantafx.base.controls.SegmentedControl
import atlantafx.base.controls.ToggleLabel
import javafx.application.Platform

/**
 * A filter with a few fixed options, shown as a [SegmentedControl] instead of a combo box. [options] pairs the
 * text of each segment with the value it stands for; the first option is selected by default.
 */
class SegmentedFilter<T>(
    private val control: SegmentedControl,
    private val options: List<Pair<String, T>>,
) {
    private val labels = options.map { ToggleLabel(it.first) }

    init {
        control.segments.setAll(labels)
        labels.forEach { it.toggleGroup = control.toggleGroup }
        control.toggleGroup.selectToggle(labels.first())
    }

    var value: T
        get() {
            val index = labels.indexOf(control.toggleGroup.selectedToggle)
            return options[if (index < 0) 0 else index].second
        }
        set(newValue) {
            select(newValue)
            Platform.runLater { select(newValue) }
        }

    private fun select(newValue: T) {
        val index = options.indexOfFirst { it.second == newValue }
        if (index >= 0) control.toggleGroup.selectToggle(labels[index])
    }

    fun onChange(action: () -> Unit) {
        control.toggleGroup.selectedToggleProperty().addListener { _, _, _ -> action() }
    }
}
