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
                prompt shouldContain "propose_transactions"
                prompt shouldContain "propose_investment_operations"
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
                prompt shouldContain "propose_investment_operations"
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
                prompt shouldContain "propose_transactions"
                prompt shouldNotContain "Financial market operations"
                prompt shouldNotContain "credit card purchase split into installments"
            }
        }
    })
