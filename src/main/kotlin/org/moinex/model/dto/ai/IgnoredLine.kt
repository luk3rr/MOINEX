/*
 * Filename: IgnoredLine.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.dto.ai

import org.moinex.service.matching.MatchTargetType

/**
 * A proposed line that the app skipped because the same record is already stored. [display] is the text shown to
 * the user, already translated; [reasons] explain why the two were considered the same.
 */
data class IgnoredLine(
    val display: String,
    val targetType: MatchTargetType,
    val targetId: Int,
    val reasons: List<String>,
)
