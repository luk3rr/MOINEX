/*
 * Filename: NetWorthSnapshotServiceTest.java
 * Created on: January 24, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.moinex.model.NetWorthSnapshot;
import org.moinex.repository.NetWorthSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class NetWorthSnapshotServiceTest {

    @Mock private NetWorthSnapshotRepository netWorthSnapshotRepository;

    @InjectMocks private NetWorthSnapshotService netWorthSnapshotService;

    private NetWorthSnapshot snapshot;

    @BeforeEach
    void setUp() {
        snapshot =
                NetWorthSnapshot.builder()
                        .id(1)
                        .month(1)
                        .year(2025)
                        .assets(new BigDecimal("10000.00"))
                        .liabilities(new BigDecimal("2000.00"))
                        .netWorth(new BigDecimal("8000.00"))
                        .walletBalances(new BigDecimal("5000.00"))
                        .investments(new BigDecimal("5000.00"))
                        .creditCardDebt(new BigDecimal("1000.00"))
                        .negativeWalletBalances(new BigDecimal("1000.00"))
                        .calculatedAt(LocalDateTime.now().toString())
                        .build();
    }

    @Nested
    @DisplayName("Get Snapshot Tests")
    class GetSnapshotTests {
        @Test
        @DisplayName("Should get snapshot for a specific month and year")
        void getSnapshot_Success() {
            when(netWorthSnapshotRepository.findByMonthAndYear(1, 2025))
                    .thenReturn(Optional.of(snapshot));

            Optional<NetWorthSnapshot> result = netWorthSnapshotService.getSnapshot(1, 2025);

            assertTrue(result.isPresent());
            assertEquals(snapshot.getId(), result.get().getId());
            assertEquals(1, result.get().getMonth());
            assertEquals(2025, result.get().getYear());
        }

        @Test
        @DisplayName("Should return empty Optional when snapshot does not exist")
        void getSnapshot_NotFound() {
            when(netWorthSnapshotRepository.findByMonthAndYear(1, 2025))
                    .thenReturn(Optional.empty());

            Optional<NetWorthSnapshot> result = netWorthSnapshotService.getSnapshot(1, 2025);

            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Save Snapshot Tests")
    class SaveSnapshotTests {
        @Test
        @DisplayName("Should save new snapshot successfully")
        void saveSnapshot_NewSnapshot_Success() {
            when(netWorthSnapshotRepository.findByMonthAndYear(1, 2025))
                    .thenReturn(Optional.empty());
            when(netWorthSnapshotRepository.save(any(NetWorthSnapshot.class))).thenReturn(snapshot);

            NetWorthSnapshot result =
                    netWorthSnapshotService.saveSnapshot(
                            1,
                            2025,
                            new BigDecimal("10000.00"),
                            new BigDecimal("2000.00"),
                            new BigDecimal("8000.00"),
                            new BigDecimal("5000.00"),
                            new BigDecimal("5000.00"),
                            new BigDecimal("1000.00"),
                            new BigDecimal("1000.00"));

            assertNotNull(result);
            assertEquals(1, result.getMonth());
            assertEquals(2025, result.getYear());
            assertEquals(new BigDecimal("10000.00"), result.getAssets());
            assertEquals(new BigDecimal("2000.00"), result.getLiabilities());
            assertEquals(new BigDecimal("8000.00"), result.getNetWorth());

            ArgumentCaptor<NetWorthSnapshot> captor =
                    ArgumentCaptor.forClass(NetWorthSnapshot.class);
            verify(netWorthSnapshotRepository).save(captor.capture());
            assertEquals(1, captor.getValue().getMonth());
        }

        @Test
        @DisplayName("Should update existing snapshot successfully")
        void saveSnapshot_ExistingSnapshot_Success() {
            NetWorthSnapshot existingSnapshot =
                    NetWorthSnapshot.builder()
                            .id(1)
                            .month(1)
                            .year(2025)
                            .assets(new BigDecimal("9000.00"))
                            .liabilities(new BigDecimal("1500.00"))
                            .netWorth(new BigDecimal("7500.00"))
                            .walletBalances(new BigDecimal("4500.00"))
                            .investments(new BigDecimal("4500.00"))
                            .creditCardDebt(new BigDecimal("750.00"))
                            .negativeWalletBalances(new BigDecimal("750.00"))
                            .calculatedAt(LocalDateTime.now().toString())
                            .build();

            when(netWorthSnapshotRepository.findByMonthAndYear(1, 2025))
                    .thenReturn(Optional.of(existingSnapshot));
            when(netWorthSnapshotRepository.save(any(NetWorthSnapshot.class)))
                    .thenReturn(existingSnapshot);

            NetWorthSnapshot result =
                    netWorthSnapshotService.saveSnapshot(
                            1,
                            2025,
                            new BigDecimal("10000.00"),
                            new BigDecimal("2000.00"),
                            new BigDecimal("8000.00"),
                            new BigDecimal("5000.00"),
                            new BigDecimal("5000.00"),
                            new BigDecimal("1000.00"),
                            new BigDecimal("1000.00"));

            assertNotNull(result);
            assertEquals(1, result.getId());

            ArgumentCaptor<NetWorthSnapshot> captor =
                    ArgumentCaptor.forClass(NetWorthSnapshot.class);
            verify(netWorthSnapshotRepository).save(captor.capture());
            assertEquals(new BigDecimal("10000.00"), captor.getValue().getAssets());
            assertEquals(new BigDecimal("2000.00"), captor.getValue().getLiabilities());
        }

        @Test
        @DisplayName("Should save snapshot with correct calculated timestamp")
        void saveSnapshot_CalculatedAtTimestamp() {
            when(netWorthSnapshotRepository.findByMonthAndYear(1, 2025))
                    .thenReturn(Optional.empty());
            when(netWorthSnapshotRepository.save(any(NetWorthSnapshot.class))).thenReturn(snapshot);

            netWorthSnapshotService.saveSnapshot(
                    1,
                    2025,
                    new BigDecimal("10000.00"),
                    new BigDecimal("2000.00"),
                    new BigDecimal("8000.00"),
                    new BigDecimal("5000.00"),
                    new BigDecimal("5000.00"),
                    new BigDecimal("1000.00"),
                    new BigDecimal("1000.00"));

            ArgumentCaptor<NetWorthSnapshot> captor =
                    ArgumentCaptor.forClass(NetWorthSnapshot.class);
            verify(netWorthSnapshotRepository).save(captor.capture());
            assertNotNull(captor.getValue().getCalculatedAt());
        }
    }

    @Nested
    @DisplayName("Delete All Snapshots Tests")
    class DeleteAllSnapshotsTests {
        @Test
        @DisplayName("Should delete all snapshots successfully")
        void deleteAllSnapshots_Success() {
            netWorthSnapshotService.deleteAllSnapshots();

            verify(netWorthSnapshotRepository).deleteAll();
        }
    }
}
