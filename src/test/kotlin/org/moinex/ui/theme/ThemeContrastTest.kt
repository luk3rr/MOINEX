package org.moinex.ui.theme

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty

class ThemeContrastTest :
    BehaviorSpec({
        val textMinimum = 4.5
        val componentMinimum = 3.0

        data class Pair(
            val foreground: String,
            val background: String,
            val minimum: Double,
        )

        val pairs =
            listOf(
                "theme-text-primary",
                "theme-text-secondary",
                "theme-accent-text",
                "theme-positive",
                "theme-negative",
                "theme-warning",
            ).flatMap { text ->
                listOf("theme-bg-base", "theme-bg-card").map { Pair(text, it, textMinimum) }
            } +
                listOf(
                    Pair("theme-on-accent", "theme-accent", textMinimum),
                    Pair("theme-on-accent", "theme-accent-focus", textMinimum),
                    Pair("theme-on-sidebar", "theme-bg-sidebar", textMinimum),
                    Pair("theme-on-sidebar", "theme-bg-sidebar-hover", textMinimum),
                    Pair("theme-border-field", "theme-bg-card", componentMinimum),
                    Pair("theme-accent-text", "theme-bg-card", componentMinimum),
                    Pair("theme-chart-projection", "theme-bg-card", componentMinimum),
                ) +
                (0..39).map { Pair("theme-chart-series-%02d".format(it), "theme-bg-card", componentMinimum) }

        Given("the color tokens of each theme") {
            listOf("light", "dark").forEach { theme ->
                val tokens = ThemeTokens.load(theme)

                When("measuring the contrast of the text, component and sidebar pairs in the $theme theme") {
                    val failures =
                        pairs.mapNotNull { pair ->
                            val ratio =
                                ThemeTokens.contrast(
                                    ThemeTokens.color(tokens, pair.foreground),
                                    ThemeTokens.color(tokens, pair.background),
                                )
                            if (ratio < pair.minimum) {
                                "${pair.foreground} on ${pair.background}: %.2f < %.1f".format(ratio, pair.minimum)
                            } else {
                                null
                            }
                        }

                    Then("every pair reaches its WCAG AA minimum") {
                        failures.shouldBeEmpty()
                    }
                }
            }
        }
    })
