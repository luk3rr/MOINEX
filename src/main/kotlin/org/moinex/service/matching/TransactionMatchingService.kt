/*
 * Filename: TransactionMatchingService.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.matching

import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.atEndOfDay
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.MessageFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Recognizes that a statement line is a record the app already has. A line matches when the amount is exactly the
 * same, the date is at most [DATE_WINDOW_DAYS] days away and the account (wallet or card) is the same; the
 * description only breaks ties, so a different name never hides a duplicate.
 */
@Service
class TransactionMatchingService(
    private val walletTransactionRepository: WalletTransactionRepository,
    private val transferRepository: TransferRepository,
    private val creditCardDebtRepository: CreditCardDebtRepository,
    private val preferencesService: PreferencesService,
) {
    companion object {
        const val DATE_WINDOW_DAYS = 3L
        private const val AMOUNT_SCALE = 2
        private const val DAY_PENALTY = 0.05
        private const val UNKNOWN_ACCOUNT_PENALTY = 0.10
        private const val DESCRIPTION_BONUS = 0.05
        private const val SIMILAR_DESCRIPTION_THRESHOLD = 0.5
    }

    /** The reason texts, translated once per lookup instead of once per candidate. */
    private class Texts(
        val exactAmount: String,
        val installment: String,
        val sameDate: String,
        val dateOffset: String,
        val sameWallet: String,
        val sameCard: String,
        val accountUnknown: String,
        val similarDescription: String,
    )

    private fun texts() =
        Texts(
            exactAmount = translate(TranslationKeys.AI_MATCH_REASON_EXACT_AMOUNT),
            installment = translate(TranslationKeys.AI_MATCH_REASON_INSTALLMENT),
            sameDate = translate(TranslationKeys.AI_MATCH_REASON_SAME_DATE),
            dateOffset = translate(TranslationKeys.AI_MATCH_REASON_DATE_OFFSET),
            sameWallet = translate(TranslationKeys.AI_MATCH_REASON_SAME_WALLET),
            sameCard = translate(TranslationKeys.AI_MATCH_REASON_SAME_CARD),
            accountUnknown = translate(TranslationKeys.AI_MATCH_REASON_ACCOUNT_UNKNOWN),
            similarDescription = translate(TranslationKeys.AI_MATCH_REASON_SIMILAR_DESCRIPTION),
        )

    private data class Target(
        val type: MatchTargetType,
        val id: Int,
        val status: MatchStatus,
        val date: LocalDate,
        val amount: BigDecimal,
        val description: String,
        val walletId: Int? = null,
        val receiverWalletId: Int? = null,
        val creditCardId: Int? = null,
        val isIncome: Boolean? = null,
        val installments: Int = 1,
    )

    fun findMatches(query: MatchQuery): List<Match> = findMatches(listOf(query)).first()

    /**
     * Matches every query against the stored records of the date window that covers all of them, reading the
     * database once. The result has one list per query, in the same order, best candidate first.
     */
    @Transactional(readOnly = true)
    fun findMatches(queries: List<MatchQuery>): List<List<Match>> {
        if (queries.isEmpty()) return emptyList()

        val from = queries.minOf { it.date }.minusDays(DATE_WINDOW_DAYS)
        val to = queries.maxOf { it.date }.plusDays(DATE_WINDOW_DAYS)

        val index = HashMap<BigDecimal, MutableList<Target>>()
        loadTargets(from, to).forEach { (key, target) -> index.getOrPut(key) { mutableListOf() }.add(target) }

        val texts = texts()
        return queries.map { query -> matchOne(query, index[query.amount.normalized()].orEmpty(), texts) }
    }

    private fun matchOne(
        query: MatchQuery,
        candidates: List<Target>,
        texts: Texts,
    ): List<Match> =
        candidates
            .distinct()
            .mapNotNull { target -> evaluate(query, target, texts) }
            .sortedWith(
                compareByDescending<Triple<Target, Double, List<String>>> { it.second }
                    .thenByDescending { DescriptionSimilarity.score(query.description, it.first.description) }
                    .thenBy { abs(ChronoUnit.DAYS.between(query.date, it.first.date)) }
                    .thenBy { it.first.id },
            ).map { (target, score, reasons) ->
                Match(
                    targetType = target.type,
                    targetId = target.id,
                    status = target.status,
                    score = score,
                    reasons = reasons,
                    existingDescription = target.description,
                    date = target.date,
                    amount = target.amount,
                )
            }

    private fun evaluate(
        query: MatchQuery,
        target: Target,
        texts: Texts,
    ): Triple<Target, Double, List<String>>? {
        val dayDistance = abs(ChronoUnit.DAYS.between(query.date, target.date))
        if (dayDistance > DATE_WINDOW_DAYS) return null

        val accountReason = accountReason(query, target, texts) ?: return null

        val reasons = mutableListOf<String>()
        reasons += texts.exactAmount
        if (target.type == MatchTargetType.CREDIT_CARD_DEBT && target.installments > 1) {
            reasons += MessageFormat.format(texts.installment, target.installments)
        }
        reasons +=
            if (dayDistance == 0L) {
                texts.sameDate
            } else {
                MessageFormat.format(texts.dateOffset, dayDistance)
            }
        reasons += accountReason.text

        val similarity = DescriptionSimilarity.score(query.description, target.description)
        if (similarity >= SIMILAR_DESCRIPTION_THRESHOLD) {
            reasons += texts.similarDescription
        }

        val score =
            (1.0 - DAY_PENALTY * dayDistance - accountReason.penalty + DESCRIPTION_BONUS * similarity)
                .coerceIn(0.0, 1.0)

        return Triple(target, score, reasons)
    }

    private class AccountReason(
        val text: String,
        val penalty: Double,
    )

    private fun accountReason(
        query: MatchQuery,
        target: Target,
        texts: Texts,
    ): AccountReason? {
        val onCard = target.type == MatchTargetType.CREDIT_CARD_DEBT

        return when {
            query.creditCardId != null ->
                if (onCard && target.creditCardId == query.creditCardId) {
                    AccountReason(texts.sameCard, 0.0)
                } else {
                    null
                }
            query.walletId != null -> walletReason(query, target, texts)
            else -> AccountReason(texts.accountUnknown, UNKNOWN_ACCOUNT_PENALTY)
        }
    }

    private fun walletReason(
        query: MatchQuery,
        target: Target,
        texts: Texts,
    ): AccountReason? {
        val sameWallet = AccountReason(texts.sameWallet, 0.0)

        return when (target.type) {
            MatchTargetType.CREDIT_CARD_DEBT -> null
            MatchTargetType.WALLET_TRANSACTION ->
                sameWallet.takeIf {
                    target.walletId == query.walletId && (query.isIncome == null || query.isIncome == target.isIncome)
                }
            MatchTargetType.TRANSFER ->
                sameWallet.takeIf {
                    when (query.isIncome) {
                        true -> target.receiverWalletId == query.walletId
                        false -> target.walletId == query.walletId
                        null -> target.walletId == query.walletId || target.receiverWalletId == query.walletId
                    }
                }
        }
    }

    private fun loadTargets(
        from: LocalDate,
        to: LocalDate,
    ): List<Pair<BigDecimal, Target>> {
        val start = from.atStartOfDay()
        val end = to.atEndOfDay()

        val walletTargets =
            walletTransactionRepository.findTransactionsBetweenDates(start, end).map { transaction ->
                transaction.amount.normalized() to
                    Target(
                        type = MatchTargetType.WALLET_TRANSACTION,
                        id = transaction.id!!,
                        status =
                            if (transaction.status == WalletTransactionStatus.CONFIRMED) {
                                MatchStatus.CONFIRMED
                            } else {
                                MatchStatus.PENDING
                            },
                        date = transaction.date.toLocalDate(),
                        amount = transaction.amount,
                        description = transaction.description.orEmpty(),
                        walletId = transaction.wallet.id,
                        isIncome = transaction.type == WalletTransactionType.INCOME,
                    )
            }

        val transferTargets =
            transferRepository.findBetweenDates(start, end).map { transfer ->
                transfer.amount.normalized() to
                    Target(
                        type = MatchTargetType.TRANSFER,
                        id = transfer.id!!,
                        status = MatchStatus.CONFIRMED,
                        date = transfer.date.toLocalDate(),
                        amount = transfer.amount,
                        description = transfer.description.orEmpty(),
                        walletId = transfer.senderWallet.id,
                        receiverWalletId = transfer.receiverWallet.id,
                    )
            }

        val debtTargets =
            creditCardDebtRepository.findByDateBetween(start, end).flatMap { debt ->
                val target =
                    Target(
                        type = MatchTargetType.CREDIT_CARD_DEBT,
                        id = debt.id!!,
                        status = MatchStatus.CONFIRMED,
                        date = debt.date.toLocalDate(),
                        amount = debt.amount,
                        description = debt.description.orEmpty(),
                        creditCardId = debt.creditCard.id,
                        installments = debt.installments,
                    )
                installmentAmountKeys(debt.amount, debt.installments).map { key -> key to target }
            }

        return walletTargets + transferTargets + debtTargets
    }

    /**
     * A debt stores the whole purchase; a statement line shows one installment. The installment is the total divided
     * by the count, rounded to cents, and the neighbours cover the cent the card issuer may have moved to the first
     * or last installment.
     */
    private fun installmentAmountKeys(
        total: BigDecimal,
        installments: Int,
    ): Set<BigDecimal> {
        if (installments <= 1) return setOf(total.normalized())

        val count = installments.toBigDecimal()
        return setOf(
            total.divide(count, AMOUNT_SCALE, RoundingMode.HALF_UP),
            total.divide(count, AMOUNT_SCALE, RoundingMode.DOWN),
            total.divide(count, AMOUNT_SCALE, RoundingMode.UP),
        ).map { it.normalized() }.toSet()
    }

    private fun BigDecimal.normalized(): BigDecimal = abs().setScale(AMOUNT_SCALE, RoundingMode.HALF_UP)

    private fun translate(key: String): String = preferencesService.translate(key)
}
