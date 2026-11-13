/*
 * Filename: ImportMessagesTest.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.imports

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.common.constant.TranslationKeys
import org.moinex.dataexchange.spec.Problem
import org.moinex.dataexchange.spec.ProblemCodes
import org.moinex.dataexchange.spec.Severity
import java.util.Properties

class ImportMessagesTest :
    BehaviorSpec({
        val codes =
            listOf(
                ProblemCodes.NOT_MOINEX_FILE,
                ProblemCodes.UNKNOWN_LAYOUT,
                ProblemCodes.MISSING_SHEET_OR_COLUMN,
                ProblemCodes.FILE_TOO_LARGE,
                ProblemCodes.INVALID_VALUE,
                ProblemCodes.UNRESOLVED_REFERENCE,
                ProblemCodes.FORMULA_CELL,
                ProblemCodes.DOMAIN_RULE,
                ProblemCodes.ADJUSTED_VALUE,
                ProblemCodes.UNKNOWN_COLUMN,
            )

        listOf("messages_en", "messages_pt_BR").forEach { bundle ->
            Given("the $bundle bundle") {
                val props = Properties()
                ImportMessagesTest::class.java
                    .getResourceAsStream(
                        "/i18n/$bundle.properties",
                    )!!
                    .reader(Charsets.UTF_8)
                    .use(props::load)

                Then("every data exchange translation key has a text") {
                    TranslationKeys::class.java.declaredFields
                        .filter { it.name.startsWith("DATA_EXCHANGE_") && !it.name.endsWith("_PREFIX") }
                        .map { it.get(null) as String }
                        .filter { props.getProperty(it).isNullOrBlank() }
                        .shouldBe(emptyList())
                }

                Then("every problem code has a message") {
                    codes.forEach { code ->
                        val key = Problem("e", 1, null, code, Severity.ERROR).i18nKey
                        props.getProperty(key).isNullOrBlank() shouldBe false
                    }
                }
            }
        }
    })
