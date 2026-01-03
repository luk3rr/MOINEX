/*
 * Filename: GoalAssetAllocationService.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.moinex.dto.GoalAssetAllocationDTO;
import org.moinex.model.goal.Goal;
import org.moinex.model.goal.GoalAssetAllocation;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.Ticker;
import org.moinex.repository.goal.GoalAssetAllocationRepository;
import org.moinex.repository.goal.GoalRepository;
import org.moinex.repository.investment.BondRepository;
import org.moinex.repository.investment.TickerRepository;
import org.moinex.util.enums.AllocationType;
import org.moinex.util.enums.GoalAssetType;
import org.moinex.util.enums.GoalTrackingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class GoalAssetAllocationService {

    private final GoalAssetAllocationRepository allocationRepository;
    private final GoalRepository goalRepository;
    private final BondRepository bondRepository;
    private final TickerRepository tickerRepository;

    @Autowired
    public GoalAssetAllocationService(
            GoalAssetAllocationRepository allocationRepository,
            GoalRepository goalRepository,
            BondRepository bondRepository,
            TickerRepository tickerRepository) {
        this.allocationRepository = allocationRepository;
        this.goalRepository = goalRepository;
        this.bondRepository = bondRepository;
        this.tickerRepository = tickerRepository;
    }

    /**
     * Adds a new asset allocation to a goal
     *
     * @param dto DTO with allocation data
     * @return ID of the created allocation
     * @throws EntityNotFoundException If goal or asset does not exist
     * @throws IllegalArgumentException If validations fail
     */
    @Transactional
    public Integer addAllocation(GoalAssetAllocationDTO dto) {
        log.info("Adding allocation: goal={}, assetType={}, assetId={}", 
                 dto.getGoalId(), dto.getAssetType(), dto.getAssetId());

        validateAllocation(dto);

        Goal goal = goalRepository.findById(dto.getGoalId())
                .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + dto.getGoalId()));

        if (!goal.isAssetBased()) {
            throw new IllegalArgumentException("Goal must be in ASSET_ALLOCATION mode to add allocations");
        }

        validateAssetExists(dto.getAssetType(), dto.getAssetId());
        validateTotalPercentage(dto);
        validateAvailableQuantity(dto);

        GoalAssetAllocation allocation = GoalAssetAllocation.builder()
                .goal(goal)
                .assetType(dto.getAssetType())
                .assetId(dto.getAssetId())
                .allocationType(dto.getAllocationType())
                .allocationValue(dto.getAllocationValue())
                .build();

        allocation = allocationRepository.save(allocation);
        log.info("Allocation created with id: {}", allocation.getId());

        return allocation.getId();
    }

    /**
     * Removes an allocation
     *
     * @param allocationId Allocation ID
     * @throws EntityNotFoundException If allocation does not exist
     */
    @Transactional
    public void removeAllocation(Integer allocationId) {
        log.info("Removing allocation with id: {}", allocationId);

        GoalAssetAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new EntityNotFoundException("Allocation not found with id: " + allocationId));

        allocationRepository.delete(allocation);
        log.info("Allocation removed successfully");
    }

    /**
     * Updates an existing allocation
     *
     * @param allocationId Allocation ID
     * @param dto DTO with new data
     * @throws EntityNotFoundException If allocation does not exist
     * @throws IllegalArgumentException If validations fail
     */
    @Transactional
    public void updateAllocation(Integer allocationId, GoalAssetAllocationDTO dto) {
        log.info("Updating allocation with id: {}", allocationId);

        GoalAssetAllocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new EntityNotFoundException("Allocation not found with id: " + allocationId));

        validateAllocation(dto);
        validateTotalPercentage(dto, allocationId);
        validateAvailableQuantity(dto);

        allocation.setAllocationType(dto.getAllocationType());
        allocation.setAllocationValue(dto.getAllocationValue());

        allocationRepository.save(allocation);
        log.info("Allocation updated successfully");
    }

    /**
     * Retrieves all allocations for a goal
     *
     * @param goal Goal to retrieve allocations for
     * @return List of allocation DTOs
     */
    @Transactional(readOnly = true)
    public List<GoalAssetAllocationDTO> getAllocationsByGoal(Goal goal) {
        List<GoalAssetAllocation> allocations = allocationRepository.findByGoal(goal);

        return allocations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all allocations for a goal by ID
     *
     * @param goalId Goal ID
     * @return List of allocation DTOs
     */
    @Transactional(readOnly = true)
    public List<GoalAssetAllocationDTO> getAllocationsByGoalId(Integer goalId) {
        List<GoalAssetAllocation> allocations = allocationRepository.findByGoalId(goalId);

        return allocations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Calculates the current value of an allocation
     *
     * @param allocation Allocation to calculate value for
     * @return Current value of the allocation
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateAllocationValue(GoalAssetAllocation allocation) {
        if (allocation.getAssetType() == GoalAssetType.BOND) {
            return calculateBondAllocationValue(allocation);
        } else {
            return calculateTickerAllocationValue(allocation);
        }
    }

    /**
     * Calculates the current value of a bond allocation
     *
     * @param allocation Bond allocation
     * @return Current value of the allocation
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateBondAllocationValue(GoalAssetAllocation allocation) {
        Bond bond = bondRepository.findById(allocation.getAssetId())
                .orElseThrow(() -> new EntityNotFoundException("Bond not found with id: " + allocation.getAssetId()));

        BigDecimal totalBondValue = bondRepository.getTotalInvestedValue(bond.getId());
        if (totalBondValue == null) {
            totalBondValue = BigDecimal.ZERO;
        }

        if (bond.getCurrentUnitValue() != null && bond.getCurrentUnitValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalQuantity = bondRepository.getTotalQuantity(bond.getId());
            if (totalQuantity != null && totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                totalBondValue = bond.getCurrentUnitValue().multiply(totalQuantity);
            }
        }

        return calculateValueByAllocationType(
                allocation.getAllocationType(),
                allocation.getAllocationValue(),
                totalBondValue,
                null
        );
    }

    /**
     * Calculates the current value of a ticker allocation
     *
     * @param allocation Ticker allocation
     * @return Current value of the allocation
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTickerAllocationValue(GoalAssetAllocation allocation) {
        Ticker ticker = tickerRepository.findById(allocation.getAssetId())
                .orElseThrow(() -> new EntityNotFoundException("Ticker not found with id: " + allocation.getAssetId()));

        BigDecimal totalTickerValue = ticker.getCurrentUnitValue()
                .multiply(ticker.getCurrentQuantity())
                .setScale(2, RoundingMode.HALF_UP);

        return calculateValueByAllocationType(
                allocation.getAllocationType(),
                allocation.getAllocationValue(),
                totalTickerValue,
                ticker.getCurrentQuantity()
        );
    }

    /**
     * Calcula valor baseado no tipo de alocação
     *
     * @param allocationType Tipo de alocação
     * @param allocationValue Valor da alocação
     * @param totalAssetValue Valor total do ativo
     * @param totalQuantity Quantidade total (para tickers)
     * @return Valor calculado
     */
    private BigDecimal calculateValueByAllocationType(
            AllocationType allocationType,
            BigDecimal allocationValue,
            BigDecimal totalAssetValue,
            BigDecimal totalQuantity) {

        switch (allocationType) {
            case PERCENTAGE:
                return totalAssetValue
                        .multiply(allocationValue)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            case QUANTITY:
                if (totalQuantity == null || totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
                    return BigDecimal.ZERO;
                }
                BigDecimal unitValue = totalAssetValue.divide(totalQuantity, 2, RoundingMode.HALF_UP);
                return unitValue.multiply(allocationValue).setScale(2, RoundingMode.HALF_UP);

            case VALUE:
                return allocationValue;

            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Calculates the total current value of an asset-based goal
     *
     * @param goal Goal to calculate value for
     * @return Total current value of the goal
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateGoalTotalValue(Goal goal) {
        if (!goal.isAssetBased()) {
            return goal.getBalance();
        }

        List<GoalAssetAllocation> allocations = allocationRepository.findByGoal(goal);

        return allocations.stream()
                .map(this::calculateAllocationValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validates basic allocation data
     *
     * @param dto DTO to validate
     * @throws IllegalArgumentException If validation fails
     */
    private void validateAllocation(GoalAssetAllocationDTO dto) {
        if (dto.getGoalId() == null) {
            throw new IllegalArgumentException("Goal ID is required");
        }

        if (dto.getAssetType() == null) {
            throw new IllegalArgumentException("Asset type is required");
        }

        if (dto.getAssetId() == null) {
            throw new IllegalArgumentException("Asset ID is required");
        }

        if (dto.getAllocationType() == null) {
            throw new IllegalArgumentException("Allocation type is required");
        }

        if (dto.getAllocationValue() == null || dto.getAllocationValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Allocation value must be greater than zero");
        }

        if (dto.getAllocationType() == AllocationType.PERCENTAGE) {
            if (dto.getAllocationValue().compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Percentage allocation cannot exceed 100%");
            }
        }
    }

    /**
     * Validates if the asset exists
     *
     * @param assetType Asset type
     * @param assetId Asset ID
     * @throws EntityNotFoundException If asset does not exist
     */
    private void validateAssetExists(GoalAssetType assetType, Integer assetId) {
        if (assetType == GoalAssetType.BOND) {
            if (!bondRepository.existsById(assetId)) {
                throw new EntityNotFoundException("Bond not found with id: " + assetId);
            }
        } else {
            if (!tickerRepository.existsById(assetId)) {
                throw new EntityNotFoundException("Ticker not found with id: " + assetId);
            }
        }
    }

    /**
     * Validates if the sum of percentages does not exceed 100%
     *
     * @param dto DTO with new allocation
     * @throws IllegalArgumentException If sum exceeds 100%
     */
    private void validateTotalPercentage(GoalAssetAllocationDTO dto) {
        validateTotalPercentage(dto, null);
    }

    /**
     * Validates if the sum of percentages does not exceed 100%
     *
     * @param dto DTO with new allocation
     * @param excludeAllocationId Allocation ID to exclude from calculation (for updates)
     * @throws IllegalArgumentException If sum exceeds 100%
     */
    private void validateTotalPercentage(GoalAssetAllocationDTO dto, Integer excludeAllocationId) {
        if (dto.getAllocationType() != AllocationType.PERCENTAGE) {
            return;
        }

        List<GoalAssetAllocation> existingAllocations = allocationRepository.findByGoalAndAsset(
                dto.getGoalId(),
                dto.getAssetType(),
                dto.getAssetId()
        ).stream().collect(Collectors.toList());

        BigDecimal totalPercentage = existingAllocations.stream()
                .filter(a -> !a.getId().equals(excludeAllocationId))
                .filter(a -> a.getAllocationType() == AllocationType.PERCENTAGE)
                .map(GoalAssetAllocation::getAllocationValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        totalPercentage = totalPercentage.add(dto.getAllocationValue());

        if (totalPercentage.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException(
                    "Total percentage allocation for this asset would exceed 100%. Current: " 
                    + totalPercentage.subtract(dto.getAllocationValue()) + "%");
        }
    }

    /**
     * Validates if there is available quantity of the asset
     *
     * @param dto DTO with new allocation
     * @throws IllegalArgumentException If quantity is not available
     */
    private void validateAvailableQuantity(GoalAssetAllocationDTO dto) {
        if (dto.getAllocationType() != AllocationType.QUANTITY) {
            return;
        }

        if (dto.getAssetType() == GoalAssetType.BOND) {
            BigDecimal totalQuantity = bondRepository.getTotalQuantity(dto.getAssetId());
            if (totalQuantity == null) {
                totalQuantity = BigDecimal.ZERO;
            }

            if (dto.getAllocationValue().compareTo(totalQuantity) > 0) {
                throw new IllegalArgumentException(
                        "Allocation quantity (" + dto.getAllocationValue() 
                        + ") exceeds available bond quantity (" + totalQuantity + ")");
            }
        } else {
            Ticker ticker = tickerRepository.findById(dto.getAssetId())
                    .orElseThrow(() -> new EntityNotFoundException("Ticker not found"));

            if (dto.getAllocationValue().compareTo(ticker.getCurrentQuantity()) > 0) {
                throw new IllegalArgumentException(
                        "Allocation quantity (" + dto.getAllocationValue() 
                        + ") exceeds available ticker quantity (" + ticker.getCurrentQuantity() + ")");
            }
        }
    }

    /**
     * Converts entity to DTO
     *
     * @param allocation Entity to convert
     * @return Converted DTO
     */
    private GoalAssetAllocationDTO convertToDTO(GoalAssetAllocation allocation) {
        String assetName = getAssetName(allocation.getAssetType(), allocation.getAssetId());
        BigDecimal currentValue = calculateAllocationValue(allocation);

        return GoalAssetAllocationDTO.builder()
                .id(allocation.getId())
                .goalId(allocation.getGoal().getId())
                .assetType(allocation.getAssetType())
                .assetId(allocation.getAssetId())
                .assetName(assetName)
                .allocationType(allocation.getAllocationType())
                .allocationValue(allocation.getAllocationValue())
                .currentAssetValue(currentValue)
                .build();
    }

    /**
     * Busca o nome do ativo
     *
     * @param assetType Tipo do ativo
     * @param assetId ID do ativo
     * @return Nome do ativo
     */
    private String getAssetName(GoalAssetType assetType, Integer assetId) {
        if (assetType == GoalAssetType.BOND) {
            return bondRepository.findById(assetId)
                    .map(Bond::getName)
                    .orElse("Unknown Bond");
        } else {
            return tickerRepository.findById(assetId)
                    .map(Ticker::getName)
                    .orElse("Unknown Ticker");
        }
    }
}
