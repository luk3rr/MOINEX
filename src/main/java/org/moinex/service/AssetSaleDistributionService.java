/*
 * Filename: AssetSaleDistributionService.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.moinex.dto.AffectedGoalInfo;
import org.moinex.dto.AssetSaleDistribution;
import org.moinex.model.goal.Goal;
import org.moinex.model.goal.GoalAssetAllocation;
import org.moinex.repository.goal.GoalAssetAllocationRepository;
import org.moinex.repository.goal.GoalRepository;
import org.moinex.util.enums.AllocationType;
import org.moinex.util.enums.GoalAssetType;
import org.moinex.util.enums.SaleDistributionStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AssetSaleDistributionService {

    private final GoalAssetAllocationRepository allocationRepository;
    private final GoalRepository goalRepository;
    private final GoalAssetAllocationService allocationService;

    @Autowired
    public AssetSaleDistributionService(
            GoalAssetAllocationRepository allocationRepository,
            GoalRepository goalRepository,
            GoalAssetAllocationService allocationService) {
        this.allocationRepository = allocationRepository;
        this.goalRepository = goalRepository;
        this.allocationService = allocationService;
    }

    /**
     * Retrieves goals affected by an asset sale
     *
     * @param assetType Asset type
     * @param assetId Asset ID
     * @param saleAmount Value or quantity being sold
     * @return List of information about affected goals
     */
    @Transactional(readOnly = true)
    public List<AffectedGoalInfo> getAffectedGoals(
            GoalAssetType assetType,
            Integer assetId,
            BigDecimal saleAmount) {
        
        log.info("Getting affected goals for asset: type={}, id={}, saleAmount={}", 
                 assetType, assetId, saleAmount);

        List<GoalAssetAllocation> allocations = 
            allocationRepository.findByAssetTypeAndAssetId(assetType, assetId);

        if (allocations.isEmpty()) {
            log.info("No goals affected by this sale");
            return new ArrayList<>();
        }

        List<AffectedGoalInfo> affectedGoals = new ArrayList<>();
        BigDecimal totalAllocatedValue = BigDecimal.ZERO;

        for (GoalAssetAllocation allocation : allocations) {
            BigDecimal currentValue = allocationService.calculateAllocationValue(allocation);
            totalAllocatedValue = totalAllocatedValue.add(currentValue);
        }

        for (GoalAssetAllocation allocation : allocations) {
            BigDecimal currentValue = allocationService.calculateAllocationValue(allocation);
            BigDecimal proportion = BigDecimal.ZERO;
            
            if (totalAllocatedValue.compareTo(BigDecimal.ZERO) > 0) {
                proportion = currentValue
                    .divide(totalAllocatedValue, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            }

            BigDecimal suggestedReduction = calculateSuggestedReduction(
                allocation, saleAmount, currentValue, totalAllocatedValue);

            AffectedGoalInfo info = AffectedGoalInfo.builder()
                    .goalId(allocation.getGoal().getId())
                    .goalName(allocation.getGoal().getName())
                    .currentAllocation(currentValue)
                    .proportion(proportion)
                    .suggestedReduction(suggestedReduction)
                    .build();

            affectedGoals.add(info);
        }

        log.info("Found {} affected goals", affectedGoals.size());
        return affectedGoals;
    }

    /**
     * Processes an asset sale applying the chosen distribution strategy
     *
     * @param assetType Asset type
     * @param assetId Asset ID
     * @param saleAmount Value or quantity sold
     * @param distribution Chosen distribution strategy
     * @throws IllegalArgumentException If distribution is invalid
     */
    @Transactional
    public void processAssetSale(
            GoalAssetType assetType,
            Integer assetId,
            BigDecimal saleAmount,
            AssetSaleDistribution distribution) {
        
        log.info("Processing asset sale: type={}, id={}, amount={}, strategy={}", 
                 assetType, assetId, saleAmount, distribution.getStrategy());

        List<GoalAssetAllocation> allocations = 
            allocationRepository.findByAssetTypeAndAssetId(assetType, assetId);

        if (allocations.isEmpty()) {
            log.info("No allocations to adjust");
            return;
        }

        switch (distribution.getStrategy()) {
            case PROPORTIONAL:
                applyProportionalDistribution(allocations, saleAmount);
                break;

            case SINGLE_GOAL:
                applySingleGoalDistribution(allocations, saleAmount, distribution.getSingleGoalId());
                break;

            case MANUAL:
                applyManualDistribution(allocations, distribution.getManualDistribution());
                break;

            case KEEP_ALLOCATIONS:
                log.info("Keeping allocations unchanged");
                break;

            default:
                throw new IllegalArgumentException("Unknown distribution strategy: " + distribution.getStrategy());
        }

        log.info("Asset sale processed successfully");
    }

    /**
     * Applies proportional distribution of the sale among all goals
     *
     * @param allocations List of affected allocations
     * @param saleAmount Value or quantity sold
     */
    @Transactional
    public void applyProportionalDistribution(
            List<GoalAssetAllocation> allocations,
            BigDecimal saleAmount) {
        
        log.info("Applying proportional distribution for {} allocations", allocations.size());

        BigDecimal totalAllocatedValue = BigDecimal.ZERO;
        for (GoalAssetAllocation allocation : allocations) {
            BigDecimal value = allocationService.calculateAllocationValue(allocation);
            totalAllocatedValue = totalAllocatedValue.add(value);
        }

        if (totalAllocatedValue.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Total allocated value is zero, cannot distribute proportionally");
            return;
        }

        for (GoalAssetAllocation allocation : allocations) {
            BigDecimal currentValue = allocationService.calculateAllocationValue(allocation);
            BigDecimal proportion = currentValue.divide(totalAllocatedValue, 6, RoundingMode.HALF_UP);
            BigDecimal reduction = saleAmount.multiply(proportion);

            adjustAllocation(allocation, reduction);
        }

        log.info("Proportional distribution applied successfully");
    }

    /**
     * Applies distribution to a single goal
     *
     * @param allocations List of affected allocations
     * @param saleAmount Value or quantity sold
     * @param goalId ID of the goal that should be affected
     */
    @Transactional
    public void applySingleGoalDistribution(
            List<GoalAssetAllocation> allocations,
            BigDecimal saleAmount,
            Integer goalId) {
        
        log.info("Applying single goal distribution for goal: {}", goalId);

        if (goalId == null) {
            throw new IllegalArgumentException("Goal ID is required for single goal distribution");
        }

        GoalAssetAllocation targetAllocation = allocations.stream()
                .filter(a -> a.getGoal().getId().equals(goalId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                    "Goal " + goalId + " is not linked to this asset"));

        adjustAllocation(targetAllocation, saleAmount);
        log.info("Single goal distribution applied successfully");
    }

    /**
     * Applies manual distribution defined by the user
     *
     * @param allocations List of affected allocations
     * @param manualDistribution Map with goalId -> reduction value
     */
    @Transactional
    public void applyManualDistribution(
            List<GoalAssetAllocation> allocations,
            Map<Integer, BigDecimal> manualDistribution) {
        
        log.info("Applying manual distribution for {} goals", 
                 manualDistribution != null ? manualDistribution.size() : 0);

        if (manualDistribution == null || manualDistribution.isEmpty()) {
            throw new IllegalArgumentException("Manual distribution map is required");
        }

        for (GoalAssetAllocation allocation : allocations) {
            Integer goalId = allocation.getGoal().getId();
            BigDecimal reduction = manualDistribution.get(goalId);

            if (reduction != null && reduction.compareTo(BigDecimal.ZERO) > 0) {
                adjustAllocation(allocation, reduction);
            }
        }

        log.info("Manual distribution applied successfully");
    }

    /**
     * Adjusts an allocation by reducing its value
     *
     * @param allocation Allocation to be adjusted
     * @param reductionAmount Reduction amount
     */
    private void adjustAllocation(GoalAssetAllocation allocation, BigDecimal reductionAmount) {
        log.debug("Adjusting allocation {}: type={}, current={}, reduction={}", 
                  allocation.getId(), 
                  allocation.getAllocationType(),
                  allocation.getAllocationValue(),
                  reductionAmount);

        AllocationType type = allocation.getAllocationType();
        BigDecimal currentValue = allocation.getAllocationValue();

        switch (type) {
            case PERCENTAGE:
                BigDecimal currentAssetValue = allocationService.calculateAllocationValue(allocation);
                if (currentAssetValue.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal reductionPercentage = reductionAmount
                        .divide(currentAssetValue, 6, RoundingMode.HALF_UP)
                        .multiply(currentValue);
                    
                    BigDecimal newPercentage = currentValue.subtract(reductionPercentage);
                    if (newPercentage.compareTo(BigDecimal.ZERO) <= 0) {
                        allocationRepository.delete(allocation);
                        log.info("Allocation {} removed (percentage reached zero)", allocation.getId());
                    } else {
                        allocation.setAllocationValue(newPercentage.setScale(2, RoundingMode.HALF_UP));
                        allocationRepository.save(allocation);
                        log.info("Allocation {} updated: {}% -> {}%", 
                                 allocation.getId(), currentValue, newPercentage);
                    }
                }
                break;

            case QUANTITY:
                BigDecimal totalAssetValue = allocationService.calculateAllocationValue(allocation);
                BigDecimal unitValue = totalAssetValue.divide(currentValue, 6, RoundingMode.HALF_UP);
                BigDecimal quantityReduction = reductionAmount.divide(unitValue, 6, RoundingMode.HALF_UP);
                
                BigDecimal newQuantity = currentValue.subtract(quantityReduction);
                if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    allocationRepository.delete(allocation);
                    log.info("Allocation {} removed (quantity reached zero)", allocation.getId());
                } else {
                    allocation.setAllocationValue(newQuantity.setScale(8, RoundingMode.HALF_UP));
                    allocationRepository.save(allocation);
                    log.info("Allocation {} updated: {} -> {} units", 
                             allocation.getId(), currentValue, newQuantity);
                }
                break;

            case VALUE:
                BigDecimal newValue = currentValue.subtract(reductionAmount);
                if (newValue.compareTo(BigDecimal.ZERO) <= 0) {
                    allocationRepository.delete(allocation);
                    log.info("Allocation {} removed (value reached zero)", allocation.getId());
                } else {
                    allocation.setAllocationValue(newValue.setScale(2, RoundingMode.HALF_UP));
                    allocationRepository.save(allocation);
                    log.info("Allocation {} updated: {} -> {}", 
                             allocation.getId(), currentValue, newValue);
                }
                break;
        }
    }

    /**
     * Calculates the suggested reduction for an allocation based on the sale
     *
     * @param allocation Allocation
     * @param saleAmount Sale value
     * @param currentValue Current allocation value
     * @param totalAllocatedValue Total allocated value
     * @return Suggested reduction value
     */
    private BigDecimal calculateSuggestedReduction(
            GoalAssetAllocation allocation,
            BigDecimal saleAmount,
            BigDecimal currentValue,
            BigDecimal totalAllocatedValue) {

        if (totalAllocatedValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal proportion = currentValue.divide(totalAllocatedValue, 6, RoundingMode.HALF_UP);
        return saleAmount.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Checks if there are goals linked to an asset
     *
     * @param assetType Asset type
     * @param assetId Asset ID
     * @return true if there are linked goals
     */
    @Transactional(readOnly = true)
    public boolean hasLinkedGoals(GoalAssetType assetType, Integer assetId) {
        Long count = allocationRepository.countGoalsByAsset(assetType, assetId);
        return count != null && count > 0;
    }

    /**
     * Counts how many goals are linked to an asset
     *
     * @param assetType Asset type
     * @param assetId Asset ID
     * @return Number of linked goals
     */
    @Transactional(readOnly = true)
    public Long countLinkedGoals(GoalAssetType assetType, Integer assetId) {
        return allocationRepository.countGoalsByAsset(assetType, assetId);
    }
}
