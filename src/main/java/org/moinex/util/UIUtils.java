/*
 * Filename: UIUtils.java
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Chart;
import javafx.scene.control.*;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.NonNull;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.service.UserPreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Utility class for UI-related functionalities
 */
@Component
public final class UIUtils {
    private static final DecimalFormat currencyFormat =
            new DecimalFormat(Constants.CURRENCY_FORMAT);

    private static final DecimalFormat percentageFormat =
            new DecimalFormat(Constants.PERCENTAGE_FORMAT);

    private static UserPreferencesService userPreferencesService;

    @Autowired
    public UIUtils(UserPreferencesService userPreferencesService) {
        UIUtils.userPreferencesService = userPreferencesService;
    }

    /**
     * Add a tooltip to the XYChart node
     *
     * @param node The node to add the tooltip
     * @param text The text of the tooltip
     */
    public static void addTooltipToXYChartNode(Node node, String text) {
        node.setOnMouseEntered(event -> node.setStyle("-fx-opacity: 0.7;"));
        node.setOnMouseExited(event -> node.setStyle("-fx-opacity: 1;"));

        addTooltipToNode(node, text);
    }

    /**
     * Add a tooltip to a node
     *
     * @param node The node to add the tooltip
     * @param text The text of the tooltip
     */
    public static void addTooltipToNode(Node node, String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.getStyleClass().add(Constants.TOOLTIP_STYLE);
        tooltip.setShowDelay(Duration.seconds(Constants.TOOLTIP_ANIMATION_DELAY));
        tooltip.setHideDelay(Duration.seconds(Constants.TOOLTIP_ANIMATION_DURATION));

        Tooltip.install(node, tooltip);
    }

    /**
     * Remove any tooltip associated with a node.
     *
     * @param node The node to remove the tooltip from.
     */
    public static void removeTooltipFromNode(Node node) {
        Tooltip.install(node, null);
    }

    /**
     * Format a number to a currency string
     *
     * @param value The value to be formatted
     * @note Automatically formats to 2 fraction digits, rounding half up
     */
    public static String formatCurrency(Number value) {
        if (userPreferencesService != null && userPreferencesService.hideMonetaryValues()) {
            return "****";
        }

        return currencyFormat.format(value);
    }

    /**
     * Format a number to a currency string with dynamic precision
     *
     * @param value The value to be formatted
     */
    public static String formatCurrencyDynamic(Number value) {
        if (userPreferencesService != null && userPreferencesService.hideMonetaryValues()) {
            return "****";
        }

        DecimalFormat dynamicFormat = new DecimalFormat(Constants.CURRENCY_FORMAT);

        // Determine the number of fraction digits dynamically
        int fractionDigits;
        if (value instanceof BigDecimal bigDecimalValue) {
            fractionDigits = determineFractionDigits(bigDecimalValue);
        } else {
            // Default to 2 fraction digits for other Number types
            fractionDigits = 2;
        }

        dynamicFormat.setMinimumFractionDigits(fractionDigits);
        dynamicFormat.setMaximumFractionDigits(fractionDigits);

        return dynamicFormat.format(value);
    }

    /**
     * Determines the number of fraction digits dynamically based on a BigDecimal value
     *
     * @param value The BigDecimal value
     * @return Number of fraction digits
     */
    private static Integer determineFractionDigits(BigDecimal value) {
        // For values greater than 1, always display 2 decimal places
        // For values less than 1, display the necessary decimal places
        // This is especially useful for cryptocurrency values
        if (value.compareTo(BigDecimal.ONE) >= 0) {
            return 2;
        }

        BigDecimal absValue = value.stripTrailingZeros().abs();
        int scale = absValue.scale();

        return Math.max(scale, 2);
    }

    /**
     * Format a number to percentage string
     *
     * @param value The value to be formatted
     */
    public static String formatPercentage(Number value) {
        if (value.doubleValue() < Constants.NEGATIVE_PERCENTAGE_THRESHOLD) {
            return "Too much negative";
        }

        return percentageFormat.format(Math.abs(value.doubleValue())) + " %";
    }

