package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
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
import org.moinex.model.investment.InvestmentTarget;
import org.moinex.repository.investment.InvestmentTargetRepository;
import org.moinex.util.enums.AssetType;
import org.moinex.util.enums.TickerType;

@ExtendWith(MockitoExtension.class)
class InvestmentTargetServiceTest {

    @Mock private InvestmentTargetRepository investmentTargetRepository;

    @InjectMocks private InvestmentTargetService investmentTargetService;

    private InvestmentTarget stockTarget;
    private InvestmentTarget bondTarget;
    private InvestmentTarget reitTarget;

    @BeforeEach
    void setUp() {
        stockTarget =
                InvestmentTarget.builder()
                        .id(1)
                        .assetType(AssetType.STOCK)
                        .targetPercentage(new BigDecimal("60"))
                        .isActive(true)
                        .build();

        bondTarget =
                InvestmentTarget.builder()
                        .id(2)
                        .assetType(AssetType.BOND)
                        .targetPercentage(new BigDecimal("30"))
                        .isActive(true)
                        .build();

        reitTarget =
                InvestmentTarget.builder()
                        .id(3)
                        .assetType(AssetType.REIT)
                        .targetPercentage(new BigDecimal("10"))
                        .isActive(true)
                        .build();
    }

    @Nested
    @DisplayName("Get All Active Targets Tests")
    class GetAllActiveTargetsTests {
        @Test
        @DisplayName("Should return all active targets")
        void getAllActiveTargets_ReturnsAllActiveTargets() {
            List<InvestmentTarget> targets = Arrays.asList(stockTarget, bondTarget, reitTarget);
            when(investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc())
                    .thenReturn(targets);

            List<InvestmentTarget> result = investmentTargetService.getAllActiveTargets();

            assertEquals(3, result.size());
            assertTrue(result.contains(stockTarget));
            assertTrue(result.contains(bondTarget));
            assertTrue(result.contains(reitTarget));
            verify(investmentTargetRepository).findAllByIsActiveTrueOrderByAssetTypeAsc();
        }

        @Test
        @DisplayName("Should return empty list when no active targets")
        void getAllActiveTargets_ReturnsEmptyListWhenNoActiveTargets() {
            when(investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc())
                    .thenReturn(Collections.emptyList());

            List<InvestmentTarget> result = investmentTargetService.getAllActiveTargets();

            assertTrue(result.isEmpty());
            verify(investmentTargetRepository).findAllByIsActiveTrueOrderByAssetTypeAsc();
        }

        @Test
        @DisplayName("Should return only active targets")
        void getAllActiveTargets_ReturnsOnlyActiveTargets() {
            InvestmentTarget inactiveTarget =
                    InvestmentTarget.builder()
                            .id(4)
                            .assetType(AssetType.ETF)
                            .targetPercentage(new BigDecimal("5"))
                            .isActive(false)
                            .build();

            List<InvestmentTarget> activeTargets = Arrays.asList(stockTarget, bondTarget);
            when(investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc())
                    .thenReturn(activeTargets);

            List<InvestmentTarget> result = investmentTargetService.getAllActiveTargets();

            assertEquals(2, result.size());
            assertFalse(result.contains(inactiveTarget));
            verify(investmentTargetRepository).findAllByIsActiveTrueOrderByAssetTypeAsc();
        }
    }

    @Nested
    @DisplayName("Get Target By Type Tests")
    class GetTargetByTypeTests {
        @Test
        @DisplayName("Should return target for given asset type")
        void getTargetByType_ReturnsTargetForGivenType() {
            when(investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.STOCK))
                    .thenReturn(Optional.of(stockTarget));

            InvestmentTarget result = investmentTargetService.getTargetByType(AssetType.STOCK);

            assertEquals(stockTarget, result);
            verify(investmentTargetRepository).findByAssetTypeAndIsActiveTrue(AssetType.STOCK);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when target not found")
        void getTargetByType_ThrowsExceptionWhenNotFound() {
            when(investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.ETF))
                    .thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> investmentTargetService.getTargetByType(AssetType.ETF));

