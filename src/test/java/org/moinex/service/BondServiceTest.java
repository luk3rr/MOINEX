/*
 * Filename: BondServiceTest.java
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.model.Category;
import org.moinex.model.enums.BondType;
import org.moinex.model.enums.InterestIndex;
import org.moinex.model.enums.InterestType;
import org.moinex.model.enums.OperationType;
import org.moinex.model.enums.TransactionStatus;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.BondOperation;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletTransaction;
import org.moinex.repository.investment.BondOperationRepository;
import org.moinex.repository.investment.BondRepository;

@ExtendWith(MockitoExtension.class)
class BondServiceTest {
    @Mock private BondRepository bondRepository;

    @Mock private BondOperationRepository bondOperationRepository;

    @Mock private WalletTransactionService walletTransactionService;

    @Mock private WalletService walletService;

    @InjectMocks private BondService bondService;

    private Bond bond1;
    private Bond bond2;
    private Bond archivedBond;
    private BondOperation buyOperation;
    private BondOperation sellOperation;
    private Wallet wallet;
    private WalletTransaction walletTransaction;

    @BeforeEach
    void beforeEach() {
        wallet = new Wallet(1, "Test Wallet", BigDecimal.valueOf(10000));

        LocalDateTime futureDate3Years = LocalDateTime.now().plusYears(3);
        LocalDateTime futureDate1Year = LocalDateTime.now().plusYears(1);
        LocalDateTime futureDate6Months = LocalDateTime.now().plusMonths(6);

        bond1 =
                Bond.builder()
                        .name("Tesouro Selic 2029")
                        .symbol("SELIC2029")
                        .type(BondType.TREASURY_POSTFIXED)
                        .issuer("Tesouro Nacional")
                        .maturityDate(futureDate3Years)
                        .interestType(InterestType.FLOATING)
                        .interestIndex(InterestIndex.SELIC)
                        .archived(false)
                        .build();

        bond2 =
                Bond.builder()
                        .name("CDB Banco XYZ")
                        .symbol("CDB123")
                        .type(BondType.CDB)
                        .issuer("Banco XYZ")
                        .maturityDate(futureDate1Year)
                        .interestType(InterestType.FIXED)
                        .interestRate(BigDecimal.valueOf(12.5))
                        .archived(false)
                        .build();

        archivedBond =
                Bond.builder()
                        .name("LCI Antiga")
                        .symbol("LCI001")
                        .type(BondType.LCI)
                        .issuer("Banco ABC")
                        .maturityDate(futureDate6Months)
                        .interestType(InterestType.FLOATING)
                        .interestIndex(InterestIndex.IPCA)
                        .interestRate(BigDecimal.valueOf(5.0))
                        .archived(true)
                        .build();

        walletTransaction =
                WalletTransaction.builder()
                        .wallet(wallet)
                        .amount(BigDecimal.valueOf(1000))
                        .date(LocalDateTime.now())
                        .build();

        buyOperation =
                BondOperation.builder()
                        .bond(bond1)
                        .operationType(OperationType.BUY)
                        .quantity(BigDecimal.valueOf(10))
                        .unitPrice(BigDecimal.valueOf(100))
                        .fees(BigDecimal.valueOf(5))
                        .taxes(BigDecimal.ZERO)
                        .walletTransaction(walletTransaction)
                        .build();

        sellOperation =
                BondOperation.builder()
                        .bond(bond1)
                        .operationType(OperationType.SELL)
                        .quantity(BigDecimal.valueOf(5))
                        .unitPrice(BigDecimal.valueOf(110))
                        .fees(BigDecimal.valueOf(3))
                        .taxes(BigDecimal.ZERO)
                        .walletTransaction(walletTransaction)
                        .build();
    }

    @Nested
    @DisplayName("Get All Bonds Tests")
    class GetAllBondsTests {
        @Test
        @DisplayName("Should return all non-archived bonds")
        void shouldReturnAllNonArchivedBonds() {
            when(bondRepository.findByArchivedFalseOrderByNameAsc())
                    .thenReturn(Arrays.asList(bond1, bond2));

            List<Bond> result = bondService.getAllNonArchivedBonds();

            assertEquals(2, result.size());
            assertFalse(result.get(0).isArchived());
            assertFalse(result.get(1).isArchived());
            verify(bondRepository).findByArchivedFalseOrderByNameAsc();
        }

        @Test
        @DisplayName("Should return all archived bonds")
        void shouldReturnAllArchivedBonds() {
            when(bondRepository.findByArchivedTrueOrderByNameAsc())
                    .thenReturn(Collections.singletonList(archivedBond));

            List<Bond> result = bondService.getAllArchivedBonds();

            assertEquals(1, result.size());
            assertTrue(result.get(0).isArchived());
            verify(bondRepository).findByArchivedTrueOrderByNameAsc();
        }

        @Test
        @DisplayName("Should return empty list when no bonds exist")
        void shouldReturnEmptyListWhenNoBondsExist() {
            when(bondRepository.findByArchivedFalseOrderByNameAsc())
                    .thenReturn(Collections.emptyList());

            List<Bond> result = bondService.getAllNonArchivedBonds();

            assertTrue(result.isEmpty());
            verify(bondRepository).findByArchivedFalseOrderByNameAsc();
        }
    }

    @Nested
    @DisplayName("Archive/Unarchive Bond Tests")
    class ArchiveUnarchiveBondTests {
        @Test
        @DisplayName("Should archive bond successfully")
        void shouldArchiveBondSuccessfully() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));
            when(bondRepository.save(any(Bond.class))).thenReturn(bond1);

            bondService.archiveBond(1);

            verify(bondRepository).findById(1);
            verify(bondRepository).save(argThat(bond -> bond.isArchived()));
        }

        @Test
        @DisplayName("Should unarchive bond successfully")
        void shouldUnarchiveBondSuccessfully() {
            when(bondRepository.findById(3)).thenReturn(Optional.of(archivedBond));
            when(bondRepository.save(any(Bond.class))).thenReturn(archivedBond);

            bondService.unarchiveBond(3);

            verify(bondRepository).findById(3);
            verify(bondRepository).save(argThat(bond -> !bond.isArchived()));
        }

        @Test
        @DisplayName("Should throw exception when archiving non-existent bond")
        void shouldThrowExceptionWhenArchivingNonExistentBond() {
            when(bondRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> bondService.archiveBond(999));
            verify(bondRepository).findById(999);
            verify(bondRepository, never()).save(any(Bond.class));
        }

        @Test
        @DisplayName("Should throw exception when unarchiving non-existent bond")
        void shouldThrowExceptionWhenUnarchivingNonExistentBond() {
            when(bondRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> bondService.unarchiveBond(999));
            verify(bondRepository).findById(999);
            verify(bondRepository, never()).save(any(Bond.class));
        }
    }

    @Nested
    @DisplayName("Delete Bond Tests")
    class DeleteBondTests {
        @Test
        @DisplayName("Should delete bond without operations")
        void shouldDeleteBondWithoutOperations() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.emptyList());

            bondService.deleteBond(1);

            verify(bondRepository).findById(1);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
            verify(bondRepository).delete(bond1);
        }

        @Test
        @DisplayName("Should throw exception when deleting bond with operations")
        void shouldThrowExceptionWhenDeletingBondWithOperations() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(buyOperation));

            assertThrows(IllegalStateException.class, () -> bondService.deleteBond(1));
            verify(bondRepository).findById(1);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
            verify(bondRepository, never()).delete(any(Bond.class));
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent bond")
        void shouldThrowExceptionWhenDeletingNonExistentBond() {
            when(bondRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> bondService.deleteBond(999));
            verify(bondRepository).findById(999);
            verify(bondRepository, never()).delete(any(Bond.class));
        }
    }

    @Nested
    @DisplayName("Get Operation Count Tests")
    class GetOperationCountTests {
        @Test
        @DisplayName("Should return correct operation count")
        void shouldReturnCorrectOperationCount() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Arrays.asList(buyOperation, sellOperation));

            int count = bondService.getOperationCountByBond(1);

            assertEquals(2, count);
            verify(bondRepository).findById(1);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
        }

        @Test
        @DisplayName("Should return zero when bond has no operations")
        void shouldReturnZeroWhenBondHasNoOperations() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.emptyList());

            int count = bondService.getOperationCountByBond(1);

            assertEquals(0, count);
            verify(bondRepository).findById(1);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
        }

        @Test
        @DisplayName("Should throw exception when bond not found")
        void shouldThrowExceptionWhenBondNotFound() {
            when(bondRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class, () -> bondService.getOperationCountByBond(999));
            verify(bondRepository).findById(999);
            verify(bondOperationRepository, never()).findByBondOrderByOperationDateAsc(any());
        }
    }

    @Nested
    @DisplayName("Get Current Quantity Tests")
    class GetCurrentQuantityTests {
        @Test
        @DisplayName("Should calculate current quantity correctly")
        void shouldCalculateCurrentQuantityCorrectly() {
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Arrays.asList(buyOperation, sellOperation));

            BigDecimal quantity = bondService.getCurrentQuantity(bond1);

            assertEquals(BigDecimal.valueOf(5), quantity);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
        }

        @Test
        @DisplayName("Should return zero when bond has no operations")
        void shouldReturnZeroWhenBondHasNoOperations() {
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.emptyList());

            BigDecimal quantity = bondService.getCurrentQuantity(bond1);

            assertEquals(BigDecimal.ZERO, quantity);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
        }

        @Test
        @DisplayName("Should return correct quantity for only buy operations")
        void shouldReturnCorrectQuantityForOnlyBuyOperations() {
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(buyOperation));

            BigDecimal quantity = bondService.getCurrentQuantity(bond1);

            assertEquals(BigDecimal.valueOf(10), quantity);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
        }
    }

    @Nested
    @DisplayName("Get Average Unit Price Tests")
    class GetAverageUnitPriceTests {
        @Test
        @DisplayName("Should calculate average unit price correctly")
        void shouldCalculateAverageUnitPriceCorrectly() {
            when(bondOperationRepository.findByBondAndOperationTypeOrderByOperationDateAsc(
                            bond1, OperationType.BUY))
                    .thenReturn(Collections.singletonList(buyOperation));

            BigDecimal avgPrice = bondService.getAverageUnitPrice(bond1);

            assertEquals(0, BigDecimal.valueOf(100).compareTo(avgPrice));
            verify(bondOperationRepository)
                    .findByBondAndOperationTypeOrderByOperationDateAsc(bond1, OperationType.BUY);
        }

        @Test
        @DisplayName("Should return zero when bond has no operations")
        void shouldReturnZeroWhenBondHasNoOperations() {
            when(bondOperationRepository.findByBondAndOperationTypeOrderByOperationDateAsc(
                            bond1, OperationType.BUY))
                    .thenReturn(Collections.emptyList());

            BigDecimal avgPrice = bondService.getAverageUnitPrice(bond1);

            assertEquals(BigDecimal.ZERO, avgPrice);
            verify(bondOperationRepository)
                    .findByBondAndOperationTypeOrderByOperationDateAsc(bond1, OperationType.BUY);
        }
    }

    @Nested
    @DisplayName("Get Invested Value Tests")
    class GetInvestedValueTests {
        @Test
        @DisplayName("Should calculate invested value correctly")
        void shouldCalculateInvestedValueCorrectly() {
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Arrays.asList(buyOperation, sellOperation));

            when(bondOperationRepository.findByBondAndOperationTypeOrderByOperationDateAsc(
                            bond1, OperationType.BUY))
                    .thenReturn(Collections.singletonList(buyOperation));

            BigDecimal invested = bondService.getInvestedValue(bond1);

            assertEquals(0, BigDecimal.valueOf(500).compareTo(invested));
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
            verify(bondOperationRepository)
                    .findByBondAndOperationTypeOrderByOperationDateAsc(bond1, OperationType.BUY);
        }

        @Test
        @DisplayName("Should return zero when bond has no operations")
        void shouldReturnZeroWhenBondHasNoOperations() {
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.emptyList());

            when(bondOperationRepository.findByBondAndOperationTypeOrderByOperationDateAsc(
                            bond1, OperationType.BUY))
                    .thenReturn(Collections.emptyList());

            BigDecimal invested = bondService.getInvestedValue(bond1);

            assertEquals(BigDecimal.ZERO, invested);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
            verify(bondOperationRepository)
                    .findByBondAndOperationTypeOrderByOperationDateAsc(bond1, OperationType.BUY);
        }
    }

    @Nested
    @DisplayName("Calculate Profit Tests")
    class CalculateProfitTests {
        @Test
        @DisplayName("Should calculate profit correctly with operations")
        void shouldCalculateProfitCorrectlyWithOperations() {
            BondOperation operationWithProfit =
                    BondOperation.builder()
                            .bond(bond1)
                            .operationType(OperationType.SELL)
                            .quantity(BigDecimal.valueOf(5))
                            .unitPrice(BigDecimal.valueOf(110))
                            .fees(BigDecimal.valueOf(3))
                            .taxes(BigDecimal.ZERO)
                            .netProfit(BigDecimal.valueOf(47))
                            .walletTransaction(walletTransaction)
                            .build();

            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(operationWithProfit));

            BigDecimal profit = bondService.calculateProfit(bond1);

            assertEquals(BigDecimal.valueOf(47), profit);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
        }

        @Test
        @DisplayName("Should return zero when bond has no operations")
        void shouldReturnZeroWhenBondHasNoOperations() {
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.emptyList());

            BigDecimal profit = bondService.calculateProfit(bond1);

            assertEquals(BigDecimal.ZERO, profit);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
        }
    }

    @Nested
    @DisplayName("Get Total Invested Value Tests")
    class GetTotalInvestedValueTests {
        @Test
        @DisplayName("Should calculate total invested value for all non-archived bonds")
        void shouldCalculateTotalInvestedValueForAllNonArchivedBonds() {
            when(bondRepository.findByArchivedFalseOrderByNameAsc())
                    .thenReturn(Arrays.asList(bond1, bond2));

            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(buyOperation));
            when(bondOperationRepository.findByBondAndOperationTypeOrderByOperationDateAsc(
                            bond1, OperationType.BUY))
                    .thenReturn(Collections.singletonList(buyOperation));

            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond2))
                    .thenReturn(Collections.emptyList());

            BigDecimal totalInvested = bondService.getTotalInvestedValue();

            assertEquals(0, BigDecimal.valueOf(1000).compareTo(totalInvested));
            verify(bondRepository).findByArchivedFalseOrderByNameAsc();
        }

        @Test
        @DisplayName("Should return zero when no bonds exist")
        void shouldReturnZeroWhenNoBondsExist() {
            when(bondRepository.findByArchivedFalseOrderByNameAsc())
                    .thenReturn(Collections.emptyList());

            BigDecimal totalInvested = bondService.getTotalInvestedValue();

            assertEquals(BigDecimal.ZERO, totalInvested);
            verify(bondRepository).findByArchivedFalseOrderByNameAsc();
        }
    }

    @Nested
    @DisplayName("Add Bond Tests")
    class AddBondTests {
        @Test
        @DisplayName("Should add bond successfully")
        void shouldAddBondSuccessfully() {
            String name = "Tesouro IPCA+ 2035";
            String symbol = "IPCA2035";
            BondType type = BondType.TREASURY_POSTFIXED;
            String issuer = "Tesouro Nacional";
            LocalDateTime maturityDate = LocalDateTime.now().plusYears(10);
            InterestType interestType = InterestType.FLOATING;
            InterestIndex interestIndex = InterestIndex.IPCA;
            BigDecimal interestRate = BigDecimal.valueOf(6.0);

            when(bondRepository.existsBySymbol(symbol)).thenReturn(false);

            bondService.addBond(
                    name,
                    symbol,
                    type,
                    issuer,
                    maturityDate,
                    interestType,
                    interestIndex,
                    interestRate);

            verify(bondRepository).existsBySymbol(symbol);
            verify(bondRepository).save(any(Bond.class));
        }

        @Test
        @DisplayName("Should throw exception when bond with symbol already exists")
        void shouldThrowExceptionWhenBondWithSymbolAlreadyExists() {
            String symbol = "EXISTING";
            when(bondRepository.existsBySymbol(symbol)).thenReturn(true);

            assertThrows(
                    jakarta.persistence.EntityExistsException.class,
                    () ->
                            bondService.addBond(
                                    "Name",
                                    symbol,
                                    BondType.CDB,
                                    "Issuer",
                                    LocalDateTime.now().plusYears(1),
                                    InterestType.FIXED,
                                    null,
                                    BigDecimal.valueOf(10)));

            verify(bondRepository).existsBySymbol(symbol);
            verify(bondRepository, never()).save(any(Bond.class));
        }

        @Test
        @DisplayName("Should add bond with null symbol")
        void shouldAddBondWithNullSymbol() {
            bondService.addBond(
                    "Name",
                    null,
                    BondType.LCI,
                    "Issuer",
                    LocalDateTime.now().plusYears(2),
                    InterestType.FLOATING,
                    InterestIndex.CDI,
                    null);

            verify(bondRepository, never()).existsBySymbol(any());
            verify(bondRepository).save(any(Bond.class));
        }
    }

    @Nested
    @DisplayName("Get Bond By Id Tests")
    class GetBondByIdTests {
        @Test
        @DisplayName("Should return bond when found")
        void shouldReturnBondWhenFound() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));

            Bond result = bondService.getBondById(1);

            assertEquals(bond1, result);
            verify(bondRepository).findById(1);
        }

        @Test
        @DisplayName("Should throw exception when bond not found")
        void shouldThrowExceptionWhenBondNotFound() {
            when(bondRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> bondService.getBondById(999));
            verify(bondRepository).findById(999);
        }
    }

    @Nested
    @DisplayName("Update Bond Tests")
    class UpdateBondTests {
        @Test
        @DisplayName("Should update bond successfully")
        void shouldUpdateBondSuccessfully() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));

            String newName = "Updated Name";
            String newSymbol = "UPDATED";
            BondType newType = BondType.LCA;
            LocalDateTime newMaturityDate = LocalDateTime.now().plusYears(5);

            bondService.updateBond(
                    1,
                    newName,
                    newSymbol,
                    newType,
                    "New Issuer",
                    newMaturityDate,
                    InterestType.FIXED,
                    null,
                    BigDecimal.valueOf(8.5));

            verify(bondRepository).findById(1);
            verify(bondRepository).save(argThat(bond -> bond.getName().equals(newName)));
        }

        @Test
        @DisplayName("Should throw exception when updating non-existent bond")
        void shouldThrowExceptionWhenUpdatingNonExistentBond() {
            when(bondRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            bondService.updateBond(
                                    999,
                                    "Name",
                                    "SYM",
                                    BondType.CDB,
                                    "Issuer",
                                    LocalDateTime.now(),
                                    InterestType.FIXED,
                                    null,
                                    null));

            verify(bondRepository).findById(999);
            verify(bondRepository, never()).save(any(Bond.class));
        }
    }

    @Nested
    @DisplayName("Get Operations Tests")
    class GetOperationsTests {
        @Test
        @DisplayName("Should return all operations")
        void shouldReturnAllOperations() {
            List<BondOperation> operations = Arrays.asList(buyOperation, sellOperation);
            when(bondOperationRepository.findAllByOrderByOperationDateDesc())
                    .thenReturn(operations);

            List<BondOperation> result = bondService.getAllOperations();

            assertEquals(2, result.size());
            verify(bondOperationRepository).findAllByOrderByOperationDateDesc();
        }

        @Test
        @DisplayName("Should return operations by bond")
        void shouldReturnOperationsByBond() {
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Arrays.asList(buyOperation, sellOperation));

            List<BondOperation> result = bondService.getOperationsByBond(bond1);

            assertEquals(2, result.size());
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
        }
    }

    @Nested
    @DisplayName("Get Total Current Value Tests")
    class GetTotalCurrentValueTests {
        @Test
        @DisplayName("Should calculate total current value correctly")
        void shouldCalculateTotalCurrentValueCorrectly() {
            BigDecimal marketPrice = BigDecimal.valueOf(105);
            when(bondRepository.findByArchivedFalseOrderByNameAsc())
                    .thenReturn(Arrays.asList(bond1, bond2));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(buyOperation));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond2))
                    .thenReturn(Collections.emptyList());

            BigDecimal totalValue = bondService.getTotalCurrentValue(marketPrice);

            // bond1 has 10 units * 105 = 1050
            assertEquals(0, BigDecimal.valueOf(1050).compareTo(totalValue));
            verify(bondRepository).findByArchivedFalseOrderByNameAsc();
        }

        @Test
        @DisplayName("Should return zero when no bonds exist")
        void shouldReturnZeroWhenNoBondsExist() {
            when(bondRepository.findByArchivedFalseOrderByNameAsc())
                    .thenReturn(Collections.emptyList());

            BigDecimal totalValue = bondService.getTotalCurrentValue(BigDecimal.valueOf(100));

            assertEquals(BigDecimal.ZERO, totalValue);
        }
    }

    @Nested
    @DisplayName("Get Total Interest Received Tests")
    class GetTotalInterestReceivedTests {
        @Test
        @DisplayName("Should calculate total interest received correctly")
        void shouldCalculateTotalInterestReceivedCorrectly() {
            BondOperation sellWithProfit =
                    BondOperation.builder()
                            .bond(bond1)
                            .operationType(OperationType.SELL)
                            .quantity(BigDecimal.valueOf(5))
                            .unitPrice(BigDecimal.valueOf(110))
                            .netProfit(BigDecimal.valueOf(50))
                            .walletTransaction(walletTransaction)
                            .build();

            when(bondRepository.findByArchivedFalseOrderByNameAsc())
                    .thenReturn(Collections.singletonList(bond1));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(sellWithProfit));

            BigDecimal totalInterest = bondService.getTotalInterestReceived();

            assertEquals(0, BigDecimal.valueOf(50).compareTo(totalInterest));
            verify(bondRepository).findByArchivedFalseOrderByNameAsc();
        }

        @Test
        @DisplayName("Should return zero when no operations with profit")
        void shouldReturnZeroWhenNoOperationsWithProfit() {
            when(bondRepository.findByArchivedFalseOrderByNameAsc())
                    .thenReturn(Collections.singletonList(bond1));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(buyOperation));

            BigDecimal totalInterest = bondService.getTotalInterestReceived();

            assertEquals(BigDecimal.ZERO, totalInterest);
        }
    }

    @Nested
    @DisplayName("Calculate Operation Profit Loss Tests")
    class CalculateOperationProfitLossTests {
        @Test
        @DisplayName("Should return zero for buy operation")
        void shouldReturnZeroForBuyOperation() {
            BigDecimal profitLoss = bondService.calculateOperationProfitLoss(buyOperation);

            assertEquals(BigDecimal.ZERO, profitLoss);
        }

        @Test
        @DisplayName("Should return net profit for sell operation")
        void shouldReturnNetProfitForSellOperation() {
            BondOperation sellWithProfit =
                    BondOperation.builder()
                            .bond(bond1)
                            .operationType(OperationType.SELL)
                            .quantity(BigDecimal.valueOf(5))
                            .unitPrice(BigDecimal.valueOf(110))
                            .netProfit(BigDecimal.valueOf(47))
                            .walletTransaction(walletTransaction)
                            .build();

            BigDecimal profitLoss = bondService.calculateOperationProfitLoss(sellWithProfit);

            assertEquals(0, BigDecimal.valueOf(47).compareTo(profitLoss));
        }

        @Test
        @DisplayName("Should return zero when sell operation has null net profit")
        void shouldReturnZeroWhenSellOperationHasNullNetProfit() {
            BondOperation sellWithoutProfit =
                    BondOperation.builder()
                            .bond(bond1)
                            .operationType(OperationType.SELL)
                            .quantity(BigDecimal.valueOf(5))
                            .unitPrice(BigDecimal.valueOf(110))
                            .netProfit(null)
                            .walletTransaction(walletTransaction)
                            .build();

            BigDecimal profitLoss = bondService.calculateOperationProfitLoss(sellWithoutProfit);

            assertEquals(BigDecimal.ZERO, profitLoss);
        }
    }

    @Nested
    @DisplayName("Add Operation Tests")
    class AddOperationTests {
        private Category category;
        private LocalDate operationDate;

        @BeforeEach
        void setUp() {
            category = Category.builder().name("Investment").build();
            operationDate = LocalDate.now();
        }

        @Test
        @DisplayName("Should add buy operation successfully")
        void shouldAddBuyOperationSuccessfully() {
            Integer walletId = 1;
            BigDecimal quantity = BigDecimal.valueOf(10);
            BigDecimal unitPrice = BigDecimal.valueOf(100);
            BigDecimal fees = BigDecimal.valueOf(5);
            BigDecimal taxes = BigDecimal.valueOf(2);
            Integer transactionId = 100;

            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));
            when(walletTransactionService.addExpense(
                            eq(walletId),
                            eq(category),
                            any(LocalDateTime.class),
                            any(BigDecimal.class),
                            anyString(),
                            eq(TransactionStatus.CONFIRMED)))
                    .thenReturn(transactionId);
            when(walletTransactionService.getTransactionById(transactionId))
                    .thenReturn(walletTransaction);

            bondService.addOperation(
                    1,
                    walletId,
                    OperationType.BUY,
                    quantity,
                    unitPrice,
                    operationDate,
                    fees,
                    taxes,
                    null,
                    category,
                    "Buy operation",
                    TransactionStatus.CONFIRMED);

            verify(bondRepository).findById(1);
            verify(walletTransactionService)
                    .addExpense(
                            eq(walletId),
                            eq(category),
                            any(LocalDateTime.class),
                            eq(BigDecimal.valueOf(1007)), // 100*10 + 5 + 2
                            eq("Buy operation"),
                            eq(TransactionStatus.CONFIRMED));
            verify(bondOperationRepository).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should add sell operation successfully")
        void shouldAddSellOperationSuccessfully() {
            Integer walletId = 1;
            BigDecimal quantity = BigDecimal.valueOf(5);
            BigDecimal unitPrice = BigDecimal.valueOf(110);
            BigDecimal netProfit = BigDecimal.valueOf(50);
            Integer transactionId = 101;

            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(buyOperation));
            when(walletTransactionService.addIncome(
                            eq(walletId),
                            eq(category),
                            any(LocalDateTime.class),
                            any(BigDecimal.class),
                            anyString(),
                            eq(TransactionStatus.CONFIRMED)))
                    .thenReturn(transactionId);
            when(walletTransactionService.getTransactionById(transactionId))
                    .thenReturn(walletTransaction);

            bondService.addOperation(
                    1,
                    walletId,
                    OperationType.SELL,
                    quantity,
                    unitPrice,
                    operationDate,
                    null,
                    null,
                    netProfit,
                    category,
                    "Sell operation",
                    TransactionStatus.CONFIRMED);

            verify(bondRepository).findById(1);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
            verify(walletTransactionService)
                    .addIncome(
                            eq(walletId),
                            eq(category),
                            any(LocalDateTime.class),
                            eq(BigDecimal.valueOf(600)), // 110*5 + 50
                            eq("Sell operation"),
                            eq(TransactionStatus.CONFIRMED));
            verify(bondOperationRepository).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when bond not found")
        void shouldThrowExceptionWhenBondNotFound() {
            when(bondRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            bondService.addOperation(
                                    999,
                                    1,
                                    OperationType.BUY,
                                    BigDecimal.valueOf(10),
                                    BigDecimal.valueOf(100),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondRepository).findById(999);
            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when quantity is zero")
        void shouldThrowExceptionWhenQuantityIsZero() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.addOperation(
                                    1,
                                    1,
                                    OperationType.BUY,
                                    BigDecimal.ZERO,
                                    BigDecimal.valueOf(100),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when quantity is negative")
        void shouldThrowExceptionWhenQuantityIsNegative() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.addOperation(
                                    1,
                                    1,
                                    OperationType.BUY,
                                    BigDecimal.valueOf(-5),
                                    BigDecimal.valueOf(100),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when unit price is zero")
        void shouldThrowExceptionWhenUnitPriceIsZero() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.addOperation(
                                    1,
                                    1,
                                    OperationType.BUY,
                                    BigDecimal.valueOf(10),
                                    BigDecimal.ZERO,
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when unit price is negative")
        void shouldThrowExceptionWhenUnitPriceIsNegative() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.addOperation(
                                    1,
                                    1,
                                    OperationType.BUY,
                                    BigDecimal.valueOf(10),
                                    BigDecimal.valueOf(-100),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when selling more than available quantity")
        void shouldThrowExceptionWhenSellingMoreThanAvailableQuantity() {
            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(buyOperation));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.addOperation(
                                    1,
                                    1,
                                    OperationType.SELL,
                                    BigDecimal.valueOf(20), // More than available (10)
                                    BigDecimal.valueOf(110),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondRepository).findById(1);
            verify(bondOperationRepository).findByBondOrderByOperationDateAsc(bond1);
            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should handle null fees and taxes correctly")
        void shouldHandleNullFeesAndTaxesCorrectly() {
            Integer walletId = 1;
            BigDecimal quantity = BigDecimal.valueOf(10);
            BigDecimal unitPrice = BigDecimal.valueOf(100);
            Integer transactionId = 102;

            when(bondRepository.findById(1)).thenReturn(Optional.of(bond1));
            when(walletTransactionService.addExpense(
                            eq(walletId),
                            eq(category),
                            any(LocalDateTime.class),
                            any(BigDecimal.class),
                            anyString(),
                            eq(TransactionStatus.CONFIRMED)))
                    .thenReturn(transactionId);
            when(walletTransactionService.getTransactionById(transactionId))
                    .thenReturn(walletTransaction);

            bondService.addOperation(
                    1,
                    walletId,
                    OperationType.BUY,
                    quantity,
                    unitPrice,
                    operationDate,
                    null, // null fees
                    null, // null taxes
                    null,
                    category,
                    "Buy operation",
                    TransactionStatus.CONFIRMED);

            verify(walletTransactionService)
                    .addExpense(
                            eq(walletId),
                            eq(category),
                            any(LocalDateTime.class),
                            eq(BigDecimal.valueOf(1000)), // 100*10 + 0 + 0
                            eq("Buy operation"),
                            eq(TransactionStatus.CONFIRMED));
            verify(bondOperationRepository).save(any(BondOperation.class));
        }
    }

    @Nested
    @DisplayName("Update Operation Tests")
    class UpdateOperationTests {
        private Category category;
        private LocalDate operationDate;
        private BondOperation existingBuyOperation;
        private BondOperation existingSellOperation;

        @BeforeEach
        void setUp() {
            category = Category.builder().name("Investment").build();
            operationDate = LocalDate.now();

            existingBuyOperation =
                    BondOperation.builder()
                            .id(1)
                            .bond(bond1)
                            .operationType(OperationType.BUY)
                            .quantity(BigDecimal.valueOf(10))
                            .unitPrice(BigDecimal.valueOf(100))
                            .fees(BigDecimal.valueOf(5))
                            .taxes(BigDecimal.valueOf(2))
                            .walletTransaction(walletTransaction)
                            .build();

            existingSellOperation =
                    BondOperation.builder()
                            .id(2)
                            .bond(bond1)
                            .operationType(OperationType.SELL)
                            .quantity(BigDecimal.valueOf(5))
                            .unitPrice(BigDecimal.valueOf(110))
                            .netProfit(BigDecimal.valueOf(50))
                            .walletTransaction(walletTransaction)
                            .build();
        }

        @Test
        @DisplayName("Should update buy operation successfully")
        void shouldUpdateBuyOperationSuccessfully() {
            Integer walletId = 1;
            BigDecimal newQuantity = BigDecimal.valueOf(15);
            BigDecimal newUnitPrice = BigDecimal.valueOf(105);
            BigDecimal newFees = BigDecimal.valueOf(7);
            BigDecimal newTaxes = BigDecimal.valueOf(3);

            when(bondOperationRepository.findById(1)).thenReturn(Optional.of(existingBuyOperation));
            when(walletService.getWalletById(walletId)).thenReturn(wallet);

            bondService.updateOperation(
                    1,
                    walletId,
                    newQuantity,
                    newUnitPrice,
                    operationDate,
                    newFees,
                    newTaxes,
                    null,
                    category,
                    "Updated buy operation",
                    TransactionStatus.CONFIRMED);

            verify(bondOperationRepository).findById(1);
            verify(walletTransactionService).updateTransaction(any(WalletTransaction.class));
            verify(bondOperationRepository).save(existingBuyOperation);
            assertEquals(0, newQuantity.compareTo(existingBuyOperation.getQuantity()));
            assertEquals(0, newUnitPrice.compareTo(existingBuyOperation.getUnitPrice()));
        }

        @Test
        @DisplayName("Should update sell operation successfully")
        void shouldUpdateSellOperationSuccessfully() {
            Integer walletId = 1;
            BigDecimal newQuantity = BigDecimal.valueOf(3);
            BigDecimal newUnitPrice = BigDecimal.valueOf(115);
            BigDecimal newNetProfit = BigDecimal.valueOf(45);

            when(bondOperationRepository.findById(2))
                    .thenReturn(Optional.of(existingSellOperation));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(buyOperation));
            when(walletService.getWalletById(walletId)).thenReturn(wallet);

            bondService.updateOperation(
                    2,
                    walletId,
                    newQuantity,
                    newUnitPrice,
                    operationDate,
                    null,
                    null,
                    newNetProfit,
                    category,
                    "Updated sell operation",
                    TransactionStatus.CONFIRMED);

            verify(bondOperationRepository).findById(2);
            verify(walletTransactionService).updateTransaction(any(WalletTransaction.class));
            verify(bondOperationRepository).save(existingSellOperation);
            assertEquals(0, newQuantity.compareTo(existingSellOperation.getQuantity()));
        }

        @Test
        @DisplayName("Should throw exception when operation not found")
        void shouldThrowExceptionWhenOperationNotFound() {
            when(bondOperationRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            bondService.updateOperation(
                                    999,
                                    1,
                                    BigDecimal.valueOf(10),
                                    BigDecimal.valueOf(100),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository).findById(999);
            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when quantity is zero")
        void shouldThrowExceptionWhenQuantityIsZero() {
            when(bondOperationRepository.findById(1)).thenReturn(Optional.of(existingBuyOperation));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.updateOperation(
                                    1,
                                    1,
                                    BigDecimal.ZERO,
                                    BigDecimal.valueOf(100),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when quantity is negative")
        void shouldThrowExceptionWhenQuantityIsNegative() {
            when(bondOperationRepository.findById(1)).thenReturn(Optional.of(existingBuyOperation));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.updateOperation(
                                    1,
                                    1,
                                    BigDecimal.valueOf(-5),
                                    BigDecimal.valueOf(100),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when unit price is zero")
        void shouldThrowExceptionWhenUnitPriceIsZero() {
            when(bondOperationRepository.findById(1)).thenReturn(Optional.of(existingBuyOperation));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.updateOperation(
                                    1,
                                    1,
                                    BigDecimal.valueOf(10),
                                    BigDecimal.ZERO,
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when unit price is negative")
        void shouldThrowExceptionWhenUnitPriceIsNegative() {
            when(bondOperationRepository.findById(1)).thenReturn(Optional.of(existingBuyOperation));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.updateOperation(
                                    1,
                                    1,
                                    BigDecimal.valueOf(10),
                                    BigDecimal.valueOf(-100),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should throw exception when updating sell with insufficient quantity")
        void shouldThrowExceptionWhenUpdatingSellWithInsufficientQuantity() {
            when(bondOperationRepository.findById(2))
                    .thenReturn(Optional.of(existingSellOperation));
            when(bondOperationRepository.findByBondOrderByOperationDateAsc(bond1))
                    .thenReturn(Collections.singletonList(buyOperation));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            bondService.updateOperation(
                                    2,
                                    1,
                                    BigDecimal.valueOf(20),
                                    BigDecimal.valueOf(110),
                                    operationDate,
                                    null,
                                    null,
                                    null,
                                    category,
                                    "Test",
                                    TransactionStatus.CONFIRMED));

            verify(bondOperationRepository, never()).save(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should handle null fees and taxes correctly")
        void shouldHandleNullFeesAndTaxesCorrectly() {
            Integer walletId = 1;
            BigDecimal newQuantity = BigDecimal.valueOf(12);
            BigDecimal newUnitPrice = BigDecimal.valueOf(102);

            when(bondOperationRepository.findById(1)).thenReturn(Optional.of(existingBuyOperation));
            when(walletService.getWalletById(walletId)).thenReturn(wallet);

            bondService.updateOperation(
                    1,
                    walletId,
                    newQuantity,
                    newUnitPrice,
                    operationDate,
                    null,
                    null,
                    null,
                    category,
                    "Updated operation",
                    TransactionStatus.CONFIRMED);

            verify(walletTransactionService).updateTransaction(any(WalletTransaction.class));
            verify(bondOperationRepository).save(existingBuyOperation);
        }

        @Test
        @DisplayName("Should update wallet transaction correctly")
        void shouldUpdateWalletTransactionCorrectly() {
            Integer walletId = 1;
            BigDecimal newQuantity = BigDecimal.valueOf(8);
            BigDecimal newUnitPrice = BigDecimal.valueOf(98);

            when(bondOperationRepository.findById(1)).thenReturn(Optional.of(existingBuyOperation));
            when(walletService.getWalletById(walletId)).thenReturn(wallet);

            bondService.updateOperation(
                    1,
                    walletId,
                    newQuantity,
                    newUnitPrice,
                    operationDate,
                    BigDecimal.valueOf(4),
                    BigDecimal.valueOf(1),
                    null,
                    category,
                    "Updated operation",
                    TransactionStatus.CONFIRMED);

            ArgumentCaptor<WalletTransaction> captor =
                    ArgumentCaptor.forClass(WalletTransaction.class);
            verify(walletTransactionService).updateTransaction(captor.capture());

            WalletTransaction updatedTransaction = captor.getValue();
            assertEquals(wallet, updatedTransaction.getWallet());
            assertEquals(category, updatedTransaction.getCategory());
            assertEquals("Updated operation", updatedTransaction.getDescription());
        }
    }

    @Nested
    @DisplayName("Delete Operation Tests")
    class DeleteOperationTests {
        private BondOperation operationToDelete;

        @BeforeEach
        void setUp() {
            operationToDelete =
                    BondOperation.builder()
                            .id(1)
                            .bond(bond1)
                            .operationType(OperationType.BUY)
                            .quantity(BigDecimal.valueOf(10))
                            .unitPrice(BigDecimal.valueOf(100))
                            .walletTransaction(walletTransaction)
                            .build();
        }

        @Test
        @DisplayName("Should delete operation successfully")
        void shouldDeleteOperationSuccessfully() {
            when(bondOperationRepository.findById(1)).thenReturn(Optional.of(operationToDelete));

            bondService.deleteOperation(1);

            verify(bondOperationRepository).findById(1);
            verify(bondOperationRepository).delete(operationToDelete);
            verify(walletTransactionService).deleteTransaction(walletTransaction.getId());
        }

        @Test
        @DisplayName("Should throw exception when operation not found")
        void shouldThrowExceptionWhenOperationNotFound() {
            when(bondOperationRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> bondService.deleteOperation(999));

            verify(bondOperationRepository).findById(999);
            verify(bondOperationRepository, never()).delete(any(BondOperation.class));
        }

        @Test
        @DisplayName("Should delete operation without wallet transaction")
        void shouldDeleteOperationWithoutWalletTransaction() {
            BondOperation operationWithoutTransaction =
                    BondOperation.builder()
                            .id(2)
                            .bond(bond1)
                            .operationType(OperationType.BUY)
                            .quantity(BigDecimal.valueOf(5))
                            .unitPrice(BigDecimal.valueOf(100))
                            .walletTransaction(null)
                            .build();

            when(bondOperationRepository.findById(2))
                    .thenReturn(Optional.of(operationWithoutTransaction));

            bondService.deleteOperation(2);

            verify(bondOperationRepository).delete(operationWithoutTransaction);
            verify(walletTransactionService, never()).deleteTransaction(any());
        }

        @Test
        @DisplayName("Should delete operation and its wallet transaction")
        void shouldDeleteOperationAndItsWalletTransaction() {
            when(bondOperationRepository.findById(1)).thenReturn(Optional.of(operationToDelete));

            bondService.deleteOperation(1);

            verify(bondOperationRepository).delete(operationToDelete);
            verify(walletTransactionService).deleteTransaction(walletTransaction.getId());
        }

        @Test
        @DisplayName("Should handle deletion of sell operation")
        void shouldHandleDeletionOfSellOperation() {
            BondOperation sellOperation =
                    BondOperation.builder()
                            .id(3)
                            .bond(bond1)
                            .operationType(OperationType.SELL)
                            .quantity(BigDecimal.valueOf(5))
                            .unitPrice(BigDecimal.valueOf(110))
                            .walletTransaction(walletTransaction)
                            .build();

            when(bondOperationRepository.findById(3)).thenReturn(Optional.of(sellOperation));

            bondService.deleteOperation(3);

            verify(bondOperationRepository).delete(sellOperation);
            verify(walletTransactionService).deleteTransaction(walletTransaction.getId());
        }
    }
}
