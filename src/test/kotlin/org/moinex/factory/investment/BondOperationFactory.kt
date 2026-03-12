package org.moinex.factory.investment

import org.moinex.model.enums.OperationType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.model.wallettransaction.WalletTransaction
import java.math.BigDecimal

object BondOperationFactory {
    fun create(
        id: Int? = null,
        bond: Bond = BondFactory.create(id = 1),
        operationType: OperationType = OperationType.BUY,
        quantity: BigDecimal = BigDecimal("100"),
        unitPrice: BigDecimal = BigDecimal("10.00"),
        fees: BigDecimal = BigDecimal.ZERO,
        taxes: BigDecimal = BigDecimal.ZERO,
        netProfit: BigDecimal = BigDecimal.ZERO,
        spread: BigDecimal? = null,
        walletTransaction: WalletTransaction? = null,
    ): BondOperation =
        BondOperation(
            id = id,
            bond = bond,
            operationType = operationType,
            quantity = quantity,
            unitPrice = unitPrice,
            fees = fees,
            taxes = taxes,
            netProfit = netProfit,
            spread = spread,
            walletTransaction = walletTransaction,
        )
}
