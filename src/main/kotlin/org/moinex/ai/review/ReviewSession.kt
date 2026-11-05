/*
 * Filename: ReviewSession.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.review

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import org.moinex.ai.audit.AuditState
import org.moinex.ai.audit.ChangeSetAuditLogger
import org.moinex.ai.operation.AppOperation
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.ChangeState
import org.moinex.ai.operation.DraftValues
import org.moinex.ai.operation.OperationRegistry
import org.moinex.ai.operation.ProposedChange
import org.moinex.ai.operation.ValidationResult
import org.moinex.common.constant.TranslationKeys
import org.moinex.service.PreferencesService
import org.moinex.service.matching.MatchStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.text.MessageFormat

/** A proposal under review: the change as the assistant made it, plus what the user did with it since. */
class ReviewItem(
    val change: ProposedChange,
    val changeSetId: String,
    selected: Boolean,
) {
    val stateProperty = SimpleObjectProperty(ChangeState.PENDING)
    val payloadProperty = SimpleObjectProperty(change.payload)
    val selectedProperty = SimpleBooleanProperty(selected)
    val messageProperty = SimpleStringProperty("")
    val targetIdProperty = SimpleObjectProperty(change.targetId)

    var state: ChangeState
        get() = stateProperty.get()
        set(value) = stateProperty.set(value)

    val isOpen: Boolean get() =
        state == ChangeState.PENDING ||
            state == ChangeState.EDITED ||
            state == ChangeState.ERROR
}

data class ReviewCounters(
    val pending: Int,
    val approved: Int,
    val rejected: Int,
    val error: Int,
)

/**
 * The review of everything the assistant proposed in the conversation. It holds no JavaFX nodes: the panel observes
 * [items] and the properties of each [ReviewItem]. Nothing is written until [approve] runs an operation, and each
 * decision goes to the audit log.
 */
