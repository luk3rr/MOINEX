package org.moinex.app

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class CapturePlanTest :
    BehaviorSpec({
        Given("the capture plan of the first unit") {
            val targets = CapturePlan.targets

            When("listing the screens") {
                Then("it has the 16 screens of the before capture, in the agreed order") {
                    targets.map { it.id } shouldContainExactly
                        listOf(
                            "home",
                            "annual-summary",
                            "wallets",
                            "credit-cards",
                            "transactions",
                            "goals",
                            "plans",
                            "fire",
                            "wishlist",
                            "savings-overview",
                            "savings-stocks-funds",
                            "savings-bonds",
                            "ai-chat",
                            "settings",
                            "notifications",
                            "dialog-add-expense-error",
                        )
                }

                Then("the indexes are the fixed ones, starting at 1 without gaps or repeats") {
                    targets.map { it.index } shouldContainExactly (1..16).toList()
                }

                Then("every screen has a stylesheet and every dialog has none") {
                    targets
                        .filter { it.kind == CaptureKind.SCREEN }
                        .all { it.styleSheet != null } shouldBe true
                    targets.filter { it.kind == CaptureKind.DIALOG }.all { it.styleSheet == null } shouldBe true
                }
            }
        }

        Given("a target and a theme") {
            val wallets = CapturePlan.targets.first { it.id == "wallets" }

            When("building the output path") {
                val path = CapturePlan.relativePath("dark", wallets)

                Then("it is theme/NN-id.png with a two digit index") {
                    path.toString() shouldBe "dark/03-wallets.png"
                }
            }
        }

        Given("the two capture themes") {
            Then("they are light and dark") {
                CapturePlan.themes shouldContainExactly listOf("light", "dark")
            }
        }

        Given("a user.home and the HOME of the environment") {
            When("both point to the same directory, even with a trailing separator or dots") {
                Then("it is the real home and the capture must be refused") {
                    CapturePlan.isRealUserHome("/home/someone", "/home/someone") shouldBe true
                    CapturePlan.isRealUserHome("/home/someone/", "/home/someone") shouldBe true
                    CapturePlan.isRealUserHome("/home/someone/./", "/home/someone") shouldBe true
                }
            }

            When("user.home is a throwaway directory") {
                Then("the capture is allowed") {
                    CapturePlan.isRealUserHome("/tmp/moinex-capture-abc", "/home/someone") shouldBe false
                }
            }

            When("the environment has no HOME or user.home is empty") {
                Then("it is not considered the real home") {
                    CapturePlan.isRealUserHome("/tmp/x", null) shouldBe false
                    CapturePlan.isRealUserHome("", "/home/someone") shouldBe false
                }
            }
        }
    })
