package org.moinex.ai.review

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.moinex.ai.audit.AuditState
import org.moinex.ai.audit.ChangeSetAuditLogger
import org.moinex.ai.operation.AppOperation
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

class ReviewSessionTest :
    BehaviorSpec({
        val applied = mutableListOf<String>()

        val operation: AppOperation<String> =
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

                override fun validate(payload: String) =
                    if (payload.isBlank()) ValidationResult.error("empty") else ValidationResult.OK

                override fun toDraft(payload: String) = DraftValues(name, mapOf("text" to payload))

                override fun fromDraft(draft: DraftValues) = draft.fields["text"] as? String

                override fun execute(payload: String): ApplyResult.Success {
                    check(payload != "boom") { "service refused" }
                    applied += payload
                    return ApplyResult.Success(listOf(applied.size), payload)
                }
            }

        val registry = OperationRegistry(listOf(operation))
        val audit = mockk<ChangeSetAuditLogger>(relaxed = true)

        fun change(
            id: String,
            payload: String,
            badge: ChangeBadge = ChangeBadge.NEW,
            dependsOn: List<String> = emptyList(),
        ) = ProposedChange(id, "create_note", payload, "note $payload", badge, emptyList(), 0.9, dependsOn)

        fun session(vararg changes: ProposedChange) =
            ReviewSession(registry, audit).also { it.add(ChangeSet("set-1", changes.toList())) }

        Given("a change set with new, matching, duplicate and low confidence changes") {
            val review =
                session(
                    change("a", "a"),
                    change("b", "b", ChangeBadge.MATCHES_EXISTING),
                    change("c", "c", ChangeBadge.POSSIBLE_DUPLICATE),
                    change("d", "d", ChangeBadge.LOW_CONFIDENCE),
                )

            Then("only the safe ones start selected") {
                review.items.map { it.selectedProperty.get() } shouldBe listOf(true, true, false, false)
            }

            Then("all start pending") {
                review.counters().pending shouldBe 4
            }
        }

        Given("a batch where the middle change fails to apply") {
            applied.clear()
            val review = session(change("a", "a"), change("b", "boom"), change("c", "c"))

            When("approving all of them") {
                review.approve(review.items.toList())

                Then("the failure is isolated: the others are applied and the failing one shows its message") {
                    applied shouldBe listOf("a", "c")
                    review.items.map { it.state } shouldBe
                        listOf(ChangeState.APPROVED, ChangeState.ERROR, ChangeState.APPROVED)
                    review.items[1].messageProperty.get() shouldBe "service refused"
                    review.counters().approved shouldBe 2
                    review.counters().error shouldBe 1
                }

                Then("every decision is audited with its change set and operation") {
                    verify(exactly = 1) {
                        audit.record("set-1", "a", "create_note", AuditState.APPROVED, any(), any())
                    }
                    verify(exactly = 1) {
                        audit.record("set-1", "b", "create_note", AuditState.ERROR, emptyList(), "service refused")
                    }
                }
            }
        }

        Given("a change rejected and then restored") {
            val review = session(change("a", "a"))

            When("rejecting") {
                review.reject(review.items[0])

                Then("it leaves the open list, is deselected and is audited") {
                    review.items[0].state shouldBe ChangeState.REJECTED
                    review.items[0].isOpen shouldBe false
                    review.items[0].selectedProperty.get() shouldBe false
                    review.openCount() shouldBe 0
                    verify { audit.record("set-1", "a", "create_note", AuditState.REJECTED, emptyList(), any()) }
                }

                Then("approving a rejected change does nothing") {
                    applied.clear()
                    review.approve(listOf(review.items[0]))
                    applied shouldBe emptyList()
                }

                Then("the rejection can be undone") {
                    review.undoReject(review.items[0])
                    review.items[0].state shouldBe ChangeState.PENDING
                }
            }
        }

        Given("a change edited in the draft editor") {
            applied.clear()
            val review = session(change("a", "a"))

            When("saving valid draft values") {
                val ok = review.applyDraft(review.items[0], DraftValues("create_note", mapOf("text" to "edited")))

                Then("the payload is replaced, the state is edited and approving applies the edit") {
                    ok shouldBe true
                    review.items[0].state shouldBe ChangeState.EDITED
                    review.summaryOf(review.items[0]) shouldBe "note edited"
                    review.approve(listOf(review.items[0]))
                    applied shouldBe listOf("edited")
                }
            }

            When("the draft values cannot make a payload") {
                val other = session(change("z", "z"))
                val ok = other.applyDraft(other.items[0], DraftValues("create_note", mapOf("text" to 5)))

                Then("nothing changes") {
                    ok shouldBe false
                    other.items[0].state shouldBe ChangeState.PENDING
                }
            }
        }

        Given("a change that is invalid as proposed") {
            val review = session(change("a", " "))

            Then("it reports the validation errors and fails when approved") {
                review.validationOf(review.items[0]).errors shouldBe listOf("empty")
                review.approve(review.items.toList())
                review.items[0].state shouldBe ChangeState.ERROR
            }
        }

        Given("a change that depends on another") {
            applied.clear()
            val review = session(change("child", "child", dependsOn = listOf("parent")), change("parent", "parent"))

            When("both are approved together") {
                review.approve(review.items.toList())

                Then("the one depended on is applied first") {
                    applied shouldBe listOf("parent", "child")
                }
            }
        }

        Given("an error that the user fixed") {
            applied.clear()
            val review = session(change("a", "boom"))
            review.approve(review.items.toList())

            When("editing it and approving again") {
                review.applyDraft(review.items[0], DraftValues("create_note", mapOf("text" to "fixed")))
                review.approve(review.items.toList())

                Then("it is applied") {
                    review.items[0].state shouldBe ChangeState.APPROVED
                    applied shouldBe listOf("fixed")
                }
            }
        }

        Given("the same proposal arriving twice, as when the model resends an item") {
            val review = session(change("a", "same"))

            When("a second change set carries an identical open proposal and a different one") {
                review.add(ChangeSet("set-2", listOf(change("a2", "same"), change("b2", "other"))))

                Then("only the new one is added") {
                    review.items.map { it.change.id } shouldBe listOf("a", "b2")
                }
            }

            When("the first one was already approved and the same payload comes again") {
                review.approve(listOf(review.items[0]))
                review.add(ChangeSet("set-3", listOf(change("a3", "same"))))

                Then("it is accepted again, since the earlier one is no longer open") {
                    review.items.map { it.change.id } shouldBe listOf("a", "b2", "a3")
                }
            }
        }
    })
