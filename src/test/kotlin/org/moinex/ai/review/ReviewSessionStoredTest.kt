/*
 * Filename: ReviewSessionStoredTest.kt
 * Created on: November 7, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.review

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.moinex.ai.audit.ChangeSetAuditLogger
import org.moinex.ai.history.RawPayload
import org.moinex.ai.operation.ApplyResult
import org.moinex.ai.operation.BaseOperation
import org.moinex.ai.operation.ChangeBadge
import org.moinex.ai.operation.ChangeSet
import org.moinex.ai.operation.ChangeState
import org.moinex.ai.operation.DraftValues
import org.moinex.ai.operation.EntityCatalog
import org.moinex.ai.operation.OperationDomain
import org.moinex.ai.operation.OperationRegistry
import org.moinex.ai.operation.OperationRisk
import org.moinex.ai.operation.ProposedChange
import org.moinex.ai.operation.ValidationResult
import org.moinex.service.PreferencesService
import org.moinex.service.ai.StoredChangeSet
import org.moinex.service.ai.StoredChangeState
import java.time.LocalDateTime

class ReviewSessionStoredTest :
    BehaviorSpec({
        isolationMode = IsolationMode.InstancePerLeaf

        val applied = mutableListOf<String>()

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

                override fun summary(payload: String) = "note $payload"

                override fun validate(payload: String) = ValidationResult.OK

                override fun toDraft(payload: String) = DraftValues(name, mapOf("text" to payload))

                override fun fromDraft(draft: DraftValues) = draft.fields["text"] as? String

                override fun execute(payload: String): ApplyResult.Success {
                    applied += payload
                    return ApplyResult.Success(listOf(applied.size), payload)
                }
            }
        val registry = OperationRegistry(listOf(operation))
        val preferences = mockk<PreferencesService>().also { every { it.translate(any()) } answers { firstArg() } }
        val review = ReviewSession(registry, mockk<ChangeSetAuditLogger>(relaxed = true), preferences)
        val moment = LocalDateTime.of(2026, 11, 6, 10, 0)

        fun change(
            id: String,
            payload: Any = id,
            operationName: String = "create_note",
            badge: ChangeBadge = ChangeBadge.NEW,
        ) = ProposedChange(id, operationName, payload, "note $id", badge, emptyList(), 0.9)

        fun stored(
            state: ChangeState,
            payload: Any,
            message: String? = null,
        ) = StoredChangeState(state, payload, message, moment.takeIf { state == ChangeState.APPROVED })

        Given("a stored answer with a pending, an edited, an approved and a rejected proposal") {
            val set =
                StoredChangeSet(
                    messageIndex = 1,
                    changeSet = ChangeSet("set-1", listOf(change("a"), change("b"), change("c"), change("d"))),
                    states =
                        mapOf(
                            "a" to stored(ChangeState.PENDING, "a"),
                            "b" to stored(ChangeState.EDITED, "b edited"),
                            "c" to stored(ChangeState.APPROVED, "c"),
                            "d" to stored(ChangeState.REJECTED, "d"),
                        ),
                )
            val events = mutableListOf<String>()
            review.onItemChanged = { events += "${it.change.id}:${it.state}" }

            When("it is loaded") {
                review.load(listOf(set))
                val byId = review.items.associateBy { it.change.id }

                Then("each proposal comes back with its state and the payload in use") {
                    byId.getValue("a").state shouldBe ChangeState.PENDING
                    byId.getValue("b").state shouldBe ChangeState.EDITED
                    byId.getValue("b").payloadProperty.get() shouldBe "b edited"
                    byId.getValue("c").state shouldBe ChangeState.APPROVED
                    byId.getValue("d").state shouldBe ChangeState.REJECTED
                }

                Then("they all keep the answer they came from") {
                    review.items.map { it.changeSetId }.toSet() shouldBe setOf("set-1")
                }

                Then("only the pending and the edited ones can still be decided") {
                    review.items.map { it.isOpen } shouldBe listOf(true, true, false, false)
                    review.openCount() shouldBe 2
                }

                Then("nothing is read only, and approving everything touches only the open ones") {
                    review.items.map { it.readOnly } shouldBe listOf(false, false, false, false)
                    review.approve(review.items.toList())
                    applied shouldBe listOf("a", "b edited")
                    byId.getValue("c").state shouldBe ChangeState.APPROVED
                    byId.getValue("d").state shouldBe ChangeState.REJECTED
                }

                Then("a rejected proposal can be taken back, as in a conversation that was never closed") {
                    review.undoReject(byId.getValue("d"))
                    byId.getValue("d").state shouldBe ChangeState.PENDING
                    byId.getValue("d").isOpen shouldBe true
                    events shouldBe listOf("d:PENDING")
                }

                Then("loading is not a decision, so nothing is reported to be stored") {
                    events shouldBe emptyList()
                }

                Then("the open ones start selected, the closed ones do not") {
                    review.items.map { it.selectedProperty.get() } shouldBe listOf(true, true, false, false)
                }
            }
        }

        Given("a loaded pending proposal") {
            val events = mutableListOf<String>()
            review.load(
                listOf(
                    StoredChangeSet(
                        0,
                        ChangeSet("set-2", listOf(change("p"))),
                        mapOf("p" to stored(ChangeState.PENDING, "p")),
                    ),
                ),
            )
            review.onItemChanged = { events += "${it.change.id}:${it.state}:${it.payloadProperty.get()}" }
            val item = review.items.single()

            When("the user approves it") {
                review.approve(listOf(item))

                Then("the decision is reported with the payload that was applied") {
                    events.last() shouldBe "p:APPROVED:p"
                    applied shouldBe listOf("p")
                }
            }

            When("the user rejects it") {
                review.reject(item)

                Then("the rejection is reported") { events shouldBe listOf("p:REJECTED:p") }
            }

            When("the user edits it") {
                review.applyDraft(item, DraftValues("create_note", mapOf("text" to "p2")))

                Then("the edited payload and state are reported") {
                    events shouldBe listOf("p:PENDING:p2", "p:EDITED:p2")
                }
            }
        }

        Given("a proposal of a live answer") {
            val events = mutableListOf<String>()
            review.onItemChanged = { events += "${it.change.id}:${it.state}" }
            review.add(ChangeSet("set-3", listOf(change("n"))))

            When("it is rejected and the rejection is undone in the same screen") {
                review.reject(review.items.single())
                review.undoReject(review.items.single())

                Then("it is not read only and both changes are reported") {
                    review.items.single().readOnly shouldBe false
                    events shouldBe listOf("n:REJECTED", "n:PENDING")
                }
            }
        }

        Given("a stored proposal that can no longer be rebuilt") {
            val raw = RawPayload("{\"a\":1}")
            review.load(
                listOf(
                    StoredChangeSet(
                        0,
                        ChangeSet("set-4", listOf(change("g", raw, operationName = "operation_from_the_future"))),
                        mapOf("g" to stored(ChangeState.ERROR, raw, "unreadable")),
                    ),
                ),
            )
            val item = review.items.single()

            Then("it is shown from the stored summary, read only, and nothing in the panel calls its operation") {
                item.readOnly shouldBe true
                item.isOpen shouldBe false
                review.summaryOf(item) shouldBe "note g"
                review.validationOf(item).errors shouldBe emptyList()
                review.previewOf(item) shouldBe null
                review.draftOf(item) shouldBe null
                review.draftSpecOf(item) shouldBe emptyList()
                item.messageProperty.get() shouldBe "unreadable"
            }
        }
    })
