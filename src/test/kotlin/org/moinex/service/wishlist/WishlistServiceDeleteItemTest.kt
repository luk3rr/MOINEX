package org.moinex.service.wishlist

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
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
import org.moinex.repository.wishlist.WishlistItemLinkRepository
import org.moinex.repository.wishlist.WishlistItemRepository
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import java.util.Optional

class WishlistServiceDeleteItemTest :
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

        Given("an existing wishlist item without wallet transaction") {
            When("deleting the item") {
                val item = WishlistItemFactory.create(id = 1, walletTransaction = null)

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)
                every { wishlistItemRepository.delete(item) } just runs

                service.deleteItem(1)

                Then("should not delete any wallet transaction") {
                    verify(exactly = 0) { walletService.deleteWalletTransaction(any()) }
                }

                Then("should delete the item") {
                    verify { wishlistItemRepository.delete(item) }
                }
            }
        }

        Given("an existing wishlist item with wallet transaction") {
            When("deleting the item") {
                val transaction = WalletTransactionFactory.create(id = 100)
                val item = WishlistItemFactory.create(id = 1, walletTransaction = transaction)

                every { wishlistItemRepository.findById(1) } returns Optional.of(item)
                every { walletService.deleteWalletTransaction(100) } just runs
                every { wishlistItemRepository.delete(item) } just runs

                service.deleteItem(1)

                Then("should delete the associated wallet transaction") {
                    verify { walletService.deleteWalletTransaction(100) }
                }

                Then("should delete the item") {
                    verify { wishlistItemRepository.delete(item) }
                }
            }
        }

        Given("a non-existent wishlist item") {
            When("trying to delete") {
                every { wishlistItemRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.deleteItem(999)
                    }
                }

                Then("should not delete anything") {
                    verify(exactly = 0) { wishlistItemRepository.delete(any()) }
                    verify(exactly = 0) { walletService.deleteWalletTransaction(any()) }
                }
            }
        }

        Given("a purchased wishlist item with wallet transaction") {
            When("deleting the item") {
                val transaction = WalletTransactionFactory.create(id = 200)
                val item =
                    WishlistItemFactory.create(
                        id = 2,
                        walletTransaction = transaction,
                    )

                every { wishlistItemRepository.findById(2) } returns Optional.of(item)
                every { walletService.deleteWalletTransaction(200) } just runs
                every { wishlistItemRepository.delete(item) } just runs

                service.deleteItem(2)

                Then("should delete the wallet transaction first") {
                    verify { walletService.deleteWalletTransaction(200) }
                }

                Then("should delete the item") {
                    verify { wishlistItemRepository.delete(item) }
                }
            }
        }

        Given("a purchased wishlist item with credit card debt") {
            When("deleting the item") {
                val debt = CreditCardDebtFactory.create(id = 300)
                val item =
                    WishlistItemFactory.create(
                        id = 3,
                        creditCardDebt = debt,
                    )

                every { wishlistItemRepository.findById(3) } returns Optional.of(item)
                every { creditCardService.deleteDebt(300) } just runs
                every { wishlistItemRepository.delete(item) } just runs

                service.deleteItem(3)

                Then("should delete the credit card debt first") {
                    verify { creditCardService.deleteDebt(300) }
                }

                Then("should delete the item") {
                    verify { wishlistItemRepository.delete(item) }
                }
            }
        }
    })
