package org.moinex.ai.chat

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class MarkdownRendererTest :
    BehaviorSpec({
        Given("basic Markdown formatting") {
            When("converting bold, a heading and a list") {
                val html =
                    MarkdownRenderer.toHtmlFragment(
                        """
                        **Você gastou** R${'$'} 100.

                        # Resumo

                        - Mercado
                        - Restaurante
                        """.trimIndent(),
                    )

                Then("it produces the matching HTML tags") {
                    html.contains("<strong>Você gastou</strong>") shouldBe true
                    html.contains("<h1>Resumo</h1>") shouldBe true
                    html.contains("<li>Mercado</li>") shouldBe true
                }
            }
        }

        Given("a reply containing a literal <script> tag") {
            When("converting it") {
                val html = MarkdownRenderer.toHtmlFragment("Olá <script>alert('x')</script>")

                Then("the tag is escaped as text, not rendered as an executable element") {
                    html.contains("<script>") shouldBe false
                    html.contains("&lt;script&gt;") shouldBe true
                }
            }
        }

        Given("a link with a javascript: URI") {
            When("converting it") {
                val html = MarkdownRenderer.toHtmlFragment("[clique aqui](javascript:alert(1))")

                Then("the href is stripped so it can't execute") {
                    html.contains("javascript:") shouldBe false
                }
            }
        }

        Given("a link with a plain https URI") {
            When("converting it") {
                val html = MarkdownRenderer.toHtmlFragment("[MOINEX](https://example.com)")

                Then("the href is kept") {
                    html.contains("href=\"https://example.com\"") shouldBe true
                }
            }
        }

        Given("a GitHub-Flavored-Markdown pipe table (e.g. a financial summary)") {
            When("converting it") {
                val html =
                    MarkdownRenderer.toHtmlFragment(
                        """
                        | Componente | Valor |
                        |---|---|
                        | Ativos | R${'$'} 27.625,03 |
                        | Passivos | R${'$'} 3.443,35 |
                        """.trimIndent(),
                    )

                Then("it renders as an actual HTML table, not raw pipe syntax") {
                    html.contains("<table>") shouldBe true
                    html.contains("<th>Componente</th>") shouldBe true
                    html.contains("<td>Ativos</td>") shouldBe true
                    html.contains("|---|") shouldBe false
                }
            }
        }
    })
