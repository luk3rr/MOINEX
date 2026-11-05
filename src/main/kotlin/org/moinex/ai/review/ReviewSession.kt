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
import org.springframework.stereotype.Component

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
) {
    val items: ObservableList<ReviewItem> = FXCollections.observableArrayList()

    fun add(changeSet: ChangeSet) {
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
        ordered.forEach { applyOne(it) }
        return ordered
    }

    fun reject(item: ReviewItem) {
        if (!item.isOpen) return
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
        val payload = operationOf(item).fromDraft(draft) ?: return false
        item.payloadProperty.set(payload)
        item.state = ChangeState.EDITED
        item.messageProperty.set("")
        return true
    }

    private fun applyOne(item: ReviewItem) {
        val operation = operationOf(item)
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
            is ApplyResult.Failure -> {
                item.state = ChangeState.ERROR
                item.messageProperty.set(result.message)
                auditLogger.record(
                    item.changeSetId,
                    item.change.id,
                    operation.name,
                    AuditState.ERROR,
                    emptyList(),
                    result.message,
                )
            }
        }
    }

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
