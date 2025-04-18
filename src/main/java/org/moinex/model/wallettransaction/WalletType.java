/*
 * Filename: wallet_type.java
 * Created on: September 29, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.wallettransaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a wallet type
 * A wallet type is a category of wallets
 */
@Entity
@Table(name = "wallet_type")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletType
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "icon",  length = 30)
    private String icon;

    /**
     * Constructor for testing purposes
     */
    public WalletType(Long id, String name)
    {
        this.id   = id;
        this.name = name;
    }
}
