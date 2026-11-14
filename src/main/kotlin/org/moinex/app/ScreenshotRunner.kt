/*
 * Filename: ScreenshotRunner.kt
 * Created on: October 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.app

import javafx.application.Platform
import javafx.event.Event
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TabPane
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.DialogCard
import org.moinex.common.ui.DialogResize
import org.moinex.common.ui.DialogSize
import org.moinex.common.ui.closeDialog
import org.moinex.common.util.FxUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.enums.PeriodType
import org.moinex.model.investment.FundamentalAnalysis
import org.moinex.model.investment.Ticker
import org.moinex.repository.investment.FundamentalAnalysisRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.service.PreferencesService
import org.moinex.service.ThemeService
import org.moinex.ui.dialog.investment.view.BondInterestHistoryController
import org.moinex.ui.dialog.investment.view.FundamentalAnalysisController
import org.moinex.ui.dialog.investment.view.TickerDetailController
import org.moinex.ui.dialog.wallettransaction.create.AddExpenseController
import org.moinex.ui.main.MainController
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.File
import java.text.MessageFormat
import java.time.LocalDateTime
import javax.imageio.ImageIO
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds

/**
 * Development tool that opens each screen of [CapturePlan] in both themes and saves one PNG per screen and theme.
 *
 * It only runs when the system property [CapturePlan.PROPERTY_CAPTURE_DIR] is set, and it refuses to run when
 * `user.home` is the real home of the person running the app, so the real database and preferences are never touched.
 */
