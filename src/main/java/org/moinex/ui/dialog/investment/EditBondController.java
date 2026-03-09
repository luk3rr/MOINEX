/*
 * Filename: EditBondController.java
 * Created on: January  3, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import com.jfoenix.controls.JFXButton;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.model.enums.BondType;
import org.moinex.model.enums.InterestIndex;
import org.moinex.model.enums.InterestType;
import org.moinex.model.investment.Bond;
import org.moinex.service.BondService;
import org.moinex.service.PreferencesService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public final class EditBondController {

    @FXML private TextField nameField;
    @FXML private TextField symbolField;
    @FXML private ComboBox<BondType> bondTypeComboBox;
    @FXML private TextField issuerField;
    @FXML private DatePicker maturityDatePicker;
    @FXML private ComboBox<InterestType> interestTypeComboBox;
    @FXML private ComboBox<InterestIndex> interestIndexComboBox;
    @FXML private TextField interestRateField;
    @FXML private CheckBox archivedCheckBox;
    @FXML private JFXButton saveButton;
    @FXML private JFXButton cancelButton;

    private BondService bondService;
    private PreferencesService preferencesService;
    private Bond bond = null;

    @Autowired
    public EditBondController(BondService bondService, PreferencesService preferencesService) {
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

    public void setBond(Bond bond) {
        this.bond = bond;
        nameField.setText(bond.getName());
        symbolField.setText(bond.getSymbol());
        bondTypeComboBox.setValue(bond.getType());
        issuerField.setText(bond.getIssuer());

        if (bond.getMaturityDate() != null) {
            maturityDatePicker.setValue(bond.getMaturityDate());
        }

        interestTypeComboBox.setValue(bond.getInterestType());
        interestIndexComboBox.setValue(bond.getInterestIndex());

        if (bond.getInterestRate() != null) {
            interestRateField.setText(bond.getInterestRate().toString());
        }

        archivedCheckBox.setSelected(bond.getArchived());
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
        boolean archived = archivedCheckBox.isSelected();

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

        // Check if there are any modifications
        boolean hasChanges =
                !bond.getName().equals(name)
                        || !bondType.equals(bond.getType())
                        || !maturityDate.atStartOfDay().equals(bond.getMaturityDate())
                        || archived != bond.getArchived();

        if (symbol != null && !symbol.isBlank()) {
            hasChanges = hasChanges || !symbol.equals(bond.getSymbol());
        } else {
            hasChanges = hasChanges || bond.getSymbol() != null;
        }

        if (issuer != null && !issuer.isBlank()) {
            hasChanges = hasChanges || !issuer.equals(bond.getIssuer());
        } else {
            hasChanges = hasChanges || bond.getIssuer() != null;
        }

        if (interestType != null) {
            hasChanges = hasChanges || !interestType.equals(bond.getInterestType());
        } else {
            hasChanges = hasChanges || bond.getInterestType() != null;
        }

        if (interestIndex != null) {
            hasChanges = hasChanges || !interestIndex.equals(bond.getInterestIndex());
        } else {
            hasChanges = hasChanges || bond.getInterestIndex() != null;
        }

        if (interestRate != null) {
            hasChanges =
                    hasChanges
                            || (bond.getInterestRate() == null
                                    || interestRate.compareTo(bond.getInterestRate()) != 0);
        } else {
            hasChanges = hasChanges || bond.getInterestRate() != null;
        }

        if (!hasChanges) {
            WindowUtils.showInformationDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.INVESTMENT_DIALOG_NO_CHANGES_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.BOND_DIALOG_NO_CHANGES_MESSAGE));
            return;
        }

        try {
            bondService.updateBond(
                    bond.getId(),
                    name,
                    symbol != null && !symbol.isBlank() ? symbol : null,
                    bondType,
                    issuer != null && !issuer.isBlank() ? issuer : null,
                    maturityDate.atStartOfDay(),
                    interestType,
                    interestIndex,
                    interestRate);

            // Update archived status if changed
            if (archived != bond.getArchived()) {
                if (archived) {
                    bondService.archiveBond(bond.getId());
                } else {
                    bondService.unarchiveBond(bond.getId());
                }
            }

            WindowUtils.showSuccessDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.BOND_DIALOG_UPDATED_TITLE),
                    preferencesService.translate(
                            Constants.TranslationKeys.BOND_DIALOG_UPDATED_MESSAGE));

            Stage stage = (Stage) saveButton.getScene().getWindow();
            stage.close();

        } catch (EntityNotFoundException | IllegalArgumentException e) {
            WindowUtils.showErrorDialog(
                    preferencesService.translate(
                            Constants.TranslationKeys.BOND_DIALOG_ERROR_UPDATING_TITLE),
                    e.getMessage());
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
