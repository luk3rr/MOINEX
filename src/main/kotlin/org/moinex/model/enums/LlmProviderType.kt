/*
 * Filename: LlmProviderType.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.enums

/**
 * [displayName] is the provider's own brand name (e.g. "OpenAI") - not user-facing UI copy, so
 * unlike everything else shown in Settings it does not go through i18n.
 */
enum class LlmProviderType(
    val displayName: String,
) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic (Claude)"),
    GEMINI("Google Gemini"),
    OPENROUTER("OpenRouter"),
}
