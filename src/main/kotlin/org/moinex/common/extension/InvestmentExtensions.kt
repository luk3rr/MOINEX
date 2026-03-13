package org.moinex.common.extension

import org.moinex.model.dto.BondOperationStateDTO
import org.moinex.model.enums.OperationType
import org.moinex.model.investment.BondOperation
import java.math.BigDecimal
import java.time.LocalDate

fun List<BondOperation>.operationsUntil(date: LocalDate) = asSequence().takeWhile { it.localDate <= date }

fun List<BondOperation>.buyOperationsUntil(date: LocalDate) = operationsUntil(date).filter { it.operationType == OperationType.BUY }

fun List<BondOperation>.calculateOperationStateUntil(date: LocalDate): BondOperationStateDTO =
    this
        .operationsUntil(date)
        .sortedBy { it.localDate }
        .fold(BondOperationStateDTO(BigDecimal.ZERO, BigDecimal.ZERO, null)) { state, operation ->
            when (operation.operationType) {
                OperationType.BUY ->
                    BondOperationStateDTO(
                        quantity = state.quantity + operation.quantity,
                        lastSpread = operation.spread ?: state.lastSpread,
                        lastBuyPrice = operation.unitPrice,
                    )
                OperationType.SELL ->
                    state.copy(
                        quantity = state.quantity - operation.quantity,
                    )
            }
        }

fun List<BondOperation>.calculateQuantityUntil(date: LocalDate): BigDecimal = calculateOperationStateUntil(date).quantity

fun List<BondOperation>.getLastBuyPrice(): BigDecimal? =
    filter { it.isPurchase() }
        .maxByOrNull { it.localDate }
        ?.unitPrice
