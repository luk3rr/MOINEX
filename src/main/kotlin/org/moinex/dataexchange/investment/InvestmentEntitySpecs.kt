/*
 * Filename: InvestmentEntitySpecs.kt
 * Created on: November 13, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.investment

import org.moinex.common.extension.toRounded
import org.moinex.dataexchange.spec.ApplyContext
import org.moinex.dataexchange.spec.CellType
import org.moinex.dataexchange.spec.Cols
import org.moinex.dataexchange.spec.Column
import org.moinex.dataexchange.spec.EntitySpec
import org.moinex.dataexchange.spec.NaturalKeys
import org.moinex.dataexchange.spec.ParsedRow
import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.spec.ResolveContext
import org.moinex.dataexchange.wallet.WalletEntities
import org.moinex.model.enums.AssetType
import org.moinex.model.enums.BondType
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.CryptoExchange
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.InvestmentTarget
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.InvestmentTargetRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

object InvestmentEntities {
    const val GROUP = "investments"
    const val TICKERS = "tickers"
    const val PURCHASES = "ticker_purchases"
    const val SALES = "ticker_sales"
    const val DIVIDENDS = "dividends"
    const val CRYPTO_EXCHANGES = "crypto_exchanges"
    const val BONDS = "bonds"
    const val BOND_OPERATIONS = "bond_operations"
    const val TARGETS = "investment_targets"

    fun exact(value: BigDecimal?): String? =
        value?.let {
            if (it.signum() ==
                0
            ) {
                "0"
            } else {
                it.stripTrailingZeros().toPlainString()
            }
        }

    val transactionColumns =
        listOf(
            Column(Cols.WALLET, CellType.TEXT, required = true),
            Column(Cols.DATE, CellType.DATETIME, required = true),
            Column(Cols.TRANSACTION_AMOUNT, CellType.DECIMAL, required = true),
            Column(Cols.DESCRIPTION, CellType.TEXT),
        )

    fun transactionCells(tx: WalletTransaction): Map<String, Any?> =
        mapOf(
            Cols.WALLET to tx.wallet.name,
            Cols.DATE to tx.date,
            Cols.TRANSACTION_AMOUNT to tx.amount,
            Cols.DESCRIPTION to tx.description,
        )

    fun transactionKey(
        row: ParsedRow,
        type: WalletTransactionType,
    ) = WalletEntities.transactionKey(
        row.text(Cols.WALLET),
        row.dateTime(Cols.DATE),
        type,
        row.decimal(Cols.TRANSACTION_AMOUNT),
        row.text(Cols.DESCRIPTION),
    )

    fun checkTransaction(
        entity: String,
        row: ParsedRow,
        type: WalletTransactionType,
        ctx: ResolveContext,
    ): List<Problem> =
        if (ctx.exists(WalletEntities.TRANSACTIONS, transactionKey(row, type))) {
            emptyList()
        } else {
            listOf(WalletEntities.error(entity, row, Cols.WALLET, ProblemCodes.UNRESOLVED_REFERENCE, "transaction"))
        }

    fun nonNegative(
        entity: String,
        row: ParsedRow,
        vararg columns: String,
    ): List<Problem> =
        columns.mapNotNull { column ->
            val value = row.decimal(column)
            if (value != null && value < BigDecimal.ZERO) {
                WalletEntities.error(entity, row, column, ProblemCodes.DOMAIN_RULE, "negative")
            } else {
                null
            }
        }

    fun positive(
        entity: String,
        row: ParsedRow,
        vararg columns: String,
    ): List<Problem> =
        columns.mapNotNull { column ->
            val value = row.decimal(column)
            if (value != null && value.signum() <= 0) {
                WalletEntities.error(entity, row, column, ProblemCodes.DOMAIN_RULE, "positive")
            } else {
                null
            }
        }
}

@Component
class TransactionLinker(
    private val repository: WalletTransactionRepository,
) {
    fun take(
        ctx: ApplyContext,
        key: String,
    ): WalletTransaction {
        val index =
            ctx.memo("wallet_transaction_index") {
                val map = mutableMapOf<String, MutableList<WalletTransaction>>()
                repository.findAll().sortedBy { it.id }.forEach {
                    map
                        .getOrPut(
                            WalletEntities.transactionKey(it.wallet.name, it.date, it.type, it.amount, it.description),
                        ) {
                            mutableListOf()
                        }.add(it)
                }
                map
            }
        val used = ctx.memo("wallet_transaction_used") { mutableMapOf<String, Int>() }
        val candidates = index.getValue(key)
        val position = used.getOrDefault(key, 0)
        used[key] = position + 1
        return candidates[position % candidates.size]
    }
}

@Component
class TickerSpec(
    private val repository: TickerRepository,
    private val service: TickerService,
) : EntitySpec {
    override val id = InvestmentEntities.TICKERS
    override val group = InvestmentEntities.GROUP
    override val columns =
        listOf(
            Column(Cols.SYMBOL, CellType.TEXT, required = true),
            Column(Cols.NAME, CellType.TEXT, required = true),
            Column(Cols.TYPE, CellType.ENUM, required = true, enumClass = AssetType::class.java),
            Column(Cols.CURRENT_QUANTITY, CellType.DECIMAL, required = true),
            Column(Cols.AVERAGE_UNIT_VALUE, CellType.DECIMAL, required = true),
            Column(Cols.AVERAGE_UNIT_VALUE_COUNT, CellType.DECIMAL),
            Column(Cols.CURRENT_UNIT_VALUE, CellType.DECIMAL, required = true),
            Column(Cols.LAST_UPDATE, CellType.DATETIME, required = true),
            Column(Cols.CREATED_AT, CellType.DATETIME, required = true),
            Column(Cols.ARCHIVED, CellType.BOOLEAN),
            Column(Cols.DOMAIN, CellType.TEXT),
        )

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedBy { it.symbol }.asSequence().map {
            mapOf(
                Cols.SYMBOL to it.symbol,
                Cols.NAME to it.name,
                Cols.TYPE to it.type,
                Cols.CURRENT_QUANTITY to it.currentQuantity,
                Cols.AVERAGE_UNIT_VALUE to it.averageUnitValue,
                Cols.AVERAGE_UNIT_VALUE_COUNT to it.averageUnitValueCount,
                Cols.CURRENT_UNIT_VALUE to it.currentUnitValue,
                Cols.LAST_UPDATE to it.lastUpdate,
                Cols.CREATED_AT to it.createdAt,
                Cols.ARCHIVED to it.isArchived,
                Cols.DOMAIN to it.domain,
            )
        }

    override fun naturalKey(row: ParsedRow) = row.text(Cols.SYMBOL)!!.trim().uppercase()

    override fun existingKeys(): Map<String, Int> = repository.findAll().groupingBy { it.symbol }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        InvestmentEntities.nonNegative(
            id,
            row,
            Cols.CURRENT_QUANTITY,
            Cols.AVERAGE_UNIT_VALUE,
            Cols.AVERAGE_UNIT_VALUE_COUNT,
            Cols.CURRENT_UNIT_VALUE,
        ) +
            if (row.text(Cols.SYMBOL)!!.isBlank() || row.text(Cols.NAME)!!.isBlank()) {
                listOf(WalletEntities.error(id, row, Cols.SYMBOL, ProblemCodes.DOMAIN_RULE, "empty"))
            } else {
                emptyList()
            }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        service.createTicker(
            Ticker(
                lastUpdate = row.dateTime(Cols.LAST_UPDATE)!!,
                createdAt = row.dateTime(Cols.CREATED_AT)!!,
                isArchived = row.bool(Cols.ARCHIVED) ?: false,
                domain = row.text(Cols.DOMAIN),
                name = row.text(Cols.NAME)!!,
                symbol = row.text(Cols.SYMBOL)!!,
                type = row.enum<AssetType>(Cols.TYPE)!!,
                currentQuantity = row.decimal(Cols.CURRENT_QUANTITY)!!,
                currentUnitValue = row.decimal(Cols.CURRENT_UNIT_VALUE)!!,
                averageUnitValue = row.decimal(Cols.AVERAGE_UNIT_VALUE)!!,
                averageUnitValueCount = row.decimal(Cols.AVERAGE_UNIT_VALUE_COUNT),
            ),
        )
    }
}

@Component
class TickerPurchaseSpec(
    private val repository: TickerPurchaseRepository,
    private val tickerRepository: TickerRepository,
    private val linker: TransactionLinker,
) : EntitySpec {
    override val id = InvestmentEntities.PURCHASES
    override val group = InvestmentEntities.GROUP
    override val dependsOn = listOf(InvestmentEntities.TICKERS, WalletEntities.TRANSACTIONS)
    override val columns =
        listOf(
            Column(Cols.TICKER, CellType.TEXT, required = true),
            Column(Cols.QUANTITY, CellType.DECIMAL, required = true),
            Column(Cols.UNIT_PRICE, CellType.DECIMAL, required = true),
        ) + InvestmentEntities.transactionColumns

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedWith(compareBy({ it.walletTransaction!!.date }, { it.id })).asSequence().map {
            mapOf(Cols.TICKER to it.ticker.symbol, Cols.QUANTITY to it.quantity, Cols.UNIT_PRICE to it.unitPrice) +
                InvestmentEntities.transactionCells(it.walletTransaction!!)
        }

    override fun naturalKey(row: ParsedRow) =
        NaturalKeys.of(
            row.text(Cols.TICKER)!!.trim().uppercase(),
            InvestmentEntities.exact(row.decimal(Cols.QUANTITY)),
            row.decimal(Cols.UNIT_PRICE),
            InvestmentEntities.transactionKey(row, WalletTransactionType.EXPENSE),
        )

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy {
                val tx = it.walletTransaction!!
                NaturalKeys.of(
                    it.ticker.symbol,
                    InvestmentEntities.exact(it.quantity),
                    it.unitPrice,
                    WalletEntities.transactionKey(tx.wallet.name, tx.date, tx.type, tx.amount, tx.description),
                )
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        WalletEntities.checkReference(id, row, Cols.TICKER, InvestmentEntities.TICKERS, ctx) +
            InvestmentEntities.positive(id, row, Cols.QUANTITY) +
            InvestmentEntities.nonNegative(id, row, Cols.UNIT_PRICE) +
            InvestmentEntities.checkTransaction(id, row, WalletTransactionType.EXPENSE, ctx)

    override fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> = rows.sortedBy { it.dateTime(Cols.DATE) }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        repository.save(
            TickerPurchase(
                ticker = tickerRepository.findBySymbol(row.text(Cols.TICKER)!!.trim().uppercase())!!,
                quantity = row.decimal(Cols.QUANTITY)!!,
                unitPrice = row.decimal(Cols.UNIT_PRICE)!!,
                walletTransaction =
                    linker.take(
                        ctx,
                        InvestmentEntities.transactionKey(row, WalletTransactionType.EXPENSE),
                    ),
            ),
        )
    }
}

@Component
class TickerSaleSpec(
    private val repository: TickerSaleRepository,
    private val tickerRepository: TickerRepository,
    private val linker: TransactionLinker,
) : EntitySpec {
    override val id = InvestmentEntities.SALES
    override val group = InvestmentEntities.GROUP
    override val dependsOn = listOf(InvestmentEntities.TICKERS, WalletEntities.TRANSACTIONS)
    override val columns =
        listOf(
            Column(Cols.TICKER, CellType.TEXT, required = true),
            Column(Cols.QUANTITY, CellType.DECIMAL, required = true),
            Column(Cols.UNIT_PRICE, CellType.DECIMAL, required = true),
            Column(Cols.AVERAGE_COST, CellType.DECIMAL, required = true),
        ) + InvestmentEntities.transactionColumns

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedWith(compareBy({ it.walletTransaction!!.date }, { it.id })).asSequence().map {
            mapOf(
                Cols.TICKER to it.ticker.symbol,
                Cols.QUANTITY to it.quantity,
                Cols.UNIT_PRICE to it.unitPrice,
                Cols.AVERAGE_COST to it.averageCost,
            ) + InvestmentEntities.transactionCells(it.walletTransaction!!)
        }

    override fun naturalKey(row: ParsedRow) =
        NaturalKeys.of(
            row.text(Cols.TICKER)!!.trim().uppercase(),
            InvestmentEntities.exact(row.decimal(Cols.QUANTITY)),
            row.decimal(Cols.UNIT_PRICE),
            InvestmentEntities.transactionKey(row, WalletTransactionType.INCOME),
        )

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy {
                val tx = it.walletTransaction!!
                NaturalKeys.of(
                    it.ticker.symbol,
                    InvestmentEntities.exact(it.quantity),
                    it.unitPrice,
                    WalletEntities.transactionKey(tx.wallet.name, tx.date, tx.type, tx.amount, tx.description),
                )
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        WalletEntities.checkReference(id, row, Cols.TICKER, InvestmentEntities.TICKERS, ctx) +
            InvestmentEntities.positive(id, row, Cols.QUANTITY) +
            InvestmentEntities.nonNegative(id, row, Cols.UNIT_PRICE, Cols.AVERAGE_COST) +
            InvestmentEntities.checkTransaction(id, row, WalletTransactionType.INCOME, ctx)

    override fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> = rows.sortedBy { it.dateTime(Cols.DATE) }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        repository.save(
            TickerSale(
                ticker = tickerRepository.findBySymbol(row.text(Cols.TICKER)!!.trim().uppercase())!!,
                averageCost = row.decimal(Cols.AVERAGE_COST)!!,
                quantity = row.decimal(Cols.QUANTITY)!!,
                unitPrice = row.decimal(Cols.UNIT_PRICE)!!,
                walletTransaction =
                    linker.take(
                        ctx,
                        InvestmentEntities.transactionKey(row, WalletTransactionType.INCOME),
                    ),
            ),
        )
    }
}

@Component
class DividendSpec(
    private val repository: DividendRepository,
    private val tickerRepository: TickerRepository,
    private val linker: TransactionLinker,
) : EntitySpec {
    override val id = InvestmentEntities.DIVIDENDS
    override val group = InvestmentEntities.GROUP
    override val dependsOn = listOf(InvestmentEntities.TICKERS, WalletEntities.TRANSACTIONS)
    override val columns =
        listOf(Column(Cols.TICKER, CellType.TEXT, required = true)) + InvestmentEntities.transactionColumns

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedWith(compareBy({ it.walletTransaction!!.date }, { it.id })).asSequence().map {
            mapOf(Cols.TICKER to it.ticker.symbol) + InvestmentEntities.transactionCells(it.walletTransaction!!)
        }

    override fun naturalKey(row: ParsedRow) =
        NaturalKeys.of(
            row.text(Cols.TICKER)!!.trim().uppercase(),
            InvestmentEntities.transactionKey(row, WalletTransactionType.INCOME),
        )

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy {
                val tx = it.walletTransaction!!
                NaturalKeys.of(
                    it.ticker.symbol,
                    WalletEntities.transactionKey(tx.wallet.name, tx.date, tx.type, tx.amount, tx.description),
                )
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        WalletEntities.checkReference(id, row, Cols.TICKER, InvestmentEntities.TICKERS, ctx) +
            InvestmentEntities.checkTransaction(id, row, WalletTransactionType.INCOME, ctx)

    override fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> = rows.sortedBy { it.dateTime(Cols.DATE) }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        repository.save(
            Dividend(
                ticker = tickerRepository.findBySymbol(row.text(Cols.TICKER)!!.trim().uppercase())!!,
                walletTransaction =
                    linker.take(
                        ctx,
                        InvestmentEntities.transactionKey(row, WalletTransactionType.INCOME),
                    ),
            ),
        )
    }
}

@Component
class CryptoExchangeSpec(
    private val repository: CryptoExchangeRepository,
    private val tickerRepository: TickerRepository,
) : EntitySpec {
    override val id = InvestmentEntities.CRYPTO_EXCHANGES
    override val group = InvestmentEntities.GROUP
    override val dependsOn = listOf(InvestmentEntities.TICKERS)
    override val columns =
        listOf(
            Column(Cols.SOLD_TICKER, CellType.TEXT, required = true),
            Column(Cols.RECEIVED_TICKER, CellType.TEXT, required = true),
            Column(Cols.SOLD_QUANTITY, CellType.DECIMAL, required = true),
            Column(Cols.RECEIVED_QUANTITY, CellType.DECIMAL, required = true),
            Column(Cols.DATE, CellType.DATETIME, required = true),
            Column(Cols.DESCRIPTION, CellType.TEXT),
        )

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedWith(compareBy({ it.date }, { it.id })).asSequence().map {
            mapOf(
                Cols.SOLD_TICKER to it.soldCrypto.symbol,
                Cols.RECEIVED_TICKER to it.receivedCrypto.symbol,
                Cols.SOLD_QUANTITY to it.soldQuantity,
                Cols.RECEIVED_QUANTITY to it.receivedQuantity,
                Cols.DATE to it.date,
                Cols.DESCRIPTION to it.description,
            )
        }

    private fun key(
        sold: String?,
        received: String?,
        soldQuantity: BigDecimal?,
        receivedQuantity: BigDecimal?,
        date: LocalDateTime?,
        description: String?,
    ) = NaturalKeys.of(
        sold?.trim()?.uppercase(),
        received?.trim()?.uppercase(),
        InvestmentEntities.exact(soldQuantity?.toRounded()),
        InvestmentEntities.exact(receivedQuantity?.toRounded()),
        date,
        description,
    )

    override fun naturalKey(row: ParsedRow) =
        key(
            row.text(Cols.SOLD_TICKER),
            row.text(Cols.RECEIVED_TICKER),
            row.decimal(Cols.SOLD_QUANTITY),
            row.decimal(Cols.RECEIVED_QUANTITY),
            row.dateTime(Cols.DATE),
            row.text(Cols.DESCRIPTION),
        )

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy {
                key(
                    it.soldCrypto.symbol,
                    it.receivedCrypto.symbol,
                    it.soldQuantity,
                    it.receivedQuantity,
                    it.date,
                    it.description,
                )
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        WalletEntities.checkReference(id, row, Cols.SOLD_TICKER, InvestmentEntities.TICKERS, ctx) +
            WalletEntities.checkReference(id, row, Cols.RECEIVED_TICKER, InvestmentEntities.TICKERS, ctx) +
            InvestmentEntities.positive(id, row, Cols.SOLD_QUANTITY, Cols.RECEIVED_QUANTITY) +
            if (row.text(Cols.SOLD_TICKER)?.trim()?.uppercase() ==
                row.text(Cols.RECEIVED_TICKER)?.trim()?.uppercase()
            ) {
                listOf(WalletEntities.error(id, row, Cols.RECEIVED_TICKER, ProblemCodes.DOMAIN_RULE, "same ticker"))
            } else {
                emptyList()
            }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        repository.save(
            CryptoExchange(
                soldCrypto = tickerRepository.findBySymbol(row.text(Cols.SOLD_TICKER)!!.trim().uppercase())!!,
                receivedCrypto = tickerRepository.findBySymbol(row.text(Cols.RECEIVED_TICKER)!!.trim().uppercase())!!,
                soldQuantity = row.decimal(Cols.SOLD_QUANTITY)!!,
                receivedQuantity = row.decimal(Cols.RECEIVED_QUANTITY)!!,
                date = row.dateTime(Cols.DATE)!!,
                description = row.text(Cols.DESCRIPTION),
            ),
        )
    }
}

@Component
class BondSpec(
    private val repository: BondRepository,
    private val service: BondService,
) : EntitySpec {
    override val id = InvestmentEntities.BONDS
    override val group = InvestmentEntities.GROUP
    override val columns =
        listOf(
            Column(Cols.NAME, CellType.TEXT, required = true),
            Column(Cols.SYMBOL, CellType.TEXT),
            Column(Cols.TYPE, CellType.ENUM, required = true, enumClass = BondType::class.java),
            Column(Cols.ISSUER, CellType.TEXT),
            Column(Cols.MATURITY_DATE, CellType.DATE),
            Column(Cols.INTEREST_TYPE, CellType.ENUM, enumClass = InterestType::class.java),
            Column(Cols.INTEREST_INDEX, CellType.ENUM, enumClass = InterestIndex::class.java),
            Column(Cols.INTEREST_RATE, CellType.DECIMAL),
            Column(Cols.ARCHIVED, CellType.BOOLEAN),
        )

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedWith(compareBy({ it.name }, { it.id })).asSequence().map {
            mapOf(
                Cols.NAME to it.name,
                Cols.SYMBOL to it.symbol,
                Cols.TYPE to it.type,
                Cols.ISSUER to it.issuer,
                Cols.MATURITY_DATE to it.maturityDate,
                Cols.INTEREST_TYPE to it.interestType,
                Cols.INTEREST_INDEX to it.interestIndex,
                Cols.INTEREST_RATE to it.interestRate,
                Cols.ARCHIVED to it.archived,
            )
        }

    override fun naturalKey(row: ParsedRow) =
        NaturalKeys.of(row.text(Cols.NAME)!!.trim(), row.text(Cols.SYMBOL)?.trim()?.uppercase())

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy {
                NaturalKeys.of(it.name, it.symbol)
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> =
        InvestmentEntities.nonNegative(id, row, Cols.INTEREST_RATE) +
            if (row.text(Cols.NAME)!!.isBlank()) {
                listOf(WalletEntities.error(id, row, Cols.NAME, ProblemCodes.DOMAIN_RULE, "empty"))
            } else {
                emptyList()
            }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        service.createBond(
            Bond(
                name = row.text(Cols.NAME)!!,
                symbol = row.text(Cols.SYMBOL),
                type = row.enum<BondType>(Cols.TYPE)!!,
                issuer = row.text(Cols.ISSUER),
                maturityDate = row.date(Cols.MATURITY_DATE),
                interestType = row.enum<InterestType>(Cols.INTEREST_TYPE),
                interestIndex = row.enum<InterestIndex>(Cols.INTEREST_INDEX),
                interestRate = row.decimal(Cols.INTEREST_RATE),
                archived = row.bool(Cols.ARCHIVED) ?: false,
            ),
        )
    }
}

@Component
class BondOperationSpec(
    private val repository: BondOperationRepository,
    private val bondRepository: BondRepository,
    private val linker: TransactionLinker,
) : EntitySpec {
    override val id = InvestmentEntities.BOND_OPERATIONS
    override val group = InvestmentEntities.GROUP
    override val dependsOn = listOf(InvestmentEntities.BONDS, WalletEntities.TRANSACTIONS)
    override val columns =
        listOf(
            Column(Cols.BOND_NAME, CellType.TEXT, required = true),
            Column(Cols.BOND_SYMBOL, CellType.TEXT),
            Column(Cols.OPERATION_TYPE, CellType.ENUM, required = true, enumClass = OperationType::class.java),
            Column(Cols.QUANTITY, CellType.DECIMAL, required = true),
            Column(Cols.UNIT_PRICE, CellType.DECIMAL, required = true),
            Column(Cols.FEES, CellType.DECIMAL),
            Column(Cols.TAXES, CellType.DECIMAL),
            Column(Cols.NET_PROFIT, CellType.DECIMAL),
            Column(Cols.SPREAD, CellType.DECIMAL),
        ) + InvestmentEntities.transactionColumns

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedWith(compareBy({ it.walletTransaction!!.date }, { it.id })).asSequence().map {
            mapOf(
                Cols.BOND_NAME to it.bond.name,
                Cols.BOND_SYMBOL to it.bond.symbol,
                Cols.OPERATION_TYPE to it.operationType,
                Cols.QUANTITY to it.quantity,
                Cols.UNIT_PRICE to it.unitPrice,
                Cols.FEES to it.fees,
                Cols.TAXES to it.taxes,
                Cols.NET_PROFIT to it.netProfit,
                Cols.SPREAD to it.spread,
            ) + InvestmentEntities.transactionCells(it.walletTransaction!!)
        }

    private fun txType(type: OperationType?) =
        if (type ==
            OperationType.SELL
        ) {
            WalletTransactionType.INCOME
        } else {
            WalletTransactionType.EXPENSE
        }

    override fun naturalKey(row: ParsedRow): String {
        val type = row.enum<OperationType>(Cols.OPERATION_TYPE)
        return NaturalKeys.of(
            row.text(Cols.BOND_NAME)?.trim(),
            row.text(Cols.BOND_SYMBOL)?.trim()?.uppercase(),
            type,
            InvestmentEntities.exact(row.decimal(Cols.QUANTITY)?.toRounded()),
            row.decimal(Cols.UNIT_PRICE),
            InvestmentEntities.transactionKey(row, txType(type)),
        )
    }

    override fun existingKeys(): Map<String, Int> =
        repository
            .findAll()
            .groupingBy {
                val tx = it.walletTransaction!!
                NaturalKeys.of(
                    it.bond.name,
                    it.bond.symbol,
                    it.operationType,
                    InvestmentEntities.exact(it.quantity),
                    it.unitPrice,
                    WalletEntities.transactionKey(tx.wallet.name, tx.date, tx.type, tx.amount, tx.description),
                )
            }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val bondKey = NaturalKeys.of(row.text(Cols.BOND_NAME)!!.trim(), row.text(Cols.BOND_SYMBOL)?.trim()?.uppercase())
        val problems = mutableListOf<Problem>()
        if (!ctx.exists(InvestmentEntities.BONDS, bondKey)) {
            problems.add(WalletEntities.error(id, row, Cols.BOND_NAME, ProblemCodes.UNRESOLVED_REFERENCE))
        }
        problems.addAll(InvestmentEntities.positive(id, row, Cols.QUANTITY))
        problems.addAll(InvestmentEntities.nonNegative(id, row, Cols.UNIT_PRICE, Cols.FEES, Cols.TAXES))
        problems.addAll(
            InvestmentEntities.checkTransaction(id, row, txType(row.enum<OperationType>(Cols.OPERATION_TYPE)), ctx),
        )
        return problems
    }

    override fun applyOrder(rows: List<ParsedRow>): List<ParsedRow> = rows.sortedBy { it.dateTime(Cols.DATE) }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        val type = row.enum<OperationType>(Cols.OPERATION_TYPE)!!
        val name = row.text(Cols.BOND_NAME)!!.trim()
        val symbol = row.text(Cols.BOND_SYMBOL)?.trim()?.uppercase()
        val bond = bondRepository.findAll().first { it.name == name && it.symbol == symbol }
        repository.save(
            BondOperation(
                bond = bond,
                operationType = type,
                quantity = row.decimal(Cols.QUANTITY)!!,
                unitPrice = row.decimal(Cols.UNIT_PRICE)!!,
                fees = row.decimal(Cols.FEES) ?: BigDecimal.ZERO,
                taxes = row.decimal(Cols.TAXES) ?: BigDecimal.ZERO,
                netProfit = row.decimal(Cols.NET_PROFIT) ?: BigDecimal.ZERO,
                spread = row.decimal(Cols.SPREAD),
                walletTransaction = linker.take(ctx, InvestmentEntities.transactionKey(row, txType(type))),
            ),
        )
    }
}

@Component
class InvestmentTargetSpec(
    private val repository: InvestmentTargetRepository,
) : EntitySpec {
    override val id = InvestmentEntities.TARGETS
    override val group = InvestmentEntities.GROUP
    override val columns =
        listOf(
            Column(Cols.ASSET_TYPE, CellType.ENUM, required = true, enumClass = AssetType::class.java),
            Column(Cols.TARGET_PERCENTAGE, CellType.DECIMAL, required = true),
            Column(Cols.ACTIVE, CellType.BOOLEAN),
        )

    override fun count() = repository.count()

    override fun export() =
        repository.findAll().sortedBy { it.assetType }.asSequence().map {
            mapOf(
                Cols.ASSET_TYPE to it.assetType,
                Cols.TARGET_PERCENTAGE to it.targetPercentage,
                Cols.ACTIVE to it.isActive,
            )
        }

    override fun naturalKey(row: ParsedRow) = row.values[Cols.ASSET_TYPE].toString()

    override fun existingKeys(): Map<String, Int> = repository.findAll().groupingBy { it.assetType.name }.eachCount()

    override fun validate(
        row: ParsedRow,
        ctx: ResolveContext,
    ): List<Problem> {
        val value = row.decimal(Cols.TARGET_PERCENTAGE)!!
        return if (value < BigDecimal.ZERO || value > BigDecimal(100)) {
            listOf(WalletEntities.error(id, row, Cols.TARGET_PERCENTAGE, ProblemCodes.DOMAIN_RULE, "0..100"))
        } else {
            emptyList()
        }
    }

    override fun apply(
        row: ParsedRow,
        ctx: ApplyContext,
    ) {
        repository.save(
            InvestmentTarget(
                assetType = row.enum<AssetType>(Cols.ASSET_TYPE)!!,
                targetPercentage = row.decimal(Cols.TARGET_PERCENTAGE)!!,
                isActive = row.bool(Cols.ACTIVE) ?: true,
            ),
        )
    }
}
