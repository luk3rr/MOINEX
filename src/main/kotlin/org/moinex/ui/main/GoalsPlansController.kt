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
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.RowAction
import org.moinex.common.ui.ScreenActionProvider
import org.moinex.common.util.UIUtils
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class GoalsPlansController(
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) : ScreenActionProvider {
    @FXML
    private lateinit var tabPane: TabPane

    private var goalController: Any? = null
    private var wishlistController: Any? = null

    override fun onActionsChanged(callback: () -> Unit) {
        tabPane.selectionModel.selectedItemProperty().addListener { _, _, _ -> callback() }
    }

    private fun activeProvider(): ScreenActionProvider? =
        when (tabPane.selectionModel.selectedItem) {
            goalsTab -> goalController
            wishlistTab -> wishlistController
            else -> null
        } as? ScreenActionProvider

    override fun screenActions(): List<RowAction> = activeProvider()?.screenActions().orEmpty()

    override fun screenContext(): String =
        listOfNotNull(
            preferencesService.translate(TranslationKeys.MAIN_GOALS_AND_PLANS),
            activeProvider()?.screenContext()?.takeIf { it.isNotEmpty() },
        ).joinToString(" · ")

    @FXML
    private lateinit var goalsTab: Tab

    @FXML
    private lateinit var plansTab: Tab

    @FXML
    private lateinit var wishlistTab: Tab

    @FXML
    private lateinit var fireTab: Tab

    companion object {
        private val logger = LoggerFactory.getLogger(GoalsPlansController::class.java)
    }

    @FXML
    fun initialize() {
        runCatching {
            goalController =
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

            wishlistController =
                UIUtils.loadContentIntoTab(
                    wishlistTab,
                    Files.WISHLIST_FXML,
                    Files.WISHLIST_STYLE_SHEET,
                    springContext,
                    javaClass,
                    preferencesService.bundle,
                )

            UIUtils.loadContentIntoTab(
                fireTab,
                Files.FIRE_CALCULATOR_FXML,
                Files.FIRE_CALCULATOR_STYLE_SHEET,
                springContext,
                javaClass,
                preferencesService.bundle,
            )
        }.onFailure { e ->
            logger.error("Error loading content: '{}'", e.message, e)
        }
    }
}
