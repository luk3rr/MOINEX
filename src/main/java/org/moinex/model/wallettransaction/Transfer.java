/*
 * Filename: Transfer.java
 * Created on: August 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.wallettransaction;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;

/**
 * Represents a transfer between wallets
 */
@Entity
@Table(name = "transfer")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "sender_wallet_id", referencedColumnName = "id", nullable = false)
    private Wallet senderWallet;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "receiver_wallet_id", referencedColumnName = "id", nullable = false)
    private Wallet receiverWallet;

    @Column(name = "date", nullable = false)
    private String date;

    @Column(name = "amount", nullable = false, scale = 2)
    private BigDecimal amount;

    @Column(name = "description")
    private String description;

    public abstract static class TransferBuilder<
            C extends Transfer, B extends TransferBuilder<C, B>> {
        public B date(LocalDateTime date) {
            this.date = date.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    /**
     * Constructor for testing purposes
     */
    public Transfer(
            Integer id,
            Wallet senderWallet,
            Wallet receiverWallet,
            LocalDateTime date,
            BigDecimal amount,
            String description) {
        this.id = id;
        this.senderWallet = senderWallet;
        this.receiverWallet = receiverWallet;
        this.date = date.format(Constants.DB_DATE_FORMATTER);
        this.amount = amount;
        this.description = description;
    }

    public LocalDateTime getDate() {
        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    public void setDate(LocalDateTime date) {
        this.date = date.format(Constants.DB_DATE_FORMATTER);
    }
}
