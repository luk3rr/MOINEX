package org.moinex.ui.dialog.wallettransaction;

import jakarta.persistence.EntityNotFoundException;
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

    private final ObservableList<Transfer> masterData = FXCollections.observableArrayList();
    private FilteredList<Transfer> filteredData;

    @Autowired
    public TransferController(
            WalletTransactionService walletTransactionService,
            ConfigurableApplicationContext springContext) {
        this.walletTransactionService = walletTransactionService;
        this.springContext = springContext;
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
        TableColumn<Transfer, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getId()));

        TableColumn<Transfer, String> dateColumn = new TableColumn<>("Date");
        dateColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                param.getValue()
                                        .getDate()
                                        .format(Constants.DATE_FORMATTER_NO_TIME)));

        TableColumn<Transfer, String> descriptionColumn = new TableColumn<>("Description");
        descriptionColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getDescription()));

        TableColumn<Transfer, String> amountColumn = new TableColumn<>("Amount");
        amountColumn.setCellValueFactory(
                param ->
                        new SimpleStringProperty(
                                UIUtils.formatCurrency(param.getValue().getAmount())));

        TableColumn<Transfer, String> senderColumn = new TableColumn<>("Sender");
        senderColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getSenderWallet().getName()));

        TableColumn<Transfer, String> receiverColumn = new TableColumn<>("Receiver");
        receiverColumn.setCellValueFactory(
                param -> new SimpleStringProperty(param.getValue().getReceiverWallet().getName()));

        TableColumn<Transfer, String> categoryColumn = new TableColumn<>("Category");
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
        UIUtils.setDatePickerFormat(transfersStartDatePicker);
        UIUtils.setDatePickerFormat(transfersEndDatePicker);

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
                "Add New Transfer",
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
                    "No transfer selected", "Please select a transfer to edit");
            return;
        }

        WindowUtils.openModalWindow(
                Constants.EDIT_TRANSFER_FXML,
                "Edit Transfer",
                springContext,
                (EditTransferController controller) -> controller.setTransfer(selectedTransfer),
                List.of(this::loadTransfersFromDatabase));
    }

    @FXML
    private void handleDelete() {
        Transfer selectedTransfer = transfersTableView.getSelectionModel().getSelectedItem();
        if (selectedTransfer == null) {
            WindowUtils.showInformationDialog(
                    "No transfer selected", "Please select a transfer to delete");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Delete Transfer",
                "Are you sure you want to delete the transfer with ID "
                        + selectedTransfer.getId()
                        + "? This action cannot be undone.")) {
            try {
                walletTransactionService.deleteTransfer(selectedTransfer.getId());
                WindowUtils.showSuccessDialog("Success", "Transfer deleted successfully.");

                loadTransfersFromDatabase();
                updateTableView();

            } catch (EntityNotFoundException e) {
                WindowUtils.showErrorDialog("Error", e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }
}
