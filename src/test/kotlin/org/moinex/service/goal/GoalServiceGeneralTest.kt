package org.moinex.service.goal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.goal.GoalFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.repository.goal.GoalRepository
import org.moinex.service.goal.GoalService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class GoalServiceGeneralTest :
    BehaviorSpec({
        val goalRepository = mockk<GoalRepository>()
        val walletService = mockk<WalletService>()

        val service = GoalService(goalRepository, walletService)

        afterContainer { clearAllMocks(answers = true) }

        Given("a non-archived goal") {
            val goal = GoalFactory.create(id = 1, name = "Goal to Archive", isArchived = false)

            When("archiving the goal") {
                every { goalRepository.findById(1) } returns Optional.of(goal)
                every { walletService.removeAllVirtualWalletsFromMasterWallet(1) } returns Unit

                service.archiveGoal(1)

                Then("should set isArchived to true") {
                    goal.isArchived shouldBe true
                }
            }
        }

        Given("an already archived goal") {
            val goal = GoalFactory.create(id = 2, name = "Already Archived", isArchived = true)

            When("archiving an already archived goal") {
                every { goalRepository.findById(2) } returns Optional.of(goal)
                every { walletService.removeAllVirtualWalletsFromMasterWallet(2) } returns Unit

                service.archiveGoal(2)

                Then("should remain archived") {
                    goal.isArchived shouldBe true
                }
            }
        }

        Given("a master goal being archived") {
            val goal = GoalFactory.create(id = 3, name = "Master Goal", isArchived = false, masterWallet = null)

            When("archiving a master goal") {
                every { goalRepository.findById(3) } returns Optional.of(goal)
                every { walletService.removeAllVirtualWalletsFromMasterWallet(3) } returns Unit

                service.archiveGoal(3)

                Then("should set isArchived to true") {
                    goal.isArchived shouldBe true
                }

                Then("should remove all virtual wallets") {
                    verify { walletService.removeAllVirtualWalletsFromMasterWallet(3) }
                }
            }
        }

        Given("a virtual goal being archived") {
            val masterWallet =
                WalletFactory.create(
                    id = 1,
                    name = "Main Wallet",
                    balance = BigDecimal("5000.00"),
                    masterWallet = null,
                )
            val goal =
                GoalFactory.create(
                    id = 4,
                    name = "Virtual Goal",
                    isArchived = false,
                    masterWallet = masterWallet,
                )

            When("archiving a virtual goal") {
                every { goalRepository.findById(4) } returns Optional.of(goal)

                service.archiveGoal(4)

                Then("should set isArchived to true") {
                    goal.isArchived shouldBe true
                }

                Then("should remove master wallet reference") {
                    goal.masterWallet shouldBe null
                }
            }
        }

        Given("a non-existent goal to archive") {
            When("attempting to archive a non-existent goal") {
                every { goalRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.archiveGoal(999)
                    }
                }
            }
        }

        Given("an archived goal") {
            val goal = GoalFactory.create(id = 5, name = "Goal to Unarchive", isArchived = true)

            When("unarchiving the goal") {
                every { goalRepository.findById(5) } returns Optional.of(goal)

                service.unarchiveGoal(5)

                Then("should set isArchived to false") {
                    goal.isArchived shouldBe false
                }
            }
        }

        Given("a non-archived goal to unarchive") {
            val goal = GoalFactory.create(id = 6, name = "Already Active", isArchived = false)

            When("unarchiving a non-archived goal") {
                every { goalRepository.findById(6) } returns Optional.of(goal)

                service.unarchiveGoal(6)

                Then("should remain non-archived") {
                    goal.isArchived shouldBe false
                }
            }
        }

        Given("a non-existent goal to unarchive") {
            When("attempting to unarchive a non-existent goal") {
                every { goalRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.unarchiveGoal(999)
                    }
                }
            }
        }

        Given("an incomplete goal with sufficient balance") {
            val goal =
                GoalFactory.create(
                    id = 7,
                    name = "Goal to Complete",
                    initialBalance = BigDecimal("10000.00"),
                    targetBalance = BigDecimal("10000.00"),
                    completionDate = null,
                    masterWallet = null,
                )

            When("completing the goal") {
                every { goalRepository.findById(7) } returns Optional.of(goal)

                service.completeGoal(7)

                Then("should set completion date to today") {
                    goal.completionDate shouldBe LocalDate.now()
                }

                Then("should set target balance to current balance") {
                    goal.targetBalance shouldBe BigDecimal("10000.00")
                }

                Then("should remove master wallet reference") {
                    goal.masterWallet shouldBe null
                }
            }
        }

        Given("an incomplete goal with insufficient balance") {
            val goal =
                GoalFactory.create(
                    id = 8,
                    name = "Incomplete Goal",
                    initialBalance = BigDecimal("5000.00"),
                    targetBalance = BigDecimal("10000.00"),
                    completionDate = null,
                )

            When("attempting to complete goal with insufficient balance") {
                every { goalRepository.findById(8) } returns Optional.of(goal)

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.completeGoal(8)
                    }
                }

                Then("should not set completion date") {
                    goal.completionDate shouldBe null
                }
            }
        }

        Given("a completed goal") {
            val goal =
                GoalFactory.create(
                    id = 9,
                    name = "Already Completed",
                    initialBalance = BigDecimal("10000.00"),
                    targetBalance = BigDecimal("10000.00"),
                    completionDate = LocalDate.now().minusDays(10),
                )

            When("completing an already completed goal") {
                every { goalRepository.findById(9) } returns Optional.of(goal)

                service.completeGoal(9)

                Then("should update completion date to today") {
                    goal.completionDate shouldBe LocalDate.now()
                }
            }
        }

        Given("a non-existent goal to complete") {
            When("attempting to complete a non-existent goal") {
                every { goalRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.completeGoal(999)
                    }
                }
            }
        }

        Given("a completed goal with a completion date") {
            val goal =
                GoalFactory.create(
                    id = 10,
                    name = "Completed Goal",
                    initialBalance = BigDecimal("10000.00"),
                    targetBalance = BigDecimal("10000.00"),
                    completionDate = LocalDate.now().minusDays(5),
                )

            When("reopening the goal") {
                every { goalRepository.findById(10) } returns Optional.of(goal)

                service.reopenGoal(10)

                Then("should remove completion date") {
                    goal.completionDate shouldBe null
                }
            }
        }

        Given("an incomplete goal") {
            val goal =
                GoalFactory.create(
                    id = 11,
                    name = "Incomplete Goal",
                    initialBalance = BigDecimal("5000.00"),
                    targetBalance = BigDecimal("10000.00"),
                    completionDate = null,
                )

            When("reopening an incomplete goal") {
                every { goalRepository.findById(11) } returns Optional.of(goal)

                service.reopenGoal(11)

                Then("should remain incomplete") {
                    goal.completionDate shouldBe null
                }
            }
        }

        Given("a non-existent goal to reopen") {
            When("attempting to reopen a non-existent goal") {
                every { goalRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.reopenGoal(999)
                    }
                }
            }
        }
    })
