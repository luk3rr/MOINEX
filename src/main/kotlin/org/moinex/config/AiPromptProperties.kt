/*
 * Filename: AiPromptProperties.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "moinex.ai")
class AiPromptProperties(
    val systemPrompt: Map<String, String> = emptyMap(),
    val attachmentInstructionTemplate: String = "",
)
