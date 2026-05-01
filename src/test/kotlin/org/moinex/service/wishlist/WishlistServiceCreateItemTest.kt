package org.moinex.service.wishlist

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.common.ClockProvider
import org.moinex.factory.wishlist.WishlistItemFactory
import org.moinex.factory.wishlist.WishlistItemLinkFactory
import org.moinex.repository.wishlist.WishlistItemLinkRepository
import org.moinex.repository.wishlist.WishlistItemRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService

class WishlistServiceCreateItemTest :
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

        Given("a valid wishlist item without links") {
            When("creating the item") {
                val item = WishlistItemFactory.create(id = null, title = "New Laptop")
                val savedItem = WishlistItemFactory.create(id = 1, title = "New Laptop")

                every { wishlistItemRepository.save(item) } returns savedItem

                val result = service.createItem(item, emptyList())

                Then("should return the created item id") {
                    result shouldBe 1
                }

                Then("should call repository save method") {
                    verify { wishlistItemRepository.save(item) }
                }

                Then("should not save any links") {
                    verify(exactly = 0) { wishlistItemLinkRepository.save(any()) }
                }
            }
        }

        Given("a valid wishlist item with links") {
            When("creating the item with multiple links") {
                val item = WishlistItemFactory.create(id = null, title = "Gaming Console")
                val savedItem = WishlistItemFactory.create(id = 2, title = "Gaming Console")

                val link1 = WishlistItemLinkFactory.create(id = null, url = "https://store1.com", label = "Store 1")
                val link2 = WishlistItemLinkFactory.create(id = null, url = "https://store2.com", label = "Store 2")
                val links = listOf(link1, link2)

                every { wishlistItemRepository.save(item) } returns savedItem
                every { wishlistItemLinkRepository.save(any()) } returnsArgument 0

                val result = service.createItem(item, links)

                Then("should return the created item id") {
                    result shouldBe 2
                }

                Then("should call repository save method") {
                    verify { wishlistItemRepository.save(item) }
                }

                Then("should save all links") {
                    verify(exactly = 2) { wishlistItemLinkRepository.save(any()) }
                }

                Then("should associate links with the saved item") {
                    links.forEach { link ->
                        link.wishlistItem shouldBe savedItem
                    }
                }
            }
        }

        Given("a wishlist item with single link") {
            When("creating the item") {
                val item = WishlistItemFactory.create(id = null, title = "Headphones")
                val savedItem = WishlistItemFactory.create(id = 3, title = "Headphones")

                val link = WishlistItemLinkFactory.create(id = null, url = "https://audio-store.com")
                val links = listOf(link)

                every { wishlistItemRepository.save(item) } returns savedItem
                every { wishlistItemLinkRepository.save(any()) } returnsArgument 0

                val result = service.createItem(item, links)

                Then("should return the created item id") {
                    result shouldBe 3
                }

                Then("should save exactly one link") {
                    verify(exactly = 1) { wishlistItemLinkRepository.save(any()) }
                }

                Then("should associate link with the saved item") {
                    link.wishlistItem shouldBe savedItem
                }
            }
        }
    })
