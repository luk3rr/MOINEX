/*
 * Filename: BondManagementOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.investments

import org.json.JSONArray
import org.json.JSONObject
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.BaseOperation
import org.moinex.ai.operation.DraftField
import org.moinex.ai.operation.DraftFieldType
import org.moinex.ai.operation.DraftValues
import org.moinex.ai.operation.EntityCatalog
import org.moinex.ai.operation.OperationDomain
import org.moinex.ai.operation.OperationRisk
import org.moinex.ai.operation.Preview
import org.moinex.ai.operation.ValidationResult
import org.moinex.model.enums.BondType
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.investment.Bond
import org.moinex.service.investment.BondService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * A bond to register or, when [bondId] is set, to change. Blank text and null mean "not informed"; [bondType] is
 * null while the model's type matched none.
 */
data class BondPayload(
    val bondId: Int?,
    val name: String,
    val symbol: String,
    val bondType: BondType?,
    val issuer: String,
    val maturityDate: LocalDate?,
    val interestType: InterestType?,
    val interestIndex: InterestIndex?,
    val interestRate: BigDecimal?,
    val before: String? = null,
)

private inline fun <reified T : Enum<T>> String?.toEnum(): T? =
    enumValues<T>().firstOrNull { it.name.equals(this, ignoreCase = true) }

private val BOND_DRAFT_SPEC =
    listOf(
        DraftField("name", "aidraft.field.name", DraftFieldType.TEXT),
        DraftField("symbol", "aidraft.field.symbol", DraftFieldType.TEXT, required = false),
        DraftField("bondType", "aidraft.field.bondType", DraftFieldType.TEXT),
        DraftField("issuer", "aidraft.field.issuer", DraftFieldType.TEXT, required = false),
        DraftField("maturityDate", "aidraft.field.maturityDate", DraftFieldType.DATE, required = false),
        DraftField("interestType", "aidraft.field.interestType", DraftFieldType.TEXT, required = false),
        DraftField("interestIndex", "aidraft.field.interestIndex", DraftFieldType.TEXT, required = false),
        DraftField("interestRate", "aidraft.field.interestRate", DraftFieldType.DECIMAL, required = false),
    )

/** Shared by the bond creation and change operations. */
abstract class BondManagementOperation(
    protected val bondService: BondService,
) : BaseOperation<BondPayload>() {
    override val domain = OperationDomain.INVESTMENTS

    private fun enumProperty(
        description: String,
        values: List<String>,
    ) = property("string", description).put("enum", JSONArray(values.map { it.lowercase() }))

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "bondType" to enumProperty("For bonds: the kind of bond.", BondType.entries.map { it.name }),
            "issuer" to property("string", "For bonds: who issued it."),
            "maturityDate" to property("string", "For bonds: maturity date (YYYY-MM-DD)."),
            "interestType" to enumProperty("For bonds: how interest is set.", InterestType.entries.map { it.name }),
            "interestIndex" to enumProperty("For bonds: the index it follows.", InterestIndex.entries.map { it.name }),
            "interestRate" to property("number", "For bonds: the rate, e.g. 110 for 110% of the CDI."),
        )

    override fun validate(payload: BondPayload): ValidationResult =
        when {
            payload.name.isBlank() -> ValidationResult.error("name is required")
            payload.bondType == null ->
                ValidationResult.error(
                    "bondType must be one of ${BondType.entries.map { it.name.lowercase() }}",
                )
            payload.interestRate != null && payload.interestRate.signum() < 0 ->
                ValidationResult.error("interestRate cannot be negative")
            else -> ValidationResult.OK
        }

    override fun summary(payload: BondPayload) =
        "${payload.name}, ${payload.bondType?.name?.lowercase() ?: "?"}" +
            (payload.maturityDate?.let { ", $it" } ?: "")

    override fun draftSpec() = BOND_DRAFT_SPEC

    override fun toDraft(payload: BondPayload) =
        DraftValues(
            name,
            mapOf(
                "bondId" to payload.bondId,
                "name" to payload.name,
                "symbol" to payload.symbol,
                "bondType" to payload.bondType?.name?.lowercase(),
                "issuer" to payload.issuer,
                "maturityDate" to payload.maturityDate,
                "interestType" to payload.interestType?.name?.lowercase(),
                "interestIndex" to payload.interestIndex?.name?.lowercase(),
                "interestRate" to payload.interestRate,
            ),
        )

    override fun fromDraft(draft: DraftValues): BondPayload? {
        val fields = draft.fields
        return BondPayload(
            bondId = fields["bondId"].asInt(),
            name = (fields["name"] as? String)?.trim() ?: return null,
            symbol = fields["symbol"] as? String ?: "",
            bondType = (fields["bondType"] as? String).toEnum<BondType>(),
            issuer = fields["issuer"] as? String ?: "",
            maturityDate = fields["maturityDate"].asDate(),
            interestType = (fields["interestType"] as? String).toEnum<InterestType>(),
            interestIndex = (fields["interestIndex"] as? String).toEnum<InterestIndex>(),
            interestRate = fields["interestRate"].asDecimal(),
        )
    }

    protected fun JSONObject.optDateOrNull(key: String): LocalDate? =
        optString(key).takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }

    protected fun JSONObject.optRateOrNull(key: String): BigDecimal? = if (has(key)) getBigDecimal(key) else null

    protected fun String?.bondType() = toEnum<BondType>()

    protected fun String?.interestType() = toEnum<InterestType>()

    protected fun String?.interestIndex() = toEnum<InterestIndex>()

    protected fun toBond(payload: BondPayload) =
        Bond(
            id = payload.bondId,
            name = payload.name,
            symbol = payload.symbol.takeIf { it.isNotBlank() },
            type = payload.bondType!!,
            issuer = payload.issuer.takeIf { it.isNotBlank() },
            maturityDate = payload.maturityDate,
            interestType = payload.interestType,
            interestIndex = payload.interestIndex,
            interestRate = payload.interestRate,
        )
}

