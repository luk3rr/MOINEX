package org.moinex.common

import java.math.BigDecimal
import java.math.RoundingMode

fun BigDecimal.toRounded(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)

fun BigDecimal.isZero() = this.compareTo(BigDecimal.ZERO) == 0

fun BigDecimal.isNotZero() = this.compareTo(BigDecimal.ZERO) != 0

private fun toBigDecimalValue(value: Number): BigDecimal =
    when (value) {
        is BigDecimal -> value
        is Int -> value.toBigDecimal()
        is Long -> value.toBigDecimal()
        is Double -> value.toBigDecimal()
        is Float -> value.toBigDecimal()
        else -> throw IllegalArgumentException("Unsupported type: ${value::class.simpleName}")
    }

fun <T : Number> BigDecimal.isEqual(value: T): Boolean = this.compareTo(toBigDecimalValue(value)) == 0

fun <T : Number> BigDecimal.isNotEqual(value: T): Boolean = this.compareTo(toBigDecimalValue(value)) != 0

fun BigDecimal.isBetween(
    min: BigDecimal,
    max: BigDecimal,
) = this in min..max
