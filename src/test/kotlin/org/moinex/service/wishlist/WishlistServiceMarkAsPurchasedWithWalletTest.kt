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
import org.moinex.factory.wallet.WalletTransactionFactory
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

class WishlistServiceMarkAsPurchasedWithWalletTest :
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
            When("marking as purchased") {
                val item = WishlistItemFactory.create(id = 1, status = WishlistItemStatus.PENDING)
                val transaction = WalletTransactionFactory.create(id = 100)
                val now = LocalDateTime.of(2026, 3, 29, 10, 0)

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)
                every { walletService.getWalletTransactionById(100) } returns transaction
                every { clockProvider.now() } returns now

                val result = service.markAsPurchasedWithWallet(1, 100)

                Then("should update status to PURCHASED") {
                    result.status shouldBe WishlistItemStatus.PURCHASED
                }

                Then("should set purchased date") {
                    result.purchasedAt shouldBe now
                }

                Then("should associate wallet transaction") {
                    result.walletTransaction shouldBe transaction
                }

                Then("should call walletService to get transaction") {
                    verify { walletService.getWalletTransactionById(100) }
                }
            }
        }

        Given("an already purchased wishlist item") {
            When("trying to mark as purchased again") {
                val transaction = WalletTransactionFactory.create(id = 100)
                val item =
                    WishlistItemFactory.create(
                        id = 1,
                        status = WishlistItemStatus.PURCHASED,
                        walletTransaction = transaction,
                    )

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.markAsPurchasedWithWallet(1, 200)
                    }.message shouldBe "Wishlist item is already marked as purchased"
                }

                Then("should not call walletService") {
                    verify(exactly = 0) { walletService.getWalletTransactionById(any()) }
                }
            }
        }

        Given("a non-existent wishlist item") {
            When("trying to mark as purchased") {
                every { wishlistItemRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.markAsPurchasedWithWallet(999, 100)
                    }
                }

                Then("should not call walletService") {
                    verify(exactly = 0) { walletService.getWalletTransactionById(any()) }
                }
            }
        }

        Given("a pending item with different wallet transactions") {
            When("marking as purchased with transaction A") {
                val item = WishlistItemFactory.create(id = 1, status = WishlistItemStatus.PENDING)
                val transactionA = WalletTransactionFactory.create(id = 100)
                val now = LocalDateTime.of(2026, 3, 29, 14, 30)

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)
                every { walletService.getWalletTransactionById(100) } returns transactionA
                every { clockProvider.now() } returns now

                val result = service.markAsPurchasedWithWallet(1, 100)

                Then("should associate with transaction A") {
                    result.walletTransaction shouldBe transactionA
                }

                Then("should set correct purchase timestamp") {
                    result.purchasedAt shouldBe now
                }
            }
        }

        Given("multiple pending items") {
            When("marking different items as purchased") {
                val item1 = WishlistItemFactory.create(id = 1, status = WishlistItemStatus.PENDING)
                val item2 = WishlistItemFactory.create(id = 2, status = WishlistItemStatus.PENDING)
                val transaction1 = WalletTransactionFactory.create(id = 100)
                val transaction2 = WalletTransactionFactory.create(id = 200)
                val now1 = LocalDateTime.of(2026, 3, 29, 10, 0)
                val now2 = LocalDateTime.of(2026, 3, 29, 15, 0)

                every { wishlistItemRepository.findById(1) } returns Optional.of(item1)
                every { wishlistItemRepository.findById(2) } returns Optional.of(item2)
                every { walletService.getWalletTransactionById(100) } returns transaction1
                every { walletService.getWalletTransactionById(200) } returns transaction2
                every { clockProvider.now() } returnsMany listOf(now1, now2)

                val result1 = service.markAsPurchasedWithWallet(1, 100)
                val result2 = service.markAsPurchasedWithWallet(2, 200)

                Then("should mark both items correctly") {
                    result1.status shouldBe WishlistItemStatus.PURCHASED
                    result2.status shouldBe WishlistItemStatus.PURCHASED
                }

                Then("should associate correct transactions") {
                    result1.walletTransaction shouldBe transaction1
                    result2.walletTransaction shouldBe transaction2
                }

                Then("should set different timestamps") {
                    result1.purchasedAt shouldBe now1
                    result2.purchasedAt shouldBe now2
                }
            }
        }
    })
