package org.moinex.service.investment

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.moinex.common.ClockProvider
import org.moinex.factory.CategoryFactory
import org.moinex.factory.investment.BondFactory
import org.moinex.factory.investment.BondOperationFactory
import org.moinex.factory.investment.BondOperationWalletTransactionDTOFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.factory.wallet.WalletTransactionFactory
import org.moinex.model.enums.BondType
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class BondServiceUpdateBondOperationWalletBalanceTest :
    BehaviorSpec({
        val bondRepository = mockk<BondRepository>()
        val bondOperationRepository = mockk<BondOperationRepository>()
        val bondInterestCalculationService = mockk<BondInterestCalculationService>()
        val bondNotificationService = mockk<NotificationService>(relaxed = true)
        val bondPreferencesService = mockk<PreferencesService>(relaxed = true)

        val walletRepository = mockk<WalletRepository>()
        val transfersRepository = mockk<TransferRepository>()
        val walletTransactionRepository = mockk<WalletTransactionRepository>()
        val walletTypeRepository = mockk<WalletTypeRepository>()
        val walletNotificationService = mockk<NotificationService>(relaxed = true)
        val walletPreferencesService = mockk<PreferencesService>(relaxed = true)
        val clockProvider = ClockProvider()

        val walletService =
            WalletService(
                walletRepository,
                transfersRepository,
                walletTransactionRepository,
                walletTypeRepository,
                walletNotificationService,
                walletPreferencesService,
                clockProvider,
            )

        val bondService =
            BondService(
                bondRepository,
                bondOperationRepository,
                walletService,
                bondInterestCalculationService,
                bondNotificationService,
                bondPreferencesService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a confirmed SELL bond operation whose sale amount is edited") {
            When("updating the bond operation with a higher unit price") {
                val wallet = WalletFactory.create(id = 1, balance = BigDecimal("1000.00"))
                val category = CategoryFactory.create(id = 1)
                val bond = BondFactory.create(id = 1, name = "Test Bond", type = BondType.CDB)

                val walletTransaction =
                    WalletTransactionFactory.create(
                        id = 100,
                        wallet = wallet,
                        category = category,
                        type = WalletTransactionType.INCOME,
                        amount = BigDecimal("750.00"),
                        status = WalletTransactionStatus.CONFIRMED,
                    )

                val originalBondOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("10"),
                        unitPrice = BigDecimal("75.00"),
                        netProfit = BigDecimal.ZERO,
                        walletTransaction = walletTransaction,
                    )
                val updatedBondOperation =
                    BondOperationFactory.create(
                        id = 1,
                        bond = bond,
                        operationType = OperationType.SELL,
                        quantity = BigDecimal("10"),
                        unitPrice = BigDecimal("90.00"),
                        netProfit = BigDecimal.ZERO,
                        walletTransaction = walletTransaction,
                    )
                val dto =
                    BondOperationWalletTransactionDTOFactory.create(
                        wallet = wallet,
                        category = category,
                        date = LocalDateTime.now(),
                        status = WalletTransactionStatus.CONFIRMED,
                    )

                val buyOperation =
                    BondOperationFactory.create(
                        id = 2,
                        bond = bond,
                        operationType = OperationType.BUY,
                        quantity = BigDecimal("20"),
                        unitPrice = BigDecimal("50.00"),
                    )

                every { bondOperationRepository.findById(1) } returns Optional.of(originalBondOperation)
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(buyOperation, originalBondOperation)
                every { walletTransactionRepository.findById(100) } returns Optional.of(walletTransaction)

                bondService.updateBondOperation(updatedBondOperation, dto)

                Then("the wallet transaction amount should reflect the new sale value") {
                    walletTransaction.amount shouldBe BigDecimal("900.00")
                }

                Then("the wallet balance should be adjusted by the sale value difference") {
                    wallet.balance shouldBe BigDecimal("1150.00")
                }
            }
        }
    })
