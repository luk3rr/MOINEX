/*
 * Filename: SavingsController.kt (original filename: SavingsController.java)
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 17/03/2026
 */

package org.moinex.ui.main

import javafx.fxml.FXML
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import org.moinex.service.PreferencesService
import org.moinex.util.Constants
import org.moinex.util.UIUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class SavingsController(
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var tabPane: TabPane

    @FXML
    private lateinit var overviewTab: Tab

    @FXML
    private lateinit var stocksFundsTab: Tab

    @FXML
    private lateinit var bondsTab: Tab

    companion object {
        private val logger = LoggerFactory.getLogger(SavingsController::class.java)
    }

    @FXML
    fun initialize() {
        runCatching {
            UIUtils.loadContentIntoTab(
                overviewTab,
                Constants.SAVINGS_OVERVIEW_FXML,
                Constants.SAVINGS_OVERVIEW_STYLE_SHEET,
                springContext,
                javaClass,
                preferencesService.getBundle(),
            )

            UIUtils.loadContentIntoTab(
                stocksFundsTab,
                Constants.SAVINGS_STOCKS_FUNDS_FXML,
                Constants.SAVINGS_STOCKS_FUNDS_STYLE_SHEET,
                springContext,
                javaClass,
                preferencesService.getBundle(),
            )

            UIUtils.loadContentIntoTab(
                bondsTab,
                Constants.SAVINGS_BONDS_FXML,
                Constants.SAVINGS_BONDS_STYLE_SHEET,
                springContext,
                javaClass,
                preferencesService.getBundle(),
            )
        }.onFailure { e ->
            logger.error("Error loading content: '{}' - Cause: {}", e.message, e.cause, e)
        }
    }
}
