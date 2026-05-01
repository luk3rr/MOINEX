/*
 * Filename: ThemeService.kt
 * Created on: April 30, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service

import javafx.scene.Parent
import javafx.scene.Scene
import org.moinex.common.constant.Files
import org.springframework.stereotype.Service
import java.lang.ref.WeakReference

@Service
class ThemeService(
    private val preferencesService: PreferencesService,
) {
    private var mainRootRef: WeakReference<Parent>? = null

    fun registerMainRoot(root: Parent) {
        mainRootRef = WeakReference(root)
        applyTo(root)
    }

    fun toggleAndApply() {
        preferencesService.toggleTheme()
        mainRootRef?.get()?.let { applyTo(it) }
    }

    fun getTokensStylesheetPath(): String =
        if (preferencesService.isDarkMode()) Files.DARK_TOKENS_CSS else Files.LIGHT_TOKENS_CSS

    fun getTokensStylesheetUrl(): String =
        ThemeService::class.java.getResource(getTokensStylesheetPath())!!.toExternalForm()

    fun applyTo(parent: Parent) {
        val url = getTokensStylesheetUrl()
        parent.stylesheets.removeIf { it.contains("/css/theme/") }
        parent.stylesheets.add(0, url)
    }

    fun applyTo(scene: Scene) {
        val url = getTokensStylesheetUrl()
        scene.stylesheets.removeIf { it.contains("/css/theme/") }
        scene.stylesheets.add(0, url)
    }
}
