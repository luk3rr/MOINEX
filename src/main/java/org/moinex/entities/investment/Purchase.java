/*
 * Filename: Purchase.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.moinex.entities.WalletTransaction;

@Entity
@Table(name = "purchase")
public class Purchase extends Transaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Default constructor for JPA
     */
    public Purchase() { }

    /**
     * Constructor for testing purposes
     * @param id The id of the purchase
     * @param ticker The ticker of the purchase
     * @param quantity The quantity of the purchase
     * @param unitPrice The unit price of the purchase
     * @param walletTransaction The wallet transaction of the purchase
     */
    public Purchase(Long              id,
                    Ticker            ticker,
                    BigDecimal        quantity,
                    BigDecimal        unitPrice,
                    WalletTransaction walletTransaction)
    {
        super(ticker, quantity, unitPrice, walletTransaction);
        this.id = id;
    }

    /**
     * Constructor for Purchase
     * @param ticker The ticker of the purchase
     * @param quantity The quantity of the purchase
     * @param unitPrice The unit price of the purchase
     * @param walletTransaction The wallet transaction of the purchase
     */
    public Purchase(Ticker            ticker,
                    BigDecimal        quantity,
                    BigDecimal        unitPrice,
                    WalletTransaction walletTransaction)
    {
        super(ticker, quantity, unitPrice, walletTransaction);
    }

    public Long GetId()
    {
        return id;
    }
}
