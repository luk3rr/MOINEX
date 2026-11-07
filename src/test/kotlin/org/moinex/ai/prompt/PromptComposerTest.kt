package org.moinex.ai.prompt

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith

class PromptComposerTest :
    BehaviorSpec({
        val composer = PromptComposer()

        Given("the English prompt") {
            val prompt = composer.compose("en")

            Then("it keeps the rules of the former single prompt") {
                prompt shouldStartWith "You are the MOINEX financial assistant"
                prompt shouldContain "no tool that writes directly to the database"
                prompt shouldContain "propose_changes"
                prompt shouldContain "record_stock_purchase"
                prompt shouldContain "ignored list"
                prompt shouldContain "allowDuplicate"
                prompt shouldContain "find_matching_transactions"
                prompt shouldContain "installmentNumber"
                prompt shouldContain "existingTransactionId"
            }
        }

        Given("the Portuguese prompt") {
            val prompt = composer.compose("pt-BR")

            Then("it has the same rules in Portuguese") {
                prompt shouldStartWith "Você é o assistente financeiro do MOINEX"
                prompt shouldContain "record_stock_purchase"
                prompt shouldContain "lista ignored"
                prompt shouldContain "allowDuplicate"
                prompt shouldContain "find_matching_transactions"
                prompt shouldContain "installmentNumber"
            }
        }

        Given("a language without prompt files") {
            Then("it falls back to English") {
                composer.compose("fr") shouldBe composer.compose("en")
            }
        }

        Given("only some domain blocks are requested") {
            val prompt = composer.compose("en", listOf("transactions"))

            Then("the core and the requested blocks are present and the others are not") {
                prompt shouldContain "You are the MOINEX financial assistant"
                prompt shouldContain "propose_changes"
                prompt shouldNotContain "Financial market operations"
                prompt shouldNotContain "credit card purchase split into installments"
            }
        }

        Given("a request for examples or mocks, with no values from the user") {
            Then("the core of every language lets the model invent values and requires all the fields") {
                composer.compose("en", listOf("transactions")) shouldContain "make up plausible fictional values"
                composer.compose("en", listOf("transactions")) shouldContain "(mock)"
                composer.compose("pt-BR", listOf("transactions")) shouldContain "invente valores plausíveis"
                composer.compose("pt-BR", listOf("transactions")) shouldContain "(mock)"
            }

            Then("the rule stays limited to examples, so real entries still take their values from the user") {
                composer.compose("en") shouldContain "applies only when the user asks for examples or tests"
                composer.compose("pt-BR") shouldContain "vale só quando o usuário pedir exemplos ou testes"
            }
        }
    })
