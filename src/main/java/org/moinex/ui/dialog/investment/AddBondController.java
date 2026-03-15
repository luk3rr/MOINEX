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
import org.moinex.model.enums.BondType;
import org.moinex.model.enums.InterestIndex;
import org.moinex.model.enums.InterestType;
import org.moinex.model.investment.Bond;
import org.moinex.service.PreferencesService;
import org.moinex.service.investment.BondService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public final class AddBondController {

    @FXML private TextField nameField;
    @FXML private TextField symbolField;
    @FXML private ComboBox<BondType> bondTypeComboBox;
    @FXML private TextField issuerField;
    @FXML private DatePicker maturityDatePicker;
    @FXML private ComboBox<InterestType> interestTypeComboBox;
    @FXML private ComboBox<InterestIndex> interestIndexComboBox;
    @FXML private TextField interestRateField;
    @FXML private JFXButton saveButton;
    @FXML private JFXButton cancelButton;

    private BondService bondService;
    private PreferencesService preferencesService;

    @Autowired
    public AddBondController(BondService bondService, PreferencesService preferencesService) {
        this.bondService = bondService;
        this.preferencesService = preferencesService;
    }

    @FXML
    public void initialize() {
        configureComboBoxes();
        populateComboBoxes();
        configureListeners();

        UIUtils.setDatePickerFormat(maturityDatePicker, preferencesService);
    }

    @FXML
    protected void handleSave() {
        String name = nameField.getText();
        String symbol = symbolField.getText();
        String issuer = issuerField.getText();
        BondType bondType = bondTypeComboBox.getValue();
        LocalDate maturityDate = maturityDatePicker.getValue();
        InterestType interestType = interestTypeComboBox.getValue();
        InterestIndex interestIndex = interestIndexComboBox.getValue();
        String interestRateStr = interestRateField.getText();

        if (name == null || bondType == null || name.isBlank() || maturityDate == null) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.BOND_DIALOG_EMPTY_FIELDS_MESSAGE));
            return;
        }

        BigDecimal interestRate = null;
        if (interestRateStr != null && !interestRateStr.isBlank()) {
            try {
                interestRate = new BigDecimal(interestRateStr);
            } catch (NumberFormatException e) {
                WindowUtils.showErrorDialog(
                        preferencesService.translate(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                        preferencesService.translate(
                                Constants.TranslationKeys
                                        .INVESTMENT_DIALOG_INVALID_NUMBER_MESSAGE));
                return;
            }
        }

        try {
            bondService.createBond(
                    new Bond(
                            null,
                            name,
                            symbol,
                            bondType,
                            issuer,
                            maturityDate,
                            interestType,
                            interestIndex,
                            interestRate,
                            false));

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(Constants.TranslationKeys.BOND_DIALOG_ADDED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.BOND_DIALOG_ADDED_MESSAGE));

            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();

        } catch (EntityExistsException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.BOND_DIALOG_ALREADY_EXISTS_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.BOND_DIALOG_ALREADY_EXISTS_MESSAGE));
        } catch (Exception e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(Constants.TranslationKeys.DIALOG_ERROR_TITLE),
                    e.getMessage());
        }
    }

    @FXML
    protected void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void configureComboBoxes() {
        UIUtils.configureComboBox(
                bondTypeComboBox, t -> UIUtils.translateBondType(t, preferencesService));
        UIUtils.configureComboBox(
                interestTypeComboBox, t -> UIUtils.translateInterestType(t, preferencesService));
        UIUtils.configureComboBox(interestIndexComboBox, InterestIndex::name);
    }

    private void populateComboBoxes() {
        bondTypeComboBox.getItems().setAll(BondType.values());
        interestTypeComboBox.getItems().setAll(InterestType.values());
        interestIndexComboBox.getItems().setAll(InterestIndex.values());
    }

    private void configureListeners() {
        interestRateField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX)) {
                                interestRateField.setText(oldValue);
                            }
                        });
    }
}
