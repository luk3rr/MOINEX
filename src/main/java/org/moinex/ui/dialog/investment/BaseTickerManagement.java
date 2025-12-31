/*
 * Filename: BaseTickerManagement.java
 * Created on: March  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.service.I18nService;
import org.moinex.service.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.enums.TickerType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class to implement common methods for AddTickerController and EditTickerController
 */
@NoArgsConstructor
public abstract class BaseTickerManagement {
    @FXML protected TextField nameField;

    @FXML protected TextField symbolField;

    @FXML protected TextField currentPriceField;

    @FXML protected TextField quantityField;

    @FXML protected TextField avgUnitPriceField;

    @FXML protected ComboBox<TickerType> typeComboBox;

    protected TickerService tickerService;
    protected I18nService i18nService;

    /**
     * Constructor
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    protected BaseTickerManagement(TickerService tickerService, I18nService i18nService) {
        this.tickerService = tickerService;
        this.i18nService = i18nService;
    }

    @FXML
    protected void initialize() {
        configureComboBoxes();
        populateTypeComboBox();
        configureListeners();
    }

    @FXML
    protected void handleCancel() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    protected abstract void handleSave();

    protected void populateTypeComboBox() {
        typeComboBox.getItems().setAll(TickerType.values());
    }

    protected void configureComboBoxes() {
        UIUtils.configureComboBox(typeComboBox, t -> UIUtils.translateTickerType(t, i18nService));
    }

    protected void configureListeners() {
        currentPriceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX)) {
                                currentPriceField.setText(oldValue);
                            }
                        });

        quantityField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX)) {
                                quantityField.setText(oldValue);
                            }
                        });

        avgUnitPriceField
                .textProperty()
                .addListener(
                        (observable, oldValue, newValue) -> {
                            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX)) {
                                avgUnitPriceField.setText(oldValue);
                            }
                        });
    }
}
