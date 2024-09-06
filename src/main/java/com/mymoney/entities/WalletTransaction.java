/*
 * Filename: WalletTransaction.java
 * Created on: August 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package com.mymoney.entities;

import com.mymoney.util.TransactionType;
import java.time.LocalDate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a transaction in a wallet
 */
@Entity
@Table(name = "wallet_transaction")
public class WalletTransaction
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "transaction_id")
    private Long m_id;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "wallet", referencedColumnName = "name")
    private Wallet m_wallet;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "category_id")
    private Category m_category;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TransactionType m_type;

    @Column(name = "date", nullable = false)
    private LocalDate m_date;

    @Column(name = "amount", nullable = false)
    private Double m_amount;

    @Column(name = "description")
    private String m_description;

    /**
     * Default constructor for JPA
     */
    public WalletTransaction() { }

    /**
     * Constructor for WalletTransaction
     * @param wallet The wallet that the transaction belongs to
     * @param category The category of the transaction
     * @param type The type of the transaction
     * @param date The date of the transaction
     * @param amount The amount of the transaction
     * @param description A description of the transaction
     */
    public WalletTransaction(Wallet          wallet,
                             Category        category,
                             TransactionType type,
                             LocalDate       date,
                             Double          amount,
                             String          description)
    {
        m_wallet      = wallet;
        m_category    = category;
        m_type        = type;
        m_date        = date;
        m_amount      = amount;
        m_description = description;
    }

    /**
     * Get the transaction id
     * @return The transaction id
     */
    public Long GetId()
    {
        return m_id;
    }

    /**
     * Get the wallet that the transaction belongs to
     * @return The wallet that the transaction belongs to
     */
    public Wallet GetWallet()
    {
        return m_wallet;
    }

    /**
     * Get the category of the transaction
     * @return The category of the transaction
     */
    public Category GetCategory()
    {
        return m_category;
    }

    /**
     * Get the type of the transaction
     * @return The type of the transaction
     */
    public LocalDate GetDate()
    {
        return m_date;
    }

    /**
     * Get the description of the transaction
     * @return The description of the transaction
     */
    public String GetDescription()
    {
        return m_description;
    }

    /**
     * Get the amount of the transaction
     * @return The amount of the transaction
     */
    public Double GetAmount()
    {
        return m_amount;
    }

    /**
     * Get the type of the transaction
     * @return The type of the transaction
     */
    public TransactionType GetType()
    {
        return m_type;
    }

    /**
     * Set the wallet that the transaction belongs to
     * @param wallet The wallet that the transaction belongs to
     */
    public void SetWallet(Wallet wallet)
    {
        m_wallet = wallet;
    }

    /**
     * Set the category of the transaction
     * @param category The category of the transaction
     */
    public void SetCategory(Category category)
    {
        m_category = category;
    }

    /**
     * Set the type of the transaction
     * @param type The type of the transaction
     */
    public void SetDate(LocalDate date)
    {
        m_date = date;
    }

    /**
     * Set the description of the transaction
     * @param description The description of the transaction
     */
    public void SetDescription(String description)
    {
        m_description = description;
    }

    /**
     * Set the amount of the transaction
     * @param amount The amount of the transaction
     */
    public void SetAmount(Double amount)
    {
        m_amount = amount;
    }

    /**
     * Set the type of the transaction
     * @param type The type of the transaction
     */
    public void SetType(TransactionType type)
    {
        m_type = type;
    }
}