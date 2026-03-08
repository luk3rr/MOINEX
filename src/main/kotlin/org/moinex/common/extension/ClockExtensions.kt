package org.moinex.common.extension

import org.moinex.util.Constants
import java.time.LocalDate
import java.time.LocalDateTime

fun LocalDate.isBeforeOrEqual(other: LocalDate): Boolean = this.isBefore(other) || this.isEqual(other)

fun LocalDate.isAfterOrEqual(other: LocalDate): Boolean = this.isAfter(other) || this.isEqual(other)

fun LocalDate.isBetween(
    start: LocalDate,
    end: LocalDate,
): Boolean = this.isAfterOrEqual(start) && this.isBeforeOrEqual(end)

fun LocalDateTime.isBeforeOrEqual(other: LocalDateTime): Boolean = this.isBefore(other) || this.isEqual(other)

fun LocalDateTime.isAfterOrEqual(other: LocalDateTime): Boolean = this.isAfter(other) || this.isEqual(other)

fun LocalDateTime.isBetween(
    start: LocalDateTime,
    end: LocalDateTime,
): Boolean = this.isAfterOrEqual(start) && this.isBeforeOrEqual(end)

fun LocalDate.atEndOfDay(): LocalDateTime = this.atTime(23, 59, 59, 999999999)

fun LocalDate.isOpenEnded(): Boolean = this == Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE
