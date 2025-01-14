/*
 * Filename: TickerPurchase.java
 * Created on: January  5, 2025
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
import org.moinex.entities.WalletTransaction;

@Entity
@Table(name = "ticker_purchase")
public class TickerPurchase extends Transaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticker_id", referencedColumnName = "id", nullable = true)
    private Ticker ticker;

    /**
     * Default constructor for JPA
     */
    public TickerPurchase() { }

    /**
     * Constructor for testing purposes
     * @param id The id of the purchase
     * @param ticker The ticker of the purchase
     * @param quantity The quantity of the purchase
     * @param unitPrice The unit price of the purchase
     * @param walletTransaction The wallet transaction of the purchase
     */
    public TickerPurchase(Long              id,
                          Ticker            ticker,
                          BigDecimal        quantity,
                          BigDecimal        unitPrice,
                          WalletTransaction walletTransaction)
    {
        super(quantity, unitPrice, walletTransaction);
        this.id     = id;
        this.ticker = ticker;
    }

    /**
     * Constructor for TickerPurchase
     * @param ticker The ticker of the purchase
     * @param quantity The quantity of the purchase
     * @param unitPrice The unit price of the purchase
     * @param walletTransaction The wallet transaction of the purchase
     */
    public TickerPurchase(Ticker            ticker,
                          BigDecimal        quantity,
                          BigDecimal        unitPrice,
                          WalletTransaction walletTransaction)
    {
        super(quantity, unitPrice, walletTransaction);
        this.ticker = ticker;
    }

    public Long GetId()
    {
        return id;
    }

    public Ticker GetTicker()
    {
        return ticker;
    }

    public void SetTicker(Ticker ticker)
    {
        this.ticker = ticker;
    }
}
