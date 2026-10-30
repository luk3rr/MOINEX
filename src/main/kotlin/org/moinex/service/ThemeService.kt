/*
 * Filename: ThemeService.kt
 * Created on: April 30, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service

import atlantafx.base.theme.Dracula
import atlantafx.base.theme.PrimerLight
import javafx.application.Application
import javafx.collections.ListChangeListener
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Labeled
import javafx.scene.image.ImageView
import javafx.stage.Window
import org.moinex.common.constant.Files
import org.moinex.common.extension.applyIconTheme
import org.springframework.stereotype.Service
import java.lang.ref.WeakReference

/**
 * Base theme of the AtlantaFX library used under the MOINEX tokens.
 */
enum class BaseTheme {
    DRACULA,
    PRIMER_LIGHT,
    ;

    companion object {
        fun forMode(isDarkMode: Boolean): BaseTheme = if (isDarkMode) DRACULA else PRIMER_LIGHT
    }
}

/**
 * Applies the AtlantaFX base theme and the MOINEX token layer to every window of the application,
 * including windows opened later and native alerts.
 */
@Service
class ThemeService(
    private val preferencesService: PreferencesService,
) {
    private var mainRootRef: WeakReference<Parent>? = null
    private var installed = false

    companion object {
        private const val BYPASS_THEME_CHANGE_KEY_WORD = "colored-icon"
    }

    /**
     * Sets the base theme and starts applying the token layer to every window. Must run on the JavaFX
     * application thread, once, before the first window is shown.
     */
    fun install() {
        if (installed) return
        installed = true

        applyBaseTheme()

        Window.getWindows().addListener(
            ListChangeListener<Window> { change ->
                while (change.next()) {
                    if (change.wasAdded()) {
                        change.addedSubList.forEach(::applyToWindow)
                    }
                }
            },
        )
        Window.getWindows().forEach(::applyToWindow)
    }

    fun registerMainRoot(root: Parent) {
        mainRootRef = WeakReference(root)
        applyTo(root)
    }

    fun isDarkMode(): Boolean = preferencesService.isDarkMode()

    fun toggleAndApply() {
        preferencesService.toggleTheme()
        applyCurrentTheme()
    }

    fun applyCurrentTheme() {
        applyBaseTheme()
        Window.getWindows().forEach(::applyToWindow)
        mainRootRef?.get()?.let { applyTo(it) }
    }

    fun getTokensStylesheetPath(): String =
        if (preferencesService.isDarkMode()) Files.DARK_TOKENS_CSS else Files.LIGHT_TOKENS_CSS

    fun getTokensStylesheetUrl(): String =
        ThemeService::class.java.getResource(getTokensStylesheetPath())!!.toExternalForm()

    private fun themeStylesheetUrls(): List<String> =
        listOf(
            getTokensStylesheetUrl(),
            ThemeService::class.java.getResource(Files.THEME_BASE_CSS)!!.toExternalForm(),
        )

    fun applyIconsTo(node: Node) {
        applyIconThemeToTree(node, preferencesService.isDarkMode())
    }

    fun applyTo(parent: Parent) {
        parent.stylesheets.removeIf { it.contains(Files.CSS_THEME_PATH) }
        parent.stylesheets.addAll(0, themeStylesheetUrls())
        applyIconThemeToTree(parent, preferencesService.isDarkMode())
    }

    fun applyTo(scene: Scene) {
        scene.stylesheets.removeIf { it.contains(Files.CSS_THEME_PATH) }
        scene.stylesheets.addAll(0, themeStylesheetUrls())
        scene.root?.let { applyIconThemeToTree(it, preferencesService.isDarkMode()) }
    }

    private fun applyBaseTheme() {
        val theme =
            when (BaseTheme.forMode(preferencesService.isDarkMode())) {
                BaseTheme.DRACULA -> Dracula()
                BaseTheme.PRIMER_LIGHT -> PrimerLight()
            }
        Application.setUserAgentStylesheet(theme.userAgentStylesheet)
    }

    private fun applyToWindow(window: Window) {
        window.scene?.let { applyTo(it) }
    }

    private fun applyIconThemeToTree(
        node: Node,
        isDarkMode: Boolean,
    ) {
        if (node is ImageView && !node.styleClass.contains(BYPASS_THEME_CHANGE_KEY_WORD)) {
            node.applyIconTheme(isDarkMode)
        }

        // Labeled covers Button, MenuButton, Label, CheckBox, etc. whose graphic
        // is a property and may not appear in childrenUnmodifiable before the skin is built.
        if (node is Labeled) {
            node.graphic?.let { applyIconThemeToTree(it, isDarkMode) }
        }

        if (node is Parent) {
            node.childrenUnmodifiable.forEach { applyIconThemeToTree(it, isDarkMode) }
        }
    }
}
