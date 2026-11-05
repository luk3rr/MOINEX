package org.moinex.ai.audit

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ChangeSetAuditLoggerTest :
    BehaviorSpec({
        val logger = ChangeSetAuditLogger()

        Given("an approved operation") {
            Then("the line has the agreed fields") {
                logger.format(
                    "cs-1",
                    "ch-2",
                    "create_wallet_transaction",
                    AuditState.APPROVED,
                    listOf(55, 56),
                    "Market",
                ) shouldBe
                    "changeSetId=cs-1 changeId=ch-2 op=create_wallet_transaction state=APPROVED ids=55,56 " +
                    "summary=\"Market\""
            }
        }

        Given("a summary with quotes, line breaks and a long text") {
            val line =
                logger.format(
                    "cs",
                    "ch",
                    "op",
                    AuditState.ERROR,
                    emptyList(),
                    "He said \"hi\"\n" + "x".repeat(200),
                )

            Then("it stays on one line, without double quotes inside, and is truncated") {
                line.contains('\n') shouldBe false
                line.substringAfter("summary=\"").dropLast(1).contains('"') shouldBe false
                (line.length < 300) shouldBe true
            }
        }
    })
