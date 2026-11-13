/*
 * Filename: WalletServiceBulkImportTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.wallet

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.moinex.common.ClockProvider
import org.moinex.common.util.UIUtils
import org.moinex.factory.CategoryFactory
import org.moinex.factory.wallet.TransferFactory
import org.moinex.factory.wallet.WalletFactory
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import java.math.BigDecimal
import java.util.Optional

class WalletServiceBulkImportTest :
    BehaviorSpec({
        val walletRepository = mockk<WalletRepository>()
        val transfersRepository = mockk<TransferRepository>()
        val preferencesService = mockk<PreferencesService>(relaxed = true)
        UIUtils(preferencesService)

        val service =
            WalletService(
                walletRepository,
                transfersRepository,
                mockk<WalletTransactionRepository>(),
                mockk<WalletTypeRepository>(),
                mockk<NotificationService>(relaxed = true),
                preferencesService,
                ClockProvider(),
            )

        Given("a transfer larger than the sender balance") {
            val sender = WalletFactory.create(id = 1, balance = BigDecimal("10.00"))
            val receiver = WalletFactory.create(id = 2, balance = BigDecimal("0.00"))
            val transfer =
                TransferFactory.create(
                    senderWallet = sender,
                    receiverWallet = receiver,
                    amount = BigDecimal("50.00"),
                    category = CategoryFactory.create(id = 1),
                )
            every { walletRepository.findById(1) } returns Optional.of(sender)
            every { walletRepository.findById(2) } returns Optional.of(receiver)
            every { walletRepository.getAllocatedBalanceByMasterWallet(any()) } returns BigDecimal.ZERO
            every { transfersRepository.save(any()) } returns
                TransferFactory.create(id = 9, senderWallet = sender, receiverWallet = receiver)

            When("the balance rules are enforced (default)") {
                Then("it is refused as before") {
                    shouldThrow<IllegalStateException> { service.createTransfer(transfer) }
                }
            }

            When("the balance rules are not enforced") {
                val id = service.createTransfer(transfer, enforceBalanceRules = false)

                Then("the transfer is created and both balances move") {
                    id shouldBe 9
                    sender.balance shouldBe BigDecimal("-40.00")
                    receiver.balance shouldBe BigDecimal("50.00")
                }
            }
        }
    })
