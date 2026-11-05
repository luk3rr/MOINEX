/*
 * Filename: ReviewSessionMatchTest.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.review

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.json.JSONObject
import org.moinex.ai.audit.ChangeSetAuditLogger
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.BaseOperation
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.EntityCatalog
import org.moinex.ai.operation.OperationDomain
import org.moinex.ai.operation.OperationRegistry
import org.moinex.ai.operation.OperationRisk
import org.moinex.ai.operation.ProposedChange
import org.moinex.ai.operation.ValidationResult
import org.moinex.service.PreferencesService
import org.moinex.service.matching.Match
import org.moinex.service.matching.MatchStatus
import org.moinex.service.matching.MatchTargetType
import java.math.BigDecimal
import java.time.LocalDate

class ReviewSessionMatchTest :
    BehaviorSpec({
        val operation =
            object : BaseOperation<String>() {
                override val name = "create_note"
                override val domain = OperationDomain.TRANSACTIONS
                override val risk = OperationRisk.LOW

                override fun modelDescription() = "create_note: test"

                override fun modelProperties() = emptyMap<String, JSONObject>()

                override fun fromModelArguments(
                    arguments: JSONObject,
                    catalog: EntityCatalog,
                ) = arguments.toString()

                override fun summary(payload: String) = payload

                override fun validate(payload: String) = ValidationResult.OK

                override fun confirmingPending(
                    payload: String,
                    existingTransactionId: Int,
                ) = if (existingTransactionId == STALE_ID) null else "confirm #$existingTransactionId"

                override fun execute(payload: String) = ApplyResult.Success(listOf(1), payload)
            }

        fun candidate(
            id: Int,
            status: MatchStatus = MatchStatus.PENDING,
        ) = Match(
            MatchTargetType.WALLET_TRANSACTION,
            id,
            status,
            0.9,
            listOf("same amount"),
            "Gym",
            LocalDate.of(2026, 10, 10),
            BigDecimal("99.90"),
        )

        fun review(): ReviewSession {
            val session =
                ReviewSession(
                    OperationRegistry(listOf(operation)),
                    mockk<ChangeSetAuditLogger>(relaxed = true),
                    mockk<PreferencesService>(relaxed = true),
                )
            val change =
                ProposedChange(
                    "a",
                    "create_note",
                    "confirm #1",
                    "confirm #1",
                    ChangeBadge.MATCHES_EXISTING,
                    emptyList(),
                    0.9,
                    matchCandidates =
                        listOf(
                            candidate(1),
                            candidate(2),
                            candidate(3, MatchStatus.CONFIRMED),
                            candidate(STALE_ID),
                        ),
                    targetId = 1,
                )
            session.add(ChangeSet("set", listOf(change)))
            return session
        }

        Given("a change matching two pending candidates") {
            val session = review()
            val item = session.items.single()

            Then("it starts on the target the tool chose") {
                item.targetIdProperty.get() shouldBe 1
            }

            When("the user picks the other pending candidate") {
                val changed = session.retarget(item, 2)

                Then("the payload is rebuilt for it and nothing is written") {
                    changed shouldBe true
                    item.targetIdProperty.get() shouldBe 2
                    item.payloadProperty.get() shouldBe "confirm #2"
                    item.isOpen shouldBe true
                }
            }

            When("the user picks the target it already has") {
                Then("nothing changes") {
                    session.retarget(item, 2) shouldBe false
                }
            }

            When("the user picks an already confirmed record") {
                Then("it is refused") {
                    session.retarget(item, 3) shouldBe false
                    item.targetIdProperty.get() shouldBe 2
                }
            }

            When("the operation can no longer confirm the chosen record") {
                Then("the previous target is kept") {
                    session.retarget(item, STALE_ID) shouldBe false
                    item.targetIdProperty.get() shouldBe 2
                    item.payloadProperty.get() shouldBe "confirm #2"
                }
            }

            When("the id is not among the candidates") {
                Then("it is refused") {
                    session.retarget(item, 99) shouldBe false
                }
            }
        }
    }) {
    private companion object {
        const val STALE_ID = 4
    }
}
