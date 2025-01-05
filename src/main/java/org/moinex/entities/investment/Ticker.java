/*
 * Filename: Ticker.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "ticker")
public class Ticker
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "symbol", nullable = false, unique = true)
    private String symbol;

    @Column(name = "current_quantity", nullable = false)
    private Long currentQuantity;

    @Column(name = "current_unit_value", nullable = false, scale = 2)
    private BigDecimal currentUnitValue;

    @Column(name = "last_update", nullable = false)
    private String lastUpdate;

    /**
     * Default constructor for JPA
     */
    public Ticker() { }

    /**
     * Constructor for Ticker
     * @param symbol The symbol of the ticker
     * @param currentQuantity The current quantity of the ticker
     * @param currentUnitValue The current unit value of the ticker
     * @param lastUpdate The last update of the ticker
     */
    public Ticker(String     symbol,
                  Long       currentQuantity,
                  BigDecimal currentUnitValue,
                  String     lastUpdate)
    {
        this.symbol           = symbol;
        this.currentQuantity  = currentQuantity;
        this.currentUnitValue = currentUnitValue;
        this.lastUpdate       = lastUpdate;
    }

    public Long GetId()
    {
        return id;
    }

    public String GetSymbol()
    {
        return symbol;
    }

    public Long GetCurrentQuantity()
    {
        return currentQuantity;
    }

    public BigDecimal GetCurrentUnitValue()
    {
        return currentUnitValue;
    }

    public String GetLastUpdate()
    {
        return lastUpdate;
    }

    public void SetSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public void SetCurrentQuantity(Long currentQuantity)
    {
        this.currentQuantity = currentQuantity;
    }

    public void SetCurrentUnitValue(BigDecimal currentUnitValue)
    {
        this.currentUnitValue = currentUnitValue;
    }

    public void SetLastUpdate(String lastUpdate)
    {
        this.lastUpdate = lastUpdate;
    }
}
