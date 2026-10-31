package org.moinex.ui.common.search

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import java.util.Properties

class SearchActionCatalogTest :
    BehaviorSpec({
        val host =
            object : SearchHost {
                override fun showScreen(
                    screen: SearchScreen,
                    tabIndex: Int?,
                ) = Unit

                override fun openEntry(entry: SearchEntry) = Unit

                override fun toggleTheme() = Unit

                override fun openCalendar() = Unit

                override fun openCalculator() = Unit

                override fun toggleNotifications() = Unit

                override fun runScreenAction(
                    screen: SearchScreen,
                    tabIndex: Int?,
                    index: Int,
                ) = Unit
            }
        val actions = SearchActionCatalog(host).actions()

        fun bundle(name: String): Properties =
            Properties().apply {
                SearchActionCatalogTest::class.java
                    .getResourceAsStream("/i18n/$name.properties")!!
                    .use { load(it) }
            }

        Given("the catalog") {
            Then("it has 23 actions with unique ids") {
                actions.size shouldBe 23
                actions.map { it.id }.toSet().size shouldBe 23
            }

            Then("every screen action of the global search has its labels in both languages") {
                listOf("messages_en", "messages_pt_BR").forEach { name ->
                    val props = bundle(name)
                    val keys =
                        ScreenActionEntries.all.flatMap { entry -> listOf(entry.labelKey) + entry.contextKeys }
                    keys.filter { props.getProperty(it).isNullOrBlank() }.shouldBeEmpty()
                }
                SearchActionCatalog(host).screenActions { it }.size shouldBe ScreenActionEntries.all.size
            }

            Then("every action and group has a title in both languages") {
                listOf("messages_en", "messages_pt_BR").forEach { name ->
                    val props = bundle(name)
                    val missing =
                        actions.flatMap { listOf(it.titleKey, it.keywordsKey) } +
                            SearchGroup.entries.map { it.titleKey }
                    missing.filter { props.getProperty(it).isNullOrBlank() }.shouldBeEmpty()
                }
            }
        }
    })
