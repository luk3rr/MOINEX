/*
 * Filename: WalletTransaction.java
 * Created on: August 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.wallettransaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;
import org.moinex.util.enums.TransactionStatus;

/**
 * Represents a transaction in a wallet
 */
@Entity
@Table(name = "wallet_transaction")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class WalletTransaction extends BaseTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @Column(name = "date", nullable = false)
    private String date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    public abstract static class WalletTransactionBuilder<
                    C extends WalletTransaction, B extends WalletTransactionBuilder<C, B>>
            extends BaseTransactionBuilder<C, B> {
        public B date(LocalDateTime date) {
            this.date = date.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    public WalletTransactionBuilder<?, ?> toBuilder() {
        return this.toBuilderImpl();
    }

    protected WalletTransactionBuilder<?, ?> toBuilderImpl() {
        return WalletTransaction.builder()
                .id(this.getId())
                .date(this.getDate())
                .status(this.getStatus())
                .amount(this.getAmount())
                .type(this.getType())
                .wallet(this.getWallet())
                .category(this.getCategory())
                .description(this.getDescription());
    }

    public LocalDateTime getDate() {
        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    public void setDate(LocalDateTime date) {
        this.date = date.format(Constants.DB_DATE_FORMATTER);
    }
}
