/*
 * Filename: BondService.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.moinex.model.Category;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.repository.investment.BondOperationRepository;
import org.moinex.repository.investment.BondRepository;
import org.moinex.util.enums.BondType;
import org.moinex.util.enums.InterestIndex;
import org.moinex.util.enums.InterestType;
import org.moinex.util.enums.OperationType;
import org.moinex.util.enums.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BondService {

    private final BondRepository bondRepository;
    private final BondOperationRepository bondOperationRepository;
    private final WalletTransactionService walletTransactionService;
    private final WalletService walletService;

    @Autowired
    public BondService(
            BondRepository bondRepository,
            BondOperationRepository bondOperationRepository,
            WalletTransactionService walletTransactionService,
            WalletService walletService) {
        this.bondRepository = bondRepository;
        this.bondOperationRepository = bondOperationRepository;
        this.walletTransactionService = walletTransactionService;
        this.walletService = walletService;
    }

    @Transactional
    public void addBond(
            String name,
            String symbol,
            BondType bondType,
            String issuer,
            LocalDateTime maturityDate,
            InterestType interestType,
            InterestIndex interestIndex,
            BigDecimal interestRate) {

        if (symbol != null && !symbol.isBlank() && bondRepository.existsBySymbol(symbol)) {
            throw new EntityExistsException("Bond with symbol " + symbol + " already exists");
        }

        Bond bond =
                Bond.builder()
                        .name(name)
                        .symbol(symbol)
                        .type(bondType)
                        .issuer(issuer)
                        .maturityDate(maturityDate)
                        .interestType(interestType)
                        .interestIndex(interestIndex)
                        .interestRate(interestRate)
                        .archived(false)
                        .build();

        bondRepository.save(bond);
        log.info("Bond {} added successfully", name);
    }

    @Transactional(readOnly = true)
    public List<Bond> getAllNonArchivedBonds() {
        return bondRepository.findByArchivedFalseOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Bond> getAllArchivedBonds() {
        return bondRepository.findByArchivedTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Bond getBondById(Integer id) {
        return bondRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bond not found with id: " + id));
    }

    @Transactional
    public void updateBond(
            Integer id,
            String name,
            String symbol,
            BondType bondType,
            String issuer,
            LocalDateTime maturityDate,
            InterestType interestType,
            InterestIndex interestIndex,
            BigDecimal interestRate) {

        Bond bond =
                bondRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Bond not found with id: " + id));

        bond.setName(name);
        bond.setSymbol(symbol);
        bond.setType(bondType);
        bond.setIssuer(issuer);
        bond.setMaturityDate(maturityDate);
        bond.setInterestType(interestType);
        bond.setInterestIndex(interestIndex);
        bond.setInterestRate(interestRate);

        bondRepository.save(bond);
        log.info("Bond {} updated successfully", name);
    }

    @Transactional(readOnly = true)
    public Integer getOperationCountByBond(Integer bondId) {
        Bond bond =
                bondRepository
                        .findById(bondId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Bond not found with id: " + bondId));

        return bondOperationRepository.findByBondOrderByOperationDateAsc(bond).size();
    }

    @Transactional
    public void deleteBond(Integer id) {
        Bond bond =
                bondRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Bond not found with id: " + id));

        // Check if the bond has operations associated with it
        List<BondOperation> operations =
                bondOperationRepository.findByBondOrderByOperationDateAsc(bond);
        if (!operations.isEmpty()) {
            throw new IllegalStateException(
                    String.format(
                            "Bond with id %d has operations associated with it and cannot be"
                                    + " deleted. Remove the operations first or archive the bond",
                            id));
        }

        bondRepository.delete(bond);
        log.info("Bond {} was permanently deleted", bond.getName());
    }

    @Transactional
    public void archiveBond(Integer id) {
        Bond bond =
                bondRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Bond not found with id: " + id));

        bond.setArchived(true);
        bondRepository.save(bond);
        log.info("Bond {} archived", bond.getName());
    }

    @Transactional
    public void unarchiveBond(Integer id) {
        Bond bond =
                bondRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Bond not found with id: " + id));

        bond.setArchived(false);
        bondRepository.save(bond);
        log.info("Bond {} unarchived", bond.getName());
    }

    @Transactional(readOnly = true)
    public BigDecimal getCurrentQuantity(Bond bond) {
        List<BondOperation> operations =
                bondOperationRepository.findByBondOrderByOperationDateAsc(bond);

        BigDecimal quantity = BigDecimal.ZERO;
        for (BondOperation op : operations) {
            if (op.getOperationType() == OperationType.BUY) {
                quantity = quantity.add(op.getQuantity());
            } else {
                quantity = quantity.subtract(op.getQuantity());
            }
        }
        return quantity;
    }

    @Transactional(readOnly = true)
    public BigDecimal getAverageUnitPrice(Bond bond) {
        List<BondOperation> purchases =
                bondOperationRepository.findByBondAndOperationTypeOrderByOperationDateAsc(
                        bond, OperationType.BUY);

        if (purchases.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalValue = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (BondOperation purchase : purchases) {
            totalValue = totalValue.add(purchase.getUnitPrice().multiply(purchase.getQuantity()));
            totalQuantity = totalQuantity.add(purchase.getQuantity());
        }

        if (totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalValue.divide(totalQuantity, 8, java.math.RoundingMode.HALF_UP);
    }

    @Transactional
    public void addOperation(
            Integer bondId,
            Integer walletId,
            OperationType operationType,
            BigDecimal quantity,
            BigDecimal unitPrice,
            LocalDate operationDate,
            BigDecimal fees,
            BigDecimal taxes,
            BigDecimal netProfit,
            Category category,
            String description,
            TransactionStatus status) {
        Bond bond =
                bondRepository
                        .findById(bondId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Bond not found with id: " + bondId));

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }

        if (operationType == OperationType.SELL) {
            BigDecimal currentQuantity = getCurrentQuantity(bond);
            if (currentQuantity.compareTo(quantity) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient bond quantity. Available: "
                                + currentQuantity
                                + ", Requested: "
                                + quantity);
            }
        }

        BigDecimal feesAmount = fees != null ? fees : BigDecimal.ZERO;
        BigDecimal taxesAmount = taxes != null ? taxes : BigDecimal.ZERO;
        BigDecimal netProfitAmount = netProfit != null ? netProfit : BigDecimal.ZERO;

        BigDecimal baseAmount = unitPrice.multiply(quantity);
        BigDecimal amount;

        if (operationType == OperationType.BUY) {
            amount = baseAmount.add(feesAmount).add(taxesAmount);
        } else {
            amount = baseAmount.add(netProfitAmount);
        }

        LocalDateTime dateTime = operationDate.atStartOfDay();
        Integer transactionId;

        if (operationType == OperationType.BUY) {
            transactionId =
                    walletTransactionService.addExpense(
                            walletId, category, dateTime, amount, description, status);
        } else {
            transactionId =
                    walletTransactionService.addIncome(
                            walletId, category, dateTime, amount, description, status);
        }

        WalletTransaction walletTransaction =
                walletTransactionService.getTransactionById(transactionId);

        BondOperation operation =
                BondOperation.builder()
                        .bond(bond)
                        .operationType(operationType)
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .fees(fees)
                        .taxes(taxes)
                        .netProfit(netProfit)
                        .walletTransaction(walletTransaction)
                        .build();

        bondOperationRepository.save(operation);

        log.info(
                "BondOperation {} with id {} added to bond {}. Wallet transaction with id {}"
                        + " created",
                operationType,
                operation.getId(),
                bond.getName(),
                transactionId);
    }

    @Transactional(readOnly = true)
    public List<BondOperation> getAllOperations() {
        return bondOperationRepository.findAllByOrderByOperationDateDesc();
    }

    @Transactional(readOnly = true)
    public List<BondOperation> getOperationsByBond(Bond bond) {
        return bondOperationRepository.findByBondOrderByOperationDateAsc(bond);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateProfit(Bond bond) {
        List<BondOperation> operations =
                bondOperationRepository.findByBondOrderByOperationDateAsc(bond);

        BigDecimal totalProfit = BigDecimal.ZERO;

        for (BondOperation op : operations) {
            if (op.getOperationType() == OperationType.SELL && op.getNetProfit() != null) {
                totalProfit = totalProfit.add(op.getNetProfit());
            }
        }

        return totalProfit;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalInvestedValue() {
        List<Bond> bonds = bondRepository.findByArchivedFalseOrderByNameAsc();
        BigDecimal total = BigDecimal.ZERO;

        for (Bond bond : bonds) {
            BigDecimal currentQuantity = getCurrentQuantity(bond);
            if (currentQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal averagePrice = getAverageUnitPrice(bond);
                total = total.add(averagePrice.multiply(currentQuantity));
            }
        }

        return total;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalCurrentValue(BigDecimal currentMarketPrice) {
        List<Bond> bonds = bondRepository.findByArchivedFalseOrderByNameAsc();
        BigDecimal total = BigDecimal.ZERO;

        for (Bond bond : bonds) {
            total = total.add(currentMarketPrice.multiply(getCurrentQuantity(bond)));
        }

        return total;
    }

    @Transactional(readOnly = true)
    public BigDecimal getInvestedValue(Bond bond) {
        return getAverageUnitPrice(bond).multiply(getCurrentQuantity(bond));
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalInterestReceived() {
        List<Bond> bonds = bondRepository.findByArchivedFalseOrderByNameAsc();
        BigDecimal total = BigDecimal.ZERO;

        for (Bond bond : bonds) {
            List<BondOperation> operations =
                    bondOperationRepository.findByBondOrderByOperationDateAsc(bond);

            for (BondOperation op : operations) {
                if (op.getOperationType() == OperationType.SELL && op.getNetProfit() != null) {
                    if (op.getNetProfit().compareTo(BigDecimal.ZERO) > 0) {
                        total = total.add(op.getNetProfit());
                    }
                }
            }
        }

        return total;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateOperationProfitLoss(BondOperation operation) {
        if (operation.getOperationType() == OperationType.BUY) {
            return BigDecimal.ZERO;
        }

        return operation.getNetProfit() != null ? operation.getNetProfit() : BigDecimal.ZERO;
    }

    @Transactional
    public void updateOperation(
            Integer operationId,
            Integer walletId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            LocalDate operationDate,
            BigDecimal fees,
            BigDecimal taxes,
            BigDecimal netProfit,
            Category category,
            String description,
            TransactionStatus status) {
        BondOperation operation =
                bondOperationRepository
                        .findById(operationId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "BondOperation not found with id: " + operationId));

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unit price must be greater than zero");
        }

        if (operation.getOperationType() == OperationType.SELL) {
            BigDecimal currentQuantity = getCurrentQuantity(operation.getBond());
            BigDecimal availableQuantity = currentQuantity.add(operation.getQuantity());
            if (availableQuantity.compareTo(quantity) < 0) {
                throw new IllegalArgumentException(
                        "Insufficient bond quantity. Available: "
                                + availableQuantity
                                + ", Requested: "
                                + quantity);
            }
        }

        BigDecimal feesAmount = fees != null ? fees : BigDecimal.ZERO;
        BigDecimal taxesAmount = taxes != null ? taxes : BigDecimal.ZERO;
        BigDecimal netProfitAmount = netProfit != null ? netProfit : BigDecimal.ZERO;

        BigDecimal baseAmount = unitPrice.multiply(quantity);
        BigDecimal amount;

        if (operation.getOperationType() == OperationType.BUY) {
            amount = baseAmount.add(feesAmount).add(taxesAmount);
        } else {
            amount = baseAmount.add(netProfitAmount);
        }

        LocalDateTime dateTime = operationDate.atStartOfDay();

        WalletTransaction walletTransaction = operation.getWalletTransaction();
        walletTransaction.setWallet(walletService.getWalletById(walletId));
        walletTransaction.setCategory(category);
        walletTransaction.setDate(dateTime);
        walletTransaction.setAmount(amount);
        walletTransaction.setDescription(description);
        walletTransaction.setStatus(status);

        walletTransactionService.updateTransaction(walletTransaction);

        operation.setQuantity(quantity);
        operation.setUnitPrice(unitPrice);
        operation.setFees(fees);
        operation.setTaxes(taxes);
        operation.setNetProfit(netProfit);

        bondOperationRepository.save(operation);

        log.info("BondOperation with id {} updated successfully", operationId);
    }

    @Transactional
    public void deleteOperation(Integer operationId) {
        BondOperation operation =
                bondOperationRepository
                        .findById(operationId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "BondOperation not found with id: " + operationId));

        WalletTransaction walletTransaction = operation.getWalletTransaction();

        bondOperationRepository.delete(operation);

        if (walletTransaction != null) {
            walletTransactionService.deleteTransaction(walletTransaction.getId());
        }

        log.info("BondOperation with id {} deleted successfully", operationId);
    }

    @Transactional(readOnly = true)
    public BondOperation getOperationById(Integer operationId) {
        return bondOperationRepository
                .findById(operationId)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "BondOperation not found with id: " + operationId));
    }
}
