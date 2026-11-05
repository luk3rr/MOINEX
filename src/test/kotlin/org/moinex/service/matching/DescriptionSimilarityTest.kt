package org.moinex.service.matching

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class DescriptionSimilarityTest :
    BehaviorSpec({
        Given("two descriptions of the same expense written differently") {
            Then("noise words, accents and case are ignored") {
                DescriptionSimilarity.tokens("PIX COMPRA Padaria Pão Quente 123") shouldBe
                    setOf("padaria", "pao", "quente")
            }

            Then("identical tokens score one and a statement name that only adds words scores just below") {
                DescriptionSimilarity.score("Academia", "academia") shouldBe 1.0
                DescriptionSimilarity.score("PIX ACADEMIA FIT", "Academia") shouldBe 0.9
            }

            Then("partial overlap scores between zero and one") {
                DescriptionSimilarity.score("Mercado Central Centro", "Mercado Sul") shouldBeGreaterThan 0.0
            }
        }

        Given("descriptions with nothing in common or nothing meaningful") {
            Then("the score is zero") {
                DescriptionSimilarity.score("Uber", "Academia") shouldBe 0.0
                DescriptionSimilarity.score("PIX 123", "Academia") shouldBe 0.0
                DescriptionSimilarity.score(null, "Academia") shouldBe 0.0
            }
        }
    })
