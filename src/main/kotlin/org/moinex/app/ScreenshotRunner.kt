/*
 * Filename: ScreenshotRunner.kt
 * Created on: October 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.app

import javafx.application.Platform
import javafx.event.Event
import javafx.scene.Scene
import javafx.scene.SnapshotParameters
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.PixelFormat
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.stage.Window
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.FxUtils
import org.moinex.common.util.WindowUtils
import org.moinex.service.PreferencesService
import org.moinex.service.ThemeService
import org.moinex.ui.dialog.wallettransaction.create.AddExpenseController
import org.moinex.ui.main.MainController
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.awt.image.BufferedImage
import java.io.File
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
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ScreenshotRunner::class.java)
        private const val WINDOW_SETTLE_MILLIS = 800L
        private const val NOTIFICATIONS_SETTLE_MILLIS = 1500L
        private const val REFUSED_EXIT_CODE = 2

        fun isEnabled(): Boolean = System.getProperty(CapturePlan.PROPERTY_CAPTURE_DIR) != null
    }

    fun runIfEnabled(stage: Stage) {
        val directory = System.getProperty(CapturePlan.PROPERTY_CAPTURE_DIR) ?: return

        if (CapturePlan.isRealUserHome(System.getProperty("user.home"), System.getenv("HOME"))) {
            logger.error(
                "Capture refused: user.home is the real home. Run with a throwaway -Duser.home (see scripts/capture-screens.sh).",
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

            CaptureKind.DIALOG -> captureAddExpenseDialog(stage, target, file)

            CaptureKind.ALERT -> captureConfirmationAlert(stage, target, file)
        }
        if (file.exists()) logger.info("Saved {}", file.name) else logger.warn("Not saved: {}", file.name)
    }

    private suspend fun captureAddExpenseDialog(
        mainStage: Stage,
        target: CaptureTarget,
        file: File,
    ) {
        FxUtils.launchOnFxThread {
            delay(target.settleMillis.milliseconds)
            val dialog =
                Window
                    .getWindows()
                    .filterIsInstance<Stage>()
                    .lastOrNull { it.isShowing && it !== mainStage }

            if (dialog == null) {
                logger.error("The dialog '{}' did not open; nothing captured", target.id)
            } else {
                pressEnter(dialog)
                delay(target.settleMillis.milliseconds)
                writePng(snapshot(dialog), file)
                dialog.close()
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
        FxUtils.launchOnFxThread {
            delay(target.settleMillis.milliseconds)
            val alert =
                Window
                    .getWindows()
                    .filterIsInstance<Stage>()
                    .lastOrNull { it.isShowing && it !== mainStage }

            if (alert == null) {
                logger.error("The alert '{}' did not open; nothing captured", target.id)
            } else {
                writePng(snapshot(alert), file)
                alert.close()
            }
        }

        yield()

        WindowUtils.showConfirmationDialog("Delete this item?", "This action cannot be undone.")
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
