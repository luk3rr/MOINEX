/*
 * Filename: EmbeddedPython.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.util

import java.io.File

object EmbeddedPython {
    private const val WINDOWS_RELATIVE_PATH = "python-embedded\\python.exe"
    private const val LINUX_RELATIVE_PATH = "../lib/python/bin/python3"

    /**
     * Resolves the Python interpreter bundled with a jpackage installation.
     *
     * @param osName lowercase value of the `os.name` system property
     * @param appPath value of the `jpackage.app-path` system property, null when not running from a package
     * @param exists file existence check, injectable for tests
     * @return the absolute interpreter path, or null when no bundled interpreter is available
     */
    fun resolve(
        osName: String,
        appPath: String?,
        exists: (File) -> Boolean = File::exists,
    ): String? {
        val appDir = appPath?.let { File(it).parentFile } ?: return null

        val candidate =
            if (osName.contains("win")) {
                File(appDir, WINDOWS_RELATIVE_PATH)
            } else {
                File(appDir, LINUX_RELATIVE_PATH)
            }

        return candidate
            .takeIf(exists)
            ?.absoluteFile
            ?.normalize()
            ?.path
    }
}
