/*
 * Filename: ThemeService.kt
 * Created on: April 30, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service

import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Labeled
import javafx.scene.image.ImageView
import org.moinex.common.constant.Files
import org.moinex.common.extension.applyIconTheme
import org.springframework.stereotype.Service
import java.lang.ref.WeakReference

@Service
class ThemeService(
    private val preferencesService: PreferencesService,
) {
    private var mainRootRef: WeakReference<Parent>? = null

    companion object {
        private const val BYPASS_THEME_CHANGE_KEY_WORD = "colored-icon"
    }

    fun registerMainRoot(root: Parent) {
        mainRootRef = WeakReference(root)
        applyTo(root)
    }

    fun toggleAndApply() {
        preferencesService.toggleTheme()
        mainRootRef?.get()?.let { applyTo(it) }
    }

    fun applyCurrentTheme() {
        mainRootRef?.get()?.let { applyTo(it) }
    }

    fun getTokensStylesheetPath(): String =
        if (preferencesService.isDarkMode()) Files.DARK_TOKENS_CSS else Files.LIGHT_TOKENS_CSS

    fun getTokensStylesheetUrl(): String =
        ThemeService::class.java.getResource(getTokensStylesheetPath())!!.toExternalForm()

    fun applyIconsTo(node: Node) {
        applyIconThemeToTree(node, preferencesService.isDarkMode())
    }

    fun applyTo(parent: Parent) {
        val url = getTokensStylesheetUrl()
        parent.stylesheets.removeIf { it.contains(Files.CSS_THEME_PATH) }
        parent.stylesheets.add(0, url)
        applyIconThemeToTree(parent, preferencesService.isDarkMode())
    }

    fun applyTo(scene: Scene) {
        val url = getTokensStylesheetUrl()
        scene.stylesheets.removeIf { it.contains(Files.CSS_THEME_PATH) }
        scene.stylesheets.add(0, url)
        scene.root?.let { applyIconThemeToTree(it, preferencesService.isDarkMode()) }
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
