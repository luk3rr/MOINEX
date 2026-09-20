/*
 * Filename: LlmProviderFactory.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.provider

import org.moinex.ai.credentials.AiCredentialStore
import org.moinex.model.enums.LlmProviderType
import org.springframework.stereotype.Service

@Service
class LlmProviderFactory(
    private val credentialStore: AiCredentialStore,
) {
    /** The provider configured by the user in Settings, or null if none is set up yet. */
    fun currentProvider(): LlmProvider? {
        val type = credentialStore.selectedProvider ?: return null
        val apiKey = credentialStore.getApiKey(type)?.takeIf { it.isNotBlank() } ?: return null
        return buildProvider(type, apiKey, credentialStore.getModel(type))
    }

    /**
     * [model] overrides the provider's own default (e.g. when a provider deprecates its default
     * model id, the user can point at a current one from Settings without a code change/release).
     * Null or blank falls back to that provider's default.
     */
    fun buildProvider(
        type: LlmProviderType,
        apiKey: String,
        model: String? = null,
    ): LlmProvider {
        val trimmedModel = model?.trim()?.takeIf { it.isNotBlank() }

        return when (type) {
            LlmProviderType.OPENAI -> OpenAiProvider(apiKey, trimmedModel ?: OpenAiProvider.DEFAULT_MODEL)
            LlmProviderType.ANTHROPIC -> AnthropicProvider(apiKey, trimmedModel ?: AnthropicProvider.DEFAULT_MODEL)
            LlmProviderType.GEMINI -> GeminiProvider(apiKey, trimmedModel ?: GeminiProvider.DEFAULT_MODEL)
            LlmProviderType.OPENROUTER -> OpenRouterProvider(apiKey, trimmedModel ?: OpenRouterProvider.DEFAULT_MODEL)
        }
    }
}
