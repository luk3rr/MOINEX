package org.moinex.ui.common

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.moinex.model.dto.MetricReason
import java.util.Properties

class FundamentalReasonTextTest :
    BehaviorSpec({
        fun bundle(name: String): Properties =
            Properties().apply {
                FundamentalReasonTextTest::class.java
                    .getResourceAsStream("/i18n/$name.properties")!!
                    .reader(Charsets.UTF_8)
                    .use { load(it) }
            }

        Given("every reason a metric can have no value") {
            val keys = MetricReason.entries.map { FundamentalReasonText.translationKey(it) }

            Then("each reason has its own translation key") {
                keys.toSet().size shouldBe MetricReason.entries.size
            }

            listOf("messages_pt_BR", "messages_en").forEach { name ->
                Then("the $name bundle has a text for each of them") {
                    val properties = bundle(name)
                    properties.stringPropertyNames() shouldContainAll keys
                    keys.forEach { properties.getProperty(it).shouldNotBeBlank() }
                }
            }
        }
    })
