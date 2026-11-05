/*
 * Filename: GoalOperations.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation.planning

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
import org.moinex.common.constant.Constants
import org.moinex.model.goal.Goal
import org.moinex.service.goal.GoalService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/** A goal to create or, when [goalId] is set, to change; its balance is never touched by a change. */
data class GoalPayload(
    val goalId: Int?,
    val name: String,
    val targetBalance: BigDecimal,
    val initialBalance: BigDecimal,
    val targetDate: LocalDate,
    val motivation: String,
    val before: String? = null,
)

/** [goalId] is null while the model's goal name matched no goal. */
data class GoalArchivePayload(
    val goalId: Int?,
    val currentName: String,
)

private val GOAL_DRAFT_SPEC =
    listOf(
        DraftField("name", "aidraft.field.name", DraftFieldType.TEXT),
        DraftField("targetBalance", "aidraft.field.targetAmount", DraftFieldType.DECIMAL),
        DraftField("initialBalance", "aidraft.field.initialBalance", DraftFieldType.DECIMAL),
        DraftField("targetDate", "aidraft.field.targetDate", DraftFieldType.DATE),
        DraftField("motivation", "aidraft.field.motivation", DraftFieldType.TEXT, required = false),
    )

/** Shared by the goal creation and change operations. */
abstract class GoalManagementOperation(
    protected val goalService: GoalService,
) : BaseOperation<GoalPayload>() {
    override val domain = OperationDomain.PLANNING

    override fun modelProperties(): Map<String, JSONObject> =
        mapOf(
            "targetAmount" to property("number", "For goals: the amount to reach."),
            "targetDate" to property("string", "For goals and wishlist items: the target date (YYYY-MM-DD)."),
            "motivation" to property("string", "For goals: why the user wants it."),
        )

    override fun validate(payload: GoalPayload): ValidationResult =
        when {
            payload.name.isBlank() -> ValidationResult.error("name is required")
            payload.targetBalance.signum() <= 0 -> ValidationResult.error("targetAmount must be positive")
            payload.initialBalance.signum() < 0 -> ValidationResult.error("initial balance cannot be negative")
            payload.initialBalance > payload.targetBalance ->
                ValidationResult.error("initial balance cannot exceed the target")
            else -> ValidationResult.OK
        }

    override fun summary(payload: GoalPayload) =
        "${payload.name}, ${payload.targetBalance.toPlainString()}, ${payload.targetDate}"

    override fun draftSpec() = GOAL_DRAFT_SPEC

    override fun toDraft(payload: GoalPayload) =
        DraftValues(
            name,
            mapOf(
                "goalId" to payload.goalId,
                "name" to payload.name,
                "targetBalance" to payload.targetBalance,
                "initialBalance" to payload.initialBalance,
                "targetDate" to payload.targetDate,
                "motivation" to payload.motivation,
            ),
        )

    override fun fromDraft(draft: DraftValues): GoalPayload? {
        val fields = draft.fields
        return GoalPayload(
            goalId = fields["goalId"].asInt(),
            name = (fields["name"] as? String)?.trim() ?: return null,
            targetBalance = fields["targetBalance"].asDecimal() ?: return null,
            initialBalance = fields["initialBalance"].asDecimal() ?: BigDecimal.ZERO,
            targetDate = fields["targetDate"].asDate() ?: return null,
            motivation = fields["motivation"] as? String ?: "",
        )
    }
}

@Component
class CreateGoalOperation(
    goalService: GoalService,
    private val walletService: WalletService,
) : GoalManagementOperation(goalService) {
    override val name = "create_goal"
    override val risk = OperationRisk.LOW

    override fun modelRequired() = listOf("name", "targetAmount", "targetDate")

    override fun modelDescription() =
        "create_goal: a savings goal (name, targetAmount, targetDate in the future, optional amount as the " +
            "initial balance and motivation). It is a standalone goal, not funded from another wallet."

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ) = GoalPayload(
        goalId = null,
        name = arguments.getString("name").trim(),
        targetBalance = arguments.getBigDecimal("targetAmount"),
        initialBalance = if (arguments.has("amount")) arguments.getBigDecimal("amount") else BigDecimal.ZERO,
        targetDate = LocalDate.parse(arguments.getString("targetDate")),
        motivation = arguments.optString("motivation"),
    )

    override fun validate(payload: GoalPayload): ValidationResult {
        val base = super.validate(payload)
        return if (base.isValid && !payload.targetDate.isAfter(LocalDate.now())) {
            ValidationResult.error("targetDate must be in the future")
        } else {
            base
        }
    }

    override fun execute(payload: GoalPayload): ApplyResult.Success {
        val type = walletService.getAllWalletTypes().first { it.name == Constants.GOAL_DEFAULT_WALLET_TYPE_NAME }
        val id =
            goalService.createGoal(
                Goal(
                    initialBalance = payload.initialBalance,
                    targetBalance = payload.targetBalance,
                    targetDate = payload.targetDate,
                    motivation = payload.motivation.takeIf { it.isNotBlank() },
                    name = payload.name,
                    type = type,
                ),
            )
        return ApplyResult.Success(listOf(id), summary(payload))
    }
}

