/*
 * Filename: EmbeddedPythonTest.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class EmbeddedPythonTest :
    BehaviorSpec({
        Given("a Linux jpackage installation") {
            val appPath = "/opt/moinex/bin/Moinex"

            When("the bundled interpreter exists") {
                val result = EmbeddedPython.resolve("linux", appPath) { it.path.endsWith("python3") }

                Then("the interpreter inside lib/python is returned") {
                    result shouldBe "/opt/moinex/lib/python/bin/python3"
                }
            }
            When("the bundled interpreter is missing") {
                Then("null is returned so the caller falls back to the system Python") {
                    EmbeddedPython.resolve("linux", appPath) { false } shouldBe null
                }
            }
        }

        Given("no jpackage installation") {
            When("jpackage.app-path is not set") {
                Then("null is returned on Linux and Windows") {
                    EmbeddedPython.resolve("linux", null) { true } shouldBe null
                    EmbeddedPython.resolve("windows 11", null) { true } shouldBe null
                }
            }
        }

        Given("a Windows jpackage installation") {
            val appPath = "/app/Moinex.exe"

            When("the embedded python.exe exists") {
                val result = EmbeddedPython.resolve("windows 11", appPath) { it.name.endsWith("python.exe") }

                Then("the python-embedded interpreter next to the launcher is returned") {
                    result?.endsWith("python-embedded\\python.exe") shouldBe true
                }
            }
            When("the embedded python.exe is missing") {
                Then("null is returned") {
                    EmbeddedPython.resolve("windows 11", appPath) { false } shouldBe null
                }
            }
        }

        Given("a Linux package that also has a Windows-style folder") {
            When("only the Windows layout exists") {
                Then("it is ignored on Linux") {
                    EmbeddedPython.resolve(
                        "linux",
                        "/opt/moinex/bin/Moinex",
                    ) { it.name.endsWith("python.exe") } shouldBe
                        null
                }
            }
        }
    })
