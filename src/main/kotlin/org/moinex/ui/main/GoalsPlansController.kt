/*
 * Filename: GoalsPlansController.kt (original filename: GoalsPlansController.java)
 * Created on: July 26, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 17/03/2026
 */

package org.moinex.ui.main

import javafx.fxml.FXML
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import org.moinex.common.constant.Files
import org.moinex.common.util.UIUtils
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class GoalsPlansController(
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var tabPane: TabPane

    @FXML
    private lateinit var goalsTab: Tab

    @FXML
    private lateinit var plansTab: Tab

    @FXML
    private lateinit var wishlistTab: Tab

    companion object {
        private val logger = LoggerFactory.getLogger(GoalsPlansController::class.java)
    }

    @FXML
    fun initialize() {
        runCatching {
            UIUtils.loadContentIntoTab(
                goalsTab,
                Files.GOALS_FXML,
                Files.GOALS_STYLE_SHEET,
                springContext,
                javaClass,
                preferencesService.bundle,
            )

            UIUtils.loadContentIntoTab(
                plansTab,
                Files.PLANS_FXML,
                Files.PLANS_STYLE_SHEET,
                springContext,
                javaClass,
                preferencesService.bundle,
            )

            UIUtils.loadContentIntoTab(
                wishlistTab,
                Files.WISHLIST_FXML,
                Files.WISHLIST_STYLE_SHEET,
                springContext,
                javaClass,
                preferencesService.bundle,
            )
        }.onFailure { e ->
            logger.error("Error loading content: '{}'", e.message, e)
        }
    }
}
