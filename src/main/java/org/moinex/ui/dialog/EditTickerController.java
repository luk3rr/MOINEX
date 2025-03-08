/*
 * Filename: EditTickerController.java
 * Created on: January  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.investment.Ticker;
import org.moinex.services.TickerService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TickerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Ticker dialog
 */
@Controller
@NoArgsConstructor
public class EditTickerController
{
    @FXML
    private TextField nameField;

    @FXML
    private TextField symbolField;

    @FXML
    private TextField currentPriceField;

    @FXML
    private TextField quantityField;

    @FXML
    private TextField avgUnitPriceField;

    @FXML
    private ComboBox<TickerType> typeComboBox;

    @FXML
    private CheckBox archivedCheckBox;

    private TickerService tickerService;

    private Ticker ticker = null;

    /**
     * Constructor
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditTickerController(TickerService tickerService)
    {
        this.tickerService = tickerService;
    }

    public void setTicker(Ticker tk)
    {
        this.ticker = tk;
        nameField.setText(ticker.getName());
        symbolField.setText(ticker.getSymbol());
        currentPriceField.setText(ticker.getCurrentUnitValue().toString());
        quantityField.setText(ticker.getCurrentQuantity().toString());
        avgUnitPriceField.setText(ticker.getAverageUnitValue().toString());
        typeComboBox.setValue(ticker.getType());

        archivedCheckBox.setSelected(ticker.isArchived());
    }

    @FXML
    private void initialize()
    {
        configureComboBoxes();
        populateTypeComboBox();
        configureListeners();
    }

    @FXML
    private void handleCancel()
    {
        Stage stage = (Stage)nameField.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleSave()
    {
        // Get name and symbol and remove leading and trailing whitespaces
        String name   = nameField.getText().strip();
        String symbol = symbolField.getText().strip();

        String     currentPriceStr = currentPriceField.getText();
        TickerType type            = typeComboBox.getValue();
        String     quantityStr     = quantityField.getText();
        String     avgUnitPriceStr = avgUnitPriceField.getText();

        if (name == null || symbol == null || currentPriceStr == null || type == null ||
            name.strip().isEmpty() || symbol.strip().isEmpty() ||
            currentPriceStr.strip().isEmpty())
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");

            return;
        }

        // If quantity is set, then avgUnitPrice must be set or vice-versa
        if ((quantityStr == null || quantityStr.strip().isEmpty()) &&
                !(avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()) ||
            (avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()) &&
                !(quantityStr == null || quantityStr.strip().isEmpty()))
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Quantity must be set if average unit price is set or vice-versa");

            return;
        }

        try
        {
            BigDecimal currentPrice = new BigDecimal(currentPriceStr);

            BigDecimal quantity;
            BigDecimal avgUnitPrice;

            if ((quantityStr == null || quantityStr.strip().isEmpty()) &&
                (avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()))
            {
                quantity     = BigDecimal.ZERO;
                avgUnitPrice = BigDecimal.ZERO;
            }
            else
            {
                quantity     = new BigDecimal(quantityStr);
                avgUnitPrice = new BigDecimal(avgUnitPriceStr);
            }

            boolean archived = archivedCheckBox.isSelected();

            // Check if has any modification
            if (ticker.getName().equals(name) && ticker.getSymbol().equals(symbol) &&
                ticker.getCurrentUnitValue().compareTo(currentPrice) == 0 &&
                ticker.getType().equals(type) &&
                ticker.getCurrentQuantity().compareTo(quantity) == 0 &&
                ticker.getAverageUnitValue().compareTo(avgUnitPrice) == 0 &&
                ticker.isArchived() == archived)
            {
                WindowUtils.showInformationDialog("No changes",
                                                  "No changes were to the ticker");

                return;
            }
            else // If there is any modification, update the ticker
            {
                ticker.setName(name);
                ticker.setSymbol(symbol);
                ticker.setCurrentUnitValue(currentPrice);
                ticker.setType(type);
                ticker.setCurrentQuantity(quantity);
                ticker.setAverageUnitValue(avgUnitPrice);
                ticker.setArchived(archived);

                tickerService.updateTicker(ticker);

                WindowUtils.showSuccessDialog("Ticker updated",
                                              "The ticker updated successfully.");
            }

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.showErrorDialog("Invalid number", "Invalid price or quantity");
        }
        catch (EntityNotFoundException | IllegalArgumentException e)
        {
            WindowUtils.showErrorDialog("Error while adding ticker", e.getMessage());
        }
    }

    private void populateTypeComboBox()
    {
        typeComboBox.getItems().addAll(TickerType.values());
    }

    private void configureComboBoxes()
    {
        UIUtils.configureComboBox(typeComboBox, TickerType::name);
    }

    private void configureListeners()
    {
        currentPriceField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
                {
                    currentPriceField.setText(oldValue);
                }
            });

        quantityField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
            {
                quantityField.setText(oldValue);
            }
        });

        avgUnitPriceField.textProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (!newValue.matches(Constants.INVESTMENT_VALUE_REGEX))
                {
                    avgUnitPriceField.setText(oldValue);
                }
            });
    }
}
