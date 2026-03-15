/*
 * Filename: TickerService.kt (original filename: TickerService.java)
 * Created on: January  6, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 15/03/2026
 */

package org.moinex.service.investment

import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isZero
import org.moinex.common.retry.retry
import org.moinex.config.RetryConfig
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.CryptoExchange
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.service.wallet.WalletService
import org.moinex.util.APIUtils
import org.moinex.util.Constants
import org.moinex.util.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class TickerService(
    private val tickerRepository: TickerRepository,
    private val tickerPurchaseRepository: TickerPurchaseRepository,
    private val tickerSaleRepository: TickerSaleRepository,
    private val dividendRepository: DividendRepository,
    private val cryptoExchangeRepository: CryptoExchangeRepository,
    private val walletService: WalletService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        private const val API_RESPONSE_LOGO_PATH_FIELD = "logo_path"
        private const val API_RESPONSE_ERROR_FIELD = "error"
        private const val API_RESPONSE_PRICE_FIELD = "price"
        private const val API_RESPONSE_WEBSITE_FIELD = "website"
    }

    @Transactional
    fun createTicker(ticker: Ticker): Int {
        check(!tickerRepository.existsBySymbol(ticker.symbol)) {
            "Ticker with symbol ${ticker.symbol} already exists"
        }

        val newTicker = tickerRepository.save(ticker)

        logger.info("$newTicker created successfully")

        return newTicker.id!!
    }

    @Transactional
    fun updateTicker(updatedTicker: Ticker) {
        val tickerFromDatabase = tickerRepository.findByIdOrThrow(updatedTicker.id!!)

        tickerFromDatabase.apply {
            name = updatedTicker.name
            symbol = updatedTicker.symbol
            type = updatedTicker.type
            currentUnitValue = updatedTicker.currentUnitValue
            currentQuantity = updatedTicker.currentQuantity
            averageUnitValue = updatedTicker.averageUnitValue
        }

        logger.info("$tickerFromDatabase updated successfully")
    }

    @Transactional
    fun deleteTicker(id: Int) {
        val ticker = tickerRepository.findByIdOrThrow(id)

        check(getTransactionCountByTicker(id) == 0) {
            "Ticker with id $id has transactions associated with it and cannot be deleted. " +
                "Remove the transactions first or archive the ticker"
        }

        tickerRepository.delete(ticker)

        logger.info("$ticker deleted successfully")
    }

    @Transactional
    fun unarchiveTicker(id: Int) {
        val ticker = tickerRepository.findByIdOrThrow(id)

        ticker.apply {
            isArchived = false
        }

        logger.info("$ticker unarchived successfully")
    }

    @Transactional
    fun createTickerPurchase(
        tickerPurchase: TickerPurchase,
        walletTransactionContextDTO: WalletTransactionContextDTO,
    ) {
        val tickerFromDatabase = tickerRepository.findByIdOrThrow(tickerPurchase.ticker.id!!)

        val amount = tickerPurchase.unitPrice.multiply(tickerPurchase.quantity)

        val walletTransactionId =
            walletService.createWalletTransaction(
                WalletTransaction.from(walletTransactionContextDTO, WalletTransactionType.EXPENSE, amount),
            )

        val walletTransaction = walletService.getWalletTransactionById(walletTransactionId)

        tickerPurchase.apply { this.walletTransaction = walletTransaction }

        updateTickerAfterPurchase(tickerFromDatabase, tickerPurchase)

        val newPurchase = tickerPurchaseRepository.save(tickerPurchase)

        logger.info("$newPurchase created successfully")
    }

    @Transactional
    fun updateTickerPurchase(updatedPurchase: TickerPurchase) {
        val tickerPurchaseFromDatabase = tickerPurchaseRepository.findByIdOrThrow(updatedPurchase.id!!)

        check(tickerRepository.existsById(updatedPurchase.ticker.id!!)) {
            "${updatedPurchase.ticker} not found and cannot update purchase"
        }

        tickerPurchaseFromDatabase.apply {
            quantity = updatedPurchase.quantity
            unitPrice = updatedPurchase.unitPrice
        }

        val walletTransaction = updatedPurchase.walletTransaction!!
        walletTransaction.amount = updatedPurchase.unitPrice.multiply(updatedPurchase.quantity)

        walletService.updateWalletTransaction(walletTransaction)

        logger.info("$tickerPurchaseFromDatabase updated successfully")
    }

    @Transactional
    fun deleteTickerPurchase(purchaseId: Int) {
        val purchaseFromDatabase = tickerPurchaseRepository.findByIdOrThrow(purchaseId)

        tickerPurchaseRepository.delete(purchaseFromDatabase)
        walletService.deleteWalletTransaction(purchaseFromDatabase.walletTransaction!!.id!!)

        logger.info("$purchaseFromDatabase deleted successfully")
    }

    @Transactional
    fun createTickerSale(
        tickerSale: TickerSale,
        walletTransactionContextDTO: WalletTransactionContextDTO,
    ) {
        val tickerFromDatabase = tickerRepository.findByIdOrThrow(tickerSale.ticker.id!!)

        check(tickerSale.quantity <= tickerFromDatabase.currentQuantity) {
            "Quantity must be less than or equal to the available quantity"
        }

        val amount = tickerSale.unitPrice.multiply(tickerSale.quantity)

        val walletTransactionId =
            walletService.createWalletTransaction(
                WalletTransaction.from(walletTransactionContextDTO, WalletTransactionType.INCOME, amount),
            )

        val walletTransaction = walletService.getWalletTransactionById(walletTransactionId)

        tickerSale.apply { this.walletTransaction = walletTransaction }

        tickerSaleRepository.save(tickerSale)

        tickerFromDatabase.currentQuantity = tickerFromDatabase.currentQuantity.subtract(tickerSale.quantity)

        logger.info("$tickerSale created successfully")
    }

    @Transactional
    fun updateSale(updatedSale: TickerSale) {
        val tickerSaleFromDatabase = tickerSaleRepository.findByIdOrThrow(updatedSale.id!!)

        check(tickerRepository.existsById(updatedSale.ticker.id!!)) {
            "Ticker with id ${updatedSale.ticker.id} not found and cannot update sale"
        }

        tickerSaleFromDatabase.apply {
            quantity = updatedSale.quantity
            unitPrice = updatedSale.unitPrice
        }

        val walletTransaction = updatedSale.walletTransaction!!

        walletTransaction.amount = updatedSale.unitPrice.multiply(updatedSale.quantity)

        walletService.updateWalletTransaction(walletTransaction)

        logger.info("$tickerSaleFromDatabase updated successfully")
    }

    @Transactional
    fun deleteTickerSale(saleId: Int) {
        val saleFromDatabase = tickerSaleRepository.findByIdOrThrow(saleId)

        tickerSaleRepository.delete(saleFromDatabase)
        walletService.deleteWalletTransaction(saleFromDatabase.walletTransaction!!.id!!)

        logger.info("$saleFromDatabase deleted successfully")
    }

    @Transactional
    fun createDividend(
        dividend: Dividend,
        amount: BigDecimal,
        walletTransactionContextDTO: WalletTransactionContextDTO,
    ) {
        check(tickerRepository.existsById(dividend.ticker.id!!)) {
            "${dividend.ticker} not found and cannot create dividend"
        }

        check(amount > BigDecimal.ZERO) {
            "Amount must be greater than zero"
        }

        val walletTransactionId =
            walletService.createWalletTransaction(
                WalletTransaction.from(walletTransactionContextDTO, WalletTransactionType.INCOME, amount),
            )

        val walletTransaction = walletService.getWalletTransactionById(walletTransactionId)

        dividend.apply { this.walletTransaction = walletTransaction }

        val newDividend = dividendRepository.save(dividend)

        logger.info("$newDividend created successfully")
    }

    @Transactional
    fun updateDividend(updatedDividend: Dividend) {
        val dividendFromDatabase = dividendRepository.findByIdOrThrow(updatedDividend.id!!)

        check(tickerRepository.existsById(updatedDividend.ticker.id!!)) {
            "${updatedDividend.ticker} not found and cannot update dividend"
        }

        dividendFromDatabase.apply {
            ticker = updatedDividend.ticker
        }

        walletService.updateWalletTransaction(updatedDividend.walletTransaction!!)

        logger.info("$dividendFromDatabase updated successfully")
    }

    @Transactional
    fun deleteDividend(dividendId: Int) {
        val dividendFromDatabase = dividendRepository.findByIdOrThrow(dividendId)

        dividendRepository.delete(dividendFromDatabase)
        walletService.deleteWalletTransaction(dividendFromDatabase.walletTransaction!!.id!!)

        logger.info("$dividendFromDatabase deleted successfully")
    }

    @Transactional
    fun createCryptoExchange(cryptoExchange: CryptoExchange) {
        val sourcerTickerFromDatabase = tickerRepository.findByIdOrThrow(cryptoExchange.soldCrypto.id!!)
        val targetTickerFromDatabase = tickerRepository.findByIdOrThrow(cryptoExchange.receivedCrypto.id!!)

        check(cryptoExchange.soldQuantity <= sourcerTickerFromDatabase.currentQuantity) {
            "Source quantity must be less than or equal to the current quantity"
        }

        sourcerTickerFromDatabase.currentQuantity = sourcerTickerFromDatabase.currentQuantity.subtract(cryptoExchange.soldQuantity)
        targetTickerFromDatabase.currentQuantity = targetTickerFromDatabase.currentQuantity.add(cryptoExchange.receivedQuantity)

        val newCryptoExchange = cryptoExchangeRepository.save(cryptoExchange)

        logger.info("$newCryptoExchange created successfully")
    }

    @Transactional
    fun updateCryptoExchange(updatedCryptoExchange: CryptoExchange) {
        val cryptoExchangeFromDatabase = cryptoExchangeRepository.findByIdOrThrow(updatedCryptoExchange.id!!)

        check(tickerRepository.existsById(updatedCryptoExchange.soldCrypto.id!!)) {
            "Source ${updatedCryptoExchange.soldCrypto} not found and cannot update crypto exchange"
        }

        check(tickerRepository.existsById(updatedCryptoExchange.receivedCrypto.id!!)) {
            "Target ${updatedCryptoExchange.receivedCrypto} not found and cannot update crypto exchange"
        }

        changeSoldCrypto(cryptoExchangeFromDatabase, updatedCryptoExchange.soldCrypto)
        changeReceivedCrypto(cryptoExchangeFromDatabase, updatedCryptoExchange.receivedCrypto)
        changeSoldQuantity(cryptoExchangeFromDatabase, updatedCryptoExchange.soldQuantity)
        changeReceivedQuantity(cryptoExchangeFromDatabase, updatedCryptoExchange.receivedQuantity)

        cryptoExchangeFromDatabase.date = updatedCryptoExchange.date
        cryptoExchangeFromDatabase.description = updatedCryptoExchange.description

        logger.info("$cryptoExchangeFromDatabase updated successfully")
    }

    @Transactional
    fun deleteCryptoExchange(exchangeId: Int) {
        val cryptoExchangeFromDatabase = cryptoExchangeRepository.findByIdOrThrow(exchangeId)

        cryptoExchangeFromDatabase.soldCrypto.currentQuantity =
            cryptoExchangeFromDatabase.soldCrypto.currentQuantity.add(cryptoExchangeFromDatabase.soldQuantity)

        cryptoExchangeFromDatabase.receivedCrypto.currentQuantity =
            cryptoExchangeFromDatabase.receivedCrypto.currentQuantity.subtract(cryptoExchangeFromDatabase.receivedQuantity)

        tickerRepository.save(cryptoExchangeFromDatabase.soldCrypto)
        tickerRepository.save(cryptoExchangeFromDatabase.receivedCrypto)

        cryptoExchangeRepository.delete(cryptoExchangeFromDatabase)

        logger.info("$cryptoExchangeFromDatabase deleted successfully")
    }

    @Transactional
    suspend fun updateAllNonArchivedTickersPrices() {
        logger.info("Starting update of all non-archived ticker prices")

        val activeTickers = tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc()

        if (activeTickers.isEmpty()) {
            logger.info("No active tickers found, skipping price update")
            return
        }

        logger.info("Updating prices for {} active tickers", activeTickers.size)

        val failed = updateTickersPriceFromApi(activeTickers)

        if (failed.isEmpty()) {
            logger.info("All ticker prices updated successfully")
        } else {
            logger.warn(
                "Failed to update {} ticker prices: {}",
                failed.size,
                failed.joinToString(", ") { it.symbol },
            )
        }
    }

    @Transactional
    suspend fun updateTickersPriceFromApi(tickers: List<Ticker>): List<Ticker> {
        if (tickers.isEmpty()) return emptyList()

        val symbols = tickers.map { it.symbol }

        logger.info("Fetching prices for {} tickers", symbols.size)

        val jsonObject =
            retry(
                config = RetryConfig.TICKER_PRICE,
                logger = logger,
                operationName = "Fetch ticker prices",
            ) {
                APIUtils.fetchStockPrices(symbols)
            }

        val updatedTickers = mutableListOf<Ticker>()
        val failedTickers = mutableListOf<Ticker>()

        tickers.forEach { ticker ->
            runCatching {
                val tickerData =
                    jsonObject.optJSONObject(ticker.symbol)
                        ?: error("API response does not contain data for ${ticker.symbol}")

                if (tickerData.has(API_RESPONSE_ERROR_FIELD)) {
                    error(tickerData.getString(API_RESPONSE_ERROR_FIELD))
                }

                ticker.currentUnitValue =
                    tickerData.getBigDecimal(API_RESPONSE_PRICE_FIELD)

                ticker.lastUpdate = LocalDateTime.now()

                if (tickerData.has(API_RESPONSE_WEBSITE_FIELD)) {
                    ticker.domain = tickerData.getString(API_RESPONSE_WEBSITE_FIELD)
                }

                updatedTickers.add(ticker)
            }.onFailure { e ->
                logger.warn(
                    "Failed to update ticker {}: {}",
                    ticker.symbol,
                    e.message,
                )
                failedTickers.add(ticker)
            }
        }

        if (updatedTickers.isNotEmpty()) {
            tickerRepository.saveAll(updatedTickers)
        }

        downloadMissingLogosFromApi(updatedTickers)

        return failedTickers
    }

    fun getAllTickers() = tickerRepository.findAllByOrderBySymbolAsc()

    fun getAllArchivedTickers() = tickerRepository.findAllByIsArchivedTrueOrderBySymbolAsc()

    fun getAllNonArchivedTickers() = tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc()

    fun getAllNonArchivedTickersByType(type: AssetType) = tickerRepository.findAllByTypeAndIsArchivedFalseOrderBySymbolAsc(type)

    fun getAllPurchases() = tickerPurchaseRepository.findAll().toList()

    fun getAllSales() = tickerSaleRepository.findAll().toList()

    fun getAllNonArchivedSales() = tickerSaleRepository.findAllNonArchived()

    fun getAllPurchasesByTicker(tickerId: Int) = tickerPurchaseRepository.findAllByTickerId(tickerId)

    fun getAllSalesByTicker(tickerId: Int) = tickerSaleRepository.findAllByTickerId(tickerId)

    fun getAllDividends() = dividendRepository.findAll()

    fun getAllCryptoExchanges() = cryptoExchangeRepository.findAll()

    fun getTransactionCountByTicker(tickerId: Int): Int =
        getPurchaseCountByTicker(tickerId) +
            getSaleCountByTicker(tickerId) +
            getDividendCountByTicker(tickerId) +
            getCryptoExchangeCountByTicker(tickerId)

    fun getPurchaseCountByTicker(tickerId: Int): Int = tickerRepository.getPurchaseCountByTicker(tickerId)

    fun getSaleCountByTicker(tickerId: Int): Int = tickerRepository.getSaleCountByTicker(tickerId)

    fun getDividendCountByTicker(tickerId: Int): Int = tickerRepository.getDividendCountByTicker(tickerId)

    fun getCryptoExchangeCountByTicker(tickerId: Int): Int = tickerRepository.getCryptoExchangeCountByTicker(tickerId)

    private fun isLogoPresent(domain: String?): Boolean =
        domain?.takeIf { it.isNotBlank() }?.let {
            runCatching {
                val filename =
                    it
                        .replace("https://", "")
                        .replace("http://", "")
                        .replace("www.", "")
                        .substringBefore("/") + ".png"

                val logoPath = FileUtils.getPath(Constants.LOGOS_DIR, filename)
                FileUtils.exists(logoPath)
            }.getOrDefault(false)
        } ?: false

    private suspend fun downloadMissingLogosFromApi(tickers: List<Ticker>) {
        val websitesToDownload =
            tickers
                .asSequence()
                .filterNot { it.domain.isNullOrEmpty() }
                .map { it.domain!! }
                .filterNot { isLogoPresent(it) }
                .distinct()
                .toList()

        if (websitesToDownload.isEmpty()) {
            logger.info("All logos already cached, skipping download")
            return
        }

        logger.info("Downloading {} missing logos", websitesToDownload.size)

        val jsonObject =
            retry(
                config = RetryConfig.TICKER_LOGO,
                logger = logger,
                operationName = "Download logos",
            ) {
                APIUtils.fetchStockLogos(websitesToDownload)
            }

        val results =
            websitesToDownload.mapNotNull { website ->
                val logoData = jsonObject.optJSONObject(website) ?: return@mapNotNull null

                when {
                    logoData.has(API_RESPONSE_LOGO_PATH_FIELD) -> {
                        logger.debug("Logo downloaded for {}", website)
                        true
                    }
                    logoData.has(API_RESPONSE_ERROR_FIELD) -> {
                        logger.debug(
                            "Failed to download logo for {}: {}",
                            website,
                            logoData.getString(API_RESPONSE_ERROR_FIELD),
                        )
                        false
                    }
                    else -> null
                }
            }

        val (success, fail) = results.partition { it }

        logger.info(
            "Logo download complete: {} successful, {} failed",
            success.size,
            fail.size,
        )
    }

    private fun updateTickerAfterPurchase(
        ticker: Ticker,
        tickerPurchase: TickerPurchase,
    ) {
        ticker.currentQuantity = ticker.currentQuantity.add(tickerPurchase.quantity)
        val totalQuantity = ticker.averageUnitValueCount.add(tickerPurchase.quantity)

        if (ticker.averageUnitValueCount.isZero()) {
            ticker.averageUnitValue = tickerPurchase.unitPrice
        } else {
            val currentTotalValue = ticker.averageUnitValue.multiply(ticker.averageUnitValueCount)
            val newTotalValue = tickerPurchase.unitPrice.multiply(tickerPurchase.quantity)

            ticker.averageUnitValue =
                currentTotalValue
                    .add(newTotalValue)
                    .divide(totalQuantity, 2, RoundingMode.HALF_UP)
        }

        ticker.averageUnitValueCount = totalQuantity
    }

    private fun changeSoldQuantity(
        oldExchange: CryptoExchange,
        soldQuantity: BigDecimal,
    ) {
        oldExchange.soldCrypto.currentQuantity =
            oldExchange.soldCrypto.currentQuantity.add(oldExchange.soldQuantity.subtract(soldQuantity))

        oldExchange.soldQuantity = soldQuantity
    }

    private fun changeReceivedQuantity(
        oldExchange: CryptoExchange,
        receivedQuantity: BigDecimal,
    ) {
        oldExchange.receivedCrypto.currentQuantity =
            oldExchange.receivedCrypto.currentQuantity.subtract(
                oldExchange.receivedQuantity.subtract(receivedQuantity),
            )

        oldExchange.receivedQuantity = receivedQuantity
    }

    private fun changeSoldCrypto(
        oldExchange: CryptoExchange,
        soldCrypto: Ticker,
    ) {
        if (oldExchange.soldCrypto.id == soldCrypto.id) {
            return
        }

        oldExchange.soldCrypto.currentQuantity =
            oldExchange.soldCrypto.currentQuantity.add(oldExchange.soldQuantity)

        soldCrypto.currentQuantity = soldCrypto.currentQuantity.subtract(oldExchange.soldQuantity)

        oldExchange.soldCrypto = soldCrypto
    }

    private fun changeReceivedCrypto(
        oldExchange: CryptoExchange,
        receivedCrypto: Ticker,
    ) {
        if (oldExchange.receivedCrypto.id == receivedCrypto.id) {
            return
        }

        oldExchange.receivedCrypto.currentQuantity =
            oldExchange.receivedCrypto.currentQuantity.subtract(oldExchange.receivedQuantity)

        receivedCrypto.currentQuantity = receivedCrypto.currentQuantity.add(oldExchange.receivedQuantity)

        oldExchange.receivedCrypto = receivedCrypto
    }
}
