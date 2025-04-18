/*
 * Filename: Ticker.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;
import org.moinex.util.enums.TickerType;

/**
 * Class that represents a ticker
 */
@Entity
@Table(name = "ticker")
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Ticker extends Asset
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TickerType type;

    @Column(name = "last_update", nullable = false)
    private String lastUpdate;

    @Builder.Default
    @Column(name             = "archived",
            nullable         = false,
            columnDefinition = "boolean default false")
    private boolean isArchived = false; // Default value is false

    public abstract static class TickerBuilder<C extends   Ticker, B
                                                   extends TickerBuilder<C, B>>
        extends AssetBuilder<C, B>
    {
        public B lastUpdate(LocalDateTime lastUpdate)
        {
            this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

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
        super(name,
              symbol,
              currentQuantity,
              currentUnitValue,
              averageUnitValue,
              BigDecimal.ONE);

        this.id         = id;
        this.type       = type;
        this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
    }

    public LocalDateTime getLastUpdate()
    {
        return LocalDateTime.parse(lastUpdate, Constants.DB_DATE_FORMATTER);
    }

    public void setLastUpdate(LocalDateTime lastUpdate)
    {
        this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
    }
}
