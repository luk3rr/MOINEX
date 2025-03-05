/*
 * Filename: CreditCardTransaction.java
 * Created on: March  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;

/**
 * Base class for transactions
 */
@MappedSuperclass
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class CreditCardTransaction
{
    @ManyToOne
    @JoinColumn(name = "crc_id", referencedColumnName = "id", nullable = false)
    private CreditCard creditCard;

    @Column(name = "date", nullable = false)
    private String date;

    @Column(name             = "amount",
            nullable         = false,
            scale            = 2)
    private BigDecimal amount;

    @Column(name = "description", nullable = true)
    private String description;

    public static abstract class CreditCardTransactionBuilder<
        C extends CreditCardTransaction, B extends CreditCardTransactionBuilder<C, B>>
    {
        public B date(LocalDateTime date)
        {
            this.date = date.format(Constants.DB_DATE_FORMATTER);
            return self();
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
