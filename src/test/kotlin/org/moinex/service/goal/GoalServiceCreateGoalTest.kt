package org.moinex.service.goal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.goal.GoalFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.enums.GoalFundingStrategy
import org.moinex.repository.goal.GoalRepository
import org.moinex.service.GoalService
import org.moinex.service.WalletService
import java.math.BigDecimal

class GoalServiceCreateGoalTest :
    BehaviorSpec({
        val goalRepository = mockk<GoalRepository>()
        val walletService = mockk<WalletService>()

        val service = GoalService(goalRepository, walletService)

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid goal without master wallet") {
            And("the goal name does not already exist") {
                When("creating a new goal") {
                    val goal = GoalFactory.create(id = null, name = "Emergency Fund")
                    every { goalRepository.existsByName("Emergency Fund") } returns false
                    every { walletService.existsByName("Emergency Fund") } returns false
                    every { walletService.existsWalletTypeByName("Goal") } returns true
                    every { goalRepository.save(any()) } returns
                        GoalFactory.create(id = 1, name = "Emergency Fund")

                    val result = service.createGoal(goal)

                    Then("should return the created goal id") {
                        result shouldBe 1
                    }

                    Then("should call repository save method") {
                        verify { goalRepository.save(any()) }
                    }
                }
            }

            And("the goal name already exists as a goal") {
                When("creating a new goal") {
                    val goal = GoalFactory.create(id = null, name = "Vacation")
                    every { goalRepository.existsByName("Vacation") } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createGoal(goal)
                        }
                    }
                }
            }

            And("the goal name already exists as a wallet") {
                When("creating a new goal") {
                    val goal = GoalFactory.create(id = null, name = "Main Wallet")
                    every { goalRepository.existsByName("Main Wallet") } returns false
                    every { walletService.existsByName("Main Wallet") } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createGoal(goal)
                        }
                    }
                }
            }

            And("the goal wallet type does not exist") {
                When("creating a new goal") {
                    val goal = GoalFactory.create(id = null, name = "House")
                    every { goalRepository.existsByName("House") } returns false
                    every { walletService.existsByName("House") } returns false
                    every { walletService.existsWalletTypeByName("Goal") } returns false

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createGoal(goal)
                        }
                    }
                }
            }
        }

        Given("a valid goal with master wallet") {
            And("the master wallet is a master wallet") {
                When("creating a new goal without funding strategy") {
                    val masterWallet =
                        WalletFactory.create(
                            id = 1,
                            name = "Main Wallet",
                            balance = BigDecimal("5000.00"),
                            masterWallet = null,
                        )
                    val goal =
                        GoalFactory.create(
                            id = null,
                            name = "Car",
                            initialBalance = BigDecimal("2000.00"),
                            masterWallet = masterWallet,
                        )

                    every { goalRepository.existsByName("Car") } returns false
                    every { walletService.existsByName("Car") } returns false
                    every { walletService.existsWalletTypeByName("Goal") } returns true
                    every { goalRepository.save(any()) } returns
                        GoalFactory.create(id = 1, name = "Car")

                    val result = service.createGoal(goal)

                    Then("should return the created goal id") {
                        result shouldBe 1
                    }

                    Then("should call repository save method") {
                        verify { goalRepository.save(any()) }
                    }
                }

                When("creating a new goal with NEW_DEPOSIT strategy") {
                    val masterWallet =
                        WalletFactory.create(
                            id = 1,
                            name = "Main Wallet",
                            balance = BigDecimal("5000.00"),
                            masterWallet = null,
                        )
                    val goal =
                        GoalFactory.create(
                            id = null,
                            name = "Motorcycle",
                            initialBalance = BigDecimal("1500.00"),
                            masterWallet = masterWallet,
                        )

                    every { goalRepository.existsByName("Motorcycle") } returns false
                    every { walletService.existsByName("Motorcycle") } returns false
                    every { walletService.existsWalletTypeByName("Goal") } returns true
                    every { walletService.getWalletById(1) } returns masterWallet
                    every { goalRepository.save(any()) } returns
                        GoalFactory.create(id = 1, name = "Motorcycle")

                    val result = service.createGoal(goal, GoalFundingStrategy.NEW_DEPOSIT)

                    Then("should return the created goal id") {
                        result shouldBe 1
                    }

                    Then("should increase master wallet balance") {
                        masterWallet.balance shouldBe BigDecimal("6500.00")
                    }

                    Then("should call repository save method") {
                        verify { goalRepository.save(any()) }
                    }
                }

                When("creating a new goal with ALLOCATE_FROM_EXISTING strategy") {
                    val masterWallet =
                        WalletFactory.create(
                            id = 1,
                            name = "Main Wallet",
                            balance = BigDecimal("5000.00"),
                            masterWallet = null,
                        )
                    val goal =
                        GoalFactory.create(
                            id = null,
                            name = "Laptop",
                            initialBalance = BigDecimal("3000.00"),
                            masterWallet = masterWallet,
                        )

                    every { goalRepository.existsByName("Laptop") } returns false
                    every { walletService.existsByName("Laptop") } returns false
                    every { walletService.existsWalletTypeByName("Goal") } returns true
                    every { walletService.getWalletById(1) } returns masterWallet
                    every { walletService.getUnallocatedBalance(masterWallet) } returns
                        BigDecimal("4000.00")
                    every { goalRepository.save(any()) } returns
                        GoalFactory.create(id = 1, name = "Laptop")

                    val result = service.createGoal(goal, GoalFundingStrategy.ALLOCATE_FROM_EXISTING)

                    Then("should return the created goal id") {
                        result shouldBe 1
                    }

                    Then("should not change master wallet balance") {
                        masterWallet.balance shouldBe BigDecimal("5000.00")
                    }

                    Then("should call repository save method") {
                        verify { goalRepository.save(any()) }
                    }
                }

                When("creating a new goal with ALLOCATE_FROM_EXISTING strategy but insufficient balance") {
                    val masterWallet =
                        WalletFactory.create(
                            id = 1,
                            name = "Main Wallet",
                            balance = BigDecimal("5000.00"),
                            masterWallet = null,
                        )
                    val goal =
                        GoalFactory.create(
                            id = null,
                            name = "House",
                            initialBalance = BigDecimal("50000.00"),
                            targetBalance = BigDecimal("50000.00"),
                            masterWallet = masterWallet,
                        )

                    every { goalRepository.existsByName("House") } returns false
                    every { walletService.existsByName("House") } returns false
                    every { walletService.existsWalletTypeByName("Goal") } returns true
                    every { walletService.getWalletById(1) } returns masterWallet
                    every { walletService.getUnallocatedBalance(masterWallet) } returns
                        BigDecimal("4000.00")

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createGoal(goal, GoalFundingStrategy.ALLOCATE_FROM_EXISTING)
                        }
                    }
                }
            }

            And("the master wallet is a virtual wallet") {
                When("creating a new goal") {
                    val masterWallet =
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
                    val goal =
                        GoalFactory.create(
                            id = null,
                            name = "Travel",
                            masterWallet = masterWallet,
                        )

                    every { goalRepository.existsByName("Travel") } returns false
                    every { walletService.existsByName("Travel") } returns false
                    every { walletService.existsWalletTypeByName("Goal") } returns true

                    Then("should throw IllegalStateException") {
                        shouldThrow<IllegalStateException> {
                            service.createGoal(goal)
                        }
                    }
                }
            }
        }

        Given("a goal with zero initial balance") {
            And("the goal has a master wallet with NEW_DEPOSIT strategy") {
                When("creating the goal") {
                    val masterWallet =
                        WalletFactory.create(
                            id = 1,
                            name = "Main Wallet",
                            balance = BigDecimal("5000.00"),
                            masterWallet = null,
                        )
                    val goal =
                        GoalFactory.create(
                            id = null,
                            name = "Future Goal",
                            initialBalance = BigDecimal.ZERO,
                            masterWallet = masterWallet,
                        )

                    every { goalRepository.existsByName("Future Goal") } returns false
                    every { walletService.existsByName("Future Goal") } returns false
                    every { walletService.existsWalletTypeByName("Goal") } returns true
                    every { goalRepository.save(any()) } returns
                        GoalFactory.create(id = 1, name = "Future Goal")

                    val result = service.createGoal(goal, GoalFundingStrategy.NEW_DEPOSIT)

                    Then("should return the created goal id") {
                        result shouldBe 1
                    }

                    Then("should not change master wallet balance") {
                        masterWallet.balance shouldBe BigDecimal("5000.00")
                    }
                }
            }
        }

        Given("a master goal (goal without master wallet)") {
            When("creating with NEW_DEPOSIT strategy") {
                val goal =
                    GoalFactory.create(
                        id = null,
                        name = "Master Goal",
                        initialBalance = BigDecimal("1000.00"),
                        masterWallet = null,
                    )

                every { goalRepository.existsByName("Master Goal") } returns false
                every { walletService.existsByName("Master Goal") } returns false
                every { walletService.existsWalletTypeByName("Goal") } returns true
                every { goalRepository.save(any()) } returns
                    GoalFactory.create(id = 1, name = "Master Goal")

                val result = service.createGoal(goal, GoalFundingStrategy.NEW_DEPOSIT)

                Then("should return the created goal id") {
                    result shouldBe 1
                }

                Then("should call repository save method") {
                    verify { goalRepository.save(any()) }
                }
            }
        }
    })
