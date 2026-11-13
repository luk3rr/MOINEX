/*
 * Filename: FileGuardTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.tabular

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.dataexchange.spec.ProblemCodes
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class FileGuardTest :
    BehaviorSpec({
        fun zipWith(
            suffix: String,
            block: (ZipOutputStream) -> Unit,
        ): Path {
            val file = Files.createTempFile("guard", suffix)
            ZipOutputStream(Files.newOutputStream(file)).use(block)
            return file
        }

        fun ZipOutputStream.add(
            name: String,
            content: ByteArray = "x".toByteArray(),
        ) {
            putNextEntry(ZipEntry(name))
            write(content)
            closeEntry()
        }

        Given("a valid small zip") {
            Then("it passes") {
                FileGuard().check(zipWith(".zip") { it.add("a.csv") }) shouldBe null
            }
        }

        Given("files that are not exports") {
            Then("a wrong extension is E01") {
                val txt = Files.createTempFile("guard", ".txt")
                FileGuard().check(txt)?.code shouldBe ProblemCodes.NOT_MOINEX_FILE
            }
            Then("a .zip that is not a zip is E01") {
                val fake = Files.createTempFile("guard", ".zip")
                Files.writeString(fake, "not a zip")
                FileGuard().check(fake)?.code shouldBe ProblemCodes.NOT_MOINEX_FILE
            }
            Then("a missing file is E01") {
                FileGuard().check(Path.of("/nonexistent/file.zip"))?.code shouldBe ProblemCodes.NOT_MOINEX_FILE
            }
        }

        Given("files over the limits") {
            Then("a file over the size limit is E04") {
                val file = zipWith(".zip") { it.add("a.csv", ByteArray(2000) { i -> (i * 31).toByte() }) }
                FileGuard(maxFileBytes = 100).check(file)?.code shouldBe ProblemCodes.FILE_TOO_LARGE
            }
            Then("too many entries is E04") {
                val file = zipWith(".zip") { z -> repeat(5) { z.add("e$it.csv") } }
                FileGuard(maxEntries = 3).check(file)?.code shouldBe ProblemCodes.FILE_TOO_LARGE
            }
            Then("a zip slip name is E04") {
                val file = zipWith(".zip") { it.add("../evil.csv") }
                FileGuard().check(file)?.code shouldBe ProblemCodes.FILE_TOO_LARGE
            }
            Then("a zip bomb by ratio is E04") {
                val file = zipWith(".zip") { it.add("bomb.csv", ByteArray(5 * 1024 * 1024)) }
                FileGuard(maxRatio = 50).check(file)?.code shouldBe ProblemCodes.FILE_TOO_LARGE
            }
            Then("too much uncompressed data is E04") {
                val file = zipWith(".zip") { it.add("big.csv", ByteArray(4000) { i -> (i * 17).toByte() }) }
                FileGuard(maxUncompressedBytes = 1000).check(file)?.code shouldBe ProblemCodes.FILE_TOO_LARGE
            }
        }
    })
