/*
 * Filename: EntityCatalog.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import org.moinex.model.Category
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.investment.Bond
import org.moinex.model.investment.Ticker
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.CategoryService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.text.Normalizer

/**
 * The records the assistant can refer to by name, read once per proposal. The model never sees ids: it names a
 * wallet, card, category, ticker or bond, and the catalog resolves the name.
 */
data class EntityCatalog(
    val wallets: List<Wallet> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    val tickers: List<Ticker> = emptyList(),
    val bonds: List<Bond> = emptyList(),
    val categories: List<Category> = emptyList(),
) {
    fun wallet(name: String?): Wallet? = byName(wallets, name) { it.name }

    fun creditCard(name: String?): CreditCard? = byName(creditCards, name) { it.name }

    /**
     * Exact name first (ignoring case and accents), then a unique record whose name words all appear in the given
     * name or the other way round, so "Banco Inter" finds the wallet "Inter". An ambiguous name matches nothing.
     */
    private fun <T> byName(
        records: List<T>,
        name: String?,
        nameOf: (T) -> String,
    ): T? {
        val wanted = normalize(name)
        if (wanted.isEmpty()) return null

        records.firstOrNull { normalize(nameOf(it)) == wanted }?.let { return it }

        val wantedWords = wanted.split(' ').toSet()
        return records
            .filter { record ->
                val words = normalize(nameOf(record)).split(' ').toSet()
                words.isNotEmpty() && (wantedWords.containsAll(words) || words.containsAll(wantedWords))
            }.singleOrNull()
    }

    private fun normalize(text: String?): String =
        Normalizer
            .normalize(text.orEmpty().lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()

    fun category(name: String?): Category? = categories.firstOrNull { it.name.equals(name, ignoreCase = true) }

    /**
     * Matches by symbol, name, or the symbol's base part before an exchange suffix, since a ticker stored as
     * "PETR4.SA" is read off a statement as "PETR4".
     */
    fun ticker(assetName: String?): Ticker? =
        tickers.firstOrNull {
            it.symbol.equals(assetName, ignoreCase = true) ||
                it.name.equals(assetName, ignoreCase = true) ||
                it.symbol.substringBefore(".").equals(assetName, ignoreCase = true)
        }

    fun bond(assetName: String?): Bond? =
        bonds.firstOrNull {
            it.symbol?.equals(assetName, ignoreCase = true) == true || it.name.equals(assetName, ignoreCase = true)
        }
}

@Component
class EntityCatalogProvider(
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val tickerService: TickerService,
    private val bondService: BondService,
    private val categoryService: CategoryService,
) {
    fun snapshot(): EntityCatalog =
        EntityCatalog(
            wallets = walletService.getAllNonArchivedWalletsOrderedByName(),
            creditCards = creditCardService.getAllNonArchivedCreditCardsOrderedByName(),
            tickers = tickerService.getAllNonArchivedTickers(),
            bonds = bondService.getAllNonArchivedBonds(),
            categories = categoryService.getNonArchivedCategoriesOrderedByName(),
        )
}
