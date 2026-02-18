/*
 * Filename: InvestmentPerformanceSnapshotServiceTest.java
 * Created on: February 18, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.moinex.model.InvestmentPerformanceSnapshot;
import org.moinex.repository.InvestmentPerformanceSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class InvestmentPerformanceSnapshotServiceTest {

    @Mock private InvestmentPerformanceSnapshotRepository repository;

    @InjectMocks private InvestmentPerformanceSnapshotService snapshotService;

    private InvestmentPerformanceSnapshot snapshot;

    @BeforeEach
    void setUp() {
        snapshot =
                InvestmentPerformanceSnapshot.builder()
                        .id(1)
                        .month(1)
                        .year(2026)
                        .investedValue(new BigDecimal("10000.00"))
                        .portfolioValue(new BigDecimal("10500.00"))
                        .accumulatedCapitalGains(new BigDecimal("500.00"))
                        .monthlyCapitalGains(new BigDecimal("100.00"))
                        .calculatedAt(LocalDateTime.now().toString())
                        .build();
    }

    @Nested
    @DisplayName("Get Snapshot Tests")
    class GetSnapshotTests {
        @Test
        @DisplayName("Should get snapshot for a specific month and year")
        void getSnapshot_Success() {
            when(repository.findByMonthAndYear(1, 2026)).thenReturn(Optional.of(snapshot));

            Optional<InvestmentPerformanceSnapshot> result = snapshotService.getSnapshot(1, 2026);

            assertTrue(result.isPresent());
            assertEquals(snapshot.getId(), result.get().getId());
            assertEquals(1, result.get().getMonth());
            assertEquals(2026, result.get().getYear());
            verify(repository).findByMonthAndYear(1, 2026);
        }

        @Test
        @DisplayName("Should return empty Optional when snapshot does not exist")
        void getSnapshot_NotFound() {
            when(repository.findByMonthAndYear(1, 2026)).thenReturn(Optional.empty());

            Optional<InvestmentPerformanceSnapshot> result = snapshotService.getSnapshot(1, 2026);

            assertFalse(result.isPresent());
            verify(repository).findByMonthAndYear(1, 2026);
        }
    }

    @Nested
    @DisplayName("Get All Snapshots Tests")
    class GetAllSnapshotsTests {
        @Test
        @DisplayName("Should return all snapshots ordered by date")
        void getAllSnapshots_Success() {
            InvestmentPerformanceSnapshot snapshot2 =
                    InvestmentPerformanceSnapshot.builder()
                            .id(2)
                            .month(2)
                            .year(2026)
                            .investedValue(new BigDecimal("11000.00"))
                            .portfolioValue(new BigDecimal("11600.00"))
                            .accumulatedCapitalGains(new BigDecimal("600.00"))
                            .monthlyCapitalGains(new BigDecimal("100.00"))
                            .build();

            when(repository.findAllOrderedByDate()).thenReturn(List.of(snapshot, snapshot2));

            List<InvestmentPerformanceSnapshot> result = snapshotService.getAllSnapshots();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(repository).findAllOrderedByDate();
        }

        @Test
        @DisplayName("Should return empty list when no snapshots exist")
        void getAllSnapshots_Empty() {
            when(repository.findAllOrderedByDate()).thenReturn(Collections.emptyList());

            List<InvestmentPerformanceSnapshot> result = snapshotService.getAllSnapshots();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(repository).findAllOrderedByDate();
        }
    }

    @Nested
    @DisplayName("Save Snapshot Tests")
    class SaveSnapshotTests {
        @Test
        @DisplayName("Should save new snapshot successfully")
        void saveSnapshot_NewSnapshot_Success() {
            when(repository.findByMonthAndYear(1, 2026)).thenReturn(Optional.empty());
            when(repository.save(any(InvestmentPerformanceSnapshot.class))).thenReturn(snapshot);

            InvestmentPerformanceSnapshot result =
                    snapshotService.saveSnapshot(
                            1,
                            2026,
                            new BigDecimal("10000.00"),
                            new BigDecimal("10500.00"),
                            new BigDecimal("500.00"),
                            new BigDecimal("100.00"));

            assertNotNull(result);
            assertEquals(1, result.getMonth());
            assertEquals(2026, result.getYear());
            assertEquals(new BigDecimal("10000.00"), result.getInvestedValue());
            assertEquals(new BigDecimal("10500.00"), result.getPortfolioValue());
            assertEquals(new BigDecimal("500.00"), result.getAccumulatedCapitalGains());
            assertEquals(new BigDecimal("100.00"), result.getMonthlyCapitalGains());

            ArgumentCaptor<InvestmentPerformanceSnapshot> captor =
                    ArgumentCaptor.forClass(InvestmentPerformanceSnapshot.class);
            verify(repository).save(captor.capture());

            InvestmentPerformanceSnapshot saved = captor.getValue();
            assertEquals(1, saved.getMonth());
            assertEquals(2026, saved.getYear());
        }

        @Test
        @DisplayName("Should update existing snapshot successfully")
        void saveSnapshot_ExistingSnapshot_Update() {
            when(repository.findByMonthAndYear(1, 2026)).thenReturn(Optional.of(snapshot));
            when(repository.save(any(InvestmentPerformanceSnapshot.class))).thenReturn(snapshot);

            BigDecimal newInvestedValue = new BigDecimal("12000.00");
            BigDecimal newPortfolioValue = new BigDecimal("12800.00");
            BigDecimal newAccGains = new BigDecimal("800.00");
            BigDecimal newMonthGains = new BigDecimal("300.00");

            InvestmentPerformanceSnapshot result =
                    snapshotService.saveSnapshot(
                            1,
                            2026,
                            newInvestedValue,
                            newPortfolioValue,
                            newAccGains,
                            newMonthGains);

            assertNotNull(result);
            verify(repository).save(snapshot);
            assertEquals(newInvestedValue, snapshot.getInvestedValue());
            assertEquals(newPortfolioValue, snapshot.getPortfolioValue());
            assertEquals(newAccGains, snapshot.getAccumulatedCapitalGains());
            assertEquals(newMonthGains, snapshot.getMonthlyCapitalGains());
        }
    }

    @Nested
    @DisplayName("Delete All Snapshots Tests")
    class DeleteAllSnapshotsTests {
        @Test
        @DisplayName("Should delete all snapshots successfully")
        void deleteAllSnapshots_Success() {
            doNothing().when(repository).deleteAll();

            snapshotService.deleteAllSnapshots();

            verify(repository).deleteAll();
        }
    }

    @Nested
    @DisplayName("Has Snapshots Tests")
    class HasSnapshotsTests {
        @Test
        @DisplayName("Should return true when snapshots exist")
        void hasSnapshots_True() {
            when(repository.count()).thenReturn(5L);

            boolean result = snapshotService.hasSnapshots();

            assertTrue(result);
            verify(repository).count();
        }

        @Test
        @DisplayName("Should return false when no snapshots exist")
        void hasSnapshots_False() {
            when(repository.count()).thenReturn(0L);

            boolean result = snapshotService.hasSnapshots();

            assertFalse(result);
            verify(repository).count();
        }
    }
}
