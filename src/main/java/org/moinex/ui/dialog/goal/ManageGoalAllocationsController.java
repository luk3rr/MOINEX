/*
 * Filename: ManageGoalAllocationsController.java
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.goal;

import com.jfoenix.controls.JFXButton;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.dto.GoalAssetAllocationDTO;
import org.moinex.model.goal.Goal;
import org.moinex.service.GoalAssetAllocationService;
import org.moinex.service.GoalService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for managing goal asset allocations
 */
@Controller
@NoArgsConstructor
public class ManageGoalAllocationsController {

    @FXML private Label goalNameLabel;
    @FXML private Label totalValueLabel;
    @FXML private Label targetBalanceLabel;
    @FXML private TableView<GoalAssetAllocationDTO> allocationsTable;
    @FXML private TableColumn<GoalAssetAllocationDTO, String> assetTypeColumn;
    @FXML private TableColumn<GoalAssetAllocationDTO, String> assetNameColumn;
    @FXML private TableColumn<GoalAssetAllocationDTO, String> allocationTypeColumn;
    @FXML private TableColumn<GoalAssetAllocationDTO, String> allocationValueColumn;
    @FXML private TableColumn<GoalAssetAllocationDTO, String> currentValueColumn;
    @FXML private HBox selectedAllocationActionsPane;
    @FXML private JFXButton addAllocationButton;

    private GoalAssetAllocationService allocationService;
    private GoalService goalService;
    private I18nService i18nService;
    private ConfigurableApplicationContext springContext;

    private Goal currentGoal;

    @Autowired
    public ManageGoalAllocationsController(
            GoalAssetAllocationService allocationService,
            GoalService goalService,
            I18nService i18nService,
            ConfigurableApplicationContext springContext) {
        this.allocationService = allocationService;
        this.goalService = goalService;
        this.i18nService = i18nService;
        this.springContext = springContext;
    }

    @FXML
    protected void initialize() {
        setupTableColumns();
        setupTableSelectionListener();
    }

    /**
     * Sets the goal to manage allocations for
     *
     * @param goal Goal
     */
    public void setGoal(Goal goal) {
        this.currentGoal = goal;
        loadGoalInfo();
        loadAllocations();
    }

    @FXML
    protected void handleAddAllocation() {
        openAllocationDialog(null);
    }

    @FXML
    protected void handleEditAllocation() {
        GoalAssetAllocationDTO selected = allocationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openAllocationDialog(selected);
        }
    }

    @FXML
    protected void handleDeleteAllocation() {
        GoalAssetAllocationDTO selected = allocationsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            deleteAllocation(selected);
        }
    }

    @FXML
    protected void handleRefresh() {
        loadAllocations();
    }

    @FXML
    protected void handleClose() {
        Stage stage = (Stage) allocationsTable.getScene().getWindow();
        stage.close();
    }

    private void setupTableColumns() {
        assetTypeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAssetType().toString()));

        assetNameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAssetName()));

        allocationTypeColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAllocationType().toString()));

        allocationValueColumn.setCellValueFactory(cellData -> {
            BigDecimal value = cellData.getValue().getAllocationValue();
            String formatted = switch (cellData.getValue().getAllocationType()) {
                case PERCENTAGE -> String.format("%.2f%%", value);
                case QUANTITY -> String.format("%.4f", value);
                case VALUE -> String.format("$ %.2f", value);
            };
            return new SimpleStringProperty(formatted);
        });

        currentValueColumn.setCellValueFactory(cellData -> {
            BigDecimal value = cellData.getValue().getCurrentAssetValue();
            return new SimpleStringProperty(String.format("$ %.2f", value));
        });

    }

    private void setupTableSelectionListener() {
        allocationsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                boolean hasSelection = newSelection != null;
                selectedAllocationActionsPane.setVisible(hasSelection);
                selectedAllocationActionsPane.setManaged(hasSelection);
            });
    }

    private void loadGoalInfo() {
        if (currentGoal == null) {
            return;
        }

        goalNameLabel.setText(currentGoal.getName());
        targetBalanceLabel.setText(String.format("$ %.2f", currentGoal.getTargetBalance()));
        updateTotalValue();
    }

    private void loadAllocations() {
        if (currentGoal == null) {
            return;
        }

        List<GoalAssetAllocationDTO> allocations = 
            allocationService.getAllocationsByGoalId(currentGoal.getId());
        
        allocationsTable.getItems().clear();
        allocationsTable.getItems().addAll(allocations);
        
        updateTotalValue();
    }

    private void updateTotalValue() {
        if (currentGoal == null) {
            return;
        }

        BigDecimal totalValue = allocationService.calculateGoalTotalValue(currentGoal);
        totalValueLabel.setText(String.format("$ %.2f", totalValue));
    }

    private void openAllocationDialog(GoalAssetAllocationDTO existingAllocation) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ui/dialog/goal/add_goal_asset_allocation.fxml"));
            loader.setControllerFactory(springContext::getBean);
            
            Parent root = loader.load();
            
            AddGoalAssetAllocationController controller = loader.getController();
            controller.setGoal(currentGoal);
            
            if (existingAllocation != null) {
                controller.setExistingAllocation(existingAllocation);
            }
            
            Stage stage = new Stage();
            String titleKey = existingAllocation == null 
                ? Constants.TranslationKeys.GOAL_LABEL_ADD_ASSET_ALLOCATION
                : Constants.TranslationKeys.DIALOG_BUTTON_EDIT;
            stage.setTitle(i18nService.tr(titleKey));
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            stage.showAndWait();
            
            loadAllocations();
            
        } catch (IOException e) {
            WindowUtils.showErrorDialog(
                i18nService.tr(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                "Failed to open allocation dialog: " + e.getMessage());
        }
    }

    private void deleteAllocation(GoalAssetAllocationDTO allocation) {
        boolean confirmed = WindowUtils.showConfirmationDialog(
            i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_CONFIRM_DELETE_ALLOCATION_TITLE),
            i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_CONFIRM_DELETE_ALLOCATION_MESSAGE));
        
        if (!confirmed) {
            return;
        }

        try {
            allocationService.removeAllocation(allocation.getId());
            WindowUtils.showSuccessDialog(
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ALLOCATION_DELETED_TITLE),
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ALLOCATION_DELETED_MESSAGE));
            loadAllocations();
        } catch (Exception e) {
            WindowUtils.showErrorDialog(
                i18nService.tr(Constants.TranslationKeys.GOAL_DIALOG_ERROR_DELETING_ALLOCATION_TITLE),
                e.getMessage());
        }
    }
}
