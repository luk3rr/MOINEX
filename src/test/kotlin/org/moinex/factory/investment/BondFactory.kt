package org.moinex.factory.investment

import org.moinex.model.enums.BondType
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.model.investment.Bond
import java.math.BigDecimal
import java.time.LocalDate

object BondFactory {
    fun create(
        id: Int? = null,
        name: String = "Test Bond",
        symbol: String? = "BOND001",
        type: BondType = BondType.CDB,
        issuer: String? = "Test Bank",
        maturityDate: LocalDate? = LocalDate.now().plusYears(1),
        interestType: InterestType? = InterestType.FIXED,
        interestIndex: InterestIndex? = null,
        interestRate: BigDecimal? = BigDecimal("10.00"),
        archived: Boolean = false,
    ): Bond =
        Bond(
            id = id,
            name = name,
            symbol = symbol,
            type = type,
            issuer = issuer,
            maturityDate = maturityDate,
            interestType = interestType,
            interestIndex = interestIndex,
            interestRate = interestRate,
            archived = archived,
        )
}
