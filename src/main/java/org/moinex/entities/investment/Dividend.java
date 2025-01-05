/*
 * Filename: Dividend.java
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
import org.moinex.entities.WalletTransaction;

@Entity
@Table(name = "dividend")
public class Dividend
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticker_id", referencedColumnName = "id", nullable = false)
    private Ticker ticker;

    @ManyToOne
    @JoinColumn(name                 = "wallet_transaction_id",
                referencedColumnName = "id",
                nullable             = false)
    private WalletTransaction walletTransaction;

    /**
     * Default constructor for JPA
     */
    public Dividend() { }

    /**
     * Constructor for Dividend
     * @param ticker The ticker of the dividend
     * @param walletTransaction The wallet transaction of the dividend
     */
    public Dividend(Ticker ticker, WalletTransaction walletTransaction)
    {
        this.ticker            = ticker;
        this.walletTransaction = walletTransaction;
    }

    public Long GetId()
    {
        return id;
    }

    public Ticker GetTicker()
    {
        return ticker;
    }

    public WalletTransaction GetWalletTransaction()
    {
        return walletTransaction;
    }

    public void SetTicker(Ticker ticker)
    {
        this.ticker = ticker;
    }

    public void SetWalletTransaction(WalletTransaction walletTransaction)
    {
        this.walletTransaction = walletTransaction;
    }
}
