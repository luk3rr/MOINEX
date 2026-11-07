/*
 * Filename: ProposeChangesTool.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.moinex.ai.operation.AppOperation
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.EntityCatalog
import org.moinex.ai.operation.EntityCatalogProvider
import org.moinex.ai.operation.OperationRegistry
import org.moinex.ai.operation.ProposedChange
import org.moinex.ai.operation.transactions.CategoryPayload
import org.moinex.common.constant.TranslationKeys
import org.moinex.model.dto.ai.IgnoredLine
import org.moinex.service.PreferencesService
import org.moinex.service.matching.Match
import org.moinex.service.matching.MatchStatus
import org.moinex.service.matching.MatchTargetType
import org.moinex.service.matching.TransactionMatchingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.util.UUID

/**
 * The only way the assistant puts changes in front of the user: it names operations and their fields, the app turns
 * them into proposals (resolving names, checking what is already recorded) and nothing is written until the user
 * approves each one in the review panel. A line already recorded comes back as ignored, never as a proposal.
 */
@Service
class ProposeChangesTool(
    private val registry: OperationRegistry,
    private val catalogProvider: EntityCatalogProvider,
    private val matchingService: TransactionMatchingService,
    private val preferencesService: PreferencesService,
) : LlmTool {
    companion object {
        const val NAME = "propose_changes"
        const val LOW_CONFIDENCE_THRESHOLD = 0.6
        private const val DEFAULT_CONFIDENCE = 1.0
        private const val CREATE_CATEGORY = "create_category"
        private val logger = LoggerFactory.getLogger(ProposeChangesTool::class.java)
    }

    data class ErrorItem(
        val index: Int,
        val message: String,
    )

    data class Outcome(
        val changeSet: ChangeSet,
        val errors: List<ErrorItem>,
    )

    override val name = NAME

    override val description: String
        get() =
            preferencesService.translate(TranslationKeys.AI_TOOL_PROPOSE_CHANGES_DESCRIPTION) +
                "\n" +
                registry.all().joinToString("\n") {
                    "- " + it.modelDescription() + requiredNote(it) +
                        exampleNote(it)
                }

    override val jsonSchema: String
        get() = buildJsonSchema().toString()

    private fun buildJsonSchema(): JSONObject {
        val properties = itemProperties()

        val item =
            JSONObject()
                .put("type", "object")
                .put("properties", properties)
                .put("required", JSONArray(listOf("operation")))

        return JSONObject()
            .put("type", "object")
            .put("properties", JSONObject().put("changes", JSONObject().put("type", "array").put("items", item)))
            .put("required", JSONArray(listOf("changes")))
    }

    private fun itemProperties(): JSONObject {
        val properties =
            JSONObject()
                .put(
                    "operation",
                    JSONObject().put("type", "string").put("enum", JSONArray(registry.all().map { it.name }.sorted())),
                ).put(
                    "date",
                    JSONObject()
                        .put("type", "string")
                        .put("format", "date")
                        .put(
                            "description",
                            "Date of the operation. For a card purchase, the date it was made, not the date it " +
                                "was posted to the statement nor the invoice due date.",
                        ),
                ).put("description", JSONObject().put("type", "string"))
                .put(
                    "amount",
                    JSONObject()
                        .put("type", "number")
                        .put(
                            "description",
                            "Always positive - never negative, regardless of direction. For create_credit_card_debt " +
                                "with installments greater than 1, this is the value of THIS installment/line as " +
                                "printed (e.g. the R$50 next to \"3/10\"), never a computed total - the app " +
                                "multiplies it by the installment count itself. Not used by the investment " +
                                "operations.",
                        ),
                ).put(
                    "walletName",
                    JSONObject()
                        .put("type", "string")
                        .put(
                            "description",
                            "REQUIRED for create_wallet_transaction and the investment operations: the existing " +
                                "wallet the money comes from or goes to. Pick it from the existing wallets in " +
                                "context using the institution or account shown in the document (e.g. a statement " +
                                "from \"Banco Inter\" means the wallet whose name contains \"Inter\"). Leave it " +
                                "empty only when the document gives no hint at all.",
                        ),
                ).put("creditCardName", JSONObject().put("type", "string"))
                .put("categoryName", JSONObject().put("type", "string"))
                .put(
                    "confidence",
                    JSONObject()
                        .put("type", "number")
                        .put(
                            "description",
                            "Optional, 0 to 1: how sure you are that this item is right. Leave it out when you are " +
                                "confident; send a value below 0.6 only when the item is a guess (an unreadable " +
                                "line, a doubtful amount or a wallet/category you had to assume).",
                        ),
                ).put(
                    "allowDuplicate",
                    JSONObject()
                        .put("type", "boolean")
                        .put(
                            "description",
                            "Set to true only when the user explicitly asked to record an item even though the app " +
                                "reported it as already recorded (in the ignored list of an earlier result). " +
                                "Never set it on your own.",
                        ),
                )

        registry.all().flatMap { it.modelProperties().entries }.forEach { (key, value) -> properties.put(key, value) }
        return properties
    }

    /**
     * A complete item of [operation] with a placeholder for each required field. Models that leave out the fields they
     * are only told about in prose copy a concrete example, so it goes in the description and in the error that a
     * field-less item gets.
     */
    fun exampleOf(operation: AppOperation<*>): String {
        val properties = itemProperties()
        val fields =
            listOf("operation") + operation.modelRequired().filter { it != "operation" }
        return fields.joinToString(prefix = "{", postfix = "}", separator = ",") { key ->
            val value =
                if (key == "operation") {
                    "\"${operation.name}\""
                } else {
                    placeholderOf(key, properties.optJSONObject(key))
                }
            "\"$key\":$value"
        }
    }

    private fun placeholderOf(
        key: String,
        schema: JSONObject?,
    ): String {
        val enumValues =
            schema
                ?.optJSONArray("enum")
                ?.toList()
                ?.map { it.toString() }
                .orEmpty()
        return when {
            enumValues.isNotEmpty() -> "\"${enumValues.joinToString("|")}\""
            schema?.optString("format") == "date" -> "\"YYYY-MM-DD\""
            schema?.optString("type") == "number" || schema?.optString("type") == "integer" -> "<number>"
            schema?.optString("type") == "boolean" -> "true|false"
            key.endsWith("WalletName") || key == "walletName" -> "\"<existing wallet>\""
            key == "categoryName" -> "\"<existing category>\""
            key == "creditCardName" -> "\"<existing card>\""
            else -> "\"<text>\""
        }
    }

    private fun exampleNote(operation: AppOperation<*>): String = " Example: " + exampleOf(operation)

    private fun requiredNote(operation: AppOperation<*>): String =
        operation
            .modelRequired()
            .takeIf { it.isNotEmpty() }
            ?.let { " Required fields: " + it.joinToString() + "." }
            .orEmpty()

    fun describeAvailableContext(): String {
        val catalog = catalogProvider.snapshot()
        val categories = catalog.categories.joinToString(", ") { it.name }
        val wallets = catalog.wallets.joinToString(", ") { it.name }
        val creditCards = catalog.creditCards.joinToString(", ") { it.name }
        val tickers = catalog.tickers.joinToString(", ") { "${it.symbol} (${it.name})" }
        val bonds =
            catalog.bonds.joinToString(", ") { bond -> bond.symbol?.let { "$it (${bond.name})" } ?: bond.name }

        return "Existing categories: $categories\nExisting wallets: $wallets\nExisting credit cards: $creditCards\n" +
            "Existing tickers (stocks/FIIs/ETFs/crypto): $tickers\nExisting bonds/CDBs: $bonds"
    }

    override fun execute(argumentsJson: String): String = toModelJson(propose(argumentsJson))

    private class Parsed(
        val index: Int,
        val operation: AppOperation<Any>,
        val payload: Any,
        val arguments: JSONObject,
    )

    fun propose(argumentsJson: String): Outcome {
        logger.debug("propose_changes arguments: {}", argumentsJson)
        val items = JSONObject(argumentsJson).optJSONArray("changes") ?: JSONArray()
        val catalog = catalogProvider.snapshot()
        val errors = mutableListOf<ErrorItem>()

        val parsed =
            (0 until items.length()).mapNotNull { index ->
                runCatching { parse(index, items.getJSONObject(index), catalog) }
                    .onFailure { errors += ErrorItem(index, it.message ?: it::class.simpleName.orEmpty()) }
                    .getOrNull()
            }

        val queries = parsed.map { it.operation.matchQuery(it.payload) }
        val found = matchingService.findMatches(queries.filterNotNull())
        val matchesByItem = ArrayDeque(found)

        val changes = mutableListOf<ProposedChange>()
        val ignored = mutableListOf<IgnoredLine>()

        parsed.forEachIndexed { position, item ->
            val matches = if (queries[position] != null) matchesByItem.removeFirst() else emptyList()
            decide(item, matches, ignored)?.let { changes += it }
        }

        val outcome =
            Outcome(ChangeSet(UUID.randomUUID().toString(), withCategoryCards(changes, catalog), ignored), errors)
        logger.debug(
            "propose_changes outcome: {} items received, accepted={}, ignored={}, errors={}",
            items.length(),
            outcome.changeSet.changes.map { "${it.operationName}:${it.badge}" },
            outcome.changeSet.ignored.map { "${it.targetType}#${it.targetId}" },
            errors.map { "#${it.index}: ${it.message}" },
        )
        return outcome
    }

    /**
     * A category the changes use but that does not exist yet becomes its own card, put before the changes that need
     * it; those changes depend on it, so it is created only when the user approves it. A "create_category" the model
     * sent itself for the same name is reused instead of duplicated.
     */
    @Suppress("UNCHECKED_CAST")
    private fun withCategoryCards(
        changes: List<ProposedChange>,
        catalog: EntityCatalog,
    ): List<ProposedChange> {
        val categoryOperation = registry.find(CREATE_CATEGORY) as? AppOperation<Any> ?: return changes
        val cards = linkedMapOf<String, ProposedChange>()
        changes
            .filter { it.operationName == CREATE_CATEGORY }
            .forEach { cards.putIfAbsent(categoryKey((it.payload as CategoryPayload).name), it) }

        val extra = mutableListOf<ProposedChange>()
        val linked =
            changes.map { change ->
                val needed =
                    (registry.find(change.operationName) as? AppOperation<Any>)
                        ?.categoryNeeded(change.payload)
                        ?.takeIf { catalog.category(it) == null }
                        ?: return@map change

                val card =
                    cards.getOrPut(categoryKey(needed)) {
                        val payload = CategoryPayload(needed.trim())
                        ProposedChange(
                            id = UUID.randomUUID().toString(),
                            operationName = CREATE_CATEGORY,
                            payload = payload,
                            summary = categoryOperation.summary(payload),
                            badge = ChangeBadge.NEW,
                            reasons = emptyList(),
                            confidence = 1.0,
                        ).also { extra += it }
                    }
                change.copy(dependsOn = change.dependsOn + card.id)
            }
        return extra + linked
    }

    private fun categoryKey(name: String) = name.trim().lowercase()

    private fun parse(
        index: Int,
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): Parsed {
        val operationName = arguments.optString("operation")

        @Suppress("UNCHECKED_CAST")
        val operation =
            registry.find(operationName) as? AppOperation<Any> ?: error("unknown operation: $operationName")
        val payload =
            try {
                operation.fromModelArguments(arguments, catalog)
            } catch (e: JSONException) {
                val received = arguments.keySet().sorted()
                logger.info(
                    "Item {} of propose_changes ({}) arrived with fields {}: {}",
                    index,
                    operationName,
                    received,
                    e.message,
                )
                error(
                    "$operationName could not be read (${e.message}). The item arrived with only these fields: " +
                        "$received. Send the fields of the operation in the same item, next to \"operation\", " +
                        "like this: ${exampleOf(operation)} - and resend it.",
                )
            }
        val missing = operation.missingFromModel(payload)
        require(missing.isEmpty()) {
            "$operationName is missing: ${missing.joinToString("; ")}. Resend it with the values from the document."
        }
        return Parsed(index, operation, payload, arguments)
    }

    private fun decide(
        item: Parsed,
        matches: List<Match>,
        ignored: MutableList<IgnoredLine>,
    ): ProposedChange? {
        val allowDuplicate = item.arguments.optBoolean("allowDuplicate", false)
        val confidence = item.arguments.optDouble("confidence", DEFAULT_CONFIDENCE)
        val llmExistingId = item.arguments.optInt("existingTransactionId", -1).takeIf { it > 0 }

        val pending = matches.firstOrNull { it.status == MatchStatus.PENDING }
        val confirmed = matches.firstOrNull { it.status == MatchStatus.CONFIRMED }

        val confirmedPayload =
            (llmExistingId ?: pending?.targetId)
                ?.takeIf { !allowDuplicate || llmExistingId != null }
                ?.let { item.operation.confirmingPending(item.payload, it) }

        return when {
            confirmedPayload != null ->
                change(
                    item,
                    confirmedPayload,
                    ChangeBadge.MATCHES_EXISTING,
                    pending?.reasons.orEmpty(),
                    confidence,
                    matches,
                    llmExistingId ?: pending?.targetId,
                )
            confirmed != null && !allowDuplicate -> {
                ignored += ignoredLine(item, confirmed)
                null
            }
            confirmed != null ->
                change(item, item.payload, ChangeBadge.POSSIBLE_DUPLICATE, confirmed.reasons, confidence, matches)
            confidence < LOW_CONFIDENCE_THRESHOLD ->
                change(item, item.payload, ChangeBadge.LOW_CONFIDENCE, emptyList(), confidence, matches)
            else -> change(item, item.payload, ChangeBadge.NEW, emptyList(), confidence, matches)
        }
    }

    private fun change(
        item: Parsed,
        payload: Any,
        badge: ChangeBadge,
        reasons: List<String>,
        confidence: Double,
        matches: List<Match>,
        targetId: Int? = null,
    ) = ProposedChange(
        id = UUID.randomUUID().toString(),
        operationName = item.operation.name,
        payload = payload,
        summary = item.operation.summary(payload),
        badge = badge,
        reasons = reasons,
        confidence = confidence,
        matchCandidates = matches,
        targetId = targetId,
    )

    private fun ignoredLine(
        item: Parsed,
        match: Match,
    ): IgnoredLine {
        val target =
            preferencesService.translate(
                when (match.targetType) {
                    MatchTargetType.WALLET_TRANSACTION -> TranslationKeys.AI_CHAT_IGNORED_TARGET_WALLET_TRANSACTION
                    MatchTargetType.CREDIT_CARD_DEBT -> TranslationKeys.AI_CHAT_IGNORED_TARGET_CREDIT_CARD_DEBT
                    MatchTargetType.TRANSFER -> TranslationKeys.AI_CHAT_IGNORED_TARGET_TRANSFER
                },
            )
        val display =
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.AI_CHAT_IGNORED_ITEM),
                item.operation.summary(item.payload),
                target,
                match.targetId,
                match.reasons.joinToString(", "),
            )
        return IgnoredLine(display, match.targetType, match.targetId, match.reasons)
    }

    fun toModelJson(outcome: Outcome): String =
        JSONObject()
            .put(
                "accepted",
                JSONArray(
                    outcome.changeSet.changes.map {
                        JSONObject()
                            .put("id", it.id)
                            .put("operation", it.operationName)
                            .put("summary", it.summary)
                            .put("badge", it.badge.name.lowercase())
                    },
                ),
            ).put(
                "ignored",
                JSONArray(
                    outcome.changeSet.ignored.map {
                        JSONObject()
                            .put("display", it.display)
                            .put("targetType", it.targetType.name.lowercase())
                            .put("targetId", it.targetId)
                            .put("reasons", JSONArray(it.reasons))
                    },
                ),
            ).put(
                "errors",
                JSONArray(outcome.errors.map { JSONObject().put("index", it.index).put("message", it.message) }),
            ).toString()
}
