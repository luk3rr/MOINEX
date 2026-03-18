package org.moinex.common.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileUtils {
    fun exists(path: Path): Boolean = Files.exists(path)

    fun getPath(
        dir: String,
        filename: String,
    ): Path = Paths.get(dir, filename)

    fun createDirectoriesIfNotExists(path: Path): Result<Path> =
        runCatching {
            if (!Files.exists(path)) {
                Files.createDirectories(path)
            } else {
                path
            }
        }
}
