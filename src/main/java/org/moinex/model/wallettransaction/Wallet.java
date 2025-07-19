/*
 * Filename: Wallet.java
 * Created on: March 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.wallettransaction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Represents a wallet
 * A wallet is a container for money
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "wallet")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "type_id", referencedColumnName = "id")
    private WalletType type;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "balance", nullable = false, scale = 2)
    private BigDecimal balance;

    @Builder.Default
    @Column(name = "archived", nullable = false)
    private boolean isArchived = false; // Default value is false

    @ManyToOne
    @JoinColumn(name = "master_wallet_id", referencedColumnName = "id")
    private Wallet masterWallet;

    /**
     * Constructor for testing purposes
     */
    public Wallet(Integer id, String name, BigDecimal balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return id != null && id.equals(wallet.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
}
