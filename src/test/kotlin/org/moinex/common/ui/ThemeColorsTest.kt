package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.moinex.ui.theme.ThemeTokens

class ThemeColorsTest :
    BehaviorSpec({
        fun rgb(hex: String): Int = hex.removePrefix("#").toInt(16)

        Given("the colors that code draws directly") {
            val dark = ThemeTokens.load("dark")
            val light = ThemeTokens.load("light")

            When("comparing them with the theme tokens") {
                Then("each one equals its token") {
                    val pairs =
                        listOf(
                            Triple("theme-text-primary", ThemeColors.DARK_TEXT_PRIMARY, ThemeColors.LIGHT_TEXT_PRIMARY),
                            Triple("theme-bg-card", ThemeColors.DARK_BG_CARD, ThemeColors.LIGHT_BG_CARD),
                            Triple("theme-bg-card-alt", ThemeColors.DARK_BG_CARD_ALT, ThemeColors.LIGHT_BG_CARD_ALT),
                            Triple("theme-bg-sidebar", ThemeColors.DARK_BG_SIDEBAR, ThemeColors.LIGHT_BG_SIDEBAR),
                            Triple("theme-on-sidebar", ThemeColors.DARK_ON_SIDEBAR, ThemeColors.LIGHT_ON_SIDEBAR),
                            Triple("theme-warning", ThemeColors.DARK_WARNING, ThemeColors.LIGHT_WARNING),
                            Triple("theme-alloc-track", ThemeColors.DARK_ALLOC_TRACK, ThemeColors.LIGHT_ALLOC_TRACK),
                        )
                    pairs.forEach { (token, darkHex, lightHex) ->
                        ThemeTokens.color(dark, token) shouldBe rgb(darkHex)
                        ThemeTokens.color(light, token) shouldBe rgb(lightHex)
                    }
                    ThemeTokens.color(dark, "theme-accent") shouldBe rgb(ThemeColors.DARK_ACCENT)
                    ThemeTokens.color(light, "theme-accent-text") shouldBe rgb(ThemeColors.LIGHT_ACCENT_TEXT)
                }
            }

            When("measuring the Sankey palettes against the card") {
                Then("every color reaches 3:1") {
                    val failures =
                        ThemeColors.DARK_SANKEY_PALETTE.filter {
                            ThemeTokens.contrast(rgb(it), rgb(ThemeColors.DARK_BG_CARD)) < 3.0
                        } +
                            ThemeColors.LIGHT_SANKEY_PALETTE.filter {
                                ThemeTokens.contrast(rgb(it), rgb(ThemeColors.LIGHT_BG_CARD)) < 3.0
                            }
                    failures.shouldBeEmpty()
                }
            }
        }
    })
