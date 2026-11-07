/*
 * Filename: ChatPayloadCodec.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.history

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.moinex.ai.chat.ToolCall
import org.moinex.ai.operation.AppOperation
import org.moinex.ai.operation.OperationRegistry
import org.moinex.model.dto.ai.IgnoredLine
import org.moinex.service.matching.Match
import org.slf4j.LoggerFactory
import org.springframework.core.ResolvableType
import org.springframework.stereotype.Component
import org.springframework.util.ClassUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * The text of a stored payload that could not be turned back into the payload of its operation, because the
 * operation no longer exists or the text no longer fits it. It can be shown, but never applied.
 */
data class RawPayload(
    val json: String,
)

/**
 * Turns what the assistant proposed, and the calls it made, into JSON and back, so a conversation can be stored
 * and reopened. The payload class of each operation is read from the type argument of its [AppOperation], so a new
 * operation needs no registration here.
 */
@Component
class ChatPayloadCodec(
    private val registry: OperationRegistry,
) {
    private val logger = LoggerFactory.getLogger(ChatPayloadCodec::class.java)

    private val mapper: JsonMapper =
        JsonMapper
            .builder()
            .addModule(KotlinModule.Builder().build())
            .addModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    private val payloadClasses = ConcurrentHashMap<String, Class<*>>()

    /** The class of the payload of the operation [operationName], or null when no such operation exists. */
    fun payloadClassOf(operationName: String): Class<*>? {
        payloadClasses[operationName]?.let { return it }
        val operation = registry.find(operationName) ?: return null
        val resolved =
            ResolvableType
                .forClass(ClassUtils.getUserClass(operation))
                .`as`(AppOperation::class.java)
                .getGeneric(0)
                .resolve() ?: return null
        payloadClasses[operationName] = resolved
        return resolved
    }

    fun payloadToJson(payload: Any): String =
        if (payload is RawPayload) payload.json else mapper.writeValueAsString(payload)

    /** Never throws: what cannot be rebuilt comes back as a [RawPayload]. */
    fun payloadFromJson(
        operationName: String,
        json: String,
    ): Any {
        val type = payloadClassOf(operationName) ?: return RawPayload(json)
        return runCatching { mapper.readValue(json, type) }
            .onFailure {
                logger.warn(
                    "Stored payload of operation {} cannot be read: {}",
                    operationName,
                    it.javaClass.simpleName,
                )
            }.getOrNull() ?: RawPayload(json)
    }

    fun matchesToJson(matches: List<Match>): String = mapper.writeValueAsString(matches)

    fun matchesFromJson(json: String): List<Match> = mapper.readValue(json)

    fun toolCallsToJson(calls: List<ToolCall>): String = mapper.writeValueAsString(calls)

    fun toolCallsFromJson(json: String): List<ToolCall> = mapper.readValue(json)

    fun stringsToJson(values: List<String>): String = mapper.writeValueAsString(values)

    fun stringsFromJson(json: String): List<String> = mapper.readValue(json)

    fun ignoredLinesToJson(lines: List<IgnoredLine>): String = mapper.writeValueAsString(lines)

    fun ignoredLinesFromJson(json: String): List<IgnoredLine> = mapper.readValue(json)
}
