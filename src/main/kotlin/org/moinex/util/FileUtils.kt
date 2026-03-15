package org.moinex.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileUtils {
    fun exists(path: Path): Boolean = Files.exists(path)

    fun getPath(
        dir: String,
        filename: String,
    ): Path = Paths.get(dir, filename)
}
