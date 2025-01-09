/*
 * Filename: EditTickerController.java
 * Created on: January  9, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.dialog;

import java.math.BigDecimal;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.moinex.entities.investment.Ticker;
import org.moinex.services.TickerService;
import org.moinex.util.TickerType;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Edit Ticker dialog
 */
@Controller
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
    private ComboBox<String> typeComboBox;

    @FXML
    private CheckBox archivedCheckBox;

    @Autowired
    private ConfigurableApplicationContext springContext;

    private TickerService tickerService;

    private Ticker tickerToUpdate;

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

    public void SetTicker(Ticker tk)
    {
        this.tickerToUpdate = tk;

        nameField.setText(tk.GetName());
        symbolField.setText(tk.GetSymbol());
        currentPriceField.setText(tk.GetCurrentUnitValue().toString());
        quantityField.setText(tk.GetCurrentQuantity().toString());
        avgUnitPriceField.setText(tk.GetAveragePrice().toString());
        typeComboBox.setValue(tk.GetType().toString());

        archivedCheckBox.setSelected(tk.IsArchived());
    }

    @FXML
    private void initialize()
    {
        ConfigureTypeComboBox();
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

        String currentPriceStr = currentPriceField.getText();
        String typeStr         = typeComboBox.getValue();
        String quantityStr     = quantityField.getText();
        String avgUnitPriceStr = avgUnitPriceField.getText();

        if (name == null || symbol == null || currentPriceStr == null ||
            typeStr == null || name.strip().isEmpty() || symbol.strip().isEmpty() ||
            currentPriceStr.strip().isEmpty())
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Empty fields",
                                        "Please fill all required fields");

            return;
        }

        // If quantity is set, then avgUnitPrice must be set or vice-versa
        if ((quantityStr == null || quantityStr.strip().isEmpty()) &&
                !(avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()) ||
            (avgUnitPriceStr == null || avgUnitPriceStr.strip().isEmpty()) &&
                !(quantityStr == null || quantityStr.strip().isEmpty()))
        {
            WindowUtils.ShowInformationDialog(
                "Info",
                "Invalid fields",
                "Quantity must be set if average unit price is set or vice-versa");

            return;
        }

        try
        {
            BigDecimal currentPrice = new BigDecimal(currentPriceStr);

            TickerType type = TickerType.valueOf(typeStr);

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

            Boolean archived = archivedCheckBox.isSelected();

            // Check if has any modification
            if (tickerToUpdate.GetName().equals(name) &&
                tickerToUpdate.GetSymbol().equals(symbol) &&
                tickerToUpdate.GetCurrentUnitValue().compareTo(currentPrice) == 0 &&
                tickerToUpdate.GetType().equals(type) &&
                tickerToUpdate.GetCurrentQuantity().compareTo(quantity) == 0 &&
                tickerToUpdate.GetAveragePrice().compareTo(avgUnitPrice) == 0 &&
                tickerToUpdate.IsArchived().equals(archived))
            {
                WindowUtils.ShowInformationDialog("Info",
                                                  "No changes",
                                                  "No changes were to the ticker");

                return;
            }
            else // If there is any modification, update the ticker
            {
                tickerToUpdate.SetName(name);
                tickerToUpdate.SetSymbol(symbol);
                tickerToUpdate.SetCurrentUnitValue(currentPrice);
                tickerToUpdate.SetType(type);
                tickerToUpdate.SetCurrentQuantity(quantity);
                tickerToUpdate.SetAveragePrice(avgUnitPrice);
                tickerToUpdate.SetArchived(archived);

                tickerService.UpdateTicker(tickerToUpdate);

                WindowUtils.ShowSuccessDialog("Success",
                                              "Ticker updated",
                                              "The ticker updated successfully.");
            }

            Stage stage = (Stage)nameField.getScene().getWindow();
            stage.close();
        }
        catch (NumberFormatException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Invalid number",
                                        "Invalid price or quantity");
        }
        catch (RuntimeException e)
        {
            WindowUtils.ShowErrorDialog("Error",
                                        "Error while adding ticker",
                                        e.getMessage());
        }
    }

    private void ConfigureTypeComboBox()
    {
        for (TickerType type : TickerType.values())
        {
            typeComboBox.getItems().add(type.toString());
        }
    }
}
