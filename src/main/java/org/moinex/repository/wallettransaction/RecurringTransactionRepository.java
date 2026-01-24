/*
 * Filename: RecurringTransactionRepository.java
 * Created on: November 10, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.wallettransaction;

import java.util.List;
import org.moinex.model.enums.RecurringTransactionStatus;
import org.moinex.model.enums.TransactionType;
import org.moinex.model.wallettransaction.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the RecurringTransaction entity
 * This repository provides methods to query the database for RecurringTransaction
 */
@Repository
public interface RecurringTransactionRepository
        extends JpaRepository<RecurringTransaction, Integer> {

    /**
     * Get the recurring transaction by its status
     * @param status The status of the recurring transaction
     * @return A list of recurring transactions with the given status
     */
    List<RecurringTransaction> findByStatus(RecurringTransactionStatus status);

    /**
     * Get the recurring transaction by its type
     * @param type The type of the recurring transaction
     * @return A list of recurring transactions with the given type
     */
    List<RecurringTransaction> findByType(TransactionType type);
}
