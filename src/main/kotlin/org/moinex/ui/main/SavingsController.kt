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
import org.moinex.common.constant.TranslationKeys
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
    private lateinit var performanceTab: Tab

    @FXML
    private lateinit var stocksFundsTab: Tab

    @FXML
    private lateinit var bondsTab: Tab

    companion object {
        private val logger = LoggerFactory.getLogger(SavingsController::class.java)
    }

    private var stocksFundsController: Any? = null
    private var bondsController: Any? = null

    override fun onActionsChanged(callback: () -> Unit) {
        tabPane.selectionModel.selectedItemProperty().addListener { _, _, _ -> callback() }
    }

    private fun activeProvider(): ScreenActionProvider? =
        when (tabPane.selectionModel.selectedItem) {
            stocksFundsTab -> stocksFundsController
            bondsTab -> bondsController
            else -> null
        } as? ScreenActionProvider

    override fun screenActions(): List<RowAction> = activeProvider()?.screenActions().orEmpty()

    override fun screenContext(): String =
        listOfNotNull(
            preferencesService.translate(TranslationKeys.MAIN_SAVINGS),
            activeProvider()?.screenContext()?.takeIf { it.isNotEmpty() },
        ).joinToString(" · ")

    @FXML
    fun initialize() {
        runCatching {
            loadTab(overviewTab)

            tabPane.selectionModel.selectedItemProperty().addListener { _, _, tab -> tab?.let(::loadTab) }
        }.onFailure { e ->
            logger.error("Error loading content: '{}' - Cause: {}", e.message, e.cause, e)
        }
    }

    /**
     * The tabs are loaded the first time they are shown, so that opening the screen only builds the one that is in
     * front. Loading is done before the selection listeners that read the tab's controller.
     */
    private fun loadTab(tab: Tab) {
        if (tab.content != null) return

        runCatching {
            val (fxml, styleSheet) =
                when (tab) {
                    overviewTab -> Files.SAVINGS_OVERVIEW_FXML to Files.SAVINGS_OVERVIEW_STYLE_SHEET
                    performanceTab -> Files.SAVINGS_PERFORMANCE_FXML to Files.SAVINGS_PERFORMANCE_STYLE_SHEET
                    stocksFundsTab -> Files.SAVINGS_STOCKS_FUNDS_FXML to Files.SAVINGS_STOCKS_FUNDS_STYLE_SHEET
                    else -> Files.SAVINGS_BONDS_FXML to Files.SAVINGS_BONDS_STYLE_SHEET
                }

            val controller =
                UIUtils.loadContentIntoTab(
                    tab,
                    fxml,
                    styleSheet,
                    springContext,
                    javaClass,
                    preferencesService.bundle,
                )

            when (tab) {
                stocksFundsTab -> stocksFundsController = controller
                bondsTab -> bondsController = controller
            }
        }.onFailure { e ->
            logger.error("Error loading tab '{}': {} - Cause: {}", tab.text, e.message, e.cause, e)
        }
    }
}
