/*
 * Filename: WalletRepository.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.wallettransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.moinex.model.wallettransaction.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Integer> {
    /**
     * Check if a wallet with the given name exists
     * @param name The name of the wallet
     * @return True if a wallet with the given name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Get a wallet by its name
     * @param name The name of the wallet
     * @return The wallet with the given name
     */
    Optional<Wallet> findByName(String name);

    /**
     * Get all wallets ordered by name
     * @return A list with all wallets ordered by name
     */
    List<Wallet> findAllByOrderByNameAsc();

    /**
     * Get all wallets that are archived
     * @return A list with all wallets that are archived
     */
    List<Wallet> findAllByIsArchivedTrue();

    /**
     * Get all wallets that are not archived
     * @return A list with all wallets that are not archived
     */
    List<Wallet> findAllByIsArchivedFalse();

    /**
     * Get all wallets that are not archived ordered by name
     * @return A list with all wallets that are not archived
     */
    List<Wallet> findAllByIsArchivedFalseOrderByNameAsc();

    /**
     * Find the sum of balances of all goals associated with a specific master wallet
     *
     * @param masterWalletId The ID of the master wallet
     * @return The sum of balances of all goals associated with the master wallet
     */
    @Query(
            "SELECT COALESCE(SUM(w.balance), 0) "
                    + "FROM Wallet w "
                    + "WHERE w.masterWallet.id = :masterWalletId")
    BigDecimal getSumOfBalancesByMasterWallet(@Param("masterWalletId") Integer masterWalletId);
}
