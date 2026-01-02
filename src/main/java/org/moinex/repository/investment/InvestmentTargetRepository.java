/*
 * Filename: InvestmentTargetRepository.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import java.util.Optional;
import org.moinex.model.investment.InvestmentTarget;
import org.moinex.util.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestmentTargetRepository extends JpaRepository<InvestmentTarget, Integer> {
    List<InvestmentTarget> findAllByIsActiveTrueOrderByAssetTypeAsc();

    Optional<InvestmentTarget> findByAssetTypeAndIsActiveTrue(AssetType assetType);

    boolean existsByAssetType(AssetType assetType);
}
