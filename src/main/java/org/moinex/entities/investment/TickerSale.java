/*
 * Filename: TickerSale.java
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.entities.wallettransaction.WalletTransaction;

@Entity
@Table(name = "ticker_sale")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class TickerSale extends Transaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticker_id", referencedColumnName = "id" )
    private Ticker ticker;

    @Column(name = "average_cost", nullable = false)
    private BigDecimal averageCost;

    /**
     * Constructor for testing purposes
     */
    public TickerSale(Long              id,
                      Ticker            ticker,
                      BigDecimal        quantity,
                      BigDecimal        unitPrice,
                      WalletTransaction walletTransaction,
                      BigDecimal        averageCost)
    {
        super(quantity, unitPrice, walletTransaction);

        this.id          = id;
        this.ticker      = ticker;
        this.averageCost = averageCost;
    }
}
