package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.List;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.wallettransaction.Transfer;
import org.moinex.service.I18nService;
import org.moinex.service.WalletTransactionService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Manage Transfers dialog
 */
@Controller
@NoArgsConstructor
public class TransferController {
    @FXML private TableView<Transfer> transfersTableView;
    @FXML private TextField searchField;
    @FXML private DatePicker transfersEndDatePicker;
    @FXML private DatePicker transfersStartDatePicker;

    private ConfigurableApplicationContext springContext;
    private WalletTransactionService walletTransactionService;
    private I18nService i18nService;

    private final ObservableList<Transfer> masterData = FXCollections.observableArrayList();
    private FilteredList<Transfer> filteredData;

    @Autowired
    public TransferController(
            WalletTransactionService walletTransactionService,
            ConfigurableApplicationContext springContext,
            I18nService i18nService) {
        this.walletTransactionService = walletTransactionService;
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadTransfersFromDatabase();

        configureTableView();
        configureDatePickers();
        setupFilterListeners();

        filteredData = new FilteredList<>(masterData, p -> true);
        updateTableView();
    }

    /**
     * Configures the TableView columns to display Transfer data.
     */
    private void configureTableView() {
        TableColumn<Transfer, Integer> idColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_ID));
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        TableColumn<Transfer, String> dateColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_DATE));
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatDateForDisplay(
                                        param.getValue().getDate(), i18nService)));

        TableColumn<Transfer, String> descriptionColumn =
                new TableColumn<>(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_TABLE_DESCRIPTION));
        descriptionColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getDescription()));

        TableColumn<Transfer, String> amountColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_AMOUNT));
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatCurrency(param.getValue().getAmount())));

        TableColumn<Transfer, String> senderColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_SENDER));
        senderColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getSenderWallet().getName()));

        TableColumn<Transfer, String> receiverColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_RECEIVER));
        receiverColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getReceiverWallet().getName()));

        TableColumn<Transfer, String> categoryColumn =
                new TableColumn<>(
                        i18nService.tr(Constants.TranslationKeys.WALLETTRANSACTION_TABLE_CATEGORY));
        categoryColumn.setCellValueFactory(
                param -> {
                    if (param.getValue().getCategory() != null) {
                        return new SimpleStringProperty(param.getValue().getCategory().getName());
                    }
                    return new SimpleStringProperty("-");
                });

        UIUtils.alignTableColumn(
                List.of(
                        idColumn,
                        dateColumn,
                        amountColumn,
                        senderColumn,
                        receiverColumn,
                        categoryColumn),
                Pos.CENTER);

        transfersTableView.getColumns().add(idColumn);
        transfersTableView.getColumns().add(dateColumn);
        transfersTableView.getColumns().add(descriptionColumn);
        transfersTableView.getColumns().add(amountColumn);
        transfersTableView.getColumns().add(senderColumn);
        transfersTableView.getColumns().add(receiverColumn);
        transfersTableView.getColumns().add(categoryColumn);
    }

    /**
     * Configures the date pickers with appropriate formatters and default values.
     */
    private void configureDatePickers() {
        UIUtils.setDatePickerFormat(transfersStartDatePicker, i18nService);
        UIUtils.setDatePickerFormat(transfersEndDatePicker, i18nService);

        transfersStartDatePicker.setValue(LocalDate.now().withDayOfMonth(1));
        transfersEndDatePicker.setValue(LocalDate.now().plusMonths(1).withDayOfMonth(1));
    }

    /**
     * Loads all transfers from the database into the master list and sets up the filtered list.
     */
    private void loadTransfersFromDatabase() {
        masterData.setAll(walletTransactionService.getAllTransfers());
    }

    /**
     * Sets up listeners on the search field and date pickers to filter the table view.
     */
    private void setupFilterListeners() {
        searchField.textProperty().addListener((obs, old, val) -> updateTableView());
        transfersStartDatePicker.valueProperty().addListener((obs, old, val) -> updateTableView());
        transfersEndDatePicker.valueProperty().addListener((obs, old, val) -> updateTableView());
    }

    /**
     * Applies the current filters from the UI to the table view data.
     */
    private void updateTableView() {
        String searchText = searchField.getText().toLowerCase();
        LocalDate startDate = transfersStartDatePicker.getValue();
        LocalDate endDate = transfersEndDatePicker.getValue();

        filteredData.setPredicate(
                transfer -> {
                    boolean matchesSearch =
                            searchText.isEmpty()
                                    || transfer.getDescription().toLowerCase().contains(searchText)
                                    || transfer.getSenderWallet()
                                            .getName()
                                            .toLowerCase()
                                            .contains(searchText)
                                    || transfer.getReceiverWallet()
                                            .getName()
                                            .toLowerCase()
                                            .contains(searchText)
                                    || String.valueOf(transfer.getId()).contains(searchText);

                    boolean matchesDate = true;
                    LocalDate transferDate = transfer.getDate().toLocalDate();
                    if (startDate != null && transferDate.isBefore(startDate)) {
                        matchesDate = false;
                    }
                    if (endDate != null && transferDate.isAfter(endDate)) {
                        matchesDate = false;
                    }

                    return matchesSearch && matchesDate;
                });

        transfersTableView.setItems(filteredData);
    }

    @FXML
    private void handleAdd() {
        WindowUtils.openModalWindow(
                Constants.ADD_TRANSFER_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_ADD_NEW_TRANSFER_TITLE),
                springContext,
                (AddTransferController controller) -> {},
                List.of(
                        () -> {
                            loadTransfersFromDatabase();
                            updateTableView();
                        }));
    }

    @FXML
    private void handleEdit() {
        Transfer selectedTransfer = transfersTableView.getSelectionModel().getSelectedItem();
        if (selectedTransfer == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_EDIT_MESSAGE));
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TRANSFER_FXML,
                i18nService.tr(
                        Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_EDIT_TRANSFER_TITLE),
                springContext,
                (EditTransferController controller) -> controller.setTransfer(selectedTransfer),
                List.of(this::loadTransfersFromDatabase));
    }

    @FXML
    private void handleDelete() {
        Transfer selectedTransfer = transfersTableView.getSelectionModel().getSelectedItem();
        if (selectedTransfer == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .WALLETTRANSACTION_DIALOG_NO_TRANSFER_SELECTED_DELETE_MESSAGE));
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                i18nService.tr(
                        Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_DELETE_TRANSFER_TITLE),
                MessageFormat.format(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_DELETE_TRANSFER_MESSAGE),
                        selectedTransfer.getId()),
                i18nService.getBundle())) {
            try {
                walletTransactionService.deleteTransfer(selectedTransfer.getId());
                WindowUtils.showSuccessDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_SUCCESS_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .WALLETTRANSACTION_DIALOG_TRANSFER_DELETED_MESSAGE));

                loadTransfersFromDatabase();
                updateTableView();

            } catch (EntityNotFoundException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.WALLETTRANSACTION_DIALOG_ERROR_TITLE),
                        e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }
}
