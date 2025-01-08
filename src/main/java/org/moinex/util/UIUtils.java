/*
 * Filename: UIUtils.java
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Utility class for UI-related functionalities
 */
public final class UIUtils
{
    private static final DecimalFormat currencyFormat =
        new DecimalFormat(Constants.CURRENCY_FORMAT);

    private static final DecimalFormat percentageFormat =
        new DecimalFormat(Constants.PERCENTAGE_FORMAT);

    /**
     * Prevent instantiation
     */
    private UIUtils() { }

    /**
     * Add a tooltip to a XYChart node
     * @param node The node to add the tooltip
     * @param text The text of the tooltip
     */
    static public void AddTooltipToXYChartNode(Node node, String text)
    {
        node.setOnMouseEntered(event -> { node.setStyle("-fx-opacity: 0.7;"); });
        node.setOnMouseExited(event -> { node.setStyle("-fx-opacity: 1;"); });

        AddTooltipToNode(node, text);
    }

    /**
     * Add a tooltip to a node
     * @param node The node to add the tooltip
     * @param text The text of the tooltip
     */
    static public void AddTooltipToNode(Node node, String text)
    {
        Tooltip tooltip = new Tooltip(text);
        tooltip.getStyleClass().add(Constants.TOOLTIP_STYLE);
        tooltip.setShowDelay(Duration.seconds(Constants.TOOLTIP_ANIMATION_DELAY));
        tooltip.setHideDelay(Duration.seconds(Constants.TOOLTIP_ANIMATION_DURATION));

        Tooltip.install(node, tooltip);
    }

    /**
     * Format a number to a currency string with dynamic precision
     * @param value The value to be formatted
     */
    public static String FormatCurrency(Number value)
    {
        DecimalFormat dynamicFormat = new DecimalFormat(Constants.CURRENCY_FORMAT);

        // Determine the number of fraction digits dynamically
        Integer fractionDigits;
        if (value instanceof BigDecimal)
        {
            BigDecimal bigDecimalValue = (BigDecimal)value;
            fractionDigits             = DetermineFractionDigits(bigDecimalValue);
        }
        else
        {
            // Default to 2 fraction digits for other Number types
            fractionDigits = 2;
        }

        dynamicFormat.setMinimumFractionDigits(fractionDigits);
        dynamicFormat.setMaximumFractionDigits(fractionDigits);

        return dynamicFormat.format(value);
    }

    /**
     * Determines the number of fraction digits dynamically based on a BigDecimal value
     * @param value The BigDecimal value
     * @return Number of fraction digits
     */
    private static Integer DetermineFractionDigits(BigDecimal value)
    {
        if (value.compareTo(BigDecimal.ZERO) == 0)
        {
            return 2;
        }

        BigDecimal absValue = value.stripTrailingZeros().abs();
        Integer    scale    = absValue.scale();

        return scale <= 2 ? 2 : scale;
    }

    /**
     * Format a number to percentage string
     * @param value The value to be formatted
     */
    static public String FormatPercentage(Number value)
    {
        return percentageFormat.format(value) + " %";
    }

    /**
     * Format the date picker to display the date in a specific format
     * @param datePicker The date picker to format
     */
    static public void SetDatePickerFormat(DatePicker datePicker)
    {
        // Set how the date is displayed in the date picker
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date)
            {
                return date != null ? date.format(Constants.DATE_FORMATTER_NO_TIME)
                                    : "";
            }

            @Override
            public LocalDate fromString(String string)
            {
                return LocalDate.parse(string, Constants.DATE_FORMATTER_NO_TIME);
            }
        });
    }

    /**
     * Format the last four digits of a credit card number
     * @param lastFourDigits The last four digits of the credit card number
     * @return Formatted credit card number string
     */
    static public String FormatCreditCardNumber(String lastFourDigits)
    {
        if (lastFourDigits.length() != 4)
        {
            throw new IllegalArgumentException(
                "The input must contain exactly 4 digits.");
        }

        return Constants.CREDIT_CARD_NUMBER_FORMAT.replace("####", lastFourDigits);
    }

    /**
     * Reset the text of a label to "-"
     * @param label The label to reset
     */
    static public void ResetLabel(Label label)
    {
        label.setText("-");
        SetLabelStyle(label, Constants.NEUTRAL_BALANCE_STYLE);
    }

    /**
     * Set the style of a label
     * @param label The label to set the style
     * @param style The style to set
     */
    static public void SetLabelStyle(Label label, String style)
    {
        label.getStyleClass().removeAll(Constants.NEGATIVE_BALANCE_STYLE,
                                        Constants.POSITIVE_BALANCE_STYLE,
                                        Constants.NEUTRAL_BALANCE_STYLE);

        label.getStyleClass().add(style);
    }
}
