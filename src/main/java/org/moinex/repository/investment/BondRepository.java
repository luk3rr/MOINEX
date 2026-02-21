/*
 * Filename: BondRepository.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import org.moinex.model.investment.Bond;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BondRepository extends JpaRepository<Bond, Integer> {
    List<Bond> findByArchivedFalseOrderByNameAsc();

    List<Bond> findByArchivedTrueOrderByNameAsc();

    boolean existsBySymbol(String symbol);
}
