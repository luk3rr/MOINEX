/*
 * Filename: Sale.java
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
@Table(name = "sale")
public class Sale extends Transaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "average_cost", nullable = false)
    private BigDecimal averageCost;

    /**
     * Default constructor for JPA
     */
    public Sale() { }

    /**
     * Constructor for testing purposes
     * @param id The id of the sale
     * @param ticker The ticker of the sale
     * @param quantity The quantity of the sale
     * @param unitPrice The unit price of the sale
     * @param walletTransaction The wallet transaction of the sale
     * @param averageCost The average cost of the sale at the moment of the transaction
     */
    public Sale(Long              id,
                Ticker            ticker,
                BigDecimal        quantity,
                BigDecimal        unitPrice,
                WalletTransaction walletTransaction,
                BigDecimal        averageCost)
    {
        super(ticker, quantity, unitPrice, walletTransaction);
        this.id          = id;
        this.averageCost = averageCost;
    }

    /**
     * Constructor for Sale
     * @param ticker The ticker of the sale
     * @param quantity The quantity of the sale
     * @param unitPrice The unit price of the sale
     * @param walletTransaction The wallet transaction of the sale
     * @param averageCost The average cost of the sale at the moment of the transaction
     */
    public Sale(Ticker            ticker,
                BigDecimal        quantity,
                BigDecimal        unitPrice,
                WalletTransaction walletTransaction,
                BigDecimal        averageCost)
    {
        super(ticker, quantity, unitPrice, walletTransaction);
        this.averageCost = averageCost;
    }

    public Long GetId()
    {
        return id;
    }

    public BigDecimal GetAverageCost()
    {
        return averageCost;
    }

    public void SetAverageCost(BigDecimal averageCost)
    {
        this.averageCost = averageCost;
    }
}
