/*
 * Filename: NetWorthSnapshotRepository.java
 * Created on: January 22, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository;

import java.util.List;
import java.util.Optional;
import org.moinex.model.NetWorthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface NetWorthSnapshotRepository extends JpaRepository<NetWorthSnapshot, Integer> {

    /**
     * Find snapshot by month and year
     * @param month The month
     * @param year The year
     * @return Optional snapshot
     */
    Optional<NetWorthSnapshot> findByMonthAndYear(Integer month, Integer year);

    /**
     * Get all snapshots ordered by year and month
     * @return List of snapshots
     */
    @Query("SELECT nws FROM NetWorthSnapshot nws ORDER BY nws.year ASC, nws.month ASC")
    List<NetWorthSnapshot> findAllOrderedByDate();

    /**
     * Delete all snapshots
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM NetWorthSnapshot")
    void deleteAll();

    /**
     * Check if snapshot exists for month and year
     * @param month The month
     * @param year The year
     * @return True if exists
     */
    boolean existsByMonthAndYear(Integer month, Integer year);
}
