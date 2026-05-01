/*
 * Filename: ArchivedWalletsController.kt (original filename: ArchivedWalletsController.java)
 * Created on: October 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 21/03/2026
 */

package org.moinex.ui.dialog.wallettransaction.view

import jakarta.persistence.EntityNotFoundException
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
import org.moinex.model.wallettransaction.Wallet
import org.moinex.service.PreferencesService
import org.moinex.service.wallet.WalletService
import org.springframework.stereotype.Controller

@Controller
class ArchivedWalletsController(
    private val walletService: WalletService,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var walletTableView: TableView<Wallet>

    @FXML
    private lateinit var searchField: TextField

    private var archivedWallets: List<Wallet> = emptyList()

    @FXML
    fun initialize() {
        loadArchivedWalletsFromDatabase()
        configureTableView()
        updateWalletTableView()

        searchField.textProperty().addListener { _, _, _ ->
            updateWalletTableView()
        }
    }

    @FXML
    private fun handleUnarchive() {
        val selectedWallet = walletTableView.selectionModel.selectedItem

        if (selectedWallet == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_TITLE),
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_UNARCHIVE_MESSAGE,
                ),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_UNARCHIVE_WALLET_TITLE) +
                    " " + selectedWallet.name,
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_UNARCHIVE_WALLET_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                walletService.unarchiveWallet(selectedWallet.id!!)
                archivedWallets = archivedWallets.filter { it != selectedWallet }
                updateWalletTableView()
            }.onFailure { e ->
                if (e is EntityNotFoundException) {
                    WindowUtils.showErrorDialog(
                        preferencesService.translate(
                            TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_UNARCHIVING_WALLET_TITLE,
                        ),
                        e.message ?: "Unknown error",
                    )
                } else {
                    throw e
                }
            }
        }
    }

    @FXML
    private fun handleDelete() {
        val selectedWallet = walletTableView.selectionModel.selectedItem

        if (selectedWallet == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_TITLE),
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_NO_WALLET_SELECTED_DELETE_MESSAGE,
                ),
            )
            return
        }

        if (walletService.getWalletTransactionAndTransferCountByWallet(selectedWallet.id!!) > 0) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_HAS_TRANSACTIONS_TITLE),
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_DIALOG_WALLET_HAS_TRANSACTIONS_MESSAGE,
                ),
            )
            return
        }

        if (WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_REMOVE_WALLET_TITLE) +
                    " " + selectedWallet.name,
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_DIALOG_REMOVE_WALLET_MESSAGE),
                preferencesService.bundle,
            )
        ) {
            runCatching {
                walletService.deleteWallet(selectedWallet.id!!)
                archivedWallets = archivedWallets.filter { it != selectedWallet }
                updateWalletTableView()
            }.onFailure { e ->
                when (e) {
                    is EntityNotFoundException, is IllegalStateException -> {
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(
                                TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_REMOVING_WALLET_TITLE,
                            ),
                            e.message ?: "Unknown error",
                        )
                    }
                    else -> throw e
                }
            }
        }
    }

    @FXML
    private fun handleCancel() {
        val stage = searchField.scene.window as Stage
        stage.close()
    }

    private fun loadArchivedWalletsFromDatabase() {
        archivedWallets = walletService.getAllArchivedWallets()
    }

    private fun updateWalletTableView() {
        val similarTextOrId = searchField.text.lowercase()

        walletTableView.items.clear()

        if (similarTextOrId.isEmpty()) {
            walletTableView.items.setAll(archivedWallets)
        } else {
            archivedWallets
                .filter { w ->
                    val type = w.type.name.lowercase()
                    val name = w.name.lowercase()
                    val id = w.id.toString()

                    type.contains(similarTextOrId) ||
                        name.contains(similarTextOrId) ||
                        id.contains(similarTextOrId)
                }.forEach { walletTableView.items.add(it) }
        }

        walletTableView.refresh()
    }

    private fun configureTableView() {
        val idColumn =
            TableColumn<Wallet, Int>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_ID),
            ).apply {
                setCellValueFactory { SimpleObjectProperty(it.value.id) }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        val walletColumn =
            TableColumn<Wallet, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_TABLE_WALLET),
            ).apply {
                setCellValueFactory { SimpleStringProperty(it.value.name) }
            }

        val typeColumn =
            TableColumn<Wallet, String>(
                preferencesService.translate(TranslationKeys.WALLETTRANSACTION_LABEL_TYPE),
            ).apply {
                setCellValueFactory {
                    SimpleStringProperty(UIUtils.translateWalletType(it.value.type))
                }
            }

        val numOfTransactionsColumn =
            TableColumn<Wallet, Int>(
                preferencesService.translate(
                    TranslationKeys.WALLETTRANSACTION_TABLE_ASSOCIATED_TRANSACTIONS,
                ),
            ).apply {
                setCellValueFactory {
                    SimpleObjectProperty(
                        walletService.getWalletTransactionAndTransferCountByWallet(it.value.id!!),
                    )
                }
                UIUtils.alignTableColumn(this, Pos.CENTER)
            }

        walletTableView.columns.addAll(
            idColumn,
            walletColumn,
            typeColumn,
            numOfTransactionsColumn,
        )
    }
}
