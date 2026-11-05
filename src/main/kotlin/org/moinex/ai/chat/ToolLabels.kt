/*
 * Filename: ToolLabels.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

import org.moinex.service.PreferencesService

/** The name a person reads for what the assistant is doing, shown while it works; unknown tools show their name. */
object ToolLabels {
    fun of(
        toolName: String,
        preferencesService: PreferencesService,
    ): String =
        runCatching { preferencesService.translate("aitool.label.$toolName") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() && it != "aitool.label.$toolName" }
            ?: toolName
}
