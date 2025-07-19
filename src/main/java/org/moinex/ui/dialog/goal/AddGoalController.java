/*
 * Filename: AddGoalController.java
 * Created on: December 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.goal;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.GoalService;
import org.moinex.service.WalletService;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.GoalFundingStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Goal dialog
 */
@Controller
@NoArgsConstructor
public final class AddGoalController extends BaseGoalManagement {
    @FXML private ComboBox<Wallet> masterWalletComboBox;

    @FXML private TitledPane goalFundingStrategyPane;

    @FXML private RadioButton newDepositRadioButton;

    @FXML private RadioButton allocateFromMasterWalletRadioButton;

    private ToggleGroup strategyToggleGroup;

    private List<Wallet> wallets;

    private WalletService walletService;

    /**
     * Constructor
     *
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddGoalController(GoalService goalService, WalletService walletService) {
        super(goalService);
        this.walletService = walletService;
    }

    @Override
    @FXML
    protected void initialize() {
        super.initialize(); // Chama o initialize da classe base
        setupDynamicVisibilityListeners();

        loadWalletsFromDatabase();

        configureComboBoxes();

        populateComboBoxes();
    }

    private void loadWalletsFromDatabase() {
        wallets = walletService.getAllNonArchivedWalletsOrderedByName();
    }

    private void populateComboBoxes() {
        masterWalletComboBox.getItems().setAll(wallets);
    }

    private void configureComboBoxes() {
        UIUtils.configureComboBox(masterWalletComboBox, Wallet::getName);
    }

    @Override
    @FXML
    protected void handleSave() {
        String goalName = nameField.getText();
        goalName = goalName.strip(); // Remove leading and trailing whitespaces

        String initialBalanceStr = initialBalanceField.getText();
        String targetBalanceStr = targetBalanceField.getText();
        LocalDate targetDate = targetDatePicker.getValue();
        String motivation = motivationTextArea.getText();
        Wallet masterWallet = masterWalletComboBox.getValue();

        if (goalName.isEmpty()
                || initialBalanceStr.isEmpty()
                || targetBalanceStr.isEmpty()
                || targetDate == null) {
            WindowUtils.showInformationDialog(
                    "Empty fields", "Please fill all required fields before saving");

            return;
        }

        GoalFundingStrategy strategy = null;

        if (goalFundingStrategyPane.isVisible()) {
            Toggle selectedToggle = strategyToggleGroup.getSelectedToggle();
            if (selectedToggle == null) {
                WindowUtils.showInformationDialog(
                        "Strategy required", "Please select an initial balance strategy.");
                return;
            }
            strategy = (GoalFundingStrategy) selectedToggle.getUserData();
        }

        try {
            BigDecimal initialBalance = new BigDecimal(initialBalanceStr);
            BigDecimal targetBalance = new BigDecimal(targetBalanceStr);

            goalService.addGoal(
                    goalName,
                    initialBalance,
                    targetBalance,
                    targetDate,
                    motivation,
                    masterWallet,
                    strategy);

            WindowUtils.showSuccessDialog("Goal created", "The goal was successfully created.");

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog("Invalid balance", "Please enter a valid balance.");
        } catch (IllegalArgumentException
                | EntityExistsException
                | EntityNotFoundException
                | MoinexException.InsufficientResourcesException e) {
            WindowUtils.showErrorDialog("Error creating goal", e.getMessage());
        }
    }

    /**
     * Configures the listeners to dynamically show/hide the initial balance strategy options
     */
    private void setupDynamicVisibilityListeners() {
        strategyToggleGroup = new ToggleGroup();
        newDepositRadioButton.setToggleGroup(strategyToggleGroup);
        allocateFromMasterWalletRadioButton.setToggleGroup(strategyToggleGroup);

        newDepositRadioButton.setUserData(GoalFundingStrategy.NEW_DEPOSIT);
        allocateFromMasterWalletRadioButton.setUserData(GoalFundingStrategy.ALLOCATE_FROM_EXISTING);

        goalFundingStrategyPane.setVisible(false);
        goalFundingStrategyPane.setManaged(false);

        ChangeListener<Object> listener =
                (observable, oldValue, newValue) -> {
                    boolean masterWalletSelected = masterWalletComboBox.getValue() != null;
                    boolean initialBalanceFilled =
                            initialBalanceField.getText() != null
                                    && !initialBalanceField.getText().isBlank();

                    boolean showOptions = masterWalletSelected && initialBalanceFilled;

                    goalFundingStrategyPane.setVisible(showOptions);
                    goalFundingStrategyPane.setManaged(showOptions);

                    // Resize the window to accommodate new components
                    if (goalFundingStrategyPane.getScene() != null
                            && goalFundingStrategyPane.getScene().getWindow() != null) {
                        goalFundingStrategyPane.getScene().getWindow().sizeToScene();
                    }
                };

        masterWalletComboBox.valueProperty().addListener(listener);
        initialBalanceField.textProperty().addListener(listener);
    }
}
