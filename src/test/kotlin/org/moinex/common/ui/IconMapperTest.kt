package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class IconMapperTest :
    BehaviorSpec({
        val walletTypeFiles =
            listOf(
                "broker.png",
                "checking.png",
                "cryptocurrency.png",
                "food_voucher.png",
                "goal.png",
                "meal_voucher.png",
                "others.png",
                "savings.png",
                "wallet.png",
            )

        Given("the nine wallet type icon files stored in the database") {
            When("mapping each of them") {
                Then("each one has its own known literal") {
                    walletTypeFiles.forEach { file ->
                        IconMapper.isKnownWalletTypeIcon(file) shouldBe true
                        IconMapper.walletTypeLiteral(file) shouldNotBe Icons.DEFAULT
                    }
                }
            }
        }

        Given("an icon file name that is not one of the known ones") {
            When("mapping it, or a null name") {
                Then("it falls back to the default icon") {
                    IconMapper.walletTypeLiteral("custom.png") shouldBe Icons.DEFAULT
                    IconMapper.walletTypeLiteral(null) shouldBe Icons.DEFAULT
                    IconMapper.isKnownWalletTypeIcon("custom.png") shouldBe false
                }
            }
        }
    })
