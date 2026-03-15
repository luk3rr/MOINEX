package org.moinex.common.extension

import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate

fun JSONObject.decimal(
    name: String,
    field: String,
): BigDecimal {
    val jsonObj = getJSONObject(name)
    return when (val value = jsonObj[field]) {
        is String -> value.toBigDecimal()
        is Number -> value.toString().toBigDecimal()
        else -> throw IllegalArgumentException("Field $field must be a string or number")
    }
}

fun JSONObject.field(
    obj: String,
    field: String,
): String = getJSONObject(obj).getString(field)

fun JSONObject.bacenDate(
    obj: String,
    field: String,
): LocalDate = getJSONObject(obj).getString(field).toLocalDateBACENFormat()

fun JSONObject.throwIfError(operationName: String = "API call"): JSONObject {
    if (has("error")) {
        val errorMessage = getString("error")
        throw RuntimeException("$operationName failed: $errorMessage")
    }
    return this
}
