/*
 * Filename: CryptoExchange.java
 * Created on: January 28, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;

/**
 * Class that represents a crypto exchange
 */
@Entity
@Table(name = "crypto_exchange")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class CryptoExchange
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sold_crypto_id", referencedColumnName = "id", nullable = false)
    private Ticker soldCrypto;

    @ManyToOne
    @JoinColumn(name                 = "received_crypto_id",
                referencedColumnName = "id",
                nullable             = false)
    private Ticker receivedCrypto;

    @Column(name = "sold_quantity", nullable = false)
    private BigDecimal soldQuantity;

    @Column(name = "received_quantity", nullable = false)
    private BigDecimal receivedQuantity;

    @Column(name = "date", nullable = false)
    private String date;

    @Column(name = "description" )
    private String description;

    public abstract static class CryptoExchangeBuilder<
        C extends CryptoExchange, B extends CryptoExchangeBuilder<C, B>>
    {
        public B date(LocalDateTime date)
        {
            this.date = date.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    /**
     * Constructor for testing purposes
     */
    public CryptoExchange(Long          id,
                          Ticker        soldCrypto,
                          Ticker        receivedCrypto,
                          BigDecimal    soldQuantity,
                          BigDecimal    receivedQuantity,
                          LocalDateTime date,
                          String        description)
    {
        this.id               = id;
        this.soldCrypto       = soldCrypto;
        this.receivedCrypto   = receivedCrypto;
        this.soldQuantity     = soldQuantity;
        this.receivedQuantity = receivedQuantity;
        this.date             = date.format(Constants.DB_DATE_FORMATTER);
        this.description      = description;
    }

    public LocalDateTime getDate()
    {
        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    public void setDate(LocalDateTime date)
    {
        this.date = date.format(Constants.DB_DATE_FORMATTER);
    }
}
