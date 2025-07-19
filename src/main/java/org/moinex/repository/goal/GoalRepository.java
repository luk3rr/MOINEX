/*
 * Filename: GoalRepository.java
 * Created on: December  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.goal;

import java.math.BigDecimal;
import org.moinex.model.goal.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Integer> {

    /**
     * Check if a goal with the given name exists
     *
     * @param name The name of the goal
     * @return True if a goal with the given name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Find the sum of balances of all goals associated with a specific master wallet
     *
     * @param masterWalletId The ID of the master wallet
     * @return The sum of balances of all goals associated with the master wallet
     */
    @Query(
            "SELECT COALESCE(SUM(g.balance), 0) "
                    + "FROM Goal g "
                    + "WHERE g.masterWallet.id = :masterWalletId")
    BigDecimal getSumOfBalancesByMasterWallet(@Param("masterWalletId") Integer masterWalletId);
}
