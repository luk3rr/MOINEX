/*
 * Filename: Asset.kt (original filename: Asset.java)
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.MappedSuperclass
import org.moinex.common.extension.isZero
import org.moinex.model.enums.AssetType
import org.moinex.util.Constants
import java.math.BigDecimal

@MappedSuperclass
abstract class Asset(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "symbol", nullable = false, unique = true)
    var symbol: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    var type: AssetType,
    @Column(name = "average_unit_value_count", nullable = false)
    var averageUnitValueCount: BigDecimal,
    currentQuantity: BigDecimal,
    currentUnitValue: BigDecimal,
    averageUnitValue: BigDecimal,
) {
    @Column(name = "current_quantity", nullable = false)
    var currentQuantity: BigDecimal = currentQuantity
        set(value) {
            require(value >= BigDecimal.ZERO) {
                "Current quantity must be non-negative"
            }
            field = value
            if (value.isZero()) {
                averageUnitValue = BigDecimal.ZERO
                averageUnitValueCount = BigDecimal.ZERO
            }
        }

    @Column(name = "current_unit_value", nullable = false)
    var currentUnitValue: BigDecimal = currentUnitValue
        set(value) {
            require(value >= BigDecimal.ZERO) {
                "Current unit value must be non-negative"
            }
            field = Constants.roundPrice(value, type)
        }

    @Column(name = "average_unit_value", nullable = false)
    var averageUnitValue: BigDecimal = averageUnitValue
        set(value) {
            require(averageUnitValue >= BigDecimal.ZERO) {
                "Average unit value must be non-negative"
            }

            field = Constants.roundPrice(value, type)
        }

    init {
        name = name.trim()
        symbol = symbol.trim().uppercase()

        require(name.isNotEmpty()) {
            "Asset name cannot be empty"
        }
        require(symbol.isNotEmpty()) {
            "Asset symbol cannot be empty"
        }

        this.currentQuantity = currentQuantity
    }
}
