/*
 * Filename: AiChatController.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.concurrent.Worker
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.shape.Rectangle
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.stage.FileChooser
import org.moinex.ai.chat.AiChatService
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ChatTurnResult
import org.moinex.ai.chat.EmojiRenderer
import org.moinex.ai.chat.MarkdownRenderer
import org.moinex.ai.ingest.StatementIngestService
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.FxUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.dto.ai.ExtractedTransactionDTO
import org.moinex.service.PreferencesService
import org.moinex.ui.dialog.ai.TransactionReviewController
import org.slf4j.LoggerFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller
import java.awt.Desktop
import java.net.URI
import java.text.MessageFormat
import java.util.Base64

@Controller
class AiChatController(
    private val aiChatService: AiChatService,
    private val statementIngestService: StatementIngestService,
    private val preferencesService: PreferencesService,
    private val springContext: ConfigurableApplicationContext,
) {
    @FXML
    private lateinit var messagesScrollPane: ScrollPane

    @FXML
    private lateinit var messagesContainer: VBox

    @FXML
    private lateinit var inputField: TextField

    @FXML
    private lateinit var sendButton: JFXButton

    @FXML
    private lateinit var attachButton: JFXButton

    @FXML
    private lateinit var attachmentChip: HBox

    @FXML
    private lateinit var attachmentLabel: Label

    @FXML
    private lateinit var thinkingLabel: Label

    private val history = mutableListOf<ChatMessage>()
    private val pendingReviews = mutableListOf<List<ExtractedTransactionDTO>>()
    private var pendingAttachment: PendingAttachment? = null

    private data class PendingAttachment(
        val fileName: String,
        val extractedText: String,
    )

    companion object {
        private val logger = LoggerFactory.getLogger(AiChatController::class.java)
        private const val BUBBLE_MAX_WIDTH = 480.0
        private const val MIN_BUBBLE_HEIGHT = 30.0
        private const val MAX_BUBBLE_HEIGHT = 2000.0
        private const val BUBBLE_CORNER_RADIUS = 28.0

        private const val STYLE_BUBBLE = "ai-chat-bubble"
        private const val STYLE_BUBBLE_USER = "ai-chat-bubble-user"
        private const val STYLE_BUBBLE_ASSISTANT = "ai-chat-bubble-assistant"
        private const val STYLE_BUBBLE_NOTICE = "ai-chat-bubble-notice"
        private const val STYLE_ATTACH_BUTTON = "ai-chat-attach-button"

        private const val LIGHT_SIDEBAR_BG = "#1abc9c"
        private const val LIGHT_TEXT_ON_DARK = "#ffffff"
        private const val LIGHT_CARD_ALT_BG = "#f5f5f5"
        private const val LIGHT_TEXT_PRIMARY = "#333333"
        private const val LIGHT_WARNING_BG = "#fff3cd"
        private const val LIGHT_WARNING_TEXT = "#856404"

        private const val DARK_SIDEBAR_BG = "#0a614e"
        private const val DARK_TEXT_ON_DARK = "#F8F8F2"
        private const val DARK_CARD_ALT_BG = "#21222c"
        private const val DARK_TEXT_PRIMARY = "#F8F8F2"
        private const val DARK_WARNING_BG = "#282A36"
        private const val DARK_WARNING_TEXT = "#FFB86C"
    }

    @FXML
    fun initialize() {
        setBusy(false)
        inputField.setOnAction { handleSend() }
        history.forEach { message -> appendBubble(message.role, message.displayText ?: message.content) }
        pendingReviews.forEach { renderReviewButton(it) }
        pendingAttachment?.let { setAttachment(it.fileName, it.extractedText) }
    }

    @FXML
    fun handleSend() {
        val text = inputField.text?.trim().orEmpty()
        val attachment = pendingAttachment
        if (text.isBlank() && attachment == null) return

        val messageContent =
            if (attachment != null) {
                aiChatService.buildAttachmentMessage(
                    attachment.fileName,
                    attachment.extractedText,
                    text.ifBlank { null },
                )
            } else {
                text
            }
        val bubbleText =
            when {
                attachment != null && text.isNotBlank() -> "$text\n\n📎 ${attachment.fileName}"
                attachment != null -> "📎 ${attachment.fileName}"
                else -> text
            }

        appendBubble(ChatRole.USER, bubbleText)
        history.add(ChatMessage(role = ChatRole.USER, content = messageContent, displayText = bubbleText))
        inputField.clear()
        clearAttachment()
        setBusy(true)

        FxUtils.launchBackgroundThenUI(
            background = { aiChatService.sendMessage(history.toList()) },
            onUI = { result -> handleResult(result) },
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
        val file = chooser.showOpenDialog(attachButton.scene.window) ?: return

        setAttaching(true)

        FxUtils.launchBackgroundThenUI(
            background = { runCatching { statementIngestService.extractText(file.toPath()) } },
            onUI = { result ->
                setAttaching(false)
                result.fold(
                    onSuccess = { extractedText -> setAttachment(file.name, extractedText) },
                    onFailure = { e ->
                        appendNotice(
                            MessageFormat.format(
                                preferencesService.translate(TranslationKeys.AI_CHAT_ATTACH_ERROR),
                                e.message,
                            ),
                        )
                    },
                )
            },
        )
    }

    @FXML
    fun handleRemoveAttachment() {
        clearAttachment()
    }

    private fun setAttachment(
        fileName: String,
        extractedText: String,
    ) {
        pendingAttachment = PendingAttachment(fileName, extractedText)
        attachmentLabel.text = "📎 $fileName"
        attachmentChip.isVisible = true
        attachmentChip.isManaged = true
    }

    private fun clearAttachment() {
        pendingAttachment = null
        attachmentChip.isVisible = false
        attachmentChip.isManaged = false
    }

    private fun setAttaching(attaching: Boolean) {
        attachButton.isDisable = attaching
    }

    private fun handleResult(result: ChatTurnResult) {
        setBusy(false)

        when (result) {
            is ChatTurnResult.Success -> {
                history.add(ChatMessage(role = ChatRole.ASSISTANT, content = result.reply))
                appendBubble(ChatRole.ASSISTANT, result.reply)
            }
            is ChatTurnResult.TransactionsProposed -> {
                history.add(ChatMessage(role = ChatRole.ASSISTANT, content = result.reply))
                if (result.reply.isNotBlank()) appendBubble(ChatRole.ASSISTANT, result.reply)
                appendReviewButton(result.transactions)
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

    private fun appendReviewButton(transactions: List<ExtractedTransactionDTO>) {
        pendingReviews.add(transactions)
        renderReviewButton(transactions)
    }

    private fun renderReviewButton(transactions: List<ExtractedTransactionDTO>) {
        val button =
            JFXButton(
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.AI_CHAT_REVIEW_BUTTON),
                    transactions.size,
                ),
            ).apply {
                styleClass.add(STYLE_ATTACH_BUTTON)
                setOnAction { openTransactionReview(transactions) }
            }
        messagesContainer.children.add(HBox(button).apply { alignment = Pos.CENTER_LEFT })
        scrollToBottom()
    }

    private fun openTransactionReview(transactions: List<ExtractedTransactionDTO>) {
        WindowUtils.openModalWindow(
            Files.TRANSACTION_REVIEW_FXML,
            preferencesService.translate(TranslationKeys.AI_REVIEW_TITLE),
            springContext,
            { controller: TransactionReviewController -> controller.setProposedTransactions(transactions) },
            emptyList(),
            preferencesService.bundle,
        )
    }

    private fun setBusy(busy: Boolean) {
        inputField.isDisable = busy
        sendButton.isDisable = busy
        thinkingLabel.isVisible = busy
        thinkingLabel.isManaged = busy
    }

    private fun appendBubble(
        role: ChatRole,
        text: String,
    ) {
        val bubbleStyle = if (role == ChatRole.USER) STYLE_BUBBLE_USER else STYLE_BUBBLE_ASSISTANT
        val (background, textColor) = bubbleColors(bubbleStyle)
        val bubble = createMarkdownBubble(text, background, textColor)
        val row =
            HBox(bubble).apply {
                alignment = if (role == ChatRole.USER) Pos.CENTER_RIGHT else Pos.CENTER_LEFT
            }
        messagesContainer.children.add(row)
        scrollToBottom()
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
                    DARK_SIDEBAR_BG to DARK_TEXT_ON_DARK
                } else {
                    LIGHT_SIDEBAR_BG to
                        LIGHT_TEXT_ON_DARK
                }
            STYLE_BUBBLE_NOTICE ->
                if (isDark) {
                    DARK_WARNING_BG to DARK_WARNING_TEXT
                } else {
                    LIGHT_WARNING_BG to
                        LIGHT_WARNING_TEXT
                }
            else -> if (isDark) DARK_CARD_ALT_BG to DARK_TEXT_PRIMARY else LIGHT_CARD_ALT_BG to LIGHT_TEXT_PRIMARY
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
