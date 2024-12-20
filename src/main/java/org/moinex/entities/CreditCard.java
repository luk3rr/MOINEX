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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Represents a credit card
 */
@Entity
@Table(name = "credit_card")
public class CreditCard
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name = "operator_id", referencedColumnName = "id")
    private CreditCardOperator operator;

    @ManyToOne()
    @OnDelete(action = OnDeleteAction.NO_ACTION)
    @JoinColumn(name                 = "default_billing_wallet_id",
                referencedColumnName = "id",
                nullable             = true)
    private Wallet defaultBillingWallet;

    @Column(name = "name", nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "billing_due_day", nullable = false)
    private Integer billingDueDay;

    @Column(name = "closing_day", nullable = false)
    private Integer closingDay;

    @Column(name = "max_debt", nullable = false, scale = 2)
    private BigDecimal maxDebt;

    @Column(name = "last_four_digits", nullable = true, length = 4)
    private String lastFourDigits;

    @Column(name             = "archived",
            nullable         = false,
            columnDefinition = "boolean default false")
    private Boolean archived = false; // Default value is false

    /**
     * Default constructor for JPA
     */
    public CreditCard() { }

    /**
     * Constructor for CreditCard
     * @param name The name of the credit card
     * @param billingDueDay The day of the month the bill is due
     * @param closingDay The day of the month the bill is closed
     * @param maxDebt The maximum debt allowed for the credit card
     * @param lastFourDigits The last four digits of the credit card
     */
    public CreditCard(String             name,
                      Integer            billingDueDay,
                      Integer            closingDay,
                      BigDecimal         maxDebt,
                      String             lastFourDigits,
                      CreditCardOperator operator)
    {
        this.name           = name;
        this.billingDueDay  = billingDueDay;
        this.closingDay     = closingDay;
        this.maxDebt        = maxDebt;
        this.lastFourDigits = lastFourDigits;
        this.operator       = operator;

        this.defaultBillingWallet = null;
    }

    /**
     * Constructor for CreditCard
     * @param name The name of the credit card
     * @param billingDueDay The day of the month the bill is due
     * @param closingDay The day of the month the bill is closed
     * @param maxDebt The maximum debt allowed for the credit card
     * @param lastFourDigits The last four digits of the credit card
     * @param operator The operator of the credit card
     * @param defaultBillingWallet The default billing wallet of the credit card
     */
    public CreditCard(String             name,
                      Integer            billingDueDay,
                      Integer            closingDay,
                      BigDecimal         maxDebt,
                      String             lastFourDigits,
                      CreditCardOperator operator,
                      Wallet             defaultBillingWallet)
    {
        this.name                 = name;
        this.billingDueDay        = billingDueDay;
        this.closingDay           = closingDay;
        this.maxDebt              = maxDebt;
        this.lastFourDigits       = lastFourDigits;
        this.operator             = operator;
        this.defaultBillingWallet = defaultBillingWallet;
    }

    /**
     * Get the id of the credit card
     * @return The id of the credit card
     */
    public Long GetId()
    {
        return id;
    }

    /**
     * Get the operator of the credit card
     * @return The operator of the credit card
     */
    public CreditCardOperator GetOperator()
    {
        return operator;
    }

    /**
     * Get the default billing wallet of the credit card
     * @return The default billing wallet of the credit card
     */
    public Wallet GetDefaultBillingWallet()
    {
        return defaultBillingWallet;
    }

    /**
     * Get the name of the credit card
     * @return The name of the credit card
     */
    public String GetName()
    {
        return name;
    }

    /**
     * Get the day of the month the bill is due
     * @return The day of the month the bill is due
     */
    public Integer GetBillingDueDay()
    {
        return billingDueDay;
    }

    /**
     * Get the day of the month the bill is closed
     * @return The day of the month the bill is closed
     */
    public Integer GetClosingDay()
    {
        return closingDay;
    }

    /**
     * Get the maximum debt allowed for the credit card
     * @return The maximum debt allowed for the credit card
     */
    public BigDecimal GetMaxDebt()
    {
        return maxDebt;
    }

    /**
     * Get the last four digits of the credit card
     * @return The last four digits of the credit card
     */
    public String GetLastFourDigits()
    {
        return lastFourDigits;
    }

    /**
     * Get the archived status of the credit card
     * @return The archived status of the credit card
     */
    public Boolean IsArchived()
    {
        return archived;
    }

    /**
     * Set the operator of the credit card
     * @param operator The new operator of the credit card
     */
    public void SetOperator(CreditCardOperator operator)
    {
        this.operator = operator;
    }

    /**
     * Set the default billing wallet of the credit card
     * @param defaultBillingWallet The new default billing wallet of the credit card
     */
    public void SetDefaultBillingWallet(Wallet defaultBillingWallet)
    {
        this.defaultBillingWallet = defaultBillingWallet;
    }

    /**
     * Set the name of the credit card
     * @param name The new name of the credit card
     */
    public void SetName(String name)
    {
        this.name = name;
    }

    /**
     * Set the day of the month the bill is due
     * @param billingDueDay The new day of the month the bill is due
     */
    public void SetBillingDueDay(Integer billingDueDay)
    {
        this.billingDueDay = billingDueDay;
    }

    /**
     * Set the day of the month the bill is closed
     * @param closingDay The new day of the month the bill is closed
     */
    public void SetClosingDay(Integer closingDay)
    {
        this.closingDay = closingDay;
    }

    /**
     * Set the maximum debt allowed for the credit card
     * @param maxDebt The new maximum debt allowed for the credit card
     */
    public void SetMaxDebt(BigDecimal maxDebt)
    {
        this.maxDebt = maxDebt;
    }

    /**
     * Set the last four digits of the credit card
     * @param lastFourDigits The new last four digits of the credit card
     */
    public void SetLastFourDigits(String lastFourDigits)
    {
        this.lastFourDigits = lastFourDigits;
    }

    /**
     * Set the archived status of the credit card
     * @param archived The new archived status of the credit card
     */
    public void SetArchived(Boolean archived)
    {
        this.archived = archived;
    }
}
