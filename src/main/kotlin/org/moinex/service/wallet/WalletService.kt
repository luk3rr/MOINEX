package org.moinex.service.wallet

import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isZero
import org.moinex.common.util.UIUtils
import org.moinex.model.enums.NotificationType
import org.moinex.model.enums.WalletTransactionStatus
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.model.wallettransaction.WalletType
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.text.MessageFormat
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth

@Service
class WalletService(
    private val walletRepository: WalletRepository,
    private val transfersRepository: TransferRepository,
    private val walletTransactionRepository: WalletTransactionRepository,
    private val walletTypeRepository: WalletTypeRepository,
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
) {
    private val logger = LoggerFactory.getLogger(WalletService::class.java)

    @Transactional
    fun createWallet(wallet: Wallet): Int {
        check(!walletRepository.existsByName(wallet.name)) {
            "Wallet with name '${wallet.name}' already exists"
        }

        val newWallet = walletRepository.save(wallet)

        logger.info("$newWallet created successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_CREATED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_CREATED_MESSAGE),
            relatedEntityId = newWallet.id!!,
        )

        return newWallet.id!!
    }

    @Transactional
    fun deleteWallet(id: Int) {
        val walletFromDatabase = walletRepository.findByIdOrThrow(id)

        check(getWalletTransactionAndTransferCountByWallet(id) == 0) {
            "$walletFromDatabase has transactions and cannot be deleted. " +
                "Remove the transactions first or archive the wallet"
        }

        if (walletFromDatabase.isMaster()) {
            removeAllVirtualWalletsFromMasterWallet(walletFromDatabase.id!!)
        }

        walletRepository.delete(walletFromDatabase)

        logger.info("Wallet with id {} was permanently deleted", id)

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_DELETED_TITLE),
            message =
                MessageFormat.format(
                    preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_DELETED_MESSAGE,
                    ),
                    walletFromDatabase.name,
                ),
            relatedEntityId = walletFromDatabase.id!!,
        )
    }

    @Transactional
    fun archiveWallet(id: Int) {
        val walletFromDatabase = walletRepository.findByIdOrThrow(id)

        walletFromDatabase.apply {
            isArchived = true
        }

        if (walletFromDatabase.isMaster()) {
            walletFromDatabase.apply { balance = getUnallocatedBalance(walletFromDatabase) }
            updateWalletBalance(walletFromDatabase)
            removeAllVirtualWalletsFromMasterWallet(walletFromDatabase.id!!)
        } else {
            walletFromDatabase.masterWallet = null
        }

        logger.info("$walletFromDatabase archived successfully")
    }

    @Transactional
    fun unarchiveWallet(id: Int) {
        val walletFromDatabase = walletRepository.findByIdOrThrow(id)

        walletFromDatabase.apply {
            isArchived = false
        }

        logger.info("$walletFromDatabase unarchived successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_UNARCHIVED_TITLE),
            message =
                MessageFormat.format(
                    preferencesService.translate(
                        TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_UNARCHIVED_MESSAGE,
                    ),
                    walletFromDatabase.name,
                ),
            relatedEntityId = walletFromDatabase.id!!,
        )
    }

    @Transactional
    fun renameWallet(updatedWallet: Wallet) {
        val walletFromDatabase = walletRepository.findByIdOrThrow(updatedWallet.id!!)

        check(!walletRepository.existsByNameAndIdNot(updatedWallet.name, updatedWallet.id!!)) {
            "Wallet with name '${updatedWallet.name}' already exists"
        }

        walletFromDatabase.apply {
            name = updatedWallet.name
        }

        logger.info("$walletFromDatabase renamed successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_NAME_CHANGED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_NAME_CHANGED_MESSAGE),
            relatedEntityId = walletFromDatabase.id!!,
        )
    }

    @Transactional
    fun changeWalletType(updatedWallet: Wallet) {
        val walletFromDatabase = walletRepository.findByIdOrThrow(updatedWallet.id!!)

        check(walletTypeRepository.existsById(updatedWallet.type.id!!)) {
            "Wallet type '${updatedWallet.type.name}' does not exist"
        }

        walletFromDatabase.apply {
            type = updatedWallet.type
        }

        logger.info("$walletFromDatabase type changed successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_TYPE_CHANGED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_TYPE_CHANGED_MESSAGE),
            relatedEntityId = walletFromDatabase.id!!,
        )
    }

    @Transactional
    fun updateWalletBalance(updatedWallet: Wallet) {
        val walletFromDatabase = walletRepository.findByIdOrThrow(updatedWallet.id!!)

        val diff = updatedWallet.balance.subtract(walletFromDatabase.balance)

        when {
            diff > BigDecimal.ZERO -> incrementWalletBalance(walletFromDatabase, diff)
            diff < BigDecimal.ZERO -> decrementWalletBalance(walletFromDatabase, diff.abs())
            else -> logger.info("$walletFromDatabase balance remains unchanged")
        }

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_UPDATED_TITLE),
            message =
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_BALANCE_UPDATED_MESSAGE,
                ),
            relatedEntityId = walletFromDatabase.id!!,
        )
    }

    @Transactional
    fun removeAllVirtualWalletsFromMasterWallet(masterWalletId: Int) {
        val virtualWallets = walletRepository.findVirtualWalletsByMasterWallet(masterWalletId)

        virtualWallets.forEach { virtualWallet ->
            virtualWallet.masterWallet = null
            logger.info(
                "Virtual wallet with id {} unlinked from master wallet with id {}",
                virtualWallet.id,
                masterWalletId,
            )
        }
    }

    @Transactional
    fun createWalletTransaction(
        transaction: WalletTransaction,
        publishNotification: Boolean = false,
    ): Int {
        val walletFromDatabase = walletRepository.findByIdOrThrow(transaction.wallet.id!!)

        val newTransaction = walletTransactionRepository.save(transaction)

        if (transaction.status == WalletTransactionStatus.CONFIRMED) {
            when (transaction.type) {
                WalletTransactionType.INCOME -> incrementWalletBalance(walletFromDatabase, transaction.amount)
                WalletTransactionType.EXPENSE -> decrementWalletBalance(walletFromDatabase, transaction.amount)
            }
        }

        logger.info(
            "${transaction.type} with status ${transaction.status} of ${
                UIUtils.formatCurrency(
                    transaction.amount,
                )} added to wallet with id ${walletFromDatabase.id}",
        )

        if (publishNotification) {
            notificationService.send(
                type = NotificationType.SUCCESS,
                title = preferencesService.translate(TranslationKeys.NOTIFICATION_WALLET_TRANSACTION_TITLE),
                message =
                    MessageFormat.format(
                        preferencesService
                            .translate(TranslationKeys.NOTIFICATION_WALLET_TRANSACTION_MESSAGE),
                        transaction.description ?: "",
                        UIUtils.formatCurrency(transaction.amount),
                    ),
                relatedEntityId = newTransaction.id!!,
            )
        }

        return newTransaction.id!!
    }

    @Transactional
    fun updateWalletTransaction(updatedTransaction: WalletTransaction) {
        val transactionFromDatabase = walletTransactionRepository.findByIdOrThrow(updatedTransaction.id!!)

        updateTransactionWallet(transactionFromDatabase, updatedTransaction.wallet)
        updateTransactionType(transactionFromDatabase, updatedTransaction.type)
        updateTransactionAmount(transactionFromDatabase, updatedTransaction.amount)
        updateTransactionStatus(transactionFromDatabase, updatedTransaction.status)

        transactionFromDatabase.apply {
            date = updatedTransaction.date
            description = updatedTransaction.description
            category = updatedTransaction.category
            includeInAnalysis = updatedTransaction.includeInAnalysis
        }

        logger.info("$transactionFromDatabase updated successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSACTION_UPDATED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSACTION_UPDATED_MESSAGE),
            relatedEntityId = transactionFromDatabase.id!!,
        )
    }

    @Transactional
    fun deleteWalletTransaction(transactionId: Int) {
        val transactionFromDatabase = walletTransactionRepository.findByIdOrThrow(transactionId)

        if (transactionFromDatabase.status == WalletTransactionStatus.CONFIRMED) {
            when (transactionFromDatabase.type) {
                WalletTransactionType.INCOME ->
                    decrementWalletBalance(
                        transactionFromDatabase.wallet,
                        transactionFromDatabase.amount,
                    )
                WalletTransactionType.EXPENSE ->
                    incrementWalletBalance(
                        transactionFromDatabase.wallet,
                        transactionFromDatabase.amount,
                    )
            }
        }

        walletTransactionRepository.delete(transactionFromDatabase)

        logger.info("$transactionFromDatabase deleted from ${transactionFromDatabase.wallet}")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSACTION_DELETED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSACTION_DELETED_MESSAGE),
            relatedEntityId = transactionFromDatabase.id!!,
        )
    }

    @Transactional
    fun createTransfer(transfer: Transfer): Int {
        val senderWallet = walletRepository.findByIdOrThrow(transfer.senderWallet.id!!)
        val receiverWallet = walletRepository.findByIdOrThrow(transfer.receiverWallet.id!!)

        check(!(senderWallet.isMaster() && receiverWallet.isVirtual() && receiverWallet.masterWallet == senderWallet)) {
            "You cannot transfer money from a master wallet to its virtual wallet."
        }

        check(senderWallet.balance >= transfer.amount) {
            "Sender wallet does not have enough balance to transfer"
        }

        val newTransfer = transfersRepository.save(transfer)

        decrementWalletBalance(senderWallet, transfer.amount)
        incrementWalletBalance(receiverWallet, transfer.amount)

        logger.info("$newTransfer was created")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSFER_CREATED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSFER_CREATED_MESSAGE),
            relatedEntityId = newTransfer.id!!,
        )

        return newTransfer.id!!
    }

    @Transactional
    fun updateTransfer(updatedTransfer: Transfer) {
        val transferFromDatabase = transfersRepository.findByIdOrThrow(updatedTransfer.id!!)

        revertTransferWalletBalances(transferFromDatabase, updatedTransfer)

        transferFromDatabase.apply {
            senderWallet = updatedTransfer.senderWallet
            receiverWallet = updatedTransfer.receiverWallet
            amount = updatedTransfer.amount
            date = updatedTransfer.date
            description = updatedTransfer.description
            category = updatedTransfer.category
        }

        logger.info("$transferFromDatabase updated successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSFER_UPDATED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSFER_UPDATED_MESSAGE),
            relatedEntityId = transferFromDatabase.id!!,
        )
    }

    @Transactional
    fun deleteTransfer(transferId: Int) {
        val transferFromDatabase = transfersRepository.findByIdOrThrow(transferId)

        val senderWallet = transferFromDatabase.senderWallet
        val receiverWallet = transferFromDatabase.receiverWallet
        val amount = transferFromDatabase.amount

        logger.info("Reverting $transferFromDatabase")

        incrementWalletBalance(senderWallet, amount)
        decrementWalletBalance(receiverWallet, amount)

        transfersRepository.delete(transferFromDatabase)

        logger.info("$transferFromDatabase deleted successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_SUCCESS_TITLE),
            message =
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_TRANSFER_DELETED_MESSAGE),
            relatedEntityId = transferFromDatabase.id!!,
        )
    }

    fun getWalletById(id: Int): Wallet = walletRepository.findByIdOrThrow(id)

    fun getWalletTransactionById(id: Int): WalletTransaction = walletTransactionRepository.findByIdOrThrow(id)

    fun getAllWalletTypes(): List<WalletType> = walletTypeRepository.findAllByOrderByNameAsc()

    fun existsWalletTypeByName(name: String): Boolean = walletTypeRepository.existsByName(name)

    fun getAllArchivedWallets(): List<Wallet> = walletRepository.findAllByIsArchivedTrue()

    fun getAllNonArchivedWalletsOrderedByName(): List<Wallet> =
        walletRepository.findAllByIsArchivedFalseOrderByNameAsc()

    fun getAllWalletsOrderedByName(): List<Wallet> = walletRepository.findAllByOrderByNameAsc()

    fun getAllNonArchivedWalletsOrderedByTransactionCountDesc(): List<Wallet> =
        walletRepository
            .findAllByIsArchivedFalse()
            .sortedByDescending { wallet ->
                walletTransactionRepository.getTransactionCountByWallet(wallet.id!!) +
                    transfersRepository.getTransferCountByWallet(wallet.id!!)
            }

    fun getUnallocatedBalance(masterWallet: Wallet): BigDecimal {
        val allocatedBalance =
            walletRepository.getAllocatedBalanceByMasterWallet(masterWallet.id!!)

        return masterWallet.balance.subtract(allocatedBalance)
    }

    fun getCountOfVirtualWalletsByMasterWalletId(masterWalletId: Int): Int =
        walletRepository.getCountOfVirtualWalletsByMasterWalletId(masterWalletId)

    fun getAllNonArchivedWalletTransactionsByType(type: WalletTransactionType): List<WalletTransaction> =
        walletTransactionRepository.findAllNonArchivedTransactionsByType(type)

    fun getAllWalletTransactionsByMonth(yearMonth: YearMonth): List<WalletTransaction> =
        walletTransactionRepository.findTransactionsByMonth(yearMonth.monthValue, yearMonth.year)

    fun getAllWalletTransactionsByWalletAndMonth(
        walletId: Int,
        yearMonth: YearMonth,
    ): List<WalletTransaction> =
        walletTransactionRepository.findTransactionsByWalletAndMonth(
            walletId,
            yearMonth.monthValue,
            yearMonth.year,
        )

    fun getAllNonArchivedWalletTransactionsByMonth(yearMonth: YearMonth): List<WalletTransaction> =
        walletTransactionRepository.findNonArchivedTransactionsByMonth(yearMonth.monthValue, yearMonth.year)

    fun getAllNonArchivedWalletTransactionsByMonthForAnalysis(yearMonth: YearMonth): List<WalletTransaction> =
        walletTransactionRepository.findNonArchivedTransactionsByMonthForAnalysis(yearMonth.monthValue, yearMonth.year)

    fun getAllNonArchivedWalletTransactionsByYear(year: Year): List<WalletTransaction> =
        walletTransactionRepository.findNonArchivedTransactionsByYear(year.value)

    fun getAllNonArchivedWalletTransactionsByWalletAndMonth(
        walletId: Int,
        yearMonth: YearMonth,
    ): List<WalletTransaction> =
        walletTransactionRepository.findNonArchivedTransactionsByWalletAndMonth(
            walletId,
            yearMonth.monthValue,
            yearMonth.year,
        )

    fun getAllNonArchivedWalletTransactionsBetweenDates(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<WalletTransaction> = walletTransactionRepository.findNonArchivedTransactionsBetweenDates(startDate, endDate)

    fun getAllNonArchivedConfirmedWalletTransactionsByMonthForAnalysis(yearMonth: YearMonth): List<WalletTransaction> =
        walletTransactionRepository.findNonArchivedAndConfirmedTransactionsByMonthForAnalysis(
            yearMonth.monthValue,
            yearMonth.year,
        )

    fun getAllNonArchivedLastWalletTransactions(n: Int): List<WalletTransaction> =
        walletTransactionRepository.findNonArchivedLastTransactions(
            PageRequest.ofSize(n),
        )

    fun getOldestWalletTransactionDate(): LocalDateTime =
        walletTransactionRepository.findOldestTransactionDate() ?: LocalDateTime.now()

    fun getWalletTransactionAndTransferCountByWallet(walletId: Int): Int =
        walletTransactionRepository.getTransactionCountByWallet(walletId) +
            transfersRepository.getTransferCountByWallet(walletId)

    fun getWalletTransactionAndTransferCountByCategory(categoryId: Int): Int =
        walletTransactionRepository.getTransactionCountByCategory(categoryId) +
            transfersRepository.getTransferCountByCategory(categoryId)

    fun getWalletTransactionSuggestionsByType(type: WalletTransactionType): List<WalletTransaction> =
        walletTransactionRepository.findSuggestions(type)

    fun getAllWalletTransactionsByWalletAfterDate(
        walletId: Int,
        date: LocalDateTime,
    ): List<WalletTransaction> = walletTransactionRepository.findTransactionsByWalletAfterDate(walletId, date)

    fun getFirstWalletTransactionDate(walletId: Int): LocalDateTime? =
        walletTransactionRepository.findFirstTransactionDate(walletId)

    fun getOldestNonArchivedTransactionDate(): LocalDateTime? =
        walletTransactionRepository.findOldestNonArchivedTransactionDate()

    fun getEarliestTransactionDateByWallets(walletIds: List<Int>): LocalDateTime? =
        walletTransactionRepository.findEarliestTransactionDateByWallets(walletIds)

    fun getAllWalletTransactionsByWalletsAfterDate(
        walletIds: List<Int>,
        date: LocalDateTime,
    ) = walletTransactionRepository.findTransactionsByWalletsAfterDate(walletIds, date)

    fun getAllTransfers(): List<Transfer> = transfersRepository.findAll()

    fun getTransfersByWalletAndMonth(
        walletId: Int,
        yearMonth: YearMonth,
    ): List<Transfer> =
        transfersRepository.findTransfersByWalletAndMonth(walletId, yearMonth.monthValue, yearMonth.year)

    fun getTransferSuggestions(): List<Transfer> = transfersRepository.findSuggestions()

    fun existsByName(name: String): Boolean = walletRepository.existsByName(name)

    fun existsByNameAndIdNot(
        name: String,
        id: Int,
    ): Boolean = walletRepository.existsByNameAndIdNot(name, id)

    fun getTotalWalletTransactionAmountByCategoriesAndTypeAndDateTimeBetween(
        categoryIds: List<Int>,
        walletTransactionType: WalletTransactionType,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): BigDecimal =
        walletTransactionRepository.sumAmountByCategoriesAndDateRange(
            categoryIds,
            walletTransactionType,
            startDate,
            endDate,
        )

    private fun incrementWalletBalance(
        walletFromDatabase: Wallet,
        amount: BigDecimal,
    ) {
        updateWalletBalance(walletFromDatabase, amount, true)
    }

    private fun decrementWalletBalance(
        walletFromDatabase: Wallet,
        amount: BigDecimal,
    ) {
        updateWalletBalance(walletFromDatabase, amount, false)
    }

    private fun updateWalletBalance(
        walletToUpdate: Wallet,
        amount: BigDecimal,
        isIncrement: Boolean,
    ) {
        if (amount.isZero()) return

        check(amount > BigDecimal.ZERO) {
            "Amount must be positive but was $amount"
        }

        val adjustedAmount = if (isIncrement) amount else amount.negate()

        when {
            walletToUpdate.isVirtual() -> {
                check(walletToUpdate.balance.add(adjustedAmount) >= BigDecimal.ZERO) {
                    "Virtual wallets cannot have negative balances"
                }

                val master = walletToUpdate.masterWallet!!
                master.balance += adjustedAmount

                logger.info(
                    "{} (master) balance {} by {}",
                    master,
                    if (isIncrement) "incremented" else "decremented",
                    amount,
                )
            }
            walletToUpdate.isMaster() && !isIncrement -> {
                val unallocatedBalance = getUnallocatedBalance(walletToUpdate)

                check(unallocatedBalance >= amount) {
                    "Master wallet cannot be decremented below the unallocated balance. " +
                        "\nUnallocated balance is ${UIUtils.formatCurrency(unallocatedBalance)}"
                }
            }
        }

        walletToUpdate.balance += adjustedAmount

        logger.info(
            "Wallet with id {} balance {} by {}",
            walletToUpdate.id,
            if (isIncrement) "incremented" else "decremented",
            amount,
        )
    }

    private fun updateTransactionWallet(
        oldTransaction: WalletTransaction,
        newWallet: Wallet,
    ) {
        if (oldTransaction.wallet.id == newWallet.id) {
            logger.info("Transaction with id {} has the same wallet as before", oldTransaction.id)
            return
        }

        if (oldTransaction.status == WalletTransactionStatus.CONFIRMED) {
            when (oldTransaction.type) {
                WalletTransactionType.EXPENSE -> {
                    incrementWalletBalance(oldTransaction.wallet, oldTransaction.amount)
                    decrementWalletBalance(newWallet, oldTransaction.amount)
                }
                WalletTransactionType.INCOME -> {
                    decrementWalletBalance(oldTransaction.wallet, oldTransaction.amount)
                    incrementWalletBalance(newWallet, oldTransaction.amount)
                }
            }
        }

        oldTransaction.wallet = newWallet

        logger.info(
            "Transaction with id {} wallet changed to {}",
            oldTransaction.id,
            newWallet.name,
        )
    }

    private fun updateTransactionType(
        oldTransaction: WalletTransaction,
        newType: WalletTransactionType,
    ) {
        if (oldTransaction.type == newType) {
            logger.info("Transaction with id {} has the same type as before", oldTransaction.id)
            return
        }

        if (oldTransaction.status == WalletTransactionStatus.CONFIRMED) {
            when (oldTransaction.type) {
                WalletTransactionType.EXPENSE ->
                    incrementWalletBalance(
                        oldTransaction.wallet,
                        oldTransaction.amount.multiply(BigDecimal.TWO),
                    )
                WalletTransactionType.INCOME ->
                    decrementWalletBalance(
                        oldTransaction.wallet,
                        oldTransaction.amount.multiply(BigDecimal.TWO),
                    )
            }
        }

        oldTransaction.type = newType

        logger.info("Transaction with id {} type changed to {}", oldTransaction.id, newType)
    }

    private fun updateTransactionAmount(
        oldTransaction: WalletTransaction,
        newAmount: BigDecimal,
    ) {
        val diff = oldTransaction.amount.minus(newAmount)

        if (diff == BigDecimal.ZERO) {
            logger.info("Transaction with id {} has the same amount as before", oldTransaction.id)
            return
        }

        if (oldTransaction.status == WalletTransactionStatus.CONFIRMED) {
            val shouldIncrement =
                (oldTransaction.type == WalletTransactionType.EXPENSE && diff > BigDecimal.ZERO) ||
                    (oldTransaction.type == WalletTransactionType.INCOME && diff < BigDecimal.ZERO)

            if (shouldIncrement) {
                incrementWalletBalance(oldTransaction.wallet, diff.abs())
            } else {
                decrementWalletBalance(oldTransaction.wallet, diff.abs())
            }
        }

        oldTransaction.amount = newAmount

        logger.info("Transaction with id {} amount changed to {}", oldTransaction.id, newAmount)
    }

    private fun updateTransactionStatus(
        oldTransaction: WalletTransaction,
        newStatus: WalletTransactionStatus,
    ) {
        if (oldTransaction.status == newStatus) {
            logger.info("Transaction with id {} has the same status as before", oldTransaction.id)
            return
        }

        if (oldTransaction.status == WalletTransactionStatus.CONFIRMED &&
            newStatus == WalletTransactionStatus.PENDING
        ) {
            when (oldTransaction.type) {
                WalletTransactionType.EXPENSE -> incrementWalletBalance(oldTransaction.wallet, oldTransaction.amount)
                WalletTransactionType.INCOME -> decrementWalletBalance(oldTransaction.wallet, oldTransaction.amount)
            }
        } else {
            when (oldTransaction.type) {
                WalletTransactionType.EXPENSE -> decrementWalletBalance(oldTransaction.wallet, oldTransaction.amount)
                WalletTransactionType.INCOME -> incrementWalletBalance(oldTransaction.wallet, oldTransaction.amount)
            }
        }

        oldTransaction.status = newStatus

        logger.info("Transaction with id {} status changed to {}", oldTransaction.id, newStatus)
    }

    private fun revertTransferWalletBalances(
        oldTransfer: Transfer,
        newTransfer: Transfer,
    ) {
        val senderChanged = oldTransfer.senderWallet.id != newTransfer.senderWallet.id
        val receiverChanged = oldTransfer.receiverWallet.id != newTransfer.receiverWallet.id

        when {
            !senderChanged && !receiverChanged -> {
                val difference = newTransfer.amount.minus(oldTransfer.amount)
                when {
                    difference > BigDecimal.ZERO -> {
                        decrementWalletBalance(newTransfer.senderWallet, difference)
                        incrementWalletBalance(newTransfer.receiverWallet, difference)
                    }
                    difference < BigDecimal.ZERO -> {
                        incrementWalletBalance(newTransfer.senderWallet, difference.abs())
                        decrementWalletBalance(newTransfer.receiverWallet, difference.abs())
                    }
                }
            }
            senderChanged && !receiverChanged -> {
                incrementWalletBalance(oldTransfer.senderWallet, oldTransfer.amount)
                decrementWalletBalance(newTransfer.senderWallet, newTransfer.amount)

                val difference = newTransfer.amount.minus(oldTransfer.amount)
                when {
                    difference > BigDecimal.ZERO -> incrementWalletBalance(newTransfer.receiverWallet, difference)
                    difference < BigDecimal.ZERO -> decrementWalletBalance(newTransfer.receiverWallet, difference.abs())
                }
            }
            !senderChanged && receiverChanged -> {
                decrementWalletBalance(oldTransfer.receiverWallet, oldTransfer.amount)
                incrementWalletBalance(newTransfer.receiverWallet, newTransfer.amount)

                val difference = newTransfer.amount.minus(oldTransfer.amount)
                when {
                    difference > BigDecimal.ZERO -> decrementWalletBalance(newTransfer.senderWallet, difference)
                    difference < BigDecimal.ZERO -> incrementWalletBalance(newTransfer.senderWallet, difference.abs())
                }
            }
            else -> {
                incrementWalletBalance(oldTransfer.senderWallet, oldTransfer.amount)
                decrementWalletBalance(oldTransfer.receiverWallet, oldTransfer.amount)

                decrementWalletBalance(newTransfer.senderWallet, newTransfer.amount)
                incrementWalletBalance(newTransfer.receiverWallet, newTransfer.amount)
            }
        }
    }
}
