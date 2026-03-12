package org.moinex.service.investment

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityNotFoundException
import org.moinex.factory.investment.BondFactory
import org.moinex.model.enums.BondType
import org.moinex.model.enums.InterestIndex
import org.moinex.model.enums.InterestType
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.service.BondInterestCalculationService
import org.moinex.service.BondService
import org.moinex.service.WalletService
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class BondServiceCRUDBondTest :
    BehaviorSpec({
        val bondRepository = mockk<BondRepository>()
        val bondOperationRepository = mockk<BondOperationRepository>()
        val walletService = mockk<WalletService>()
        val bondInterestCalculationService = mockk<BondInterestCalculationService>()

        val service =
            BondService(
                bondRepository,
                bondOperationRepository,
                walletService,
                bondInterestCalculationService,
            )

        afterContainer { clearAllMocks(answers = true) }

        Given("a valid bond with all required fields") {
            When("creating the bond") {
                val bond =
                    BondFactory.create(
                        name = "Test Bond",
                        symbol = "BOND001",
                        type = BondType.CDB,
                    )

                every { bondRepository.existsBySymbol("BOND001") } returns false
                every { bondRepository.save(any()) } returns bond

                service.createBond(bond)

                Then("should call repository save method") {
                    verify { bondRepository.save(any()) }
                }
            }
        }

        Given("a bond with null symbol") {
            When("creating the bond") {
                val bond =
                    BondFactory.create(
                        name = "Bond Without Symbol",
                        symbol = null,
                        type = BondType.LCI,
                    )

                every { bondRepository.save(any()) } returns bond

                service.createBond(bond)

                Then("should create successfully without symbol validation") {
                    verify { bondRepository.save(any()) }
                }
            }
        }

        Given("a bond with duplicate symbol") {
            When("creating the bond") {
                val bond =
                    BondFactory.create(
                        name = "Duplicate Bond",
                        symbol = "DUP001",
                        type = BondType.TREASURY_PREFIXED,
                    )

                every { bondRepository.existsBySymbol("DUP001") } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.createBond(bond)
                    }
                }
            }
        }

        Given("a valid bond to update") {
            When("updating the bond") {
                val originalBond =
                    BondFactory.create(
                        id = 1,
                        name = "Original Bond",
                        symbol = "ORIG001",
                        type = BondType.CDB,
                        issuer = "Bank A",
                        interestRate = BigDecimal("10.00"),
                    )
                val updatedBond =
                    BondFactory.create(
                        id = 1,
                        name = "Updated Bond",
                        symbol = "UPD001",
                        type = BondType.LCI,
                        issuer = "Bank B",
                        interestRate = BigDecimal("12.00"),
                    )

                every { bondRepository.findById(1) } returns Optional.of(originalBond)
                every { bondRepository.existsBySymbolAndIdNot("UPD001", 1) } returns false

                service.updateBond(updatedBond)

                Then("should update bond name") {
                    originalBond.name shouldBe "Updated Bond"
                }

                Then("should update bond symbol") {
                    originalBond.symbol shouldBe "UPD001"
                }

                Then("should update bond type") {
                    originalBond.type shouldBe BondType.LCI
                }

                Then("should update bond issuer") {
                    originalBond.issuer shouldBe "Bank B"
                }

                Then("should update bond interest rate") {
                    originalBond.interestRate shouldBe BigDecimal("12.00")
                }
            }
        }

        Given("a bond with updated symbol that already exists") {
            When("updating the bond") {
                val originalBond =
                    BondFactory.create(
                        id = 1,
                        name = "Bond A",
                        symbol = "BONDA",
                        type = BondType.CDB,
                    )
                val updatedBond =
                    BondFactory.create(
                        id = 1,
                        name = "Bond A",
                        symbol = "BONDB",
                        type = BondType.CDB,
                    )

                every { bondRepository.findById(1) } returns Optional.of(originalBond)
                every { bondRepository.existsBySymbolAndIdNot("BONDB", 1) } returns true

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.updateBond(updatedBond)
                    }
                }
            }
        }

        Given("a bond with all fields updated") {
            When("updating all bond fields") {
                val originalBond =
                    BondFactory.create(
                        id = 2,
                        name = "Original Name",
                        symbol = "ORIG",
                        type = BondType.CDB,
                        issuer = "Original Bank",
                        maturityDate = LocalDate.of(2025, 12, 31),
                        interestType = InterestType.FIXED,
                        interestIndex = null,
                        interestRate = BigDecimal("8.50"),
                    )
                val updatedBond =
                    BondFactory.create(
                        id = 2,
                        name = "New Name",
                        symbol = "NEW",
                        type = BondType.TREASURY_POSTFIXED,
                        issuer = "New Bank",
                        maturityDate = LocalDate.of(2026, 6, 30),
                        interestType = InterestType.FLOATING,
                        interestIndex = InterestIndex.SELIC,
                        interestRate = BigDecimal("15.75"),
                    )

                every { bondRepository.findById(2) } returns Optional.of(originalBond)
                every { bondRepository.existsBySymbolAndIdNot("NEW", 2) } returns false

                service.updateBond(updatedBond)

                Then("should update all fields correctly") {
                    originalBond.name shouldBe "New Name"
                    originalBond.symbol shouldBe "NEW"
                    originalBond.type shouldBe BondType.TREASURY_POSTFIXED
                    originalBond.issuer shouldBe "New Bank"
                    originalBond.maturityDate shouldBe LocalDate.of(2026, 6, 30)
                    originalBond.interestType shouldBe InterestType.FLOATING
                    originalBond.interestIndex shouldBe InterestIndex.SELIC
                    originalBond.interestRate shouldBe BigDecimal("15.75")
                }
            }
        }

        Given("a bond without operations") {
            When("deleting the bond") {
                val bond =
                    BondFactory.create(
                        id = 1,
                        name = "Bond to Delete",
                        type = BondType.CDB,
                    )

                every { bondRepository.findById(1) } returns Optional.of(bond)
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns emptyList()
                every { bondRepository.delete(bond) } returns Unit

                service.deleteBond(1)

                Then("should call repository delete method") {
                    verify { bondRepository.delete(bond) }
                }
            }
        }

        Given("a bond with associated operations") {
            When("deleting the bond") {
                val bond =
                    BondFactory.create(
                        id = 2,
                        name = "Bond with Operations",
                        type = BondType.LCI,
                    )

                every { bondRepository.findById(2) } returns Optional.of(bond)
                every { bondOperationRepository.findByBondOrderByOperationDateAsc(bond) } returns
                    listOf(mockk(), mockk())

                Then("should throw IllegalStateException") {
                    shouldThrow<IllegalStateException> {
                        service.deleteBond(2)
                    }
                }
            }
        }

        Given("a bond that does not exist") {
            When("trying to update the bond") {
                val updatedBond =
                    BondFactory.create(
                        id = 999,
                        name = "Non-existent Bond",
                        type = BondType.CDB,
                    )

                every { bondRepository.findById(999) } returns Optional.empty()

                Then("should throw NoSuchElementException") {
                    shouldThrow<EntityNotFoundException> {
                        service.updateBond(updatedBond)
                    }
                }
            }

            When("trying to delete the bond") {
                every { bondRepository.findById(999) } returns Optional.empty()

                Then("should throw NoSuchElementException") {
                    shouldThrow<EntityNotFoundException> {
                        service.deleteBond(999)
                    }
                }
            }

            When("trying to archive the bond") {
                every { bondRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.archiveBond(999)
                    }
                }
            }

            When("trying to unarchive the bond") {
                every { bondRepository.findById(999) } returns Optional.empty()

                Then("should throw EntityNotFoundException") {
                    shouldThrow<EntityNotFoundException> {
                        service.unarchiveBond(999)
                    }
                }
            }
        }

        Given("multiple bonds to create") {
            When("creating multiple bonds sequentially") {
                val bond1 =
                    BondFactory.create(
                        id = 1,
                        name = "Bond 1",
                        symbol = "BOND1",
                        type = BondType.CDB,
                    )
                val bond2 =
                    BondFactory.create(
                        id = 2,
                        name = "Bond 2",
                        symbol = "BOND2",
                        type = BondType.LCI,
                    )
                val bond3 =
                    BondFactory.create(
                        id = 3,
                        name = "Bond 3",
                        symbol = "BOND3",
                        type = BondType.TREASURY_PREFIXED,
                    )

                every { bondRepository.existsBySymbol("BOND1") } returns false
                every { bondRepository.existsBySymbol("BOND2") } returns false
                every { bondRepository.existsBySymbol("BOND3") } returns false
                every { bondRepository.save(any()) } returnsMany listOf(bond1, bond2, bond3)

                service.createBond(bond1)
                service.createBond(bond2)
                service.createBond(bond3)

                Then("should call save three times") {
                    verify(exactly = 3) { bondRepository.save(any()) }
                }
            }
        }

        Given("a bond with null symbol to update") {
            When("updating the bond") {
                val originalBond =
                    BondFactory.create(
                        id = 3,
                        name = "Bond Without Symbol",
                        symbol = null,
                        type = BondType.CDB,
                    )
                val updatedBond =
                    BondFactory.create(
                        id = 3,
                        name = "Updated Bond",
                        symbol = null,
                        type = BondType.LCI,
                    )

                every { bondRepository.findById(3) } returns Optional.of(originalBond)

                service.updateBond(updatedBond)

                Then("should update bond without symbol validation") {
                    originalBond.name shouldBe "Updated Bond"
                    originalBond.symbol shouldBe null
                }
            }
        }

        Given("a bond with interest details") {
            When("updating interest details") {
                val originalBond =
                    BondFactory.create(
                        id = 4,
                        name = "Bond with Interest",
                        type = BondType.CDB,
                        interestType = InterestType.FIXED,
                        interestRate = BigDecimal("10.00"),
                    )
                val updatedBond =
                    BondFactory.create(
                        id = 4,
                        name = "Bond with Interest",
                        type = BondType.CDB,
                        interestType = InterestType.FLOATING,
                        interestIndex = InterestIndex.CDI,
                        interestRate = BigDecimal("5.50"),
                    )

                every { bondRepository.findById(4) } returns Optional.of(originalBond)
                every { bondRepository.existsBySymbolAndIdNot(any(), 4) } returns false

                service.updateBond(updatedBond)

                Then("should update interest type") {
                    originalBond.interestType shouldBe InterestType.FLOATING
                }

                Then("should update interest index") {
                    originalBond.interestIndex shouldBe InterestIndex.CDI
                }

                Then("should update interest rate") {
                    originalBond.interestRate shouldBe BigDecimal("5.50")
                }
            }
        }

        Given("a non-archived bond") {
            When("archiving the bond") {
                val bond =
                    BondFactory.create(
                        id = 5,
                        name = "Bond to Archive",
                        type = BondType.CDB,
                        archived = false,
                    )

                every { bondRepository.findById(5) } returns Optional.of(bond)

                service.archiveBond(5)

                Then("should set archived flag to true") {
                    bond.archived shouldBe true
                }
            }

            When("unarchiving the bond") {
                val bond =
                    BondFactory.create(
                        id = 10,
                        name = "Non-archived Bond",
                        type = BondType.CDB,
                        archived = false,
                    )

                every { bondRepository.findById(10) } returns Optional.of(bond)

                service.unarchiveBond(10)

                Then("should remain non-archived") {
                    bond.archived shouldBe false
                }
            }
        }

        Given("an archived bond") {
            When("unarchiving the bond") {
                val bond =
                    BondFactory.create(
                        id = 6,
                        name = "Bond to Unarchive",
                        type = BondType.LCI,
                        archived = true,
                    )

                every { bondRepository.findById(6) } returns Optional.of(bond)

                service.unarchiveBond(6)

                Then("should set archived flag to false") {
                    bond.archived shouldBe false
                }
            }
        }

        Given("multiple bonds with different archive states") {
            When("archiving and unarchiving bonds") {
                val bond1 =
                    BondFactory.create(
                        id = 7,
                        name = "Bond 1",
                        type = BondType.CDB,
                        archived = false,
                    )
                val bond2 =
                    BondFactory.create(
                        id = 8,
                        name = "Bond 2",
                        type = BondType.LCI,
                        archived = true,
                    )

                every { bondRepository.findById(7) } returns Optional.of(bond1)
                every { bondRepository.findById(8) } returns Optional.of(bond2)

                service.archiveBond(7)
                service.unarchiveBond(8)

                Then("should archive the first bond") {
                    bond1.archived shouldBe true
                }

                Then("should unarchive the second bond") {
                    bond2.archived shouldBe false
                }
            }
        }

        Given("an already archived bond") {
            When("archiving the bond again") {
                val bond =
                    BondFactory.create(
                        id = 9,
                        name = "Already Archived Bond",
                        type = BondType.TREASURY_PREFIXED,
                        archived = true,
                    )

                every { bondRepository.findById(9) } returns Optional.of(bond)

                service.archiveBond(9)

                Then("should remain archived") {
                    bond.archived shouldBe true
                }
            }
        }
    })
