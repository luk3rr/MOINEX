/*
 * Filename: Transfer.java
 * Created on: August 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities;

import jakarta.persistence.CascadeType;
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
import org.moinex.util.Constants;

/**
 * Represents a transfer between wallets
 */
@Entity
@Table(name = "transfer")
public class Transfer
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name                 = "sender_wallet_id",
                referencedColumnName = "id",
                nullable             = false)
    private Wallet senderWallet;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name                 = "receiver_wallet_id",
                referencedColumnName = "id",
                nullable             = false)
    private Wallet receiverWallet;

    @Column(name = "date", nullable = false)
    private String date;

    @Column(name = "amount", nullable = false, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", nullable = true)
    private String description;

    /**
     * Default constructor for JPA
     */
    public Transfer() { }

    /**
     * Constructor for testing purposes
     * @param id The id of the transfer
     * @param senderWallet The wallet that sends the money
     * @param receiverWallet The wallet that receives the money
     * @param date The date of the transfer
     * @param amount The amount transferred
     * @param description A description of the transfer
     */
    public Transfer(Long          id,
                    Wallet        senderWallet,
                    Wallet        receiverWallet,
                    LocalDateTime date,
                    BigDecimal    amount,
                    String        description)
    {
        this.id             = id;
        this.senderWallet   = senderWallet;
        this.receiverWallet = receiverWallet;
        this.date           = date.format(Constants.DB_DATE_FORMATTER);
        this.amount         = amount;
        this.description    = description;
    }

    /**
     * Constructor for Transfer
     * @param senderWallet The wallet that sends the money
     * @param receiverWallet The wallet that receives the money
     * @param date The date of the transfer
     * @param amount The amount transferred
     * @param description A description of the transfer
     */
    public Transfer(Wallet        senderWallet,
                    Wallet        receiverWallet,
                    LocalDateTime date,
                    BigDecimal    amount,
                    String        description)
    {
        this.senderWallet   = senderWallet;
        this.receiverWallet = receiverWallet;
        this.date           = date.format(Constants.DB_DATE_FORMATTER);
        this.amount         = amount;
        this.description    = description;
    }

    /**
     * Get the transfer id
     * @return The transfer id
     */
    public Long GetId()
    {
        return id;
    }

    /**
     * Get the wallet that sends the money
     * @return The wallet that sends the money
     */
    public Wallet GetSenderWallet()
    {
        return senderWallet;
    }

    /**
     * Get the wallet that receives the money
     * @return The wallet that receives the money
     */
    public Wallet GetReceiverWallet()
    {
        return receiverWallet;
    }

    /**
     * Get the date of the transfer
     * @return The date of the transfer
     */
    public LocalDateTime GetDate()
    {
        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the description of the transfer
     * @return The description of the transfer
     */
    public String GetDescription()
    {
        return description;
    }

    /**
     * Get the amount transferred
     * @return The amount transferred
     */
    public BigDecimal GetAmount()
    {
        return amount;
    }

    /**
     * Set the sender wallet
     * @param senderWallet The sender wallet
     */
    public void SetSenderWallet(Wallet senderWallet)
    {
        this.senderWallet = senderWallet;
    }

    /**
     * Set the receiver wallet
     * @param receiverWallet The receiver wallet
     */
    public void SetReceiverWallet(Wallet receiverWallet)
    {
        this.receiverWallet = receiverWallet;
    }

    /**
     * Set the date of the transfer
     * @param date The date of the transfer
     */
    public void SetDate(LocalDateTime date)
    {
        this.date = date.format(Constants.DB_DATE_FORMATTER);
    }

    /**
     * Set the description of the transfer
     * @param description The description of the transfer
     */
    public void SetDescription(String description)
    {
        this.description = description;
    }

    /**
     * Set the amount transferred
     * @param amount The amount transferred
     */
    public void SetAmount(BigDecimal amount)
    {
        this.amount = amount;
    }
}
