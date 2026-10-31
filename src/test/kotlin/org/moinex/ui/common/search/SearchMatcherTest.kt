package org.moinex.ui.common.search

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SearchMatcherTest :
    BehaviorSpec({
        val titles =
            mapOf(
                "search.action.screen.wallets" to "Carteiras",
                "search.action.screen.wallets.keywords" to "contas, saldo",
                "search.action.entry.expense" to "Nova despesa",
                "search.action.entry.expense.keywords" to "gasto, compra",
                "search.action.tab.fire" to "Independência financeira",
                "search.action.tab.fire.keywords" to "fire, aposentadoria",
            )
        val translate: (String) -> String = { titles[it] ?: "" }
        val noop = {}
        val wallets = SearchAction("screen.wallets", SearchGroup.SCREENS, "i", run = noop)
        val expense = SearchAction("entry.expense", SearchGroup.ENTRIES, "i", run = noop)
        val fire = SearchAction("tab.fire", SearchGroup.TABS, "i", run = noop)
        val all = listOf(expense, fire, wallets)

        Given("an action of the current screen, with its label already translated") {
            val addWallet =
                SearchAction("screen.0", SearchGroup.SCREEN_ACTIONS, "i", label = "Adicionar carteira", run = noop)

            Then("it is found by its label, ignoring accents and case, without a translation key") {
                SearchMatcher.rank("ADICIONAR", listOf(addWallet), translate) shouldBe listOf(addWallet)
                SearchMatcher.rank("tabela", listOf(addWallet), translate) shouldBe emptyList()
            }
        }

        Given("a query") {
            Then("an empty query lists every action in group order") {
                SearchMatcher.rank("", all, translate) shouldBe listOf(wallets, fire, expense)
            }

            Then("accents and case are ignored") {
                SearchMatcher.rank("INDEPENDENCIA", all, translate) shouldBe listOf(fire)
            }

            Then("a synonym finds the action") {
                SearchMatcher.rank("aposentadoria", all, translate) shouldBe listOf(fire)
                SearchMatcher.rank("gasto", all, translate) shouldBe listOf(expense)
            }

            Then("a title prefix ranks before a title that contains the text, and both before a synonym") {
                SearchMatcher.rank("n", all, translate) shouldBe listOf(expense, fire, wallets)
            }

            Then("no match gives an empty list") {
                SearchMatcher.rank("zzz", all, translate) shouldBe emptyList()
            }
        }
    })
