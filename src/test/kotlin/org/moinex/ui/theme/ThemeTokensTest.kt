package org.moinex.ui.theme

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class ThemeTokensTest :
    BehaviorSpec({
        val contractTokens =
            listOf(
                "theme-bg-base",
                "theme-bg-card",
                "theme-bg-inset",
                "theme-border",
                "theme-border-field",
                "theme-text-primary",
                "theme-text-secondary",
                "theme-text-subtle",
                "theme-accent",
                "theme-on-accent",
                "theme-accent-focus",
                "theme-accent-text",
                "theme-positive",
                "theme-negative",
                "theme-warning",
                "theme-bg-sidebar",
                "theme-bg-sidebar-hover",
                "theme-on-sidebar",
                "theme-icon",
            )

        Given("the light and the dark token stylesheets") {
            val light = ThemeTokens.load("light")
            val dark = ThemeTokens.load("dark")

            When("comparing the token names") {
                Then("both define exactly the same set of tokens") {
                    (light.keys - dark.keys).shouldBeEmpty()
                    (dark.keys - light.keys).shouldBeEmpty()
                }
            }

            When("looking for the tokens of the design contract") {
                Then("every contract token exists in both themes") {
                    contractTokens.filterNot { it in light }.shouldBeEmpty()
                    contractTokens.filterNot { it in dark }.shouldBeEmpty()
                }
            }

            When("reading the identity colors") {
                Then("the fill accent is the MOINEX purple in both themes") {
                    ThemeTokens.color(light, "theme-accent") shouldBe 0xBD93F9
                    ThemeTokens.color(dark, "theme-accent") shouldBe 0xBD93F9
                }

                Then("the sidebar keeps its theme colors") {
                    ThemeTokens.color(dark, "theme-bg-sidebar") shouldBe 0x0A614E
                    ThemeTokens.color(light, "theme-bg-sidebar") shouldBe 0x1ABC9C
                }
            }
        }
    })
