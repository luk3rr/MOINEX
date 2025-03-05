/*
 * Filename: BondPurchase.java
 * Created on: January 14, 2025
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
import org.moinex.entities.WalletTransaction;

@Entity
@Table(name = "bond_purchase")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class BondPurchase extends Transaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bond_id", referencedColumnName = "id", nullable = true)
    private Bond bond;

    /**
     * Constructor for testing purposes
     */
    public BondPurchase(Long              id,
                        Bond              bond,
                        BigDecimal        quantity,
                        BigDecimal        unitPrice,
                        WalletTransaction walletTransaction)
    {
        super(quantity, unitPrice, walletTransaction);

        this.id   = id;
        this.bond = bond;
    }
}
