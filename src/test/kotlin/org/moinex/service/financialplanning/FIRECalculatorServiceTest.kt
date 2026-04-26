package org.moinex.service.financialplanning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.moinex.factory.financialplanning.FIRECalculatorSettingsFactory
import org.moinex.repository.financialplanning.FIRECalculatorSettingsRepository
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class FIRECalculatorServiceTest :
    BehaviorSpec({
        val repository = mockk<FIRECalculatorSettingsRepository>()
        val service = FIRECalculatorService(repository)

        afterContainer { clearAllMocks(answers = true) }

        Given("a net worth already above the FIRE target") {
            val settings =
                FIRECalculatorSettingsFactory.create(
                    currentNetWorth = BigDecimal("2000000.00"),
                    monthlyExpense = BigDecimal("5000.00"),
                    withdrawalRate = BigDecimal("4.00"),
                )

            When("calculating the projection") {
                val result = service.calculate(settings)

                Then("monthsToFire should be 0") {
                    result.monthsToFire shouldBe 0
                }

                Then("fireDate should be today") {
                    result.fireDate shouldBe LocalDate.now()
                }

                Then("ageAtFire should equal current age") {
                    result.ageAtFire shouldBe settings.currentAge
                }

                Then("dataPoints should contain the current net worth at month 0") {
                    result.dataPoints.first() shouldBe (0 to settings.currentNetWorth)
                }
            }
        }

        Given("a typical convergence scenario with 10% annual return") {
            val settings =
                FIRECalculatorSettingsFactory.create(
                    currentNetWorth = BigDecimal("100000.00"),
                    monthlyContribution = BigDecimal("3000.00"),
                    annualReturnRate = BigDecimal("10.00"),
                    monthlyExpense = BigDecimal("5000.00"),
                    withdrawalRate = BigDecimal("4.00"),
                    currentAge = 30,
                )

            When("calculating the projection") {
                val result = service.calculate(settings)

                Then("should converge before 50 years") {
                    result.monthsToFire shouldNotBe null
                    result.monthsToFire!! shouldBeGreaterThanOrEqualTo 1
                }

                Then("fireDate should be in the future") {
                    result.fireDate!! shouldBeGreaterThanOrEqualTo LocalDate.now()
                }

                Then("projected net worth at FIRE should be >= FIRE target") {
                    result.projectedNetWorthAtFire!! shouldBeGreaterThanOrEqualTo result.fireTarget
                }

                Then("dataPoints should have MAX_MONTHS entries") {
                    result.dataPoints.size shouldBe 600
                }

                Then("ageAtFire should be greater than current age") {
                    result.ageAtFire!! shouldBeGreaterThanOrEqualTo settings.currentAge
                }
            }
        }

        Given("a scenario that does not converge in 50 years") {
            val settings =
                FIRECalculatorSettingsFactory.create(
                    currentNetWorth = BigDecimal("1000.00"),
                    monthlyContribution = BigDecimal("10.00"),
                    annualReturnRate = BigDecimal("0.01"),
                    monthlyExpense = BigDecimal("50000.00"),
                    withdrawalRate = BigDecimal("4.00"),
                )

            When("calculating the projection") {
                val result = service.calculate(settings)

                Then("monthsToFire should be null") {
                    result.monthsToFire shouldBe null
                }

                Then("fireDate should be null") {
                    result.fireDate shouldBe null
                }

                Then("ageAtFire should be null") {
                    result.ageAtFire shouldBe null
                }

                Then("dataPoints should still contain all 600 months") {
                    result.dataPoints.size shouldBe 600
                }
            }
        }

        Given("an annual return rate of 0 percent") {
            val settings =
                FIRECalculatorSettingsFactory.create(
                    currentNetWorth = BigDecimal("0.00"),
                    monthlyContribution = BigDecimal("2000.00"),
                    annualReturnRate = BigDecimal("0.00"),
                    monthlyExpense = BigDecimal("3000.00"),
                    withdrawalRate = BigDecimal("4.00"),
                )

            When("calculating the projection") {
                val result = service.calculate(settings)

                Then("projection should grow linearly by monthly contribution") {
                    result.monthsToFire shouldNotBe null
                    // fireTarget = 3000 * 12 / 0.04 = 900000; at 2000/month = 450 months
                    result.monthsToFire shouldBe 450
                }
            }
        }

        Given("a monthly contribution of 0") {
            val settings =
                FIRECalculatorSettingsFactory.create(
                    currentNetWorth = BigDecimal("500000.00"),
                    monthlyContribution = BigDecimal("0.00"),
                    annualReturnRate = BigDecimal("8.00"),
                    monthlyExpense = BigDecimal("4000.00"),
                    withdrawalRate = BigDecimal("4.00"),
                )

            When("calculating the projection") {
                val result = service.calculate(settings)

                Then("projection should depend solely on compound growth of principal") {
                    // fireTarget = 4000 * 12 / 0.04 = 1200000; need 500000 to grow to 1200000 at 8% annual
                    result.monthsToFire shouldNotBe null
                }
            }
        }

        Given("invalid settings with monthly expense equal to 0") {
            val settings =
                FIRECalculatorSettingsFactory.create(
                    monthlyExpense = BigDecimal("0.00"),
                )

            When("calculating the projection") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> { service.calculate(settings) }
                }
            }
        }

        Given("invalid settings with withdrawal rate equal to 0") {
            val settings =
                FIRECalculatorSettingsFactory.create(
                    withdrawalRate = BigDecimal("0.00"),
                )

            When("calculating the projection") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> { service.calculate(settings) }
                }
            }
        }

        Given("invalid settings with negative current net worth") {
            val settings =
                FIRECalculatorSettingsFactory.create(
                    currentNetWorth = BigDecimal("-1.00"),
                )

            When("calculating the projection") {
                Then("should throw IllegalArgumentException") {
                    shouldThrow<IllegalArgumentException> { service.calculate(settings) }
                }
            }
        }

        Given("valid settings to be saved") {
            val settings = FIRECalculatorSettingsFactory.create()

            When("saving the settings") {
                every { repository.save(settings) } returns settings

                service.saveSettings(settings)

                Then("should call repository save exactly once") {
                    verify(exactly = 1) { repository.save(settings) }
                }
            }
        }

        Given("invalid settings with current age equal to 0") {
            val settings = FIRECalculatorSettingsFactory.create(currentAge = 0)

            When("saving the settings") {
                Then("should throw IllegalArgumentException without calling repository") {
                    shouldThrow<IllegalArgumentException> { service.saveSettings(settings) }
                    verify(exactly = 0) { repository.save(any()) }
                }
            }
        }

        Given("stored settings in the repository") {
            val settings = FIRECalculatorSettingsFactory.create()

            When("retrieving the settings") {
                every { repository.findById(1) } returns Optional.of(settings)

                val result = service.getSettings()

                Then("should return the stored settings") {
                    result shouldBe settings
                }
            }
        }

        Given("no settings stored in the repository") {
            When("retrieving the settings") {
                every { repository.findById(1) } returns Optional.empty()

                val result = service.getSettings()

                Then("should return null") {
                    result shouldBe null
                }
            }
        }
    })
