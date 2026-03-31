/*
 * Filename: WishlistController.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.Hyperlink
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.Separator
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.VBox
import org.moinex.common.constant.Files
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.isPending
import org.moinex.common.extension.isPurchased
import org.moinex.common.util.UIUtils
import org.moinex.common.util.WindowUtils
import org.moinex.model.Category
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.model.enums.WishlistItemStatus
import org.moinex.model.wishlist.WishlistItem
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import org.moinex.service.wishlist.WishlistService
import org.moinex.ui.dialog.creditcard.AddCreditCardDebtController
import org.moinex.ui.dialog.wallettransaction.create.AddExpenseController
import org.moinex.ui.dialog.wishlist.AddWishlistItemController
import org.moinex.ui.dialog.wishlist.EditWishlistItemController
import org.moinex.ui.dialog.wishlist.MarkAsPurchasedController
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class WishlistController(
    private val wishlistService: WishlistService,
    private val categoryService: CategoryService,
    private val springContext: ConfigurableApplicationContext,
    private val preferencesService: PreferencesService,
) {
    @FXML
    private lateinit var wishlistTableView: TableView<WishlistItem>

    @FXML
    private lateinit var statusFilterComboBox: ComboBox<String>

    @FXML
    private lateinit var categoryFilterComboBox: ComboBox<Category>

    @FXML
    private lateinit var searchField: TextField

    @FXML
    private lateinit var itemCountLabel: Label

    @FXML
    private lateinit var totalValueLabel: Label

    private val allItems = FXCollections.observableArrayList<WishlistItem>()
    private val filteredItems = FXCollections.observableArrayList<WishlistItem>()
    private var categories: List<Category> = emptyList()

    @FXML
    fun initialize() {
        configureTableView()
        configureFilters()
        loadCategoriesFromDatabase()
        populateFilterComboBoxes()
        loadItemsFromDatabase()
        updateSummary()
        updateTableView()
    }

    @FXML
    private fun handleAdd() {
        WindowUtils.openModalWindow(
            Files.ADD_WISHLIST_ITEM_FXML,
            preferencesService.translate(TranslationKeys.WISHLIST_DIALOG_ADD_TITLE),
            springContext,
            { _: AddWishlistItemController -> },
            listOf(Runnable { loadItemsFromDatabase() }),
        )
    }

    @FXML
    private fun handleEdit() {
        val selectedItem = wishlistTableView.selectionModel.selectedItem
        if (selectedItem == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.EDIT_WISHLIST_ITEM_FXML,
            preferencesService.translate(TranslationKeys.WISHLIST_DIALOG_EDIT_TITLE),
            springContext,
            { controller: EditWishlistItemController ->
                controller.setItem(selectedItem)
            },
            listOf(Runnable { loadItemsFromDatabase() }),
        )
    }

    @FXML
    private fun handleDelete() {
        val selectedItem = wishlistTableView.selectionModel.selectedItem
        if (selectedItem == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_MESSAGE),
            )
            return
        }

        val confirmed =
            WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_DELETE_CONFIRMATION_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_DELETE_CONFIRMATION_MESSAGE),
            )

        if (confirmed) {
            runCatching {
                wishlistService.deleteItem(selectedItem.id!!)
                loadItemsFromDatabase()
                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.WISHLIST_DELETED_TITLE),
                    preferencesService.translate(TranslationKeys.WISHLIST_DELETED_MESSAGE),
                )
            }.onFailure { e ->
                WindowUtils.showErrorDialog(
                    preferencesService.translate(TranslationKeys.WISHLIST_ERROR_DELETING_TITLE),
                    e.message ?: "Unknown error",
                )
            }
        }
    }

    @FXML
    private fun handleMarkAsPurchased() {
        val selectedItem = wishlistTableView.selectionModel.selectedItem
        if (selectedItem == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_MESSAGE),
            )
            return
        }

        if (selectedItem.isPurchased()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_ALREADY_PURCHASED_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_ALREADY_PURCHASED_MESSAGE),
            )
            return
        }

        WindowUtils.openModalWindow(
            Files.MARK_AS_PURCHASED_FXML,
            preferencesService.translate(TranslationKeys.WISHLIST_MARK_AS_PURCHASED_SELECT_PAYMENT_METHOD),
            springContext,
            { controller: MarkAsPurchasedController ->
                controller.setItem(selectedItem)
                controller.setOnWalletSelectedCallback {
                    handlePurchaseWithWallet(selectedItem)
                }
                controller.setOnCreditCardSelectedCallback {
                    handlePurchaseWithCreditCard(selectedItem)
                }
            },
            emptyList(),
        )
    }

    private fun handlePurchaseWithWallet(item: WishlistItem) {
        WindowUtils.openModalWindow(
            Files.ADD_EXPENSE_FXML,
            preferencesService.translate(TranslationKeys.TRANSACTION_DIALOG_ADD_EXPENSE_TITLE),
            springContext,
            { controller: AddExpenseController ->
                controller.prefillDescription(item.title)
                controller.prefillTransactionValue(item.estimatedPrice)
                controller.prefillCategory(item.category)
                controller.setOnTransactionCreatedCallback { transactionId ->
                    runCatching {
                        wishlistService.markAsPurchasedWithWallet(item.id!!, transactionId)
                    }.onFailure { e ->
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(TranslationKeys.WISHLIST_ERROR_TITLE),
                            e.message ?: "Unknown error",
                        )
                    }
                }
            },
            listOf(
                Runnable {
                    loadItemsFromDatabase()
                    updateTableView()
                    updateSummary()
                },
            ),
        )
    }

    private fun handlePurchaseWithCreditCard(item: WishlistItem) {
        WindowUtils.openModalWindow(
            Files.ADD_CREDIT_CARD_DEBT_FXML,
            preferencesService.translate(TranslationKeys.CREDIT_CARD_DIALOG_ADD_DEBT_TITLE),
            springContext,
            { controller: AddCreditCardDebtController ->
                controller.prefillDescription(item.title)
                controller.prefillAmount(item.estimatedPrice)
                controller.prefillCategory(item.category)
                controller.setOnDebtCreatedCallback { debtId ->
                    runCatching {
                        wishlistService.markAsPurchasedWithCreditCard(item.id!!, debtId)
                    }.onFailure { e ->
                        WindowUtils.showErrorDialog(
                            preferencesService.translate(TranslationKeys.WISHLIST_ERROR_TITLE),
                            e.message ?: "Unknown error",
                        )
                    }
                }
            },
            listOf(
                Runnable {
                    loadItemsFromDatabase()
                    updateTableView()
                    updateSummary()
                },
            ),
        )
    }

    @FXML
    private fun handleMarkAsPending() {
        val selectedItem = wishlistTableView.selectionModel.selectedItem
        if (selectedItem == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_MESSAGE),
            )
            return
        }

        if (selectedItem.isPending()) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_ALREADY_PENDING_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_ALREADY_PENDING_MESSAGE),
            )
            return
        }

        val confirmed =
            WindowUtils.showConfirmationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_MARK_PENDING_CONFIRMATION_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_MARK_PENDING_CONFIRMATION_MESSAGE),
            )

        if (confirmed) {
            runCatching {
                wishlistService.markAsPending(selectedItem.id!!)
                loadItemsFromDatabase()
                updateTableView()
                updateSummary()

                WindowUtils.showSuccessDialog(
                    preferencesService.translate(TranslationKeys.WISHLIST_MARKED_PENDING_TITLE),
                    preferencesService.translate(TranslationKeys.WISHLIST_MARKED_PENDING_MESSAGE),
                )
            }.onFailure { e ->
                WindowUtils.showErrorDialog(
                    preferencesService.translate(TranslationKeys.WISHLIST_ERROR_TITLE),
                    e.message ?: "Unknown error",
                )
            }
        }
    }

    @FXML
    private fun handleViewDetails() {
        val selectedItem = wishlistTableView.selectionModel.selectedItem
        if (selectedItem == null) {
            WindowUtils.showInformationDialog(
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_TITLE),
                preferencesService.translate(TranslationKeys.WISHLIST_NO_ITEM_SELECTED_MESSAGE),
            )
            return
        }

        val links = wishlistService.getLinksForItem(selectedItem.id!!)

        val content =
            VBox(6.0).apply {
                padding = Insets(10.0)

                children.addAll(
                    UIUtils.createDetailLabel(
                        TranslationKeys.WISHLIST_LABEL_TITLE,
                        selectedItem.title,
                        copyable = true,
                    ),
                    UIUtils.createDetailLabel(
                        TranslationKeys.WISHLIST_LABEL_ESTIMATED_PRICE,
                        UIUtils.formatCurrency(selectedItem.estimatedPrice),
                        copyable = true,
                    ),
                    UIUtils.createDetailLabel(
                        TranslationKeys.WISHLIST_DETAILS_TARGET_DATE,
                        selectedItem.targetDate?.toString() ?: "-",
                        copyable = true,
                    ),
                    UIUtils.createDetailLabel(
                        TranslationKeys.WISHLIST_LABEL_CATEGORY,
                        selectedItem.category.name,
                        copyable = true,
                    ),
                    UIUtils.createDetailLabel(
                        TranslationKeys.WISHLIST_LABEL_PRIORITY,
                        translatePriority(selectedItem.priority),
                        copyable = true,
                    ),
                    UIUtils.createDetailLabel(
                        TranslationKeys.WISHLIST_LABEL_STATUS,
                        translateStatus(selectedItem.status),
                        copyable = true,
                    ),
                    UIUtils.createDetailLabel(
                        TranslationKeys.WISHLIST_DETAILS_NOTES,
                        selectedItem.notes ?: "-",
                        copyable = true,
                    ),
                )

                if (selectedItem.isPurchased()) {
                    children.add(
                        UIUtils.createDetailLabel(
                            TranslationKeys.WISHLIST_DETAILS_PURCHASED_DATE,
                            UIUtils.formatDateForDisplay(selectedItem.purchasedAt),
                            copyable = true,
                        ),
                    )

                    selectedItem.walletTransaction?.let { transaction ->
                        children.addAll(
                            UIUtils.createDetailLabel(
                                TranslationKeys.WISHLIST_DETAILS_PAYMENT_METHOD,
                                preferencesService.translate(TranslationKeys.WISHLIST_PAYMENT_METHOD_WALLET),
                                copyable = true,
                            ),
                            UIUtils.createDetailLabel(
                                TranslationKeys.WISHLIST_DETAILS_TRANSACTION_ID,
                                transaction.id?.toString() ?: "-",
                                copyable = true,
                            ),
                        )
                    }

                    selectedItem.creditCardDebt?.let { debt ->
                        children.addAll(
                            UIUtils.createDetailLabel(
                                TranslationKeys.WISHLIST_DETAILS_PAYMENT_METHOD,
                                preferencesService.translate(TranslationKeys.WISHLIST_PAYMENT_METHOD_CREDIT_CARD),
                                copyable = true,
                            ),
                            UIUtils.createDetailLabel(
                                TranslationKeys.WISHLIST_DETAILS_DEBT_ID,
                                debt.id?.toString() ?: "-",
                                copyable = true,
                            ),
                        )
                    }
                }

                children.addAll(
                    Separator(),
                    UIUtils.createDetailLabel(
                        TranslationKeys.WISHLIST_DETAILS_LINKS,
                        "",
                        showColon = true,
                    ),
                )

                if (links.isEmpty()) {
                    children.add(
                        Label(preferencesService.translate(TranslationKeys.WISHLIST_NO_LINKS)),
                    )
                } else {
                    links.forEach { link ->
                        val hyperlink =
                            Hyperlink(link.label ?: link.url).apply {
                                setOnAction { event ->
                                    event.consume()
                                    WindowUtils.openUrl(link.url)
                                }
                                contextMenu =
                                    ContextMenu(
                                        MenuItem(
                                            preferencesService.translate(
                                                TranslationKeys.WISHLIST_COPY_LINK,
                                            ),
                                        ).apply {
                                            setOnAction {
                                                Clipboard.getSystemClipboard().setContent(
                                                    ClipboardContent().apply { putString(link.url) },
                                                )
                                            }
                                        },
                                    )
                            }

                        if (link.label != null) {
                            UIUtils.addTooltipToNode(hyperlink, link.url)
                        }

                        children.add(hyperlink)
                    }
                }
            }

        WindowUtils.showDetailDialog(
            preferencesService.translate(TranslationKeys.WISHLIST_DETAILS_TITLE),
            content,
        )
    }

    private fun configureTableView() {
        val idColumn = createIdColumn()
        val titleColumn = createTitleColumn()
        val priceColumn = createPriceColumn()
        val dateColumn = createDateColumn()
        val categoryColumn = createCategoryColumn()
        val priorityColumn = createPriorityColumn()
        val statusColumn = createStatusColumn()

        wishlistTableView.columns.addAll(
            idColumn,
            titleColumn,
            priceColumn,
            dateColumn,
            categoryColumn,
            priorityColumn,
            statusColumn,
        )
    }

    private fun createIdColumn(): TableColumn<WishlistItem, Int> =
        TableColumn<WishlistItem, Int>(
            preferencesService.translate(TranslationKeys.WISHLIST_COLUMN_ID),
        ).apply {
            setCellValueFactory { SimpleObjectProperty(it.value.id) }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }

    private fun createTitleColumn(): TableColumn<WishlistItem, String> =
        TableColumn<WishlistItem, String>(
            preferencesService.translate(TranslationKeys.WISHLIST_COLUMN_TITLE),
        ).apply {
            setCellValueFactory { SimpleStringProperty(it.value.title) }
        }

    private fun createPriceColumn(): TableColumn<WishlistItem, String> =
        TableColumn<WishlistItem, String>(
            preferencesService.translate(TranslationKeys.WISHLIST_COLUMN_PRICE),
        ).apply {
            setCellValueFactory { SimpleStringProperty(UIUtils.formatCurrency(it.value.estimatedPrice)) }
        }

    private fun createDateColumn(): TableColumn<WishlistItem, String> =
        TableColumn<WishlistItem, String>(
            preferencesService.translate(TranslationKeys.WISHLIST_COLUMN_DATE),
        ).apply {
            setCellValueFactory { SimpleStringProperty(it.value.targetDate?.toString() ?: "-") }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }

    private fun createCategoryColumn(): TableColumn<WishlistItem, String> =
        TableColumn<WishlistItem, String>(
            preferencesService.translate(TranslationKeys.WISHLIST_COLUMN_CATEGORY),
        ).apply {
            setCellValueFactory { SimpleStringProperty(it.value.category.name) }
        }

    private fun createPriorityColumn(): TableColumn<WishlistItem, String> =
        TableColumn<WishlistItem, String>(
            preferencesService.translate(TranslationKeys.WISHLIST_COLUMN_PRIORITY),
        ).apply {
            setCellValueFactory { SimpleStringProperty(translatePriority(it.value.priority)) }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }

    private fun createStatusColumn(): TableColumn<WishlistItem, String> =
        TableColumn<WishlistItem, String>(
            preferencesService.translate(TranslationKeys.WISHLIST_COLUMN_STATUS),
        ).apply {
            setCellValueFactory { SimpleStringProperty(translateStatus(it.value.status)) }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }

    private fun configureFilters() {
        statusFilterComboBox.setOnAction { applyFilters() }
        categoryFilterComboBox.setOnAction { applyFilters() }
        searchField.textProperty().addListener { _, _, _ -> applyFilters() }
    }

    private fun loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName()
    }

    private fun populateFilterComboBoxes() {
        statusFilterComboBox.items.setAll(
            preferencesService.translate(TranslationKeys.WISHLIST_FILTER_ALL),
            preferencesService.translate(TranslationKeys.WISHLIST_FILTER_PENDING),
            preferencesService.translate(TranslationKeys.WISHLIST_FILTER_PURCHASED),
        )
        statusFilterComboBox.value = preferencesService.translate(TranslationKeys.WISHLIST_FILTER_ALL)

        categoryFilterComboBox.items.add(null)
        categoryFilterComboBox.items.addAll(categories)
        UIUtils.configureComboBox(categoryFilterComboBox) { it.name }
        categoryFilterComboBox.value = null
    }

    private fun loadItemsFromDatabase() {
        allItems.setAll(wishlistService.getAllItems())
        applyFilters()
    }

    private fun applyFilters() {
        val statusFilter = statusFilterComboBox.value
        val categoryFilter = categoryFilterComboBox.value
        val searchText = searchField.text?.trim()?.lowercase() ?: ""

        val filtered =
            allItems.filter { item ->
                val matchesStatus =
                    when (statusFilter) {
                        preferencesService.translate(TranslationKeys.WISHLIST_FILTER_PENDING) ->
                            item.isPending()
                        preferencesService.translate(TranslationKeys.WISHLIST_FILTER_PURCHASED) ->
                            item.isPurchased()
                        else -> true
                    }

                val matchesCategory = categoryFilter == null || item.category.id == categoryFilter.id

                val matchesSearch =
                    searchText.isEmpty() ||
                        item.title.lowercase().contains(searchText) ||
                        item.category.name
                            .lowercase()
                            .contains(searchText) ||
                        translatePriority(item.priority).lowercase().contains(searchText) ||
                        translateStatus(item.status).lowercase().contains(searchText) ||
                        UIUtils.formatCurrency(item.estimatedPrice).contains(searchText) ||
                        (item.targetDate?.toString()?.contains(searchText) == true) ||
                        (item.notes?.lowercase()?.contains(searchText) == true)

                matchesStatus && matchesCategory && matchesSearch
            }

        filteredItems.setAll(filtered)
        updateSummary()
        updateTableView()
    }

    private fun updateSummary() {
        val count = filteredItems.size
        val total = filteredItems.sumOf { it.estimatedPrice }

        itemCountLabel.text = "$count ${preferencesService.translate(TranslationKeys.WISHLIST_ITEMS)}"
        totalValueLabel.text =
            "${preferencesService.translate(
                TranslationKeys.WISHLIST_TOTAL_ESTIMATED,
            )}: ${UIUtils.formatCurrency(total)}"
    }

    private fun updateTableView() {
        wishlistTableView.items.clear()
        wishlistTableView.items.addAll(filteredItems)
        wishlistTableView.refresh()
    }

    private fun translatePriority(priority: WishlistItemPriority): String =
        when (priority) {
            WishlistItemPriority.LOW -> preferencesService.translate(TranslationKeys.WISHLIST_PRIORITY_LOW)
            WishlistItemPriority.MEDIUM -> preferencesService.translate(TranslationKeys.WISHLIST_PRIORITY_MEDIUM)
            WishlistItemPriority.HIGH -> preferencesService.translate(TranslationKeys.WISHLIST_PRIORITY_HIGH)
        }

    private fun translateStatus(status: WishlistItemStatus): String =
        when (status) {
            WishlistItemStatus.PENDING -> preferencesService.translate(TranslationKeys.WISHLIST_STATUS_PENDING)
            WishlistItemStatus.PURCHASED -> preferencesService.translate(TranslationKeys.WISHLIST_STATUS_PURCHASED)
        }
}
