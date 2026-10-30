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
import org.moinex.common.constant.Files
import org.moinex.common.ui.RowAction
import org.moinex.common.ui.ScreenActionProvider
import org.moinex.common.util.UIUtils
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class SavingsController(
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) : ScreenActionProvider {
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

    private var stocksFundsController: Any? = null
    private var bondsController: Any? = null

    override fun screenActions(): List<RowAction> {
        val provider =
            when {
                stocksFundsTab.isSelected -> stocksFundsController
                bondsTab.isSelected -> bondsController
                else -> null
            } as? ScreenActionProvider
        return provider?.screenActions().orEmpty()
    }

    @FXML
    fun initialize() {
        runCatching {
            UIUtils.loadContentIntoTab(
                overviewTab,
                Files.SAVINGS_OVERVIEW_FXML,
                Files.SAVINGS_OVERVIEW_STYLE_SHEET,
                springContext,
                javaClass,
                preferencesService.bundle,
            )

            stocksFundsController =
                UIUtils.loadContentIntoTab(
                    stocksFundsTab,
                    Files.SAVINGS_STOCKS_FUNDS_FXML,
                    Files.SAVINGS_STOCKS_FUNDS_STYLE_SHEET,
                    springContext,
                    javaClass,
                    preferencesService.bundle,
                )

            bondsController =
                UIUtils.loadContentIntoTab(
                    bondsTab,
                    Files.SAVINGS_BONDS_FXML,
                    Files.SAVINGS_BONDS_STYLE_SHEET,
                    springContext,
                    javaClass,
                    preferencesService.bundle,
                )
        }.onFailure { e ->
            logger.error("Error loading content: '{}' - Cause: {}", e.message, e.cause, e)
        }
    }
}
