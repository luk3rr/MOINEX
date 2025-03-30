/*
 * Filename: EditTickerController.java
 * Created on: January  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog.investment;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.moinex.entities.investment.Ticker;
import org.moinex.services.TickerService;
import org.moinex.util.WindowUtils;
import org.moinex.util.enums.TickerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Ticker dialog
 */
@Controller
@NoArgsConstructor
public final class EditTickerController extends BaseTickerManagement
{
    @FXML
    private CheckBox archivedCheckBox;

    private Ticker ticker = null;

    /**
     * Constructor
     * @param tickerService Ticker service
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public EditTickerController(TickerService tickerService)
    {
        super(tickerService);
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
    @Override
    protected void handleSave()
    {
        // Get name and symbol and remove leading and trailing whitespaces
        String name   = nameField.getText().strip();
        String symbol = symbolField.getText().strip();

        String     currentPriceStr = currentPriceField.getText();
        TickerType type            = typeComboBox.getValue();
        String     quantityStr     = quantityField.getText();
        String     avgUnitPriceStr = avgUnitPriceField.getText();

        if (currentPriceStr == null || type == null || name.isBlank() || symbol.isBlank() || currentPriceStr.isBlank())
        {
            WindowUtils.showInformationDialog(
                "Empty fields",
                "Please fill all required fields before saving");

            return;
        }

        // If quantity is set, then avgUnitPrice must be set or vice versa
        if ((quantityStr == null || quantityStr.isBlank()) &&
                !(avgUnitPriceStr == null || avgUnitPriceStr.isBlank()) ||
            (avgUnitPriceStr == null || avgUnitPriceStr.isBlank()) &&
                !(quantityStr == null || quantityStr.isBlank()))
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

            if ((quantityStr == null || quantityStr.isBlank()) &&
                (avgUnitPriceStr == null || avgUnitPriceStr.isBlank()))
            {
                quantity     = BigDecimal.ZERO;
                avgUnitPrice = BigDecimal.ZERO;
            }
            else
            {
                assert quantityStr != null;
                quantity     = new BigDecimal(quantityStr);
                assert avgUnitPriceStr != null;
                avgUnitPrice = new BigDecimal(avgUnitPriceStr);
            }

            boolean archived = archivedCheckBox.isSelected();

            // Check if it has any modification
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
}
