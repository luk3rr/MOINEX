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
import org.moinex.factory.CategoryFactory
import org.moinex.factory.wishlist.WishlistItemFactory
import org.moinex.factory.wishlist.WishlistItemLinkFactory
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.repository.wishlist.WishlistItemLinkRepository
import org.moinex.repository.wishlist.WishlistItemRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class WishlistServiceUpdateItemTest :
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

        Given("an existing wishlist item") {
            And("updating basic properties without changing links") {
                When("updating the item") {
                    val existingItem = WishlistItemFactory.create(id = 1, title = "Old Title")
                    val updatedItem =
                        WishlistItemFactory.create(
                            id = 1,
                            title = "New Title",
                            estimatedPrice = BigDecimal("200.00"),
                            targetDate = LocalDate.of(2026, 12, 31),
                            priority = WishlistItemPriority.HIGH,
                            notes = "Updated notes",
                        )

                    every { wishlistItemRepository.findById(1) } returns Optional.of(existingItem)
                    every { wishlistItemRepository.save(existingItem) } returns existingItem
                    every { wishlistItemLinkRepository.findAllByWishlistItemId(1) } returns emptyList()

                    service.updateItem(updatedItem, emptyList())

                    Then("should update all properties") {
                        existingItem.title shouldBe "New Title"
                        existingItem.estimatedPrice shouldBe BigDecimal("200.00")
                        existingItem.targetDate shouldBe LocalDate.of(2026, 12, 31)
                        existingItem.priority shouldBe WishlistItemPriority.HIGH
                        existingItem.notes shouldBe "Updated notes"
                    }

                    Then("should call repository save method") {
                        verify { wishlistItemRepository.save(existingItem) }
                    }
                }
            }

            And("updating with category change") {
                When("changing the category") {
                    val oldCategory = CategoryFactory.create(id = 1, name = "Electronics")
                    val newCategory = CategoryFactory.create(id = 2, name = "Furniture")
                    val existingItem = WishlistItemFactory.create(id = 1, category = oldCategory)
                    val updatedItem = WishlistItemFactory.create(id = 1, category = newCategory)

                    every { wishlistItemRepository.findById(1) } returns Optional.of(existingItem)
                    every { wishlistItemRepository.save(existingItem) } returns existingItem
                    every { wishlistItemLinkRepository.findAllByWishlistItemId(1) } returns emptyList()

                    service.updateItem(updatedItem, emptyList())

                    Then("should update the category") {
                        existingItem.category shouldBe newCategory
                    }
                }
            }

            And("adding new links") {
                When("updating with new links") {
                    val existingItem = WishlistItemFactory.create(id = 1)
                    val newLink1 = WishlistItemLinkFactory.create(id = null, url = "https://new1.com")
                    val newLink2 = WishlistItemLinkFactory.create(id = null, url = "https://new2.com")

                    every { wishlistItemRepository.findById(1) } returns Optional.of(existingItem)
                    every { wishlistItemRepository.save(existingItem) } returns existingItem
                    every { wishlistItemLinkRepository.findAllByWishlistItemId(1) } returns emptyList()
                    every { wishlistItemLinkRepository.save(any()) } returnsArgument 0

                    service.updateItem(existingItem, listOf(newLink1, newLink2))

                    Then("should save all new links") {
                        verify(exactly = 2) { wishlistItemLinkRepository.save(any()) }
                    }

                    Then("should associate links with the item") {
                        newLink1.wishlistItem shouldBe existingItem
                        newLink2.wishlistItem shouldBe existingItem
                    }
                }
            }

            And("updating existing links") {
                When("modifying link properties") {
                    val existingItem = WishlistItemFactory.create(id = 1)
                    val existingLink =
                        WishlistItemLinkFactory.create(
                            id = 10,
                            url = "https://old.com",
                            label = "Old Label",
                        )
                    val updatedLink =
                        WishlistItemLinkFactory.create(
                            id = 10,
                            url = "https://new.com",
                            label = "New Label",
                        )

                    every { wishlistItemRepository.findById(1) } returns Optional.of(existingItem)
                    every { wishlistItemRepository.save(existingItem) } returns existingItem
                    every { wishlistItemLinkRepository.findAllByWishlistItemId(1) } returns listOf(existingLink)
                    every { wishlistItemLinkRepository.save(existingLink) } returns existingLink

                    service.updateItem(existingItem, listOf(updatedLink))

                    Then("should update link properties") {
                        existingLink.url shouldBe "https://new.com"
                        existingLink.label shouldBe "New Label"
                    }

                    Then("should save the updated link") {
                        verify { wishlistItemLinkRepository.save(existingLink) }
                    }
                }
            }

            And("removing links") {
                When("updating with fewer links") {
                    val existingItem = WishlistItemFactory.create(id = 1)
                    val link1 = WishlistItemLinkFactory.create(id = 10, url = "https://keep.com")
                    val link2 = WishlistItemLinkFactory.create(id = 11, url = "https://remove.com")

                    every { wishlistItemRepository.findById(1) } returns Optional.of(existingItem)
                    every { wishlistItemRepository.save(existingItem) } returns existingItem
                    every { wishlistItemLinkRepository.findAllByWishlistItemId(1) } returns listOf(link1, link2)
                    every { wishlistItemLinkRepository.delete(link2) } just runs
                    every { wishlistItemLinkRepository.save(link1) } returns link1

                    service.updateItem(existingItem, listOf(link1))

                    Then("should delete removed links") {
                        verify { wishlistItemLinkRepository.delete(link2) }
                    }

                    Then("should keep existing links") {
                        verify { wishlistItemLinkRepository.save(link1) }
                    }
                }
            }

            And("replacing all links") {
                When("removing old links and adding new ones") {
                    val existingItem = WishlistItemFactory.create(id = 1)
                    val oldLink = WishlistItemLinkFactory.create(id = 10, url = "https://old.com")
                    val newLink = WishlistItemLinkFactory.create(id = null, url = "https://new.com")

                    every { wishlistItemRepository.findById(1) } returns Optional.of(existingItem)
                    every { wishlistItemRepository.save(existingItem) } returns existingItem
                    every { wishlistItemLinkRepository.findAllByWishlistItemId(1) } returns listOf(oldLink)
                    every { wishlistItemLinkRepository.delete(oldLink) } just runs
                    every { wishlistItemLinkRepository.save(newLink) } returns newLink

                    service.updateItem(existingItem, listOf(newLink))

                    Then("should delete old links") {
                        verify { wishlistItemLinkRepository.delete(oldLink) }
                    }

                    Then("should save new links") {
                        verify { wishlistItemLinkRepository.save(newLink) }
                    }
                }
            }
        }

        Given("a non-existent wishlist item") {
            When("trying to update") {
                val item = WishlistItemFactory.create(id = 999)

                every { wishlistItemRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.updateItem(item, emptyList())
                    }
                }

                Then("should not save anything") {
                    verify(exactly = 0) { wishlistItemRepository.save(any()) }
                    verify(exactly = 0) { wishlistItemLinkRepository.save(any()) }
                }
            }
        }
    })