@Component
class ReviewSession(
    private val registry: OperationRegistry,
    private val auditLogger: ChangeSetAuditLogger,
    private val preferencesService: PreferencesService,
) {
    private val logger = LoggerFactory.getLogger(ReviewSession::class.java)

    val items: ObservableList<ReviewItem> = FXCollections.observableArrayList()

    fun add(changeSet: ChangeSet) {
        logger.debug(
            "Review: change set {} with {} (ignored lines: {})",
            changeSet.id,
            changeSet.changes.map { "${it.operationName}:${it.badge}" },
            changeSet.ignored.size,
        )
        changeSet.changes.forEach { change ->
            val alreadyOpen =
                items.any {
                    it.isOpen &&
                        it.change.operationName == change.operationName &&
                        it.payloadProperty.get() == change.payload
                }
            if (alreadyOpen) return@forEach

            val preselected = change.badge == ChangeBadge.NEW || change.badge == ChangeBadge.MATCHES_EXISTING
            items += ReviewItem(change, changeSet.id, preselected)
        }
    }

    fun clear() = items.clear()

    fun counters(): ReviewCounters =
        ReviewCounters(
            pending = items.count { it.state == ChangeState.PENDING || it.state == ChangeState.EDITED },
            approved = items.count { it.state == ChangeState.APPROVED },
            rejected = items.count { it.state == ChangeState.REJECTED },
            error = items.count { it.state == ChangeState.ERROR },
        )

    fun openCount(): Int = items.count { it.isOpen }

    fun summaryOf(item: ReviewItem): String = operationOf(item).summary(item.payloadProperty.get())

    fun validationOf(item: ReviewItem): ValidationResult = operationOf(item).validate(item.payloadProperty.get())

    fun previewOf(item: ReviewItem) = operationOf(item).preview(item.payloadProperty.get())

    fun draftOf(item: ReviewItem): DraftValues? = operationOf(item).toDraft(item.payloadProperty.get())

    fun draftSpecOf(item: ReviewItem) = operationOf(item).draftSpec()

    /**
     * Applies the given open items, each after the items it depends on, one failing never stopping the rest.
     * @return the items in the order they were attempted
     */
    fun approve(selected: List<ReviewItem>): List<ReviewItem> {
        val ordered = orderByDependencies(selected.filter { it.isOpen })
        logger.debug("Review: approve {}", ordered.map { "${it.change.operationName}:${it.change.id}" })
        ordered.forEach { applyOne(it) }
        return ordered
    }

    fun reject(item: ReviewItem) {
        if (!item.isOpen) return
        logger.debug("Review: reject {} ({})", item.change.id, item.change.operationName)
        item.state = ChangeState.REJECTED
        item.selectedProperty.set(false)
        auditLogger.record(
            item.changeSetId,
            item.change.id,
            item.change.operationName,
            AuditState.REJECTED,
            emptyList(),
            summaryOf(item),
        )
    }

    fun undoReject(item: ReviewItem) {
        if (item.state == ChangeState.REJECTED) item.state = ChangeState.PENDING
    }

    /** Replaces the payload with what the user saved in the editor; false when the values make no valid payload. */
    fun applyDraft(
        item: ReviewItem,
        draft: DraftValues,
    ): Boolean {
        val payload = operationOf(item).fromDraft(draft)
        logger.debug(
            "Review: draft for {} -> {}",
            item.change.id,
            if (payload ==
                null
            ) {
                "rejected (no valid payload)"
            } else {
                "applied"
            },
        )
        payload ?: return false
        item.payloadProperty.set(payload)
        item.state = ChangeState.EDITED
        item.messageProperty.set("")
        return true
    }

    /**
     * Points a "matches existing" proposal at another pending candidate of the same change. Nothing is written; the
     * payload is rebuilt by the operation and the user still has to approve. False when [targetId] is not a pending
     * candidate or the operation cannot confirm it.
     */
    fun retarget(
        item: ReviewItem,
        targetId: Int,
    ): Boolean {
        if (!item.isOpen || item.targetIdProperty.get() == targetId) return false

        val candidate =
            item.change.matchCandidates.firstOrNull { it.targetId == targetId && it.status == MatchStatus.PENDING }
        val payload = candidate?.let { operationOf(item).confirmingPending(item.payloadProperty.get(), targetId) }
        logger.debug(
            "Review: retarget {} to #{} -> {}",
            item.change.id,
            targetId,
            if (payload ==
                null
            ) {
                "refused"
            } else {
                "ok"
            },
        )
        if (payload == null) return false

        item.targetIdProperty.set(targetId)
        item.payloadProperty.set(payload)
        item.messageProperty.set("")
        return true
    }

    private fun applyOne(item: ReviewItem) {
        val operation = operationOf(item)
        val blocker = blockingDependency(item)
        if (blocker != null) {
            fail(
                item,
                operation.name,
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.AIPANEL_DEPENDENCY_BLOCKED),
                    summaryOf(blocker),
                ),
            )
            return
        }
        val result = operation.apply(item.payloadProperty.get())

        when (result) {
            is ApplyResult.Success -> {
                item.state = ChangeState.APPROVED
                item.selectedProperty.set(false)
                item.messageProperty.set("")
                auditLogger.record(
                    item.changeSetId,
                    item.change.id,
                    operation.name,
                    AuditState.APPROVED,
                    result.ids,
                    result.summary,
                )
            }
            is ApplyResult.Failure -> fail(item, operation.name, result.message)
        }
    }

    private fun fail(
        item: ReviewItem,
        operationName: String,
        message: String,
    ) {
        item.state = ChangeState.ERROR
        item.messageProperty.set(message)
        auditLogger.record(item.changeSetId, item.change.id, operationName, AuditState.ERROR, emptyList(), message)
    }

    /** The item this one depends on that is not applied, so that nothing is written before its prerequisite. */
    private fun blockingDependency(item: ReviewItem): ReviewItem? =
        item.change.dependsOn
            .mapNotNull { id -> items.firstOrNull { it.change.id == id } }
            .firstOrNull { it.state != ChangeState.APPROVED }

    private fun orderByDependencies(candidates: List<ReviewItem>): List<ReviewItem> {
        val remaining = candidates.toMutableList()
        val ordered = mutableListOf<ReviewItem>()
        val doneIds = items.filter { it.state == ChangeState.APPROVED }.map { it.change.id }.toMutableSet()

        while (remaining.isNotEmpty()) {
            val ready =
                remaining.filter { item ->
                    item.change.dependsOn.all {
                        it in doneIds ||
                            remaining.none { r -> r.change.id == it }
                    }
                }
            val next = ready.ifEmpty { remaining.toList() }
            ordered += next
            remaining.removeAll(next.toSet())
            doneIds += next.map { it.change.id }
        }
        return ordered
    }

    @Suppress("UNCHECKED_CAST")
    private fun operationOf(item: ReviewItem): AppOperation<Any> =
        registry.find(item.change.operationName) as? AppOperation<Any>
            ?: error("Unknown operation: ${item.change.operationName}")
}
