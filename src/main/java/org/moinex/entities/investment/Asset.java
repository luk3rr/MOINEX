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

    @Column(name = "current_quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal currentQuantity;

    @Column(name = "current_unit_value", nullable = false, scale = 2)
    private BigDecimal currentUnitValue;

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
     */
    public Asset(String     name,
                 String     symbol,
                 BigDecimal currentQuantity,
                 BigDecimal currentUnitValue)
    {
        this.name             = name;
        this.symbol           = symbol;
        this.currentQuantity  = currentQuantity;
        this.currentUnitValue = currentUnitValue;
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
}
