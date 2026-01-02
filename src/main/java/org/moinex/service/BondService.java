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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.moinex.model.investment.Bond;
import org.moinex.repository.investment.BondRepository;
import org.moinex.util.enums.BondType;
import org.moinex.util.enums.InterestIndex;
import org.moinex.util.enums.InterestType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BondService {

    private final BondRepository bondRepository;

    @Autowired
    public BondService(BondRepository bondRepository) {
        this.bondRepository = bondRepository;
    }

    @Transactional
    public void addBond(
            String name,
            String symbol,
            BondType bondType,
            BigDecimal currentValue,
            BigDecimal averageValue,
            BigDecimal quantity,
            InterestType interestType,
            InterestIndex interestIndex,
            BigDecimal interestRate,
            LocalDate maturityDate) {

        if (bondRepository.existsBySymbol(symbol)) {
            throw new EntityExistsException("Bond with symbol " + symbol + " already exists");
        }

        Bond bond =
                Bond.builder()
                        .name(name)
                        .symbol(symbol)
                        .type(bondType)
                        .currentUnitValue(currentValue)
                        .averageUnitValue(averageValue)
                        .currentQuantity(quantity)
                        .averageUnitValueCount(BigDecimal.ONE)
                        .interestType(interestType)
                        .interestIndex(interestIndex)
                        .interestRate(interestRate)
                        .maturityDate(maturityDate != null ? maturityDate.toString() : null)
                        .archived(false)
                        .build();

        bondRepository.save(bond);
        log.info("Bond {} added successfully", symbol);
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
    public Bond getBondBySymbol(String symbol) {
        return bondRepository
                .findBySymbol(symbol)
                .orElseThrow(() -> new EntityNotFoundException("Bond not found: " + symbol));
    }

    @Transactional
    public void updateBond(
            Integer id,
            String name,
            String symbol,
            BondType bondType,
            BigDecimal currentValue,
            InterestType interestType,
            InterestIndex interestIndex,
            BigDecimal interestRate,
            LocalDate maturityDate) {

        Bond bond =
                bondRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Bond not found with id: " + id));

        bond.setName(name);
        bond.setSymbol(symbol);
        bond.setType(bondType);
        bond.setCurrentUnitValue(currentValue);
        bond.setInterestType(interestType);
        bond.setInterestIndex(interestIndex);
        bond.setInterestRate(interestRate);
        bond.setMaturityDate(maturityDate != null ? maturityDate.toString() : null);

        bondRepository.save(bond);
        log.info("Bond {} updated successfully", symbol);
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
        log.info("Bond {} archived", bond.getSymbol());
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
        log.info("Bond {} unarchived", bond.getSymbol());
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalBondValue() {
        return bondRepository.findByArchivedFalseOrderByNameAsc().stream()
                .map(bond -> bond.getCurrentQuantity().multiply(bond.getCurrentUnitValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
