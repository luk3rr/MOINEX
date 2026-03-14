package org.moinex.common.extension

import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate

fun JSONObject.decimal(
    name: String,
    field: String,
): BigDecimal {
    val jsonObj = obj(name)
    return when (val value = jsonObj[field]) {
        is String -> value.toBigDecimal()
        is Number -> value.toString().toBigDecimal()
        else -> throw IllegalArgumentException("Field $field must be a string or number")
    }
}

fun JSONObject.field(
    obj: String,
    field: String,
): String = obj(obj).str(field)

fun JSONObject.bacenDate(
    obj: String,
    field: String,
): LocalDate = obj(obj).str(field).toLocalDateBACENFormat()

fun JSONObject.obj(name: String): JSONObject = getJSONObject(name)

fun JSONObject.str(field: String): String = getString(field)
