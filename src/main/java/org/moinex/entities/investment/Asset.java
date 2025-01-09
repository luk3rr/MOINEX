/*
 * Filename: Asset.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.investment;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;

/**
 * Class that represents an asset
 */
@MappedSuperclass
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

    /**
     * Default constructor for JPA
     */
    public Asset() { }

    /**
     * Constructor for Asset
     * @param name The name of the asset
     * @param symbol The symbol of the asset
     * @param currentQuantity The current quantity of the asset
     * @param currentUnitValue The current unit value of the asset
     * @param averageUnitValue The average unit value of the asset
     * @param averageUnitValueCount The average unit value count of the asset
     */
    public Asset(String     name,
                 String     symbol,
                 BigDecimal currentQuantity,
                 BigDecimal currentUnitValue,
                 BigDecimal averageUnitValue,
                 BigDecimal averageUnitValueCount)
    {
        this.name                  = name;
        this.symbol                = symbol;
        this.currentQuantity       = currentQuantity;
        this.currentUnitValue      = currentUnitValue;
        this.averageUnitValue      = averageUnitValue;
        this.averageUnitValueCount = averageUnitValueCount;
    }

    public String GetName()
    {
        return name;
    }

    public String GetSymbol()
    {
        return symbol;
    }

    public BigDecimal GetCurrentQuantity()
    {
        return currentQuantity;
    }

    public BigDecimal GetCurrentUnitValue()
    {
        return currentUnitValue;
    }

    public BigDecimal GetAveragePrice()
    {
        return averageUnitValue;
    }

    public BigDecimal GetAveragePriceCount()
    {
        return averageUnitValueCount;
    }

    public void SetName(String name)
    {
        this.name = name;
    }

    public void SetSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public void SetCurrentQuantity(BigDecimal currentQuantity)
    {
        this.currentQuantity = currentQuantity;
    }

    public void SetCurrentUnitValue(BigDecimal currentUnitValue)
    {
        this.currentUnitValue = currentUnitValue;
    }

    public void SetAveragePrice(BigDecimal averageUnitValue)
    {
        this.averageUnitValue = averageUnitValue;
    }

    public void SetAveragePriceCount(BigDecimal averageUnitValueCount)
    {
        this.averageUnitValueCount = averageUnitValueCount;
    }
}
