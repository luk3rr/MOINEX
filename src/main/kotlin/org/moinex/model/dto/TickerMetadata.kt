/*
 * Filename: TickerMetadata.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto

import org.json.JSONObject
import org.moinex.common.constant.Constants
import java.time.LocalDateTime

/**
 * What the app knows about a ticker that has no column of its own, kept as one extensible JSON document. The price,
 * its moment and the website stay in the ticker's columns. Reading is lenient: missing, null or malformed fields
 * become null, and the keys this version does not know are kept in [other], so a record written by a newer version
 * survives a round trip.
 */
data class TickerMetadata(
    val currency: String? = null,
    val historyUpdatedAt: LocalDateTime? = null,
    val other: Map<String, Any> = emptyMap(),
) {
    fun toJson(): String {
        val json = JSONObject(other)
        json.put(SCHEMA_VERSION_FIELD, SCHEMA_VERSION)
        currency?.let { json.put(CURRENCY_FIELD, it) }
        historyUpdatedAt?.let { json.put(HISTORY_UPDATED_AT_FIELD, it.format(Constants.DB_DATE_FORMATTER)) }
        return json.toString()
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val SCHEMA_VERSION_FIELD = "schema_version"
        const val CURRENCY_FIELD = "currency"
        const val HISTORY_UPDATED_AT_FIELD = "history_updated_at"

        private val KNOWN_FIELDS = setOf(SCHEMA_VERSION_FIELD, CURRENCY_FIELD, HISTORY_UPDATED_AT_FIELD)

        fun fromJson(json: String?): TickerMetadata {
            if (json.isNullOrBlank()) return TickerMetadata()

            val parsed = runCatching { JSONObject(json) }.getOrNull() ?: return TickerMetadata()

            return TickerMetadata(
                currency = parsed.textOrNull(CURRENCY_FIELD),
                historyUpdatedAt =
                    parsed.textOrNull(HISTORY_UPDATED_AT_FIELD)?.let {
                        runCatching { LocalDateTime.parse(it, Constants.DB_DATE_FORMATTER) }.getOrNull()
                    },
                other =
                    parsed
                        .keySet()
                        .filter { it !in KNOWN_FIELDS && !parsed.isNull(it) }
                        .associateWith { parsed.get(it) },
            )
        }

        private fun JSONObject.textOrNull(key: String): String? =
            if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    }
}
