/*
 * Filename: ArchivedCreditCardsController.kt (original filename: ArchivedCreditCardsController.java)
 * Created on: October 29, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 20/03/2026
 */

package org.moinex.ui.dialog.creditcard

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.creditcard.CreditCard
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.springframework.stereotype.Controller

@Controller
class ArchivedCreditCardsController(
    private val creditCardService: CreditCardService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var creditCardTableView: TableView<CreditCard>

    @FXML
    private lateinit var searchField: TextField

    private var archivedCreditCards: MutableList<CreditCard> = mutableListOf()

    @FXML
    private fun initialize() {
        loadArchivedCreditCardsFromDatabase()
        configureTableView()
        updateCreditCardTableView()

        searchField.textProperty().addListener { _, _, _ -> updateCreditCardTableView() }
    }

    @FXML
    private fun handleUnarchive() {
        val selectedCrc = creditCardTableView.selectionModel.selectedItem

        if (selectedCrc == null) {
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_NO_SELECTION_UNARCHIVE),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                preferencesService
                    .translate(TranslationKeys.CREDITCARD_DIALOG_UNARCHIVE_TITLE)
                    .replace("{0}", selectedCrc.name),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_UNARCHIVE_MESSAGE),
            )
        ) {
            runCatching {
                creditCardService.unarchiveCreditCard(selectedCrc.id!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_UNARCHIVED_TITLE),
                    preferencesService
                        .translate(TranslationKeys.CREDITCARD_DIALOG_UNARCHIVED_MESSAGE)
                        .replace("{0}", selectedCrc.name),
                )

                archivedCreditCards.remove(selectedCrc)
                updateCreditCardTableView()
            }.onFailure { e ->
                WindowUtils.showErrorDialog(
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_ERROR_UNARCHIVING_TITLE),
                    e.message ?: "Unknown error",
                )
            }
        }
    }

    @FXML
    private fun handleDelete() {
        val selectedCrc = creditCardTableView.selectionModel.selectedItem

        if (selectedCrc == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_NO_SELECTION_TITLE),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_NO_SELECTION_DELETE),
            )
            return
        }

        if (creditCardService.getDebtCountByCreditCard(selectedCrc.id!!) > 0) {
            WindowUtils.showErrorDialog(
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_HAS_DEBTS_TITLE),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_HAS_DEBTS_MESSAGE),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                preferencesService
                    .translate(TranslationKeys.CREDITCARD_DIALOG_DELETE_TITLE)
                    .replace("{0}", selectedCrc.name),
                preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_DELETE_MESSAGE),
            )
        ) {
            runCatching {
                creditCardService.deleteCreditCard(selectedCrc.id!!)

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_DELETED_TITLE),
                    preferencesService
                        .translate(TranslationKeys.CREDITCARD_DIALOG_DELETED_MESSAGE)
                        .replace("{0}", selectedCrc.name),
                )

                archivedCreditCards.remove(selectedCrc)
                updateCreditCardTableView()
            }.onFailure { e ->
                WindowUtils.showErrorDialog(
                    preferencesService.translate(TranslationKeys.CREDITCARD_DIALOG_ERROR_DELETING_TITLE),
                    e.message ?: "Unknown error",
                )
            }
        }
    }

    @FXML
    private fun handleCancel() {
        (searchField.scene.window as Stage).close()
    }

    private fun loadArchivedCreditCardsFromDatabase() {
        archivedCreditCards = creditCardService.getAllArchivedCreditCards().toMutableList()
    }

    private fun updateCreditCardTableView() {
        val similarTextOrId = searchField.text.lowercase()

        creditCardTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            creditCardTableView.items.setAll(archivedCreditCards)
        } else {
            archivedCreditCards
                .filter { c ->
                    val operatorName = c.operator.name.lowercase()
                    val id = c.id.toString()
                    val name = c.name.lowercase()

                    operatorName.contains(similarTextOrId) ||
                        id.contains(similarTextOrId) ||
                        name.contains(similarTextOrId)
                }.forEach { creditCardTableView.items.add(it) }
        }

        creditCardTableView.refresh()
    }

    private fun configureTableView() {
        val idColumn = createIdColumn()
        val crcColumn = createCreditCardColumn()
        val operatorColumn = createOperatorColumn()
        val numOfDebtsColumn = createDebtsColumn()

        creditCardTableView.columns.addAll(idColumn, crcColumn, operatorColumn, numOfDebtsColumn)
    }

    private fun createIdColumn(): TableColumn<CreditCard, Int> =
        TableColumn<CreditCard, Int>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_ID),
        ).apply {
            setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }

    private fun createCreditCardColumn(): TableColumn<CreditCard, String> =
        TableColumn<CreditCard, String>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_CREDIT_CARD),
        ).apply {
            setCellValueFactory { param -> SimpleStringProperty(param.value.name) }
        }

    private fun createOperatorColumn(): TableColumn<CreditCard, String> =
        TableColumn<CreditCard, String>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_OPERATOR),
        ).apply {
            setCellValueFactory { param -> SimpleStringProperty(param.value.operator.name) }
        }

    private fun createDebtsColumn(): TableColumn<CreditCard, Int> =
        TableColumn<CreditCard, Int>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_ASSOCIATED_DEBTS),
        ).apply {
            setCellValueFactory { param ->
                SimpleObjectProperty(creditCardService.getDebtCountByCreditCard(param.value.id!!))
            }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }
}
