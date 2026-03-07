/*
 * Filename: Asset.kt (original filename: Asset.java)
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import java.math.BigDecimal

@MappedSuperclass
abstract class Asset(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "symbol", nullable = false, unique = true)
    var symbol: String,
    @Column(name = "current_quantity", nullable = false)
    var currentQuantity: BigDecimal,
    @Column(name = "current_unit_value", nullable = false)
    var currentUnitValue: BigDecimal,
    @Column(name = "average_unit_value", nullable = false)
    var averageUnitValue: BigDecimal,
    @Column(name = "average_unit_value_count", nullable = false)
    var averageUnitValueCount: BigDecimal,
) {
    init {
        name = name.trim()
        symbol = symbol.trim().uppercase()

        require(name.isNotEmpty()) {
            "Asset name cannot be empty"
        }
        require(symbol.isNotEmpty()) {
            "Asset symbol cannot be empty"
        }
        require(currentQuantity >= BigDecimal.ZERO) {
            "Current quantity must be non-negative"
        }
        require(currentUnitValue >= BigDecimal.ZERO) {
            "Current unit value must be non-negative"
        }
        require(averageUnitValue >= BigDecimal.ZERO) {
            "Average unit value must be non-negative"
        }
    }
}
