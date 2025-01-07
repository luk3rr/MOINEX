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
import java.time.LocalDateTime;
import org.moinex.util.Constants;

/**
 * Base class for transactions in the investment domain
 */
@MappedSuperclass
public abstract class Transaction
{
    @ManyToOne
    @JoinColumn(name = "ticker_id", referencedColumnName = "id", nullable = true)
    private Ticker ticker;

    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

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
    public Transaction(Ticker        ticker,
                       BigDecimal    quantity,
                       BigDecimal    unitPrice,
                       LocalDateTime transactionDate)
    {
        this.ticker          = ticker;
        this.quantity        = quantity;
        this.unitPrice       = unitPrice;
        this.transactionDate = transactionDate.format(Constants.DB_DATE_FORMATTER);
    }

    public Ticker GetTicker()
    {
        return ticker;
    }

    public void SetTicker(Ticker ticker)
    {
        this.ticker = ticker;
    }

    public BigDecimal GetQuantity()
    {
        return quantity;
    }

    public void SetQuantity(BigDecimal quantity)
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

    public LocalDateTime GetTransactionDate()
    {
        return LocalDateTime.parse(transactionDate, Constants.DB_DATE_FORMATTER);
    }

    public void SetTransactionDate(LocalDateTime transactionDate)
    {
        this.transactionDate = transactionDate.format(Constants.DB_DATE_FORMATTER);
    }
}
