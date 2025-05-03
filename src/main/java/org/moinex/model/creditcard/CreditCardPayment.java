/*
 * Filename: CreditCardPayment.java
 * Created on: August 26, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.creditcard;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.util.Constants;

/**
 * Represents a credit card payment
 * A credit card payment is a payment made to a credit card debt
 */
@Entity
@Table(name = "credit_card_payment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "wallet_id", referencedColumnName = "id")
    private Wallet wallet;

    @ManyToOne
    @JoinColumn(name = "debt_id", referencedColumnName = "id", nullable = false)
    private CreditCardDebt creditCardDebt;

    @Column(name = "date", nullable = false)
    private String date;

    @Column(name = "amount", nullable = false, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "rebateUsed", nullable = false, scale = 2)
    private BigDecimal rebateUsed = BigDecimal.ZERO;

    @Column(name = "installment", nullable = false)
    private Integer installment;

    public static class CreditCardPaymentBuilder {
        public CreditCardPaymentBuilder date(LocalDateTime date) {
            this.date = date.format(Constants.DB_DATE_FORMATTER);
            return this;
        }
    }

    public LocalDateTime getDate() {
        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    public void setDate(LocalDateTime date) {
        this.date = date.format(Constants.DB_DATE_FORMATTER);
    }
}
