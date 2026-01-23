/*
 * Filename: NetWorthSnapshot.java
 * Created on: January 22, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a cached snapshot of net worth calculation for a specific month
 * This avoids expensive recalculations on every page load
 */
@Entity
@Table(name = "net_worth_snapshot")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetWorthSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "assets", nullable = false, scale = 2)
    private BigDecimal assets;

    @Column(name = "liabilities", nullable = false, scale = 2)
    private BigDecimal liabilities;

    @Column(name = "net_worth", nullable = false, scale = 2)
    private BigDecimal netWorth;

    @Column(name = "wallet_balances", nullable = false, scale = 2)
    private BigDecimal walletBalances;

    @Column(name = "investments", nullable = false, scale = 2)
    private BigDecimal investments;

    @Column(name = "credit_card_debt", nullable = false, scale = 2)
    private BigDecimal creditCardDebt;

    @Column(name = "negative_wallet_balances", nullable = false, scale = 2)
    private BigDecimal negativeWalletBalances;

    @Column(name = "calculated_at", nullable = false)
    private String calculatedAt;
}
