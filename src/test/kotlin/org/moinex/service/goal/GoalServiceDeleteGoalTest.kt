package org.moinex.service.goal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.goal.GoalFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.repository.goal.GoalRepository
import org.moinex.service.GoalService
import org.moinex.service.WalletService
import java.math.BigDecimal
import java.util.Optional

class GoalServiceDeleteGoalTest :
    BehaviorSpec({
        val goalRepository = mockk<GoalRepository>()
        val walletService = mockk<WalletService>()

        val service = GoalService(goalRepository, walletService)

        afterContainer { clearAllMocks(answers = true) }

        Given("an existing goal without transactions") {
            And("the goal is a master goal") {
                When("deleting the goal") {
                    val goal = GoalFactory.create(id = 1, name = "Master Goal", masterWallet = null)

                    every { goalRepository.findById(1) } returns Optional.of(goal)
                    every { walletService.getWalletTransactionAndTransferCountByWallet(1) } returns 0
                    every { walletService.removeAllVirtualWalletsFromMasterWallet(1) } just runs
                    every { goalRepository.delete(goal) } returns Unit

                    service.deleteGoal(1)

                    Then("should call removeAllVirtualWalletsFromMasterWallet") {
                        verify { walletService.removeAllVirtualWalletsFromMasterWallet(1) }
                    }

                    Then("should call repository delete method") {
                        verify { goalRepository.delete(goal) }
                    }
                }
            }

            And("the goal is a virtual goal") {
                When("deleting the goal") {
                    val masterWallet =
                        WalletFactory.create(
                            id = 1,
                            name = "Main Wallet",
                            balance = BigDecimal("5000.00"),
                            masterWallet = null,
                        )
                    val goal =
                        GoalFactory.create(
                            id = 2,
                            name = "Virtual Goal",
                            masterWallet = masterWallet,
                        )

                    every { goalRepository.findById(2) } returns Optional.of(goal)
                    every { walletService.getWalletTransactionAndTransferCountByWallet(2) } returns 0
                    every { goalRepository.delete(goal) } returns Unit

                    service.deleteGoal(2)

                    Then("should not call removeAllVirtualWalletsFromMasterWallet") {
                        verify(exactly = 0) { walletService.removeAllVirtualWalletsFromMasterWallet(any()) }
                    }

                    Then("should call repository delete method") {
                        verify { goalRepository.delete(goal) }
                    }
                }
            }
        }

        Given("an existing goal with transactions") {
            When("trying to delete the goal") {
                val goal = GoalFactory.create(id = 1, name = "Goal with Transactions")

                every { goalRepository.findById(1) } returns Optional.of(goal)
                every { walletService.getWalletTransactionAndTransferCountByWallet(1) } returns 5

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.deleteGoal(1)
                    }
                }

                Then("should not call repository delete method") {
                    verify(exactly = 0) { goalRepository.delete(any()) }
                }
            }
        }

        Given("a non-existent goal") {
            When("trying to delete the goal") {
                every { goalRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.deleteGoal(999)
                    }
                }

                Then("should not call repository delete method") {
                    verify(exactly = 0) { goalRepository.delete(any()) }
                }
            }
        }

        Given("a master goal with virtual wallets") {
            When("deleting the goal") {
                val goal = GoalFactory.create(id = 1, name = "Master Goal with Virtual Wallets", masterWallet = null)

                every { goalRepository.findById(1) } returns Optional.of(goal)
                every { walletService.getWalletTransactionAndTransferCountByWallet(1) } returns 0
                every { walletService.removeAllVirtualWalletsFromMasterWallet(1) } just runs
                every { goalRepository.delete(goal) } just runs

                service.deleteGoal(1)

                Then("should remove all virtual wallets before deleting") {
                    verify { walletService.removeAllVirtualWalletsFromMasterWallet(1) }
                    verify { goalRepository.delete(goal) }
                }
            }
        }
    })
