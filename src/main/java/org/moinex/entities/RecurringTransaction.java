/*
 * Filename: RecurringTransaction.java
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;
import org.moinex.util.enums.RecurringTransactionFrequency;
import org.moinex.util.enums.RecurringTransactionStatus;
import org.moinex.util.enums.TransactionType;

@Entity
@Inheritance
@Table(name = "recurring_transaction")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class RecurringTransaction extends BaseTransaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "start_date", nullable = false)
    private String startDate;

    @Column(name = "end_date", nullable = false)
    private String endDate;

    @Column(name = "next_due_date", nullable = false)
    private String nextDueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private RecurringTransactionFrequency frequency;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name             = "status",
            nullable         = false,
            columnDefinition = "varchar default 'ACTIVE'")
    private RecurringTransactionStatus status = RecurringTransactionStatus.ACTIVE;

    public abstract static class RecurringTransactionBuilder<
        C extends RecurringTransaction, B extends RecurringTransactionBuilder<C, B>>
        extends BaseTransactionBuilder<C, B>
    {
        public B startDate(LocalDateTime startDate)
        {
            this.startDate = startDate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }

        public B endDate(LocalDateTime endDate)
        {
            this.endDate = endDate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }

        public B nextDueDate(LocalDateTime nextDueDate)
        {
            this.nextDueDate =
                nextDueDate.with(Constants.RECURRING_TRANSACTION_DUE_DATE_DEFAULT_TIME)
                    .format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    /**
     * Constructor for testing purposes
     */
    public RecurringTransaction(Long                          id,
                                Wallet                        wallet,
                                Category                      category,
                                TransactionType               type,
                                BigDecimal                    amount,
                                LocalDateTime                 startDate,
                                LocalDateTime                 endDate,
                                LocalDateTime                 nextDueDate,
                                RecurringTransactionFrequency frequency,
                                String                        description)
    {
        super(wallet, category, type, amount, description);

        this.id        = id;
        this.startDate = startDate.format(Constants.DB_DATE_FORMATTER);
        this.endDate   = endDate.format(Constants.DB_DATE_FORMATTER);
        this.nextDueDate =
            nextDueDate.with(Constants.RECURRING_TRANSACTION_DUE_DATE_DEFAULT_TIME)
                .format(Constants.DB_DATE_FORMATTER);
        this.frequency = frequency;
        this.status    = RecurringTransactionStatus.ACTIVE;
    }

    public LocalDateTime getStartDate()
    {
        return LocalDateTime.parse(startDate, Constants.DB_DATE_FORMATTER);
    }

    public LocalDateTime getEndDate()
    {
        return LocalDateTime.parse(endDate, Constants.DB_DATE_FORMATTER);
    }

    public LocalDateTime getNextDueDate()
    {
        return LocalDateTime.parse(nextDueDate, Constants.DB_DATE_FORMATTER);
    }

    public void setStartDate(LocalDateTime startDate)
    {
        this.startDate = startDate.format(Constants.DB_DATE_FORMATTER);
    }

    public void setEndDate(LocalDateTime endDate)
    {
        this.endDate = endDate.format(Constants.DB_DATE_FORMATTER);
    }

    public void setNextDueDate(LocalDateTime nextDueDate)
    {
        // Set the default time for the next due date
        this.nextDueDate =
            nextDueDate.with(Constants.RECURRING_TRANSACTION_DUE_DATE_DEFAULT_TIME)
                .format(Constants.DB_DATE_FORMATTER);
    }
}
