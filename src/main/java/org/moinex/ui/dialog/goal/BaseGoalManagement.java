/*
 * Filename: BaseGoalManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.goal;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.GoalService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class to implement the common behavior of the Add and Edit Goal
 */
@NoArgsConstructor
public abstract class BaseGoalManagement {
    @FXML protected ComboBox<Wallet> masterWalletComboBox;

    @FXML protected TextField nameField;

    @FXML protected TextField balanceField;

    @FXML protected TextField targetBalanceField;

    @FXML protected DatePicker targetDatePicker;

    @FXML protected TextArea motivationTextArea;

    protected List<Wallet> masterWallets;

    protected WalletService walletService;

    protected GoalService goalService;

    protected I18nService i18nService;

    /**
     * Constructor
     *
     * @param goalService GoalService
     * @param walletService WalletService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    protected BaseGoalManagement(GoalService goalService, WalletService walletService) {
        this.goalService = goalService;
        this.walletService = walletService;
    }

    protected void setI18nService(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    @FXML
    protected void initialize() {
        UIUtils.setDatePickerFormat(targetDatePicker);

        loadWalletsFromDatabase();

        configureComboBoxes();

        populateComboBoxes();

        // Ensure that the balance fields only accept monetary values
        balanceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                balanceField.setText(oldValue);
                            }
                        });

        targetBalanceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                targetBalanceField.setText(oldValue);
                            }
                        });
    }

    @FXML
    protected abstract void handleSave();

    @FXML
    protected void handleCancel() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void populateComboBoxes() {
        masterWalletComboBox.getItems().add(null);
        masterWalletComboBox.getItems().addAll(masterWallets);

        masterWalletComboBox.setCellFactory(
                lv ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(Wallet item, boolean empty) {
                                super.updateItem(item, empty);
                                if (empty || item == null) {
                                    setText(""); // Display empty text when no wallet is selected
                                } else {
                                    setText(item.getName());
                                }
                            }
                        });

        masterWalletComboBox.setButtonCell(
                new ListCell<>() {
                    @Override
                    protected void updateItem(Wallet item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(""); // Display empty text when no wallet is selected
                        } else {
                            setText(item.getName());
                        }
                    }
                });
    }

    private void configureComboBoxes() {
        UIUtils.configureComboBox(masterWalletComboBox, Wallet::getName);
    }

    private void loadWalletsFromDatabase() {
        masterWallets =
                walletService.getAllNonArchivedWalletsOrderedByName().stream()
                        .filter(Wallet::isMaster)
                        .toList();
    }
}
