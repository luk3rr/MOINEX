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
