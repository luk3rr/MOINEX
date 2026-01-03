/*
 * Filename: AddGoalAssetAllocationController.java
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.goal;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.dto.GoalAssetAllocationDTO;
import org.moinex.model.goal.Goal;
import org.moinex.model.investment.Bond;
import org.moinex.model.investment.Ticker;
import org.moinex.repository.investment.BondRepository;
import org.moinex.repository.investment.TickerRepository;
import org.moinex.service.BondService;
import org.moinex.service.GoalAssetAllocationService;
import org.moinex.service.I18nService;
import org.moinex.service.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.AllocationType;
import org.moinex.util.enums.GoalAssetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Goal Asset Allocation dialog
 */
@Controller
@NoArgsConstructor
public class AddGoalAssetAllocationController {

    @FXML private RadioButton bondRadioButton;
    @FXML private RadioButton tickerRadioButton;
    @FXML private ComboBox<Object> assetComboBox;
    @FXML private RadioButton percentageRadioButton;
    @FXML private RadioButton quantityRadioButton;
    @FXML private RadioButton valueRadioButton;
    @FXML private TextField allocationValueField;
    @FXML private Label allocationValueLabel;
    @FXML private VBox assetInfoPane;
    @FXML private Label currentValueLabel;
    @FXML private Label availableQuantityLabel;

    private ToggleGroup assetTypeGroup;
    private ToggleGroup allocationTypeGroup;

    private GoalAssetAllocationService allocationService;
    private BondRepository bondRepository;
    private TickerRepository tickerRepository;
    private BondService bondService;
    private TickerService tickerService;
    private I18nService i18nService;

    private Goal currentGoal;
    private GoalAssetAllocationDTO existingAllocation;
    private boolean isTemporaryMode = false;
    private AddGoalController parentController;

    @Autowired
    public AddGoalAssetAllocationController(
            GoalAssetAllocationService allocationService,
            BondRepository bondRepository,
            TickerRepository tickerRepository,
            BondService bondService,
            TickerService tickerService,
            I18nService i18nService) {
        this.allocationService = allocationService;
        this.bondRepository = bondRepository;
        this.tickerRepository = tickerRepository;
        this.bondService = bondService;
        this.tickerService = tickerService;
        this.i18nService = i18nService;
    }

    @FXML
    protected void initialize() {
        setupToggleGroups();
        setupListeners();
        assetInfoPane.setVisible(false);
        assetInfoPane.setManaged(false);
    }

    /**
     * Sets the goal for which to add/edit allocation
     *
     * @param goal Goal
     */
    public void setGoal(Goal goal) {
        this.currentGoal = goal;
    }

    /**
     * Sets temporary mode (for configuring allocations before goal creation)
     *
     * @param temporaryMode True if in temporary mode
     */
    public void setTemporaryMode(boolean temporaryMode) {
        this.isTemporaryMode = temporaryMode;
    }

    /**
     * Sets the parent controller for temporary mode
     *
     * @param parentController Parent AddGoalController
     */
    public void setParentController(AddGoalController parentController) {
        this.parentController = parentController;
    }

    /**
     * Sets existing allocation for editing
     *
     * @param allocation Existing allocation DTO
     */
    public void setExistingAllocation(GoalAssetAllocationDTO allocation) {
        this.existingAllocation = allocation;
        populateFields();
    }

