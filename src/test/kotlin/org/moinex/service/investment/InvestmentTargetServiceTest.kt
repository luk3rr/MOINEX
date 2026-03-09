/*
 * Filename: InvestmentTargetServiceTest.kt
 * Created on: March 8, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.common.extension.isEqual
import org.moinex.factory.investment.InvestmentTargetFactory
import org.moinex.model.enums.AssetType
import org.moinex.repository.investment.InvestmentTargetRepository
import org.moinex.service.InvestmentTargetService
import java.math.BigDecimal

class InvestmentTargetServiceTest :
    BehaviorSpec({
        val investmentTargetRepository = mockk<InvestmentTargetRepository>()
        val service = InvestmentTargetService(investmentTargetRepository)

        afterContainer { clearAllMocks(answers = true) }

        Given("valid investment targets totaling 100%") {
            When("saving targets") {
                val targets =
                    mapOf(
                        AssetType.STOCK to BigDecimal("60"),
                        AssetType.BOND to BigDecimal("40"),
                    )
                val currentTargets = emptyList<org.moinex.model.investment.InvestmentTarget>()
                every { investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc() } returns currentTargets
                every { investmentTargetRepository.save(any()) } returnsArgument 0

                service.saveTargets(targets)

                Then("should save all targets") {
                    verify(exactly = 2) { investmentTargetRepository.save(any()) }
                }
            }
        }

        Given("targets with percentage totaling less than 100%") {
            When("saving targets") {
                val targets =
                    mapOf(
                        AssetType.STOCK to BigDecimal("50"),
                        AssetType.BOND to BigDecimal("30"),
                    )

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.saveTargets(targets)
                    }
                }
            }
        }

        Given("targets with percentage totaling more than 100%") {
            When("saving targets") {
                val targets =
                    mapOf(
                        AssetType.STOCK to BigDecimal("60"),
                        AssetType.BOND to BigDecimal("50"),
                    )

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.saveTargets(targets)
                    }
                }
            }
        }

        Given("targets with negative percentage") {
            When("saving targets") {
                val targets =
                    mapOf(
                        AssetType.STOCK to BigDecimal("-10"),
                        AssetType.BOND to BigDecimal("110"),
                    )

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.saveTargets(targets)
                    }
                }
            }
        }

        Given("targets with percentage exceeding 100") {
            When("saving targets") {
                val targets =
                    mapOf(
                        AssetType.STOCK to BigDecimal("101"),
                    )

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.saveTargets(targets)
                    }
                }
            }
        }

        Given("existing targets to be updated") {
            When("saving new targets for existing asset types") {
                val existingStock =
                    InvestmentTargetFactory.create(
                        id = 1,
                        assetType = AssetType.STOCK,
                        targetPercentage = BigDecimal("50"),
                        isActive = true,
                    )
                val existingBond =
                    InvestmentTargetFactory.create(
                        id = 2,
                        assetType = AssetType.BOND,
                        targetPercentage = BigDecimal("50"),
                        isActive = true,
                    )
                val currentTargets = listOf(existingStock, existingBond)

                val newTargets =
                    mapOf(
                        AssetType.STOCK to BigDecimal("70"),
                        AssetType.BOND to BigDecimal("30"),
                    )

                every { investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc() } returns currentTargets

                service.saveTargets(newTargets)

                Then("should update existing targets") {
                    existingStock.targetPercentage shouldBe BigDecimal("70")
                    existingBond.targetPercentage shouldBe BigDecimal("30")
                }
            }
        }

        Given("existing target to be deactivated") {
            When("saving targets with zero percentage for existing asset type") {
                val existingStock =
                    InvestmentTargetFactory.create(
                        id = 1,
                        assetType = AssetType.STOCK,
                        targetPercentage = BigDecimal("50"),
                        isActive = true,
                    )
                val currentTargets = listOf(existingStock)

                val newTargets =
                    mapOf(
                        AssetType.STOCK to BigDecimal.ZERO,
                        AssetType.BOND to BigDecimal("100"),
                    )

                every { investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc() } returns currentTargets
                every { investmentTargetRepository.save(any()) } returnsArgument 0

                service.saveTargets(newTargets)

                Then("should deactivate the target with zero percentage") {
                    existingStock.isActive shouldBe false
                }

                Then("should create new target for bond") {
                    verify { investmentTargetRepository.save(any()) }
                }
            }
        }

        Given("existing target not in new targets") {
            When("saving targets without existing asset type") {
                val existingStock =
                    InvestmentTargetFactory.create(
                        id = 1,
                        assetType = AssetType.STOCK,
                        targetPercentage = BigDecimal("50"),
                        isActive = true,
                    )
                val currentTargets = listOf(existingStock)

                val newTargets =
                    mapOf(
                        AssetType.BOND to BigDecimal("100"),
                    )

                every { investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc() } returns currentTargets
                every { investmentTargetRepository.save(any()) } returnsArgument 0

                service.saveTargets(newTargets)

                Then("should deactivate the missing target") {
                    existingStock.isActive shouldBe false
                }

                Then("should create new target for bond") {
                    verify { investmentTargetRepository.save(any()) }
                }
            }
        }

        Given("active investment targets in database") {
            When("getting all active targets") {
                val targets =
                    listOf(
                        InvestmentTargetFactory.create(
                            id = 1,
                            assetType = AssetType.STOCK,
                            targetPercentage = BigDecimal("60"),
                        ),
                        InvestmentTargetFactory.create(
                            id = 2,
                            assetType = AssetType.BOND,
                            targetPercentage = BigDecimal("40"),
                        ),
                    )
                every { investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc() } returns targets

                val result = service.getAllActiveTargets()

                Then("should return all active targets") {
                    result.size shouldBe 2
                    result[0].assetType shouldBe AssetType.STOCK
                    result[1].assetType shouldBe AssetType.BOND
                }

                Then("should call repository method") {
                    verify { investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc() }
                }
            }
        }

        Given("no active investment targets in database") {
            When("getting all active targets") {
                every { investmentTargetRepository.findAllByIsActiveTrueOrderByAssetTypeAsc() } returns emptyList()

                val result = service.getAllActiveTargets()

                Then("should return empty list") {
                    result.size shouldBe 0
                }
            }
        }

        Given("an existing active target for a specific asset type") {
            When("getting target by asset type") {
                val target =
                    InvestmentTargetFactory.create(
                        id = 1,
                        assetType = AssetType.STOCK,
                        targetPercentage = BigDecimal("60"),
                        isActive = true,
                    )
                every { investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.STOCK) } returns target

                val result = service.getTargetByType(AssetType.STOCK)

                Then("should return the target") {
                    result.assetType shouldBe AssetType.STOCK
                    result.targetPercentage.isEqual(60) shouldBe true
                }

                Then("should call repository method") {
                    verify { investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.STOCK) }
                }
            }
        }

        Given("no active target for a specific asset type") {
            When("getting target by asset type") {
                every { investmentTargetRepository.findByAssetTypeAndIsActiveTrue(AssetType.CRYPTOCURRENCY) } returns null

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.getTargetByType(AssetType.CRYPTOCURRENCY)
                    }
                }
            }
        }

        Given("valid targets for partial validation") {
            When("validating partial targets") {
                val targets =
                    mapOf(
                        AssetType.STOCK to BigDecimal("60"),
                        AssetType.BOND to BigDecimal("40"),
                    )

                val result = service.validate(targets)

                Then("should return valid result") {
                    result.isValid shouldBe true
                }

                Then("should calculate correct total") {
                    result.total shouldBe BigDecimal("100")
                }

                Then("should have no errors") {
                    result.errors.size shouldBe 0
                }
            }
        }

        Given("targets with invalid percentages") {
            When("validating targets with negative percentage") {
                val targets =
                    mapOf(
                        AssetType.STOCK to BigDecimal("-10"),
                        AssetType.BOND to BigDecimal("110"),
                    )

                val result = service.validate(targets)

                Then("should return invalid result") {
                    result.isValid shouldBe false
                }

                Then("should contain error messages") {
                    result.errors.size shouldBe 2
                }
            }
        }

        Given("targets not totaling 100%") {
            When("validating targets with incorrect total") {
                val targets =
                    mapOf(
                        AssetType.STOCK to BigDecimal("50"),
                        AssetType.BOND to BigDecimal("30"),
                    )

                val result = service.validate(targets)

                Then("should return invalid result") {
                    result.isValid shouldBe false
                }

                Then("should contain total error") {
                    result.errors.any { it.contains("100") } shouldBe true
                }
            }
        }

        Given("targets with percentages at boundaries") {
            When("validating targets with 0 and 100 percentages") {
                val targets =
                    mapOf(
                        AssetType.STOCK to BigDecimal.ZERO,
                        AssetType.BOND to BigDecimal("100"),
                    )

                val result = service.validate(targets)

                Then("should return valid result") {
                    result.isValid shouldBe true
                }

                Then("should calculate correct total") {
                    result.total shouldBe BigDecimal("100")
                }
            }
        }

        Given("empty targets map") {
            When("validating empty targets") {
                val targets = emptyMap<AssetType, BigDecimal>()

                val result = service.validate(targets)

                Then("should return invalid result") {
                    result.isValid shouldBe false
                }

                Then("should have total error") {
                    result.errors.any { it.contains("100") } shouldBe true
                }
            }
        }
    })
