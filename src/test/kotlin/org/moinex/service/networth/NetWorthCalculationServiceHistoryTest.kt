package org.moinex.service.networth

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.moinex.common.ClockProvider
import org.moinex.factory.investment.TickerFactory
import org.moinex.factory.wallet.RecurringTransactionFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.model.NetWorthSnapshot
import org.moinex.model.enums.WalletTransactionType
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.moinex.service.investment.BondInterestCalculationService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerPriceHistoryService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class NetWorthCalculationServiceHistoryTest :
    BehaviorSpec({
        val clockProvider = ClockProvider()
        val currentMonth = clockProvider.currentMonth()

        fun recalculate(
            tickers: List<org.moinex.model.investment.Ticker> = emptyList(),
            recurring: List<org.moinex.model.wallettransaction.RecurringTransaction> = emptyList(),
            priceOf: (LocalDate) -> BigDecimal? = { null },
        ): Map<YearMonth, NetWorthSnapshot> {
            val netWorthService = mockk<NetWorthService>()
            val walletService = mockk<WalletService>()
            val recurringTransactionService = mockk<RecurringTransactionService>()
            val creditCardService = mockk<CreditCardService>()
            val tickerService = mockk<TickerService>()
            val bondService = mockk<BondService>()
            val priceService = mockk<TickerPriceHistoryService>()
            val wallet = WalletFactory.create(id = 1, name = "Wallet", balance = BigDecimal("5000.00"))
            val saved = slot<List<NetWorthSnapshot>>()
            val all = mutableListOf<NetWorthSnapshot>()

            every { walletService.getAllWalletsOrderedByName() } returns listOf(wallet)
            every { netWorthService.deleteSnapshotsOutsideRange(any(), any()) } returns Unit
            every { netWorthService.findByReferenceMonth(any()) } returns null
            every { walletService.getEarliestTransactionDateByWallets(any()) } returns
                LocalDateTime.now().minusMonths(3)
            every { walletService.getAllNonArchivedWalletTransactionsByMonthForAnalysis(any()) } returns emptyList()
            every { walletService.getAllTransfersByWalletsFromDate(any(), any()) } returns emptyList()
            every { walletService.getAllWalletTransactionsByWalletsAfterDate(any(), any()) } returns emptyList()
            every { recurringTransactionService.getFutureRecurringTransactionsByMonthForAnalysis(any(), any()) } returns
                emptyList()
            every {
                recurringTransactionService.getAllRecurringTransactionsByType(
                    WalletTransactionType.INCOME,
                )
            } returns
                recurring
            every {
                recurringTransactionService.getAllRecurringTransactionsByType(
                    WalletTransactionType.EXPENSE,
                )
            } returns
                emptyList()
            every { creditCardService.getDebtAtDate(any()) } returns BigDecimal.ZERO
            every { creditCardService.getAllPaidPaymentsByWalletsFromDateOnward(any(), any()) } returns emptyList()
            every { tickerService.getAllPurchases() } returns emptyList()
            every { tickerService.getAllSales() } returns emptyList()
            every { tickerService.getAllTickers() } returns tickers
            every { bondService.getOperationsByDateBefore(any()) } returns emptyList()
            every { priceService.getClosestPriceBeforeDate(any(), any()) } answers { priceOf(secondArg()) }
            every { netWorthService.saveBatch(capture(saved)) } answers {
                all.addAll(saved.captured)
            }

            kotlinx.coroutines.runBlocking {
                NetWorthCalculationService(
                    netWorthService,
                    walletService,
                    recurringTransactionService,
                    creditCardService,
                    mockk<RecurringCreditCardDebtService>(relaxed = true),
                    tickerService,
                    bondService,
                    mockk<BondInterestCalculationService>(),
                    priceService,
                    clockProvider,
                ).recalculateAllSnapshots()
            }

            return all.associateBy { it.referenceMonth }
        }

        Given("10 units of a ticker worth R$ 20 today and R$ 10 at the close of a past month") {
            val ticker =
                TickerFactory.create(
                    id = 1,
                    currentQuantity = BigDecimal("10"),
                    currentUnitValue = BigDecimal("20.00"),
                )
            val firstMonth = currentMonth.minusMonths(3)

            When("the net worth of that past month is calculated") {
                val snapshots =
                    recalculate(
                        tickers = listOf(ticker),
                        priceOf = { date -> if (YearMonth.from(date) == firstMonth) BigDecimal("10.00") else null },
                    )

                Then("the investments are valued at the close of that month, R$ 100, not at today's price") {
                    snapshots.getValue(firstMonth).investments.compareTo(BigDecimal("100.00")) shouldBe 0
                }
            }
        }

        Given("a finite monthly recurring income of R$ 2.000 included in the net worth") {
            val wallet = WalletFactory.create(id = 1)
            val recurring =
                RecurringTransactionFactory.create(
                    id = 1,
                    wallet = wallet,
                    type = WalletTransactionType.INCOME,
                    amount = BigDecimal("2000.00"),
                    startDate = LocalDate.now().minusMonths(6),
                    nextDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1),
                    endDate = LocalDate.now().plusMonths(24),
                    includeInNetWorth = true,
                )

            When("the net worth of past, current and future months is calculated") {
                val snapshots = recalculate(recurring = listOf(recurring))
                val wallets = BigDecimal("5000.00")

                Then(
                    "past and current months do not count installments already generated or due: assets equal the wallets",
                ) {
                    snapshots.getValue(currentMonth.minusMonths(3)).assets.compareTo(wallets) shouldBe 0
                    snapshots.getValue(currentMonth.minusMonths(1)).assets.compareTo(wallets) shouldBe 0
                    snapshots.getValue(currentMonth).assets.compareTo(wallets) shouldBe 0
                }

                Then("a future month counts the installment still to be generated: R$ 5.000 + R$ 2.000") {
                    snapshots.getValue(currentMonth.plusMonths(2)).assets.compareTo(BigDecimal("7000.00")) shouldBe 0
                }
            }
        }
    })
