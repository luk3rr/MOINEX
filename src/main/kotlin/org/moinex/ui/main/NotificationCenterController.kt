package org.moinex.ui.main

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ListView
import org.moinex.model.Notification
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.ui.common.NotificationListCell
import org.springframework.stereotype.Controller

@Controller
class NotificationCenterController(
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var notificationList: ListView<Notification>

    @FXML
    private lateinit var emptyLabel: Label

    @FXML
    fun initialize() {
        notificationList.setCellFactory { NotificationListCell(preferencesService.bundle) }
    }

    fun refresh() {
        val items = notificationService.getAll()
        notificationList.items = FXCollections.observableList(items)
        val isEmpty = items.isEmpty()
        emptyLabel.isVisible = isEmpty
        emptyLabel.isManaged = isEmpty
    }

    @FXML
    private fun handleMarkAllRead() {
        notificationService.markAllAsRead()
        refresh()
    }

    @FXML
    private fun handleClearAll() {
        notificationService.deleteAll()
        refresh()
    }
}
