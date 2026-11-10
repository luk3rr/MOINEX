/*
 * Filename: BaseOperation.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Shared plumbing: `apply` validates, runs the write and turns any exception into [ApplyResult.Failure].
 */
abstract class BaseOperation<P : Any> : AppOperation<P> {
    private val logger = LoggerFactory.getLogger(this::class.java)

    protected abstract fun execute(payload: P): ApplyResult.Success

    final override fun apply(payload: P): ApplyResult {
        val validation = validate(payload)
        if (!validation.isValid) return ApplyResult.Failure(validation.errors.joinToString("; "))

        return runCatching { execute(payload) }
            .getOrElse { e ->
                logger.warn("Operation {} failed: {}", name, e.message)
                ApplyResult.Failure(e.message ?: e::class.simpleName.orEmpty())
            }
    }

    protected fun missingDescription(description: String): List<String> =
        if (description.isBlank()) listOf(DESCRIPTION_MISSING) else emptyList()

    protected fun JSONObject.requireDate(key: String): LocalDate = LocalDate.parse(getString(key))

    protected fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key) || !has(key)) null else getInt(key)

    protected fun JSONObject.optDecimalOrZero(key: String): BigDecimal =
        if (isNull(key) || !has(key)) BigDecimal.ZERO else getBigDecimal(key)

    protected fun property(
        type: String,
        description: String? = null,
    ): JSONObject = JSONObject().put("type", type).apply { description?.let { put("description", it) } }

    protected fun Any?.asInt(): Int? = (this as? Number)?.toInt()

    protected fun Any?.asBoolean(): Boolean = this as? Boolean ?: false

    protected fun Any?.asDecimal(): BigDecimal? =
        when (this) {
            is BigDecimal -> this
            is Number -> BigDecimal(toString())
            is String -> toBigDecimalOrNull()
            else -> null
        }

    protected fun Any?.asDate(): LocalDate? =
        when (this) {
            is LocalDate -> this
            is String -> runCatching { LocalDate.parse(this) }.getOrNull()
            else -> null
        }
}

const val DESCRIPTION_MISSING: String =
    "description is required: a short label for the record, such as the payee or merchant, or what the " +
        "operation is about (e.g. \"Resgate CDB Banco Inter\")"
