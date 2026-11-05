package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.wallet.WalletFactory

class EntityCatalogTest :
    BehaviorSpec({
        val inter = WalletFactory.create(id = 1, name = "Inter")
        val nubank = WalletFactory.create(id = 2, name = "Nubank Conta")
        val nubankSavings = WalletFactory.create(id = 3, name = "Nubank Poupança")
        val catalog =
            EntityCatalog(
                wallets = listOf(inter, nubank, nubankSavings),
                creditCards = listOf(CreditCardFactory.create(id = 9, name = "Inter Black")),
            )

        Given("the name a statement shows for the institution") {
            Then("the exact name wins, ignoring case and accents") {
                catalog.wallet("INTER")?.id shouldBe 1
                catalog.wallet("nubank poupanca")?.id shouldBe 3
            }

            Then("a longer name that contains the wallet name finds it") {
                catalog.wallet("Banco Inter")?.id shouldBe 1
                catalog.wallet("Banco Inter S.A.")?.id shouldBe 1
            }

            Then("a name shared by several wallets matches none, never guessing") {
                catalog.wallet("Nubank") shouldBe null
            }

            Then("an unrelated or blank name matches nothing") {
                catalog.wallet("Itaú") shouldBe null
                catalog.wallet("  ") shouldBe null
                catalog.wallet(null) shouldBe null
            }

            Then("cards are matched the same way") {
                catalog.creditCard("Cartão Inter Black")?.id shouldBe 9
            }
        }
    })
