/*
 * Filename: JavaFXApp.kt (original filename: JavaFXApp.java)
 * Created on: September 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.app

import javafx.application.Application
import javafx.application.HostServices
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.APIUtils
import org.moinex.common.util.FxUtils
import org.moinex.common.util.WindowUtils
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.getBean
import org.springframework.context.ConfigurableApplicationContext

class JavaFXApp : Application() {
    private lateinit var springContext: ConfigurableApplicationContext

    companion object {
        private val logger = LoggerFactory.getLogger(JavaFXApp::class.java)
        private lateinit var hostServicesInstance: HostServices

        fun getHostServicesInstance(): HostServices {
            check(::hostServicesInstance.isInitialized) {
                "HostServices not initialized yet"
            }
            return hostServicesInstance
        }
    }

    override fun init() {
        val args = parameters.raw.toTypedArray()
        springContext = SpringApp.start(args)
    }

    override fun start(primaryStage: Stage) {
        hostServicesInstance = hostServices

        runCatching {
            val splashLoader = FXMLLoader(javaClass.getResource(Files.SPLASH_SCREEN_FXML))
            val splashRoot = splashLoader.load<Parent>()
            val splashStage =
                Stage().apply {
                    initStyle(StageStyle.UNDECORATED)
                    scene = Scene(splashRoot)
                    show()
                }

            FxUtils.launchBackgroundThenUI(
                background = {
                    val preferencesService = springContext.getBean<PreferencesService>()
                    val loader =
                        FXMLLoader(
                            javaClass.getResource(Files.MAIN_FXML),
                            preferencesService.bundle,
                        )
                    loader.setControllerFactory { springContext.getBean(it) }
                    val mainRoot = loader.load<Parent>()

                    delay(1000)

                    Pair(preferencesService, mainRoot)
                },
                onUI = { (preferencesService, mainRoot) ->
                    primaryStage.apply {
                        title = preferencesService.translate(TranslationKeys.APP_TITLE)
                        icons.addAll(
                            listOf("256", "128", "64", "32").mapNotNull { size ->
                                javaClass
                                    .getResourceAsStream("${Files.APP_ICONS_PATH}moinex-icon-$size.png")
                                    ?.let { Image(it) }
                            },
                        )
                        scene = Scene(mainRoot)
                        show()
                    }
                    splashStage.close()

                    val fallbackPath = System.getProperty(SpringApp.DB_FALLBACK_PROPERTY)
                    if (fallbackPath != null) {
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(TranslationKeys.STARTUP_DB_INACCESSIBLE_HEADER),
                            preferencesService.translate(TranslationKeys.STARTUP_DB_INACCESSIBLE_MESSAGE) +
                                "\n$fallbackPath",
                        )
                    }
                },
            )
        }.onFailure { e ->
            logger.error("Failed to start JavaFX application", e)
            Platform.exit()
        }
    }

    override fun stop() {
        runCatching {
            runBlocking {
                APIUtils.shutdownExecutor()
            }
            springContext.close()
            super.stop()
        }.onFailure { e ->
            logger.error("Error during application shutdown", e)
        }
        Platform.exit()
    }
}
