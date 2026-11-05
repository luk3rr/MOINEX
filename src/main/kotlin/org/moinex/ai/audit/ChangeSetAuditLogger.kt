/*
 * Filename: ChangeSetAuditLogger.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.audit

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

enum class AuditState {
    APPROVED,
    REJECTED,
    ERROR,
}

/**
 * One structured INFO line per decision the user takes on an assistant proposal, in the logger
 * `org.moinex.ai.audit`, so what the assistant caused to be written can be traced in the app log.
 */
@Component
class ChangeSetAuditLogger {
    companion object {
        const val LOGGER_NAME = "org.moinex.ai.audit"
        private const val MAX_SUMMARY_LENGTH = 80
    }

    private val logger = LoggerFactory.getLogger(LOGGER_NAME)

    fun record(
        changeSetId: String,
        changeId: String,
        operationName: String,
        state: AuditState,
        ids: List<Int>,
        summary: String,
    ) {
        logger.info(format(changeSetId, changeId, operationName, state, ids, summary))
    }

    fun format(
        changeSetId: String,
        changeId: String,
        operationName: String,
        state: AuditState,
        ids: List<Int>,
        summary: String,
    ): String {
        val shortSummary = summary.replace('"', '\'').replace('\n', ' ').take(MAX_SUMMARY_LENGTH)
        return "changeSetId=$changeSetId changeId=$changeId op=$operationName state=$state " +
            "ids=${ids.joinToString(",")} summary=\"$shortSummary\""
    }
}
