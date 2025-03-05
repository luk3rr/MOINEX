/*
 * Filename: CreditCardDebt.java
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.moinex.util.Constants;

/**
 * Represents a credit card debt
 * A credit card debt is a debt that is associated with a credit card
 */
@Entity
@Table(name = "credit_card_debt")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardDebt
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "crc_id", referencedColumnName = "id", nullable = false)
    private CreditCard creditCard;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = false)
    private Category category;

    @Column(name = "date", nullable = false)
    private String date;

    @Column(name = "total_amount", nullable = false, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "installments", nullable = false)
    private Integer installments;

    @Column(name = "description", nullable = true)
    private String description;

    public static class CreditCardDebtBuilder
    {
        public CreditCardDebtBuilder date(LocalDateTime date)
        {
            this.date = date.format(Constants.DB_DATE_FORMATTER);
            return this;
        }
    }

    public LocalDateTime getDate()
    {
        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    public void setDate(LocalDateTime date)
    {
        this.date = date.format(Constants.DB_DATE_FORMATTER);
    }
}