@Component
class ScreenshotRunner(
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
    private val themeService: ThemeService,
    private val tickerRepository: TickerRepository,
    private val fundamentalAnalysisRepository: FundamentalAnalysisRepository,
    private val bondRepository: org.moinex.repository.investment.BondRepository,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ScreenshotRunner::class.java)
        private const val WINDOW_SETTLE_MILLIS = 800L
        private const val NOTIFICATIONS_SETTLE_MILLIS = 1500L
        private const val FUNDAMENTAL_TAB_SETTLE_MILLIS = 800L
        private const val DETAIL_CHART_SETTLE_MILLIS = 1200L
        private const val OVERVIEW_SETTLE_MILLIS = 4000L
        private const val TAB_SETTLE_MILLIS = 5000L
        private const val HOVER_SETTLE_MILLIS = 600L
        private const val GRAPH_NEXT_BUTTON_SELECTOR = "#graphNextButton"
        private const val HOME_RECALCULATE_BUTTON_SELECTOR = "#recalculateNetWorthButton"
        private const val FIRE_CALCULATE_BUTTON_SELECTOR = "#calculateButton"
        private const val FIRE_HOVER_TARGET = "fire-hover"
        private const val HOME_NET_WORTH_HOVER_TARGET = "home-net-worth-hover"
        private const val CHART_DATA_SETTLE_MILLIS = 6000L
        private val FIRE_CAPTURE_INPUTS =
            mapOf(
                "#currentNetWorthField" to "20000",
                "#monthlyContributionField" to "1500",
                "#monthlyExpenseField" to "4000",
                "#annualReturnRateField" to "8",
                "#withdrawalRateField" to "4",
                "#currentAgeField" to "30",
            )
        private const val HOVER_X_FRACTION = 0.62
        private const val HOVER_Y_FRACTION = 0.4
        private const val FUNDAMENTAL_CAPTURE_SYMBOL = "PETR4.SA"
        private const val FUNDAMENTAL_CAPTURE_SECTOR = "Energy"
        private const val FUNDAMENTAL_CAPTURE_FUND_SYMBOL = "MGLU3.SA"
        private const val FUNDAMENTAL_CAPTURE_FUND_SECTOR = "Real Estate"
        private const val FUNDAMENTAL_CAPTURE_FUND_FIXTURE = "/capture/fundamental-analysis-fund-sample.json"
        private val FUNDAMENTAL_CAPTURE_PEERS = listOf("VALE3.SA", "ITUB4.SA", "MGLU3.SA")
        private const val FUNDAMENTAL_CAPTURE_FIXTURE = "/capture/fundamental-analysis-sample.json"
        private const val SEARCH_CAPTURE_QUERY = "Adicionar"
        private const val REFUSED_EXIT_CODE = 2
        private const val STACK_STEP_MILLIS = 900L
        private const val TOAST_REPEATS = 3
        private const val RESIZE_CAPTURE_WIDTH = 420.0
        private const val RESIZE_DRAG_PIXELS = 80.0
        private const val STACK_BUTTON_SPACING = 8.0
        private const val STACK_BODY_PADDING = 16.0

        fun isEnabled(): Boolean = System.getProperty(CapturePlan.PROPERTY_CAPTURE_DIR) != null
    }

    fun runIfEnabled(stage: Stage) {
        val directory = System.getProperty(CapturePlan.PROPERTY_CAPTURE_DIR) ?: return

        if (CapturePlan.isRealUserHome(System.getProperty("user.home"), System.getenv("HOME"))) {
            logger.error(
                "Capture refused: user.home is the real home. Run with a throwaway -Duser.home (see scripts/dev/capture-screens.sh).",
            )
            exitProcess(REFUSED_EXIT_CODE)
        }

        FxUtils.launchOnFxThread {
            runCatching { captureAll(stage, File(directory)) }
                .onFailure { logger.error("Capture failed", it) }
            logger.info("Capture finished, exiting")
            Platform.exit()
        }
    }

    private suspend fun captureAll(
        stage: Stage,
        directory: File,
    ) {
        val mainController = springContext.getBean<MainController>()

        stage.isMaximized = false
        stage.width = CapturePlan.WINDOW_WIDTH
        stage.height = CapturePlan.WINDOW_HEIGHT
        delay(WINDOW_SETTLE_MILLIS.milliseconds)

        for (theme in CapturePlan.themes) {
            preferencesService.theme = theme
            themeService.applyCurrentTheme()
            logger.info("Capturing theme '{}'", theme)

            for (target in CapturePlan.selectedTargets(System.getProperty(CapturePlan.PROPERTY_CAPTURE_ONLY))) {
                val file = File(directory, CapturePlan.relativePath(theme, target).toString())
                file.parentFile.mkdirs()

                runCatching { capture(stage, mainController, target, file) }
                    .onFailure { logger.error("Could not capture '{}' in theme '{}'", target.id, theme, it) }
            }
        }

        verifyToggleOnOpenWindow()
    }

    private suspend fun verifyToggleOnOpenWindow() {
        preferencesService.theme = PreferencesService.THEME_LIGHT
        themeService.applyCurrentTheme()

        val probe =
            Stage().apply {
                scene = Scene(StackPane(Label("probe")))
                show()
            }
        delay(WINDOW_SETTLE_MILLIS.milliseconds)
        val lightApplied = probe.scene.stylesheets.any { it.endsWith("tokens.light.css") }

        themeService.toggleAndApply()
        delay(WINDOW_SETTLE_MILLIS.milliseconds)
        val darkApplied =
            probe.scene.stylesheets.any { it.endsWith("tokens.dark.css") } &&
                probe.scene.stylesheets.none { it.endsWith("tokens.light.css") }
        probe.close()

        logger.info(
            "Theme toggle on an open window: light={} dark={} -> {}",
            lightApplied,
            darkApplied,
            if (lightApplied && darkApplied) "OK" else "FAILED",
        )
    }

    private suspend fun capture(
        stage: Stage,
        mainController: MainController,
        target: CaptureTarget,
        file: File,
    ) {
        when (target.kind) {
            CaptureKind.SCREEN -> {
                mainController.loadContent(target.fxml, target.styleSheet!!)
                delay(target.settleMillis.milliseconds)
                target.detail?.takeIf { it.endsWith(CapturePlan.HOVER_SUFFIX) }?.let { detail ->
                    repeat(detail.removeSuffix(CapturePlan.HOVER_SUFFIX).toIntOrNull() ?: 0) {
                        (stage.scene.root.lookup(GRAPH_NEXT_BUTTON_SELECTOR) as? javafx.scene.control.Button)?.fire()
                        delay(TAB_SETTLE_MILLIS.milliseconds)
                    }
                    fillChartData(stage.scene.root, target.id)
                    hoverOverChart(stage.scene.root)
                    delay(HOVER_SETTLE_MILLIS.milliseconds)
                }
                writePng(snapshot(stage), file)
            }

            CaptureKind.NOTIFICATIONS -> {
                mainController.loadContent(target.fxml, target.styleSheet!!)
                delay(target.settleMillis.milliseconds)
                mainController.openNotificationPanel()
                delay(NOTIFICATIONS_SETTLE_MILLIS.milliseconds)
                writePng(snapshot(stage), file)
                mainController.closeNotificationPanel()
                delay(NOTIFICATIONS_SETTLE_MILLIS.milliseconds)
            }

            CaptureKind.SEARCH -> {
                mainController.loadContent(target.fxml, target.styleSheet!!)
                delay(target.settleMillis.milliseconds)
                Event.fireEvent(
                    stage.scene.root,
                    KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.S, false, true, false, false),
                )
                mainController.setGlobalSearchQuery(SEARCH_CAPTURE_QUERY)
                delay(NOTIFICATIONS_SETTLE_MILLIS.milliseconds)
                writePng(snapshot(stage), file)
                Event.fireEvent(
                    stage.scene.root,
                    KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.S, false, true, false, false),
                )
            }

            CaptureKind.DIALOG -> captureAddExpenseDialog(stage, target, file)

            CaptureKind.ALERT -> captureConfirmationAlert(stage, target, file)

            CaptureKind.FUNDAMENTAL -> captureFundamentalAnalysis(stage, target, file, fund = false)

            CaptureKind.FUNDAMENTAL_FUND -> captureFundamentalAnalysis(stage, target, file, fund = true)

            CaptureKind.TICKER_DETAIL -> captureTickerDetail(stage, target, file)

            CaptureKind.BOND_HISTORY -> captureBondHistory(stage, target, file)

            CaptureKind.REBALANCE -> captureRebalance(stage, mainController, target, file)

            CaptureKind.DIALOG_STACK -> captureDialogStack(stage, mainController, target, file)

            CaptureKind.DIALOG_LAYOUT -> captureDialogLayout(stage, mainController, target, file)

            CaptureKind.DIALOG_RESIZE -> captureDialogResize(stage, mainController, target, file)

            CaptureKind.TOASTS -> {
                mainController.loadContent(target.fxml, target.styleSheet!!)
                delay(target.settleMillis.milliseconds)
                repeat(TOAST_REPEATS) {
                    WindowUtils.showInformationDialog("No item selected", "Select a transaction to edit it.")
                }
                WindowUtils.showSuccessDialog("Saved", "The expense was added.")
                WindowUtils.showErrorDialog(
                    "Could not save",
                    "The wallet balance would become negative. Review the value and try again.",
                )
                delay(NOTIFICATIONS_SETTLE_MILLIS.milliseconds)
                writePng(snapshot(stage), file)
                WindowUtils.toastHub?.clear()
                delay(NOTIFICATIONS_SETTLE_MILLIS.milliseconds)
            }

            CaptureKind.TAB -> {
                mainController.loadContent(target.fxml, target.styleSheet!!)
                delay(target.settleMillis.milliseconds)
                selectTab(stage, target.detail!!.removeSuffix(CapturePlan.HOVER_SUFFIX).toInt())
                delay(TAB_SETTLE_MILLIS.milliseconds)
                if (target.detail!!.endsWith(CapturePlan.HOVER_SUFFIX)) {
                    hoverOverChart(stage.scene.root)
                    delay(HOVER_SETTLE_MILLIS.milliseconds)
                }
                writePng(snapshot(stage), file)
            }
        }
        if (file.exists()) logger.info("Saved {}", file.name) else logger.warn("Not saved: {}", file.name)
    }

    private suspend fun captureDialogStack(
        stage: Stage,
        mainController: MainController,
        target: CaptureTarget,
        file: File,
    ) {
        val host = mainController.dialogHost
        mainController.loadContent(Files.HOME_FXML, Files.HOME_STYLE_SHEET)
        delay(target.settleMillis.milliseconds)

        val (formRoot, _) = WindowUtils.loadEmbedded<AddExpenseController>(Files.ADD_EXPENSE_FXML, springContext)
        val form =
            DialogCard(
                preferencesService.translate(TranslationKeys.TRANSACTION_DIALOG_ADD_EXPENSE_TITLE),
                formRoot,
                closeTooltip = preferencesService.translate(TranslationKeys.DIALOG_BUTTON_CLOSE),
            )
        val question =
            stackQuestionCard(preferencesService.translate(TranslationKeys.DIALOG_BUTTON_DELETE), danger = true)
        val confirm =
            stackQuestionCard(preferencesService.translate(TranslationKeys.DIALOG_BUTTON_CONFIRM), danger = false)
        val depths = mutableListOf<Int>()

        FxUtils.launchOnFxThread {
            delay(STACK_STEP_MILLIS.milliseconds)
            Platform.runLater { host.show(question) }
        }
        FxUtils.launchOnFxThread {
            delay(STACK_STEP_MILLIS.milliseconds * 2)
            Platform.runLater { host.show(confirm) }
        }
        FxUtils.launchOnFxThread {
            delay(STACK_STEP_MILLIS.milliseconds * 3)
            depths.add(host.depth)
            writePng(snapshot(stage), file)
            Platform.runLater {
                confirm.closeDialog()
                depths.add(host.depth)
                question.closeDialog()
                depths.add(host.depth)
                form.closeDialog()
                depths.add(host.depth)
                logger.info("Dialog stack depths after the snapshot and each close (expected [3, 2, 1, 0]): {}", depths)
            }
        }

        yield()
        host.show(form)
    }

    private suspend fun captureDialogLayout(
        stage: Stage,
        mainController: MainController,
        target: CaptureTarget,
        file: File,
    ) {
        val host = mainController.dialogHost
        val key = WindowUtils.sizeKeyOf(target.fxml)
        val (width, height) = target.detail!!.split("x").map { it.toDouble() }
        WindowUtils.dialogSizes.save(key, DialogResize.Size(width, height))

        FxUtils.launchOnFxThread {
            delay(target.settleMillis.milliseconds)
            if (host.depth == 0) {
                logger.error("The dialog '{}' did not open; nothing captured", target.id)
            } else {
                writePng(snapshot(stage), file)
                host.closeAll()
            }
            WindowUtils.dialogSizes.clear(key)
        }

        yield()

        WindowUtils.openModalWindow(target.fxml, target.id, springContext, { _: Any? -> })
    }

    private suspend fun captureDialogResize(
        stage: Stage,
        mainController: MainController,
        target: CaptureTarget,
        file: File,
    ) {
        val host = mainController.dialogHost
        val key = "capture-resize"
        WindowUtils.dialogSizes.clear(key)
        mainController.loadContent(Files.HOME_FXML, Files.HOME_STYLE_SHEET)
        delay(target.settleMillis.milliseconds)

        fun newCard(): DialogCard {
            val (root, _) = WindowUtils.loadEmbedded<AddExpenseController>(Files.ADD_EXPENSE_FXML, springContext)
            return DialogCard(
                preferencesService.translate(TranslationKeys.TRANSACTION_DIALOG_ADD_EXPENSE_TITLE),
                root,
                width = RESIZE_CAPTURE_WIDTH,
                sizeKey = key,
                sizeStore = WindowUtils.dialogSizes,
            )
        }

        fun mouse(
            type: javafx.event.EventType<MouseEvent>,
            card: DialogCard,
            dx: Double,
        ) = MouseEvent(
            card,
            card,
            type,
            card.localToScene(card.width - 2, card.height / 2).x + dx,
            card.localToScene(card.width - 2, card.height / 2).y,
            card.localToScreen(card.width - 2, card.height / 2).x + dx,
            card.localToScreen(card.width - 2, card.height / 2).y,
            MouseButton.PRIMARY,
            1,
            false,
            false,
            false,
            false,
            type != MouseEvent.MOUSE_RELEASED,
            false,
            false,
            false,
            false,
            true,
            null,
        )

        val first = newCard()
        host.present(first)
        delay(target.settleMillis.milliseconds)
        val before = first.width
        val pressed = mouse(MouseEvent.MOUSE_PRESSED, first, 0.0)
        Event.fireEvent(first, pressed)
        val dragged = mouse(MouseEvent.MOUSE_DRAGGED, first, RESIZE_DRAG_PIXELS)
        Event.fireEvent(first, dragged)
        delay(NOTIFICATIONS_SETTLE_MILLIS.milliseconds)
        val during = first.width
        Event.fireEvent(first, mouse(MouseEvent.MOUSE_RELEASED, first, RESIZE_DRAG_PIXELS))
        writePng(snapshot(stage), file)
        val saved = WindowUtils.dialogSizes.load(key)
        host.closeAll()
        delay(NOTIFICATIONS_SETTLE_MILLIS.milliseconds)

        val second = newCard()
        host.present(second)
        delay(target.settleMillis.milliseconds)
        logger.info(
            "Dialog resize: width {} -> {} (expected +{}), saved {}, reopened at {}",
            before,
            during,
            RESIZE_DRAG_PIXELS * 2,
            saved,
            second.width,
        )
        host.closeAll()
        WindowUtils.dialogSizes.clear(key)
    }

    private fun stackQuestionCard(
        actionLabel: String,
        danger: Boolean,
    ): DialogCard {
        val cancel = Button(preferencesService.translate(TranslationKeys.DIALOG_BUTTON_CANCEL))
        val action =
            Button(actionLabel).apply {
                styleClass.add(if (danger) "danger" else "accent")
            }
        val buttons = HBox(cancel, action).apply { spacing = STACK_BUTTON_SPACING }
        val card =
            DialogCard(
                preferencesService.translate(TranslationKeys.DIALOG_CONFIRMATION_TITLE),
                Label(preferencesService.translate(TranslationKeys.DIALOG_CONFIRMATION_TITLE)).apply {
                    padding =
                        Insets(STACK_BODY_PADDING)
                },
                size = DialogSize.SMALL,
                closable = false,
                footer = buttons,
            )
        cancel.setOnAction { card.closeDialog() }
        action.setOnAction { card.closeDialog() }
        card.initialFocus = cancel
        return card
    }

    private suspend fun captureAddExpenseDialog(
        mainStage: Stage,
        target: CaptureTarget,
        file: File,
    ) {
        val host = springContext.getBean<MainController>().dialogHost

        FxUtils.launchOnFxThread {
            delay(target.settleMillis.milliseconds)
            if (host.depth == 0) {
                logger.error("The dialog '{}' did not open; nothing captured", target.id)
            } else {
                pressEnter(mainStage)
                delay(target.settleMillis.milliseconds)
                writePng(snapshot(mainStage), file)
                host.closeAll()
            }
        }

        yield()

        WindowUtils.openModalWindow(
            Files.ADD_EXPENSE_FXML,
            preferencesService.translate(TranslationKeys.TRANSACTION_DIALOG_ADD_EXPENSE_TITLE),
            springContext,
            { _: AddExpenseController -> },
        )
    }

    private suspend fun captureFundamentalAnalysis(
        mainStage: Stage,
        target: CaptureTarget,
        file: File,
        fund: Boolean,
    ) {
        val symbol = if (fund) FUNDAMENTAL_CAPTURE_FUND_SYMBOL else FUNDAMENTAL_CAPTURE_SYMBOL
        val ticker = tickerRepository.findBySymbol(symbol)
        if (ticker == null) {
            logger.error("Ticker {} not found in the capture database; nothing captured", symbol)
            return
        }
        if (fund) seedFundFundamentalAnalysis(ticker) else seedFundamentalAnalysis(ticker)

        scheduleModalCapture(mainStage, target, file) { dialog ->
            val tabTitle = target.detail?.let { preferencesService.translate(it) }
            val tabPane =
                dialog.scene.root
                    .lookupAll(".tab-pane")
                    .filterIsInstance<TabPane>()
                    .firstOrNull()
            tabPane?.tabs?.firstOrNull { it.text == tabTitle }?.let { tabPane.selectionModel.select(it) }
            delay(FUNDAMENTAL_TAB_SETTLE_MILLIS.milliseconds)
        }

        yield()

        WindowUtils.openModalWindow(
            Files.FUNDAMENTAL_ANALYSIS_FXML,
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.FUNDAMENTAL_ANALYSIS_DIALOG_TITLE),
                ticker.symbol,
            ),
            springContext,
            { controller: FundamentalAnalysisController ->
                controller.setTicker(ticker)
                controller.loadAnalysis()
            },
            emptyList(),
        )
    }

    private suspend fun captureTickerDetail(
        mainStage: Stage,
        target: CaptureTarget,
        file: File,
    ) {
        val ticker = tickerRepository.findBySymbol(FUNDAMENTAL_CAPTURE_SYMBOL)
        if (ticker == null) {
            logger.error("Ticker {} not found in the capture database; nothing captured", FUNDAMENTAL_CAPTURE_SYMBOL)
            return
        }

        scheduleModalCapture(mainStage, target, file) { dialog ->
            if (target.detail == CapturePlan.INCOME_TAB) {
                dialog.scene.root
                    .lookup(".tab-pane")
                    ?.let { (it as javafx.scene.control.TabPane).selectionModel.select(1) }
            }
            delay(DETAIL_CHART_SETTLE_MILLIS.milliseconds)
            if (target.detail == CapturePlan.HOVER_SUFFIX) {
                hoverOverChart(dialog.scene.root)
                delay(HOVER_SETTLE_MILLIS.milliseconds)
            }
        }

        yield()

        WindowUtils.openModalWindow(
            Files.TICKER_DETAIL_FXML,
            MessageFormat.format(preferencesService.translate(TranslationKeys.TICKER_DETAIL_TITLE), ticker.symbol),
            springContext,
            { controller: TickerDetailController -> controller.setTicker(ticker) {} },
            emptyList(),
        )
    }

    /**
     * The capture database has no data for some charts, so the screen gets what it needs to draw them: the FIRE
     * calculator is filled in and calculated, and the net worth of the home screen is recalculated.
     */
    private suspend fun fillChartData(
        root: javafx.scene.Parent,
        targetId: String,
    ) {
        when (targetId) {
            FIRE_HOVER_TARGET -> {
                FIRE_CAPTURE_INPUTS.forEach { (selector, value) ->
                    (root.lookup(selector) as? javafx.scene.control.TextField)?.text = value
                }
                (root.lookup(FIRE_CALCULATE_BUTTON_SELECTOR) as? javafx.scene.control.Button)?.fire()
                delay(CHART_DATA_SETTLE_MILLIS.milliseconds)
            }

            HOME_NET_WORTH_HOVER_TARGET -> {
                (root.lookup(HOME_RECALCULATE_BUTTON_SELECTOR) as? javafx.scene.control.Button)?.fire()
                delay(CHART_DATA_SETTLE_MILLIS.milliseconds)
            }
        }
    }

    /**
     * Moves a simulated mouse over the plot of the first interactive chart under [root], a little to the right of its
     * middle, so that the crosshair shows in the picture.
     */
    private fun hoverOverChart(root: javafx.scene.Parent) {
        val plot = root.lookupAll(".chart-plot-background").firstOrNull() ?: return
        val bounds = plot.localToScene(plot.boundsInLocal)
        val x = bounds.minX + bounds.width * HOVER_X_FRACTION
        val y = bounds.minY + bounds.height * HOVER_Y_FRACTION

        Event.fireEvent(
            plot,
            javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_MOVED,
                x,
                y,
                x,
                y,
                javafx.scene.input.MouseButton.NONE,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                null,
            ),
        )
    }

    private fun selectTab(
        stage: Stage,
        index: Int,
    ) {
        stage.scene.root
            .lookupAll(".tab-pane")
            .filterIsInstance<TabPane>()
            .firstOrNull()
            ?.selectionModel
            ?.select(index)
    }

    private suspend fun captureBondHistory(
        mainStage: Stage,
        target: CaptureTarget,
        file: File,
    ) {
        val bond = bondRepository.findAll().firstOrNull()
        if (bond == null) {
            logger.error("No bond in the capture database; nothing captured")
            return
        }

        scheduleModalCapture(mainStage, target, file) { delay(DETAIL_CHART_SETTLE_MILLIS.milliseconds) }

        yield()

        WindowUtils.openModalWindow(
            Files.BOND_INTEREST_HISTORY_FXML,
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.BOND_DIALOG_INTEREST_HISTORY_TITLE),
                bond.name,
            ),
            springContext,
            { controller: BondInterestHistoryController -> controller.setBond(bond) },
            emptyList(),
        )
    }

    private suspend fun captureRebalance(
        mainStage: Stage,
        mainController: MainController,
        target: CaptureTarget,
        file: File,
    ) {
        mainController.loadContent(Files.SAVINGS_OVERVIEW_FXML, Files.SAVINGS_OVERVIEW_STYLE_SHEET)
        delay(OVERVIEW_SETTLE_MILLIS.milliseconds)

        val label = preferencesService.translate(TranslationKeys.SAVINGS_REBALANCE_BUTTON)
        val button =
            mainStage.scene.root
                .lookupAll(".button")
                .filterIsInstance<Button>()
                .firstOrNull { it.text == label }
        if (button == null) {
            logger.error("The rebalance button was not found; nothing captured")
            return
        }

        scheduleModalCapture(mainStage, target, file) { delay(DETAIL_CHART_SETTLE_MILLIS.milliseconds) }

        yield()

        button.fire()
    }

    /**
     * Waits for the modal opened after this call, lets [prepare] adjust it, takes its picture and closes it.
     */
    private fun scheduleModalCapture(
        mainStage: Stage,
        target: CaptureTarget,
        file: File,
        prepare: suspend (Stage) -> Unit,
    ) {
        val host = springContext.getBean<MainController>().dialogHost

        FxUtils.launchOnFxThread {
            delay(target.settleMillis.milliseconds)
            if (host.depth == 0) {
                logger.error("The dialog '{}' did not open; nothing captured", target.id)
            } else {
                prepare(mainStage)
                writePng(snapshot(mainStage), file)
                host.closeAll()
            }
        }
    }

    private fun seedFundFundamentalAnalysis(ticker: Ticker) {
        val json =
            javaClass
                .getResourceAsStream(FUNDAMENTAL_CAPTURE_FUND_FIXTURE)!!
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        saveFundamentalAnalysis(ticker, json, FUNDAMENTAL_CAPTURE_FUND_SECTOR)
    }

    private fun seedFundamentalAnalysis(ticker: Ticker) {
        val json =
            javaClass
                .getResourceAsStream(FUNDAMENTAL_CAPTURE_FIXTURE)!!
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

        (listOf(ticker) + FUNDAMENTAL_CAPTURE_PEERS.mapNotNull { tickerRepository.findBySymbol(it) }).forEach {
            saveFundamentalAnalysis(it, json, FUNDAMENTAL_CAPTURE_SECTOR)
        }
    }

    private fun saveFundamentalAnalysis(
        ticker: Ticker,
        json: String,
        sector: String,
    ) {
        val now = LocalDateTime.now()
        val existing = fundamentalAnalysisRepository.findByTickerIdAndPeriodType(ticker.id!!, PeriodType.ANNUAL)

        if (existing != null) {
            existing.dataJson = json
            existing.sector = sector
            existing.lastUpdate = now
            fundamentalAnalysisRepository.save(existing)
        } else {
            fundamentalAnalysisRepository.save(
                FundamentalAnalysis(
                    ticker = ticker,
                    companyName = ticker.name,
                    sector = sector,
                    currency = "BRL",
                    periodType = PeriodType.ANNUAL,
                    dataJson = json,
                    lastUpdate = now,
                    createdAt = now,
                ),
            )
        }
    }

    private fun pressEnter(dialog: Stage) {
        val target = dialog.scene.focusOwner ?: dialog.scene.root
        Event.fireEvent(
            target,
            KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false),
        )
    }

    private suspend fun captureConfirmationAlert(
        mainStage: Stage,
        target: CaptureTarget,
        file: File,
    ) {
        val host = springContext.getBean<MainController>().dialogHost

        FxUtils.launchOnFxThread {
            delay(target.settleMillis.milliseconds)
            if (host.depth == 0) {
                logger.error("The confirmation '{}' did not open; nothing captured", target.id)
            } else {
                writePng(snapshot(mainStage), file)
                host.closeAll()
            }
        }

        yield()

        WindowUtils.showConfirmationDialog(
            "Delete this item?",
            "This action cannot be undone.",
            preferencesService.bundle,
            danger = true,
        )
    }

    private fun snapshot(stage: Stage): Image = stage.scene.root.snapshot(SnapshotParameters(), null)

    private fun writePng(
        image: Image,
        file: File,
    ) {
        val width = image.width.toInt()
        val height = image.height.toInt()
        val pixels = IntArray(width * height)
        image.pixelReader.getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width)

        val buffered = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        buffered.setRGB(0, 0, width, height, pixels, 0, width)
        ImageIO.write(buffered, "png", file)
    }
}
