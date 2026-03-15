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
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.enums.GoalFundingStrategy;
import org.moinex.model.goal.Goal;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.service.PreferencesService;
import org.moinex.service.goal.GoalService;
import org.moinex.service.wallet.WalletService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/** Controller for the Add Goal dialog */
@Controller
@NoArgsConstructor
public final class AddGoalController extends BaseGoalManagement {

    @FXML private TitledPane goalFundingStrategyPane;

    @FXML private RadioButton newDepositRadioButton;

    @FXML private RadioButton allocateFromMasterWalletRadioButton;

    private ToggleGroup strategyToggleGroup;

    private PreferencesService preferencesService;

    /**
     * Constructor
     *
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddGoalController(
            GoalService goalService,
            WalletService walletService,
            PreferencesService preferencesService) {
        super(goalService, walletService);
        this.preferencesService = preferencesService;
        setPreferencesService(preferencesService);
    }

    @Override
    @FXML
    protected void initialize() {
        super.initialize();
        setupDynamicVisibilityListeners();
    }

    @Override
    @FXML
    protected void handleSave() {
        String goalName = nameField.getText();
        goalName = goalName.strip(); // Remove leading and trailing whitespaces

        String initialBalanceStr = balanceField.getText();
        String targetBalanceStr = targetBalanceField.getText();
        LocalDate targetDate = targetDatePicker.getValue();
        String motivation = motivationTextArea.getText();
        Wallet masterWallet = masterWalletComboBox.getValue();

        if (goalName.isEmpty() || targetBalanceStr.isEmpty() || targetDate == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_MESSAGE));

            return;
        }

        GoalFundingStrategy strategy = null;

        if (goalFundingStrategyPane.isVisible()) {
            Toggle selectedToggle = strategyToggleGroup.getSelectedToggle();
            if (selectedToggle == null) {
                WindowUtils.showInformationDialog(
                        preferencesService.translate(
                                Constants.TranslationKeys.GOAL_DIALOG_STRATEGY_REQUIRED_TITLE),
                        preferencesService.translate(
                                Constants.TranslationKeys.GOAL_DIALOG_STRATEGY_REQUIRED_MESSAGE));
                return;
            }
            strategy = (GoalFundingStrategy) selectedToggle.getUserData();
        }

        try {
            BigDecimal initialBalance =
                    new BigDecimal(initialBalanceStr.isEmpty() ? "0" : initialBalanceStr);
            BigDecimal targetBalance = new BigDecimal(targetBalanceStr);

            WalletType goalWalletType =
                    walletService.getAllWalletTypes().stream()
                            .filter(
                                    walletType ->
                                            walletType
                                                    .getName()
                                                    .equals(
                                                            Constants
                                                                    .GOAL_DEFAULT_WALLET_TYPE_NAME))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new IllegalArgumentException(
                                                    "Goal wallet type not found"));

            goalService.createGoal(
                    new Goal(
                            initialBalance,
                            targetBalance,
                            targetDate,
                            null,
                            motivation,
                            null,
                            goalName,
                            goalWalletType,
                            false,
                            masterWallet),
                    strategy);

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.GOAL_DIALOG_GOAL_CREATED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.GOAL_DIALOG_GOAL_CREATED_MESSAGE));

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.GOAL_DIALOG_INVALID_BALANCE_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.GOAL_DIALOG_INVALID_BALANCE_MESSAGE));
        } catch (IllegalArgumentException
                | EntityExistsException
                | EntityNotFoundException
                | MoinexException.InsufficientResourcesException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.GOAL_DIALOG_ERROR_CREATING_GOAL_TITLE),
                    e.getMessage());
        }
    }

    /** Configures the listeners to dynamically show/hide the initial balance strategy options */
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
                            balanceField.getText() != null && !balanceField.getText().isBlank();

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
        balanceField.textProperty().addListener(listener);
    }
}
