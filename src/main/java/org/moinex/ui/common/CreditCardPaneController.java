/*
 * Filename: CreditCardPaneController.java
 * Created on: October 20, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import com.jfoenix.controls.JFXButton;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.NoArgsConstructor;
import org.moinex.entities.CreditCard;
import org.moinex.services.CreditCardService;
import org.moinex.ui.dialog.AddCreditCardCreditController;
import org.moinex.ui.dialog.AddCreditCardDebtController;
import org.moinex.ui.dialog.CreditCardInvoicePaymentController;
import org.moinex.ui.dialog.EditCreditCardController;
import org.moinex.ui.main.CreditCardController;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Credit Card Pane
 *
 * @note prototype is necessary so that each scene knows to which credit card it is
 * associated
 */
@Controller
@Scope("prototype") // Each instance of this controller is unique
@NoArgsConstructor
public class CreditCardPaneController
{
    @FXML
    private VBox rootVBox;

    @FXML
    private ImageView crcOperatorIcon;

    @FXML
    private Label crcName;

    @FXML
    private Label crcOperator;

    @FXML
    private Label limitLabel;

    @FXML
    private Label pendingPaymentsLabel;

    @FXML
    private Label availableLimitLabel;

    @FXML
    private Label availableRebateLabel;

    @FXML
    private Label closureDayLabel;

    @FXML
    private Label nextInvoiceLabel;

    @FXML
    private Label dueDateLabel;

    @FXML
    private Label invoiceStatusLabel;

    @FXML
    private Label invoiceMonthLabel;

    @FXML
    private Label invoiceCreditLabel;

    @FXML
    private Label limitProgressLabel;

    @FXML
    private JFXButton prevButton;

    @FXML
    private JFXButton nextButton;

    @FXML
    private Label invoiceMonthNavigatorBarLabel;

    @FXML
    private ProgressBar limitProgressBar;

    @Autowired
    private ConfigurableApplicationContext springContext;

    @Autowired
    private CreditCardController creditCardController;

    private YearMonth currentDisplayedMonth;

    private CreditCardService creditCardService;

    private CreditCard creditCard;

    /**
     * Constructor
     * @param creditCardService CreditCardService
     * @note This constructor is used for dependency injection
     */
    @Autowired
    public CreditCardPaneController(CreditCardService creditCardService)
    {
        this.creditCardService = creditCardService;
    }

    @FXML
    private void initialize()
    {
        currentDisplayedMonth = YearMonth.now();
    }

    @FXML
    private void handleAddDebt()
    {
        WindowUtils.openModalWindow(
            Constants.ADD_CREDIT_CARD_DEBT_FXML,
            "Add Credit Card Debt",
            springContext,
            (AddCreditCardDebtController controller)
                -> { controller.setCreditCard(creditCard); },
            List.of(() -> creditCardController.updateDisplay()));
    }

    @FXML
    private void handleAddCredit()
    {
        WindowUtils.openModalWindow(
            Constants.ADD_CREDIT_CARD_CREDIT_FXML,
            "Add Credit Card Credit",
            springContext,
            (AddCreditCardCreditController controller)
                -> { controller.setCreditCard(creditCard); },
            List.of(() -> creditCardController.updateDisplay()));
    }

    @FXML
    private void handleEditCreditCard()
    {
        WindowUtils.openModalWindow(
            Constants.EDIT_CREDIT_CARD_FXML,
            "Edit Credit Card",
            springContext,
            (EditCreditCardController controller)
                -> { controller.setCreditCard(creditCard); },
            List.of(() -> creditCardController.updateDisplay()));
    }

    @FXML
    private void handleArchiveCreditCard()
    {
        if (WindowUtils.showConfirmationDialog(
                "Confirmation",
                "Archive credit card " + creditCard.getName(),
                "Are you sure you want to archive this credit card?"))
        {
            try
            {
                creditCardService.archiveCreditCard(creditCard.getId());

                WindowUtils.showSuccessDialog("Success",
                                              "Credit card archived",
                                              "Credit card " + creditCard.getName() +
                                                  " has been archived");

                // Update credit card display in the main window
                creditCardController.updateDisplay();
            }
            catch (RuntimeException e)
            {
                WindowUtils.showErrorDialog("Error",
                                            "Error archiving credit card",
                                            e.getMessage());
                return;
            }
        }
    }

    @FXML
    private void handleDeleteCreditCard()
    {
        // Prevent the removal of a credit card with associated debts
        if (creditCardService.getDebtCountByCreditCard(creditCard.getId()) > 0)
        {
            WindowUtils.showErrorDialog(
                "Error",
                "Credit card has debts",
                "Cannot delete a credit card with associated debts");
            return;
        }

        if (WindowUtils.showConfirmationDialog(
                "Confirmation",
                "Delete credit card " + creditCard.getName(),
                "Are you sure you want to remove this credit card?"))
        {
            try
            {
                creditCardService.deleteCreditCard(creditCard.getId());

                WindowUtils.showSuccessDialog("Success",
                                              "Credit card deleted",
                                              "Credit card " + creditCard.getName() +
                                                  " has been deleted");

                // Update credit card display in the main window
                creditCardController.updateDisplay();
            }
            catch (RuntimeException e)
            {
                WindowUtils.showErrorDialog("Error",
                                            "Error removing credit card",
                                            e.getMessage());
                return;
            }
        }
    }

