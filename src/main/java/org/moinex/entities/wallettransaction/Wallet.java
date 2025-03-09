/*
 * Filename: Wallet.java
 * Created on: March 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.wallettransaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class Wallet
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "type_id", referencedColumnName = "id")
    private WalletType type;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "balance", nullable = false, scale = 2)
    private BigDecimal balance;

    @Builder.Default
    @Column(name             = "archived",
            nullable         = false,
            columnDefinition = "boolean default false")
    private boolean isArchived = false; // Default value is false

    /**
     * Constructor for testing purposes
     */
    public Wallet(Long id, String name, BigDecimal balance)
    {
        this.id      = id;
        this.name    = name;
        this.balance = balance;
    }
}
