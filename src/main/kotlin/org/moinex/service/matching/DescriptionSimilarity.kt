/*
 * Filename: DescriptionSimilarity.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.matching

import java.text.Normalizer

/**
 * Loose comparison of two free-text descriptions. It is only used to order candidates that already agree on
 * amount, date and account, so it never needs to be exact.
 */
object DescriptionSimilarity {
    private val NOISE =
        setOf("pix", "compra", "debito", "credito", "pgto", "pagamento", "transf", "transferencia", "ted", "doc")

    private const val CONTAINMENT_WEIGHT = 0.9

    private val DIACRITICS = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val SEPARATORS = "[^a-z0-9]+".toRegex()

    fun tokens(text: String?): Set<String> =
        Normalizer
            .normalize(text.orEmpty().lowercase(), Normalizer.Form.NFD)
            .replace(DIACRITICS, "")
            .split(SEPARATORS)
            .filter { it.length > 1 && it !in NOISE && !it.all(Char::isDigit) }
            .toSet()

    /**
     * 0.0 when either side has no meaningful token or they share none, 1.0 for the same tokens, and 0.9 when one
     * description is fully contained in the other (a statement name that adds words to the stored one).
     */
    fun score(
        first: String?,
        second: String?,
    ): Double {
        val a = tokens(first)
        val b = tokens(second)
        if (a.isEmpty() || b.isEmpty()) return 0.0

        val common = a.intersect(b).size
        if (common == 0) return 0.0

        val jaccard = common.toDouble() / a.union(b).size
        val containment = common.toDouble() / minOf(a.size, b.size)
        return maxOf(jaccard, containment * CONTAINMENT_WEIGHT)
    }
}
