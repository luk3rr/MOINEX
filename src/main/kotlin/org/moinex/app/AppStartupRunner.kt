/*
 * Filename: AppStartupRunner.kt (original filename: AppStartupRunner.java)
 * Created on: May 4, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 10/03/2026
 */

package org.moinex.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.BondInterestCalculationService
import org.moinex.service.InvestmentPerformanceCalculationService
import org.moinex.service.MarketIndicatorService
import org.moinex.service.MarketService
import org.moinex.service.RecurringTransactionService
import org.moinex.service.TickerPriceHistoryService
import org.moinex.service.TickerService
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class AppStartupRunner(
    private val marketService: MarketService,
    private val recurringTransactionService: RecurringTransactionService,
    private val priceHistoryService: TickerPriceHistoryService,
    private val tickerService: TickerService,
    private val tickerPurchaseRepository: TickerPurchaseRepository,
    private val tickerSaleRepository: TickerSaleRepository,
    private val investmentPerformanceCalculationService: InvestmentPerformanceCalculationService,
    private val marketIndicatorService: MarketIndicatorService,
    private val bondInterestCalculationService: BondInterestCalculationService,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(AppStartupRunner::class.java)

    override fun run(args: ApplicationArguments) {
        logger.info("AppStartupRunner started")
        recurringTransactionService.processRecurringTransactions()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                logger.info("Startup pipeline beginning")
                startupPipeline()
                logger.info("Startup pipeline completed successfully")
            } catch (e: Exception) {
                logger.error("Startup pipeline failed with exception", e)
            }
        }
    }

    private suspend fun startupPipeline() =
        coroutineScope {
            runStep("Updating market indicator history") {
                marketIndicatorService.updateAllIndicators()
            }

            runStep("Calculating bond interest") {
                bondInterestCalculationService.calculateInterestForAllBondsIfNeeded()
            }

            runStep("Updating Brazilian market indicators") {
                marketService.updateBrazilianMarketIndicatorsFromApiAsync().await()
            }

            runStep("Updating market quotes and commodities") {
                marketService.updateMarketQuotesAndCommoditiesFromApiAsync().await()
            }

            val priceHistoryInitialized =
                runStep("Initializing price history") {
                    priceHistoryService
                        .initializePriceHistory(
                            tickerPurchaseRepository,
                            tickerSaleRepository,
                        ).await()
                }

            if (priceHistoryInitialized != true) {
                logger.error("Startup pipeline aborted due to price history initialization failure")
                return@coroutineScope
            }

            runStep("Updating ticker prices") {
                tickerService.updateAllNonArchivedTickersPricesAsync().await()
            }

            runStep("Recalculating investment performance snapshots") {
                investmentPerformanceCalculationService.recalculateAllSnapshots().await()
            }
        }

    private suspend fun <T> runStep(
        name: String,
        block: suspend () -> T,
    ): T? =
        runCatching {
            logger.info("Starting {}", name)
            val result = block()
            logger.info("{} completed successfully", name)
            result
        }.getOrElse { e ->
            logger.error("{} failed", name, e)
            null
        }
}
