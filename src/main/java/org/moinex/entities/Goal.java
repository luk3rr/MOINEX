/*
 * Filename: Goal.java
 * Created on: December  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;

/**
 * Represents a goal
 * A goal is a wallet with a target balance and a target date, good for saving money
 * for a specific purpose, e.g., a trip, a new computer, a new bike, etc.
 */
@Entity
@Table(name = "goal")
@PrimaryKeyJoinColumn(name = "wallet_id", referencedColumnName = "id")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Goal extends Wallet
{
    @Column(name = "initial_balance", nullable = false)
    private BigDecimal initialBalance;

    @Column(name = "target_balance", nullable = false, scale = 2)
    private BigDecimal targetBalance;

    @Column(name = "target_date", nullable = false)
    private String targetDate;

    @Column(name = "completion_date")
    private String completionDate;

    @Column(name = "motivation", length = 500)
    private String motivation;

    public abstract static class GoalBuilder<C extends   Goal, B
                                                 extends GoalBuilder<C, B>>
        extends WalletBuilder<C, B>
    {
        public B targetDate(LocalDateTime targetDate)
        {
            this.targetDate = targetDate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }

        public B completionDate(LocalDateTime completionDate)
        {
            if (completionDate == null)
            {
                this.completionDate = null;
                return self();
            }

            this.completionDate = completionDate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    /**
     * Constructor for testing purposes
     */
    public Goal(Long          id,
                String        name,
                BigDecimal    initialBalance,
                BigDecimal    targetBalance,
                LocalDateTime targetDate,
                String        motivation,
                WalletType    walletType)
    {
        super(id, name, initialBalance);

        this.setType(walletType);
        this.initialBalance = initialBalance;
        this.targetBalance  = targetBalance;
        this.targetDate     = targetDate.format(Constants.DB_DATE_FORMATTER);
        this.motivation     = motivation;
    }

    public LocalDateTime getCompletionDate()
    {
        if (completionDate == null)
        {
            return null;
        }

        return LocalDateTime.parse(completionDate, Constants.DB_DATE_FORMATTER);
    }

    public LocalDateTime getTargetDate()
    {
        return LocalDateTime.parse(targetDate, Constants.DB_DATE_FORMATTER);
    }

    public void setTargetDate(LocalDateTime targetDate)
    {
        this.targetDate = targetDate.format(Constants.DB_DATE_FORMATTER);
    }

    public void setCompletionDate(LocalDateTime completionDate)
    {
        if (completionDate == null)
        {
            this.completionDate = null;
            return;
        }

        this.completionDate = completionDate.format(Constants.DB_DATE_FORMATTER);
    }

    public boolean isCompleted()
    {
        return this.completionDate != null;
    }
}
