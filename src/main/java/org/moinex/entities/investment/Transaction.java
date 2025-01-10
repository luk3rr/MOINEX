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
import org.moinex.entities.WalletTransaction;

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

    @ManyToOne
    @JoinColumn(name                 = "wallet_transaction_id",
                referencedColumnName = "id",
                nullable             = false)
    private WalletTransaction walletTransaction;

    /**
     * Default constructor for JPA
     */
    public Transaction() { }

    /**
     * Constructor for Transaction
     * @param ticker The ticker of the transaction
     * @param quantity The quantity of the transaction
     * @param unitPrice The unit price of the transaction
     * @param walletTransaction The wallet transaction of the transaction
     */
    public Transaction(Ticker            ticker,
                       BigDecimal        quantity,
                       BigDecimal        unitPrice,
                       WalletTransaction walletTransaction)
    {
        this.ticker            = ticker;
        this.quantity          = quantity;
        this.unitPrice         = unitPrice;
        this.walletTransaction = walletTransaction;
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

    public WalletTransaction GetWalletTransaction()
    {
        return walletTransaction;
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

    public void SetWalletTransaction(WalletTransaction walletTransaction)
    {
        this.walletTransaction = walletTransaction;
    }
}
