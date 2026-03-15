package org.moinex.service

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.common.extension.isZero
import org.moinex.factory.creditcard.CreditCardDebtFactory
import org.moinex.factory.creditcard.CreditCardFactory
import org.moinex.factory.creditcard.CreditCardOperatorFactory
import org.moinex.factory.creditcard.CreditCardPaymentFactory
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondInterestCalculationFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.investment.TickerPurchaseFactory
import org.moinex.factory.investment.TickerSaleFactory
import org.moinex.factory.wallet.RecurringTransactionFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.repository.NetWorthSnapshotRepository
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.investment.BondInterestCalculationService
import org.moinex.service.investment.BondService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class NetWorthServiceTest :
    BehaviorSpec({
        val netWorthSnapshotRepository = mockk<NetWorthSnapshotRepository>()
        val walletService = mockk<WalletService>()
        val recurringTransactionService = mockk<RecurringTransactionService>()
        val creditCardService = mockk<CreditCardService>()
        val tickerService = mockk<TickerService>()
        val bondService = mockk<BondService>()
        val bondInterestCalculationService = mockk<BondInterestCalculationService>()

        val service =
            NetWorthService(
                netWorthSnapshotRepository,
                walletService,
                recurringTransactionService,
                creditCardService,
                tickerService,
                bondService,
                bondInterestCalculationService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a system with wallets and transactions from past months") {
            val wallet1 = WalletFactory.create(id = 1, name = "Wallet 1", balance = BigDecimal("5000.00"))
            val wallet2 = WalletFactory.create(id = 2, name = "Wallet 2", balance = BigDecimal("3000.00"))

            val transactionDate = LocalDateTime.of(2025, 6, 15, 10, 0)

            When("recalculating all snapshots") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet1, wallet2)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns transactionDate
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should delete snapshots outside calculation range") {
                    verify { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) }
                }

                Then("should save snapshots for each month") {
                    verify(atLeast = 1) { netWorthSnapshotRepository.save(any()) }
                }

                Then("should calculate snapshots from earliest transaction date") {
                    coVerify { walletService.getEarliestTransactionDateByWallets(any()) }
                }
            }
        }

        Given("a system with only current month wallets") {
            val wallet = WalletFactory.create(id = 1, name = "Main Wallet", balance = BigDecimal("10000.00"))

            When("recalculating snapshots with no historical transactions") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns null
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should use current date as earliest date") {
                    verify { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) }
                }

                Then("should still save snapshots") {
                    verify(atLeast = 1) { netWorthSnapshotRepository.save(any()) }
                }
            }
        }

        Given("a system with wallet transactions in multiple months") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("8000.00"))
            val earliestDate = LocalDateTime.of(2025, 1, 15, 10, 0)

            val transaction1 =
                WalletTransactionFactory.create(
                    id = 1,
                    wallet = wallet,
                    date = LocalDateTime.of(2025, 1, 20, 10, 0),
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("1000.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            val transaction2 =
                WalletTransactionFactory.create(
                    id = 2,
                    wallet = wallet,
                    date = LocalDateTime.of(2025, 2, 15, 10, 0),
                    type = WalletTransactionType.EXPENSE,
                    amount = BigDecimal("500.00"),
                    status = WalletTransactionStatus.CONFIRMED,
                )

            When("calculating snapshots with historical transactions") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns earliestDate
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns listOf(transaction1, transaction2)
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should calculate wallet balances for each month") {
                    verify(atLeast = 1) { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) }
                }

                Then("should save snapshots for all months") {
                    verify(atLeast = 1) { netWorthSnapshotRepository.save(any()) }
                }
            }
        }

        Given("a system with credit card debts") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("5000.00"))
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            val creditCard = CreditCardFactory.create(id = 1, operator = operator)
            CreditCardDebtFactory.create(
                id = 1,
                creditCard = creditCard,
                amount = BigDecimal("1000.00"),
            )

            When("calculating net worth with credit card debts") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal("500.00")
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should calculate credit card debt") {
                    coVerify { creditCardService.getDebtAtDate(any()) }
                }

                Then("should include debt in liabilities") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.creditCardDebt == BigDecimal("500.00")
                            },
                        )
                    }
                }
            }
        }

        Given("a system with ticker investments") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("10000.00"))
            val ticker =
                TickerFactory.create(
                    id = 1,
                    symbol = "PETR4",
                    currentQuantity = BigDecimal("100"),
                    currentUnitValue = BigDecimal("25.00"),
                )

            val purchase =
                TickerPurchaseFactory.create(
                    id = 1,
                    ticker = ticker,
                    quantity = BigDecimal("100"),
                    unitPrice = BigDecimal("20.00"),
                )

            When("calculating net worth with ticker investments") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                coEvery { tickerService.allPurchases } returns listOf(purchase)
                coEvery { tickerService.allSales } returns emptyList()
                coEvery { tickerService.allTickers } returns listOf(ticker)
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should calculate ticker investment value") {
                    coVerify { tickerService.allPurchases }
                    coVerify { tickerService.allTickers }
                }

                Then("should include ticker value in investments") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.investments > BigDecimal.ZERO
                            },
                        )
                    }
                }
            }
        }

        Given("a system with bond investments") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("10000.00"))
            val bond = BondFactory.create(id = 1, name = "Test Bond")
            val walletTransaction =
                WalletTransactionFactory.create(
                    date = LocalDateTime.of(2025, 1, 15, 10, 0),
                )
            val bondOperation =
                BondOperationFactory.create(
                    id = 1,
                    bond = bond,
                    operationType = OperationType.BUY,
                    quantity = BigDecimal("100"),
                    unitPrice = BigDecimal("10.00"),
                    walletTransaction = walletTransaction,
                )

            When("calculating net worth with bond investments") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns listOf(bondOperation)
                coEvery { bondInterestCalculationService.getMonthlyInterestHistory(bond) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should calculate bond investment value") {
                    coVerify { bondService.getOperationsByDateBefore(any()) }
                }

                Then("should include bond value in investments") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.investments > BigDecimal.ZERO
                            },
                        )
                    }
                }
            }
        }

        Given("a system with recurring transactions marked for net worth") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("5000.00"))
            val recurringIncome =
                RecurringTransactionFactory.create(
                    id = 1,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("2000.00"),
                    startDate = LocalDate.of(2025, 1, 1),
                    endDate = LocalDate.of(2026, 12, 31),
                    includeInNetWorth = true,
                )

            When("calculating net worth with recurring income") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(WalletTransactionType.INCOME) } returns
                    listOf(recurringIncome)
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(WalletTransactionType.EXPENSE) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should calculate recurring income") {
                    coVerify { recurringTransactionService.getAllRecurringTransactionsByType(WalletTransactionType.INCOME) }
                }

                Then("should include recurring income in assets") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.assets > BigDecimal.ZERO
                            },
                        )
                    }
                }
            }
        }

        Given("a system with multiple assets and liabilities") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("10000.00"))
            val ticker =
                TickerFactory.create(
                    id = 1,
                    currentQuantity = BigDecimal("50"),
                    currentUnitValue = BigDecimal("100.00"),
                )
            val purchase = TickerPurchaseFactory.create(ticker = ticker)

            When("calculating complete net worth snapshot") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal("2000.00")
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                coEvery { tickerService.allPurchases } returns listOf(purchase)
                coEvery { tickerService.allSales } returns emptyList()
                coEvery { tickerService.allTickers } returns listOf(ticker)
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should calculate net worth as assets minus liabilities") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.netWorth == snapshot.assets.minus(snapshot.liabilities)
                            },
                        )
                    }
                }

                Then("should include all asset components") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.assets == snapshot.walletBalances.add(snapshot.investments)
                            },
                        )
                    }
                }

                Then("should include credit card debt in liabilities") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.creditCardDebt == BigDecimal("2000.00")
                            },
                        )
                    }
                }
            }
        }

        Given("a system with negative wallet balances") {
            val negativeWallet = WalletFactory.create(id = 1, name = "Negative Wallet", balance = BigDecimal("-500.00"))
            val positiveWallet = WalletFactory.create(id = 2, name = "Positive Wallet", balance = BigDecimal("3000.00"))

            When("calculating net worth with negative wallet") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(negativeWallet, positiveWallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should only count positive wallet balances as assets") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.walletBalances == BigDecimal("3000.00")
                            },
                        )
                    }
                }
            }
        }

        Given("a system with pending wallet transactions") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("5000.00"))
            val pendingTransaction =
                WalletTransactionFactory.create(
                    wallet = wallet,
                    status = WalletTransactionStatus.PENDING,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("1000.00"),
                    date = LocalDateTime.now(),
                )

            When("calculating current month net worth with pending transactions") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns listOf(pendingTransaction)
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should include pending income in current month assets") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.walletBalances >= BigDecimal("5000.00")
                            },
                        )
                    }
                }
            }
        }

        Given("a system with multiple ticker sales") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("10000.00"))
            val ticker =
                TickerFactory.create(
                    id = 1,
                    currentQuantity = BigDecimal("50"),
                    currentUnitValue = BigDecimal("100.00"),
                )
            val purchase =
                TickerPurchaseFactory.create(
                    ticker = ticker,
                    quantity = BigDecimal("100"),
                    unitPrice = BigDecimal("90.00"),
                )
            val sale =
                TickerSaleFactory.create(
                    ticker = ticker,
                    quantity = BigDecimal("50"),
                    unitPrice = BigDecimal("110.00"),
                )

            When("calculating net worth with ticker sales") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                coEvery { tickerService.allPurchases } returns listOf(purchase)
                coEvery { tickerService.allSales } returns listOf(sale)
                coEvery { tickerService.allTickers } returns listOf(ticker)
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should account for quantity changes from sales") {
                    coVerify { tickerService.allSales }
                }

                Then("should calculate correct remaining ticker value") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.investments > BigDecimal.ZERO
                            },
                        )
                    }
                }
            }
        }

        Given("a system with bond operations and interest calculations") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("10000.00"))
            val bond = BondFactory.create(id = 1, name = "Test Bond")
            val walletTransaction =
                WalletTransactionFactory.create(
                    date = LocalDateTime.of(2025, 1, 15, 10, 0),
                )
            val bondOperation =
                BondOperationFactory.create(
                    bond = bond,
                    operationType = OperationType.BUY,
                    quantity = BigDecimal("100"),
                    unitPrice = BigDecimal("10.00"),
                    walletTransaction = walletTransaction,
                )
            val interestCalculation =
                BondInterestCalculationFactory.create(
                    bond = bond,
                    referenceMonth = YearMonth.now(),
                    quantity = BigDecimal("100"),
                    finalValue = BigDecimal("1050.00"),
                )

            When("calculating net worth with bond interest") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns listOf(bondOperation)
                coEvery { bondInterestCalculationService.getMonthlyInterestHistory(bond) } returns listOf(interestCalculation)
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should use final value from interest calculation") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.investments == BigDecimal("1050.00")
                            },
                        )
                    }
                }
            }
        }

        Given("a system with credit card payments reverting wallet balance") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("5000.00"))
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            CreditCardFactory.create(id = 1, operator = operator)
            val payment =
                CreditCardPaymentFactory.create(
                    id = 1,
                    amount = BigDecimal("500.00"),
                    rebateUsed = BigDecimal("50.00"),
                    wallet = wallet,
                )

            When("calculating historical wallet balance with credit card payments") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.of(2025, 1, 15, 10, 0)
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns listOf(payment)
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should revert credit card payments from wallet balance") {
                    coVerify { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) }
                }
            }
        }

        Given("a system with zero assets and liabilities") {
            val wallet = WalletFactory.create(id = 1, name = "Empty Wallet", balance = BigDecimal.ZERO)

            When("calculating net worth with empty system") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should calculate zero net worth") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.netWorth.isZero()
                            },
                        )
                    }
                }

                Then("should still save snapshot") {
                    verify { netWorthSnapshotRepository.save(any()) }
                }
            }
        }

        Given("a system with complex multi-month scenario") {
            val wallet1 = WalletFactory.create(id = 1, name = "Wallet 1", balance = BigDecimal("5000.00"))
            val wallet2 = WalletFactory.create(id = 2, name = "Wallet 2", balance = BigDecimal("3000.00"))
            val ticker =
                TickerFactory.create(
                    id = 1,
                    currentQuantity = BigDecimal("100"),
                    currentUnitValue = BigDecimal("50.00"),
                )
            val purchase = TickerPurchaseFactory.create(ticker = ticker)
            val operator = CreditCardOperatorFactory.create(id = 1, name = "Visa")
            CreditCardFactory.create(id = 1, operator = operator)

            When("calculating complete net worth with all components") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet1, wallet2)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.of(2025, 1, 1, 0, 0)
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal("1000.00")
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                coEvery { tickerService.allPurchases } returns listOf(purchase)
                coEvery { tickerService.allSales } returns emptyList()
                coEvery { tickerService.allTickers } returns listOf(ticker)
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should calculate complete net worth correctly") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.assets == snapshot.walletBalances.add(snapshot.investments) &&
                                    snapshot.liabilities == snapshot.creditCardDebt &&
                                    snapshot.netWorth == snapshot.assets.minus(snapshot.liabilities)
                            },
                        )
                    }
                }

                Then("should include all wallet balances") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.walletBalances == BigDecimal("8000.00")
                            },
                        )
                    }
                }

                Then("should include ticker investments") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.investments > BigDecimal.ZERO
                            },
                        )
                    }
                }

                Then("should include credit card debt") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.creditCardDebt == BigDecimal("1000.00")
                            },
                        )
                    }
                }
            }
        }

        Given("a system with bond sell operations") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("10000.00"))
            val bond = BondFactory.create(id = 1, name = "Test Bond")
            val buyTransaction =
                WalletTransactionFactory.create(
                    date = LocalDateTime.of(2025, 1, 15, 10, 0),
                )
            val sellTransaction =
                WalletTransactionFactory.create(
                    date = LocalDateTime.of(2025, 3, 15, 10, 0),
                )
            val buyOperation =
                BondOperationFactory.create(
                    id = 1,
                    bond = bond,
                    operationType = OperationType.BUY,
                    quantity = BigDecimal("100"),
                    unitPrice = BigDecimal("10.00"),
                    walletTransaction = buyTransaction,
                )
            val sellOperation =
                BondOperationFactory.create(
                    id = 2,
                    bond = bond,
                    operationType = OperationType.SELL,
                    quantity = BigDecimal("50"),
                    unitPrice = BigDecimal("12.00"),
                    walletTransaction = sellTransaction,
                )

            When("calculating net worth with bond sell operations") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns listOf(buyOperation, sellOperation)
                coEvery { bondInterestCalculationService.getMonthlyInterestHistory(bond) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should calculate remaining bond quantity after sell") {
                    coVerify { bondService.getOperationsByDateBefore(any()) }
                }

                Then("should include remaining bond value in investments") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.investments > BigDecimal.ZERO
                            },
                        )
                    }
                }
            }
        }

        Given("a system with existing snapshots to update") {
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("5000.00"))
            val existingSnapshot =
                org.moinex.model.NetWorthSnapshot(
                    referenceMonth = YearMonth.now(),
                    assets = BigDecimal("3000.00"),
                    liabilities = BigDecimal("1000.00"),
                    netWorth = BigDecimal("2000.00"),
                    walletBalances = BigDecimal("3000.00"),
                    investments = BigDecimal.ZERO,
                    creditCardDebt = BigDecimal("1000.00"),
                    negativeWalletBalances = BigDecimal.ZERO,
                    calculatedAt = LocalDateTime.now().minusDays(1),
                )

            When("recalculating snapshots with existing data") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns existingSnapshot
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should update existing snapshot") {
                    verify(atLeast = 1) { netWorthSnapshotRepository.save(any()) }
                }

                Then("should call findByReferenceMonth to check for existing snapshot") {
                    verify(atLeast = 1) { netWorthSnapshotRepository.findByReferenceMonth(any()) }
                }
            }
        }

        Given("a system with wallets having mixed positive and negative balances") {
            val positiveWallet1 = WalletFactory.create(id = 1, name = "Positive 1", balance = BigDecimal("3000.00"))
            val positiveWallet2 = WalletFactory.create(id = 2, name = "Positive 2", balance = BigDecimal("2000.00"))
            val negativeWallet1 = WalletFactory.create(id = 3, name = "Negative 1", balance = BigDecimal("-500.00"))
            val negativeWallet2 = WalletFactory.create(id = 4, name = "Negative 2", balance = BigDecimal("-300.00"))
            val zeroWallet = WalletFactory.create(id = 5, name = "Zero", balance = BigDecimal.ZERO)

            When("calculating net worth with mixed wallet balances") {
                coEvery { walletService.getAllWalletsOrderedByName() } returns
                    listOf(positiveWallet1, positiveWallet2, negativeWallet1, negativeWallet2, zeroWallet)
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { walletService.getEarliestTransactionDateByWallets(any()) } returns LocalDateTime.now()
                every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
                every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns emptyList()
                coEvery { recurringTransactionService.getAllRecurringTransactionsByType(any()) } returns emptyList()
                coEvery { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
                coEvery { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
                every { tickerService.allPurchases } returns emptyList()
                every { tickerService.allSales } returns emptyList()
                every { tickerService.allTickers } returns emptyList()
                coEvery { bondService.getOperationsByDateBefore(any()) } returns emptyList()
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.recalculateAllSnapshots()

                Then("should correctly separate positive and negative balances") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.walletBalances == BigDecimal("5000.00") &&
                                    snapshot.negativeWalletBalances == BigDecimal("800.00")
                            },
                        )
                    }
                }

                Then("should not include zero balance in either category") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.walletBalances > BigDecimal.ZERO &&
                                    snapshot.negativeWalletBalances > BigDecimal.ZERO
                            },
                        )
                    }
                }

                Then("should calculate correct net worth") {
                    verify {
                        netWorthSnapshotRepository.save(
                            match { snapshot ->
                                snapshot.netWorth == BigDecimal("4200.00")
                            },
                        )
                    }
                }
            }
        }
    })
