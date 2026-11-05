package org.moinex.ai

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import java.io.File

/**
 * Guards the rule that the assistant never writes: the tools it can call and the review screens only read or hand a
 * proposal over, while every write goes through an `AppOperation` in `ai/operation`.
 */
class AiArchitectureTest :
    BehaviorSpec({
        val root = File("src/main/kotlin/org/moinex")
        val writeCall =
            Regex(
                """\b\w+Service\s*\.\s*(create|update|delete|remove|pay|archive|unarchive|add|register|confirm|set|""" +
                    """sell|buy|rebuild|refresh|reset|transfer|save)\w*\(""",
            )

        fun kotlinFiles(relative: String): List<File> =
            File(root, relative).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

        fun writesIn(files: List<File>): List<String> =
            files.flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (writeCall.containsMatchIn(line)) "${file.name}:${index + 1}: ${line.trim()}" else null
                }
            }

        Given("the tools the assistant can call") {
            val files = kotlinFiles("ai/tools")

            Then("they exist and none calls a write method of a domain service") {
                files.shouldNotBeEmpty()
                writesIn(files).shouldBeEmpty()
            }
        }

        Given("the chat orchestration") {
            Then("it does not write either") {
                writesIn(kotlinFiles("ai/chat")).shouldBeEmpty()
            }
        }

        Given("the review screens of assistant proposals") {
            val files = kotlinFiles("ui/dialog/ai")

            Then("they exist and persist nothing themselves, only through operations") {
                files.shouldNotBeEmpty()
                writesIn(files).shouldBeEmpty()
            }
        }

        Given("the operations package") {
            val files = kotlinFiles("ai/operation")

            Then("no operation is named or implemented as a deletion") {
                files.shouldNotBeEmpty()
                val deletions =
                    files.flatMap { file ->
                        file.readLines().filter { line ->
                            line.contains("override val name") &&
                                listOf("delete", "remove", "drop", "purge").any { line.contains("\"$it") }
                        }
                    }
                deletions.shouldBeEmpty()
                val deleteCalls =
                    files.flatMap { file ->
                        file.readLines().filter { Regex("""Service\s*\.\s*delete\w*\(""").containsMatchIn(it) }
                    }
                deleteCalls.shouldBeEmpty()
            }
        }
    })
