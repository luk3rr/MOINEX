/*
 * Filename: Ticker.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.moinex.util.TickerType;

/**
 * Class that represents a ticker
 */
@Entity
@Table(name = "ticker")
public class Ticker extends Asset
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TickerType type;

    @Column(name = "last_update", nullable = false)
    private String lastUpdate;

    /**
     * Default constructor for JPA
     */
    public Ticker() { }

    /**
     * Constructor for Ticker
     * @param name The name of the ticker
     * @param symbol The symbol of the ticker
     * @param type The type of the ticker
     * @param currentQuantity The current quantity of the ticker
     * @param currentUnitValue The current unit value of the ticker
     * @param lastUpdate The last update of the ticker
     */
    public Ticker(String     name,
                  String     symbol,
                  TickerType type,
                  BigDecimal currentQuantity,
                  BigDecimal currentUnitValue,
                  String     lastUpdate)
    {
        super(name, symbol, currentQuantity, currentUnitValue);
        this.type = type;
        this.lastUpdate = lastUpdate;
    }

    public Long GetId()
    {
        return id;
    }

    public TickerType GetType()
    {
        return type;
    }

    public String GetLastUpdate()
    {
        return lastUpdate;
    }

    public void SetType(TickerType type)
    {
        this.type = type;
    }

    public void SetLastUpdate(String lastUpdate)
    {
        this.lastUpdate = lastUpdate;
    }
}
