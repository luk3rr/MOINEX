package org.moinex.ai.operation

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.json.JSONObject

class OperationRegistryTest :
    BehaviorSpec({
        fun operation(
            operationName: String,
            operationDomain: OperationDomain = OperationDomain.TRANSACTIONS,
        ) = object : BaseOperation<String>() {
            override val name = operationName
            override val domain = operationDomain
            override val risk = OperationRisk.LOW

            override fun modelDescription() = "$operationName: test"

            override fun modelProperties() = emptyMap<String, JSONObject>()

            override fun fromModelArguments(
                arguments: JSONObject,
                catalog: EntityCatalog,
            ) = arguments.toString()

            override fun summary(payload: String) = payload

            override fun validate(payload: String) = ValidationResult.OK

            override fun execute(payload: String) = ApplyResult.Success(emptyList(), payload)
        }

        Given("operations with distinct names") {
            val registry =
                OperationRegistry(
                    listOf(
                        operation("create_a"),
                        operation("create_b", OperationDomain.CREDIT_CARD),
                    ),
                )

            Then("they are found by name and by domain") {
                registry.find("create_a")?.name shouldBe "create_a"
                registry.find("missing") shouldBe null
                registry.byDomain(OperationDomain.CREDIT_CARD).map { it.name } shouldContainExactlyInAnyOrder
                    listOf("create_b")
                registry.all().size shouldBe 2
            }
        }

        Given("two operations with the same name") {
            Then("the registry refuses them") {
                shouldThrow<IllegalArgumentException> {
                    OperationRegistry(
                        listOf(operation("same"), operation("same")),
                    )
                }
            }
        }

        Given("an operation whose name suggests deleting") {
            Then("the registry refuses it") {
                listOf("delete_wallet", "remove_category", "purge_all").forEach { forbidden ->
                    shouldThrow<IllegalArgumentException> { OperationRegistry(listOf(operation(forbidden))) }
                }
            }
        }
    })
