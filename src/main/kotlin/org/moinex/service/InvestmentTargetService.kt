/*
 * Filename: InvestmentTargetService.kt (original filename: InvestmentTargetService.java)
 * Created on: March 8, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 08/03/2026
 */

package org.moinex.service

import jakarta.persistence.EntityNotFoundException
import org.moinex.common.extension.isBetween
import org.moinex.common.extension.isEqual
import org.moinex.common.extension.isNotEqual
import org.moinex.model.enums.AssetType
import org.moinex.model.investment.InvestmentTarget
import org.moinex.repository.investment.InvestmentTargetRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class InvestmentTargetService(
    private val investmentTargetRepository: InvestmentTargetRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    companion object {
        data class ValidationResult(
            val isValid: Boolean,
            val total: BigDecimal,
            val errors: List<String>,
        )
    }

    @Transactional
    fun saveTargets(targets: Map<AssetType, BigDecimal>) {
        val validationResult = validate(targets)

        check(validationResult.isValid) {
            validationResult.errors.joinToString(", ")
        }

        check(validationResult.total.isEqual(100)) {
            "Total investment target percentage must equal 100"
        }

        val currentTargets = investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc()

        currentTargets.forEach { existingTarget ->
            val newPercentage = targets[existingTarget.assetType]

            if (newPercentage != null) {
                if (newPercentage == BigDecimal.ZERO) {
                    existingTarget.isActive = false
                } else {
                    existingTarget.targetPercentage = newPercentage
                }
            } else {
                existingTarget.isActive = false
            }
        }

        targets.forEach { (assetType, percentage) ->
            if (percentage > BigDecimal.ZERO) {
                val existingTarget = currentTargets.find { it.assetType == assetType }
                if (existingTarget == null) {
                    investmentTargetRepository.save(
                        InvestmentTarget(null, assetType, percentage, true),
                    )
                }
            }
        }

        logger.info("Investment targets saved successfully")
    }

    fun getAllActiveTargets(): List<InvestmentTarget> = investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc()

    fun getTargetByType(assetType: AssetType): InvestmentTarget =
        investmentTargetRepository.findByAssetTypeAndIsActiveTrue(assetType)
            ?: throw EntityNotFoundException("Investment target not found for type: $assetType")

    fun validate(targets: Map<AssetType, BigDecimal>): ValidationResult {
        val total = targets.values.sumOf { it }

        val errors = mutableListOf<String>()

        targets.forEach { (asset, percentage) ->
            if (!percentage.isBetween(BigDecimal.ZERO, BigDecimal("100"))) {
                errors.add("$asset percentage must be between 0 and 100")
            }
        }

        if (total.isNotEqual(100)) {
            errors.add("Total investment target percentage must equal 100")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            total = total,
            errors = errors,
        )
    }
}
