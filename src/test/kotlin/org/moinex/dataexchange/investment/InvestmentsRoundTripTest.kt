/*
 * Filename: InvestmentsRoundTripTest.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.moinex.common.util.FxUtils
import org.moinex.dataexchange.export.ExportService
import org.moinex.dataexchange.imports.ImportExecutor
import org.moinex.dataexchange.imports.ImportPlanner
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.dataexchange.wallet.WalletTestDatabase
import org.moinex.model.Category
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.BondType
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.CryptoExchange
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.InvestmentTarget
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletType
import org.moinex.repository.CategoryRepository
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.InvestmentTargetRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.CategoryService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime

class InvestmentsRoundTripTest :
    BehaviorSpec({
        beforeSpec {
            mockkObject(FxUtils)
            every { FxUtils.launchOnFxThread(any()) } returns mockk<Job>(relaxed = true)
        }

        afterSpec { unmockkObject(FxUtils) }

        fun seed(db: WalletTestDatabase) {
            val type = db.get<WalletTypeRepository>().save(WalletType(name = "Broker"))
            db.get<CategoryService>().createCategory(Category(name = "Investments"))
            db.get<WalletService>().createWallet(Wallet(type = type, name = "Broker", balance = BigDecimal("10000.00")))
            val wallet = db.get<WalletRepository>().findByName("Broker").get()
            val category = db.get<CategoryRepository>().findByName("Investments")!!
            val tickers = db.get<TickerService>()
            val created = LocalDateTime.of(2026, 1, 2, 3, 4, 5)

            fun ticker(
                symbol: String,
                name: String,
                kind: AssetType,
                price: String,
            ) = tickers.createTicker(
                Ticker(
                    lastUpdate = created,
                    createdAt = created,
                    domain = "$symbol.example",
                    name = name,
                    symbol = symbol,
                    type = kind,
                    currentQuantity = BigDecimal.ZERO,
                    currentUnitValue = BigDecimal(price),
                    averageUnitValue = BigDecimal.ZERO,
                ),
            )
            ticker("ACME3", "Acme Corp", AssetType.STOCK, "15.40")
            ticker("BTC", "Bitcoin", AssetType.CRYPTOCURRENCY, "100.00")
            ticker("ETH", "Ether", AssetType.CRYPTOCURRENCY, "10.00")
            val repo = db.get<TickerRepository>()

            fun context(
                day: Int,
                description: String?,
            ) = WalletTransactionContextDTO(
                wallet,
                LocalDateTime.of(2026, 3, day, 10, 0, 0),
                category,
                description,
                WalletTransactionStatus.CONFIRMED,
                true,
            )
            tickers.createTickerPurchase(
                TickerPurchase(
                    ticker = repo.findBySymbol("ACME3")!!,
                    quantity = BigDecimal("10"),
                    unitPrice = BigDecimal("12.50"),
                ),
                context(1, "buy acme"),
            )
            tickers.createTickerPurchase(
                TickerPurchase(
                    ticker = repo.findBySymbol("ACME3")!!,
                    quantity = BigDecimal("5"),
                    unitPrice = BigDecimal("13.00"),
                ),
                context(2, "buy acme"),
            )
            tickers.createTickerSale(
                TickerSale(
                    ticker = repo.findBySymbol("ACME3")!!,
                    averageCost = BigDecimal("12.6667"),
                    quantity = BigDecimal("4"),
                    unitPrice = BigDecimal("15.00"),
                ),
                context(3, "sell acme"),
            )
            tickers.createDividend(
                Dividend(ticker = repo.findBySymbol("ACME3")!!),
                BigDecimal("3.25"),
                context(4, "dividend"),
            )
            tickers.createTickerPurchase(
                TickerPurchase(
                    ticker = repo.findBySymbol("BTC")!!,
                    quantity = BigDecimal("1.5"),
                    unitPrice = BigDecimal("100.00"),
                ),
                context(5, null),
            )
            tickers.createCryptoExchange(
                CryptoExchange(
                    soldCrypto = repo.findBySymbol("BTC")!!,
                    receivedCrypto = repo.findBySymbol("ETH")!!,
                    soldQuantity = BigDecimal("0.50"),
                    receivedQuantity = BigDecimal("8.00"),
                    date = LocalDateTime.of(2026, 3, 6, 9, 0, 0),
                    description = "swap",
                ),
            )
            val bonds = db.get<BondService>()
            bonds.createBond(
                Bond(
                    name = "CDB Bank 120%",
                    symbol = "CDB1",
                    type = BondType.CDB,
                    issuer = "Bank",
                    maturityDate = LocalDate.of(2029, 5, 1),
                    interestType = InterestType.FLOATING,
                    interestIndex = InterestIndex.CDI,
                    interestRate = BigDecimal("120.00"),
                ),
            )
            val bond = db.get<BondRepository>().findAll().first()
            bonds.createBondOperation(
                BondOperation(
                    bond = bond,
                    operationType = OperationType.BUY,
                    quantity = BigDecimal("10"),
                    unitPrice = BigDecimal("100.50"),
                    fees = BigDecimal("1.00"),
                    taxes = BigDecimal("0.50"),
                ),
                context(7, "cdb buy"),
            )
            db.get<InvestmentTargetRepository>().save(
                InvestmentTarget(assetType = AssetType.STOCK, targetPercentage = BigDecimal("60.00")),
            )
        }

        fun snapshot(db: WalletTestDatabase): Map<String, Any> =
            mapOf(
                "wallet" to db.get<WalletRepository>().findAll().map { listOf(it.name, it.balance.toPlainString()) },
                "tickers" to
                    db.get<TickerRepository>().findAll().sortedBy { it.symbol }.map {
                        listOf(
                            it.symbol,
                            it.name,
                            it.type,
                            it.currentQuantity.stripTrailingZeros(),
                            it.averageUnitValue.stripTrailingZeros(),
                            it.averageUnitValueCount.stripTrailingZeros(),
                            it.currentUnitValue.toPlainString(),
                            it.lastUpdate,
                            it.createdAt,
                            it.isArchived,
                            it.domain,
                        )
                    },
                "purchases" to
                    db
                        .get<TickerPurchaseRepository>()
                        .findAll()
                        .map {
                            listOf(
                                it.ticker.symbol,
                                it.quantity.stripTrailingZeros(),
                                it.unitPrice.toPlainString(),
                                it.walletTransaction!!.date,
                                it.walletTransaction!!.amount.toPlainString(),
                            )
                        }.sortedBy { it.joinToString("|") },
                "sales" to
                    db.get<TickerSaleRepository>().findAll().map {
                        listOf(
                            it.ticker.symbol,
                            it.quantity.stripTrailingZeros(),
                            it.unitPrice.toPlainString(),
                            it.averageCost.stripTrailingZeros(),
                            it.walletTransaction!!.date,
                        )
                    },
                "dividends" to
                    db.get<DividendRepository>().findAll().map {
                        listOf(
                            it.ticker.symbol,
                            it.walletTransaction!!.amount.toPlainString(),
                            it.walletTransaction!!.date,
                        )
                    },
                "exchanges" to
                    db.get<CryptoExchangeRepository>().findAll().map {
                        listOf(
                            it.soldCrypto.symbol,
                            it.receivedCrypto.symbol,
                            it.soldQuantity.toPlainString(),
                            it.receivedQuantity.toPlainString(),
                            it.date,
                            it.description,
                        )
                    },
                "bonds" to
                    db.get<BondRepository>().findAll().map {
                        listOf(
                            it.name,
                            it.symbol,
                            it.type,
                            it.issuer,
                            it.maturityDate,
                            it.interestType,
                            it.interestIndex,
                            it.interestRate?.toPlainString(),
                            it.archived,
                        )
                    },
                "bondOperations" to
                    db.get<BondOperationRepository>().findAll().map {
                        listOf(
                            it.bond.name,
                            it.operationType,
                            it.quantity.toPlainString(),
                            it.unitPrice.toPlainString(),
                            it.fees.toPlainString(),
                            it.taxes.toPlainString(),
                            it.walletTransaction!!.amount.toPlainString(),
                        )
                    },
                "targets" to
                    db.get<InvestmentTargetRepository>().findAll().map {
                        listOf(it.assetType, it.targetPercentage.toPlainString(), it.isActive)
                    },
                "transactions" to
                    db
                        .get<WalletTransactionRepository>()
                        .findAll()
                        .map {
                            listOf(it.wallet.name, it.type, it.date, it.amount.toPlainString(), it.description)
                        }.sortedBy { it.joinToString("|") },
            )

        listOf(ExportFormat.XLSX, ExportFormat.CSV_ZIP).forEach { format ->
            Given(
                "a database with positions, trades, dividends, an exchange, a bond and a target exported as $format",
            ) {
                WalletTestDatabase().use { a ->
                    seed(a)
                    val expected = snapshot(a)
                    val file = Files.createTempFile("invest", if (format == ExportFormat.XLSX) ".xlsx" else ".zip")
                    runBlocking { a.get<ExportService>().export(a.get<ExportService>().counts().keys, format, file) }

                    When("importing into an empty database") {
                        WalletTestDatabase().use { b ->
                            val plan = runBlocking { b.get<ImportPlanner>().plan(file) }
                            (plan.fileProblems + plan.entities.flatMap { it.problems }) shouldBe emptyList()
                            b.get<ImportExecutor>().execute(plan)

                            Then("positions, trades, links to wallet transactions and balances are identical") {
                                snapshot(b) shouldBe expected
                            }

                            Then("importing again adds nothing") {
                                val second = runBlocking { b.get<ImportPlanner>().plan(file) }
                                second.hasErrors shouldBe false
                                second.entities.sumOf { it.toAdd } shouldBe 0
                            }
                        }
                    }
                }
            }
        }
    })
