/*
 * Filename: InvestmentTarget.kt (original filename: InvestmentTarget.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.toRounded
import org.moinex.model.enums.AssetType
import java.math.BigDecimal

@Entity
@Table(name = "investment_target")
class InvestmentTarget(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, unique = true)
    var assetType: AssetType,
    @Column(name = "target_percentage", nullable = false)
    var targetPercentage: BigDecimal,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
) {
    init {
        targetPercentage = targetPercentage.toRounded()

        require(targetPercentage >= BigDecimal.ZERO && targetPercentage <= BigDecimal(100)) {
            "Target percentage must be between 0 and 100"
        }
    }

    override fun toString(): String = "InvestmentTarget [id=$id, type=$assetType, target=$targetPercentage%]"
}
