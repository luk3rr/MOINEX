/*
 * Filename: AppVersionTest.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull

class AppVersionTest :
    BehaviorSpec({
        Given("the packaged version.properties") {
            When("reading the installed version") {
                Then("it is a valid semantic version") {
                    AppVersion.installed().shouldNotBeNull()
                }
            }
        }
    })
