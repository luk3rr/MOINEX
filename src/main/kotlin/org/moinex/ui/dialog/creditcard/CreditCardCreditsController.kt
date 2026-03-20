/*
 * Filename: CreditCardCreditsController.kt (original filename: CreditCardCreditsController.java)
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
import org.moinex.common.constants.Constants
import org.moinex.common.constants.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.creditcard.CreditCardCredit
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class CreditCardCreditsController(
    private val creditCardService: CreditCardService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var creditCardCreditsTableView: TableView<CreditCardCredit>

    @FXML
    private lateinit var searchField: TextField

    private var creditCardCredits: List<CreditCardCredit> = emptyList()

    @FXML
    private fun initialize() {
        loadCreditCardCreditsFromDatabase()
        configureTableView()
        updateCreditCardCreditsTableView()

        searchField.textProperty().addListener { _, _, _ -> updateCreditCardCreditsTableView() }
    }

    @FXML
    private fun handleDelete() {
        // Still empty
    }

    @FXML
    private fun handleAdd() {
        WindowUtils.openModalWindow(
            Constants.ADD_CREDIT_CARD_CREDIT_FXML,
            preferencesService.translate(TranslationKeys.CREDITCARD_CREDITS_ADD_TITLE),
            springContext,
            { _: AddCreditCardCreditController -> },
            listOf(
                Runnable {
                    loadCreditCardCreditsFromDatabase()
                    updateCreditCardCreditsTableView()
                },
            ),
        )
    }

    @FXML
    private fun handleEdit() {
        // Still empty
    }

    @FXML
    private fun handleCancel() {
        (searchField.scene.window as Stage).close()
    }

    private fun loadCreditCardCreditsFromDatabase() {
        creditCardCredits = creditCardService.getAllCreditCardCredits()
    }

    private fun updateCreditCardCreditsTableView() {
        val similarTextOrId = searchField.text.lowercase()

        creditCardCreditsTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            creditCardCreditsTableView.items.setAll(creditCardCredits)
        } else {
            creditCardCredits
                .filter { c ->
                    val id = c.id.toString()
                    val type = c.type.name.lowercase()
                    val crcName = c.creditCard.name.lowercase()
                    val date = c.date.toString().lowercase()
                    val amount = c.amount.toString().lowercase()
                    val description = c.description?.lowercase()

                    id.contains(similarTextOrId) ||
                        type.contains(similarTextOrId) ||
                        crcName.contains(similarTextOrId) ||
                        date.contains(similarTextOrId) ||
                        amount.contains(similarTextOrId) ||
                        description?.contains(similarTextOrId) ?: true
                }.forEach { creditCardCreditsTableView.items.add(it) }
        }

        creditCardCreditsTableView.refresh()
    }

    private fun configureTableView() {
        val idColumn = createIdColumn()
        val descriptionColumn = createDescriptionColumn()
        val amountColumn = createAmountColumn()
        val dateColumn = createDateColumn()
        val crcColumn = createCreditCardColumn()
        val typeColumn = createTypeColumn()

        creditCardCreditsTableView.columns.addAll(
            idColumn,
            descriptionColumn,
            amountColumn,
            dateColumn,
            crcColumn,
            typeColumn,
        )
    }

    private fun createIdColumn(): TableColumn<CreditCardCredit, Int> =
        TableColumn<CreditCardCredit, Int>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_ID),
        ).apply {
            setCellValueFactory { param -> SimpleObjectProperty(param.value.id) }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }

    private fun createDescriptionColumn(): TableColumn<CreditCardCredit, String> =
        TableColumn<CreditCardCredit, String>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_DESCRIPTION),
        ).apply {
            setCellValueFactory { param -> SimpleStringProperty(param.value.description) }
        }

    private fun createAmountColumn(): TableColumn<CreditCardCredit, String> =
        TableColumn<CreditCardCredit, String>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_AMOUNT),
        ).apply {
            setCellValueFactory { param ->
                SimpleStringProperty(UIUtils.formatCurrency(param.value.amount))
            }
        }

    private fun createDateColumn(): TableColumn<CreditCardCredit, String> =
        TableColumn<CreditCardCredit, String>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_DATE),
        ).apply {
            setCellValueFactory { param ->
                SimpleStringProperty(UIUtils.formatDateForDisplay(param.value.date))
            }
        }

    private fun createCreditCardColumn(): TableColumn<CreditCardCredit, String> =
        TableColumn<CreditCardCredit, String>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_CREDIT_CARD),
        ).apply {
            setCellValueFactory { param -> SimpleStringProperty(param.value.creditCard.name) }
        }

    private fun createTypeColumn(): TableColumn<CreditCardCredit, String> =
        TableColumn<CreditCardCredit, String>(
            preferencesService.translate(TranslationKeys.CREDITCARD_TABLE_TYPE),
        ).apply {
            setCellValueFactory { param ->
                SimpleStringProperty(UIUtils.translateCreditCardCreditType(param.value.type))
            }
        }
}
