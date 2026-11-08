/*
 * Filename: BondService.kt (original filename: BondService.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 11/03/2026
 */

package org.moinex.service.investment

import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.calculateInvestedAmountUntil
import org.moinex.common.extension.calculateQuantityUntil
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isHeldInReais
import org.moinex.common.extension.toRounded
import org.moinex.model.dto.BondDetailDTO
import org.moinex.model.dto.BondRedemptionEstimateDTO
import org.moinex.model.dto.MonthBalanceDTO
import org.moinex.model.dto.WalletTransactionContextDTO
import org.moinex.model.enums.NotificationType
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondInterestCalculation
import org.moinex.model.investment.BondOperation
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.MessageFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class BondService(
    private val bondRepository: BondRepository,
    private val bondOperationRepository: BondOperationRepository,
    private val walletService: WalletService,
    private val bondInterestCalculationService: BondInterestCalculationService,
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
) {
    private val logger = LoggerFactory.getLogger(BondService::class.java)

    companion object {
        private const val PRICE_SCALE = 8
        private const val SHARE_SCALE = 10
    }

    @Transactional
    fun createBond(bond: Bond) {
        bond.symbol?.let {
            check(!bondRepository.existsBySymbol(bond.symbol!!)) {
                "Bond with symbol ${bond.symbol} already exists"
            }
        }

        val newBond = bondRepository.save(bond)

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_ADDED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_ADDED_MESSAGE),
            relatedEntityId = newBond.id!!,
        )

        logger.info("$newBond created successfully")
    }

    @Transactional
    fun updateBond(updatedBond: Bond) {
        val bondFromDatabase = bondRepository.findByIdOrThrow(updatedBond.id!!)

        updatedBond.symbol?.let {
            check(!bondRepository.existsBySymbolAndIdNot(updatedBond.symbol!!, updatedBond.id!!)) {
                "Bond with symbol ${updatedBond.symbol} already exists"
            }
        }

        bondFromDatabase.apply {
            name = updatedBond.name
            symbol = updatedBond.symbol
            type = updatedBond.type
            issuer = updatedBond.issuer
            maturityDate = updatedBond.maturityDate
            interestType = updatedBond.interestType
            interestIndex = updatedBond.interestIndex
            interestRate = updatedBond.interestRate
        }

        logger.info("$bondFromDatabase updated successfully")
    }

    @Transactional
    fun deleteBond(id: Int) {
        val bondFromDatabase = bondRepository.findByIdOrThrow(id)

        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bondFromDatabase)

        check(operations.isEmpty()) {
            "$bondFromDatabase has operations associated with it and cannot be deleted. " +
                "Remove the operations first or archive the bond"
        }

        bondRepository.delete(bondFromDatabase)

        logger.info("$bondFromDatabase deleted successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_BOND_DELETED_TITLE),
            message =
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_BOND_DELETED_MESSAGE),
                    bondFromDatabase.name,
                ),
            relatedEntityId = bondFromDatabase.id!!,
        )
    }

    @Transactional
    fun archiveBond(id: Int) {
        val bondFromDatabase = bondRepository.findByIdOrThrow(id)

        bondFromDatabase.archived = true

        logger.info("$bondFromDatabase archived successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_UPDATED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_UPDATED_MESSAGE),
            relatedEntityId = bondFromDatabase.id!!,
        )
    }

    @Transactional
    fun unarchiveBond(id: Int) {
        val bondFromDatabase = bondRepository.findByIdOrThrow(id)

        bondFromDatabase.archived = false

        logger.info("$bondFromDatabase unarchived successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_BOND_UNARCHIVED_TITLE),
            message =
                MessageFormat.format(
                    preferencesService.translate(TranslationKeys.BOND_DIALOG_BOND_UNARCHIVED_MESSAGE),
                    bondFromDatabase.name,
                ),
            relatedEntityId = bondFromDatabase.id!!,
        )
    }

    @Transactional
    fun createBondOperation(
        bondOperation: BondOperation,
        walletTransactionContextDTO: WalletTransactionContextDTO,
    ) {
        check(bondRepository.existsById(bondOperation.bond.id!!)) {
            "${bondOperation.bond} does not exist"
        }

        if (bondOperation.operationType == OperationType.SELL) {
            val currentQuantity = getCurrentQuantity(bondOperation.bond)

            check(currentQuantity >= bondOperation.quantity) {
                "Insufficient bond quantity. Available: $currentQuantity, Requested: ${bondOperation.quantity}"
            }
        }

        val baseAmount = bondOperation.unitPrice.multiply(bondOperation.quantity)
        val operationAmount =
            when (bondOperation.operationType) {
                OperationType.BUY -> baseAmount.add(bondOperation.fees).add(bondOperation.taxes)
                OperationType.SELL -> baseAmount.add(bondOperation.netProfit)
            }

        val transactionType =
            when (bondOperation.operationType) {
                OperationType.BUY -> WalletTransactionType.EXPENSE
                OperationType.SELL -> WalletTransactionType.INCOME
            }

        val transactionId =
            walletService.createWalletTransaction(
                WalletTransaction.from(walletTransactionContextDTO, transactionType, operationAmount),
            )

        val walletTransaction = walletService.getWalletTransactionById(transactionId)

        bondOperation.apply { this.walletTransaction = walletTransaction }

        val newBondOperation = bondOperationRepository.save(bondOperation)

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_PURCHASE_ADDED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_PURCHASE_ADDED_MESSAGE),
            relatedEntityId = newBondOperation.id!!,
        )

        logger.info("$bondOperation created successfully")
    }

    @Transactional
    fun updateBondOperation(
        updatedBondOperation: BondOperation,
        walletTransactionContextDTO: WalletTransactionContextDTO,
    ) {
        val bondOperationFromDatabase = bondOperationRepository.findByIdOrThrow(updatedBondOperation.id!!)

        if (bondOperationFromDatabase.operationType == OperationType.SELL) {
            val currentQuantity = getCurrentQuantity(bondOperationFromDatabase.bond)
            val availableQuantity = currentQuantity.add(bondOperationFromDatabase.quantity)

            check(availableQuantity >= updatedBondOperation.quantity) {
                "Insufficient bond quantity. Available: $availableQuantity, Requested: ${updatedBondOperation.quantity}"
            }
        }

        val baseAmount = updatedBondOperation.unitPrice.multiply(updatedBondOperation.quantity)
        val operationAmount =
            when (bondOperationFromDatabase.operationType) {
                OperationType.BUY -> baseAmount.add(updatedBondOperation.fees).add(updatedBondOperation.taxes)
                OperationType.SELL -> baseAmount.add(updatedBondOperation.netProfit)
            }

        val walletTransaction = bondOperationFromDatabase.walletTransaction!!

        val updatedWalletTransaction =
            WalletTransaction(
                id = walletTransaction.id,
                date = walletTransactionContextDTO.date,
                status = walletTransactionContextDTO.status,
                description = walletTransactionContextDTO.description,
                includeInAnalysis = walletTransactionContextDTO.includeInAnalysis,
                wallet = walletTransactionContextDTO.wallet,
                category = walletTransactionContextDTO.category,
                type = walletTransaction.type,
                amount = operationAmount,
            )

        walletService.updateWalletTransaction(updatedWalletTransaction)

        bondOperationFromDatabase
            .apply {
                quantity = updatedBondOperation.quantity
                unitPrice = updatedBondOperation.unitPrice
                fees = updatedBondOperation.fees
                taxes = updatedBondOperation.taxes
            }.also {
                if (it.operationType == OperationType.SELL) {
                    it.netProfit = updatedBondOperation.netProfit
                }
            }

        logger.info("$bondOperationFromDatabase updated successfully")

        val (titleKey, messageKey) =
            when (updatedBondOperation.operationType) {
                OperationType.BUY ->
                    TranslationKeys.BOND_DIALOG_PURCHASE_UPDATED_TITLE to
                        TranslationKeys.BOND_DIALOG_PURCHASE_UPDATED_MESSAGE
                OperationType.SELL ->
                    TranslationKeys.BOND_DIALOG_SALE_UPDATED_TITLE to
                        TranslationKeys.BOND_DIALOG_SALE_UPDATED_MESSAGE
            }

        notificationService.send(
            type = NotificationType.SUCCESS,
            title = preferencesService.translate(titleKey),
            message = preferencesService.translate(messageKey),
            relatedEntityId = bondOperationFromDatabase.id!!,
        )
    }

    @Transactional
    fun deleteBondOperation(operationId: Int) {
        val bondOperationFromDatabase = bondOperationRepository.findByIdOrThrow(operationId)

        val walletTransaction = bondOperationFromDatabase.walletTransaction!!

        bondOperationRepository.delete(bondOperationFromDatabase)

        walletService.deleteWalletTransaction(walletTransaction.id!!)

        logger.info("$bondOperationFromDatabase deleted successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_OPERATION_DELETED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.BOND_DIALOG_OPERATION_DELETED_MESSAGE),
            relatedEntityId = bondOperationFromDatabase.id!!,
        )
    }

    @Transactional(readOnly = true)
    fun getCurrentQuantity(bond: Bond): BigDecimal {
        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)

        return operations.fold(BigDecimal.ZERO) { quantity, op ->
            when (op.operationType) {
                OperationType.BUY -> quantity.add(op.quantity)
                OperationType.SELL -> quantity.subtract(op.quantity)
            }
        }
    }

    @Transactional(readOnly = true)
    fun getAverageUnitPrice(bond: Bond): BigDecimal {
        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)
        val heldQuantity = operations.calculateQuantityUntil(LocalDate.MAX)

        if (heldQuantity.signum() > 0) {
            return operations
                .calculateInvestedAmountUntil(LocalDate.MAX)
                .divide(heldQuantity, PRICE_SCALE, RoundingMode.HALF_UP)
                .toRounded()
        }

        val purchases = operations.filter { it.isPurchase() }
        val purchasedQuantity = purchases.fold(BigDecimal.ZERO) { acc, purchase -> acc.add(purchase.quantity) }

        if (purchasedQuantity.signum() <= 0) {
            return BigDecimal.ZERO
        }

        return purchases
            .fold(BigDecimal.ZERO) { acc, purchase -> acc.add(purchase.unitPrice.multiply(purchase.quantity)) }
            .divide(purchasedQuantity, PRICE_SCALE, RoundingMode.HALF_UP)
            .toRounded()
    }

    /**
     * The position of [bond] for its detail window: invested, current value (invested plus the interest held), the
     * interest of the current month, the estimated net interest as of [today] and the value at the end of each month of
     * its interest history.
     */
    @Transactional(readOnly = true)
    fun detail(
        bond: Bond,
        today: LocalDate = LocalDate.now(),
    ): BondDetailDTO {
        val invested = getTotalInvestedValue(bond)
        val accumulated = getTotalAccumulatedInterestByBondId(bond.id!!)

        return BondDetailDTO(
            bond = bond,
            invested = invested,
            currentValue = invested.add(accumulated),
            accumulatedInterest = accumulated,
            monthInterest = getCurrentMonthInterest(bond.id!!),
            netInterest = estimateNetInterest(bond, today),
            monthlyBalance =
                bondInterestCalculationService
                    .getMonthlyInterestHistory(bond)
                    .sortedBy { it.referenceMonth }
                    .map { MonthBalanceDTO(it.referenceMonth, it.finalValue) },
        )
    }

    /**
     * The interest still held in [bond] as a percentage of what is invested in it, with two decimals; null when
     * nothing is invested.
     */
    @Transactional(readOnly = true)
    fun yieldPercentage(bond: Bond): BigDecimal? {
        val invested = getTotalInvestedValue(bond)

        if (invested.signum() <= 0) return null

        return getTotalAccumulatedInterestByBondId(bond.id!!)
            .multiply(BigDecimal(100))
            .divide(invested, 2, RoundingMode.HALF_UP)
    }

    /**
     * Whether [bond] is held as an amount in reais: a CDB, LCI or LCA whose units cost one. Bonds of those types saved
     * with another unit price keep showing quantity and price.
     */
    @Transactional(readOnly = true)
    fun isAmountOnly(bond: Bond): Boolean =
        bond.isHeldInReais() && getAverageUnitPrice(bond).compareTo(BigDecimal.ONE) == 0

    /**
     * What a redemption of [quantity] units at [unitPrice] on [date] takes out besides the principal: the same share
     * of the interest held in the bond as the share of the principal redeemed, and the estimated tax on it.
     */
    @Transactional(readOnly = true)
    fun estimateRedemption(
        bond: Bond,
        quantity: BigDecimal,
        unitPrice: BigDecimal,
        date: LocalDate,
    ): BondRedemptionEstimateDTO {
        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)
        val principalHeld = operations.calculateInvestedAmountUntil(date)

        val share =
            if (principalHeld.signum() > 0) {
                quantity
                    .multiply(
                        unitPrice,
                    ).divide(principalHeld, SHARE_SCALE, RoundingMode.HALF_UP)
                    .min(BigDecimal.ONE)
            } else {
                BigDecimal.ZERO
            }

        return redemption(bond, operations, principalHeld, share, date)
    }

    /**
     * What redeeming [gross] reais on [date] takes out of [bond]: the principal and the interest in the proportion in
     * which both are held, and the estimated tax on that interest.
     */
    @Transactional(readOnly = true)
    fun estimateRedemptionByGross(
        bond: Bond,
        gross: BigDecimal,
        date: LocalDate,
    ): BondRedemptionEstimateDTO {
        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)
        val principalHeld = operations.calculateInvestedAmountUntil(date)
        val interestHeld = interestHeld(bond)
        val balance = principalHeld.add(interestHeld)

        val share =
            if (balance.signum() > 0) {
                gross.divide(balance, SHARE_SCALE, RoundingMode.HALF_UP).min(BigDecimal.ONE)
            } else {
                BigDecimal.ZERO
            }

        return redemption(bond, operations, principalHeld, share, date)
    }

    private fun interestHeld(bond: Bond): BigDecimal =
        (bondInterestCalculationService.getLatestCalculation(bond.id!!)?.accumulatedInterest ?: BigDecimal.ZERO)
            .max(BigDecimal.ZERO)

    private fun redemption(
        bond: Bond,
        operations: List<BondOperation>,
        principalHeld: BigDecimal,
        share: BigDecimal,
        date: LocalDate,
    ): BondRedemptionEstimateDTO {
        val interest = interestHeld(bond).multiply(share).setScale(2, RoundingMode.HALF_UP)
        val lots = taxLotsAt(operations, date).map { BondTaxEstimator.Lot(it.principal.multiply(share), it.ageDays) }
        val estimate = BondTaxEstimator.estimate(bond.type, lots, interest)

        return BondRedemptionEstimateDTO(
            interest,
            estimate.tax,
            estimate.applicable,
            principalHeld.multiply(share).setScale(2, RoundingMode.HALF_UP),
        )
    }

    /**
     * The interest held in [bond], its estimated tax and the net, as if everything were redeemed on [date].
     */
    @Transactional(readOnly = true)
    fun estimateNetInterest(
        bond: Bond,
        date: LocalDate,
    ): BondRedemptionEstimateDTO {
        val operations = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)
        val interest =
            (bondInterestCalculationService.getLatestCalculation(bond.id!!)?.accumulatedInterest ?: BigDecimal.ZERO)
                .max(BigDecimal.ZERO)
        val estimate = BondTaxEstimator.estimate(bond.type, taxLotsAt(operations, date), interest)

        return BondRedemptionEstimateDTO(interest, estimate.tax, estimate.applicable)
    }

    private fun taxLotsAt(
        operations: List<BondOperation>,
        date: LocalDate,
    ): List<BondTaxEstimator.Lot> = BondTaxEstimator.lotsAt(operations, date)

    @Transactional(readOnly = true)
    fun getAllNonArchivedBonds(): List<Bond> = bondRepository.findByArchivedFalseOrderByNameAsc()

    @Transactional(readOnly = true)
    fun getAllArchivedBonds(): List<Bond> = bondRepository.findByArchivedTrueOrderByNameAsc()

    @Transactional(readOnly = true)
    fun getBondById(id: Int): Bond = bondRepository.findByIdOrThrow(id)

    @Transactional(readOnly = true)
    fun getAllOperations(): List<BondOperation> = bondOperationRepository.findAllByOrderByOperationDateDesc()

    @Transactional(readOnly = true)
    fun getOperationsByBond(bond: Bond): List<BondOperation> =
        bondOperationRepository.findByBondOrderByOperationDateAsc(bond)

    @Transactional(readOnly = true)
    fun getTotalProfit(bond: Bond): BigDecimal {
        val operations =
            bondOperationRepository.findByBondAndOperationTypeOrderByOperationDateAsc(
                bond,
                OperationType.SELL,
            )

        return operations.sumOf { it.netProfit }
    }

    @Transactional(readOnly = true)
    fun getTotalInvestedValue(): BigDecimal {
        val bonds = bondRepository.findByArchivedFalseOrderByNameAsc()

        return bonds
            .filter { getCurrentQuantity(it) > BigDecimal.ZERO }
            .fold(BigDecimal.ZERO) { total, bond -> total.add(getTotalInvestedValue(bond)) }
    }

    @Transactional(readOnly = true)
    fun getTotalCurrentValue(currentMarketPrice: BigDecimal): BigDecimal {
        val bonds = bondRepository.findByArchivedFalseOrderByNameAsc()

        return bonds.fold(BigDecimal.ZERO) { total, bond ->
            total.add(currentMarketPrice.multiply(getCurrentQuantity(bond)))
        }
    }

    @Transactional(readOnly = true)
    fun getTotalInvestedValue(bond: Bond): BigDecimal =
        bondOperationRepository
            .findByBondOrderByOperationDateAsc(bond)
            .calculateInvestedAmountUntil(LocalDate.MAX)

    @Transactional(readOnly = true)
    fun getAllBondsTotalAccumulatedInterest(): BigDecimal {
        val bonds = bondRepository.findByArchivedFalseOrderByNameAsc()

        return bonds.sumOf { getTotalAccumulatedInterestByBondId(it.id!!) }
    }

    @Transactional(readOnly = true)
    fun getOperationCountByBond(bondId: Int): Int {
        val bondFromDatabase = bondRepository.findByIdOrThrow(bondId)

        return bondOperationRepository.findByBondOrderByOperationDateAsc(bondFromDatabase).size
    }

    @Transactional(readOnly = true)
    fun getOperationsByDateBefore(date: LocalDateTime): List<BondOperation> =
        bondOperationRepository.findAllByDateBefore(date)

    @Transactional(readOnly = true)
    fun getMonthlyInterestHistory(bondId: Int): List<BondInterestCalculation> {
        val bondFromDatabase = bondRepository.findByIdOrThrow(bondId)
        return bondInterestCalculationService.getMonthlyInterestHistory(bondFromDatabase)
    }

    @Transactional(readOnly = true)
    fun getCurrentMonthInterest(bondId: Int): BigDecimal =
        bondInterestCalculationService
            .getCurrentMonthInterest(bondId) ?: BigDecimal.ZERO

    @Transactional(readOnly = true)
    fun getTotalAccumulatedInterestByBondId(bondId: Int): BigDecimal =
        bondInterestCalculationService.getLatestCalculation(bondId)?.accumulatedInterest ?: BigDecimal.ZERO

    @Transactional(readOnly = true)
    fun getTotalInvestedValueByMonth(yearMonth: YearMonth): BigDecimal {
        val operations =
            bondOperationRepository
                .findAllByOrderByOperationDateDesc()
                .filter { YearMonth.from(it.walletTransaction!!.date) == yearMonth }

        val purchased =
            operations
                .filter { it.isPurchase() }
                .sumOf { it.unitPrice.multiply(it.quantity) }

        val sold =
            operations
                .filter { it.isSale() }
                .sumOf { it.unitPrice.multiply(it.quantity) }

        val net = purchased.subtract(sold)
        return if (net > BigDecimal.ZERO) net else BigDecimal.ZERO
    }
}
