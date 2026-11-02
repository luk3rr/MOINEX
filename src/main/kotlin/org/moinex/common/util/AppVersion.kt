/*
 * Filename: AppVersion.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.util

import org.moinex.model.SemanticVersion
import java.util.Properties

object AppVersion {
    private const val VERSION_RESOURCE = "/version.properties"
    private const val VERSION_KEY = "app.version"

    fun installedText(): String =
        AppVersion::class.java.getResourceAsStream(VERSION_RESOURCE)?.use { stream ->
            Properties().apply { load(stream) }.getProperty(VERSION_KEY)
        } ?: ""

    fun installed(): SemanticVersion? = SemanticVersion.parse(installedText())
}
