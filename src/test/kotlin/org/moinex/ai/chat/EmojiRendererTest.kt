package org.moinex.ai.chat

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class EmojiRendererTest :
    BehaviorSpec({
        Given("a simple emoji with no variation selector") {
            When("replacing it with images") {
                val html = EmojiRenderer.replaceEmojiWithImages("Você gastou 💰 esse mês.")

                Then("it becomes an inline base64 SVG <img>") {
                    html.contains("<img src=\"data:image/svg+xml;base64,") shouldBe true
                    html.contains("Você gastou 💰 esse mês.") shouldBe false
                }
            }
        }

        Given("an emoji with a trailing VS16 variation selector") {
            When("replacing it with images") {
                val html = EmojiRenderer.replaceEmojiWithImages("Cuidado ⚠️ com isso.")

                Then("it is matched and replaced (falls back to the base codepoint file)") {
                    html.contains("<img src=\"data:image/svg+xml;base64,") shouldBe true
                    html.contains("Cuidado ⚠️ com isso.") shouldBe false
                }
            }
        }

        Given("an emoji outside the curated set used previously (no longer relevant - full set is bundled)") {
            When("replacing a less common emoji") {
                // Deliberately something that was NOT in the old hand-picked list.
                val html = EmojiRenderer.replaceEmojiWithImages("Você é um 🦸 financeiro!")

                Then("it is still recognized and replaced by the general range-based scanner") {
                    html.contains("<img src=\"data:image/svg+xml;base64,") shouldBe true
                    html.contains("Você é um 🦸 financeiro!") shouldBe false
                }
            }
        }

        Given("a ZWJ family emoji sequence (multiple codepoints joined by U+200D)") {
            When("replacing it with images") {
                val html = EmojiRenderer.replaceEmojiWithImages("Família: 👨‍👩‍👧‍👦")

                Then("the whole sequence becomes a single <img>, not one per component") {
                    val imgCount = Regex("<img ").findAll(html).count()
                    imgCount shouldBe 1
                    html.contains("Família: 👨‍👩‍👧‍👦") shouldBe false
                }
            }
        }

        Given("an emoji with a skin tone modifier") {
            When("replacing it with images") {
                val html = EmojiRenderer.replaceEmojiWithImages("👍🏽")

                Then("base and modifier are replaced together as one image") {
                    val imgCount = Regex("<img ").findAll(html).count()
                    imgCount shouldBe 1
                }
            }
        }

        Given("a flag emoji (two regional indicator symbols)") {
            When("replacing it with images") {
                val html = EmojiRenderer.replaceEmojiWithImages("🇧🇷")

                Then("both regional indicators become a single flag image") {
                    val imgCount = Regex("<img ").findAll(html).count()
                    imgCount shouldBe 1
                }
            }
        }

        Given("text with no emoji at all") {
            When("replacing emoji") {
                val html = EmojiRenderer.replaceEmojiWithImages("<p>Sem emoji aqui.</p>")

                Then("the text is returned unchanged") {
                    html shouldBe "<p>Sem emoji aqui.</p>"
                }
            }
        }

        Given("a markdown table containing an emoji in a cell") {
            When("replacing emoji") {
                val html = EmojiRenderer.replaceEmojiWithImages("<table><tr><td>✅ Pago</td></tr></table>")

                Then("only the emoji is replaced, the table markup is untouched") {
                    html.contains("<table><tr><td>") shouldBe true
                    html.contains("</td></tr></table>") shouldBe true
                    html.contains("<img src=\"data:image/svg+xml;base64,") shouldBe true
                }
            }
        }
    })
