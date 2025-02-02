/*
 * Filename: CryptoExchange.java
 * Created on: January 28, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.investment;

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
import org.moinex.util.Constants;

/**
 * Class that represents a crypto exchange
 */
@Entity
@Table(name = "crypto_exchange")
public class CryptoExchange
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
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

    @Column(name = "description", nullable = true)
    private String description;

    /**
     * Default constructor for JPA
     */
    public CryptoExchange() { }

    /**
     * Constructor for testing purposes
     * @param id The id of the exchange
     * @param soldCrypto The source crypto of the exchange
     * @param receivedCrypto The target crypto of the exchange
     * @param soldQuantity The source quantity of the exchange
     * @param receivedQuantity The target quantity of the exchange
     * @param date The date of the exchange
     * @param description A description of the exchange
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

    /**
     * Constructor for testing purposes
     * @param soldCrypto The source crypto of the exchange
     * @param receivedCrypto The target crypto of the exchange
     * @param soldQuantity The source quantity of the exchange
     * @param receivedQuantity The target quantity of the exchange
     * @param date The date of the exchange
     */
    public CryptoExchange(Ticker        soldCrypto,
                          Ticker        receivedCrypto,
                          BigDecimal    soldQuantity,
                          BigDecimal    receivedQuantity,
                          LocalDateTime date,
                          String        description)
    {
        this.soldCrypto       = soldCrypto;
        this.receivedCrypto   = receivedCrypto;
        this.soldQuantity     = soldQuantity;
        this.receivedQuantity = receivedQuantity;
        this.date             = date.format(Constants.DB_DATE_FORMATTER);
        this.description      = description;
    }

    public Long GetId()
    {
        return id;
    }

    public Ticker GetSoldCrypto()
    {
        return soldCrypto;
    }

    public Ticker GetReceivedCrypto()
    {
        return receivedCrypto;
    }

    public BigDecimal GetSoldQuantity()
    {
        return soldQuantity;
    }

    public BigDecimal GetReceivedQuantity()
    {
        return receivedQuantity;
    }

    public LocalDateTime GetDate()
    {
        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    public String GetDescription()
    {
        return description;
    }

    public void SetSoldCrypto(Ticker soldCrypto)
    {
        this.soldCrypto = soldCrypto;
    }

    public void SetReceivedCrypto(Ticker receivedCrypto)
    {
        this.receivedCrypto = receivedCrypto;
    }

    public void SetSoldQuantity(BigDecimal soldQuantity)
    {
        this.soldQuantity = soldQuantity;
    }

    public void SetReceivedQuantity(BigDecimal receivedQuantity)
    {
        this.receivedQuantity = receivedQuantity;
    }

    public void SetDate(LocalDateTime date)
    {
        this.date = date.format(Constants.DB_DATE_FORMATTER);
    }

    public void SetDescription(String description)
    {
        this.description = description;
    }
}
