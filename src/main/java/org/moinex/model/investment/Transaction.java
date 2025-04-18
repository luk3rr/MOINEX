/*
 * Filename: Transaction.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.model.wallettransaction.WalletTransaction;

/**
 * Base class for transactions in the investment domain
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Transaction
{
    @Column(name = "quantity", nullable = false, precision = 20, scale = 8)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, scale = 2)
    private BigDecimal unitPrice;

    @ManyToOne
    @JoinColumn(name                 = "wallet_transaction_id",
                referencedColumnName = "id",
                nullable             = false)
    private WalletTransaction walletTransaction;
}
