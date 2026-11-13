/*
 * Filename: DataExchangeText.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.ui

import org.moinex.common.constant.TranslationKeys
import org.moinex.dataexchange.spec.Problem
import org.moinex.service.PreferencesService
import java.text.MessageFormat

class DataExchangeText(
    private val preferences: PreferencesService,
) {
    fun t(
        key: String,
        vararg args: Any?,
    ): String {
        val template = preferences.translate(key)
        return if (args.isEmpty()) template else MessageFormat.format(template, *args)
    }

    fun group(id: String): String = labelOrId(TranslationKeys.DATA_EXCHANGE_GROUP_PREFIX + id, id)

    fun entity(id: String): String = labelOrId(TranslationKeys.DATA_EXCHANGE_ENTITY_PREFIX + id, id)

    fun problem(problem: Problem): String {
        val base = preferences.translate(problem.i18nKey)
        return if (problem.message.isBlank()) base else "$base (${problem.message})"
    }

    private fun labelOrId(
        key: String,
        id: String,
    ): String {
        val label = preferences.translate(key)
        return if (label.isBlank() || label == key) id else label
    }
}
