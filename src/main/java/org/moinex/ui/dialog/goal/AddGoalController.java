/*
 * Filename: AddGoalController.java
 * Created on: December 13, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.goal;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.error.MoinexException;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.GoalService;
import org.moinex.service.I18nService;
import org.moinex.service.WalletService;
import org.moinex.util.Constants;
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

    @FXML private TitledPane goalFundingStrategyPane;

    @FXML private RadioButton newDepositRadioButton;

    @FXML private RadioButton allocateFromMasterWalletRadioButton;

    @FXML private RadioButton walletModeRadioButton;

    @FXML private RadioButton assetModeRadioButton;

    @FXML private HBox walletBasedFieldsPane;

    @FXML private HBox masterWalletPane;

    @FXML private VBox assetAllocationsPane;

    @FXML private JFXButton configureAllocationsButton;

    @FXML private Label allocationsStatusLabel;

    private ToggleGroup strategyToggleGroup;

    private ToggleGroup trackingModeToggleGroup;

    private I18nService i18nService;
    
    private org.springframework.context.ConfigurableApplicationContext springContext;

    private org.moinex.service.GoalAssetAllocationService allocationService;

    private java.util.List<org.moinex.dto.GoalAssetAllocationDTO> tempAllocations = new java.util.ArrayList<>();

    /**
     * Constructor
     *
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public AddGoalController(
            GoalService goalService, 
            WalletService walletService, 
            I18nService i18nService,
            org.springframework.context.ConfigurableApplicationContext springContext,
            org.moinex.service.GoalAssetAllocationService allocationService) {
        super(goalService, walletService);
        this.i18nService = i18nService;
        this.springContext = springContext;
        this.allocationService = allocationService;
        setI18nService(i18nService);
    }

    @Override
    @FXML
    protected void initialize() {
        super.initialize();
        setupTrackingModeToggleGroup();
        setupDynamicVisibilityListeners();
    }

    @Override
    @FXML
    protected void handleSave() {
        String goalName = nameField.getText();
        goalName = goalName.strip();

        String targetBalanceStr = targetBalanceField.getText();
        LocalDate targetDate = targetDatePicker.getValue();
        String motivation = motivationTextArea.getText();

        boolean isAssetBased = assetModeRadioButton.isSelected();

        if (goalName.isEmpty() || targetBalanceStr.isEmpty() || targetDate == null) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        String initialBalanceStr = balanceField.getText();
        Wallet masterWallet = masterWalletComboBox.getValue();
        GoalFundingStrategy strategy = null;

        if (!isAssetBased && goalFundingStrategyPane.isVisible()) {
            Toggle selectedToggle = strategyToggleGroup.getSelectedToggle();
            if (selectedToggle == null) {
                WindowUtils.showInformationDialog(
                        i18nService.tr(
                                Constants.TranslationKeys.GOAL_DIALOG_STRATEGY_REQUIRED_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys.GOAL_DIALOG_STRATEGY_REQUIRED_MESSAGE));
                return;
            }
            strategy = (GoalFundingStrategy) selectedToggle.getUserData();
        }

        try {
            BigDecimal initialBalance = isAssetBased 
                    ? BigDecimal.ZERO 
                    : new BigDecimal(initialBalanceStr.isEmpty() ? "0" : initialBalanceStr);
            BigDecimal targetBalance = new BigDecimal(targetBalanceStr);

            org.moinex.util.enums.GoalTrackingMode trackingMode = isAssetBased
                    ? org.moinex.util.enums.GoalTrackingMode.ASSET_ALLOCATION
                    : org.moinex.util.enums.GoalTrackingMode.WALLET;

            Integer goalId = goalService.addGoal(
                    goalName,
                    initialBalance,
                    targetBalance,
                    targetDate,
                    motivation,
                    masterWallet,
                    strategy,
                    trackingMode);

            // Create allocations if asset-based goal
            if (isAssetBased && !tempAllocations.isEmpty()) {
                for (org.moinex.dto.GoalAssetAllocationDTO allocation : tempAllocations) {
                    allocation.setGoalId(goalId);
                    allocationService.addAllocation(allocation);
                }
            }

            WindowUtils.showSuccessDialog(
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_GOAL_CREATED_TITLE),
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_GOAL_CREATED_MESSAGE));

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_INVALID_BALANCE_TITLE),
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_INVALID_BALANCE_MESSAGE));
        } catch (IllegalArgumentException
                | EntityExistsException
                | EntityNotFoundException
                | MoinexException.InsufficientResourcesException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ERROR_CREATING_GOAL_TITLE),
                    e.getMessage());
        }
    }

    private void setupTrackingModeToggleGroup() {
        trackingModeToggleGroup = new ToggleGroup();
        walletModeRadioButton.setToggleGroup(trackingModeToggleGroup);
        assetModeRadioButton.setToggleGroup(trackingModeToggleGroup);

        trackingModeToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isWalletBased = walletModeRadioButton.isSelected();
            
            walletBasedFieldsPane.setVisible(isWalletBased);
            walletBasedFieldsPane.setManaged(isWalletBased);
            masterWalletPane.setVisible(isWalletBased);
            masterWalletPane.setManaged(isWalletBased);
            goalFundingStrategyPane.setVisible(false);
            goalFundingStrategyPane.setManaged(false);
            
            assetAllocationsPane.setVisible(!isWalletBased);
            assetAllocationsPane.setManaged(!isWalletBased);

            if (walletModeRadioButton.getScene() != null
                    && walletModeRadioButton.getScene().getWindow() != null) {
                walletModeRadioButton.getScene().getWindow().sizeToScene();
            }
        });
    }

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
                    if (!walletModeRadioButton.isSelected()) {
                        return;
                    }

                    boolean masterWalletSelected = masterWalletComboBox.getValue() != null;
                    boolean initialBalanceFilled =
                            balanceField.getText() != null && !balanceField.getText().isBlank();

                    boolean showOptions = masterWalletSelected && initialBalanceFilled;

                    goalFundingStrategyPane.setVisible(showOptions);
                    goalFundingStrategyPane.setManaged(showOptions);

                    if (goalFundingStrategyPane.getScene() != null
                            && goalFundingStrategyPane.getScene().getWindow() != null) {
                        goalFundingStrategyPane.getScene().getWindow().sizeToScene();
                    }
                };

        masterWalletComboBox.valueProperty().addListener(listener);
        balanceField.textProperty().addListener(listener);
    }

    @FXML
    protected void handleConfigureAllocations() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/ui/dialog/goal/add_goal_asset_allocation.fxml"));
            loader.setControllerFactory(springContext::getBean);
            
            javafx.scene.Parent root = loader.load();
            
            org.moinex.ui.dialog.goal.AddGoalAssetAllocationController controller = loader.getController();
            
            // Set temporary mode and parent controller
            controller.setTemporaryMode(true);
            controller.setParentController(this);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(i18nService.tr(Constants.TranslationKeys.GOAL_LABEL_ADD_ASSET_ALLOCATION));
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            stage.showAndWait();
            
            // After dialog closes, check if allocation was added
            // For now, we'll add a simple counter
            updateAllocationsStatus();
            
        } catch (java.io.IOException e) {
            WindowUtils.showErrorDialog(
                i18nService.tr(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                "Failed to open allocation dialog: " + e.getMessage());
        }
    }

    private void updateAllocationsStatus() {
        if (tempAllocations.isEmpty()) {
            allocationsStatusLabel.setText(
                i18nService.tr("goal.label.noAllocationsConfigured"));
        } else {
            allocationsStatusLabel.setText(
                java.text.MessageFormat.format(
                    i18nService.tr("goal.label.allocationsConfigured"),
                    tempAllocations.size()));
        }
    }

    /**
     * Adds a temporary allocation (called from allocation dialog)
     */
    public void addTempAllocation(org.moinex.dto.GoalAssetAllocationDTO allocation) {
        tempAllocations.add(allocation);
        updateAllocationsStatus();
    }

    /**
     * Gets the list of temporary allocations
     */
    public java.util.List<org.moinex.dto.GoalAssetAllocationDTO> getTempAllocations() {
        return tempAllocations;
    }
}
