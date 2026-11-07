/*
 * Filename: AiChatService.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.json.JSONObject
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.ProposedChange
import org.moinex.ai.prompt.PromptComposer
import org.moinex.ai.provider.LlmProviderFactory
import org.moinex.ai.tools.FindMatchingTransactionsTool
import org.moinex.ai.tools.LlmTool
import org.moinex.ai.tools.ProposeChangesTool
import org.moinex.ai.tools.QueryFinanceTool
import org.moinex.ai.tools.ReadTools
import org.moinex.common.ClockProvider
import org.moinex.common.constant.TranslationKeys
import org.moinex.config.AiPromptProperties
import org.moinex.model.dto.ai.IgnoredLine
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
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
    private val readTools: ReadTools,
    private val proposeChangesTool: ProposeChangesTool,
    private val aiPromptProperties: AiPromptProperties,
    private val promptComposer: PromptComposer,
    private val preferencesService: PreferencesService,
    private val clockProvider: ClockProvider,
) {
    companion object {
        private const val OLD_RESULT_LIMIT = 2000
        private const val LOG_LIMIT = 4000
        private val logger = LoggerFactory.getLogger(AiChatService::class.java)

        private fun clip(text: String): String =
            if (text.length <= LOG_LIMIT) text else text.take(LOG_LIMIT) + "…[${text.length - LOG_LIMIT} more chars]"
    }

    /** @param onToolCall told the name of each tool just before it runs, so the screen can show what is happening */
    suspend fun sendMessage(
        history: List<ChatMessage>,
        onToolCall: (String) -> Unit = {},
    ): ChatTurnResult {
        val provider = llmProviderFactory.currentProvider() ?: return ChatTurnResult.NoProviderConfigured
        val tools =
            buildList {
                addAll(this@AiChatService.queryFinanceTool.availableTools())
                addAll(this@AiChatService.readTools.tools())
                add(this@AiChatService.findMatchingTransactionsTool)
                add(this@AiChatService.proposeChangesTool)
            }

        var conversation = listOf(systemMessage()) + history
        if (logger.isDebugEnabled) {
            logger.debug(
                "AI turn start: provider={}, history={} messages, tools={}, maxIterations={}",
                provider::class.simpleName,
                history.size,
                tools.map { it.name },
                aiPromptProperties.maxToolIterations,
            )
            logger.debug("AI system prompt:\n{}", conversation.first().content)
            history.lastOrNull()?.let { logger.debug("AI last message ({}): {}", it.role, clip(it.content)) }
        }
        var iterations = 0
        val turnMessages = mutableListOf<ChatMessage>()
        val proposedChanges = mutableListOf<ProposedChange>()
        val ignoredLines = mutableListOf<IgnoredLine>()

        while (true) {
            val response =
                runCatching { provider.chat(conversation, tools) }
                    .onFailure {
                        logger.warn("AI provider call failed in round {}: {}", iterations + 1, it.message)
                        if (it is CancellationException) {
                            currentCoroutineContext().ensureActive()
                        }
                    }.getOrElse {
                        return ChatTurnResult.Error(
                            it.message ?: preferencesService.translate(TranslationKeys.AI_CHAT_SERVICE_UNKNOWN_ERROR),
                        )
                    }

            logger.debug(
                "AI round {}: text={} chars, toolCalls={}",
                iterations + 1,
                response.text.orEmpty().length,
                response.toolCalls.map { it.toolName },
            )

            if (response.toolCalls.isEmpty()) {
                logger.debug("AI final reply: {}", clip(response.text.orEmpty()))
                val changeSet =
                    ChangeSet(UUID.randomUUID().toString(), proposedChanges, ignoredLines)
                        .takeUnless { it.isEmpty }
                turnMessages += ChatMessage(role = ChatRole.ASSISTANT, content = response.text.orEmpty())
                return ChatTurnResult.Success(
                    reply = response.text.orEmpty(),
                    changeSet = changeSet,
                    turnMessages = turnMessages.toList(),
                )
            }

            if (iterations >= aiPromptProperties.maxToolIterations) {
                logger.warn("AI tool iteration limit reached ({})", aiPromptProperties.maxToolIterations)
                val limitMessage = preferencesService.translate(TranslationKeys.AI_CHAT_SERVICE_TOOL_LIMIT_EXCEEDED)
                val changeSet =
                    ChangeSet(UUID.randomUUID().toString(), proposedChanges, ignoredLines).takeUnless { it.isEmpty }
                return if (changeSet != null) {
                    turnMessages += ChatMessage(role = ChatRole.ASSISTANT, content = limitMessage)
                    ChatTurnResult.Success(
                        reply = limitMessage,
                        changeSet = changeSet,
                        turnMessages = turnMessages.toList(),
                    )
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
                    onToolCall(call.toolName)
                    logger.debug(
                        "AI tool call {} (id={}) arguments: {}",
                        call.toolName,
                        call.id,
                        clip(call.argumentsJson),
                    )
                    val result =
                        if (call.toolName == ProposeChangesTool.NAME) {
                            proposeChanges(call.argumentsJson, proposedChanges, ignoredLines)
                        } else {
                            executeTool(tools, call.toolName, call.argumentsJson)
                        }

                    logger.debug("AI tool result {} (id={}): {}", call.toolName, call.id, clip(result))

                    ChatMessage(
                        role = ChatRole.TOOL,
                        content = result,
                        toolCallId = call.id,
                        toolName = call.toolName,
                    )
                }

            turnMessages += assistantMessage
            turnMessages += toolResultMessages
            conversation = compactOldResults(conversation) + assistantMessage + toolResultMessages
            iterations++
        }
    }

    /** Long results of earlier rounds are cut, so a task with many queries does not fill the model's context. */
    private fun compactOldResults(conversation: List<ChatMessage>): List<ChatMessage> =
        conversation.map {
            if (it.role == ChatRole.TOOL && it.content.length > OLD_RESULT_LIMIT) {
                it.copy(content = it.content.take(OLD_RESULT_LIMIT) + "…[truncated]")
            } else {
                it
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
