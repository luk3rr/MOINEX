/*
 * Filename: LlmTool.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.tools

/**
 * A read-only function exposed to the LLM via tool/function calling.
 */
interface LlmTool {
    val name: String
    val description: String

    /** JSON Schema (as text) describing this tool's parameters, sent to the provider as-is. */
    val jsonSchema: String

    /** Executes the tool and returns a JSON string with the result, to be sent back to the model. */
    fun execute(argumentsJson: String): String
}