@Component
class CreateBondOperation(
    bondService: BondService,
) : BondManagementOperation(bondService) {
    override val name = "create_bond"
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("name", "bondType")

    override fun modelDescription() =
        "create_bond: register a fixed income asset (name, bondType, optional symbol, issuer, maturityDate, " +
            "interestType, interestIndex and interestRate). It holds no position until an application is recorded."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ) = BondPayload(
        bondId = null,
        name = arguments.getString("name").trim(),
        symbol = arguments.optString("symbol").trim(),
        bondType = arguments.optString("bondType").bondType(),
        issuer = arguments.optString("issuer").trim(),
        maturityDate = arguments.optDateOrNull("maturityDate"),
        interestType = arguments.optString("interestType").interestType(),
        interestIndex = arguments.optString("interestIndex").interestIndex(),
        interestRate = arguments.optRateOrNull("interestRate"),
    )

    override fun validate(payload: BondPayload): ValidationResult {
        val base = super.validate(payload)
        val taken =
            payload.symbol.isNotBlank() &&
                (bondService.getAllNonArchivedBonds() + bondService.getAllArchivedBonds()).any {
                    it.symbol.equals(payload.symbol, ignoreCase = true)
                }
        return if (base.isValid && taken) ValidationResult.error("bond ${payload.symbol} already exists") else base
    }

    override fun execute(payload: BondPayload): ApplyResult.Success {
        bondService.createBond(toBond(payload))
        return ApplyResult.Success(emptyList(), summary(payload))
    }
}

@Component
class UpdateBondOperation(
    bondService: BondService,
) : BondManagementOperation(bondService) {
    override val name = "update_bond"
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("assetName")

    override fun modelDescription() =
        "update_bond: change an existing bond (assetName is its current symbol or name). Send only the fields to " +
            "change; operations and position are untouched."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): BondPayload {
        val given = arguments.getString("assetName")
        val bond = catalog.bond(given) ?: error("bond '$given' not found")
        val stored =
            BondPayload(
                bond.id,
                bond.name,
                bond.symbol.orEmpty(),
                bond.type,
                bond.issuer.orEmpty(),
                bond.maturityDate,
                bond.interestType,
                bond.interestIndex,
                bond.interestRate,
            )
        return stored.copy(
            name = arguments.optString("name").trim().ifBlank { stored.name },
            symbol = arguments.optString("symbol").trim().ifBlank { stored.symbol },
            bondType = arguments.optString("bondType").takeIf { it.isNotBlank() }?.bondType() ?: stored.bondType,
            issuer = arguments.optString("issuer").trim().ifBlank { stored.issuer },
            maturityDate = arguments.optDateOrNull("maturityDate") ?: stored.maturityDate,
            interestType = arguments.optString("interestType").interestType() ?: stored.interestType,
            interestIndex = arguments.optString("interestIndex").interestIndex() ?: stored.interestIndex,
            interestRate = arguments.optRateOrNull("interestRate") ?: stored.interestRate,
            before = summary(stored),
        )
    }

    override fun preview(payload: BondPayload) = payload.before?.let { Preview(listOf(it, "→ " + summary(payload))) }

    override fun execute(payload: BondPayload): ApplyResult.Success {
        bondService.updateBond(toBond(payload))
        return ApplyResult.Success(listOf(payload.bondId!!), summary(payload))
    }
}
