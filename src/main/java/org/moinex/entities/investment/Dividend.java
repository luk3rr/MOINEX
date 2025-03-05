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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.moinex.entities.WalletTransaction;

@Entity
@Table(name = "dividend")
@Getter
@Setter
@NoArgsConstructor
@Builder
public class Dividend
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
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
     * Constructor for testing purposes
     */
    public Dividend(Long id, Ticker ticker, WalletTransaction walletTransaction)
    {
        this.id                = id;
        this.ticker            = ticker;
        this.walletTransaction = walletTransaction;
    }
}
