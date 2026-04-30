package org.moinex.ui.common

import javafx.animation.FadeTransition
import javafx.animation.PauseTransition
import javafx.animation.SequentialTransition
import javafx.animation.TranslateTransition
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.util.Duration
import org.moinex.common.constant.Files
import org.moinex.common.constant.Styles
import org.moinex.model.Notification
import org.moinex.model.enums.NotificationType

class NotificationToastOverlay(
    private val rootPane: AnchorPane,
) {
    private val stack =
        VBox(8.0).apply {
            alignment = Pos.BOTTOM_RIGHT
            isPickOnBounds = false
            maxWidth = TOAST_WIDTH + MAX_WIDTH_OFFSET
        }

    private val queue: ArrayDeque<Notification> = ArrayDeque()
    private val activeToasts = mutableListOf<HBox>()

    init {
        rootPane.children.add(stack)
        AnchorPane.setBottomAnchor(stack, STACK_BOTTOM_OFFSET)
        AnchorPane.setRightAnchor(stack, STACK_RIGHT_OFFSET)

        stack.stylesheets.add(
            NotificationToastOverlay::class.java.getResource(Files.NOTIFICATION_CSS)!!.toExternalForm(),
        )
    }

    fun show(notification: Notification) {
        if (activeToasts.size >= MAX_VISIBLE) {
            queue.addLast(notification)
            return
        }
        val card = buildCard(notification)
        stack.children.add(0, card)
        activeToasts.add(card)
        animateIn(card) {
            scheduleAutoDismiss(card)
        }
    }

    private fun dismiss(card: HBox) {
        animateOut(card) {
            stack.children.remove(card)
            activeToasts.remove(card)
            if (queue.isNotEmpty()) show(queue.removeFirst())
        }
    }

    private fun buildCard(n: Notification): HBox {
        val accentBar =
            Region().apply {
                styleClass.addAll(Styles.NOTIFICATION_TOAST_ACCENT, accentClass(n.type))
                minHeight = 0.0
                maxHeight = Double.MAX_VALUE
            }

        val title =
            Label(n.title).apply {
                styleClass.add(Styles.NOTIFICATION_TOAST_TITLE)
                isWrapText = true
                maxWidth = TOAST_WIDTH - TOAST_TITLE_AND_MESSAGE_WIDTH_OFFSET
            }

        val message =
            Label(n.message).apply {
                styleClass.add(Styles.NOTIFICATION_TOAST_MESSAGE)
                isWrapText = true
                maxWidth = TOAST_WIDTH - TOAST_TITLE_AND_MESSAGE_WIDTH_OFFSET
            }

        val textBox =
            VBox(3.0, title, message).apply {
                HBox.setHgrow(this, Priority.ALWAYS)
                HBox.setMargin(this, Insets(0.0, 0.0, 0.0, 14.0))
            }

        val closeButton =
            Button("✕").apply {
                styleClass.add(Styles.NOTIFICATION_TOAST_CLOSE)
            }

        val card =
            HBox(accentBar, textBox, closeButton).apply {
                styleClass.add(Styles.NOTIFICATION_TOAST)
                minWidth = TOAST_WIDTH
                maxWidth = TOAST_WIDTH
                alignment = Pos.CENTER_LEFT
            }

        closeButton.setOnAction { dismiss(card) }
        card.setOnMouseClicked { dismiss(card) }

        return card
    }

    private fun animateIn(
        card: HBox,
        onDone: () -> Unit,
    ) {
        card.translateX = TOAST_WIDTH
        card.opacity = 0.0

        val slide =
            TranslateTransition(Duration.millis(SLIDE_MS), card).apply {
                fromX = TOAST_WIDTH
                toX = 0.0
            }
        val fade =
            FadeTransition(Duration.millis(FADE_MS), card).apply {
                fromValue = 0.0
                toValue = 1.0
            }

        slide.play()
        fade.play()
        fade.setOnFinished { onDone() }
    }

    private fun scheduleAutoDismiss(card: HBox) {
        val pause = PauseTransition(Duration.millis(DISPLAY_MS))
        pause.setOnFinished { dismiss(card) }
        pause.play()
    }

    private fun animateOut(
        card: HBox,
        onDone: () -> Unit,
    ) {
        val fade =
            FadeTransition(Duration.millis(FADE_MS), card).apply {
                fromValue = 1.0
                toValue = 0.0
            }
        val slide =
            TranslateTransition(Duration.millis(SLIDE_MS), card).apply {
                fromX = 0.0
                toX = TOAST_WIDTH
            }
        val seq = SequentialTransition(fade, slide)
        seq.setOnFinished { onDone() }
        seq.play()
    }

    private fun accentClass(type: NotificationType): String =
        when (type) {
            NotificationType.BUDGET_GROUP_EXCEEDED -> Styles.NOTIFICATION_ACCENT_BUDGET
            NotificationType.RECURRING_TRANSACTION_PROCESSED -> Styles.NOTIFICATION_ACCENT_RECURRING
            NotificationType.RECURRING_CREDIT_CARD_DEBT_PROCESSED -> Styles.NOTIFICATION_ACCENT_RECURRING_CC
            NotificationType.WALLET_TRANSACTION_CREATED -> Styles.NOTIFICATION_ACCENT_WALLET_TRANSACTION
            NotificationType.CREDIT_CARD_TRANSACTION_CREATED -> Styles.NOTIFICATION_ACCENT_CC_TRANSACTION
            NotificationType.GOAL_ACHIEVED -> Styles.NOTIFICATION_ACCENT_GOAL
            NotificationType.GENERIC_INFO -> Styles.NOTIFICATION_ACCENT_INFO
            NotificationType.GENERIC_WARNING -> Styles.NOTIFICATION_ACCENT_WARNING
            NotificationType.GENERIC_ERROR -> Styles.NOTIFICATION_ACCENT_ERROR
        }

    companion object {
        private const val MAX_VISIBLE = 4
        private const val TOAST_WIDTH = 320.0
        private const val TOAST_TITLE_AND_MESSAGE_WIDTH_OFFSET = 60
        private const val MAX_WIDTH_OFFSET = 16
        private const val STACK_BOTTOM_OFFSET = 44.0
        private const val STACK_RIGHT_OFFSET = 16.0
        private const val SLIDE_MS = 220.0
        private const val FADE_MS = 180.0
        private const val DISPLAY_MS = 10000.0
    }
}
