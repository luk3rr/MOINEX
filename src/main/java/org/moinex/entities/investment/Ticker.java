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
import java.time.LocalDateTime;
import org.moinex.util.Constants;
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

    @Column(name             = "archived",
            nullable         = false,
            columnDefinition = "boolean default false")
    private Boolean archived = false; // Default value is false

    /**
     * Default constructor for JPA
     */
    public Ticker() { }

    /**
     * Constructor for testing purposes
     * @param id The id of the ticker
     * @param name The name of the ticker
     * @param symbol The symbol of the ticker
     * @param type The type of the ticker
     * @param currentQuantity The current quantity of the ticker
     * @param currentUnitValue The current unit value of the ticker
     * @param lastUpdate The last update of the ticker
     */
    public Ticker(Long          id,
                  String        name,
                  String        symbol,
                  TickerType    type,
                  BigDecimal    currentQuantity,
                  BigDecimal    currentUnitValue,
                  BigDecimal    averageUnitValue,
                  LocalDateTime lastUpdate)
    {
        super(name, symbol, currentQuantity, currentUnitValue, averageUnitValue, 1);
        this.id         = id;
        this.type       = type;
        this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
    }

    /**
     * Constructor for Ticker
     * @param name The name of the ticker
     * @param symbol The symbol of the ticker
     * @param type The type of the ticker
     * @param currentQuantity The current quantity of the ticker
     * @param currentUnitValue The current unit value of the ticker
     * @param averageUnitValue The average unit value of the ticker
     * @param lastUpdate The last update of the ticker
     */
    public Ticker(String        name,
                  String        symbol,
                  TickerType    type,
                  BigDecimal    currentQuantity,
                  BigDecimal    currentUnitValue,
                  BigDecimal    averageUnitValue,
                  LocalDateTime lastUpdate)
    {
        super(name, symbol, currentQuantity, currentUnitValue, averageUnitValue, 1);
        this.type       = type;
        this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
    }

    public Long GetId()
    {
        return id;
    }

    public TickerType GetType()
    {
        return type;
    }

    public LocalDateTime GetLastUpdate()
    {
        return LocalDateTime.parse(lastUpdate, Constants.DB_DATE_FORMATTER);
    }

    public Boolean IsArchived()
    {
        return archived;
    }

    public void SetType(TickerType type)
    {
        this.type = type;
    }

    public void SetLastUpdate(LocalDateTime lastUpdate)
    {
        this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
    }

    public void SetArchived(Boolean archived)
    {
        this.archived = archived;
    }
}
