/*
 * Filename: EditInvestmentTargetController.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.NoArgsConstructor;
import org.moinex.model.enums.AssetType;
import org.moinex.model.investment.InvestmentTarget;
import org.moinex.service.I18nService;
import org.moinex.service.InvestmentTargetService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public class EditInvestmentTargetController {

    @FXML private VBox targetsContainer;
    @FXML private Label totalPercentageLabel;
    @FXML private Label validationLabel;

    private InvestmentTargetService investmentTargetService;
    private I18nService i18nService;

    private List<TargetRow> targetRows = new ArrayList<>();

    @Autowired
    public EditInvestmentTargetController(
            InvestmentTargetService investmentTargetService, I18nService i18nService) {
        this.investmentTargetService = investmentTargetService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        loadInvestmentTargets();
        setupListeners();
    }

    private void loadInvestmentTargets() {
        targetsContainer.getChildren().clear();
        targetRows.clear();

        for (AssetType assetType : AssetType.values()) {
            try {
                InvestmentTarget target = investmentTargetService.getTargetByType(assetType);
                addTargetRow(assetType, target.getTargetPercentage());
            } catch (EntityNotFoundException e) {
                addTargetRow(assetType, BigDecimal.ZERO);
            }
        }

        updateTotalPercentage();
    }

    private void addTargetRow(AssetType assetType, BigDecimal targetPercentage) {
        HBox row = new HBox(10.0);
        row.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(UIUtils.translateAssetType(assetType, i18nService));
        typeLabel.setMinWidth(200.0);
        typeLabel.setStyle("-fx-font-weight: bold;");

        TextField percentageField = new TextField();
        percentageField.setText(targetPercentage.toString());
        percentageField.setPromptText("0.00");
        percentageField.setPrefWidth(100.0);

        Label percentLabel = new Label("%");

        row.getChildren().addAll(typeLabel, percentageField, percentLabel);
        targetsContainer.getChildren().add(row);

        TargetRow targetRow = new TargetRow(assetType, percentageField);
        targetRows.add(targetRow);

        percentageField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.PERCENTAGE_REGEX)) {
                                percentageField.setText(oldValue);
                            } else {
                                updateTotalPercentage();
                            }
                        });
    }

    private void setupListeners() {
        updateTotalPercentage();
    }

    private void updateTotalPercentage() {
        BigDecimal total = BigDecimal.ZERO;

        for (TargetRow row : targetRows) {
            try {
                String text = row.percentageField.getText().trim();
                if (!text.isEmpty()) {
                    BigDecimal value = new BigDecimal(text);
                    total = total.add(value);
                }
            } catch (NumberFormatException e) {
            }
        }

        totalPercentageLabel.setText(UIUtils.formatPercentage(total, i18nService));

        if (total.compareTo(new BigDecimal("100")) == 0) {
            totalPercentageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: green;");
            validationLabel.setVisible(false);
        } else {
            totalPercentageLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: red;");
            validationLabel.setText(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_TOTAL_PERCENTAGE_VALIDATION));
            validationLabel.setVisible(true);
        }
    }

    @FXML
    private void handleSave() {
        BigDecimal total = BigDecimal.ZERO;
        List<TargetData> targetDataList = new ArrayList<>();

        for (TargetRow row : targetRows) {
            try {
                String text = row.percentageField.getText().trim();
                BigDecimal percentage = text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text);

                if (percentage.compareTo(BigDecimal.ZERO) < 0
                        || percentage.compareTo(new BigDecimal("100")) > 0) {
                    WindowUtils.showErrorDialog(
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .INVESTMENT_DIALOG_INVALID_PERCENTAGE_TITLE),
                            i18nService.tr(
                                    Constants.TranslationKeys
                                            .INVESTMENT_DIALOG_INVALID_PERCENTAGE_MESSAGE));
                    return;
                }

                total = total.add(percentage);
                targetDataList.add(new TargetData(row.assetType, percentage));
            } catch (NumberFormatException e) {
                WindowUtils.showErrorDialog(
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_INVALID_PERCENTAGE_TITLE),
                        i18nService.tr(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_INVALID_PERCENTAGE_MESSAGE));
                return;
            }
        }

        if (total.compareTo(new BigDecimal("100")) != 0) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_TOTAL_PERCENTAGE_VALIDATION_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_TOTAL_PERCENTAGE_VALIDATION));
            return;
        }

        try {
            List<InvestmentTarget> allTargets = investmentTargetService.getAllActiveTargets();

            for (InvestmentTarget existingTarget : allTargets) {
                boolean found = false;
                for (TargetData data : targetDataList) {
                    if (existingTarget.getAssetType() == data.assetType) {
                        found = true;
                        if (data.percentage.compareTo(BigDecimal.ZERO) == 0) {
                            investmentTargetService.deleteTarget(existingTarget.getId());
                        } else {
                            investmentTargetService.setTarget(data.assetType, data.percentage);
                        }
                        break;
                    }
                }
                if (!found) {
                    investmentTargetService.deleteTarget(existingTarget.getId());
                }
            }

            for (TargetData data : targetDataList) {
                if (data.percentage.compareTo(BigDecimal.ZERO) > 0) {
                    investmentTargetService.setTarget(data.assetType, data.percentage);
                }
            }

            WindowUtils.showSuccessDialog(
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_TARGET_UPDATED_TITLE),
                    i18nService.tr(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_TARGET_UPDATED_MESSAGE));

            targetsContainer.getScene().getWindow().hide();
        } catch (Exception e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(
                            Constants.TranslationKeys
                                    .INVESTMENT_DIALOG_ERROR_UPDATING_TARGET_TITLE),
                    e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        targetsContainer.getScene().getWindow().hide();
    }

    private static class TargetRow {
        AssetType assetType;
        TextField percentageField;

        TargetRow(AssetType assetType, TextField percentageField) {
            this.assetType = assetType;
            this.percentageField = percentageField;
        }
    }

    private static class TargetData {
        AssetType assetType;
        BigDecimal percentage;

        TargetData(AssetType assetType, BigDecimal percentage) {
            this.assetType = assetType;
            this.percentage = percentage;
        }
    }
}
