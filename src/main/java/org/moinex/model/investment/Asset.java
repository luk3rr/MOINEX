/*
 * Filename: Asset.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Class that represents an asset
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Asset
{
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "symbol", nullable = false, unique = true)
    private String symbol;

    @Column(name = "current_quantity", nullable = false)
    private BigDecimal currentQuantity;

    @Column(name = "current_unit_value", nullable = false)
    private BigDecimal currentUnitValue;

    @Column(name = "average_unit_value", nullable = false)
    private BigDecimal averageUnitValue;

    @Column(name = "average_unit_value_count", nullable = false)
    private BigDecimal averageUnitValueCount;
}
