/*
 * Filename: AiChatService.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

import org.json.JSONObject
import org.moinex.ai.provider.LlmProviderFactory
import org.moinex.ai.tools.LlmTool
import org.moinex.ai.tools.QueryFinanceTool
import org.moinex.common.constant.TranslationKeys
import org.moinex.config.AiPromptProperties
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Service

/**
 * Orchestrates one chat turn: sends the conversation to the configured provider, executes any
 * requested tool calls locally, and loops until the model returns a final text answer.
 */
@Service
class AiChatService(
    private val llmProviderFactory: LlmProviderFactory,
    private val queryFinanceTool: QueryFinanceTool,
    private val aiPromptProperties: AiPromptProperties,
    private val preferencesService: PreferencesService,
) {
    companion object {
        private const val MAX_TOOL_ITERATIONS = 5
        private const val FALLBACK_PROMPT_LOCALE = "en"
    }

    suspend fun sendMessage(history: List<ChatMessage>): ChatTurnResult {
        val provider = llmProviderFactory.currentProvider() ?: return ChatTurnResult.NoProviderConfigured
        val tools = queryFinanceTool.availableTools()

        var conversation = listOf(systemMessage()) + history
        var iterations = 0

        while (true) {
            val response =
                runCatching { provider.chat(conversation, tools) }
                    .getOrElse {
                        return ChatTurnResult.Error(
                            it.message ?: preferencesService.translate(TranslationKeys.AI_CHAT_SERVICE_UNKNOWN_ERROR),
                        )
                    }

            if (response.toolCalls.isEmpty()) {
                return ChatTurnResult.Success(response.text.orEmpty())
            }

            if (iterations >= MAX_TOOL_ITERATIONS) {
                return ChatTurnResult.Error(
                    preferencesService.translate(TranslationKeys.AI_CHAT_SERVICE_TOOL_LIMIT_EXCEEDED),
                )
            }

            val assistantMessage =
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = response.text.orEmpty(),
                    toolCalls = response.toolCalls,
                )

            val toolResultMessages =
                response.toolCalls.map { call ->
                    ChatMessage(
                        role = ChatRole.TOOL,
                        content = executeTool(tools, call.toolName, call.argumentsJson),
                        toolCallId = call.id,
                        toolName = call.toolName,
                    )
                }

            conversation = conversation + assistantMessage + toolResultMessages
            iterations++
        }
    }

    /** Picks the system prompt matching the app's current UI language, falling back to English. */
    private fun systemMessage(): ChatMessage {
        val localeKey = preferencesService.locale.toLanguageTag().lowercase()
        val text =
            aiPromptProperties.systemPrompt[localeKey]
                ?: aiPromptProperties.systemPrompt[FALLBACK_PROMPT_LOCALE]
                ?: ""

        return ChatMessage(role = ChatRole.SYSTEM, content = text)
    }

    private fun executeTool(
        tools: List<LlmTool>,
        toolName: String,
        argumentsJson: String,
    ): String {
        val tool = tools.find { it.name == toolName } ?: return JSONObject().put("error", "unknown_tool").toString()

        return runCatching { tool.execute(argumentsJson) }
            .getOrElse { e -> JSONObject().put("error", e.message ?: "tool_execution_failed").toString() }
    }
}