    /**
     * Format the date picker to display the date in a specific format
     *
     * @param datePicker The date picker to format
     */
    public static void setDatePickerFormat(DatePicker datePicker) {
        // Set how the date is displayed in the date picker
        datePicker.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(LocalDate date) {
                        return date != null ? date.format(Constants.DATE_FORMATTER_NO_TIME) : "";
                    }

                    @Override
                    public LocalDate fromString(String string) {
                        return LocalDate.parse(string, Constants.DATE_FORMATTER_NO_TIME);
                    }
                });
    }

    /**
     * Format the last four digits of a credit card number
     *
     * @param lastFourDigits The last four digits of the credit card number
     * @return Formatted credit card number string
     */
    public static String formatCreditCardNumber(String lastFourDigits) {
        if (lastFourDigits.length() != 4) {
            throw new IllegalArgumentException("The input must contain exactly 4 digits.");
        }

        return Constants.CREDIT_CARD_NUMBER_FORMAT.replace("####", lastFourDigits);
    }

    /**
     * Reset the text of a label to "-"
     *
     * @param label The label to reset
     */
    public static void resetLabel(Label label) {
        label.setText("-");
        setLabelStyle(label, Constants.NEUTRAL_BALANCE_STYLE);
    }

    /**
     * Set the style of a label
     *
     * @param label The label to set the style
     * @param style The style to set
     */
    public static void setLabelStyle(Label label, String style) {
        label.getStyleClass()
                .removeAll(
                        Constants.NEGATIVE_BALANCE_STYLE,
                        Constants.POSITIVE_BALANCE_STYLE,
                        Constants.NEUTRAL_BALANCE_STYLE);

        label.getStyleClass().add(style);
    }

    /**
     * Configure a ComboBox with a display function
     *
     * @param comboBox        The ComboBox to configure
     * @param displayFunction The function to display the items
     * @param <T>             The type of the ComboBox items
     */
    public static <T> void configureComboBox(
            ComboBox<T> comboBox, Function<T, String> displayFunction) {
        comboBox.setCellFactory(
                listView ->
                        new ListCell<>() {
                            @Override
                            protected void updateItem(T item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(
                                        (item == null || empty)
                                                ? null
                                                : displayFunction.apply(item));
                            }
                        });

        comboBox.setButtonCell(
                new ListCell<>() {
                    @Override
                    protected void updateItem(T item, boolean empty) {
                        super.updateItem(item, empty);
                        setText((item == null || empty) ? null : displayFunction.apply(item));
                    }
                });

        comboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(T item) {
                        return (item == null) ? null : displayFunction.apply(item);
                    }

                    @Override
                    public T fromString(String string) {
                        return null;
                    }
                });
    }

    public static void updateWalletBalanceLabelStyle(
            @NonNull Wallet wt, @NonNull Label balanceLabel) {
        BigDecimal balance = wt.getBalance();
        balanceLabel.setText(formatCurrencyDynamic(balance));

        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            setLabelStyle(balanceLabel, Constants.NEGATIVE_BALANCE_STYLE);
        } else {
            setLabelStyle(balanceLabel, Constants.NEUTRAL_BALANCE_STYLE);
        }
    }

    public static void loadContentIntoTab(
            Tab tab,
            String fxmlPath,
            String cssPath,
            ConfigurableApplicationContext springContext,
            Class<?> resourceClass)
            throws IOException {

        FXMLLoader loader = new FXMLLoader(resourceClass.getResource(fxmlPath));
        loader.setControllerFactory(springContext::getBean);
        Parent content = loader.load();

        content.getStylesheets()
                .add(Objects.requireNonNull(resourceClass.getResource(cssPath)).toExternalForm());

        tab.setContent(content);
    }

    public static <S, T> void alignTableColumn(TableColumn<S, T> column, Pos alignment) {
        column.setCellFactory(
                col ->
                        new TableCell<>() {
                            @Override
                            protected void updateItem(T item, boolean empty) {
                                super.updateItem(item, empty);
                                if (item == null || empty) {
                                    setText(null);
                                    setStyle("");
                                } else {
                                    setText(item.toString());
                                    setAlignment(alignment);
                                }
                            }
                        });
    }

    public static void alignTableColumn(List<? extends TableColumn<?, ?>> columns, Pos alignment) {
        columns.forEach(column -> alignTableColumn(column, alignment));
    }

    public static void applyDefaultChartStyle(Chart chart) {
        chart.getStylesheets()
                .add(
                        UIUtils.class
                                .getResource(Constants.CHARTS_COLORS_STYLE_SHEET)
                                .toExternalForm());
    }
}
