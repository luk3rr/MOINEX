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
import java.time.LocalDateTime;

@Entity
@Table(name = "sale")
public class Sale extends Transaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Default constructor for JPA
     */
    public Sale() { }

    /**
     * Constructor for Sale
     * @param ticker The ticker of the sale
     * @param quantity The quantity of the sale
     * @param unitPrice The unit price of the sale
     * @param saleDate The sale date
     */
    public Sale(Ticker     ticker,
                BigDecimal quantity,
                BigDecimal unitPrice,
                LocalDateTime saleDate)
    {
        super(ticker, quantity, unitPrice, saleDate);
    }

    public Long GetId()
    {
        return id;
    }
}
