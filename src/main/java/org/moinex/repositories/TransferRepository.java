/*
 * Filename: TransferRepository.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repositories;

import java.util.List;
import org.moinex.entities.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    /**
     * TODO: Implement tests
     * Get the transfers by wallet
     * @param walletId The id of the wallet
     * @return A list with the transfers in the wallet
     */
    @Query("SELECT t "
           + "FROM Transfer t "
           + "WHERE t.senderWallet.id = :walletId "
           + "OR t.receiverWallet.id = :walletId "
           + "ORDER BY t.date DESC")
    List<Transfer>
    FindTransfersByWallet(@Param("walletId") Long walletId);

    /**
     * TODO: Implement tests
     * Get the transfers by month and year
     * @param month The month
     * @param year The year
     * @return A list with the transfers by month and year
     */
    @Query("SELECT t "
           + "FROM Transfer t "
           + "WHERE strftime('%m', t.date) = printf('%02d', :month) "
           + "AND strftime('%Y', t.date) = printf('%04d', :year) "
           + "ORDER BY t.date DESC")
    List<Transfer>
    FindTransferByMonthAndYear(@Param("month") Integer month,
                               @Param("year") Integer  year);

    /**
     * Get the transfers by wallet and month
     * @param walletId The id of the wallet
     * @param month The month
     * @param year The year
     * @return A list with the transfers in the wallet by month
     */
    @Query("SELECT t "
           + "FROM Transfer t "
           + "WHERE (t.senderWallet.id = :walletId "
           + "       OR t.receiverWallet.id = :walletId) "
           + "AND strftime('%m', t.date) = printf('%02d', :month) "
           + "AND strftime('%Y', t.date) = printf('%04d', :year) "
           + "ORDER BY t.date DESC")
    List<Transfer>
    FindTransfersByWalletAndMonth(@Param("walletId") Long walletId,
                                  @Param("month") Integer month,
                                  @Param("year") Integer  year);

    /**
     * Get the count of transfers by wallet
     * @param walletId The id of the wallet
     * @return The count of transfers in the wallet
     */
    @Query("SELECT COUNT(t) "
           + "FROM Transfer t "
           + "WHERE t.senderWallet.id = :walletId "
           + "OR t.receiverWallet.id = :walletId")
    Long
    GetTransferCountByWallet(@Param("walletId") Long walletId);

    /**
     * Get suggestions. Suggestions are transfers with distinct descriptions
     * and most recent date
     * @return A list with the suggestions
     */
    @Query("SELECT t "
           + "FROM Transfer t "
           + "WHERE t.senderWallet.archived = false AND "
           + "t.receiverWallet.archived = false AND "
           + "t.date = (SELECT MAX(t2.date) "
           + "                 FROM Transfer t2 "
           + "                 WHERE t2.senderWallet.archived = false AND "
           + "                       t2.receiverWallet.archived = false AND "
           + "                       t2.description = t.description) "
           + "ORDER BY t.date DESC")
    List<Transfer>
    FindSuggestions();
}
