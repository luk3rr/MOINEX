package org.moinex.service.matching

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.service.PreferencesService
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.system.measureTimeMillis

class TransactionMatchingPerformanceTest :
    BehaviorSpec({
        Given("10000 stored transactions and a statement of 200 lines") {
            val walletTransactionRepository = mockk<WalletTransactionRepository>()
            val transferRepository = mockk<TransferRepository>()
            val creditCardDebtRepository = mockk<CreditCardDebtRepository>()
            val preferencesService = mockk<PreferencesService>()
            every { preferencesService.translate(any()) } answers { firstArg() }

            val wallet = WalletFactory.create(id = 1)
            val start = LocalDate.of(2026, 1, 1)
            val stored =
                (1..10_000).map { i ->
                    WalletTransactionFactory.create(
                        id = i,
                        amount = BigDecimal(1 + i % 500).setScale(2),
                        date = start.plusDays((i % 300).toLong()).atTime(12, 0),
                        description = "Store $i",
                        status = WalletTransactionStatus.CONFIRMED,
                        wallet = wallet,
                    )
                }
            every { walletTransactionRepository.findTransactionsBetweenDates(any(), any()) } returns stored
            every { transferRepository.findBetweenDates(any(), any()) } returns emptyList()
            every { creditCardDebtRepository.findByDateBetween(any(), any()) } returns emptyList()

            val service =
                TransactionMatchingService(
                    walletTransactionRepository,
                    transferRepository,
                    creditCardDebtRepository,
                    preferencesService,
                )
            val queries =
                (1..200).map { i ->
                    MatchQuery(BigDecimal(1 + i % 500).setScale(2), start.plusDays((i % 300).toLong()), walletId = 1)
                }

            When("matching all lines in one call, after the JVM warmed up on a small batch") {
                service.findMatches(queries.take(10))
                var result: List<List<Match>> = emptyList()
                val elapsed = measureTimeMillis { result = service.findMatches(queries) }

                Then("every line finds a candidate in under one second") {
                    result.size shouldBe 200
                    result.all { it.isNotEmpty() } shouldBe true
                    elapsed shouldBeLessThan 1000L
                }
            }
        }
    })
