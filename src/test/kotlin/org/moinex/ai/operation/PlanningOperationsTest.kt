/*
 * Filename: PlanningOperationsTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.operation

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.moinex.ai.operation.planning.ArchiveGoalOperation
import org.moinex.ai.operation.planning.CreateGoalOperation
import org.moinex.ai.operation.planning.CreateWishlistItemOperation
import org.moinex.ai.operation.planning.UpdateGoalOperation
import org.moinex.ai.operation.planning.UpdateWishlistItemOperation
import org.moinex.common.constant.Constants
import org.moinex.factory.CategoryFactory
import org.moinex.factory.goal.GoalFactory
import org.moinex.factory.wallet.WalletTypeFactory
import org.moinex.factory.wishlist.WishlistItemFactory
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.model.goal.Goal
import org.moinex.model.wishlist.WishlistItem
import org.moinex.service.goal.GoalService
import org.moinex.service.wallet.WalletService
import org.moinex.service.wishlist.WishlistService
import java.math.BigDecimal
import java.time.LocalDate

class PlanningOperationsTest :
    BehaviorSpec({
        val goalService = mockk<GoalService>(relaxed = true)
        val walletService = mockk<WalletService>()
        val wishlistService = mockk<WishlistService>(relaxed = true)
        val resolver = mockk<CategoryResolver>()
        val tech = CategoryFactory.create(id = 4, name = "Tech")
        val catalog = EntityCatalog(categories = listOf(tech))
        val goal = GoalFactory.create(id = 6, name = "Trip")
        val item = WishlistItemFactory.create(id = 8, title = "Laptop", category = tech)
        val future = LocalDate.now().plusMonths(8)

        every { goalService.getAllGoals() } returns listOf(goal)
        every { goalService.getGoalById(6) } returns goal
        every { walletService.getAllWalletTypes() } returns
            listOf(WalletTypeFactory.create(2, Constants.GOAL_DEFAULT_WALLET_TYPE_NAME))
        every { wishlistService.getAllItems() } returns listOf(item)
        every { wishlistService.getLinksForItem(8) } returns emptyList()

        Given("a request to create a goal") {
            val operation = CreateGoalOperation(goalService, walletService)
            val payload =
                operation.fromModelArguments(
                    JSONObject("""{"name":"House","targetAmount":50000,"amount":2000,"targetDate":"$future"}"""),
                    catalog,
                )

            Then("it is a standalone goal written through the service") {
                operation.validate(payload).isValid shouldBe true
                val saved = slot<Goal>()
                every { goalService.createGoal(capture(saved), any()) } returns 9
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.masterWallet shouldBe null
                saved.captured.targetBalance shouldBe BigDecimal("50000.00")
            }

            Then("a past date, a zero target or an initial balance above the target are refused") {
                operation.validate(payload.copy(targetDate = LocalDate.now().minusDays(1))).isValid shouldBe false
                operation.validate(payload.copy(targetBalance = BigDecimal.ZERO)).isValid shouldBe false
                operation.validate(payload.copy(initialBalance = BigDecimal("60000"))).isValid shouldBe false
            }
        }

        Given("a request that changes only the target date of a goal") {
            val operation = UpdateGoalOperation(goalService)
            val payload =
                operation.fromModelArguments(
                    JSONObject("""{"goalName":"trip","targetDate":"$future"}"""),
                    catalog,
                )

            Then("the rest is kept and the stored balance is passed through unchanged") {
                payload.targetBalance shouldBe goal.targetBalance
                val saved = slot<Goal>()
                every { goalService.updateGoal(capture(saved)) } returns Unit
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.balance shouldBe goal.balance
                saved.captured.targetDate shouldBe future
            }

            Then("an unknown goal is an error for the model and archive refuses it") {
                runCatching {
                    operation.fromModelArguments(JSONObject("""{"goalName":"nope"}"""), catalog)
                }.isFailure shouldBe true
                val archive = ArchiveGoalOperation(goalService)
                archive
                    .validate(archive.fromModelArguments(JSONObject("""{"goalName":"nope"}"""), catalog))
                    .isValid shouldBe false
                archive.apply(archive.fromModelArguments(JSONObject("""{"goalName":"Trip"}"""), catalog))
                verify { goalService.archiveGoal(6) }
            }
        }

        Given("a request to rename an archived goal") {
            val old = GoalFactory.create(id = 7, name = "Old Plan", isArchived = true)
            every { goalService.getAllGoals() } returns listOf(old, goal)
            every { goalService.getGoalById(7) } returns old
            val operation = UpdateGoalOperation(goalService)
            val payload =
                operation.fromModelArguments(JSONObject("""{"goalName":"old plan","name":"Old Plan 2"}"""), catalog)

            Then("it is found, renamed and the goal stays archived") {
                payload.goalId shouldBe 7
                val saved = slot<Goal>()
                every { goalService.updateGoal(capture(saved)) } returns Unit
                operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                saved.captured.isArchived shouldBe true
                saved.captured.name shouldBe "Old Plan 2"
            }

            Then("with an active and an archived goal of the same name the active one wins") {
                val twin = GoalFactory.create(id = 8, name = "Trip", isArchived = true)
                every { goalService.getAllGoals() } returns listOf(twin, goal)
                operation
                    .fromModelArguments(JSONObject("""{"goalName":"trip"}"""), catalog)
                    .goalId shouldBe 6
            }
        }

        Given("wishlist requests") {
            every { resolver.resolveOrCreate(any()) } answers { CategoryFactory.create(id = 99, name = firstArg()) }

            When("creating an item") {
                val operation = CreateWishlistItemOperation(wishlistService, resolver)
                val payload =
                    operation.fromModelArguments(
                        JSONObject("""{"title":"Camera","amount":3000,"categoryName":"tech","priority":"high"}"""),
                        catalog,
                    )

                Then("the category is resolved, the priority parsed and the item has no links") {
                    payload.categoryName shouldBe "Tech"
                    payload.priority shouldBe WishlistItemPriority.HIGH
                    val saved = slot<WishlistItem>()
                    every { wishlistService.createItem(capture(saved), any()) } returns 12
                    operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                    saved.captured.title shouldBe "Camera"
                }
            }

            When("changing only the price") {
                val operation = UpdateWishlistItemOperation(wishlistService, resolver)
                val payload =
                    operation.fromModelArguments(
                        JSONObject("""{"itemTitle":"laptop","amount":4200}"""),
                        catalog,
                    )

                Then("the title, status, priority and links are kept") {
                    payload.title shouldBe "Laptop"
                    operation.categoryNeeded(payload) shouldBe null
                    val saved = slot<WishlistItem>()
                    every { wishlistService.updateItem(capture(saved), any()) } returns Unit
                    operation.apply(payload).shouldBeInstanceOf<ApplyResult.Success>()
                    saved.captured.estimatedPrice.compareTo(BigDecimal("4200")) shouldBe 0
                    saved.captured.status shouldBe item.status
                    verify { wishlistService.getLinksForItem(8) }
                }
            }
        }
    })
