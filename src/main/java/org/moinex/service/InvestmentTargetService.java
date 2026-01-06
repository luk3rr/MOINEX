/*
 * Filename: InvestmentTargetService.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import lombok.NoArgsConstructor;
import org.moinex.model.enums.AssetType;
import org.moinex.model.enums.TickerType;
import org.moinex.model.investment.InvestmentTarget;
import org.moinex.repository.investment.InvestmentTargetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@NoArgsConstructor
public class InvestmentTargetService {

    @Autowired private InvestmentTargetRepository investmentTargetRepository;

    public List<InvestmentTarget> getAllActiveTargets() {
        return investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc();
    }

    public InvestmentTarget getTargetByType(AssetType assetType) {
        return investmentTargetRepository
                .findByAssetTypeAndIsActiveTrue(assetType)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "Investment target not found for type: " + assetType));
    }

    public InvestmentTarget getTargetByTickerType(TickerType tickerType) {
        AssetType assetType = AssetType.valueOf(tickerType.name());
        return getTargetByType(assetType);
    }

    @Transactional
    public InvestmentTarget setTarget(AssetType assetType, BigDecimal targetPercentage) {
        if (targetPercentage.compareTo(BigDecimal.ZERO) < 0
                || targetPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Target percentage must be between 0 and 100");
        }

        InvestmentTarget target =
                investmentTargetRepository
                        .findByAssetType(assetType)
                        .orElse(
                                InvestmentTarget.builder()
                                        .assetType(assetType)
                                        .isActive(true)
                                        .build());

        target.setTargetPercentage(targetPercentage);
        target.setActive(true);
        return investmentTargetRepository.save(target);
    }

    @Transactional
    public void deleteTarget(Integer id) {
        InvestmentTarget target =
                investmentTargetRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Investment target not found"));

        target.setActive(false);
        investmentTargetRepository.save(target);
    }

    public boolean validateTotalPercentage() {
        BigDecimal total =
                getAllActiveTargets().stream()
                        .map(InvestmentTarget::getTargetPercentage)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.compareTo(new BigDecimal("100")) == 0;
    }
}