    @FXML
    protected void handleSave() {
        if (!validateFields()) {
            return;
        }

        try {
            GoalAssetType assetType = bondRadioButton.isSelected() 
                ? GoalAssetType.BOND 
                : GoalAssetType.TICKER;

            Object selectedAsset = assetComboBox.getValue();
            if (selectedAsset == null) {
                WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_NO_ASSET_SELECTED_TITLE),
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_NO_ASSET_SELECTED_MESSAGE));
                return;
            }

            Integer assetId = getAssetId(selectedAsset);
            AllocationType allocationType = getAllocationType();
            BigDecimal allocationValue = new BigDecimal(allocationValueField.getText());

            GoalAssetAllocationDTO dto = GoalAssetAllocationDTO.builder()
                    .goalId(isTemporaryMode ? null : currentGoal.getId())
                    .assetType(assetType)
                    .assetId(assetId)
                    .allocationType(allocationType)
                    .allocationValue(allocationValue)
                    .build();

            if (isTemporaryMode) {
                // Temporary mode: add to parent controller's list
                if (parentController != null) {
                    // Get asset name for display
                    String assetName = getAssetName(selectedAsset);
                    dto.setAssetName(assetName);
                    
                    parentController.addTempAllocation(dto);
                    WindowUtils.showSuccessDialog(
                        i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ALLOCATION_ADDED_TITLE),
                        i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ALLOCATION_ADDED_MESSAGE));
                }
            } else {
                // Normal mode: save to database
                if (existingAllocation != null) {
                    allocationService.updateAllocation(existingAllocation.getId(), dto);
                    WindowUtils.showSuccessDialog(
                        i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ALLOCATION_UPDATED_TITLE),
                        i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ALLOCATION_UPDATED_MESSAGE));
                } else {
                    allocationService.addAllocation(dto);
                    WindowUtils.showSuccessDialog(
                        i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ALLOCATION_ADDED_TITLE),
                        i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ALLOCATION_ADDED_MESSAGE));
                }
            }

            closeDialog();

        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_INVALID_ALLOCATION_VALUE_TITLE),
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_INVALID_ALLOCATION_VALUE_MESSAGE));
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            String errorTitle = existingAllocation != null 
                ? Constants.TranslationKeys.GOAL_DIALOG_ERROR_UPDATING_ALLOCATION_TITLE
                : Constants.TranslationKeys.GOAL_DIALOG_ERROR_ADDING_ALLOCATION_TITLE;
            WindowUtils.showErrorDialog(
                i18nService.tr(errorTitle),
                e.getMessage());
        }
    }

    @FXML
    protected void handleCancel() {
        closeDialog();
    }

    private void setupToggleGroups() {
        assetTypeGroup = new ToggleGroup();
        bondRadioButton.setToggleGroup(assetTypeGroup);
        tickerRadioButton.setToggleGroup(assetTypeGroup);
        bondRadioButton.setSelected(true);

        allocationTypeGroup = new ToggleGroup();
        percentageRadioButton.setToggleGroup(allocationTypeGroup);
        quantityRadioButton.setToggleGroup(allocationTypeGroup);
        valueRadioButton.setToggleGroup(allocationTypeGroup);
        percentageRadioButton.setSelected(true);
    }

    private void setupListeners() {
        assetTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            loadAssets();
            updateAssetInfo();
        });

        assetComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateAssetInfo();
        });

        allocationTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            updateAllocationValueLabel();
        });
    }

    private void loadAssets() {
        assetComboBox.getItems().clear();

        if (bondRadioButton.isSelected()) {
            List<Bond> bonds = bondRepository.findByArchivedFalseOrderByNameAsc();
            assetComboBox.getItems().addAll(bonds);
        } else {
            List<Ticker> tickers = tickerRepository.findAllByIsArchivedFalseOrderBySymbolAsc();
            assetComboBox.getItems().addAll(tickers);
        }
    }

    private void updateAssetInfo() {
        Object selectedAsset = assetComboBox.getValue();
        if (selectedAsset == null) {
            assetInfoPane.setVisible(false);
            assetInfoPane.setManaged(false);
            return;
        }

        assetInfoPane.setVisible(true);
        assetInfoPane.setManaged(true);

        if (selectedAsset instanceof Bond) {
            Bond bond = (Bond) selectedAsset;
            BigDecimal investedValue = bondService.getInvestedValue(bond);
            BigDecimal quantity = bondService.getCurrentQuantity(bond);
            
            currentValueLabel.setText(String.format("$ %.2f", investedValue));
            availableQuantityLabel.setText(String.format("%.4f", quantity));
        } else if (selectedAsset instanceof Ticker) {
            Ticker ticker = (Ticker) selectedAsset;
            BigDecimal currentValue = ticker.getCurrentUnitValue()
                .multiply(ticker.getCurrentQuantity());
            
            currentValueLabel.setText(String.format("$ %.2f", currentValue));
            availableQuantityLabel.setText(String.format("%.8f", ticker.getCurrentQuantity()));
        }
    }

    private void updateAllocationValueLabel() {
        if (percentageRadioButton.isSelected()) {
            allocationValueLabel.setText("Percentage (%)");
            allocationValueField.setPromptText("0.00");
        } else if (quantityRadioButton.isSelected()) {
            allocationValueLabel.setText("Quantity");
            allocationValueField.setPromptText("0.00");
        } else {
            allocationValueLabel.setText("Value ($)");
            allocationValueField.setPromptText("0.00");
        }
    }

    private void populateFields() {
        if (existingAllocation == null) {
            return;
        }

        if (existingAllocation.getAssetType() == GoalAssetType.BOND) {
            bondRadioButton.setSelected(true);
        } else {
            tickerRadioButton.setSelected(true);
        }

        loadAssets();

        Object asset = findAssetById(existingAllocation.getAssetId());
        if (asset != null) {
            assetComboBox.setValue(asset);
        }

        switch (existingAllocation.getAllocationType()) {
            case PERCENTAGE:
                percentageRadioButton.setSelected(true);
                break;
            case QUANTITY:
                quantityRadioButton.setSelected(true);
                break;
            case VALUE:
                valueRadioButton.setSelected(true);
                break;
        }

        allocationValueField.setText(existingAllocation.getAllocationValue().toString());
    }

    private Object findAssetById(Integer assetId) {
        if (bondRadioButton.isSelected()) {
            return bondRepository.findById(assetId).orElse(null);
        } else {
            return tickerRepository.findById(assetId).orElse(null);
        }
    }

    private Integer getAssetId(Object asset) {
        if (asset instanceof Bond) {
            return ((Bond) asset).getId();
        } else if (asset instanceof Ticker) {
            return ((Ticker) asset).getId();
        }
        return null;
    }

    private AllocationType getAllocationType() {
        if (percentageRadioButton.isSelected()) {
            return AllocationType.PERCENTAGE;
        } else if (quantityRadioButton.isSelected()) {
            return AllocationType.QUANTITY;
        } else {
            return AllocationType.VALUE;
        }
    }

    private boolean validateFields() {
        if (!isTemporaryMode && currentGoal == null) {
            WindowUtils.showErrorDialog(
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_NO_GOAL_SELECTED_TITLE),
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_NO_GOAL_SELECTED_MESSAGE));
            return false;
        }

        if (assetComboBox.getValue() == null) {
            WindowUtils.showErrorDialog(
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_NO_ASSET_SELECTED_TITLE),
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_NO_ASSET_SELECTED_MESSAGE));
            return false;
        }

        if (allocationValueField.getText().isEmpty()) {
            WindowUtils.showErrorDialog(
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_TITLE),
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_EMPTY_FIELDS_MESSAGE));
            return false;
        }

        try {
            BigDecimal value = new BigDecimal(allocationValueField.getText());
            if (value.compareTo(BigDecimal.ZERO) <= 0) {
                WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_INVALID_ALLOCATION_VALUE_TITLE),
                    i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_INVALID_ALLOCATION_VALUE_MESSAGE));
                return false;
            }
        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_INVALID_ALLOCATION_VALUE_TITLE),
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_INVALID_ALLOCATION_VALUE_MESSAGE));
            return false;
        }

        return true;
    }

    private void closeDialog() {
        Stage stage = (Stage) assetComboBox.getScene().getWindow();
        stage.close();
    }

    private String getAssetName(Object asset) {
        if (asset instanceof Bond) {
            return ((Bond) asset).getName();
        } else if (asset instanceof Ticker) {
            return ((Ticker) asset).getSymbol();
        }
        return "Unknown";
    }
}
