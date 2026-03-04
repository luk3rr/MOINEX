/*
 * Filename: CategoryServiceTest.kt
 * Created on: March 3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.CategoryFactory
import org.moinex.repository.CategoryRepository
import java.util.Optional

class CategoryServiceTest :
    BehaviorSpec({
        val categoryRepository = mockk<CategoryRepository>()
        val creditCardService = mockk<CreditCardService>()
        val walletService = mockk<WalletService>()

        val service = CategoryService(categoryRepository, creditCardService, walletService)

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid category name") {
            And("the category name does not already exist") {
                When("creating a new category") {
                    val category = CategoryFactory.create(id = null, name = "Food")
                    every { categoryRepository.existsByName("Food") } returns false
                    every { categoryRepository.save(any()) } returns
                        CategoryFactory.create(id = 1, name = "Food")

                    val result = service.createCategory(category)

                    Then("should return the created category id") {
                        result shouldBe 1
                    }

                    Then("should call repository save method") {
                        verify { categoryRepository.save(any()) }
                    }
                }
            }

            And("the category name already exists") {
                When("creating a new category") {
                    val category = CategoryFactory.create(id = null, name = "Transport")
                    every { categoryRepository.existsByName("Transport") } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createCategory(category)
                        }
                    }
                }
            }
        }

        Given("an existing category with no associated transactions") {
            And("the category exists in the database") {
                When("deleting the category") {
                    val category = CategoryFactory.create(id = 1, name = "Entertainment")
                    every { categoryRepository.findById(1) } returns Optional.of(category)
                    every { walletService.getWalletTransactionAndTransferCountByCategory(1) } returns 0
                    every { creditCardService.getDebtCountByCategory(1) } returns 0
                    every { categoryRepository.delete(category) } returns Unit

                    service.deleteCategory(1)

                    Then("should call repository delete method") {
                        verify { categoryRepository.delete(category) }
                    }
                }
            }

            And("the category does not exist") {
                When("deleting the category") {
                    every { categoryRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.deleteCategory(999)
                        }
                    }
                }
            }
        }

        Given("an existing category with associated transactions") {
            And("the category has wallet transactions") {
                When("deleting the category") {
                    val category = CategoryFactory.create(id = 1, name = "Utilities")
                    every { categoryRepository.findById(1) } returns Optional.of(category)
                    every { walletService.getWalletTransactionAndTransferCountByCategory(1) } returns 5
                    every { creditCardService.getDebtCountByCategory(1) } returns 0

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.deleteCategory(1)
                        }
                    }
                }
            }

            And("the category has credit card debts") {
                When("deleting the category") {
                    val category = CategoryFactory.create(id = 2, name = "Shopping")
                    every { categoryRepository.findById(2) } returns Optional.of(category)
                    every { walletService.getWalletTransactionAndTransferCountByCategory(2) } returns 0
                    every { creditCardService.getDebtCountByCategory(2) } returns 3

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.deleteCategory(2)
                        }
                    }
                }
            }

            And("the category has both wallet and credit card transactions") {
                When("deleting the category") {
                    val category = CategoryFactory.create(id = 3, name = "Travel")
                    every { categoryRepository.findById(3) } returns Optional.of(category)
                    every { walletService.getWalletTransactionAndTransferCountByCategory(3) } returns 2
                    every { creditCardService.getDebtCountByCategory(3) } returns 1

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.deleteCategory(3)
                        }
                    }
                }
            }
        }

        Given("an existing category with a new name") {
            And("the new name does not already exist") {
                When("renaming the category") {
                    val existingCategory = CategoryFactory.create(id = 1, name = "Old Name")
                    val updatedCategory = CategoryFactory.create(id = 1, name = "New Name")
                    every { categoryRepository.findById(1) } returns Optional.of(existingCategory)
                    every { categoryRepository.existsByName("New Name") } returns false

                    service.renameCategory(updatedCategory)

                    Then("should update the category name") {
                        existingCategory.name shouldBe "New Name"
                    }
                }
            }

            And("the new name is the same as the current name") {
                When("renaming the category") {
                    val category = CategoryFactory.create(id = 1, name = "Same Name")
                    every { categoryRepository.findById(1) } returns Optional.of(category)

                    service.renameCategory(category)

                    Then("should not update the category name") {
                        category.name shouldBe "Same Name"
                    }
                }
            }

            And("the new name already exists") {
                When("renaming the category") {
                    val category = CategoryFactory.create(id = 1, name = "Old Name")
                    val updatedCategory = CategoryFactory.create(id = 1, name = "Existing Name")
                    every { categoryRepository.findById(1) } returns Optional.of(category)
                    every { categoryRepository.existsByName("Existing Name") } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.renameCategory(updatedCategory)
                        }
                    }
                }
            }

            And("the category does not exist") {
                When("renaming the category") {
                    val updatedCategory = CategoryFactory.create(id = 999, name = "New Name")
                    every { categoryRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.renameCategory(updatedCategory)
                        }
                    }
                }
            }
        }

        Given("an existing non-archived category") {
            And("the category exists in the database") {
                When("archiving the category") {
                    val category = CategoryFactory.create(id = 1, name = "Active Category", isArchived = false)
                    every { categoryRepository.findById(1) } returns Optional.of(category)

                    service.archiveCategory(1)

                    Then("should set isArchived to true") {
                        category.isArchived shouldBe true
                    }
                }
            }

            And("the category is already archived") {
                When("archiving the category") {
                    val category = CategoryFactory.create(id = 1, name = "Already Archived", isArchived = true)
                    every { categoryRepository.findById(1) } returns Optional.of(category)

                    service.archiveCategory(1)

                    Then("should not update the category") {
                        category.isArchived shouldBe true
                    }
                }
            }

            And("the category does not exist") {
                When("archiving the category") {
                    every { categoryRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.archiveCategory(999)
                        }
                    }
                }
            }
        }

        Given("an existing archived category") {
            And("the category exists in the database") {
                When("unarchiving the category") {
                    val category = CategoryFactory.create(id = 1, name = "Archived Category", isArchived = true)
                    every { categoryRepository.findById(1) } returns Optional.of(category)
                    every { categoryRepository.save(any()) } returns category

                    service.unarchiveCategory(1)

                    Then("should set isArchived to false") {
                        category.isArchived shouldBe false
                    }
                }
            }

            And("the category is already unarchived") {
                When("unarchiving the category") {
                    val category = CategoryFactory.create(id = 1, name = "Already Unarchived", isArchived = false)
                    every { categoryRepository.findById(1) } returns Optional.of(category)

                    service.unarchiveCategory(1)

                    Then("should not update the category") {
                        category.isArchived shouldBe false
                    }
                }
            }

            And("the category does not exist") {
                When("unarchiving the category") {
                    every { categoryRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.unarchiveCategory(999)
                        }
                    }
                }
            }
        }

        Given("categories in the database") {
            When("getting all categories") {
                val categories =
                    listOf(
                        CategoryFactory.create(id = 1, name = "Food"),
                        CategoryFactory.create(id = 2, name = "Transport"),
                        CategoryFactory.create(id = 3, name = "Entertainment"),
                    )
                every { categoryRepository.findAll() } returns categories

                val result = service.getCategories()

                Then("should return all categories") {
                    result.size shouldBe 3
                }

                Then("should call repository findAll method") {
                    verify { categoryRepository.findAll() }
                }
            }
        }

        Given("non-archived categories in the database") {
            When("getting non-archived categories ordered by name") {
                val categories =
                    listOf(
                        CategoryFactory.create(id = 1, name = "Entertainment", isArchived = false),
                        CategoryFactory.create(id = 2, name = "Food", isArchived = false),
                        CategoryFactory.create(id = 3, name = "Transport", isArchived = false),
                    )
                every { categoryRepository.findAllByIsArchivedFalseOrderByNameAsc() } returns categories

                val result = service.getNonArchivedCategoriesOrderedByName()

                Then("should return non-archived categories ordered by name") {
                    result.size shouldBe 3
                    result[0].name shouldBe "Entertainment"
                    result[1].name shouldBe "Food"
                    result[2].name shouldBe "Transport"
                }

                Then("should call repository findAllByIsArchivedFalseOrderByNameAsc method") {
                    verify { categoryRepository.findAllByIsArchivedFalseOrderByNameAsc() }
                }
            }
        }

        Given("a category with transactions") {
            And("the category has wallet transactions only") {
                When("getting transaction count") {
                    every { walletService.getWalletTransactionAndTransferCountByCategory(1) } returns 5
                    every { creditCardService.getDebtCountByCategory(1) } returns 0

                    val result = service.getTransactionCountByCategory(1)

                    Then("should return the count of wallet transactions") {
                        result shouldBe 5
                    }
                }
            }

            And("the category has credit card debts only") {
                When("getting transaction count") {
                    every { walletService.getWalletTransactionAndTransferCountByCategory(2) } returns 0
                    every { creditCardService.getDebtCountByCategory(2) } returns 3

                    val result = service.getTransactionCountByCategory(2)

                    Then("should return the count of credit card debts") {
                        result shouldBe 3
                    }
                }
            }

            And("the category has both wallet transactions and credit card debts") {
                When("getting transaction count") {
                    every { walletService.getWalletTransactionAndTransferCountByCategory(3) } returns 4
                    every { creditCardService.getDebtCountByCategory(3) } returns 2

                    val result = service.getTransactionCountByCategory(3)

                    Then("should return the total count of both types") {
                        result shouldBe 6
                    }
                }
            }

            And("the category has no transactions") {
                When("getting transaction count") {
                    every { walletService.getWalletTransactionAndTransferCountByCategory(4) } returns 0
                    every { creditCardService.getDebtCountByCategory(4) } returns 0

                    val result = service.getTransactionCountByCategory(4)

                    Then("should return zero") {
                        result shouldBe 0
                    }
                }
            }
        }
    })
