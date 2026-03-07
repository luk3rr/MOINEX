package org.moinex.service.goal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.GoalFactory
import org.moinex.factory.WalletFactory
import org.moinex.repository.goal.GoalRepository
import org.moinex.service.GoalService
import org.moinex.service.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class GoalServiceUpdateGoalTest :
    BehaviorSpec({
        val goalRepository = mockk<GoalRepository>()
        val walletService = mockk<WalletService>()

        val service = GoalService(goalRepository, walletService)

        afterContainer { clearAllMocks(answers = true) }

        Given("an existing goal") {
            And("updating basic information without name conflict") {
                When("updating name, target balance, target date and motivation") {
                    val existingGoal =
                        GoalFactory.create(
                            id = 1,
                            name = "Old Name",
                            targetBalance = BigDecimal("10000.00"),
                            targetDate = LocalDate.now().plusMonths(12),
                            motivation = "Old motivation",
                        )
                    val updatedGoal =
                        GoalFactory.create(
                            id = 1,
                            name = "New Name",
                            targetBalance = BigDecimal("15000.00"),
                            targetDate = LocalDate.now().plusMonths(18),
                            motivation = "New motivation",
                        )

                    every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                    every { goalRepository.existsByNameAndIdNot("New Name", 1) } returns false
                    every { walletService.existsByNameAndIdNot("New Name", 1) } returns false

                    service.updateGoal(updatedGoal)

                    Then("should update the goal name") {
                        existingGoal.name shouldBe "New Name"
                    }

                    Then("should update the target balance") {
                        existingGoal.targetBalance shouldBe BigDecimal("15000.00")
                    }

                    Then("should update the target date") {
                        existingGoal.targetDate shouldBe updatedGoal.targetDate
                    }

                    Then("should update the motivation") {
                        existingGoal.motivation shouldBe "New motivation"
                    }
                }
            }

            And("the new name already exists as another goal") {
                When("updating the goal") {
                    val existingGoal = GoalFactory.create(id = 1, name = "Goal 1")
                    val updatedGoal = GoalFactory.create(id = 1, name = "Existing Goal")

                    every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                    every { goalRepository.existsByNameAndIdNot("Existing Goal", 1) } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.updateGoal(updatedGoal)
                        }
                    }
                }
            }

            And("the new name already exists as a wallet") {
                When("updating the goal") {
                    val existingGoal = GoalFactory.create(id = 1, name = "Goal 1")
                    val updatedGoal = GoalFactory.create(id = 1, name = "Existing Wallet")

                    every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                    every { goalRepository.existsByNameAndIdNot("Existing Wallet", 1) } returns false
                    every { walletService.existsByNameAndIdNot("Existing Wallet", 1) } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.updateGoal(updatedGoal)
                        }
                    }
                }
            }

            And("the goal does not exist") {
                When("updating the goal") {
                    val updatedGoal = GoalFactory.create(id = 999, name = "Non-existent")

                    every { goalRepository.findById(999) } returns Optional.empty()

                    Then("should throw EntityNotFoundException") {
                        shouldThrow<EntityNotFoundException> {
                            service.updateGoal(updatedGoal)
                        }
                    }
                }
            }
        }

        Given("an existing non-archived goal") {
            When("archiving the goal") {
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal to Archive",
                        isArchived = false,
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal to Archive",
                        isArchived = true,
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Goal to Archive", 1) } returns false
                every { walletService.existsByNameAndIdNot("Goal to Archive", 1) } returns false
                every { walletService.removeAllVirtualWalletsFromMasterWallet(any()) } just runs

                service.updateGoal(updatedGoal)

                Then("should set isArchived to true") {
                    existingGoal.isArchived shouldBe true
                }
            }
        }

        Given("an existing archived goal") {
            When("unarchiving the goal") {
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal to Unarchive",
                        isArchived = true,
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal to Unarchive",
                        isArchived = false,
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Goal to Unarchive", 1) } returns false
                every { walletService.existsByNameAndIdNot("Goal to Unarchive", 1) } returns false

                service.updateGoal(updatedGoal)

                Then("should set isArchived to false") {
                    existingGoal.isArchived shouldBe false
                }
            }
        }

        Given("an existing goal with balance update") {
            When("increasing the balance") {
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Savings Goal",
                        initialBalance = BigDecimal("1000.00"),
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Savings Goal",
                        initialBalance = BigDecimal("2000.00"),
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Savings Goal", 1) } returns false
                every { walletService.existsByNameAndIdNot("Savings Goal", 1) } returns false

                service.updateGoal(updatedGoal)

                Then("should update the balance") {
                    existingGoal.balance shouldBe BigDecimal("2000.00")
                }

                Then("should update the initial balance") {
                    existingGoal.initialBalance shouldBe BigDecimal("2000.00")
                }
            }

            When("decreasing the balance") {
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Savings Goal",
                        initialBalance = BigDecimal("2000.00"),
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Savings Goal",
                        initialBalance = BigDecimal("1500.00"),
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Savings Goal", 1) } returns false
                every { walletService.existsByNameAndIdNot("Savings Goal", 1) } returns false

                service.updateGoal(updatedGoal)

                Then("should update the balance") {
                    existingGoal.balance shouldBe BigDecimal("1500.00")
                }

                Then("should update the initial balance") {
                    existingGoal.initialBalance shouldBe BigDecimal("1500.00")
                }
            }
        }

        Given("an incomplete goal") {
            When("completing the goal by updating completion date") {
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal to Complete",
                        initialBalance = BigDecimal("10000.00"),
                        targetBalance = BigDecimal("10000.00"),
                        completionDate = null,
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal to Complete",
                        initialBalance = BigDecimal("10000.00"),
                        targetBalance = BigDecimal("10000.00"),
                        completionDate = LocalDate.now(),
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Goal to Complete", 1) } returns false
                every { walletService.existsByNameAndIdNot("Goal to Complete", 1) } returns false

                service.updateGoal(updatedGoal)

                Then("should set completion date") {
                    existingGoal.completionDate shouldBe LocalDate.now()
                }

                Then("should remove master wallet") {
                    existingGoal.masterWallet shouldBe null
                }
            }

            When("trying to complete goal with insufficient balance") {
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal to Complete",
                        initialBalance = BigDecimal("5000.00"),
                        targetBalance = BigDecimal("10000.00"),
                        completionDate = null,
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal to Complete",
                        initialBalance = BigDecimal("5000.00"),
                        targetBalance = BigDecimal("10000.00"),
                        completionDate = LocalDate.now(),
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Goal to Complete", 1) } returns false
                every { walletService.existsByNameAndIdNot("Goal to Complete", 1) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateGoal(updatedGoal)
                    }
                }
            }
        }

        Given("a completed goal") {
            When("reopening the goal") {
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Completed Goal",
                        initialBalance = BigDecimal("10000.00"),
                        targetBalance = BigDecimal("10000.00"),
                        completionDate = LocalDate.now().minusDays(10),
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Completed Goal",
                        initialBalance = BigDecimal("10000.00"),
                        targetBalance = BigDecimal("15000.00"),
                        completionDate = null,
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Completed Goal", 1) } returns false
                every { walletService.existsByNameAndIdNot("Completed Goal", 1) } returns false

                service.updateGoal(updatedGoal)

                Then("should remove completion date") {
                    existingGoal.completionDate shouldBe null
                }
            }
        }

        Given("a goal with master wallet") {
            When("updating balance in the same master wallet") {
                val masterWallet =
                    WalletFactory.create(
                        id = 1,
                        name = "Main Wallet",
                        balance = BigDecimal("10000.00"),
                        masterWallet = null,
                    )
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Virtual Goal",
                        initialBalance = BigDecimal("2000.00"),
                        masterWallet = masterWallet,
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Virtual Goal",
                        initialBalance = BigDecimal("3000.00"),
                        masterWallet = masterWallet,
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Virtual Goal", 1) } returns false
                every { walletService.existsByNameAndIdNot("Virtual Goal", 1) } returns false
                every { walletService.getWalletById(1) } returns masterWallet

                service.updateGoal(updatedGoal)

                Then("should increase master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("11000.00")
                }
            }

            When("changing to a different master wallet") {
                val oldMasterWallet =
                    WalletFactory.create(
                        id = 1,
                        name = "Old Master",
                        balance = BigDecimal("10000.00"),
                        masterWallet = null,
                    )
                val newMasterWallet =
                    WalletFactory.create(
                        id = 2,
                        name = "New Master",
                        balance = BigDecimal("5000.00"),
                        masterWallet = null,
                    )
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Virtual Goal",
                        initialBalance = BigDecimal("2000.00"),
                        masterWallet = oldMasterWallet,
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Virtual Goal",
                        initialBalance = BigDecimal("2000.00"),
                        masterWallet = newMasterWallet,
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Virtual Goal", 1) } returns false
                every { walletService.existsByNameAndIdNot("Virtual Goal", 1) } returns false
                every { walletService.getWalletById(2) } returns newMasterWallet

                service.updateGoal(updatedGoal)

                Then("should decrease old master wallet balance") {
                    oldMasterWallet.balance shouldBe BigDecimal("8000.00")
                }

                Then("should increase new master wallet balance") {
                    newMasterWallet.balance shouldBe BigDecimal("7000.00")
                }

                Then("should update goal master wallet reference") {
                    existingGoal.masterWallet shouldBe newMasterWallet
                }
            }

            When("removing master wallet") {
                val masterWallet =
                    WalletFactory.create(
                        id = 1,
                        name = "Master Wallet",
                        balance = BigDecimal("10000.00"),
                        masterWallet = null,
                    )
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Virtual Goal",
                        initialBalance = BigDecimal("2000.00"),
                        masterWallet = masterWallet,
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Virtual Goal",
                        initialBalance = BigDecimal("2000.00"),
                        masterWallet = null,
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Virtual Goal", 1) } returns false
                every { walletService.existsByNameAndIdNot("Virtual Goal", 1) } returns false

                service.updateGoal(updatedGoal)

                Then("should decrease master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("8000.00")
                }

                Then("should remove master wallet reference") {
                    existingGoal.masterWallet shouldBe null
                }
            }

            When("adding master wallet to a master goal") {
                val masterWallet =
                    WalletFactory.create(
                        id = 1,
                        name = "Master Wallet",
                        balance = BigDecimal("10000.00"),
                        masterWallet = null,
                    )
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Master Goal",
                        initialBalance = BigDecimal("2000.00"),
                        masterWallet = null,
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Master Goal",
                        initialBalance = BigDecimal("2000.00"),
                        masterWallet = masterWallet,
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Master Goal", 1) } returns false
                every { walletService.existsByNameAndIdNot("Master Goal", 1) } returns false
                every { walletService.getWalletById(1) } returns masterWallet

                service.updateGoal(updatedGoal)

                Then("should increase master wallet balance") {
                    masterWallet.balance shouldBe BigDecimal("12000.00")
                }

                Then("should set master wallet reference") {
                    existingGoal.masterWallet shouldBe masterWallet
                }
            }

            When("trying to set a virtual wallet as master wallet") {
                val virtualWallet =
                    WalletFactory.create(
                        id = 2,
                        name = "Virtual Wallet",
                        balance = BigDecimal("1000.00"),
                        masterWallet =
                            WalletFactory.create(
                                id = 1,
                                name = "Main Wallet",
                            ),
                    )
                val existingGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal",
                        initialBalance = BigDecimal("2000.00"),
                        masterWallet = null,
                    )
                val updatedGoal =
                    GoalFactory.create(
                        id = 1,
                        name = "Goal",
                        initialBalance = BigDecimal("2000.00"),
                        masterWallet = virtualWallet,
                    )

                every { goalRepository.findById(1) } returns Optional.of(existingGoal)
                every { goalRepository.existsByNameAndIdNot("Goal", 1) } returns false
                every { walletService.existsByNameAndIdNot("Goal", 1) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateGoal(updatedGoal)
                    }
                }
            }
        }
    })
