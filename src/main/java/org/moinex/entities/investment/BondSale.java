/*
 * Filename: BondSale.java
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
@Table(name = "bond_sale")
public class BondSale extends Transaction
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
    public BondSale() { }

    /**
     * Constructor for testing purposes
     * @param id The id of the sale
     * @param bond The bond of the sale
     * @param quantity The quantity of the sale
     * @param unitPrice The unit price of the sale
     * @param walletTransaction The wallet transaction of the sale
     */
    public BondSale(Long              id,
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
