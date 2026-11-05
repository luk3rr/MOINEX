/*
 * Filename: AppOperation.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import org.json.JSONObject
import org.moinex.service.matching.MatchQuery

/**
 * One action of the app that the assistant can propose and that, after the user approves it, is applied through the
 * existing domain services. The assistant never calls [apply]; only the review flow does.
 */
interface AppOperation<P : Any> {
    val name: String
    val domain: OperationDomain
    val risk: OperationRisk

    /** One line telling the model when to use this operation, put in the description of the proposal tool. */
    fun modelDescription(): String

    /** The fields of this operation in the proposal tool, by name; the tool merges the fields of all operations. */
    fun modelProperties(): Map<String, JSONObject>

    /** Builds the payload from what the model sent, resolving the names it used to records through [catalog]. */
    fun fromModelArguments(
        arguments: JSONObject,
        catalog: EntityCatalog,
    ): P

    /** Problems that stop the payload from being applied (a missing wallet, a zero amount...). */
    fun validate(payload: P): ValidationResult

    /**
     * What the model left out or got wrong that only the model can fix, such as a zero amount, quantity or price.
     * Unlike [validate], these items are sent back to the model to resend instead of reaching the review panel;
     * a reference the app could not resolve (wallet, card, asset) is left to the user and is not listed here.
     */
    fun missingFromModel(payload: P): List<String> = emptyList()

    /** A short line that identifies the payload to the user. */
    fun summary(payload: P): String

    /** What to look for among stored records to know whether the payload is already recorded; null when never. */
    fun matchQuery(payload: P): MatchQuery? = null

    /** The same payload confirming the pending transaction [existingTransactionId], or null if not applicable. */
    fun confirmingPending(
        payload: P,
        existingTransactionId: Int,
    ): P? = null

    /** The fields the fallback editor shows for this operation. */
    fun draftSpec(): List<DraftField> = emptyList()

    fun toDraft(payload: P): DraftValues? = null

    fun fromDraft(draft: DraftValues): P? = null

    fun preview(payload: P): Preview? = null

    /** Writes through the domain services. A failure is returned, not thrown, so one bad item never stops a batch. */
    fun apply(payload: P): ApplyResult
}
