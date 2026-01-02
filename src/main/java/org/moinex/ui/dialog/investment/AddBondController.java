/*
 * Filename: AddBondController.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityExistsException;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.service.BondService;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.BondType;
import org.moinex.util.enums.InterestIndex;
import org.moinex.util.enums.InterestType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public final class AddBondController {

    @FXML private TextField nameField;
    @FXML private TextField symbolField;
    @FXML private ComboBox<BondType> bondTypeComboBox;
    @FXML private TextField currentValueField;
    @FXML private TextField investedValueField;
    @FXML private ComboBox<InterestType> interestTypeComboBox;
    @FXML private ComboBox<InterestIndex> interestIndexComboBox;
    @FXML private TextField interestRateField;
    @FXML private DatePicker maturityDatePicker;
    @FXML private JFXButton saveButton;
    @FXML private JFXButton cancelButton;

    private BondService bondService;
    private I18nService i18nService;

    @Autowired
    public AddBondController(BondService bondService, I18nService i18nService) {
        this.bondService = bondService;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        configureListeners();
        configureComboBoxes();
        populateComboBoxes();

        UIUtils.setDatePickerFormat(maturityDatePicker, i18nService);
    }

    @FXML
    protected void handleSave() {
        String name = nameField.getText();
        String symbol = symbolField.getText();
        String currentValueStr = currentValueField.getText();
        String investedValueStr = investedValueField.getText();
        BondType bondType = bondTypeComboBox.getValue();
        InterestType interestType = interestTypeComboBox.getValue();
        InterestIndex interestIndex = interestIndexComboBox.getValue();
        String interestRateStr = interestRateField.getText();
        LocalDate maturityDate = maturityDatePicker.getValue();

        if (name == null
                || symbol == null
                || currentValueStr == null
                || investedValueStr == null
                || bondType == null
                || name.isBlank()
                || symbol.isBlank()
                || currentValueStr.isBlank()
                || investedValueStr.isBlank()) {
            WindowUtils.showInformationDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        try {
            BigDecimal currentValue = new BigDecimal(currentValueStr);
            BigDecimal investedValue = new BigDecimal(investedValueStr);
            BigDecimal interestRate =
                    interestRateStr != null && !interestRateStr.isBlank()
                            ? new BigDecimal(interestRateStr)
                            : BigDecimal.ZERO;

            bondService.addBond(
                    name,
                    symbol,
                    bondType,
                    currentValue,
                    investedValue,
                    BigDecimal.ONE,
                    interestType,
                    interestIndex,
                    interestRate,
                    maturityDate);

            WindowUtils.showSuccessDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_ADDED_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_ADDED_MESSAGE));

            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();

        } catch (NumberFormatException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_INVALID_NUMBER_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_INVALID_NUMBER_MESSAGE));
        } catch (EntityExistsException e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_ALREADY_EXISTS_TITLE),
                    i18nService.tr(Constants.TranslationKeys.BOND_DIALOG_ALREADY_EXISTS_MESSAGE));
        } catch (Exception e) {
            WindowUtils.showErrorDialog(
                    i18nService.tr(Constants.TranslationKeys.DIALOG_ERROR_TITLE), e.getMessage());
        }
    }

    @FXML
    protected void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void configureComboBoxes() {
        UIUtils.configureComboBox(bondTypeComboBox, t -> UIUtils.translateBondType(t, i18nService));
        UIUtils.configureComboBox(
                interestTypeComboBox, t -> UIUtils.translateInterestType(t, i18nService));
        UIUtils.configureComboBox(
                interestIndexComboBox, t -> UIUtils.translateInterestIndex(t, i18nService));
    }

    private void populateComboBoxes() {
        bondTypeComboBox.getItems().setAll(BondType.values());
        interestTypeComboBox.getItems().setAll(InterestType.values());
        interestIndexComboBox.getItems().setAll(InterestIndex.values());
    }

    private void configureListeners() {
        currentValueField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                currentValueField.setText(oldValue);
                            }
                        });

        investedValueField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.MONETARY_VALUE_REGEX)) {
                                investedValueField.setText(oldValue);
                            }
                        });

        interestRateField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.INTEREST_RATE_REGEX)) {
                                interestRateField.setText(oldValue);
                            }
                        });
    }
}
