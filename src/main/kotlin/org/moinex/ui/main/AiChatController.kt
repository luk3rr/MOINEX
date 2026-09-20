/*
 * Filename: AiChatController.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main

import com.jfoenix.controls.JFXButton
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.Font
import javafx.scene.text.Text
import org.moinex.ai.chat.AiChatService
import org.moinex.ai.chat.ChatMessage
import org.moinex.ai.chat.ChatRole
import org.moinex.ai.chat.ChatTurnResult
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.FxUtils
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Controller
import java.text.MessageFormat

/**
 * Read-only chat screen: the assistant can only answer analytical questions via
 * [org.moinex.ai.tools.QueryFinanceTool] - there is no action here that writes to the database.
 */
@Controller
class AiChatController(
    private val aiChatService: AiChatService,
    private val preferencesService: PreferencesService,
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
    private lateinit var thinkingLabel: Label

    private val history = mutableListOf<ChatMessage>()

    companion object {
        private val logger = LoggerFactory.getLogger(AiChatController::class.java)
        private const val BUBBLE_MAX_WIDTH = 480.0
        private val BUBBLE_FONT: Font = Font.font(13.0)

        // Approximate horizontal/vertical space TextArea's own padding/border takes beyond the
        // wrapped text itself - used to size the control to fit its content without a scrollbar.
        private const val BUBBLE_H_PADDING = 30.0
        private const val BUBBLE_V_PADDING = 24.0

        private const val STYLE_BUBBLE = "ai-chat-bubble"
        private const val STYLE_BUBBLE_USER = "ai-chat-bubble-user"
        private const val STYLE_BUBBLE_ASSISTANT = "ai-chat-bubble-assistant"
        private const val STYLE_BUBBLE_NOTICE = "ai-chat-bubble-notice"
    }

    @FXML
    fun initialize() {
        setBusy(false)
        inputField.setOnAction { handleSend() }
        history.forEach { message -> appendBubble(message.role, message.content) }
    }

    @FXML
    fun handleSend() {
        val text = inputField.text?.trim().orEmpty()
        if (text.isBlank()) return

        appendBubble(ChatRole.USER, text)
        history.add(ChatMessage(role = ChatRole.USER, content = text))
        inputField.clear()
        setBusy(true)

        FxUtils.launchBackgroundThenUI(
            background = { aiChatService.sendMessage(history.toList()) },
            onUI = { result -> handleResult(result) },
        )
    }

    private fun handleResult(result: ChatTurnResult) {
        setBusy(false)

        when (result) {
            is ChatTurnResult.Success -> {
                history.add(ChatMessage(role = ChatRole.ASSISTANT, content = result.reply))
                appendBubble(ChatRole.ASSISTANT, result.reply)
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
        val bubble = createSelectableBubble(text, bubbleStyle)
        val row =
            HBox(bubble).apply {
                alignment = if (role == ChatRole.USER) Pos.CENTER_RIGHT else Pos.CENTER_LEFT
            }
        messagesContainer.children.add(row)
        scrollToBottom()
    }

    private fun appendNotice(text: String) {
        val bubble = createSelectableBubble(text, STYLE_BUBBLE_NOTICE)
        messagesContainer.children.add(HBox(bubble).apply { alignment = Pos.CENTER })
        scrollToBottom()
    }

    /**
     * A read-only, non-editable [TextArea] instead of a [Label] - JavaFX labels don't support
     * text selection/copy, which chat messages need (e.g. to copy a figure the assistant quoted).
     * Height is computed from a same-font/same-width [Text] measurement so the bubble hugs its
     * content instead of showing an internal scrollbar.
     */
    private fun createSelectableBubble(
        text: String,
        styleClass: String,
    ): TextArea {
        val measured =
            Text(text).apply {
                font = BUBBLE_FONT
                wrappingWidth = BUBBLE_MAX_WIDTH - BUBBLE_H_PADDING
            }
        val height = measured.layoutBounds.height + BUBBLE_V_PADDING

        return TextArea(text).apply {
            isEditable = false
            isWrapText = true
            font = BUBBLE_FONT
            prefWidth = BUBBLE_MAX_WIDTH
            maxWidth = BUBBLE_MAX_WIDTH
            prefHeight = height
            minHeight = height
            maxHeight = height
            this.styleClass.addAll(STYLE_BUBBLE, styleClass)
        }
    }

    private fun scrollToBottom() {
        FxUtils.launchOnFxThread {
            messagesScrollPane.layout()
            messagesScrollPane.vvalue = 1.0
        }
    }
}
