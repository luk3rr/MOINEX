/*
 * Filename: AiChatHistoryService.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.ai

import com.fasterxml.jackson.core.JsonProcessingException
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.history.ChatPayloadCodec
import org.moinex.ai.history.RawPayload
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.ChangeState
import org.moinex.ai.operation.ProposedChange
import org.moinex.common.ClockProvider
import org.moinex.common.constant.TranslationKeys
import org.moinex.exception.MoinexException
import org.moinex.model.ai.AiChat
import org.moinex.model.ai.AiChatChange
import org.moinex.model.ai.AiChatChangeSet
import org.moinex.model.ai.AiChatMessage
import org.moinex.repository.ai.AiChatChangeRepository
import org.moinex.repository.ai.AiChatChangeSetRepository
import org.moinex.repository.ai.AiChatMessageRepository
import org.moinex.repository.ai.AiChatRepository
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Stores the conversations with the assistant: what was said, in the order the model needs it to carry on, and
 * which of them the user wants to keep. Nothing here logs the content of a conversation.
 */
@Service
class AiChatHistoryService(
    private val chatRepository: AiChatRepository,
    private val messageRepository: AiChatMessageRepository,
    private val changeSetRepository: AiChatChangeSetRepository,
    private val changeRepository: AiChatChangeRepository,
    private val codec: ChatPayloadCodec,
    private val preferencesService: PreferencesService,
    private val clockProvider: ClockProvider,
) {
    companion object {
        private const val MAX_TITLE_LENGTH = 200
        private const val AUTO_TITLE_LENGTH = 60
        private const val SNIPPET_LENGTH = 80
        private const val SNIPPET_LEAD = 24
        private const val DELETE_CHUNK_SIZE = 500
        private const val SUMMARY_LENGTH = 500
        private const val ELLIPSIS = "…"
        private const val UNREADABLE_MESSAGE = "unreadable"
        private val logger = LoggerFactory.getLogger(AiChatHistoryService::class.java)
    }

    @Transactional
    fun createChat(): Int =
        guarded("createChat") {
            val now = clockProvider.now()
            val chat =
                chatRepository.save(
                    AiChat(
                        title = preferencesService.translate(TranslationKeys.AI_HISTORY_DEFAULT_TITLE),
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            chat.id!!
        }

    @Transactional(readOnly = true)
    fun listChats(): List<ChatSummary> =
        guarded("listChats") {
            val counts = messageRepository.countVisibleMessagesByChat().toCountMap()
            val open = changeRepository.countOpenByChat().toCountMap()
            chatRepository.findAllByOrderByUpdatedAtDescIdDesc().map {
                it.toSummary(
                    counts[it.id] ?: 0,
                    open[it.id] ?: 0,
                )
            }
        }

    @Transactional(readOnly = true)
    fun latestChatId(): Int? = guarded("latestChatId") { chatRepository.findFirstByOrderByUpdatedAtDescIdDesc()?.id }

    @Transactional(readOnly = true)
    fun openChat(chatId: Int): StoredConversation =
        guarded("openChat") {
            val chat = findChat(chatId)
            val stored = messageRepository.findByChatIdOrderByPosition(chatId)
            val messages = stored.map { it.toChatMessage() }
            val changeSets = loadChangeSets(stored)
            val open = changeSets.sumOf { set -> set.states.values.count { it.state.isOpen() } }
            StoredConversation(
                chat.toSummary(stored.count { it.role != ChatRole.TOOL.name }, open),
                messages,
                changeSets,
            )
        }

    /**
     * Stores the proposals of the answer [messageId] of the conversation [chatId], all still waiting for a decision.
     * A change set with nothing to review is not stored.
     */
    @Transactional
    fun saveChangeSet(
        chatId: Int,
        messageId: Int,
        changeSet: ChangeSet,
    ) {
        if (changeSet.isEmpty) return

        guarded("saveChangeSet") {
            val message = messageRepository.findById(messageId).orElse(null)
            if (message == null || message.chatId != chatId) {
                throw MoinexException.AiChatHistoryException(
                    preferencesService.translate(TranslationKeys.AI_HISTORY_ERROR_NOT_FOUND),
                )
            }
            changeSetRepository.save(
                AiChatChangeSet(
                    id = changeSet.id,
                    messageId = messageId,
                    ignoredJson = changeSet.ignored.takeIf { it.isNotEmpty() }?.let { codec.ignoredLinesToJson(it) },
                ),
            )
            changeRepository.saveAll(
                changeSet.changes.mapIndexed {
                    index,
                    change,
                    ->
                    change.toEntity(changeSet.id, index)
                },
            )
        }
    }

    /**
     * Records what the user did with a proposal. Approving, rejecting and an error mark the moment of the decision;
     * going back to pending or edited clears it.
     */
    @Transactional
    fun updateChange(
        changeId: String,
        state: ChangeState,
        payload: Any,
        message: String?,
    ) {
        guarded("updateChange") {
            val change =
                changeRepository.findById(changeId).orElseThrow {
                    MoinexException.AiChatHistoryException(
                        preferencesService.translate(TranslationKeys.AI_HISTORY_ERROR_NOT_FOUND),
                    )
                }
            change.state = state.name
            change.payloadJson = codec.payloadToJson(payload)
            change.message = message
            change.decidedAt = if (state.isDecided()) clockProvider.now() else null
            changeRepository.save(change)
        }
    }

    /**
     * Appends [messages] to the end of the conversation and returns their ids in the same order. The first message
     * of an empty conversation names it, when it is a user message. [attachmentNames] go on the first user message.
     */
    @Transactional
    fun appendMessages(
        chatId: Int,
        messages: List<ChatMessage>,
        attachmentNames: List<String> = emptyList(),
    ): List<Int> =
        guarded("appendMessages") {
            if (messages.isEmpty()) return@guarded emptyList()

            val chat = findChat(chatId)
            val now = clockProvider.now()
            val lastPosition = messageRepository.findLastPosition(chatId)
            var attachmentsPending = attachmentNames.isNotEmpty()

            val rows =
                messages.mapIndexed { index, message ->
                    val names =
                        if (attachmentsPending &&
                            message.role == ChatRole.USER
                        ) {
                            attachmentNames
                        } else {
                            emptyList()
                        }
                    if (names.isNotEmpty()) attachmentsPending = false
                    message.toEntity(chatId, lastPosition + 1 + index, names, now)
                }
            val saved = messageRepository.saveAll(rows)

            if (lastPosition < 0) {
                messages.firstOrNull { it.role == ChatRole.USER }?.let { titleFrom(it) }?.let { chat.title = it }
            }
            chat.updatedAt = now
            chatRepository.save(chat)
            saved.map { it.id!! }
        }

    @Transactional
    fun renameChat(
        chatId: Int,
        title: String,
    ) {
        val clean = title.trim()
        if (clean.isEmpty()) {
            throw MoinexException.AiChatHistoryException(
                preferencesService.translate(TranslationKeys.AI_HISTORY_ERROR_EMPTY_TITLE),
            )
        }
        guarded("renameChat") {
            val chat = findChat(chatId)
            chat.title = clean.take(MAX_TITLE_LENGTH)
            chatRepository.save(chat)
        }
    }

    @Transactional
    fun deleteChat(chatId: Int) {
        guarded("deleteChat") { deleteChats(listOf(chatId)) }
    }

    @Transactional
    fun deleteAllChats() {
        guarded("deleteAllChats") { deleteChats(chatRepository.findAll().mapNotNull { it.id }) }
    }

    /**
     * Looks for [text] in the titles and messages, ignoring case for plain letters. Titles come first. Messages of
     * the tool results are not searched. A blank [text] finds nothing.
     */
    @Transactional(readOnly = true)
    fun search(
        text: String,
        limit: Int = 50,
    ): List<SearchHit> {
        val term = text.trim()
        if (term.isEmpty() || limit <= 0) return emptyList()

        return guarded("search") {
            val pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
            val titleHits =
                messageRepository.searchTitles(pattern, limit).map {
                    SearchHit((it[0] as Number).toInt(), it[1] as String, null, 0, it[1] as String)
                }
            val messageHits =
                messageRepository.searchMessages(pattern, limit).map {
                    val displayText = it[4] as String?
                    val content = it[5] as String
                    val source =
                        if (displayText != null &&
                            displayText.contains(term, ignoreCase = true)
                        ) {
                            displayText
                        } else {
                            content
                        }
                    SearchHit(
                        chatId = (it[0] as Number).toInt(),
                        chatTitle = it[1] as String,
                        messageId = (it[2] as Number).toInt(),
                        position = (it[3] as Number).toInt(),
                        snippet = snippetOf(source, term),
                    )
                }
            (titleHits + messageHits).take(limit)
        }
    }

    /** Applies the limits the user set in the preferences. Returns how many conversations were deleted. */
    @Transactional
    fun applyConfiguredRetention(): Int =
        applyRetention(preferencesService.aiHistoryMaxChats, preferencesService.aiHistoryMaxDays)

    /**
     * Deletes the conversations whose last activity is older than [maxDays] days and then the oldest ones beyond
     * [maxChats]. A null or non positive limit means no limit. Returns how many conversations were deleted.
     */
    @Transactional
    fun applyRetention(
        maxChats: Int?,
        maxDays: Int?,
    ): Int =
        guarded("applyRetention") {
            val days = maxDays?.takeIf { it > 0 }
            val count = maxChats?.takeIf { it > 0 }
            if (days == null && count == null) return@guarded 0

            val chats = chatRepository.findAllByOrderByUpdatedAtDescIdDesc()
            val doomed = linkedSetOf<Int>()
            if (days != null) {
                val limit = clockProvider.now().minusDays(days.toLong())
                chats.filter { it.updatedAt.isBefore(limit) }.forEach { doomed += it.id!! }
            }
            if (count != null) {
                chats.drop(count).forEach { doomed += it.id!! }
            }
            deleteChats(doomed.toList())
            doomed.size
        }

    private fun deleteChats(ids: List<Int>) {
        ids.chunked(DELETE_CHUNK_SIZE).forEach { chunk ->
            chatRepository.deleteChangesOfChats(chunk)
            chatRepository.deleteChangeSetsOfChats(chunk)
            chatRepository.deleteMessagesOfChats(chunk)
            chatRepository.deleteAllById(chunk)
        }
    }

    private fun findChat(chatId: Int): AiChat =
        chatRepository.findById(chatId).orElseThrow {
            MoinexException.AiChatHistoryException(
                preferencesService.translate(TranslationKeys.AI_HISTORY_ERROR_NOT_FOUND),
            )
        }

    private fun <T> guarded(
        operation: String,
        block: () -> T,
    ): T =
        try {
            block()
        } catch (e: MoinexException) {
            throw e
        } catch (e: Exception) {
            logger.warn("AI chat history failed in {}: {}", operation, e.message)
            throw MoinexException.AiChatHistoryException(
                preferencesService.translate(TranslationKeys.AI_HISTORY_ERROR_SAVE),
            )
        }

    private fun titleFrom(message: ChatMessage): String? {
        val firstLine =
            (message.displayText ?: message.content)
                .lineSequence()
                .map { it.trim().replace(Regex("\\s+"), " ") }
                .firstOrNull { it.isNotEmpty() } ?: return null
        return if (firstLine.length <=
            AUTO_TITLE_LENGTH
        ) {
            firstLine
        } else {
            firstLine.take(AUTO_TITLE_LENGTH).trimEnd() + ELLIPSIS
        }
    }

    private fun snippetOf(
        source: String,
        term: String,
    ): String {
        val flat = source.replace(Regex("\\s+"), " ").trim()
        val index = flat.indexOf(term, ignoreCase = true)
        if (index < 0) return flat.take(SNIPPET_LENGTH)

        val start = (index - SNIPPET_LEAD).coerceAtLeast(0)
        val end = (start + SNIPPET_LENGTH).coerceAtMost(flat.length)
        return (if (start > 0) ELLIPSIS else "") + flat.substring(start, end) +
            (if (end < flat.length) ELLIPSIS else "")
    }

    private fun List<Array<Any>>.toCountMap(): Map<Int, Int> =
        associate { (it[0] as Number).toInt() to (it[1] as Number).toInt() }

    private fun ChangeState.isOpen() =
        this == ChangeState.PENDING || this == ChangeState.EDITED || this == ChangeState.ERROR

    private fun ChangeState.isDecided() =
        this == ChangeState.APPROVED || this == ChangeState.REJECTED || this == ChangeState.ERROR

    private fun AiChat.toSummary(
        messageCount: Int,
        pendingProposals: Int = 0,
    ) = ChatSummary(id!!, title, updatedAt, messageCount, pendingProposals)

    private fun loadChangeSets(messages: List<AiChatMessage>): List<StoredChangeSet> {
        val indexByMessageId = messages.withIndex().associate { it.value.id!! to it.index }
        if (indexByMessageId.isEmpty()) return emptyList()

        val sets = changeSetRepository.findByMessageIdIn(indexByMessageId.keys)
        if (sets.isEmpty()) return emptyList()

        val changesBySet =
            changeRepository
                .findByChangeSetIdInOrderByPosition(
                    sets.map { it.id },
                ).groupBy { it.changeSetId }
        return sets
            .sortedBy { indexByMessageId.getValue(it.messageId) }
            .map { set ->
                val rows = changesBySet[set.id].orEmpty()
                val proposed = rows.map { it.toProposedChange() }
                StoredChangeSet(
                    messageIndex = indexByMessageId.getValue(set.messageId),
                    changeSet = ChangeSet(set.id, proposed, set.ignoredJson?.let { readIgnored(it) } ?: emptyList()),
                    states = rows.associate { it.id to it.toStoredState() },
                )
            }
    }

    private fun readIgnored(json: String) =
        try {
            codec.ignoredLinesFromJson(json)
        } catch (e: JsonProcessingException) {
            logger.warn("Stored ignored lines cannot be read: {}", e.javaClass.simpleName)
            emptyList()
        }

    private fun ProposedChange.toEntity(
        changeSetId: String,
        position: Int,
    ): AiChatChange {
        val json = codec.payloadToJson(payload)
        return AiChatChange(
            id = id,
            changeSetId = changeSetId,
            position = position,
            operationName = operationName,
            payloadJson = json,
            originalPayloadJson = json,
            summary = summary.take(SUMMARY_LENGTH),
            badge = badge.name,
            reasonsJson = codec.stringsToJson(reasons),
            confidence = confidence,
            dependsOnJson = codec.stringsToJson(dependsOn),
            matchCandidatesJson = codec.matchesToJson(matchCandidates),
            targetId = targetId,
            state = ChangeState.PENDING.name,
        )
    }

    private fun AiChatChange.toProposedChange() =
        ProposedChange(
            id = id,
            operationName = operationName,
            payload = codec.payloadFromJson(operationName, originalPayloadJson),
            summary = summary.orEmpty(),
            badge = ChangeBadge.valueOf(badge),
            reasons = reasonsJson?.let { codec.stringsFromJson(it) } ?: emptyList(),
            confidence = confidence,
            dependsOn = dependsOnJson?.let { codec.stringsFromJson(it) } ?: emptyList(),
            matchCandidates = matchCandidatesJson?.let { codec.matchesFromJson(it) } ?: emptyList(),
            targetId = targetId,
        )

    private fun AiChatChange.toStoredState(): StoredChangeState {
        val payload = codec.payloadFromJson(operationName, payloadJson)
        val stored = ChangeState.valueOf(state)
        return if (payload is RawPayload && stored != ChangeState.APPROVED && stored != ChangeState.REJECTED) {
            StoredChangeState(ChangeState.ERROR, payload, message ?: UNREADABLE_MESSAGE, decidedAt)
        } else {
            StoredChangeState(stored, payload, message, decidedAt)
        }
    }

    private fun ChatMessage.toEntity(
        chatId: Int,
        position: Int,
        attachmentNames: List<String>,
        createdAt: LocalDateTime,
    ) = AiChatMessage(
        chatId = chatId,
        position = position,
        role = role.name,
        content = content,
        displayText = displayText,
        attachmentNames = attachmentNames.takeIf { it.isNotEmpty() }?.let { codec.stringsToJson(it) },
        toolCallsJson = toolCalls.takeIf { it.isNotEmpty() }?.let { codec.toolCallsToJson(it) },
        toolCallId = toolCallId,
        toolName = toolName,
        responseTimeMillis = responseTimeMillis,
        createdAt = createdAt,
    )

    private fun AiChatMessage.toChatMessage() =
        ChatMessage(
            role = ChatRole.valueOf(role),
            content = content,
            displayText = displayText,
            toolCalls = toolCallsJson?.let { codec.toolCallsFromJson(it) } ?: emptyList(),
            toolCallId = toolCallId,
            toolName = toolName,
            responseTimeMillis = responseTimeMillis,
        )
}
