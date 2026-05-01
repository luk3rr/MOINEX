package org.moinex.service.wishlist

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.common.ClockProvider
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.wishlist.WishlistItemFactory
import org.moinex.model.enums.WishlistItemStatus
import org.moinex.repository.wishlist.WishlistItemLinkRepository
import org.moinex.repository.wishlist.WishlistItemRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import java.time.LocalDateTime
import java.util.Optional

class WishlistServiceMarkAsPurchasedWithCreditCardTest :
    BehaviorSpec({
        val wishlistItemRepository = mockk<WishlistItemRepository>()
        val wishlistItemLinkRepository = mockk<WishlistItemLinkRepository>()
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val notificationService = mockk<NotificationService>(relaxed = true)
        val preferencesService = mockk<PreferencesService>(relaxed = true)
        val clockProvider = mockk<ClockProvider>()

        val service =
            WishlistService(
                wishlistItemRepository,
                wishlistItemLinkRepository,
                walletService,
                creditCardService,
                notificationService,
                preferencesService,
                clockProvider,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a pending wishlist item") {
            When("marking as purchased with credit card") {
                val item = WishlistItemFactory.create(id = 1, status = WishlistItemStatus.PENDING)
                val debt = CreditCardDebtFactory.create(id = 100)
                val now = LocalDateTime.of(2026, 3, 29, 10, 0)

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)
                every { creditCardService.getCreditCardDebtById(100) } returns debt
                every { clockProvider.now() } returns now

                val result = service.markAsPurchasedWithCreditCard(1, 100)

                Then("should update status to PURCHASED") {
                    result.status shouldBe WishlistItemStatus.PURCHASED
                }

                Then("should set purchased date") {
                    result.purchasedAt shouldBe now
                }

                Then("should associate credit card debt") {
                    result.creditCardDebt shouldBe debt
                }

                Then("should clear wallet transaction") {
                    result.walletTransaction shouldBe null
                }

                Then("should call creditCardService to get debt") {
                    verify { creditCardService.getCreditCardDebtById(100) }
                }
            }
        }

        Given("an already purchased wishlist item") {
            When("trying to mark as purchased with credit card again") {
                val debt = CreditCardDebtFactory.create(id = 100)
                val item =
                    WishlistItemFactory.create(
                        id = 1,
                        status = WishlistItemStatus.PURCHASED,
                        creditCardDebt = debt,
                    )

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.markAsPurchasedWithCreditCard(1, 200)
                    }.message shouldBe "Wishlist item is already marked as purchased"
                }

                Then("should not call creditCardService") {
                    verify(exactly = 0) { creditCardService.getCreditCardDebtById(any()) }
                }
            }
        }

        Given("a non-existent wishlist item") {
            When("trying to mark as purchased with credit card") {
                every { wishlistItemRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.markAsPurchasedWithCreditCard(999, 100)
                    }
                }

                Then("should not call creditCardService") {
                    verify(exactly = 0) { creditCardService.getCreditCardDebtById(any()) }
                }
            }
        }

        Given("multiple pending items") {
            When("marking different items as purchased with credit card") {
                val item1 = WishlistItemFactory.create(id = 1, status = WishlistItemStatus.PENDING)
                val item2 = WishlistItemFactory.create(id = 2, status = WishlistItemStatus.PENDING)
                val debt1 = CreditCardDebtFactory.create(id = 100)
                val debt2 = CreditCardDebtFactory.create(id = 200)
                val now1 = LocalDateTime.of(2026, 3, 29, 10, 0)
                val now2 = LocalDateTime.of(2026, 3, 29, 15, 0)

                every { wishlistItemRepository.findById(1) } returns Optional.of(item1)
                every { wishlistItemRepository.findById(2) } returns Optional.of(item2)
                every { creditCardService.getCreditCardDebtById(100) } returns debt1
                every { creditCardService.getCreditCardDebtById(200) } returns debt2
                every { clockProvider.now() } returnsMany listOf(now1, now2)

                val result1 = service.markAsPurchasedWithCreditCard(1, 100)
                val result2 = service.markAsPurchasedWithCreditCard(2, 200)

                Then("should mark both items correctly") {
                    result1.status shouldBe WishlistItemStatus.PURCHASED
                    result2.status shouldBe WishlistItemStatus.PURCHASED
                }

                Then("should associate correct debts") {
                    result1.creditCardDebt shouldBe debt1
                    result2.creditCardDebt shouldBe debt2
                }

                Then("should set different timestamps") {
                    result1.purchasedAt shouldBe now1
                    result2.purchasedAt shouldBe now2
                }

                Then("should clear wallet transactions") {
                    result1.walletTransaction shouldBe null
                    result2.walletTransaction shouldBe null
                }
            }
        }
    })
