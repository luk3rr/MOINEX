/*
 * Filename: ChangeSet.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import org.moinex.model.dto.ai.IgnoredLine
import org.moinex.service.matching.Match

enum class ChangeBadge {
    NEW,
    MATCHES_EXISTING,
    POSSIBLE_DUPLICATE,
    LOW_CONFIDENCE,
}

enum class ChangeState {
    PENDING,
    EDITED,
    APPROVED,
    REJECTED,
    ERROR,
}

/**
 * One operation the assistant proposed, ready to be reviewed. [payload] is the typed payload of the operation named
 * [operationName]; nothing in it has been written yet. [matchCandidates] lists the stored records it may refer to.
 */
data class ProposedChange(
    val id: String,
    val operationName: String,
    val payload: Any,
    val summary: String,
    val badge: ChangeBadge,
    val reasons: List<String>,
    val confidence: Double,
    val dependsOn: List<String> = emptyList(),
    val matchCandidates: List<Match> = emptyList(),
    val targetId: Int? = null,
)

/** What one turn of the assistant proposed: [changes] to review and [ignored] lines already recorded. */
data class ChangeSet(
    val id: String,
    val changes: List<ProposedChange>,
    val ignored: List<IgnoredLine> = emptyList(),
) {
    val isEmpty: Boolean get() = changes.isEmpty() && ignored.isEmpty()
}
