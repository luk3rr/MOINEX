/*
 * Filename: ArchiveSecurityTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.dhatim.fastexcel.Workbook
import org.moinex.dataexchange.spec.CellType
import org.moinex.dataexchange.spec.Column
import org.moinex.dataexchange.tabular.TabularFiles
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ArchiveSecurityTest :
    BehaviorSpec({
        Given("an XLSX whose XML declares an external entity that points to a local file") {
            val secret = Files.createTempFile("secret", ".txt")
            Files.writeString(secret, "TOP-SECRET-CONTENT")
            val plain = Files.createTempFile("plain", ".xlsx")
            Files.newOutputStream(plain).use { out ->
                Workbook(out, "test", "16.0300").use { wb ->
                    val meta = wb.newWorksheet("_meta")
                    meta.value(0, 0, "key")
                    meta.value(0, 1, "value")
                    meta.value(1, 0, "app")
                    meta.value(1, 1, "moinex")
                    val sheet = wb.newWorksheet("wallets")
                    sheet.value(0, 0, "name")
                    sheet.value(1, 0, "PLACEHOLDER")
                }
            }
            val crafted = Files.createTempFile("xxe", ".xlsx")
            ZipFile(plain.toFile()).use { zip ->
                ZipOutputStream(Files.newOutputStream(crafted)).use { out ->
                    zip.entries().asSequence().forEach { entry ->
                        out.putNextEntry(ZipEntry(entry.name))
                        var bytes = zip.getInputStream(entry).readBytes()
                        if (entry.name.endsWith(".xml") && String(bytes).contains("PLACEHOLDER")) {
                            val text = String(bytes)
                            val declaration = text.substringBefore("?>") + "?>"
                            val body = text.removePrefix(declaration).replace("PLACEHOLDER", "&xxe;")
                            bytes =
                                (
                                    "$declaration<!DOCTYPE x [<!ENTITY xxe SYSTEM " +
                                        "\"file://${secret.toAbsolutePath()}\">]>" +
                                        body
                                ).toByteArray()
                        }
                        out.write(bytes)
                        out.closeEntry()
                    }
                }
            }

            When("the file is read") {
                val seen =
                    runCatching {
                        TabularFiles.open(crafted).use { reader ->
                            reader
                                .rows(
                                    "wallets",
                                    listOf(Column("name", CellType.TEXT)),
                                ).map { it.cells["name"] }
                                .toList()
                        }
                    }

                Then("the external entity is never resolved") {
                    seen.getOrNull().orEmpty().none { it?.contains("TOP-SECRET-CONTENT") == true } shouldBe true
                }
            }
        }
    })
