package org.moinex.common.extension

import org.moinex.util.Constants
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

fun LocalDate.isBeforeOrEqual(other: LocalDate): Boolean = !this.isAfter(other)

fun LocalDate.isAfterOrEqual(other: LocalDate): Boolean = !this.isBefore(other)

fun LocalDate.isBetween(
    start: LocalDate,
    end: LocalDate,
): Boolean = this.isAfterOrEqual(start) && this.isBeforeOrEqual(end)

fun LocalDateTime.isBeforeOrEqual(other: LocalDateTime): Boolean = !this.isAfter(other)

fun LocalDateTime.isAfterOrEqual(other: LocalDateTime): Boolean = !this.isBefore(other)

fun YearMonth.isBeforeOrEqual(other: YearMonth): Boolean = !this.isAfter(other)

fun YearMonth.isAfterOrEqual(other: YearMonth): Boolean = !this.isBefore(other)

fun LocalDateTime.isBetween(
    start: LocalDateTime,
    end: LocalDateTime,
): Boolean = this.isAfterOrEqual(start) && this.isBeforeOrEqual(end)

fun LocalDate.atEndOfDay(): LocalDateTime = this.atTime(23, 59, 59, 999999999)

fun LocalDate.isOpenEnded(): Boolean = this == Constants.RECURRING_TRANSACTION_DEFAULT_END_DATE

fun LocalDate.toBACENFormat(): String = this.format(Constants.BACEN_DATE_FORMATTER)

fun LocalDate.toNoTimeFormat(): String = this.format(Constants.DATE_FORMATTER_NO_TIME)

fun String.toLocalDateBACENFormat(): LocalDate = LocalDate.parse(this, Constants.BACEN_DATE_FORMATTER)

fun String.toNoTimeFormat(): LocalDate = LocalDate.parse(this, Constants.DATE_FORMATTER_NO_TIME)
