/*
 * Filename: DraftDialogLauncher.kt
 * Created on: November 5, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common.ai

import org.moinex.ai.operation.DraftValues
import org.moinex.ai.operation.transactions.CreateWalletTransactionOperation
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.WindowUtils
import org.moinex.service.PreferencesService
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component

/**
 * A dialog of the app that can be opened in draft mode: [applyDraft] fills its fields from the assistant's proposal
 * and, when the user saves, [onDraftSaved]'s callback receives the values instead of anything being written.
 */
interface DraftCapable {
    fun applyDraft(values: DraftValues)

    fun onDraftSaved(callback: (DraftValues) -> Unit)
}

/**
 * Opens the real dialog of the app that matches an operation, in draft mode. An operation with no such dialog is
 * left to the fallback editor, so [open] tells the caller whether it handled the draft.
 */
@Component
class DraftDialogLauncher(
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    /**
     * @return true when a dialog was shown, whether or not the user saved it; false when no dialog fits [draft]
     */
    fun open(
        draft: DraftValues,
        onSaved: (DraftValues) -> Unit,
    ): Boolean {
        val fxml = fxmlFor(draft) ?: return false

        var shown = false
        var saved: DraftValues? = null
        WindowUtils.openModalWindow(
            fxml,
            preferencesService.translate(TranslationKeys.AIPANEL_DRAFT_TITLE),
            springContext,
            { controller: DraftCapable ->
                shown = true
                controller.onDraftSaved { saved = it }
                controller.applyDraft(draft)
            },
        )

        saved?.let(onSaved)
        return shown
    }

    private fun fxmlFor(draft: DraftValues): String? =
        when (draft.operationName) {
            CreateWalletTransactionOperation.NAME ->
                if ((draft.fields["movementType"] as? String).equals("income", ignoreCase = true)) {
                    Files.ADD_INCOME_FXML
                } else {
                    Files.ADD_EXPENSE_FXML
                }
            else -> null
        }
}
