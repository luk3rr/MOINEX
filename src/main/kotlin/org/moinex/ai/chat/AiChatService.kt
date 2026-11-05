/*
 * Filename: AiChatService.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

import org.json.JSONObject
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.ProposedChange
import org.moinex.ai.prompt.PromptComposer
import org.moinex.ai.provider.LlmProviderFactory
import org.moinex.ai.tools.FindMatchingTransactionsTool
import org.moinex.ai.tools.LlmTool
import org.moinex.ai.tools.ProposeChangesTool
import org.moinex.ai.tools.QueryFinanceTool
import org.moinex.common.ClockProvider
import org.moinex.common.constant.TranslationKeys
import org.moinex.config.AiPromptProperties
import org.moinex.model.dto.ai.IgnoredLine
import org.moinex.service.PreferencesService
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.util.UUID

/**
 * Orchestrates one chat turn: sends the conversation to the configured provider, executes any
 * requested tool calls locally, and loops until the model returns a final text answer.
 */
@Service
class AiChatService(
    private val llmProviderFactory: LlmProviderFactory,
    private val queryFinanceTool: QueryFinanceTool,
    private val findMatchingTransactionsTool: FindMatchingTransactionsTool,
    private val proposeChangesTool: ProposeChangesTool,
    private val aiPromptProperties: AiPromptProperties,
    private val promptComposer: PromptComposer,
    private val preferencesService: PreferencesService,
    private val clockProvider: ClockProvider,
) {
    companion object {
        private const val MAX_TOOL_ITERATIONS = 25
    }

    suspend fun sendMessage(history: List<ChatMessage>): ChatTurnResult {
        val provider = llmProviderFactory.currentProvider() ?: return ChatTurnResult.NoProviderConfigured
        val tools = queryFinanceTool.availableTools() + findMatchingTransactionsTool + proposeChangesTool

        var conversation = listOf(systemMessage()) + history
        var iterations = 0
        val proposedChanges = mutableListOf<ProposedChange>()
        val ignoredLines = mutableListOf<IgnoredLine>()

        while (true) {
            val response =
                runCatching { provider.chat(conversation, tools) }
                    .getOrElse {
                        return ChatTurnResult.Error(
                            it.message ?: preferencesService.translate(TranslationKeys.AI_CHAT_SERVICE_UNKNOWN_ERROR),
                        )
                    }

            if (response.toolCalls.isEmpty()) {
                val changeSet =
                    ChangeSet(UUID.randomUUID().toString(), proposedChanges, ignoredLines)
                        .takeUnless { it.isEmpty }
                return ChatTurnResult.Success(reply = response.text.orEmpty(), changeSet = changeSet)
            }

            if (iterations >= MAX_TOOL_ITERATIONS) {
                val limitMessage = preferencesService.translate(TranslationKeys.AI_CHAT_SERVICE_TOOL_LIMIT_EXCEEDED)
                val changeSet =
                    ChangeSet(UUID.randomUUID().toString(), proposedChanges, ignoredLines).takeUnless { it.isEmpty }
                return if (changeSet != null) {
                    ChatTurnResult.Success(reply = limitMessage, changeSet = changeSet)
                } else {
                    ChatTurnResult.Error(limitMessage)
                }
            }

            val assistantMessage =
                ChatMessage(
                    role = ChatRole.ASSISTANT,
                    content = response.text.orEmpty(),
                    toolCalls = response.toolCalls,
                )

            val toolResultMessages =
                response.toolCalls.map { call ->
                    val result =
                        if (call.toolName == ProposeChangesTool.NAME) {
                            proposeChanges(call.argumentsJson, proposedChanges, ignoredLines)
                        } else {
                            executeTool(tools, call.toolName, call.argumentsJson)
                        }

                    ChatMessage(
                        role = ChatRole.TOOL,
                        content = result,
                        toolCallId = call.id,
                        toolName = call.toolName,
                    )
                }

            conversation = conversation + assistantMessage + toolResultMessages
            iterations++
        }
    }

    fun buildAttachmentMessage(
        attachments: List<ChatAttachment>,
        userNote: String? = null,
    ): String {
        val fileNames = attachments.joinToString(", ") { it.fileName }
        val filesBlock =
            attachments.joinToString("\n\n\n") { attachment ->
                "File \"${attachment.fileName}\":\n\n${attachment.extractedText}"
            }
        val instruction = MessageFormat.format(aiPromptProperties.attachmentInstructionTemplate, fileNames, filesBlock)
        return if (userNote.isNullOrBlank()) instruction else "$userNote\n\n$instruction"
    }

    private fun systemMessage(): ChatMessage {
        val basePrompt = promptComposer.compose(preferencesService.locale.toLanguageTag())

        val context =
            "Today's date: ${clockProvider.today()}\n" + proposeChangesTool.describeAvailableContext()

        return ChatMessage(role = ChatRole.SYSTEM, content = "$basePrompt\n\n$context")
    }

    private fun proposeChanges(
        argumentsJson: String,
        proposedChanges: MutableList<ProposedChange>,
        ignoredLines: MutableList<IgnoredLine>,
    ): String =
        runCatching {
            val outcome = proposeChangesTool.propose(argumentsJson)
            outcome.changeSet.changes.forEach { change ->
                val repeated =
                    proposedChanges.any {
                        it.operationName == change.operationName &&
                            it.payload == change.payload
                    }
                if (!repeated) proposedChanges += change
            }
            ignoredLines.addAll(outcome.changeSet.ignored)
            proposeChangesTool.toModelJson(outcome)
        }.getOrElse { e -> JSONObject().put("error", e.message ?: "tool_execution_failed").toString() }

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
