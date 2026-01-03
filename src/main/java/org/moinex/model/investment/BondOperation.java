/*
 * Filename: BondOperation.java
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.util.enums.OperationType;

@Entity
@Table(name = "bond_operation")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class BondOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bond_id", referencedColumnName = "id", nullable = false)
    private Bond bond;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operationType;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "fees")
    private BigDecimal fees;

    @Column(name = "taxes")
    private BigDecimal taxes;

    @Column(name = "net_profit")
    private BigDecimal netProfit;

    @ManyToOne
    @JoinColumn(name = "wallet_transaction_id", referencedColumnName = "id")
    private WalletTransaction walletTransaction;
}