@Component
class UpdateGoalOperation(
    goalService: GoalService,
) : GoalManagementOperation(goalService) {
    override val name = "update_goal"
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("goalName")

    override fun modelDescription() =
        "update_goal: change an existing goal (goalName is the current name, name the new one). Send only the " +
            "fields to change; its balance never changes."

    override fun modelProperties(): Map<String, JSONObject> =
        super.modelProperties() + mapOf("goalName" to property("string", "For update_goal and archive_goal."))

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): GoalPayload {
        val current = arguments.getString("goalName")
        val goal =
            goalService
                .getAllGoals()
                .filter { it.name.equals(current.trim(), true) }
                .minByOrNull { it.isArchived }
                ?: error("goal '$current' not found")
        val stored =
            GoalPayload(
                goal.id,
                goal.name,
                goal.targetBalance,
                goal.initialBalance,
                goal.targetDate,
                goal.motivation.orEmpty(),
            )
        return stored.copy(
            name = arguments.optString("name").trim().ifBlank { stored.name },
            targetBalance =
                if (arguments.has("targetAmount")) arguments.getBigDecimal("targetAmount") else stored.targetBalance,
            targetDate =
                arguments.optString("targetDate").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
                    ?: stored.targetDate,
            motivation = if (arguments.has("motivation")) arguments.getString("motivation") else stored.motivation,
            before = summary(stored),
        )
    }

    override fun preview(payload: GoalPayload) = payload.before?.let { Preview(listOf(it, "→ " + summary(payload))) }

    override fun execute(payload: GoalPayload): ApplyResult.Success {
        val goal = goalService.getGoalById(payload.goalId!!)
        goalService.updateGoal(
            Goal(
                initialBalance = goal.initialBalance,
                targetBalance = payload.targetBalance,
                targetDate = payload.targetDate,
                motivation = payload.motivation.takeIf { it.isNotBlank() },
                id = goal.id,
                name = payload.name,
                type = goal.type,
                isArchived = goal.isArchived,
                masterWallet = goal.masterWallet,
            ).also { it.balance = goal.balance },
        )
        return ApplyResult.Success(listOf(goal.id!!), summary(payload))
    }
}

@Component
class ArchiveGoalOperation(
    private val goalService: GoalService,
) : BaseOperation<GoalArchivePayload>() {
    override val name = "archive_goal"
    override val domain = OperationDomain.PLANNING
    override val risk = OperationRisk.MEDIUM

    override fun modelRequired() = listOf("goalName")

    override fun modelDescription() = "archive_goal: archive an existing goal (goalName); nothing is deleted."

    override fun modelProperties(): Map<String, JSONObject> = emptyMap()

    override fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): GoalArchivePayload {
        val current = arguments.getString("goalName")
        val goal = goalService.getAllGoals().firstOrNull { !it.isArchived && it.name.equals(current.trim(), true) }
        return GoalArchivePayload(goal?.id, goal?.name ?: current)
    }

    override fun validate(payload: GoalArchivePayload): ValidationResult =
        if (payload.goalId ==
            null
        ) {
            ValidationResult.error("goal '${payload.currentName}' not found")
        } else {
            ValidationResult.OK
        }

    override fun summary(payload: GoalArchivePayload) = payload.currentName

    override fun execute(payload: GoalArchivePayload): ApplyResult.Success {
        goalService.archiveGoal(payload.goalId!!)
        return ApplyResult.Success(listOf(payload.goalId), payload.currentName)
    }
}
