/*
 * Filename: BondService.kt (original filename: BondService.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 11/03/2026
 */

package org.moinex.service

import org.moinex.common.extension.findByIdOrThrow
import org.moinex.common.extension.isZero
import org.moinex.common.extension.toRounded
import org.moinex.model.dto.BondOperationWalletTransactionDTO
import org.moinex.model.enums.OperationType
import org.moinex.model.enums.WalletTransactionType
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondInterestCalculation
import org.moinex.model.investment.BondOperation
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class BondService(
    private val bondRepository: BondRepository,
    private val bondOperationRepository: BondOperationRepository,
    private val walletService: WalletService,
    private val bondInterestCalculationService: BondInterestCalculationService,
) {
    private val logger = LoggerFactory.getLogger(BondService::class.java)

    @Transactional
    fun createBond(bond: Bond) {
        bond.symbol?.let {
            check(!bondRepository.existsBySymbol(bond.symbol!!)) {
                "Bond with symbol ${bond.symbol} already exists"
            }
        }

        val newBond = bondRepository.save(bond)

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
    }

    @Transactional
    fun archiveBond(id: Int) {
        val bondFromDatabase = bondRepository.findByIdOrThrow(id)

        bondFromDatabase.archived = true

        logger.info("$bondFromDatabase archived successfully")
    }

    @Transactional
    fun unarchiveBond(id: Int) {
        val bondFromDatabase = bondRepository.findByIdOrThrow(id)

        bondFromDatabase.archived = false

        logger.info("$bondFromDatabase unarchived successfully")
    }

    @Transactional
    fun createBondOperation(
        bondOperation: BondOperation,
        bondOperationWalletTransactionDTO: BondOperationWalletTransactionDTO,
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
                WalletTransaction.from(bondOperationWalletTransactionDTO, transactionType, operationAmount),
            )

        val walletTransaction = walletService.getWalletTransactionById(transactionId)

        bondOperation.apply { this.walletTransaction = walletTransaction }

        bondOperationRepository.save(bondOperation)

        logger.info("$bondOperation created successfully")
    }

    @Transactional
    fun updateBondOperation(
        updatedBondOperation: BondOperation,
        bondOperationWalletTransactionDTO: BondOperationWalletTransactionDTO,
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

        walletTransaction.apply {
            wallet = bondOperationWalletTransactionDTO.wallet
            category = bondOperationWalletTransactionDTO.category
            date = bondOperationWalletTransactionDTO.date
            amount = operationAmount
            description = bondOperationWalletTransactionDTO.description
            status = bondOperationWalletTransactionDTO.status
            includeInAnalysis = bondOperationWalletTransactionDTO.includeInAnalysis
        }

        walletService.updateWalletTransaction(walletTransaction)

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
    }

    @Transactional
    fun deleteBondOperation(operationId: Int) {
        val bondOperationFromDatabase = bondOperationRepository.findByIdOrThrow(operationId)

        val walletTransaction = bondOperationFromDatabase.walletTransaction!!

        bondOperationRepository.delete(bondOperationFromDatabase)

        walletService.deleteWalletTransaction(walletTransaction.id!!)

        logger.info("$bondOperationFromDatabase deleted successfully")
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
        val purchases =
            bondOperationRepository.findByBondAndOperationTypeOrderByOperationDateAsc(
                bond,
                OperationType.BUY,
            )

        if (purchases.isEmpty()) {
            return BigDecimal.ZERO
        }

        val totalValue =
            purchases.fold(BigDecimal.ZERO) { acc, purchase ->
                acc.add(purchase.unitPrice.multiply(purchase.quantity))
            }

        val totalQuantity =
            purchases.fold(BigDecimal.ZERO) { acc, purchase ->
                acc.add(purchase.quantity)
            }

        if (totalQuantity.isZero()) {
            return BigDecimal.ZERO
        }

        return totalValue.divide(totalQuantity).toRounded()
    }

    @Transactional(readOnly = true)
    fun getAllNonArchivedBonds(): List<Bond> = bondRepository.findByArchivedFalseOrderByNameAsc()

    @Transactional(readOnly = true)
    fun getAllArchivedBonds(): List<Bond> = bondRepository.findByArchivedTrueOrderByNameAsc()

    @Transactional(readOnly = true)
    fun getBondById(id: Int): Bond = bondRepository.findByIdOrThrow(id)

    @Transactional(readOnly = true)
    fun getAllOperations(): List<BondOperation> = bondOperationRepository.findAllByOrderByOperationDateDesc()

    @Transactional(readOnly = true)
    fun getOperationsByBond(bond: Bond): List<BondOperation> = bondOperationRepository.findByBondOrderByOperationDateAsc(bond)

    @Transactional(readOnly = true)
    fun getTotalProfit(bond: Bond): BigDecimal {
        val operations = bondOperationRepository.findByBondAndOperationTypeOrderByOperationDateAsc(bond, OperationType.SELL)

        return operations.sumOf { it.netProfit }
    }

    @Transactional(readOnly = true)
    fun getTotalInvestedValue(): BigDecimal {
        val bonds = bondRepository.findByArchivedFalseOrderByNameAsc()

        return bonds
            .filter { getCurrentQuantity(it) > BigDecimal.ZERO }
            .fold(BigDecimal.ZERO) { total, bond ->
                val currentQuantity = getCurrentQuantity(bond)
                val averagePrice = getAverageUnitPrice(bond)
                total.add(averagePrice.multiply(currentQuantity))
            }
    }

    @Transactional(readOnly = true)
    fun getTotalCurrentValue(currentMarketPrice: BigDecimal): BigDecimal {
        val bonds = bondRepository.findByArchivedFalseOrderByNameAsc()

        return bonds.fold(BigDecimal.ZERO) { total, bond ->
            total.add(currentMarketPrice.multiply(getCurrentQuantity(bond)))
        }
    }

    @Transactional(readOnly = true)
    fun getTotalInvestedValue(bond: Bond): BigDecimal = getAverageUnitPrice(bond).multiply(getCurrentQuantity(bond))

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
    fun getOperationsByDateBefore(date: LocalDateTime): List<BondOperation> = bondOperationRepository.findAllByDateBefore(date)

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
}
