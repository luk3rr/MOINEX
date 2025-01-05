/*
 * Filename: Transaction.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.investment;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;

/**
 * Base class for transactions in the investment domain
 */
@MappedSuperclass
public abstract class Transaction
{
    @ManyToOne
    @JoinColumn(name = "ticker_id", referencedColumnName = "id", nullable = true)
    private Ticker ticker;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Column(name = "unit_price", nullable = false, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "transaction_date", nullable = false)
    private String transactionDate;

    /**
     * Default constructor for JPA
     */
    public Transaction() { }

    /**
     * Constructor for Transaction
     * @param ticker The ticker of the transaction
     * @param quantity The quantity of the transaction
     * @param unitPrice The unit price of the transaction
     * @param transactionDate The transaction date
     */
    public Transaction(Ticker     ticker,
                       Long       quantity,
                       BigDecimal unitPrice,
                       String     transactionDate)
    {
        this.ticker          = ticker;
        this.quantity        = quantity;
        this.unitPrice       = unitPrice;
        this.transactionDate = transactionDate;
    }

    public Ticker GetTicker()
    {
        return ticker;
    }

    public void SetTicker(Ticker ticker)
    {
        this.ticker = ticker;
    }

    public Long GetQuantity()
    {
        return quantity;
    }

    public void SetQuantity(Long quantity)
    {
        this.quantity = quantity;
    }

    public BigDecimal GetUnitPrice()
    {
        return unitPrice;
    }

    public void SetUnitPrice(BigDecimal unitPrice)
    {
        this.unitPrice = unitPrice;
    }

    public String GetTransactionDate()
    {
        return transactionDate;
    }

    public void SetTransactionDate(String transactionDate)
    {
        this.transactionDate = transactionDate;
    }
}
