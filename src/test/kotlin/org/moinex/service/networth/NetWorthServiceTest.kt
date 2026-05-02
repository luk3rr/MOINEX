package org.moinex.service.networth

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.common.ClockProvider
import org.moinex.factory.NetWorthSnapshotFactory
import org.moinex.repository.NetWorthSnapshotRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth

class NetWorthServiceTest :
    BehaviorSpec({
        val netWorthSnapshotRepository = mockk<NetWorthSnapshotRepository>()
        val clockProvider = ClockProvider()
        val service = NetWorthService(netWorthSnapshotRepository, clockProvider)

        afterContainer { clearAllMocks(answers = true) }

        Given("a system with existing snapshots to update") {
            val existingSnapshot =
                NetWorthSnapshotFactory.create(
                    assets = BigDecimal("3000.00"),
                    liabilities = BigDecimal("1000.00"),
                    netWorth = BigDecimal("2000.00"),
                    walletBalances = BigDecimal("3000.00"),
                    creditCardDebt = BigDecimal("1000.00"),
                    calculatedAt = LocalDateTime.now().minusDays(1),
                )

            val newSnapshot =
                NetWorthSnapshotFactory.create(
                    assets = BigDecimal("5000.00"),
                    liabilities = BigDecimal("1500.00"),
                    netWorth = BigDecimal("3500.00"),
                    walletBalances = BigDecimal("5000.00"),
                    creditCardDebt = BigDecimal("1500.00"),
                )

            When("saving a snapshot that already exists") {
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns existingSnapshot
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                val result = service.save(newSnapshot)

                Then("should update existing snapshot") {
                    verify { netWorthSnapshotRepository.findByReferenceMonth(YearMonth.now()) }
                    verify { netWorthSnapshotRepository.save(any()) }
                }

                Then("should preserve the existing snapshot instance") {
                    result shouldBe existingSnapshot
                }

                Then("should update all fields") {
                    result.assets shouldBe BigDecimal("5000.00")
                    result.liabilities shouldBe BigDecimal("1500.00")
                    result.netWorth shouldBe BigDecimal("3500.00")
                    result.walletBalances shouldBe BigDecimal("5000.00")
                    result.creditCardDebt shouldBe BigDecimal("1500.00")
                }
            }
        }

        Given("a system with no existing snapshots") {
            val newSnapshot = NetWorthSnapshotFactory.create()

            When("saving a new snapshot") {
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                val result = service.save(newSnapshot)

                Then("should save new snapshot") {
                    verify { netWorthSnapshotRepository.findByReferenceMonth(YearMonth.now()) }
                    verify { netWorthSnapshotRepository.save(newSnapshot) }
                }

                Then("should return the saved snapshot") {
                    result shouldBe newSnapshot
                }
            }
        }

        Given("a batch of snapshots to save") {
            val snapshots =
                listOf(
                    NetWorthSnapshotFactory.create(
                        referenceMonth = YearMonth.of(2025, 1),
                        assets = BigDecimal("1000.00"),
                        liabilities = BigDecimal("500.00"),
                        netWorth = BigDecimal("500.00"),
                        walletBalances = BigDecimal("1000.00"),
                        creditCardDebt = BigDecimal("500.00"),
                    ),
                    NetWorthSnapshotFactory.create(
                        referenceMonth = YearMonth.of(2025, 2),
                        assets = BigDecimal("2000.00"),
                        liabilities = BigDecimal("800.00"),
                        netWorth = BigDecimal("1200.00"),
                        walletBalances = BigDecimal("2000.00"),
                        creditCardDebt = BigDecimal("800.00"),
                    ),
                )

            When("saving batch") {
                every { netWorthSnapshotRepository.findByReferenceMonth(any()) } returns null
                every { netWorthSnapshotRepository.save(any()) } returnsArgument 0

                service.saveBatch(snapshots)

                Then("should save each snapshot individually") {
                    verify(exactly = 2) { netWorthSnapshotRepository.save(any()) }
                }

                Then("should check for existing snapshots") {
                    verify(exactly = 2) { netWorthSnapshotRepository.findByReferenceMonth(any()) }
                }
            }
        }

        Given("a date range for deletion") {
            val startMonth = YearMonth.of(2025, 1)
            val endMonth = YearMonth.of(2025, 12)

            When("deleting snapshots outside range") {
                every { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(any(), any()) } returns Unit

                service.deleteSnapshotsOutsideRange(startMonth, endMonth)

                Then("should call repository delete method") {
                    verify { netWorthSnapshotRepository.deleteSnapshotsOutsideRange(startMonth, endMonth) }
                }
            }
        }

        Given("a reference month to find") {
            val referenceMonth = YearMonth.now()
            val snapshot = NetWorthSnapshotFactory.create(referenceMonth = referenceMonth)

            When("finding by reference month") {
                every { netWorthSnapshotRepository.findByReferenceMonth(referenceMonth) } returns snapshot

                val result = service.findByReferenceMonth(referenceMonth)

                Then("should return the snapshot") {
                    result shouldBe snapshot
                }

                Then("should call repository find method") {
                    verify { netWorthSnapshotRepository.findByReferenceMonth(referenceMonth) }
                }
            }
        }
    })
