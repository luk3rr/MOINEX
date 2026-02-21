/*
 * Filename: BondInterestCalculationService.java
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.moinex.model.enums.InterestIndex;
import org.moinex.model.enums.OperationType;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BondInterestCalculation;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.investment.MarketIndicatorHistory;
import org.moinex.repository.investment.BondInterestCalculationRepository;
import org.moinex.repository.investment.BondOperationRepository;
import org.moinex.repository.investment.BondRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BondInterestCalculationService {

    private final BondRepository bondRepository;
    private final BondOperationRepository bondOperationRepository;
    private final BondInterestCalculationRepository bondInterestCalculationRepository;
    private final MarketIndicatorService marketIndicatorService;

    private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("252");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    public BondInterestCalculationService(
            BondRepository bondRepository,
            BondOperationRepository bondOperationRepository,
            BondInterestCalculationRepository bondInterestCalculationRepository,
            MarketIndicatorService marketIndicatorService) {
        this.bondRepository = bondRepository;
        this.bondOperationRepository = bondOperationRepository;
        this.bondInterestCalculationRepository = bondInterestCalculationRepository;
        this.marketIndicatorService = marketIndicatorService;
    }

    /**
     * Calculate accumulated interest for a bond from purchase date to calculation date
     *
     * @param bondId The bond ID
     * @param calculationDate The date to calculate interest until
     * @return BondInterestCalculation with accumulated interest
     */
    @Transactional
    public BondInterestCalculation calculateBondInterest(
            Integer bondId, LocalDate calculationDate) {
        Bond bond =
                bondRepository
                        .findById(bondId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Bond not found with id: " + bondId));

        List<BondOperation> operations =
                bondOperationRepository.findByBondOrderByOperationDateAsc(bond);

        if (operations.isEmpty()) {
            throw new IllegalArgumentException("Bond has no operations to calculate interest");
        }

        // Get first buy operation
        Optional<BondOperation> firstBuy =
                operations.stream()
                        .filter(op -> op.getOperationType() == OperationType.BUY)
                        .findFirst();

        if (firstBuy.isEmpty()) {
            throw new IllegalArgumentException("Bond has no buy operations");
        }

        LocalDate startDate = firstBuy.get().getWalletTransaction().getDate().toLocalDate();

        log.info(
                "Calculating interest for bond {} from {} to {}",
                bond.getName(),
                startDate,
                calculationDate);

        BigDecimal accumulatedInterest = BigDecimal.ZERO;
        BigDecimal investedAmount = BigDecimal.ZERO;

        // Process operations chronologically
        BigDecimal currentQuantity = BigDecimal.ZERO;
        LocalDate lastCalculationDate = startDate;
        BigDecimal lastOperationSpread = null; // Will be set when first BUY operation is found

        for (BondOperation operation : operations) {
            LocalDate operationDate = operation.getWalletTransaction().getDate().toLocalDate();

            if (operationDate.isAfter(calculationDate)) {
                break;
            }

            // Calculate interest for the period before this operation
            if (currentQuantity.compareTo(BigDecimal.ZERO) > 0
                    && operationDate.isAfter(lastCalculationDate)) {
                // Use stored spread from operation, fallback to current bond spread if not
                // available
                BigDecimal spreadToUse =
                        lastOperationSpread != null ? lastOperationSpread : bond.getInterestRate();
                BigDecimal periodInterest =
                        calculatePeriodInterest(
                                bond,
                                lastCalculationDate,
                                operationDate,
                                currentQuantity,
                                operation.getUnitPrice(),
                                spreadToUse);
                accumulatedInterest = accumulatedInterest.add(periodInterest);
            }

            // Update quantity based on operation type
            if (operation.getOperationType() == OperationType.BUY) {
                currentQuantity = currentQuantity.add(operation.getQuantity());
                investedAmount =
                        investedAmount.add(
                                operation.getQuantity().multiply(operation.getUnitPrice()));
                // Store the spread from this operation for future periods
                if (operation.getSpread() != null) {
                    lastOperationSpread = operation.getSpread();
                }
            } else {
                currentQuantity = currentQuantity.subtract(operation.getQuantity());
            }

            lastCalculationDate = operationDate;
        }

        // Calculate interest for the remaining period until calculation date
        if (currentQuantity.compareTo(BigDecimal.ZERO) > 0
                && lastCalculationDate.isBefore(calculationDate)) {
            BigDecimal lastOperationPrice =
                    operations.stream()
                            .filter(op -> op.getOperationType() == OperationType.BUY)
                            .reduce((first, second) -> second)
                            .map(BondOperation::getUnitPrice)
                            .orElse(BigDecimal.ZERO);

            // Use stored spread from operation, fallback to current bond spread if not available
            BigDecimal spreadToUse =
                    lastOperationSpread != null ? lastOperationSpread : bond.getInterestRate();
            BigDecimal periodInterest =
                    calculatePeriodInterest(
                            bond,
                            lastCalculationDate,
                            calculationDate,
                            currentQuantity,
                            lastOperationPrice,
                            spreadToUse);
            accumulatedInterest = accumulatedInterest.add(periodInterest);
        }

        BigDecimal finalValue = investedAmount.add(accumulatedInterest);

        BondInterestCalculation calculation =
                BondInterestCalculation.builder()
                        .bond(bond)
                        .calculationDate(calculationDate)
                        .startDate(startDate)
                        .endDate(calculationDate)
                        .quantity(currentQuantity)
                        .investedAmount(investedAmount)
                        .accumulatedInterest(accumulatedInterest)
                        .finalValue(finalValue)
                        .calculationMethod(getCalculationMethod(bond))
                        .createdAt(LocalDateTime.now())
                        .build();

        return bondInterestCalculationRepository.save(calculation);
    }

    /**
     * Calculate interest for a specific period
     *
     * @param bond The bond
     * @param startDate Period start date
     * @param endDate Period end date
     * @param quantity Quantity held during the period
     * @param unitPrice Unit price of the bond
     * @param spread The spread/interest rate that was in effect during this period
     * @return Interest accumulated during the period
     */
    private BigDecimal calculatePeriodInterest(
            Bond bond,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal spread) {

        return switch (bond.getInterestType()) {
            case FIXED -> calculateFixedInterest(bond, startDate, endDate, quantity, unitPrice);
            case FLOATING ->
                    calculateFloatingInterest(
                            bond,
                            startDate,
                            endDate,
                            quantity,
                            unitPrice,
                            bond.getInterestIndex(),
                            spread);
            case ZERO_COUPON -> BigDecimal.ZERO;
        };
    }

    /**
     * Calculate fixed interest (prefixado) using compound interest
     * Formula: Valor_Investido × [(1 + Taxa_Anual/100)^(Dias_Úteis/252) - 1]
     *
     * @param bond The bond
     * @param startDate Period start date
     * @param endDate Period end date
     * @param quantity Quantity held
     * @param unitPrice Unit price
     * @return Interest amount
     */
    private BigDecimal calculateFixedInterest(
            Bond bond,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal quantity,
            BigDecimal unitPrice) {

        BigDecimal investedAmount = quantity.multiply(unitPrice);

        // Count business days using the bond's interest index
        // This ensures we use the correct ANBIMA calendar for this specific bond
        InterestIndex indexForDayCount =
                Objects.requireNonNull(bond.getInterestIndex(), "Bond must have an interest index");

        List<MarketIndicatorHistory> indexData =
                marketIndicatorService.getIndicatorHistoryBetween(
                        indexForDayCount, startDate, endDate);

        int businessDays = indexData.size();

        if (businessDays == 0) {
            log.warn(
                    "No business days found between {} and {} for fixed interest calculation using"
                            + " {}",
                    startDate,
                    endDate,
                    indexForDayCount);
            return BigDecimal.ZERO;
        }

        // Convert annual rate to factor: (1 + rate/100)
        BigDecimal annualFactor =
                BigDecimal.ONE.add(bond.getInterestRate().divide(HUNDRED, 8, RoundingMode.HALF_UP));

        // Calculate exponent: business_days / 252
        double exponent = businessDays / DAYS_PER_YEAR.doubleValue();

        // Calculate accumulated factor: (1 + rate/100)^(business_days/252)
        double accumulatedFactorDouble = Math.pow(annualFactor.doubleValue(), exponent);
        BigDecimal accumulatedFactor = BigDecimal.valueOf(accumulatedFactorDouble);

        // Calculate interest: invested × (factor - 1)
        BigDecimal interest = investedAmount.multiply(accumulatedFactor.subtract(BigDecimal.ONE));

        log.debug(
                "Fixed interest: bond={}, businessDays={}, indexForDayCount={}, rate={},"
                        + " invested={}, factor={}, interest={}",
                bond.getName(),
                businessDays,
                indexForDayCount,
                bond.getInterestRate(),
                investedAmount,
                accumulatedFactor,
                interest);

        return interest;
    }

    /**
     * Calculate floating interest (pós-fixado) using compound interest
     * Formula: Valor_Investido × (Taxa_Acumulada - 1)
     * Where Taxa_Acumulada = ∏[(1 + CDI_Anual_Dia/100)^(1/252) × Percentual_CDI]
     *
     * @param bond The bond
     * @param startDate Period start date
     * @param endDate Period end date
     * @param quantity Quantity held
     * @param unitPrice Unit price
     * @param indicatorType The interest index (CDI, SELIC, IPCA)
     * @param spread The spread/percentage that was in effect during this period
     * @return Interest amount
     */
    private BigDecimal calculateFloatingInterest(
            Bond bond,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal quantity,
            BigDecimal unitPrice,
            InterestIndex indicatorType,
            BigDecimal spread) {

        BigDecimal investedAmount = quantity.multiply(unitPrice);

        try {
            // Get accumulated indicator rate for the period
            // The spread (e.g., 110% of CDI) is applied inside the accumulation
            // Use the spread from the operation, not the current bond spread
            BigDecimal percentageOfIndicator = spread != null ? spread : bond.getInterestRate();
            BigDecimal accumulatedRate =
                    getAccumulatedIndicatorRate(
                            indicatorType, startDate, endDate, percentageOfIndicator);

            BigDecimal interest = investedAmount.multiply(accumulatedRate.subtract(BigDecimal.ONE));

            log.debug(
                    "Floating interest: bond={}, indicator={}, percentage={}, accumulated={},"
                            + " invested={}, interest={}",
                    bond.getName(),
                    indicatorType,
                    percentageOfIndicator,
                    accumulatedRate,
                    investedAmount,
                    interest);

            return interest;

        } catch (EntityNotFoundException e) {
            log.warn(
                    "Could not calculate floating interest for bond {}: {}",
                    bond.getName(),
                    e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    /**
     * Get accumulated indicator rate between two dates using compound interest
     * Formula: ∏[(1 + Taxa_Anual_Dia/100)^(1/252) × (Percentual/100)]
     *
     * IMPORTANT: The BACEN API only provides data for business days (dias úteis).
     * We iterate through the historical data provided by the API, which already
     * excludes weekends and holidays according to ANBIMA calendar.
     *
     * @param indicatorType The indicator type
     * @param startDate Start date
     * @param endDate End date
     * @param percentageOfIndicator Percentage of the indicator (e.g., 110 for 110% of CDI)
     * @return Accumulated rate (e.g., 1.05 for 5% accumulated)
     */
    private BigDecimal getAccumulatedIndicatorRate(
            InterestIndex indicatorType,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal percentageOfIndicator) {

        // Get all indicator data for the period (only business days)
        List<MarketIndicatorHistory> historicalData =
                marketIndicatorService.getIndicatorHistoryBetween(
                        indicatorType, startDate, endDate);

        if (historicalData.isEmpty()) {
            log.warn(
                    "No historical data found for {} between {} and {}",
                    indicatorType,
                    startDate,
                    endDate);
            return BigDecimal.ONE;
        }

        double accumulatedFactor = getAccumulatedFactor(percentageOfIndicator, historicalData);

        log.debug(
                "Accumulated rate for {} from {} to {}: {} business days, factor={}",
                indicatorType,
                startDate,
                endDate,
                historicalData.size(),
                accumulatedFactor);

        return BigDecimal.valueOf(accumulatedFactor);
    }

    private static double getAccumulatedFactor(
            BigDecimal percentageOfIndicator, List<MarketIndicatorHistory> historicalData) {
        double accumulatedFactor = 1.0;

        // Iterate only through business days provided by BACEN
        for (MarketIndicatorHistory data : historicalData) {
            BigDecimal dailyRate = data.getRateValue();

            // BACEN provides CDI as daily rate in %
            // Apply percentage of indicator (e.g., 100% of CDI means use the rate as-is)
            double adjustedDailyRate =
                    dailyRate.doubleValue() * (percentageOfIndicator.doubleValue() / 100.0);

            // Convert daily rate to factor: (1 + rate/100)
            double dailyFactor = 1.0 + (adjustedDailyRate / 100.0);

            // Accumulate
            accumulatedFactor *= dailyFactor;
        }

        log.debug(
                "Accumulated factor calculation: {} business days, percentage={}, final factor={}",
                historicalData.size(),
                percentageOfIndicator,
                accumulatedFactor);

        return accumulatedFactor;
    }

    /**
     * Get accumulated indicator rate between two dates (100% of indicator)
     *
     * @param indicatorType The indicator type
     * @param startDate Start date
     * @param endDate End date
     * @return Accumulated rate (e.g., 1.05 for 5% accumulated)
     */
    private BigDecimal getAccumulatedIndicatorRate(
            InterestIndex indicatorType, LocalDate startDate, LocalDate endDate) {
        return getAccumulatedIndicatorRate(indicatorType, startDate, endDate, HUNDRED);
    }

    /**
     * Get the calculation method description
     *
     * @param bond The bond
     * @return Description of the calculation method
     */
    private String getCalculationMethod(Bond bond) {
        return switch (bond.getInterestType()) {
            case FIXED -> "FIXED: " + bond.getInterestRate() + "% a.a.";
            case FLOATING ->
                    "FLOATING: " + bond.getInterestIndex() + " + " + bond.getInterestRate() + "%";
            case ZERO_COUPON -> "ZERO_COUPON";
        };
    }

    /**
     * Recalculate interest for a bond (updates existing calculation)
     *
     * @param bondId The bond ID
     * @param calculationDate The date to calculate interest until
     */
    @Transactional
    public void recalculateBondInterest(Integer bondId, LocalDate calculationDate) {
        // Delete existing calculation for this date
        bondInterestCalculationRepository.deleteByBondIdAndCalculationDate(
                bondId, calculationDate.format(org.moinex.util.Constants.DATE_FORMATTER_NO_TIME));

        // Calculate new interest
        calculateBondInterest(bondId, calculationDate);
    }

    /**
     * Get the latest interest calculation for a bond
     *
     * @param bondId The bond ID
     * @return Latest BondInterestCalculation
     */
    @Transactional(readOnly = true)
    public Optional<BondInterestCalculation> getLatestCalculation(Integer bondId) {
        return bondInterestCalculationRepository.findLatestByBondId(bondId);
    }

    /**
     * Get all interest calculations for a bond
     *
     * @param bondId The bond ID
     * @return List of BondInterestCalculation
     */
    @Transactional(readOnly = true)
    public List<BondInterestCalculation> getCalculationsByBondId(Integer bondId) {
        return bondInterestCalculationRepository.findByBondIdOrderByCalculationDateDesc(bondId);
    }

    /**
     * Calculate interest for all bonds that haven't been calculated today
     * Called on application startup
     */
    @Transactional
    public void calculateInterestForAllBondsIfNeeded() {
        log.info("Starting interest calculation for bonds that haven't been calculated today");

        List<Bond> activeBonds = bondRepository.findByArchivedFalseOrderByNameAsc();
        LocalDate today = LocalDate.now();
        String todayStr = today.format(org.moinex.util.Constants.DATE_FORMATTER_NO_TIME);
        int calculatedCount = 0;

        for (Bond bond : activeBonds) {
            // Check if this bond has operations
            List<BondOperation> operations =
                    bondOperationRepository.findByBondOrderByOperationDateAsc(bond);
            if (operations.isEmpty()) {
                continue;
            }

            // Check if calculation already exists for today
            Optional<BondInterestCalculation> existingCalculation =
                    bondInterestCalculationRepository.findByBondAndCalculationDate(bond, todayStr);

            if (existingCalculation.isEmpty()) {
                try {
                    calculateBondInterest(bond.getId(), today);
                    calculatedCount++;
                    log.debug("Calculated interest for bond: {}", bond.getName());
                } catch (Exception e) {
                    log.warn(
                            "Failed to calculate interest for bond {}: {}",
                            bond.getName(),
                            e.getMessage());
                }
            }
        }

        log.info("Interest calculation completed: {} bonds calculated", calculatedCount);
    }
}
