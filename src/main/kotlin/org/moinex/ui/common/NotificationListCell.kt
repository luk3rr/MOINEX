package org.moinex.ui.common

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.scene.shape.Circle
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.model.Notification
import org.moinex.model.enums.NotificationStatus
import org.moinex.model.enums.NotificationType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.ResourceBundle

class NotificationListCell(
    private val bundle: ResourceBundle,
) : ListCell<Notification>() {
    init {
        stylesheets.add(
            NotificationListCell::class.java.getResource(Files.NOTIFICATION_CSS)!!.toExternalForm(),
        )
    }

    override fun updateItem(
        item: Notification?,
        empty: Boolean,
    ) {
        super.updateItem(item, empty)
        if (empty || item == null) {
            graphic = null
            return
        }
        graphic = buildCell(item)
    }

    private fun buildCell(n: Notification): HBox {
        val accentBar =
            Region().apply {
                styleClass.addAll(STYLE_CELL_ACCENT, accentClass(n.type))
                minHeight = 0.0
                maxHeight = Double.MAX_VALUE
            }

        val title =
            Label(n.title).apply {
                styleClass.add(STYLE_CELL_TITLE)
                if (n.status == NotificationStatus.READ) {
                    styleClass.add(STYLE_CELL_TITLE_READ)
                }
                isWrapText = true
            }

        val message =
            Label(n.message).apply {
                styleClass.add(STYLE_CELL_MESSAGE)
                isWrapText = true
            }

        val time =
            Label(relativeTime(n.createdAt)).apply {
                styleClass.add(STYLE_CELL_TIME)
            }

        val textBox =
            VBox(3.0, title, message, time).apply {
                HBox.setHgrow(this, Priority.ALWAYS)
                VBox.setMargin(this, Insets(0.0, 8.0, 0.0, 10.0))
            }

        val unreadDot =
            Circle(4.0).apply {
                styleClass.add(STYLE_UNREAD_DOT)
                isVisible = n.status == NotificationStatus.UNREAD
                HBox.setMargin(this, Insets(0.0, 6.0, 0.0, 0.0))
            }

        val cell =
            HBox(accentBar, textBox, unreadDot).apply {
                styleClass.add(STYLE_CELL)
                if (n.status == NotificationStatus.UNREAD) {
                    styleClass.add(STYLE_CELL_UNREAD)
                }
                alignment = Pos.CENTER_LEFT
                HBox.setMargin(this, Insets(4.0, 8.0, 4.0, 8.0))
            }

        return cell
    }

    private fun relativeTime(dateTime: LocalDateTime): String {
        val minutes = ChronoUnit.MINUTES.between(dateTime, LocalDateTime.now())
        return when {
            minutes < NOTIFICATION_TIME_NOW_THRESHOLD_MINUTES -> bundle.getString(TranslationKeys.NOTIFICATION_TIME_NOW)
            minutes < NOTIFICATION_TIME_MINUTES_THRESHOLD_MINUTES ->
                bundle
                    .getString(TranslationKeys.NOTIFICATION_TIME_MINUTES)
                    .replace("{0}", minutes.toString())
            minutes < NOTIFICATION_TIME_HOURS_THRESHOLD_MINUTES ->
                bundle
                    .getString(TranslationKeys.NOTIFICATION_TIME_HOURS)
                    .replace("{0}", (minutes / HOURS_IN_MINUTE).toString())
            else ->
                bundle
                    .getString(TranslationKeys.NOTIFICATION_TIME_DAYS)
                    .replace("{0}", (minutes / MINUTES_IN_DAY).toString())
        }
    }

    private fun accentClass(type: NotificationType): String =
        when (type) {
            NotificationType.BUDGET_GROUP_EXCEEDED -> ACCENT_BUDGET
            NotificationType.RECURRING_TRANSACTION_PROCESSED -> ACCENT_RECURRING
            NotificationType.RECURRING_CREDIT_CARD_DEBT_PROCESSED -> ACCENT_RECURRING_CC
            NotificationType.GOAL_ACHIEVED -> ACCENT_GOAL
            NotificationType.GENERIC_INFO -> ACCENT_INFO
            NotificationType.GENERIC_WARNING -> ACCENT_WARNING
            NotificationType.GENERIC_ERROR -> ACCENT_ERROR
        }

    companion object {
        const val HOURS_IN_MINUTE = 60
        const val MINUTES_IN_DAY = 1440
        const val NOTIFICATION_TIME_NOW_THRESHOLD_MINUTES = 1L
        const val NOTIFICATION_TIME_MINUTES_THRESHOLD_MINUTES = 60L
        const val NOTIFICATION_TIME_HOURS_THRESHOLD_MINUTES = 1440L

        const val STYLE_CELL = "notification-cell"
        const val STYLE_CELL_UNREAD = "notification-cell-unread"
        const val STYLE_CELL_ACCENT = "notification-cell-accent"
        const val STYLE_CELL_TITLE = "notification-cell-title"
        const val STYLE_CELL_TITLE_READ = "notification-cell-title-read"
        const val STYLE_CELL_MESSAGE = "notification-cell-message"
        const val STYLE_CELL_TIME = "notification-cell-time"
        const val STYLE_UNREAD_DOT = "notification-unread-dot"

        const val ACCENT_BUDGET = "notification-accent-budget"
        const val ACCENT_RECURRING = "notification-accent-recurring"
        const val ACCENT_RECURRING_CC = "notification-accent-recurring-cc"
        const val ACCENT_GOAL = "notification-accent-goal"
        const val ACCENT_INFO = "notification-accent-info"
        const val ACCENT_WARNING = "notification-accent-warning"
        const val ACCENT_ERROR = "notification-accent-error"
    }
}
