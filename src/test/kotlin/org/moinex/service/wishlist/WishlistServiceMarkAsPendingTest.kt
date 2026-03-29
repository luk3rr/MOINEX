package org.moinex.service.wishlist

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.common.ClockProvider
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.factory.wishlist.WishlistItemFactory
import org.moinex.model.enums.WishlistItemStatus
import org.moinex.repository.wishlist.WishlistItemLinkRepository
import org.moinex.repository.wishlist.WishlistItemRepository
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import java.time.LocalDateTime
import java.util.Optional

class WishlistServiceMarkAsPendingTest :
    BehaviorSpec({
        val wishlistItemRepository = mockk<WishlistItemRepository>()
        val wishlistItemLinkRepository = mockk<WishlistItemLinkRepository>()
        val walletService = mockk<WalletService>()
        val creditCardService = mockk<CreditCardService>()
        val clockProvider = mockk<ClockProvider>()

        val service =
            WishlistService(
                wishlistItemRepository,
                wishlistItemLinkRepository,
                walletService,
                creditCardService,
                clockProvider,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a purchased wishlist item") {
            When("marking as pending") {
                val transaction = WalletTransactionFactory.create(id = 100)
                val item =
                    WishlistItemFactory.create(
                        id = 1,
                        status = WishlistItemStatus.PURCHASED,
                        purchasedAt = LocalDateTime.of(2026, 3, 29, 10, 0),
                        walletTransaction = transaction,
                    )

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)
                every { walletService.deleteWalletTransaction(100) } just runs

                service.markAsPending(1)

                Then("should update status to PENDING") {
                    item.status shouldBe WishlistItemStatus.PENDING
                }

                Then("should clear purchased date") {
                    item.purchasedAt shouldBe null
                }

                Then("should clear wallet transaction") {
                    item.walletTransaction shouldBe null
                }

                Then("should delete the wallet transaction") {
                    verify { walletService.deleteWalletTransaction(100) }
                }
            }
        }

        Given("an already pending wishlist item") {
            When("trying to mark as pending again") {
                val item =
                    WishlistItemFactory.create(
                        id = 1,
                        status = WishlistItemStatus.PENDING,
                        walletTransaction = null,
                    )

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.markAsPending(1)
                    }.message shouldBe "Wishlist item is already marked as pending"
                }

                Then("should not delete any wallet transaction") {
                    verify(exactly = 0) { walletService.deleteWalletTransaction(any()) }
                }
            }
        }

        Given("a non-existent wishlist item") {
            When("trying to mark as pending") {
                every { wishlistItemRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.markAsPending(999)
                    }
                }

                Then("should not delete any wallet transaction") {
                    verify(exactly = 0) { walletService.deleteWalletTransaction(any()) }
                }
            }
        }

        Given("a purchased item without wallet transaction") {
            When("marking as pending") {
                val item =
                    WishlistItemFactory.create(
                        id = 1,
                        status = WishlistItemStatus.PURCHASED,
                        purchasedAt = LocalDateTime.of(2026, 3, 29, 10, 0),
                        walletTransaction = null,
                    )

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)

                service.markAsPending(1)

                Then("should update status to PENDING") {
                    item.status shouldBe WishlistItemStatus.PENDING
                }

                Then("should clear purchased date") {
                    item.purchasedAt shouldBe null
                }

                Then("should not try to delete wallet transaction") {
                    verify(exactly = 0) { walletService.deleteWalletTransaction(any()) }
                }
            }
        }

        Given("multiple purchased items") {
            When("marking different items as pending") {
                val transaction1 = WalletTransactionFactory.create(id = 100)
                val transaction2 = WalletTransactionFactory.create(id = 200)
                val item1 =
                    WishlistItemFactory.create(
                        id = 1,
                        status = WishlistItemStatus.PURCHASED,
                        walletTransaction = transaction1,
                    )
                val item2 =
                    WishlistItemFactory.create(
                        id = 2,
                        status = WishlistItemStatus.PURCHASED,
                        walletTransaction = transaction2,
                    )

                every { wishlistItemRepository.findById(1) } returns Optional.of(item1)
                every { wishlistItemRepository.findById(2) } returns Optional.of(item2)
                every { walletService.deleteWalletTransaction(100) } just runs
                every { walletService.deleteWalletTransaction(200) } just runs

                service.markAsPending(1)
                service.markAsPending(2)

                Then("should mark both items as pending") {
                    item1.status shouldBe WishlistItemStatus.PENDING
                    item2.status shouldBe WishlistItemStatus.PENDING
                }

                Then("should delete both wallet transactions") {
                    verify { walletService.deleteWalletTransaction(100) }
                    verify { walletService.deleteWalletTransaction(200) }
                }

                Then("should clear all wallet transaction references") {
                    item1.walletTransaction shouldBe null
                    item2.walletTransaction shouldBe null
                }
            }
        }

        Given("a purchased item that was recently marked with wallet") {
            When("reverting the purchase") {
                val transaction = WalletTransactionFactory.create(id = 100)
                val purchaseTime = LocalDateTime.of(2026, 3, 29, 14, 0)
                val item =
                    WishlistItemFactory.create(
                        id = 1,
                        status = WishlistItemStatus.PURCHASED,
                        purchasedAt = purchaseTime,
                        walletTransaction = transaction,
                    )

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)
                every { walletService.deleteWalletTransaction(100) } just runs

                service.markAsPending(1)

                Then("should revert all purchase-related fields") {
                    item.status shouldBe WishlistItemStatus.PENDING
                    item.purchasedAt shouldBe null
                    item.walletTransaction shouldBe null
                }

                Then("should refund by deleting the transaction") {
                    verify { walletService.deleteWalletTransaction(100) }
                }

                Then("should not call delete debt") {
                    verify(exactly = 0) { creditCardService.deleteDebt(any()) }
                }
            }
        }

        Given("a purchased item with credit card debt") {
            When("reverting the purchase") {
                val debt = CreditCardDebtFactory.create(id = 200)
                val purchaseTime = LocalDateTime.of(2026, 3, 29, 14, 0)
                val item =
                    WishlistItemFactory.create(
                        id = 2,
                        status = WishlistItemStatus.PURCHASED,
                        purchasedAt = purchaseTime,
                        creditCardDebt = debt,
                    )

                every { wishlistItemRepository.findById(2) } returns Optional.of(item)
                every { creditCardService.deleteDebt(200) } just runs

                service.markAsPending(2)

                Then("should revert all purchase-related fields") {
                    item.status shouldBe WishlistItemStatus.PENDING
                    item.purchasedAt shouldBe null
                    item.creditCardDebt shouldBe null
                }

                Then("should delete the credit card debt") {
                    verify { creditCardService.deleteDebt(200) }
                }

                Then("should not call delete wallet transaction") {
                    verify(exactly = 0) { walletService.deleteWalletTransaction(any()) }
                }
            }
        }
    })
