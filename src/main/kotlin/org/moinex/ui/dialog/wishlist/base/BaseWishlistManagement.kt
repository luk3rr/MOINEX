/*
 * Filename: BaseWishlistManagement.kt
 * Created on: March 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.wishlist.base

import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.control.ComboBox
import javafx.scene.control.DatePicker
import javafx.scene.control.Label
import javafx.scene.control.OverrunStyle
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.stage.Stage
import org.moinex.common.constant.Constants
import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.UIUtils
import org.moinex.model.Category
import org.moinex.model.dto.form.WishlistItemFormDTO
import org.moinex.model.enums.WishlistItemPriority
import org.moinex.model.wishlist.WishlistItemLink
import org.moinex.service.CategoryService
import org.moinex.service.PreferencesService
import java.time.LocalDate

abstract class BaseWishlistManagement(
    protected val categoryService: CategoryService,
    protected val preferencesService: PreferencesService,
) {
    @FXML
    protected lateinit var titleField: TextField

    @FXML
    protected lateinit var estimatedPriceField: TextField

    @FXML
    protected lateinit var targetDatePicker: DatePicker

    @FXML
    protected lateinit var categoryComboBox: ComboBox<Category>

    @FXML
    protected lateinit var priorityComboBox: ComboBox<WishlistItemPriority>

    @FXML
    protected lateinit var notesTextArea: TextArea

    @FXML
    protected lateinit var linksTableView: TableView<WishlistItemLink>

    @FXML
    protected lateinit var linkUrlField: TextField

    @FXML
    protected lateinit var linkLabelField: TextField

    protected var categories: List<Category> = emptyList()
    protected var links: MutableList<WishlistItemLink> = mutableListOf()

    private var estimatedPriceListener: ChangeListener<String>? = null

    @FXML
    protected open fun initialize() {
        configureComboBoxes()
        configureTableView()
        configureListeners()
        loadCategoriesFromDatabase()
        populateComboBoxes()
        UIUtils.setDatePickerFormat(targetDatePicker)
    }

    @FXML
    protected abstract fun handleSave()

    @FXML
    protected open fun handleCancel() {
        (titleField.scene.window as Stage).close()
    }

    @FXML
    protected fun handleAddLink() {
        val url = linkUrlField.text?.trim()
        val label = linkLabelField.text?.trim()

        if (url.isNullOrBlank()) {
            return
        }

        val newLink =
            WishlistItemLink(
                url = url,
                label = label.takeIf { !it.isNullOrBlank() },
                wishlistItem = null,
            )

        links.add(newLink)
        linkUrlField.clear()
        linkLabelField.clear()
        updateTableView()
    }

    @FXML
    protected fun handleRemoveLink() {
        val selectedLink = linksTableView.selectionModel.selectedItem
        if (selectedLink != null) {
            links.remove(selectedLink)
        }
        updateTableView()
    }

    protected fun loadCategoriesFromDatabase() {
        categories = categoryService.getNonArchivedCategoriesOrderedByName()
    }

    protected fun populateComboBoxes() {
        categoryComboBox.items.setAll(categories)
        priorityComboBox.items.setAll(WishlistItemPriority.entries)
        priorityComboBox.value = WishlistItemPriority.MEDIUM
    }

    protected fun updateTableView() {
        linksTableView.items.clear()
        linksTableView.items.addAll(links)
        linksTableView.refresh()
    }

    protected fun configureComboBoxes() {
        UIUtils.configureComboBox(categoryComboBox, Category::name)
        UIUtils.configureComboBox(priorityComboBox) { priority ->
            when (priority) {
                WishlistItemPriority.LOW -> preferencesService.translate(TranslationKeys.WISHLIST_PRIORITY_LOW)
                WishlistItemPriority.MEDIUM -> preferencesService.translate(TranslationKeys.WISHLIST_PRIORITY_MEDIUM)
                WishlistItemPriority.HIGH -> preferencesService.translate(TranslationKeys.WISHLIST_PRIORITY_HIGH)
            }
        }
    }

    protected fun configureTableView() {
        val labelColumn = createLabelColumn()
        val urlColumn = createUrlColumn()

        linksTableView.columns.addAll(
            labelColumn,
            urlColumn,
        )
    }

    private fun createLabelColumn(): TableColumn<WishlistItemLink, String> =
        TableColumn<WishlistItemLink, String>(
            preferencesService.translate(TranslationKeys.WISHLIST_LABEL_LINK_LABEL),
        ).apply {
            setCellValueFactory { SimpleStringProperty(it.value.label ?: "-") }
            UIUtils.alignTableColumn(this, Pos.CENTER)
        }

    private fun createUrlColumn(): TableColumn<WishlistItemLink, String> =
        TableColumn<WishlistItemLink, String>(
            preferencesService.translate(TranslationKeys.WISHLIST_LABEL_LINK_URL),
        ).apply {
            setCellValueFactory { SimpleStringProperty(it.value.url) }

            minWidth = 150.0
            prefWidth = 250.0
            maxWidth = 300.0

            setCellFactory {
                object : TableCell<WishlistItemLink, String>() {
                    private val label = Label()

                    override fun updateItem(
                        item: String?,
                        empty: Boolean,
                    ) {
                        super.updateItem(item, empty)

                        if (empty || item == null) {
                            graphic = null
                        } else {
                            label.text = item
                            label.isWrapText = false
                            label.textOverrun = OverrunStyle.ELLIPSIS

                            UIUtils.addTooltipToNode(label, item)

                            graphic = label
                        }
                    }
                }
            }

            UIUtils.alignTableColumn(this, Pos.CENTER_LEFT)
        }

    protected fun configureListeners() {
        estimatedPriceListener =
            ChangeListener { _, oldValue, newValue ->
                if (!newValue.matches(Regex(Constants.MONETARY_VALUE_REGEX))) {
                    estimatedPriceField.text = oldValue
                }
            }

        estimatedPriceField.textProperty().addListener(estimatedPriceListener)
    }

    protected fun getFieldsFromInterface(): WishlistItemFormDTO =
        WishlistItemFormDTO(
            title = titleField.text.trim(),
            estimatedPriceStr = estimatedPriceField.text.trim(),
            targetDate = targetDatePicker.value,
            category = categoryComboBox.value,
            priority = priorityComboBox.value,
            notes = notesTextArea.text?.trim(),
            links = links.toList(),
        )

    protected fun setFieldsInInterface(
        title: String,
        estimatedPrice: String,
        targetDate: LocalDate?,
        category: Category,
        priority: WishlistItemPriority,
        notes: String?,
        itemLinks: List<WishlistItemLink>,
    ) {
        titleField.text = title
        estimatedPriceField.text = estimatedPrice
        targetDatePicker.value = targetDate
        categoryComboBox.value = category
        priorityComboBox.value = priority
        notesTextArea.text = notes

        links.clear()
        links.addAll(itemLinks)
        updateTableView()
    }
}
