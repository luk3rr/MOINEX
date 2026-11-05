/*
 * Filename: ReviewPanel.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common.ai

import javafx.beans.value.ChangeListener
import javafx.collections.ListChangeListener
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.StringConverter
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.ChangeState
import org.moinex.ai.review.ReviewItem
import org.moinex.ai.review.ReviewSession
import org.moinex.common.constant.TranslationKeys
import org.moinex.service.PreferencesService
import org.moinex.service.matching.MatchStatus
import java.text.MessageFormat

/**
 * The side panel with everything the assistant proposed. It only draws the [ReviewSession] and forwards the user's
 * decisions to it; [onEdit] opens the editor for one item and [onHide] collapses the panel.
 */
class ReviewPanel(
    private val session: ReviewSession,
    private val preferencesService: PreferencesService,
    private val onEdit: (ReviewItem) -> Unit,
    private val onHide: () -> Unit,
) : VBox(10.0) {
    private enum class Filter(
        val labelKey: String,
        val badge: ChangeBadge?,
    ) {
        ALL(TranslationKeys.AIPANEL_FILTER_ALL, null),
        NEW(TranslationKeys.AIPANEL_BADGE_NEW, ChangeBadge.NEW),
        MATCHES(TranslationKeys.AIPANEL_BADGE_MATCHES, ChangeBadge.MATCHES_EXISTING),
        DUPLICATE(TranslationKeys.AIPANEL_BADGE_DUPLICATE, ChangeBadge.POSSIBLE_DUPLICATE),
        LOW_CONFIDENCE(TranslationKeys.AIPANEL_BADGE_LOW_CONFIDENCE, ChangeBadge.LOW_CONFIDENCE),
    }

    private val filterBox = ComboBox<Filter>()
    private val countersBox = HBox(6.0)
    private val cardsBox = VBox(8.0)
    private val selectedLabel = Label()
    private val approveSelectedButton = Button(translate(TranslationKeys.AIPANEL_ACTION_APPROVE_SELECTED))
    private val itemListener = ChangeListener<Any?> { _, _, _ -> refresh() }
    private val selectionListener = ChangeListener<Boolean> { _, _, _ -> updateFooter() }

    init {
        styleClass.add(STYLE_PANEL)
        padding = Insets(12.0)
        prefWidth = PANEL_WIDTH
        minWidth = PANEL_WIDTH

        val title = Label(translate(TranslationKeys.AIPANEL_TITLE)).apply { styleClass.add(STYLE_TITLE) }
        val hideButton =
            Button(translate(TranslationKeys.AIPANEL_HIDE)).apply {
                styleClass.add(STYLE_BUTTON)
                setOnAction { onHide() }
            }
        val header = HBox(8.0, title, spacer(), hideButton).apply { alignment = Pos.CENTER_LEFT }

        filterBox.items.setAll(Filter.entries)
        filterBox.value = Filter.ALL
        filterBox.converter =
            object : StringConverter<Filter>() {
                override fun toString(value: Filter?): String =
                    value?.let { MessageFormat.format(translate(it.labelKey), "").trim() } ?: ""

                override fun fromString(string: String?): Filter = Filter.ALL
            }
        filterBox.valueProperty().addListener { _, _, _ -> refresh() }

        val scroll =
            ScrollPane(cardsBox).apply {
                isFitToWidth = true
                styleClass.add(STYLE_SCROLL)
                VBox.setVgrow(this, Priority.ALWAYS)
            }

        approveSelectedButton.styleClass.addAll(STYLE_BUTTON, STYLE_BUTTON_PRIMARY)
        approveSelectedButton.setOnAction {
            session.approve(session.items.filter { it.selectedProperty.get() && it.isOpen })
        }
        val footer =
            HBox(8.0, selectedLabel, spacer(), approveSelectedButton).apply {
                alignment = Pos.CENTER_LEFT
                styleClass.add(STYLE_FOOTER)
            }

        children.addAll(header, filterBox, countersBox, scroll, footer)

        session.items.forEach(::observe)
        session.items.addListener(
            ListChangeListener { change ->
                while (change.next()) {
                    change.addedSubList.forEach(::observe)
                }
                refresh()
            },
        )
        refresh()
    }

    private fun observe(item: ReviewItem) {
        item.stateProperty.addListener(itemListener)
        item.payloadProperty.addListener(itemListener)
        item.messageProperty.addListener(itemListener)
        item.selectedProperty.addListener(selectionListener)
    }

    fun refresh() {
        val filter = filterBox.value ?: Filter.ALL
        val visible = session.items.filter { filter.badge == null || it.change.badge == filter.badge }

        renderCounters()
        cardsBox.children.setAll(
            if (visible.isEmpty()) {
                listOf(Label(translate(TranslationKeys.AIPANEL_EMPTY)).apply { styleClass.add(STYLE_MUTED) })
            } else {
                visible.map(::buildCard)
            },
        )
        updateFooter()
    }

    private fun renderCounters() {
        val counters = session.counters()
        countersBox.children.setAll(
            counterLabel(TranslationKeys.AIPANEL_COUNTER_PENDING, counters.pending, STYLE_BADGE_NEW),
            counterLabel(TranslationKeys.AIPANEL_COUNTER_APPROVED, counters.approved, STYLE_BADGE_APPROVED),
            counterLabel(TranslationKeys.AIPANEL_COUNTER_REJECTED, counters.rejected, STYLE_BADGE_REJECTED),
            counterLabel(TranslationKeys.AIPANEL_COUNTER_ERROR, counters.error, STYLE_BADGE_ERROR),
        )
    }

    private fun counterLabel(
        key: String,
        count: Int,
        style: String,
    ) = Label(MessageFormat.format(translate(key), count)).apply { styleClass.addAll(STYLE_BADGE, style) }

    private fun updateFooter() {
        val selected = session.items.count { it.selectedProperty.get() && it.isOpen }
        selectedLabel.text = MessageFormat.format(translate(TranslationKeys.AIPANEL_SELECTED_COUNT), selected)
        approveSelectedButton.isDisable = selected == 0
    }

    private fun buildCard(item: ReviewItem): VBox {
        val open = item.isOpen
        val check =
            CheckBox().apply {
                isSelected = item.selectedProperty.get()
                isDisable = !open
                selectedProperty().addListener { _, _, value -> item.selectedProperty.set(value) }
            }
        val operationLabel =
            Label(translate("aioperation.${item.change.operationName}")).apply { styleClass.add(STYLE_CARD_TITLE) }
        val header = HBox(8.0, check, operationLabel, spacer(), badgeLabel(item)).apply { alignment = Pos.CENTER_LEFT }

        val summary =
            Label(session.summaryOf(item)).apply {
                isWrapText = true
                minHeight = Region.USE_PREF_SIZE
                styleClass.add(STYLE_CARD_SUMMARY)
            }

        val card = VBox(6.0, header, summary)
        card.styleClass.add(STYLE_CARD)
        if (item.selectedProperty.get() && open) card.styleClass.add(STYLE_CARD_SELECTED)

        if (item.change.reasons.isNotEmpty() && open) {
            val reasons = item.change.reasons.joinToString(", ")
            card.children.add(
                wrapped(MessageFormat.format(translate(TranslationKeys.AIPANEL_REASONS), reasons), STYLE_MUTED),
            )
        }

        if (open) {
            session.validationOf(item).errors.forEach { card.children.add(wrapped(it, STYLE_ERROR)) }
        }
        item.messageProperty
            .get()
            .takeIf { it.isNotBlank() }
            ?.let { card.children.add(wrapped(it, STYLE_ERROR)) }

        card.children.add(buildActions(item))
        return card
    }

    private fun buildActions(item: ReviewItem): HBox {
        val actions = HBox(6.0).apply { alignment = Pos.CENTER_RIGHT }

        fun button(
            key: String,
            extraStyle: String? = null,
            action: () -> Unit,
        ) = Button(translate(key)).apply {
            styleClass.add(STYLE_BUTTON)
            extraStyle?.let { styleClass.add(it) }
            setOnAction { action() }
        }

        when (item.state) {
            ChangeState.REJECTED ->
                actions.children.add(button(TranslationKeys.AIPANEL_ACTION_UNDO_REJECT) { session.undoReject(item) })
            ChangeState.APPROVED -> Unit
            else ->
                actions.children.addAll(
                    button(TranslationKeys.AIPANEL_ACTION_EDIT, STYLE_BUTTON_EDIT) { onEdit(item) },
                    button(TranslationKeys.AIPANEL_ACTION_REJECT, STYLE_BUTTON_DANGER) { session.reject(item) },
                    button(TranslationKeys.AIPANEL_ACTION_APPROVE, STYLE_BUTTON_PRIMARY) {
                        session.approve(listOf(item))
                    },
                )
        }
        return actions
    }

    private fun badgeLabel(item: ReviewItem): Label {
        val (text, style) =
            when (item.state) {
                ChangeState.APPROVED -> translate(TranslationKeys.AIPANEL_BADGE_APPROVED) to STYLE_BADGE_APPROVED
                ChangeState.REJECTED -> translate(TranslationKeys.AIPANEL_BADGE_REJECTED) to STYLE_BADGE_REJECTED
                ChangeState.ERROR -> translate(TranslationKeys.AIPANEL_BADGE_ERROR) to STYLE_BADGE_ERROR
                else -> originalBadge(item)
            }
        val edited =
            if (item.state == ChangeState.EDITED) " · " + translate(TranslationKeys.AIPANEL_BADGE_EDITED) else ""
        return Label(text + edited).apply { styleClass.addAll(STYLE_BADGE, style) }
    }

    private fun originalBadge(item: ReviewItem): Pair<String, String> =
        when (item.change.badge) {
            ChangeBadge.NEW -> translate(TranslationKeys.AIPANEL_BADGE_NEW) to STYLE_BADGE_NEW
            ChangeBadge.MATCHES_EXISTING -> {
                val id =
                    item.change.matchCandidates
                        .firstOrNull { it.status == MatchStatus.PENDING }
                        ?.targetId
                MessageFormat.format(translate(TranslationKeys.AIPANEL_BADGE_MATCHES), id ?: "") to STYLE_BADGE_MATCH
            }
            ChangeBadge.POSSIBLE_DUPLICATE -> translate(TranslationKeys.AIPANEL_BADGE_DUPLICATE) to STYLE_BADGE_WARNING
            ChangeBadge.LOW_CONFIDENCE ->
                translate(TranslationKeys.AIPANEL_BADGE_LOW_CONFIDENCE) to STYLE_BADGE_WARNING
        }

    private fun wrapped(
        text: String,
        style: String,
    ) = Label(text).apply {
        isWrapText = true
        minHeight = Region.USE_PREF_SIZE
        styleClass.add(style)
    }

    private fun spacer() = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

    private fun translate(key: String) = preferencesService.translate(key)

    companion object {
        private const val PANEL_WIDTH = 400.0
        private const val STYLE_PANEL = "ai-review-panel"
        private const val STYLE_TITLE = "ai-review-title"
        private const val STYLE_SCROLL = "ai-review-scroll"
        private const val STYLE_FOOTER = "ai-review-footer"
        private const val STYLE_CARD = "ai-review-card"
        private const val STYLE_CARD_SELECTED = "ai-review-card-selected"
        private const val STYLE_CARD_TITLE = "ai-review-card-title"
        private const val STYLE_CARD_SUMMARY = "ai-review-card-summary"
        private const val STYLE_MUTED = "ai-review-muted"
        private const val STYLE_ERROR = "ai-review-error"
        private const val STYLE_BUTTON = "ai-chat-attach-button"
        private const val STYLE_BUTTON_PRIMARY = "ai-review-button-primary"
        private const val STYLE_BUTTON_DANGER = "ai-review-button-danger"
        private const val STYLE_BUTTON_EDIT = "ai-review-button-edit"
        private const val STYLE_BADGE = "ai-review-badge"
        private const val STYLE_BADGE_NEW = "ai-review-badge-new"
        private const val STYLE_BADGE_MATCH = "ai-review-badge-match"
        private const val STYLE_BADGE_WARNING = "ai-review-badge-warning"
        private const val STYLE_BADGE_APPROVED = "ai-review-badge-approved"
        private const val STYLE_BADGE_REJECTED = "ai-review-badge-rejected"
        private const val STYLE_BADGE_ERROR = "ai-review-badge-error"
    }
}
