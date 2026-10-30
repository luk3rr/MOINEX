package org.moinex.common.ui

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.moinex.common.constant.TranslationKeys

class FieldValidationTest :
    BehaviorSpec({
        Given("the required rule") {
            Then("an empty field fails with the required message and a filled one passes") {
                FieldValidation.requiredError(true) shouldBe TranslationKeys.DIALOG_VALIDATION_REQUIRED
                FieldValidation.requiredError(false) shouldBe null
            }
        }

        Given("the number rules") {
            Then("a blank text is left to the required rule") {
                FieldValidation.numberError("", positive = true) shouldBe null
                FieldValidation.numberError(null, positive = false) shouldBe null
            }

            Then("a text that is not a number fails") {
                FieldValidation.numberError("12a", positive = false) shouldBe
                    TranslationKeys.DIALOG_VALIDATION_INVALID_NUMBER
            }

            Then("zero and negatives fail only when a positive number is required") {
                FieldValidation.numberError("0", positive = true) shouldBe TranslationKeys.DIALOG_VALIDATION_POSITIVE
                FieldValidation.numberError("-5", positive = true) shouldBe TranslationKeys.DIALOG_VALIDATION_POSITIVE
                FieldValidation.numberError("-5", positive = false) shouldBe null
                FieldValidation.numberError(" 10.50 ", positive = true) shouldBe null
            }
        }
    })