            verify(investmentTargetRepository).findByAssetTypeAndIsActiveTrue(AssetType.ETF);
        }

        @Test
        @DisplayName("Should return bond target when requested")
        void getTargetByType_ReturnsBondTarget() {
            when(investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.BOND))
                    .thenReturn(Optional.of(bondTarget));

            InvestmentTarget result = investmentTargetService.getTargetByType(AssetType.BOND);

            assertEquals(bondTarget, result);
            assertEquals(AssetType.BOND, result.getAssetType());
        }

        @Test
        @DisplayName("Should return REIT target when requested")
        void getTargetByType_ReturnsReitTarget() {
            when(investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.REIT))
                    .thenReturn(Optional.of(reitTarget));

            InvestmentTarget result = investmentTargetService.getTargetByType(AssetType.REIT);

            assertEquals(reitTarget, result);
            assertEquals(AssetType.REIT, result.getAssetType());
        }
    }

    @Nested
    @DisplayName("Get Target By Ticker Type Tests")
    class GetTargetByTickerTypeTests {
        @Test
        @DisplayName("Should return target for given ticker type")
        void getTargetByTickerType_ReturnsTargetForGivenTickerType() {
            when(investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.STOCK))
                    .thenReturn(Optional.of(stockTarget));

            InvestmentTarget result =
                    investmentTargetService.getTargetByTickerType(TickerType.STOCK);

            assertEquals(stockTarget, result);
            verify(investmentTargetRepository).findByAssetTypeAndIsActiveTrue(AssetType.STOCK);
        }

        @Test
        @DisplayName("Should convert ticker type to asset type correctly")
        void getTargetByTickerType_ConvertsTickerTypeToAssetType() {
            when(investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.REIT))
                    .thenReturn(Optional.of(reitTarget));

            InvestmentTarget result =
                    investmentTargetService.getTargetByTickerType(TickerType.REIT);

            assertEquals(reitTarget, result);
            assertEquals(AssetType.REIT, result.getAssetType());
        }

        @Test
        @DisplayName("Should throw exception when ticker type not found")
        void getTargetByTickerType_ThrowsExceptionWhenNotFound() {
            when(investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.ETF))
                    .thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> investmentTargetService.getTargetByTickerType(TickerType.ETF));
        }
    }

    @Nested
    @DisplayName("Set Target Tests")
    class SetTargetTests {
        @Test
        @DisplayName("Should create new target when not exists")
        void setTarget_CreatesNewTargetWhenNotExists() {
            when(investmentTargetRepository.findByAssetType(AssetType.ETF))
                    .thenReturn(Optional.empty());
            when(investmentTargetRepository.save(any(InvestmentTarget.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            InvestmentTarget result =
                    investmentTargetService.setTarget(AssetType.ETF, new BigDecimal("15"));

            assertNotNull(result);
            assertEquals(AssetType.ETF, result.getAssetType());
            assertEquals(0, new BigDecimal("15").compareTo(result.getTargetPercentage()));
            assertTrue(result.isActive());
            verify(investmentTargetRepository).save(any(InvestmentTarget.class));
        }

        @Test
        @DisplayName("Should update existing target")
        void setTarget_UpdatesExistingTarget() {
            when(investmentTargetRepository.findByAssetType(AssetType.STOCK))
                    .thenReturn(Optional.of(stockTarget));
            when(investmentTargetRepository.save(any(InvestmentTarget.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            InvestmentTarget result =
                    investmentTargetService.setTarget(AssetType.STOCK, new BigDecimal("70"));

            assertEquals(0, new BigDecimal("70").compareTo(result.getTargetPercentage()));
            assertTrue(result.isActive());
            verify(investmentTargetRepository).save(stockTarget);
        }

        @Test
        @DisplayName("Should throw exception when percentage is negative")
        void setTarget_ThrowsExceptionWhenPercentageIsNegative() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> investmentTargetService.setTarget(AssetType.STOCK, new BigDecimal("-5")));

            verify(investmentTargetRepository, never()).save(any(InvestmentTarget.class));
        }

        @Test
        @DisplayName("Should throw exception when percentage is greater than 100")
        void setTarget_ThrowsExceptionWhenPercentageGreaterThan100() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            investmentTargetService.setTarget(
                                    AssetType.STOCK, new BigDecimal("101")));

            verify(investmentTargetRepository, never()).save(any(InvestmentTarget.class));
        }

        @Test
        @DisplayName("Should accept percentage of 0")
        void setTarget_AcceptsPercentageOfZero() {
            when(investmentTargetRepository.findByAssetType(AssetType.STOCK))
                    .thenReturn(Optional.of(stockTarget));
            when(investmentTargetRepository.save(any(InvestmentTarget.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            InvestmentTarget result =
                    investmentTargetService.setTarget(AssetType.STOCK, BigDecimal.ZERO);

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getTargetPercentage()));
            verify(investmentTargetRepository).save(stockTarget);
        }

        @Test
        @DisplayName("Should accept percentage of 100")
        void setTarget_AcceptsPercentageOf100() {
            when(investmentTargetRepository.findByAssetType(AssetType.STOCK))
                    .thenReturn(Optional.of(stockTarget));
            when(investmentTargetRepository.save(any(InvestmentTarget.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            InvestmentTarget result =
                    investmentTargetService.setTarget(AssetType.STOCK, new BigDecimal("100"));

            assertEquals(0, new BigDecimal("100").compareTo(result.getTargetPercentage()));
            verify(investmentTargetRepository).save(stockTarget);
        }

        @Test
        @DisplayName("Should set target as active when updating")
        void setTarget_SetsTargetAsActiveWhenUpdating() {
            InvestmentTarget inactiveTarget =
                    InvestmentTarget.builder()
                            .id(4)
                            .assetType(AssetType.ETF)
                            .targetPercentage(new BigDecimal("5"))
                            .isActive(false)
                            .build();

            when(investmentTargetRepository.findByAssetType(AssetType.ETF))
                    .thenReturn(Optional.of(inactiveTarget));
            when(investmentTargetRepository.save(any(InvestmentTarget.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            InvestmentTarget result =
                    investmentTargetService.setTarget(AssetType.ETF, new BigDecimal("20"));

            assertTrue(result.isActive());
            verify(investmentTargetRepository).save(inactiveTarget);
        }
    }

    @Nested
    @DisplayName("Delete Target Tests")
    class DeleteTargetTests {
        @Test
        @DisplayName("Should deactivate target when deleting")
        void deleteTarget_DeactivatesTarget() {
            when(investmentTargetRepository.findById(1)).thenReturn(Optional.of(stockTarget));

            investmentTargetService.deleteTarget(1);

            assertFalse(stockTarget.isActive());
            verify(investmentTargetRepository).save(stockTarget);
        }

        @Test
        @DisplayName("Should throw exception when target not found")
        void deleteTarget_ThrowsExceptionWhenNotFound() {
            when(investmentTargetRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class, () -> investmentTargetService.deleteTarget(999));

            verify(investmentTargetRepository, never()).save(any(InvestmentTarget.class));
        }

        @Test
        @DisplayName("Should save target after deactivation")
        void deleteTarget_SavesTargetAfterDeactivation() {
            when(investmentTargetRepository.findById(2)).thenReturn(Optional.of(bondTarget));

            investmentTargetService.deleteTarget(2);

            ArgumentCaptor<InvestmentTarget> captor =
                    ArgumentCaptor.forClass(InvestmentTarget.class);
            verify(investmentTargetRepository).save(captor.capture());

            InvestmentTarget savedTarget = captor.getValue();
            assertFalse(savedTarget.isActive());
            assertEquals(bondTarget, savedTarget);
        }
    }

    @Nested
    @DisplayName("Validate Total Percentage Tests")
    class ValidateTotalPercentageTests {
        @Test
        @DisplayName("Should return true when total percentage is 100")
        void validateTotalPercentage_ReturnsTrueWhenTotalIs100() {
            List<InvestmentTarget> targets = Arrays.asList(stockTarget, bondTarget, reitTarget);
            when(investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc())
                    .thenReturn(targets);

            boolean result = investmentTargetService.validateTotalPercentage();

            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when total percentage is less than 100")
        void validateTotalPercentage_ReturnsFalseWhenTotalIsLessThan100() {
            stockTarget.setTargetPercentage(new BigDecimal("50"));
            bondTarget.setTargetPercentage(new BigDecimal("30"));

            List<InvestmentTarget> targets = Arrays.asList(stockTarget, bondTarget);
            when(investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc())
                    .thenReturn(targets);

            boolean result = investmentTargetService.validateTotalPercentage();

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when total percentage is greater than 100")
        void validateTotalPercentage_ReturnsFalseWhenTotalIsGreaterThan100() {
            stockTarget.setTargetPercentage(new BigDecimal("60"));
            bondTarget.setTargetPercentage(new BigDecimal("50"));

            List<InvestmentTarget> targets = Arrays.asList(stockTarget, bondTarget);
            when(investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc())
                    .thenReturn(targets);

            boolean result = investmentTargetService.validateTotalPercentage();

            assertFalse(result);
        }

        @Test
        @DisplayName("Should return false when no targets exist")
        void validateTotalPercentage_ReturnsFalseWhenNoTargets() {
            when(investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc())
                    .thenReturn(Collections.emptyList());

            boolean result = investmentTargetService.validateTotalPercentage();

            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle decimal percentages correctly")
        void validateTotalPercentage_HandlesDecimalPercentages() {
            stockTarget.setTargetPercentage(new BigDecimal("60.5"));
            bondTarget.setTargetPercentage(new BigDecimal("30.5"));
            reitTarget.setTargetPercentage(new BigDecimal("9.0"));

            List<InvestmentTarget> targets = Arrays.asList(stockTarget, bondTarget, reitTarget);
            when(investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc())
                    .thenReturn(targets);

            boolean result = investmentTargetService.validateTotalPercentage();

            assertTrue(result);
        }
    }
}
