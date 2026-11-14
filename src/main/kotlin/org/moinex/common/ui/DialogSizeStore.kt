/*
 * Filename: DialogSizeStore.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

import java.util.prefs.Preferences

/**
 * Remembers the size the user gave to each dialog, by a key that names the dialog.
 */
interface DialogSizeStore {
    fun load(key: String): DialogResize.Size?

    fun save(
        key: String,
        size: DialogResize.Size,
    )

    fun clear(key: String)
}

/**
 * [DialogSizeStore] over the user preferences, one entry `width,height` per dialog.
 */
class PreferencesDialogSizeStore(
    private val preferences: Preferences,
) : DialogSizeStore {
    override fun load(key: String): DialogResize.Size? {
        val parts = preferences.get(prefKey(key), null)?.split(SEPARATOR) ?: return null
        val width = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
        val height = parts.getOrNull(1)?.toDoubleOrNull() ?: return null
        return DialogResize.Size(width, height).takeIf { width > 0 && height > 0 }
    }

    override fun save(
        key: String,
        size: DialogResize.Size,
    ) {
        preferences.put(prefKey(key), "${size.width}$SEPARATOR${size.height}")
    }

    override fun clear(key: String) {
        preferences.remove(prefKey(key))
    }

    private fun prefKey(key: String): String = (PREFIX + key).take(Preferences.MAX_KEY_LENGTH)

    companion object {
        private const val PREFIX = "dialog.size."
        private const val SEPARATOR = ","
    }
}
