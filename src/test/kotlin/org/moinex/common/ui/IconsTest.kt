package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import org.kordamp.ikonli.Ikon
import org.kordamp.ikonli.IkonProvider
import java.lang.reflect.Modifier
import java.util.ServiceLoader

class IconsTest :
    BehaviorSpec({
        val literalShape = Regex("""^md[a-z]+-[a-z0-9_]+$""")

        val literals =
            Icons::class.java.declaredFields
                .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
                .map { it.also { field -> field.isAccessible = true }.get(null) as String }
                .filter { literalShape.matches(it) }

        Given("the icon constants") {
            When("counting them") {
                Then("there is one for each former interface PNG plus the missing synchronize icon") {
                    literals.size shouldBeGreaterThanOrEqual 70
                }
            }

            When("resolving each literal in the Ikonli packs on the classpath") {
                val available: Set<String> =
                    ServiceLoader
                        .load(IkonProvider::class.java)
                        .flatMap { provider -> provider.ikon.enumConstants.map { (it as Ikon).description } }
                        .toSet()
                val unresolved = literals.filterNot { it in available }

                Then("every literal exists") {
                    unresolved.shouldBeEmpty()
                }
            }
        }
    })