    @FXML
    private void handleShowRebates()
    { }

    @FXML
    private void handlePrevMonth()
    {
        currentDisplayedMonth = currentDisplayedMonth.minusMonths(1);
        updateInvoiceInfo();
    }

    @FXML
    private void handleNextMonth()
    {
        currentDisplayedMonth = currentDisplayedMonth.plusMonths(1);
        updateInvoiceInfo();
    }

    @FXML
    private void handleRegisterPayment()
    {
        WindowUtils.openModalWindow(
            Constants.CREDIT_CARD_INVOICE_PAYMENT_FXML,
            "Register Payment",
            springContext,
            (CreditCardInvoicePaymentController controller)
                -> { controller.setCreditCard(creditCard, currentDisplayedMonth); },
            // Update the display after the payment is registered with the current month
            List.of(
                () -> { creditCardController.updateDisplay(currentDisplayedMonth); }));
    }

    /**
     * Load the Credit Card Pane
     * @param creditCard Credit Card to load
     * @return The updated VBox
     */
    public VBox updateCreditCardPane(CreditCard crc, YearMonth month)
    {
        // If the crc is null, do not update the pane
        if (crc == null)
        {
            setDefaultValues();
            return rootVBox;
        }

        this.creditCard            = crc;
        this.currentDisplayedMonth = month;

        crcName.setText(creditCard.getName());
        crcOperator.setText(creditCard.getOperator().getName());
        crcOperatorIcon.setImage(new Image(Constants.CRC_OPERATOR_ICONS_PATH +
                                           creditCard.getOperator().getIcon()));

        BigDecimal limit = creditCard.getMaxDebt();
        BigDecimal pendingPayments =
            creditCardService.getTotalPendingPayments(creditCard.getId());

        BigDecimal limitAvailable =
            creditCardService.getAvailableCredit(creditCard.getId());

        BigDecimal rebate = creditCard.getAvailableRebate();

        limitLabel.setText(UIUtils.formatCurrency(limit));

        availableRebateLabel.setText(UIUtils.formatCurrency(rebate));

        // If rebate > 0, then set text color to green
        if (rebate.compareTo(BigDecimal.ZERO) > 0)
        {
            availableRebateLabel.setStyle("-fx-text-fill: green");
        }
        else
        {
            availableRebateLabel.setStyle("-fx-text-fill: black");
        }

        pendingPaymentsLabel.setText(UIUtils.formatCurrency(pendingPayments));

        availableLimitLabel.setText(UIUtils.formatCurrency(limitAvailable));

        // Set percentage of the usage of the limit
        BigDecimal limitProgress =
            limit.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : pendingPayments.divide(limit, 2, RoundingMode.HALF_UP);

        limitProgressBar.setProgress(limitProgress.doubleValue());
        limitProgressLabel.setText(
            UIUtils.formatPercentage(limitProgress.doubleValue() * 100));

        dueDateLabel.setText(creditCard.getBillingDueDay().toString());

        closureDayLabel.setText(creditCard.getClosingDay().toString());

        // Fromat LocalDateTime to MM/YYYY
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");

        nextInvoiceLabel.setText(
            creditCardService.getNextInvoiceDate(creditCard.getId()).format(formatter));

        updateInvoiceInfo();

        return rootVBox;
    }

    /**
     * Update the invoice information
     */
    public void updateInvoiceInfo()
    {
        if (creditCard == null)
        {
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM/yy");

        invoiceMonthNavigatorBarLabel.setText(currentDisplayedMonth.format(formatter));

        BigDecimal totalDebts =
            creditCardService.getInvoiceAmount(creditCard.getId(),
                                               currentDisplayedMonth.getMonthValue(),
                                               currentDisplayedMonth.getYear());

        invoiceMonthLabel.setText(UIUtils.formatCurrency(totalDebts));

        invoiceStatusLabel.setText(
            creditCardService
                .getInvoiceStatus(creditCard.getId(),
                                  currentDisplayedMonth.getMonthValue(),
                                  currentDisplayedMonth.getYear())
                .toString());
    }

    /**
     * Set the default values for the credit card pane
     */
    private void setDefaultValues()
    {
        crcName.setText("");
        crcOperator.setText("");
        limitLabel.setText("");
        pendingPaymentsLabel.setText("");
        availableLimitLabel.setText("");
        availableRebateLabel.setText("");
        closureDayLabel.setText("");
        nextInvoiceLabel.setText("");
        dueDateLabel.setText("");
        invoiceStatusLabel.setText("");
        invoiceMonthLabel.setText("");
        invoiceMonthNavigatorBarLabel.setText("");
        crcOperatorIcon.setImage(new Image(Constants.DEFAULT_ICON));
    }
}
