/*
 * Filename: PromptComposer.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.prompt

import org.springframework.stereotype.Component

/**
 * Builds the system prompt from resource blocks, one `core` block and one per domain, in the user's language.
 * A block missing for the locale falls back to English; a block missing in both is skipped.
 */
@Component
class PromptComposer {
    companion object {
        const val FALLBACK_LOCALE = "en"
        const val CORE_BLOCK = "core"
        val DEFAULT_BLOCKS = listOf("transactions", "credit_card", "investments", "planning")
        private const val BASE_PATH = "/ai/prompts"
    }

    fun compose(
        localeTag: String,
        blocks: List<String> = DEFAULT_BLOCKS,
    ): String =
        (listOf(CORE_BLOCK) + blocks)
            .mapNotNull { block -> read(localeTag.lowercase(), block) ?: read(FALLBACK_LOCALE, block) }
            .joinToString("\n\n")

    private fun read(
        locale: String,
        block: String,
    ): String? =
        PromptComposer::class.java
            .getResourceAsStream("$BASE_PATH/$locale/$block.md")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText().trim() }
}
