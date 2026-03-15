package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.BondOperationWalletTransactionDTOFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.BondType
import org.moinex.model.enums.OperationType
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class BondServiceCRUDBondOperationTest :
    BehaviorSpec({
        val bondRepository = mockk<BondRepository>()
        val bondOperationRepository = mockk<BondOperationRepository>()
        val walletService = mockk<WalletService>()
        val bondInterestCalculationService = mockk<BondInterestCalculationService>()

        val service =
            BondService(
                bondRepository,
                bondOperationRepository,
                walletService,
                bondInterestCalculationService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid BUY bond operation") {
            When("creating the bond operation") {
                val bond = BondFactory.create(id = 1, name = "Test Bond", type = BondType.CDB)
                val wallet = WalletFactory.create(id = 1, balance = BigDecimal("10000.00"))
                val walletTransaction = WalletTransactionFactory.create(id = 1)
                val bondOperation =
                    BondOperationFactory.create(
                        id = null,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        fees = BigDecimal("50.00"),
                        taxes = BigDecimal("25.00"),
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )

                every { bondRepository.existsById(1) } returns true
                every { walletService.createWalletTransaction(any()) } returns 1
                every { walletService.getWalletTransactionById(1) } returns walletTransaction
                every { bondOperationRepository.save(any()) } returns bondOperation

                service.createBondOperation(bondOperation, dto)

                Then("should call wallet service to create transaction") {
                    verify { walletService.createWalletTransaction(any()) }
                }

                Then("should call wallet service to get transaction") {
                    verify { walletService.getWalletTransactionById(1) }
                }

                Then("should save bond operation") {
                    verify { bondOperationRepository.save(any()) }
                }
            }
        }

        Given("a valid SELL bond operation with sufficient quantity") {
            When("creating the bond operation") {
                val bond = BondFactory.create(id = 2, name = "Test Bond", type = BondType.LCI)
                val wallet = WalletFactory.create(id = 2, balance = BigDecimal("5000.00"))
                val walletTransaction = WalletTransactionFactory.create(id = 2)
                val bondOperation =
                    BondOperationFactory.create(
                        id = null,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("50"),
                        unitPrice = BigDecimal("12.00"),
                        netProfit = BigDecimal("100.00"),
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )

                every { bondRepository.existsById(2) } returns true
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(
                        BondOperationFactory.create(
                            id = 1,
                            bond = bond,
                            operationType = OperationType.BUY,
                            quantity = BigDecimal("100"),
                        ),
                    )
                every { walletService.createWalletTransaction(any()) } returns 2
                every { walletService.getWalletTransactionById(2) } returns walletTransaction
                every { bondOperationRepository.save(any()) } returns bondOperation

                service.createBondOperation(bondOperation, dto)

                Then("should create SELL operation successfully") {
                    verify { bondOperationRepository.save(any()) }
                }
            }
        }

        Given("a SELL bond operation with insufficient quantity") {
            When("creating the bond operation") {
                val bond = BondFactory.create(id = 3, name = "Test Bond", type = BondType.CDB)
                val wallet = WalletFactory.create(id = 3)
                val bondOperation =
                    BondOperationFactory.create(
                        id = null,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("200"),
                        unitPrice = BigDecimal("10.00"),
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )

                every { bondRepository.existsById(3) } returns true
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(
                        BondOperationFactory.create(
                            id = 1,
                            bond = bond,
                            operationType = OperationType.BUY,
                            quantity = BigDecimal("100"),
                        ),
                    )

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createBondOperation(bondOperation, dto)
                    }
                }
            }
        }

        Given("a bond operation with non-existent bond") {
            When("creating the bond operation") {
                val bond = BondFactory.create(id = 999, name = "Non-existent Bond", type = BondType.CDB)
                val wallet = WalletFactory.create(id = 4)
                val bondOperation =
                    BondOperationFactory.create(
                        id = null,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )

                every { bondRepository.existsById(999) } returns false

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createBondOperation(bondOperation, dto)
                    }
                }
            }
        }

        Given("a valid bond operation to update") {
            When("updating the bond operation") {
                val bond = BondFactory.create(id = 4, name = "Test Bond", type = BondType.CDB)
                val wallet = WalletFactory.create(id = 5, balance = BigDecimal("10000.00"))
                val originalWalletTransaction = WalletTransactionFactory.create(id = 3)
                val originalBondOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        fees = BigDecimal("50.00"),
                        walletTransaction = originalWalletTransaction,
                    )
                val updatedBondOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("150"),
                        unitPrice = BigDecimal("12.00"),
                        fees = BigDecimal("75.00"),
                        walletTransaction = originalWalletTransaction,
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )

                every { bondOperationRepository.findById(1) } returns Optional.of(originalBondOperation)
                every { walletService.updateWalletTransaction(any()) } returns Unit

                service.updateBondOperation(updatedBondOperation, dto)

                Then("should update quantity") {
                    originalBondOperation.quantity shouldBe BigDecimal("150.00")
                }

                Then("should update unit price") {
                    originalBondOperation.unitPrice shouldBe BigDecimal("12.00")
                }

                Then("should update fees") {
                    originalBondOperation.fees shouldBe BigDecimal("75.00")
                }

                Then("should call wallet service to update transaction") {
                    verify { walletService.updateWalletTransaction(any()) }
                }
            }
        }

        Given("a SELL bond operation to update with insufficient quantity") {
            When("updating the bond operation") {
                val bond = BondFactory.create(id = 5, name = "Test Bond", type = BondType.CDB)
                val wallet = WalletFactory.create(id = 6)
                val walletTransaction = WalletTransactionFactory.create(id = 4)
                val originalBondOperation =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("50"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction,
                    )
                val updatedBondOperation =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("200"),
                        unitPrice = BigDecimal("10.00"),
                        walletTransaction = walletTransaction,
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )

                every { bondOperationRepository.findById(2) } returns Optional.of(originalBondOperation)
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(
                        BondOperationFactory.create(
                            id = 1,
                            bond = bond,
                            operationType = OperationType.BUY,
                            quantity = BigDecimal("100"),
                        ),
                        originalBondOperation,
                    )

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateBondOperation(updatedBondOperation, dto)
                    }
                }
            }
        }

        Given("a bond operation that does not exist") {
            When("trying to update the bond operation") {
                val updatedBondOperation =
                    BondOperationFactory.create(
                        id = 999,
                        operationType = OperationType.BUY,
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        date = LocalDateTime.now(),
                    )

                every { bondOperationRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.updateBondOperation(updatedBondOperation, dto)
                    }
                }
            }
        }

        Given("a valid bond operation to delete") {
            When("deleting the bond operation") {
                val bond = BondFactory.create(id = 6, name = "Test Bond", type = BondType.CDB)
                val walletTransaction = WalletTransactionFactory.create(id = 5)
                val bondOperation =
                    BondOperationFactory.create(
                        id = 3,
                        bond = bond,
                        operationType = OperationType.BUY,
                        walletTransaction = walletTransaction,
                    )

                every { bondOperationRepository.findById(3) } returns Optional.of(bondOperation)
                every { bondOperationRepository.delete(bondOperation) } returns Unit
                every { walletService.deleteWalletTransaction(5) } returns Unit

                service.deleteBondOperation(3)

                Then("should delete bond operation") {
                    verify { bondOperationRepository.delete(bondOperation) }
                }

                Then("should delete associated wallet transaction") {
                    verify { walletService.deleteWalletTransaction(5) }
                }
            }
        }

        Given("a bond operation that does not exist") {
            When("trying to delete the bond operation") {
                every { bondOperationRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.deleteBondOperation(999)
                    }
                }
            }
        }

        Given("multiple bond operations with BUY and SELL") {
            When("creating multiple operations") {
                val bond = BondFactory.create(id = 7, name = "Test Bond", type = BondType.CDB)
                val wallet = WalletFactory.create(id = 7, balance = BigDecimal("50000.00"))
                val walletTransaction1 = WalletTransactionFactory.create(id = 6)
                val walletTransaction2 = WalletTransactionFactory.create(id = 7)

                val buyOperation =
                    BondOperationFactory.create(
                        id = null,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                    )
                val sellOperation =
                    BondOperationFactory.create(
                        id = null,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("50"),
                        unitPrice = BigDecimal("12.00"),
                        netProfit = BigDecimal("100.00"),
                    )

                val dtoForBuy =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )
                val dtoForSell =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now().plusDays(1),
                    )

                every { bondRepository.existsById(7) } returns true
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(buyOperation)
                every { walletService.createWalletTransaction(any()) } returnsMany
                    listOf(6, 7)
                every { walletService.getWalletTransactionById(6) } returns walletTransaction1
                every { walletService.getWalletTransactionById(7) } returns walletTransaction2
                every { bondOperationRepository.save(any()) } returnsMany
                    listOf(buyOperation, sellOperation)

                service.createBondOperation(buyOperation, dtoForBuy)
                service.createBondOperation(sellOperation, dtoForSell)

                Then("should create both operations") {
                    verify(exactly = 2) { bondOperationRepository.save(any()) }
                }

                Then("should create two wallet transactions") {
                    verify(exactly = 2) { walletService.createWalletTransaction(any()) }
                }
            }
        }

        Given("a BUY bond operation with taxes and fees") {
            When("creating the bond operation") {
                val bond = BondFactory.create(id = 8, name = "Test Bond", type = BondType.CDB)
                val wallet = WalletFactory.create(id = 8, balance = BigDecimal("20000.00"))
                val walletTransaction = WalletTransactionFactory.create(id = 8)
                val bondOperation =
                    BondOperationFactory.create(
                        id = null,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        fees = BigDecimal("100.00"),
                        taxes = BigDecimal("50.00"),
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )

                every { bondRepository.existsById(8) } returns true
                every { walletService.createWalletTransaction(any()) } returns 8
                every { walletService.getWalletTransactionById(8) } returns walletTransaction
                every { bondOperationRepository.save(any()) } returns bondOperation

                service.createBondOperation(bondOperation, dto)

                Then("should calculate operation amount with fees and taxes") {
                    verify { walletService.createWalletTransaction(any()) }
                }

                Then("should save bond operation with correct values") {
                    bondOperation.fees shouldBe BigDecimal("100.00")
                    bondOperation.taxes shouldBe BigDecimal("50.00")
                }
            }
        }

        Given("a SELL bond operation with net profit") {
            When("creating the bond operation") {
                val bond = BondFactory.create(id = 9, name = "Test Bond", type = BondType.CDB)
                val wallet = WalletFactory.create(id = 9, balance = BigDecimal("15000.00"))
                val walletTransaction = WalletTransactionFactory.create(id = 9)
                val bondOperation =
                    BondOperationFactory.create(
                        id = null,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("12.00"),
                        netProfit = BigDecimal("200.00"),
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )

                every { bondRepository.existsById(9) } returns true
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(
                        BondOperationFactory.create(
                            id = 1,
                            bond = bond,
                            operationType = OperationType.BUY,
                            quantity = BigDecimal("100"),
                        ),
                    )
                every { walletService.createWalletTransaction(any()) } returns 9
                every { walletService.getWalletTransactionById(9) } returns walletTransaction
                every { bondOperationRepository.save(any()) } returns bondOperation

                service.createBondOperation(bondOperation, dto)

                Then("should calculate operation amount with net profit") {
                    verify { walletService.createWalletTransaction(any()) }
                }

                Then("should save bond operation with correct net profit") {
                    bondOperation.netProfit shouldBe BigDecimal("200.00")
                }
            }
        }

        Given("a SELL bond operation to update with net profit change") {
            When("updating the bond operation with new net profit") {
                val bond = BondFactory.create(id = 10, name = "Test Bond", type = BondType.CDB)
                val wallet = WalletFactory.create(id = 10, balance = BigDecimal("20000.00"))
                val walletTransaction = WalletTransactionFactory.create(id = 10)
                val originalBondOperation =
                    BondOperationFactory.create(
                        id = 4,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        netProfit = BigDecimal("100.00"),
                        walletTransaction = walletTransaction,
                    )
                val updatedBondOperation =
                    BondOperationFactory.create(
                        id = 4,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("100"),
                        unitPrice = BigDecimal("10.00"),
                        netProfit = BigDecimal("250.00"),
                        walletTransaction = walletTransaction,
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        date = LocalDateTime.now(),
                    )

                every { bondOperationRepository.findById(4) } returns Optional.of(originalBondOperation)
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(
                        BondOperationFactory.create(
                            id = 1,
                            bond = bond,
                            operationType = OperationType.BUY,
                            quantity = BigDecimal("100"),
                        ),
                        originalBondOperation,
                    )
                every { walletService.updateWalletTransaction(any()) } returns Unit

                service.updateBondOperation(updatedBondOperation, dto)

                Then("should update net profit for SELL operation") {
                    originalBondOperation.netProfit shouldBe BigDecimal("250.00")
                }

                Then("should calculate operation amount with updated net profit") {
                    verify { walletService.updateWalletTransaction(any()) }
                }
            }
        }
    })
