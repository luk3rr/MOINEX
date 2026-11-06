/*
 * Filename: APIUtilsScriptDirectoryTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.common.constant.Scripts
import org.moinex.exception.MoinexException
import java.nio.file.Files

class APIUtilsScriptDirectoryTest :
    BehaviorSpec({
        Given("a script the app ships") {
            val allScripts =
                listOf(
                    Scripts.GET_STOCK_PRICE_SCRIPT,
                    Scripts.GET_STOCK_PRICE_HISTORY_SCRIPT,
                    Scripts.GET_FUNDAMENTAL_DATA_SCRIPT,
                    Scripts.GET_BRAZILIAN_MARKET_INDICATORS_SCRIPT,
                    Scripts.GET_MARKET_INDICATOR_HISTORY_SCRIPT,
                    Scripts.GET_STOCK_LOGO_SCRIPT,
                )

            When("its directory is prepared") {
                Then("it holds the script and the shared module, side by side") {
                    allScripts.forEach { script ->
                        val directory = APIUtils.prepareScriptDirectory(script)
                        try {
                            Files.exists(directory.resolve(script)) shouldBe true
                            Files.exists(directory.resolve(Scripts.RESILIENCE_MODULE)) shouldBe true
                            (Files.size(directory.resolve(script)) > 0) shouldBe true
                        } finally {
                            directory.toFile().deleteRecursively()
                        }
                    }
                }
            }
        }

        Given("a script that does not exist") {
            When("its directory is prepared") {
                Then("it fails and leaves no directory behind") {
                    val before =
                        Files.list(Files.createTempDirectory("probe").parent).use { stream ->
                            stream.filter { it.fileName.toString().startsWith("moinex-script-") }.count()
                        }

                    shouldThrow<MoinexException.ScriptNotFoundException> {
                        APIUtils.prepareScriptDirectory("missing_script.py")
                    }

                    val after =
                        Files.list(Files.createTempDirectory("probe").parent).use { stream ->
                            stream.filter { it.fileName.toString().startsWith("moinex-script-") }.count()
                        }
                    after shouldBe before
                }
            }
        }
    })
