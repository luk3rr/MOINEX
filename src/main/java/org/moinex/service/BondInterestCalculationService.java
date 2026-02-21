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
import java.time.YearMonth;
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
     * Get the latest interest calculation for a bond (last calculated month)
     *
     * @param bondId The bond ID
     * @return Latest BondInterestCalculation for the last calculated month
     */
    @Transactional(readOnly = true)
    public Optional<BondInterestCalculation> getLatestCalculation(Integer bondId) {
        Bond bond =
                bondRepository
                        .findById(bondId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Bond not found with id: " + bondId));
        return bondInterestCalculationRepository.findLastCalculatedMonth(bond);
    }

    /**
     * Synchronize monthly interest calculations for all bonds
     * Ensures all bonds have their monthly interest history up to date
     * Called on application startup
     */
    @Transactional
    public void calculateInterestForAllBondsIfNeeded() {
        log.info("Starting monthly interest synchronization for all bonds");

        List<Bond> activeBonds = bondRepository.findByArchivedFalseOrderByNameAsc();
        int syncedCount = 0;

        for (Bond bond : activeBonds) {
            // Check if this bond has operations
            List<BondOperation> operations =
                    bondOperationRepository.findByBondOrderByOperationDateAsc(bond);
            if (operations.isEmpty()) {
                log.debug("Bond {} has no operations, skipping", bond.getName());
                continue;
            }

            try {
                // Calculate and store monthly interest history
                calculateAndStoreMonthlyInterest(bond);
                syncedCount++;
                log.debug("Synchronized monthly interest for bond: {}", bond.getName());
            } catch (Exception e) {
                log.warn(
                        "Failed to synchronize monthly interest for bond {}: {}",
                        bond.getName(),
                        e.getMessage());
            }
        }

        log.info("Monthly interest synchronization completed: {} bonds synced", syncedCount);
    }

    /**
     * Calculate interest for a specific month
     *
     * @param bond The bond
     * @param month The month to calculate interest for
     * @return Monthly interest amount
     */
    private BigDecimal calculateMonthlyInterest(Bond bond, YearMonth month) {
        List<BondOperation> operations =
                bondOperationRepository.findByBondOrderByOperationDateAsc(bond);

        if (operations.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDate monthStart = month.atDay(1);
        LocalDate monthEnd = month.atEndOfMonth();

        BigDecimal monthlyInterest = BigDecimal.ZERO;
        BigDecimal currentQuantity = BigDecimal.ZERO;
        LocalDate lastCalculationDate = monthStart;
        BigDecimal lastOperationSpread = null;

        for (BondOperation operation : operations) {
            LocalDate operationDate = operation.getWalletTransaction().getDate().toLocalDate();

            if (operationDate.isAfter(monthEnd)) {
                break;
            }

            if (operationDate.isBefore(monthStart)) {
                if (operation.getOperationType() == OperationType.BUY) {
                    currentQuantity = currentQuantity.add(operation.getQuantity());
                    if (operation.getSpread() != null) {
                        lastOperationSpread = operation.getSpread();
                    }
                } else {
                    currentQuantity = currentQuantity.subtract(operation.getQuantity());
                }
                continue;
            }

            if (currentQuantity.compareTo(BigDecimal.ZERO) > 0
                    && operationDate.isAfter(lastCalculationDate)) {
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
                monthlyInterest = monthlyInterest.add(periodInterest);
            }

            if (operation.getOperationType() == OperationType.BUY) {
                currentQuantity = currentQuantity.add(operation.getQuantity());
                if (operation.getSpread() != null) {
                    lastOperationSpread = operation.getSpread();
                }
            } else {
                currentQuantity = currentQuantity.subtract(operation.getQuantity());
            }

            lastCalculationDate = operationDate;
        }

        if (currentQuantity.compareTo(BigDecimal.ZERO) > 0
                && lastCalculationDate.isBefore(monthEnd)) {
            BigDecimal lastOperationPrice =
                    operations.stream()
                            .filter(op -> op.getOperationType() == OperationType.BUY)
                            .reduce((first, second) -> second)
                            .map(BondOperation::getUnitPrice)
                            .orElse(BigDecimal.ZERO);

            BigDecimal spreadToUse =
                    lastOperationSpread != null ? lastOperationSpread : bond.getInterestRate();
            BigDecimal periodInterest =
                    calculatePeriodInterest(
                            bond,
                            lastCalculationDate,
                            monthEnd,
                            currentQuantity,
                            lastOperationPrice,
                            spreadToUse);
            monthlyInterest = monthlyInterest.add(periodInterest);
        }

        return monthlyInterest;
    }

    /**
     * Calculate and store monthly interest for all months from first operation to today
     *
     * @param bond The bond
     */
    @Transactional
    public void calculateAndStoreMonthlyInterest(Bond bond) {
        List<BondOperation> operations =
                bondOperationRepository.findByBondOrderByOperationDateAsc(bond);

        if (operations.isEmpty()) {
            log.warn(
                    "Bond {} has no operations, skipping monthly interest calculation",
                    bond.getName());
            return;
        }

        Optional<BondOperation> firstBuy =
                operations.stream()
                        .filter(op -> op.getOperationType() == OperationType.BUY)
                        .findFirst();

        if (firstBuy.isEmpty()) {
            log.warn(
                    "Bond {} has no buy operations, skipping monthly interest calculation",
                    bond.getName());
            return;
        }

        LocalDate startDate = firstBuy.get().getWalletTransaction().getDate().toLocalDate();
        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth currentMonth = YearMonth.now();

        // Find the last calculated month (by referenceMonth, not calculationDate)
        Optional<BondInterestCalculation> lastCalculation =
                bondInterestCalculationRepository.findLastCalculatedMonth(bond);
        YearMonth monthToStart = startMonth;
        BigDecimal accumulatedInterest = BigDecimal.ZERO;

        if (lastCalculation.isPresent()) {
            // Start from the month after the last calculation
            YearMonth lastMonth = lastCalculation.get().getReferenceMonth();
            accumulatedInterest = lastCalculation.get().getAccumulatedInterest();

            // Only recalculate if there are missing months or if it's the current month
            if (lastMonth.equals(currentMonth)) {
                log.debug(
                        "Bond {} already has calculation for current month, only updating it",
                        bond.getName());
                monthToStart = currentMonth;
            } else {
                monthToStart = lastMonth.plusMonths(1);
                log.info(
                        "Bond {} has missing months from {} to {}, calculating...",
                        bond.getName(),
                        monthToStart,
                        currentMonth);
            }
        } else {
            log.info(
                    "No previous calculations found for bond {}, calculating from {} to {}",
                    bond.getName(),
                    startMonth,
                    currentMonth);
        }

        YearMonth month = monthToStart;

        while (month.isBefore(currentMonth) || month.equals(currentMonth)) {
            Optional<BondInterestCalculation> existingCalculation =
                    bondInterestCalculationRepository.findByBondAndReferenceMonth(
                            bond, month.toString());

            BigDecimal monthlyInterest = calculateMonthlyInterest(bond, month);

            BigDecimal investedAmount = calculateInvestedAmountForMonth(bond, month);
            BigDecimal currentQuantity = calculateQuantityForMonth(bond, month);

            if (existingCalculation.isPresent()) {
                // For existing calculations, only update monthly interest and quantity
                // Keep the accumulated interest from the previous month
                BondInterestCalculation existing = existingCalculation.get();
                BigDecimal previousAccumulated =
                        existing.getAccumulatedInterest().subtract(existing.getMonthlyInterest());
                BigDecimal newAccumulated = previousAccumulated.add(monthlyInterest);
                BigDecimal finalValue = investedAmount.add(newAccumulated);

                existing.setCalculationDate(LocalDate.now());
                existing.setQuantity(currentQuantity);
                existing.setInvestedAmount(investedAmount);
                existing.setMonthlyInterest(monthlyInterest);
                existing.setAccumulatedInterest(newAccumulated);
                existing.setFinalValue(finalValue);
                existing.setCalculationMethod(getCalculationMethod(bond));

                bondInterestCalculationRepository.save(existing);
                accumulatedInterest = newAccumulated;
                log.debug(
                        "Updated monthly interest for bond {} month {}: monthly={}, accumulated={}",
                        bond.getName(),
                        month,
                        monthlyInterest,
                        newAccumulated);
            } else {
                // Create new calculation for missing months
                accumulatedInterest = accumulatedInterest.add(monthlyInterest);
                BigDecimal finalValue = investedAmount.add(accumulatedInterest);

                BondInterestCalculation calculation =
                        BondInterestCalculation.builder()
                                .bond(bond)
                                .referenceMonth(month)
                                .calculationDate(LocalDate.now())
                                .quantity(currentQuantity)
                                .investedAmount(investedAmount)
                                .monthlyInterest(monthlyInterest)
                                .accumulatedInterest(accumulatedInterest)
                                .finalValue(finalValue)
                                .calculationMethod(getCalculationMethod(bond))
                                .createdAt(LocalDateTime.now())
                                .build();

                bondInterestCalculationRepository.save(calculation);
                log.debug(
                        "Saved monthly interest for bond {} month {}: monthly={}, accumulated={}",
                        bond.getName(),
                        month,
                        monthlyInterest,
                        accumulatedInterest);
            }

            month = month.plusMonths(1);
        }

        log.info(
                "Monthly interest calculation completed for bond {} with total accumulated: {}",
                bond.getName(),
                accumulatedInterest);
    }

    /**
     * Calculate invested amount for a specific month
     *
     * @param bond The bond
     * @param month The month
     * @return Total invested amount in that month
     */
    private BigDecimal calculateInvestedAmountForMonth(Bond bond, YearMonth month) {
        List<BondOperation> operations =
                bondOperationRepository.findByBondOrderByOperationDateAsc(bond);

        BigDecimal investedAmount = BigDecimal.ZERO;
        LocalDate monthEnd = month.atEndOfMonth();

        for (BondOperation operation : operations) {
            LocalDate operationDate = operation.getWalletTransaction().getDate().toLocalDate();

            if (operationDate.isAfter(monthEnd)) {
                break;
            }

            if (operation.getOperationType() == OperationType.BUY) {
                investedAmount =
                        investedAmount.add(
                                operation.getQuantity().multiply(operation.getUnitPrice()));
            }
        }

        return investedAmount;
    }

    /**
     * Calculate quantity held at end of a specific month
     *
     * @param bond The bond
     * @param month The month
     * @return Quantity held at end of month
     */
    private BigDecimal calculateQuantityForMonth(Bond bond, YearMonth month) {
        List<BondOperation> operations =
                bondOperationRepository.findByBondOrderByOperationDateAsc(bond);

        BigDecimal quantity = BigDecimal.ZERO;
        LocalDate monthEnd = month.atEndOfMonth();

        for (BondOperation operation : operations) {
            LocalDate operationDate = operation.getWalletTransaction().getDate().toLocalDate();

            if (operationDate.isAfter(monthEnd)) {
                break;
            }

            if (operation.getOperationType() == OperationType.BUY) {
                quantity = quantity.add(operation.getQuantity());
            } else {
                quantity = quantity.subtract(operation.getQuantity());
            }
        }

        return quantity;
    }

    /**
     * Get current month interest from database (calculated at startup)
     * Does not recalculate, only retrieves stored value for current month
     *
     * @param bondId The bond ID
     * @return Current month interest from database
     */
    @Transactional(readOnly = true)
    public Optional<BigDecimal> getCurrentMonthInterestFromDatabase(Integer bondId) {
        Bond bond =
                bondRepository
                        .findById(bondId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Bond not found with id: " + bondId));
        YearMonth currentMonth = YearMonth.now();
        return bondInterestCalculationRepository
                .findByBondAndReferenceMonth(bond, currentMonth.toString())
                .map(BondInterestCalculation::getMonthlyInterest);
    }

    /**
     * Get monthly interest history for a bond
     *
     * @param bond The bond
     * @return List of monthly interest calculations
     */
    @Transactional(readOnly = true)
    public List<BondInterestCalculation> getMonthlyInterestHistory(Bond bond) {
        return bondInterestCalculationRepository.findByBondOrderByReferenceMonthAsc(bond);
    }
}
