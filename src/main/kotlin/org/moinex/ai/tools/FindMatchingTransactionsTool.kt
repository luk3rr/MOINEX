/*
 * Filename: FindMatchingTransactionsTool.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

import org.json.JSONArray
import org.json.JSONObject
import org.moinex.common.constant.TranslationKeys
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.matching.Match
import org.moinex.service.matching.MatchQuery
import org.moinex.service.matching.TransactionMatchingService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Read-only lookup of records the app already has for a statement line, so the assistant can answer "did I already
 * record this?" with the same rule the app applies when it receives proposals.
 */
@Service
class FindMatchingTransactionsTool(
    private val matchingService: TransactionMatchingService,
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val preferencesService: PreferencesService,
) : LlmTool {
    companion object {
        const val NAME = "find_matching_transactions"
    }

    override val name = NAME
    override val description = preferencesService.translate(TranslationKeys.AI_TOOL_FIND_MATCHES_DESCRIPTION)

    override val jsonSchema: String
        get() = buildJsonSchema().toString()

    private fun buildJsonSchema(): JSONObject {
        fun property(
            type: String,
            descriptionKey: String,
        ) = JSONObject().put("type", type).put("description", preferencesService.translate(descriptionKey))

        val properties =
            JSONObject()
                .put("amount", property("number", TranslationKeys.AI_TOOL_FIND_MATCHES_AMOUNT_PARAM))
                .put("date", property("string", TranslationKeys.AI_TOOL_FIND_MATCHES_DATE_PARAM).put("format", "date"))
                .put("walletName", property("string", TranslationKeys.AI_TOOL_FIND_MATCHES_WALLET_PARAM))
                .put("creditCardName", property("string", TranslationKeys.AI_TOOL_FIND_MATCHES_CREDIT_CARD_PARAM))
                .put(
                    "movementType",
                    property("string", TranslationKeys.AI_TOOL_FIND_MATCHES_MOVEMENT_TYPE_PARAM)
                        .put("enum", JSONArray(listOf("income", "expense"))),
                ).put("description", property("string", TranslationKeys.AI_TOOL_FIND_MATCHES_DESCRIPTION_PARAM))

        return JSONObject()
            .put("type", "object")
            .put("properties", properties)
            .put("required", JSONArray(listOf("amount", "date")))
    }

    override fun execute(argumentsJson: String): String {
        val arguments = JSONObject(argumentsJson)

        val wallet =
            arguments.optString("walletName").takeIf { it.isNotBlank() }?.let { name ->
                walletService.getAllNonArchivedWalletsOrderedByName().firstOrNull { it.name.equals(name, true) }
            }
        val creditCard =
            arguments.optString("creditCardName").takeIf { it.isNotBlank() }?.let { name ->
                creditCardService.getAllNonArchivedCreditCardsOrderedByName().firstOrNull { it.name.equals(name, true) }
            }

        val query =
            MatchQuery(
                amount = arguments.getBigDecimal("amount").abs(),
                date = LocalDate.parse(arguments.getString("date")),
                walletId = wallet?.id,
                creditCardId = creditCard?.id,
                isIncome =
                    when (arguments.optString("movementType").lowercase()) {
                        "income" -> true
                        "expense" -> false
                        else -> null
                    },
                description = arguments.optString("description").takeIf { it.isNotBlank() },
            )

        return JSONObject()
            .put("matches", JSONArray(matchingService.findMatches(query).map(::toJson)))
            .toString()
    }

    private fun toJson(match: Match): JSONObject =
        JSONObject()
            .put("targetType", match.targetType.name.lowercase())
            .put("targetId", match.targetId)
            .put("status", match.status.name.lowercase())
            .put("score", match.score)
            .put("reasons", JSONArray(match.reasons))
            .put("existingDescription", match.existingDescription)
            .put("date", match.date.toString())
            .put("amount", match.amount)
}
