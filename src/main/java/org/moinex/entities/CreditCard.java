/*
 * Filename: CreditCard.java
 * Created on: August 26, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Represents a credit card
 */
@Entity
@Table(name = "credit_card")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CreditCard
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "operator_id", referencedColumnName = "id")
    private CreditCardOperator operator;

    @Builder.Default
    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name                 = "default_billing_wallet_id",
                referencedColumnName = "id",
                nullable             = true)
    private Wallet defaultBillingWallet = null;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "billing_due_day", nullable = false)
    private Integer billingDueDay;

    @Column(name = "closing_day", nullable = false)
    private Integer closingDay;

    @Column(name = "max_debt", nullable = false, scale = 2)
    private BigDecimal maxDebt;

    @Builder.Default
    @Column(name = "available_rebate", nullable = false, scale = 2)
    private BigDecimal availableRebate = BigDecimal.ZERO;

    @Column(name = "last_four_digits", nullable = true, length = 4)
    private String lastFourDigits;

    @Builder.Default
    @Column(name             = "archived",
            nullable         = false,
            columnDefinition = "boolean default false")
    private Boolean isArchived = false; // Default value is false
}
