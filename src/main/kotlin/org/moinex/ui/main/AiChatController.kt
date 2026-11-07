/*
 * Filename: AiChatController.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main

import javafx.animation.KeyFrame
import javafx.animation.PauseTransition
import javafx.animation.Timeline
import javafx.collections.ListChangeListener
import javafx.concurrent.Worker
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.TextInputDialog
import javafx.scene.control.Tooltip
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.FlowPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.scene.shape.Rectangle
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.stage.FileChooser
import javafx.util.Duration
import org.moinex.ai.chat.AiChatService
import org.moinex.ai.chat.ChatAttachment
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ChatTurnResult
import org.moinex.ai.chat.EmojiRenderer
import org.moinex.ai.chat.MarkdownRenderer
import org.moinex.ai.chat.ToolLabels
import org.moinex.ai.chat.isShownInChat
import org.moinex.ai.ingest.StatementIngestService
import org.moinex.ai.operation.DraftChoices
import org.moinex.ai.review.ReviewItem
import org.moinex.ai.review.ReviewSession
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.ui.Icons
import org.moinex.common.ui.ThemeColors
import org.moinex.common.ui.icon
import org.moinex.common.util.FxUtils
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.exception.MoinexException
import org.moinex.model.dto.ai.IgnoredLine
import org.moinex.model.enums.RecurringTransactionFrequency
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.ai.AiChatHistoryService
import org.moinex.service.ai.ChatConversationState
import org.moinex.service.ai.ChatSummary
import org.moinex.service.ai.PersistOutcome
import org.moinex.service.ai.SearchHit
import org.moinex.service.ai.StoredChangeSet
import org.moinex.service.ai.StoredConversation
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.WalletService
import org.moinex.ui.common.ai.DraftChoice
import org.moinex.ui.common.ai.DraftDialogLauncher
import org.moinex.ui.common.ai.DraftEditorDialog
import org.moinex.ui.common.ai.ReviewPanel
import org.moinex.ui.dialog.ai.FilePasswordController
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.text.MessageFormat
import java.util.Base64

@Controller
class AiChatController(
    private val aiChatService: AiChatService,
    private val statementIngestService: StatementIngestService,
    private val preferencesService: PreferencesService,
    private val springContext: ConfigurableApplicationContext,
    private val reviewSession: ReviewSession,
    private val conversationState: ChatConversationState,
    private val historyService: AiChatHistoryService,
    private val draftLauncher: DraftDialogLauncher,
    private val walletService: WalletService,
    private val creditCardService: CreditCardService,
    private val categoryService: CategoryService,
    private val tickerService: TickerService,
    private val bondService: BondService,
) {
    @FXML
    private lateinit var messagesScrollPane: ScrollPane

    @FXML
    private lateinit var messagesContainer: VBox

    @FXML
    private lateinit var inputField: TextArea

    @FXML
    private lateinit var sendButton: Button

    @FXML
    private lateinit var attachButton: Button

    @FXML
    private lateinit var attachmentsContainer: FlowPane

    @FXML
    private lateinit var thinkingLabel: Label

    @FXML
    private lateinit var reviewContainer: VBox

    @FXML
    private lateinit var reopenReviewButton: Button

    @FXML
    private lateinit var conversationsPane: VBox

    @FXML
    private lateinit var collapsedPane: VBox

    @FXML
    private lateinit var collapseButton: Button

    @FXML
    private lateinit var expandButton: Button

    @FXML
    private lateinit var collapsedNewButton: Button

    @FXML
    private lateinit var newChatButton: Button

    @FXML
    private lateinit var searchField: TextField

    @FXML
    private lateinit var conversationsList: ListView<ConversationRow>

    @FXML
    private lateinit var deleteAllButton: Button

    @FXML
    private lateinit var chatTitleLabel: Label

    @FXML
    private lateinit var renameButton: Button

    @FXML
    private lateinit var deleteButton: Button

    private var reviewPanel: ReviewPanel? = null
    private var reviewHidden = false

    private val rowsByPosition = mutableMapOf<Int, Node>()
    private var showEmptyConversation = false
    private var searchPause: PauseTransition? = null
    private var knownChats: List<ChatSummary> = emptyList()

    /** What the list beside the chat shows: a stored conversation, or a message that matched the search. */
    sealed interface ConversationRow {
        data class Chat(
            val summary: ChatSummary,
        ) : ConversationRow

        data class Hit(
            val hit: SearchHit,
        ) : ConversationRow
    }

    private data class TurnOutcome(
        val result: ChatTurnResult,
        val elapsedMillis: Long,
        val persistFailure: String?,
    )

    private val pendingAttachments = mutableListOf<ChatAttachment>()
    private var busy = false
    private var requestStartNanos = 0L

    @Volatile
    private var currentActivity: String? = null
    private var thinkingTimeline: Timeline? = null

    private var draftInput: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(AiChatController::class.java)
        private const val BUBBLE_MAX_WIDTH = 480.0
        private const val INPUT_MAX_ROWS = 5
        private const val STYLE_INPUT_GROWING = "ai-chat-input-growing"
        private const val MIN_BUBBLE_HEIGHT = 30.0
        private const val MAX_BUBBLE_HEIGHT = 2000.0
        private const val BUBBLE_CORNER_RADIUS = 28.0

        private const val STYLE_BUBBLE = "ai-chat-bubble"
        private const val STYLE_BUBBLE_USER = "ai-chat-bubble-user"
        private const val STYLE_BUBBLE_ASSISTANT = "ai-chat-bubble-assistant"
        private const val STYLE_BUBBLE_NOTICE = "ai-chat-bubble-notice"
        private const val STYLE_ATTACH_BUTTON = "ai-chat-attach-button"
        private const val STYLE_BUBBLE_META = "ai-chat-bubble-meta"
        private const val STYLE_ATTACHMENT_CHIP = "ai-chat-attachment-chip"
        private const val STYLE_ATTACHMENT_LABEL = "ai-chat-attachment-label"
        private const val STYLE_ATTACHMENT_REMOVE = "ai-chat-attachment-remove"
        private const val STYLE_HISTORY_TITLE = "ai-history-row-title"
        private const val STYLE_HISTORY_META = "ai-history-row-meta"
        private const val STYLE_HISTORY_BADGE = "ai-history-row-badge"
        private const val STYLE_HISTORY_EMPTY = "ai-history-empty"
        private const val HISTORY_ROW_TEXT_WIDTH = 190.0
        private const val SEARCH_DEBOUNCE_MILLIS = 300.0
        private const val SCROLL_SETTLE_MILLIS = 700.0

        private const val THINKING_TICK_MILLIS = 1000.0
        private const val THINKING_MAX_DOTS = 3
        private const val TICKS_PER_WORD = 4
        private const val NANOS_PER_MILLI = 1_000_000L
        private const val MILLIS_PER_SECOND = 1000L
        private const val SECONDS_PER_MINUTE = 60L
    }

    @FXML
    fun initialize() {
        setupReviewPanel()
        setBusy(busy)
        inputField.text = draftInput
        inputField.textProperty().addListener { _, _, newValue -> draftInput = newValue }
        inputField.styleClass.add(STYLE_INPUT_GROWING)
        inputField.addEventFilter(KeyEvent.KEY_PRESSED) { event ->
            if (event.code == KeyCode.ENTER) {
                event.consume()
                if (event.isShiftDown) inputField.insertText(inputField.caretPosition, "\n") else handleSend()
            }
        }
        inputField.textProperty().addListener { _, _, text ->
            val lines = text.count { it == '\n' } + 1
            inputField.prefRowCount = lines.coerceIn(1, INPUT_MAX_ROWS)
            val grows = lines <= INPUT_MAX_ROWS
            if (grows) {
                if (STYLE_INPUT_GROWING !in inputField.styleClass) inputField.styleClass.add(STYLE_INPUT_GROWING)
            } else {
                inputField.styleClass.remove(STYLE_INPUT_GROWING)
            }
        }
        refreshAttachmentsUI()
        setupHistoryColumn()
        if (conversationState.currentChatId() == null &&
            conversationState.messages().isEmpty() &&
            !showEmptyConversation
        ) {
            openLatestConversation()
        } else {
            renderMessages(conversationState.messages(), emptyList(), null)
            refreshConversationList()
        }
        updateHeader()
    }

    @FXML
    fun handleSend() {
        val text = inputField.text?.trim().orEmpty()
        val attachments = pendingAttachments.toList()
        if (text.isBlank() && attachments.isEmpty()) return

        val messageContent =
            if (attachments.isNotEmpty()) {
                aiChatService.buildAttachmentMessage(attachments, text.ifBlank { null })
            } else {
                text
            }
        val attachmentsSummary = attachments.joinToString(", ") { it.fileName }
        val bubbleText =
            when {
                attachments.isNotEmpty() && text.isNotBlank() -> "$text\n\n📎 $attachmentsSummary"
                attachments.isNotEmpty() -> "📎 $attachmentsSummary"
                else -> text
            }

        appendBubble(ChatRole.USER, bubbleText)
        val userMessage = ChatMessage(role = ChatRole.USER, content = messageContent, displayText = bubbleText)
        val attachmentNames = attachments.map { it.fileName }
        showEmptyConversation = false
        inputField.clear()
        clearAttachments()
        requestStartNanos = System.nanoTime()
        currentActivity = null
        setBusy(true)

        FxUtils.launchBackgroundThenUI(
            background = {
                val startOutcome = conversationState.startTurn(userMessage, attachmentNames)
                val result =
                    aiChatService.sendMessage(conversationState.messages()) { toolName ->
                        currentActivity = ToolLabels.of(toolName, preferencesService)
                    }
                val elapsedMillis = (System.nanoTime() - requestStartNanos) / NANOS_PER_MILLI
                val finishOutcome =
                    if (result is ChatTurnResult.Success) {
                        conversationState.finishTurn(result, elapsedMillis)
                    } else {
                        PersistOutcome.Saved
                    }
                val failure =
                    listOf(
                        startOutcome,
                        finishOutcome,
                    ).filterIsInstance<PersistOutcome.Failed>().firstOrNull()
                TurnOutcome(result, elapsedMillis, failure?.message)
            },
            onUI = { outcome ->
                handleResult(outcome.result, outcome.elapsedMillis)
                outcome.persistFailure?.let { appendNotice(it) }
                updateHeader()
                refreshConversationList()
            },
        )
    }

    @FXML
    fun handleAttach() {
        val chooser =
            FileChooser().apply {
                extensionFilters.add(
                    FileChooser.ExtensionFilter(
                        preferencesService.translate(TranslationKeys.AI_CHAT_ATTACH_FILE_FILTER),
                        listOf("*.csv", "*.pdf"),
                    ),
                )
            }
        val files: List<File>? = chooser.showOpenMultipleDialog(attachButton.scene.window)
        if (files.isNullOrEmpty()) return

        setAttaching(true)

        FxUtils.launchOnFxThread {
            try {
                files.forEach { file -> attachFile(file) }
            } finally {
                setAttaching(false)
            }
        }
    }

    private suspend fun attachFile(file: File) {
        var password: String? = null
        var result = extractText(file, password)

        while (result.exceptionOrNull() is MoinexException.PasswordProtectedFileException) {
            password = promptPassword(file.name, wrongPassword = password != null) ?: return
            result = extractText(file, password)
        }

        result.fold(
            onSuccess = { extractedText -> addAttachment(file.name, extractedText) },
            onFailure = { e ->
                appendNotice(
                    MessageFormat.format(
                        preferencesService.translate(TranslationKeys.AI_CHAT_ATTACH_ERROR),
                        "${file.name}: ${e.message}",
                    ),
                )
            },
        )
    }

    private suspend fun extractText(
        file: File,
        password: String?,
    ): Result<String> =
        FxUtils.withBackground { runCatching { statementIngestService.extractText(file.toPath(), password) } }

    private fun promptPassword(
        fileName: String,
        wrongPassword: Boolean,
    ): String? {
        val headerKey =
            if (wrongPassword) {
                TranslationKeys.AI_CHAT_ATTACH_PASSWORD_WRONG_HEADER
            } else {
                TranslationKeys.AI_CHAT_ATTACH_PASSWORD_HEADER
            }

        var dialog: FilePasswordController? = null

        WindowUtils.openModalWindow(
            Files.FILE_PASSWORD_FXML,
            preferencesService.translate(TranslationKeys.AI_CHAT_ATTACH_PASSWORD_TITLE),
            springContext,
            { controller: FilePasswordController ->
                controller.setMessage(MessageFormat.format(preferencesService.translate(headerKey), fileName))
                dialog = controller
            },
        )

        return dialog?.password
    }

    private fun addAttachment(
        fileName: String,
        extractedText: String,
    ) {
        pendingAttachments.add(ChatAttachment(fileName, extractedText))
        refreshAttachmentsUI()
    }

    private fun removeAttachment(attachment: ChatAttachment) {
        pendingAttachments.remove(attachment)
        refreshAttachmentsUI()
    }

    private fun clearAttachments() {
        pendingAttachments.clear()
        refreshAttachmentsUI()
    }

    private fun refreshAttachmentsUI() {
        attachmentsContainer.children.setAll(pendingAttachments.map { attachmentChip(it) })
        val hasAttachments = pendingAttachments.isNotEmpty()
        attachmentsContainer.isVisible = hasAttachments
        attachmentsContainer.isManaged = hasAttachments
    }

    private fun attachmentChip(attachment: ChatAttachment): HBox {
        val label =
            Label("📎 ${attachment.fileName}").apply {
                styleClass.add(STYLE_ATTACHMENT_LABEL)
                tooltip = Tooltip(attachment.fileName)
            }
        val removeButton =
            Button("✕").apply {
                styleClass.add(STYLE_ATTACHMENT_REMOVE)
                setOnAction { removeAttachment(attachment) }
            }
        return HBox(6.0, label, removeButton).apply {
            alignment = Pos.CENTER_LEFT
            styleClass.add(STYLE_ATTACHMENT_CHIP)
        }
    }

    private fun setAttaching(attaching: Boolean) {
        attachButton.isDisable = attaching
    }

    private fun handleResult(
        result: ChatTurnResult,
        elapsedMillis: Long,
    ) {
        setBusy(false)

        when (result) {
            is ChatTurnResult.Success -> {
                val changeSet = result.changeSet
                val hasProposals = changeSet?.changes?.isNotEmpty() == true
                if (result.reply.isNotBlank() || !hasProposals) {
                    appendBubble(ChatRole.ASSISTANT, result.reply, elapsedMillis)
                }
                if (changeSet != null) {
                    if (changeSet.ignored.isNotEmpty()) {
                        appendNotice(ignoredLinesText(changeSet.ignored))
                    }
                    if (hasProposals) {
                        reviewSession.add(changeSet)
                        reviewHidden = false
                        updateReviewVisibility()
                    }
                }
            }
            is ChatTurnResult.NoProviderConfigured ->
                appendNotice(preferencesService.translate(TranslationKeys.AI_CHAT_NO_PROVIDER_CONFIGURED))
            is ChatTurnResult.Error -> {
                logger.warn("AI chat turn failed: {}", result.message)
                appendNotice(
                    MessageFormat.format(preferencesService.translate(TranslationKeys.AI_CHAT_ERROR), result.message),
                )
            }
        }
    }

    private fun ignoredLinesText(ignored: List<IgnoredLine>): String =
        MessageFormat.format(preferencesService.translate(TranslationKeys.AI_CHAT_IGNORED_HEADER), ignored.size) +
            "\n\n" + ignored.joinToString("\n\n") { it.display }

    private fun setupReviewPanel() {
        val panel =
            ReviewPanel(
                session = reviewSession,
                preferencesService = preferencesService,
                onEdit = ::editProposal,
                onHide = {
                    reviewHidden = true
                    updateReviewVisibility()
                },
            )
        reviewPanel = panel
        reviewSession.onItemChanged = ::persistItemChange
        VBox.setVgrow(panel, Priority.ALWAYS)
        panel.maxHeight = Double.MAX_VALUE
        reviewContainer.children.setAll(panel)
        reviewSession.items.addListener(ListChangeListener { updateReviewVisibility() })
        reviewSession.items.forEach { item -> item.stateProperty.addListener { _, _, _ -> updateReviewVisibility() } }
        reopenReviewButton.setOnAction {
            reviewHidden = false
            updateReviewVisibility()
        }
        updateReviewVisibility()
    }

    private fun updateReviewVisibility() {
        val hasItems = reviewSession.items.isNotEmpty()
        val showPanel = hasItems && !reviewHidden
        reviewContainer.isVisible = showPanel
        reviewContainer.isManaged = showPanel

        val showReopen = hasItems && reviewHidden
        reopenReviewButton.isVisible = showReopen
        reopenReviewButton.isManaged = showReopen
        reopenReviewButton.text =
            MessageFormat.format(
                preferencesService.translate(TranslationKeys.AIPANEL_REOPEN),
                reviewSession.openCount(),
            )
    }

    private fun persistItemChange(item: ReviewItem) {
        runCatching {
            historyService.updateChange(
                item.change.id,
                item.state,
                item.payloadProperty.get(),
                item.messageProperty.get().ifBlank { null },
            )
        }.onFailure {
            logger.warn("AI proposal state could not be stored: {}", it.javaClass.simpleName)
            appendNotice(it.message ?: preferencesService.translate(TranslationKeys.AI_HISTORY_ERROR_SAVE))
        }
    }

    private fun editProposal(item: ReviewItem) {
        val initial = reviewSession.draftOf(item) ?: return
        if (draftLauncher.open(initial) { reviewSession.applyDraft(item, it) }) return

        val edited =
            DraftEditorDialog(preferencesService, ::draftChoices).show(
                preferencesService.translate(TranslationKeys.AIPANEL_DRAFT_TITLE),
                reviewSession.draftSpecOf(item),
                initial,
            ) ?: return
        reviewSession.applyDraft(item, edited)
    }

    private fun draftChoices(kind: DraftChoices): List<DraftChoice> =
        when (kind) {
            DraftChoices.WALLETS ->
                walletService.getAllNonArchivedWalletsOrderedByName().map { DraftChoice(it.id, it.name) }
            DraftChoices.CREDIT_CARDS ->
                creditCardService.getAllNonArchivedCreditCardsOrderedByName().map { DraftChoice(it.id, it.name) }
            DraftChoices.CATEGORIES ->
                categoryService.getNonArchivedCategoriesOrderedByName().map { DraftChoice(it.name, it.name) }
            DraftChoices.TICKERS ->
                tickerService.getAllNonArchivedTickers().map { DraftChoice(it.id, "${it.symbol} - ${it.name}") }
            DraftChoices.BONDS -> bondService.getAllNonArchivedBonds().map { DraftChoice(it.id, it.name) }
            DraftChoices.FREQUENCIES ->
                RecurringTransactionFrequency.entries.map {
                    DraftChoice(it.name.lowercase(), UIUtils.translateRecurringTransactionFrequency(it))
                }
            DraftChoices.WALLET_TYPES ->
                walletService.getAllWalletTypes().map { DraftChoice(it.name, it.name) }
            DraftChoices.MOVEMENT_TYPES ->
                listOf(
                    DraftChoice("income", preferencesService.translate(TranslationKeys.TRANSACTION_TYPE_INCOME)),
                    DraftChoice("expense", preferencesService.translate(TranslationKeys.TRANSACTION_TYPE_EXPENSE)),
                )
        }

    @FXML
    fun handleNewChat() {
        if (busy) return
        conversationState.reset()
        showEmptyConversation = true
        reviewSession.clear()
        updateReviewVisibility()
        renderMessages(emptyList(), emptyList(), null)
        conversationsList.selectionModel.clearSelection()
        updateHeader()
    }

    @FXML
    fun handleCollapse() = applyCollapsed(true)

    @FXML
    fun handleExpand() = applyCollapsed(false)

    @FXML
    fun handleRename() {
        val chatId = conversationState.currentChatId() ?: return
        val dialog =
            TextInputDialog(chatTitleLabel.text).apply {
                title = preferencesService.translate(TranslationKeys.AI_HISTORY_RENAME_TITLE)
                headerText = preferencesService.translate(TranslationKeys.AI_HISTORY_RENAME_HEADER)
                contentText = ""
            }
        val newTitle = dialog.showAndWait().orElse(null)?.trim()
        if (newTitle.isNullOrEmpty()) return

        runHistory({ historyService.renameChat(chatId, newTitle) }) { refreshConversationList() }
    }

    @FXML
    fun handleDelete() {
        val chatId = conversationState.currentChatId() ?: return
        val confirmed =
            WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.AI_HISTORY_DELETE_HEADER),
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.AI_HISTORY_DELETE_MESSAGE),
                    chatTitleLabel.text,
                ),
            )
        if (!confirmed) return

        runHistory({ historyService.deleteChat(chatId) }) {
            conversationState.reset()
            reviewSession.clear()
            updateReviewVisibility()
            openLatestConversation()
        }
    }

    @FXML
    fun handleDeleteAll() {
        val confirmed =
            WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.AI_HISTORY_DELETE_ALL_HEADER),
                preferencesService.translate(TranslationKeys.AI_HISTORY_DELETE_ALL_MESSAGE),
            )
        if (!confirmed) return

        runHistory({ historyService.deleteAllChats() }) {
            conversationState.reset()
            showEmptyConversation = true
            reviewSession.clear()
            updateReviewVisibility()
            renderMessages(emptyList(), emptyList(), null)
            refreshConversationList()
        }
    }

    private fun setupHistoryColumn() {
        collapseButton.graphic = icon(Icons.ARROW_LEFT)
        collapseButton.tooltip = Tooltip(preferencesService.translate(TranslationKeys.AI_HISTORY_SIDEBAR_COLLAPSE))
        expandButton.graphic = icon(Icons.ARROW_RIGHT)
        expandButton.tooltip = Tooltip(preferencesService.translate(TranslationKeys.AI_HISTORY_SIDEBAR_EXPAND))
        collapsedNewButton.graphic = icon(Icons.ADD)
        collapsedNewButton.tooltip = Tooltip(preferencesService.translate(TranslationKeys.AI_HISTORY_SIDEBAR_NEW))
        renameButton.graphic = icon(Icons.EDIT)
        renameButton.tooltip = Tooltip(preferencesService.translate(TranslationKeys.AI_HISTORY_CHAT_RENAME))
        deleteButton.graphic = icon(Icons.TRASH)
        deleteButton.tooltip = Tooltip(preferencesService.translate(TranslationKeys.AI_HISTORY_CHAT_DELETE))
        applyCollapsed(preferencesService.aiChatSidebarCollapsed, remember = false)

        conversationsList.placeholder =
            Label(preferencesService.translate(TranslationKeys.AI_HISTORY_SIDEBAR_EMPTY)).apply {
                styleClass.add(STYLE_HISTORY_EMPTY)
            }
        conversationsList.setCellFactory {
            object : ListCell<ConversationRow>() {
                override fun updateItem(
                    item: ConversationRow?,
                    empty: Boolean,
                ) {
                    super.updateItem(item, empty)
                    text = null
                    graphic = if (empty || item == null) null else rowGraphic(item)
                }
            }
        }
        conversationsList.setOnMouseClicked { event ->
            val cell =
                generateSequence(
                    event.target as? Node,
                ) { it.parent }.filterIsInstance<ListCell<*>>().firstOrNull()
            val row = conversationsList.selectionModel.selectedItem
            if (cell != null && !cell.isEmpty && row != null && !busy) openRow(row)
        }
        searchField.textProperty().addListener { _, _, text ->
            searchPause?.stop()
            searchPause =
                PauseTransition(Duration.millis(SEARCH_DEBOUNCE_MILLIS)).also {
                    it.setOnFinished { runSearch(text.trim()) }
                    it.play()
                }
        }
    }

    private fun applyCollapsed(
        collapsed: Boolean,
        remember: Boolean = true,
    ) {
        if (remember) preferencesService.aiChatSidebarCollapsed = collapsed
        conversationsPane.isVisible = !collapsed
        conversationsPane.isManaged = !collapsed
        collapsedPane.isVisible = collapsed
        collapsedPane.isManaged = collapsed
    }

    private fun rowGraphic(row: ConversationRow): Node =
        when (row) {
            is ConversationRow.Chat -> {
                val summary = row.summary
                val meta =
                    Label(
                        UIUtils.formatDateTimeForDisplay(summary.updatedAt),
                    ).apply { styleClass.add(STYLE_HISTORY_META) }
                val line = HBox(6.0, meta)
                if (summary.pendingProposals > 0) {
                    line.children.add(
                        Label(
                            MessageFormat.format(
                                preferencesService.translate(TranslationKeys.AI_HISTORY_SIDEBAR_PENDING),
                                summary.pendingProposals,
                            ),
                        ).apply { styleClass.add(STYLE_HISTORY_BADGE) },
                    )
                }
                VBox(2.0, historyTitle(summary.title), line)
            }
            is ConversationRow.Hit ->
                VBox(
                    2.0,
                    historyTitle(row.hit.chatTitle),
                    Label(row.hit.snippet).apply {
                        styleClass.add(STYLE_HISTORY_META)
                        isWrapText = false
                        maxWidth = HISTORY_ROW_TEXT_WIDTH
                    },
                )
        }

    private fun historyTitle(text: String) =
        Label(text).apply {
            styleClass.add(STYLE_HISTORY_TITLE)
            maxWidth = HISTORY_ROW_TEXT_WIDTH
        }

    private fun openRow(row: ConversationRow) {
        when (row) {
            is ConversationRow.Chat -> openConversation(row.summary.id, null)
            is ConversationRow.Hit ->
                openConversation(row.hit.chatId, row.hit.position.takeIf { row.hit.messageId != null })
        }
    }

    private fun openConversation(
        chatId: Int,
        focusPosition: Int?,
    ) {
        runHistory({ historyService.openChat(chatId) }) { stored -> applyStored(stored, focusPosition) }
    }

    private fun openLatestConversation() {
        runHistory({ historyService.latestChatId()?.let { historyService.openChat(it) } }) { stored ->
            if (stored != null) {
                applyStored(stored, null)
            } else {
                renderMessages(emptyList(), emptyList(), null)
                updateHeader()
            }
            refreshConversationList()
        }
    }

    private fun applyStored(
        stored: StoredConversation,
        focusPosition: Int?,
    ) {
        conversationState.load(stored)
        showEmptyConversation = false
        reviewSession.clear()
        reviewSession.load(stored.changeSets)
        reviewHidden = reviewSession.openCount() == 0
        updateReviewVisibility()
        renderMessages(stored.messages, stored.changeSets, focusPosition)
        knownChats = knownChats.filter { it.id != stored.chat.id } + stored.chat
        updateHeader()
        selectActiveRow()
    }

    private fun renderMessages(
        messages: List<ChatMessage>,
        changeSets: List<StoredChangeSet>,
        focusPosition: Int?,
    ) {
        messagesContainer.children.clear()
        rowsByPosition.clear()
        val setsByMessage = changeSets.groupBy { it.messageIndex }
        messages.forEachIndexed { index, message ->
            if (message.isShownInChat) {
                rowsByPosition[index] =
                    appendBubble(message.role, message.displayText ?: message.content, message.responseTimeMillis)
            }
            setsByMessage[index]
                ?.filter { it.changeSet.ignored.isNotEmpty() }
                ?.forEach { appendNotice(ignoredLinesText(it.changeSet.ignored)) }
        }
        if (focusPosition != null) scrollToPosition(focusPosition) else scrollToBottom()
    }

    private fun scrollToPosition(position: Int) {
        val target = rowsByPosition[position]
        if (target == null) {
            scrollToBottom()
            return
        }
        PauseTransition(Duration.millis(SCROLL_SETTLE_MILLIS)).apply {
            setOnFinished {
                messagesScrollPane.layout()
                val extra = messagesContainer.layoutBounds.height - messagesScrollPane.viewportBounds.height
                if (extra > 0) messagesScrollPane.vvalue = (target.boundsInParent.minY / extra).coerceIn(0.0, 1.0)
            }
            play()
        }
    }

    private fun updateHeader() {
        val chatId = conversationState.currentChatId()
        chatTitleLabel.text = knownChats.firstOrNull { it.id == chatId }?.title.orEmpty()
        val hasChat = chatId != null
        listOf(renameButton, deleteButton).forEach {
            it.isVisible = hasChat
            it.isManaged = hasChat
        }
    }

    private fun refreshConversationList() {
        runHistory({ historyService.listChats() }) { chats ->
            knownChats = chats
            if (searchField.text.isNullOrBlank()) {
                conversationsList.items.setAll(chats.map { ConversationRow.Chat(it) })
                selectActiveRow()
            }
            updateHeader()
        }
    }

    private fun selectActiveRow() {
        val activeId = conversationState.currentChatId()
        val index = conversationsList.items.indexOfFirst { it is ConversationRow.Chat && it.summary.id == activeId }
        if (index >= 0) {
            conversationsList.selectionModel.select(index)
        } else {
            conversationsList.selectionModel.clearSelection()
        }
    }

    private fun runSearch(text: String) {
        if (text.isEmpty()) {
            refreshConversationList()
            return
        }
        conversationsList.placeholder =
            Label(preferencesService.translate(TranslationKeys.AI_HISTORY_SIDEBAR_NO_RESULTS)).apply {
                styleClass.add(STYLE_HISTORY_EMPTY)
            }
        runHistory({ historyService.search(text) }) { hits ->
            if (searchField.text.trim() == text) {
                conversationsList.items.setAll(hits.map { ConversationRow.Hit(it) })
            }
        }
    }

    private fun <T> runHistory(
        work: () -> T,
        onSuccess: (T) -> Unit = {},
    ) {
        FxUtils.launchBackgroundThenUI(
            background = { runCatching(work) },
            onUI = { result ->
                result.fold(
                    onSuccess = onSuccess,
                    onFailure = {
                        logger.warn("AI chat history action failed: {}", it.javaClass.simpleName)
                        appendNotice(it.message ?: preferencesService.translate(TranslationKeys.AI_HISTORY_ERROR_SAVE))
                    },
                )
            },
        )
    }

    private fun setBusy(isBusy: Boolean) {
        busy = isBusy
        inputField.isDisable = isBusy
        sendButton.isDisable = isBusy
        thinkingLabel.isVisible = isBusy
        thinkingLabel.isManaged = isBusy
        listOf(
            conversationsList,
            newChatButton,
            collapsedNewButton,
            renameButton,
            deleteButton,
            deleteAllButton,
            searchField,
        ).forEach { it.isDisable = isBusy }

        thinkingTimeline?.stop()
        thinkingTimeline = null
        if (isBusy) startThinkingAnimation()
    }

    private fun startThinkingAnimation() {
        val words =
            preferencesService
                .translate(TranslationKeys.AI_CHAT_THINKING_WORDS)
                .split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf(preferencesService.translate(TranslationKeys.AI_CHAT_THINKING)) }
        var tick = 0

        fun render() {
            val word = words[(tick / TICKS_PER_WORD) % words.size]
            val dots = ".".repeat(tick % (THINKING_MAX_DOTS + 1))
            val elapsed = formatElapsed((System.nanoTime() - requestStartNanos) / NANOS_PER_MILLI)
            val activity = currentActivity?.let { "  ·  $it" }.orEmpty()
            thinkingLabel.text = "$word$dots$activity  ·  $elapsed"
        }

        render()
        thinkingTimeline =
            Timeline(
                KeyFrame(
                    Duration.millis(THINKING_TICK_MILLIS),
                    {
                        tick++
                        render()
                    },
                ),
            ).apply {
                cycleCount = Timeline.INDEFINITE
                play()
            }
    }

    private fun formatElapsed(millis: Long): String {
        val totalSeconds = millis / MILLIS_PER_SECOND
        return if (totalSeconds < SECONDS_PER_MINUTE) {
            "${totalSeconds}s"
        } else {
            "${totalSeconds / SECONDS_PER_MINUTE}m ${totalSeconds % SECONDS_PER_MINUTE}s"
        }
    }

    private fun formatResponseTime(millis: Long): String =
        if (millis < SECONDS_PER_MINUTE * MILLIS_PER_SECOND) {
            String.format(java.util.Locale.ROOT, "%.1fs", millis / MILLIS_PER_SECOND.toDouble())
        } else {
            formatElapsed(millis)
        }

    private fun appendBubble(
        role: ChatRole,
        text: String,
        responseTimeMillis: Long? = null,
    ): Node {
        val bubbleStyle = if (role == ChatRole.USER) STYLE_BUBBLE_USER else STYLE_BUBBLE_ASSISTANT
        val (background, textColor) = bubbleColors(bubbleStyle)
        val shown = if (role == ChatRole.USER) text.replace("\n", "  \n") else text
        val bubble = createMarkdownBubble(shown, background, textColor)
        val content: Node =
            if (responseTimeMillis != null && role == ChatRole.ASSISTANT) {
                val meta =
                    Label("⏱ ${formatResponseTime(responseTimeMillis)}").apply {
                        styleClass.add(STYLE_BUBBLE_META)
                    }
                VBox(2.0, bubble, meta)
            } else {
                bubble
            }
        val row =
            HBox(content).apply {
                alignment = if (role == ChatRole.USER) Pos.CENTER_RIGHT else Pos.CENTER_LEFT
            }
        messagesContainer.children.add(row)
        scrollToBottom()
        return row
    }

    private fun appendNotice(text: String) {
        val (background, textColor) = bubbleColors(STYLE_BUBBLE_NOTICE)
        val bubble = createMarkdownBubble(text, background, textColor)
        messagesContainer.children.add(HBox(bubble).apply { alignment = Pos.CENTER })
        scrollToBottom()
    }

    private fun bubbleColors(styleClass: String): Pair<String, String> {
        val isDark = preferencesService.isDarkMode()
        return when (styleClass) {
            STYLE_BUBBLE_USER ->
                if (isDark) {
                    ThemeColors.DARK_BG_SIDEBAR to ThemeColors.DARK_ON_SIDEBAR
                } else {
                    ThemeColors.LIGHT_BG_SIDEBAR to ThemeColors.LIGHT_ON_SIDEBAR
                }
            STYLE_BUBBLE_NOTICE ->
                if (isDark) {
                    ThemeColors.DARK_BG_CARD to ThemeColors.DARK_WARNING
                } else {
                    ThemeColors.LIGHT_BG_CARD_ALT to ThemeColors.LIGHT_WARNING
                }
            else ->
                if (isDark) {
                    ThemeColors.DARK_BORDER to ThemeColors.DARK_TEXT_PRIMARY
                } else {
                    ThemeColors.LIGHT_BORDER to ThemeColors.LIGHT_TEXT_PRIMARY
                }
        }
    }

    private fun createMarkdownBubble(
        markdown: String,
        background: String,
        textColor: String,
    ): WebView {
        val html = buildBubbleHtml(markdown, background, textColor)

        val webView =
            WebView().apply {
                prefWidth = BUBBLE_MAX_WIDTH
                maxWidth = BUBBLE_MAX_WIDTH
                minHeight = MIN_BUBBLE_HEIGHT
                prefHeight = MIN_BUBBLE_HEIGHT
                styleClass.add(STYLE_BUBBLE)
            }

        var linkHandlingAttached = false

        webView.engine.loadWorker.stateProperty().addListener { _, oldState, newState ->
            logger.info("Chat bubble WebView load state: {} -> {}", oldState, newState)

            when (newState) {
                Worker.State.SUCCEEDED -> {
                    resizeToContent(webView)
                    if (!linkHandlingAttached) {
                        linkHandlingAttached = true
                        configureLinkHandling(webView.engine, html)
                    }
                }
                Worker.State.FAILED ->
                    logger.warn("Chat bubble WebView failed to load", webView.engine.loadWorker.exception)
                else -> Unit
            }
        }
        loadHtml(webView.engine, html)

        return webView
    }

    private fun loadHtml(
        engine: WebEngine,
        html: String,
    ) {
        val base64 = Base64.getEncoder().encodeToString(html.toByteArray(Charsets.UTF_8))
        engine.load("data:text/html;charset=utf-8;base64,$base64")
    }

    private fun resizeToContent(webView: WebView) {
        val measured = runCatching { webView.engine.executeScript("document.documentElement.scrollHeight") }
        measured.onFailure { logger.warn("Failed to measure chat bubble content height: {}", it.message) }
        val rawHeight = (measured.getOrNull() as? Number)?.toDouble() ?: MIN_BUBBLE_HEIGHT
        val height = rawHeight.coerceIn(MIN_BUBBLE_HEIGHT, MAX_BUBBLE_HEIGHT)
        logger.info("Chat bubble measured height: raw={}, clamped={}", rawHeight, height)

        webView.prefHeight = height
        webView.minHeight = height
        webView.maxHeight = height
        webView.clip =
            Rectangle(BUBBLE_MAX_WIDTH, height).apply {
                arcWidth = BUBBLE_CORNER_RADIUS
                arcHeight = BUBBLE_CORNER_RADIUS
            }
        scrollToBottom()
    }

    private fun configureLinkHandling(
        engine: WebEngine,
        originalHtml: String,
    ) {
        engine.locationProperty().addListener { _, _, newLocation ->
            if (newLocation.isNullOrBlank() || newLocation.startsWith("data:")) return@addListener

            if (newLocation.startsWith("http://") || newLocation.startsWith("https://")) {
                runCatching { Desktop.getDesktop().browse(URI(newLocation)) }
                    .onFailure { logger.warn("Failed to open link in the system browser: {}", it.message) }
            }

            FxUtils.launchOnFxThread { loadHtml(engine, originalHtml) }
        }
    }

    private fun buildBubbleHtml(
        markdown: String,
        background: String,
        textColor: String,
    ): String {
        val bodyHtml = EmojiRenderer.replaceEmojiWithImages(MarkdownRenderer.toHtmlFragment(markdown))

        return """
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <style>
              html, body {
                margin: 0;
                padding: 0;
                background: $background;
                color: $textColor;
                font-family:
                  -apple-system, "Segoe UI", Roboto, "Noto Sans",
                  "Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", "Noto Emoji",
                  sans-serif;
                font-size: 13px;
                line-height: 1.45;
                word-wrap: break-word;
              }
              body {
                box-sizing: border-box;
                width: ${BUBBLE_MAX_WIDTH.toInt()}px;
                padding: 10px 14px;
              }
              p { margin: 0 0 8px 0; }
              p:last-child { margin-bottom: 0; }
              h1, h2, h3 { margin: 8px 0 6px 0; font-size: 1.15em; }
              ul, ol { margin: 4px 0 8px 20px; padding: 0; }
              table {
                border-collapse: collapse;
                margin: 4px 0 8px 0;
                font-size: 0.95em;
                max-width: 100%;
              }
              th, td {
                border: 1px solid rgba(127, 127, 127, 0.35);
                padding: 4px 8px;
                text-align: left;
              }
              th { background: rgba(127, 127, 127, 0.18); font-weight: 700; }
              code {
                font-family: "Cascadia Code", "Fira Code", Consolas, monospace;
                background: rgba(127, 127, 127, 0.18);
                padding: 1px 4px;
                border-radius: 3px;
              }
              pre { background: rgba(127, 127, 127, 0.18); padding: 8px; border-radius: 6px; overflow-x: auto; }
              pre code { background: none; padding: 0; }
              a { color: inherit; }
              strong { font-weight: 700; }
            </style>
            </head>
            <body>$bodyHtml</body>
            </html>
            """.trimIndent()
    }

    private fun scrollToBottom() {
        FxUtils.launchOnFxThread {
            messagesScrollPane.layout()
            messagesScrollPane.vvalue = 1.0
        }
    }
}
