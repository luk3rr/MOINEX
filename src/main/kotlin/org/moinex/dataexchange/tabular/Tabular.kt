/*
 * Filename: Tabular.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import org.moinex.dataexchange.spec.Column
import org.moinex.dataexchange.spec.RawRow
import java.io.Closeable

object TabularLayout {
    const val META_SHEET = "_meta"
    const val META_APP = "app"
    const val META_APP_VALUE = "moinex"
    const val META_LAYOUT_VERSION = "layout_version"
    const val META_EXPORTED_AT = "exported_at"
    const val META_APP_VERSION = "app_version"
    const val META_ENTITIES = "entities"
    const val CURRENT_LAYOUT_VERSION = 1
}

interface TabularWriter : Closeable {
    fun writeMeta(meta: Map<String, String>)

    fun writeSheet(
        name: String,
        columns: List<Column>,
        rows: Sequence<Map<String, Any?>>,
        onRow: () -> Unit = {},
    )
}

interface TabularReader : Closeable {
    fun meta(): Map<String, String>?

    fun sheetNames(): Set<String>

    fun header(sheet: String): List<String>

    fun rows(
        sheet: String,
        columns: List<Column>,
    ): Sequence<RawRow>
}
