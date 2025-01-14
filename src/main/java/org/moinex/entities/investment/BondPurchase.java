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
import org.moinex.entities.WalletTransaction;

@Entity
@Table(name = "bond_purchase")
public class BondPurchase extends Transaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bond_id", referencedColumnName = "id", nullable = true)
    private Bond bond;

    /**
     * Default constructor for JPA
     */
    public BondPurchase() { }

    /**
     * Constructor for testing purposes
     * @param id The id of the purchase
     * @param bond The bond of the purchase
     * @param quantity The quantity of the purchase
     * @param unitPrice The unit price of the purchase
     * @param walletTransaction The wallet transaction of the purchase
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

    /**
     * Constructor for BondPurchase
     * @param bond The bond of the purchase
     * @param quantity The quantity of the purchase
     * @param unitPrice The unit price of the purchase
     * @param walletTransaction The wallet transaction of the purchase
     */
    public BondPurchase(Bond              bond,
                        BigDecimal        quantity,
                        BigDecimal        unitPrice,
                        WalletTransaction walletTransaction)
    {
        super(quantity, unitPrice, walletTransaction);
        this.bond = bond;
    }

    public Bond GetBond()
    {
        return bond;
    }
}
