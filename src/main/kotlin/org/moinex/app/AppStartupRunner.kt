/*
 * Filename: AppStartupRunner.kt (original filename: AppStartupRunner.java)
 * Created on: May 4, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 10/03/2026
 */

package org.moinex.app

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.moinex.common.util.APIUtils
import org.moinex.common.util.FxUtils
import org.moinex.service.creditcard.RecurringCreditCardDebtService
import org.moinex.service.investment.BondInterestCalculationService
import org.moinex.service.investment.InvestmentPerformanceService
import org.moinex.service.investment.MarketIndicatorService
import org.moinex.service.investment.MarketService
import org.moinex.service.investment.TickerPriceHistoryService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.RecurringTransactionService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class AppStartupRunner(
    private val marketService: MarketService,
    private val recurringTransactionService: RecurringTransactionService,
    private val recurringCreditCardDebtService: RecurringCreditCardDebtService,
    private val tickerPriceHistoryService: TickerPriceHistoryService,
    private val tickerService: TickerService,
    private val investmentPerformanceService: InvestmentPerformanceService,
    private val marketIndicatorService: MarketIndicatorService,
    private val bondInterestCalculationService: BondInterestCalculationService,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(AppStartupRunner::class.java)

    @Volatile
    private var isShuttingDown = false

    @PreDestroy
    fun onShutdown() {
        isShuttingDown = true
        logger.info("AppStartupRunner shutdown signal received")
    }

    override fun run(args: ApplicationArguments) {
        logger.info("AppStartupRunner started")

        FxUtils.launchOnBackground {
            logger.info("Processing recurring wallet transactions")
            recurringTransactionService.processRecurringTransactions()

            logger.info("Processing recurring credit card debts")
            recurringCreditCardDebtService.processRecurringDebts()

            logger.info("Startup pipeline beginning")
            startupPipeline()
            logger.info("Startup pipeline completed successfully")
        }
    }

    private suspend fun startupPipeline() =
        coroutineScope {
            val marketIndicatorStep =
                async {
                    runStep("Updating market indicator history") {
                        marketIndicatorService.updateAllIndicators()
                    }
                }

            val brazilianMarketIndicatorsStep =
                async {
                    runStep("Updating Brazilian market indicators") {
                        marketService.updateBrazilianMarketIndicatorsFromApi()
                    }
                }

            val quotesAndCommoditiesStep =
                async {
                    runStep("Updating market quotes and commodities") {
                        marketService.updateMarketQuotesAndCommoditiesFromApi()
                    }
                }

            marketIndicatorStep.await()
            brazilianMarketIndicatorsStep.await()
            quotesAndCommoditiesStep.await()

            val bondInterestCalculationStep =
                async {
                    runStep("Calculating bond interest") {
                        bondInterestCalculationService.calculateInterestForAllBondsIfNeeded()
                    }
                }

            val tickerPriceHistoryStep =
                async {
                    runStep("Initializing price history") {
                        tickerPriceHistoryService.initializePriceHistory()
                    }
                }

            val nonArchivedTickerPricesStep =
                async {
                    runStep("Updating ticker prices") {
                        tickerService.updateAllNonArchivedTickersPrices()
                    }
                }

            bondInterestCalculationStep.await()
            tickerPriceHistoryStep.await()
            nonArchivedTickerPricesStep.await()

            runStep("Recalculating investment performance snapshots") {
                investmentPerformanceService.recalculateAllSnapshots()
            }
        }

    private suspend fun <T> runStep(
        name: String,
        block: suspend () -> T,
    ): T? {
        if (isShuttingDown) {
            logger.info("{} skipped: Application is shutting down", name)
            return null
        }

        return runCatching {
            logger.info("Starting {}", name)
            val result = block()

            if (isShuttingDown || APIUtils.isShuttingDown()) {
                logger.info("{} completed but application is shutting down", name)
                return null
            }

            logger.info("{} completed successfully", name)
            result
        }.getOrElse { e ->
            if (isShuttingDown || APIUtils.isShuttingDown()) {
                logger.info("{} cancelled: Application is shutting down", name)
            } else {
                logger.error("{} failed", name, e)
            }
            null
        }
    }
}
