/*
 * Filename: UIUtils.java
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
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
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.service.I18nService;
import org.moinex.service.UserPreferencesService;
import org.moinex.util.enums.*;
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
     * @deprecated Use {@link #setDatePickerFormat(DatePicker, I18nService)} instead
     */
    @Deprecated
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
     * Format the date picker to display the date according to the locale
     * This also configures the internal DatePicker popup calendar to use the locale
     *
     * @param datePicker The date picker to format
     * @param i18nService The I18nService instance for locale
     */
    public static void setDatePickerFormat(DatePicker datePicker, I18nService i18nService) {
        Locale locale = i18nService.getLocale();

        Locale.setDefault(locale);

        // Configure the date formatter for the text field
        DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT)
                        .withLocale(locale);

        datePicker.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(LocalDate date) {
                        return date != null ? date.format(formatter) : "";
                    }

                    @Override
                    public LocalDate fromString(String string) {
                        if (string == null || string.trim().isEmpty()) {
                            return null;
                        }
                        try {
                            return LocalDate.parse(string, formatter);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                });

        // Set the chronology to use the locale
        datePicker.setChronology(java.time.chrono.IsoChronology.INSTANCE);
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
        loadContentIntoTab(tab, fxmlPath, cssPath, springContext, resourceClass, null);
    }

    public static void loadContentIntoTab(
            Tab tab,
            String fxmlPath,
            String cssPath,
            ConfigurableApplicationContext springContext,
            Class<?> resourceClass,
            ResourceBundle resources)
            throws IOException {

        FXMLLoader loader = new FXMLLoader(resourceClass.getResource(fxmlPath), resources);
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

    /**
     * Translate wallet type name to localized string
     *
     * @param walletType The wallet type to translate
     * @param i18nService The I18nService instance for translation
     * @return Translated wallet type name
     */
    public static String translateWalletType(WalletType walletType, I18nService i18nService) {
        String name = walletType.getName().toLowerCase().replace(" ", "");

        // Map common wallet type names to i18n keys
        Map<String, String> typeKeyMap =
                Map.of(
                        "checkingaccount", Constants.TranslationKeys.WALLET_TYPE_CHECKING,
                        "savingsaccount", Constants.TranslationKeys.WALLET_TYPE_SAVINGS,
                        "broker", Constants.TranslationKeys.WALLET_TYPE_BROKER,
                        "criptocurrency", Constants.TranslationKeys.WALLET_TYPE_CRIPTOCURRENCY,
                        "foodvoucher", Constants.TranslationKeys.WALLET_TYPE_FOOD_VOUCHER,
                        "mealvoucher", Constants.TranslationKeys.WALLET_TYPE_MEAL_VOUCHER,
                        "wallet", Constants.TranslationKeys.WALLET_TYPE_WALLET,
                        "goal", Constants.TranslationKeys.WALLET_TYPE_GOAL,
                        "others", Constants.TranslationKeys.WALLET_TYPE_OTHERS);

        String key = typeKeyMap.getOrDefault(name, null);
        if (key != null) {
            return i18nService.tr(key);
        }

        // Fallback to original name if no translation found
        return walletType.getName();
    }

    /**
     * Translate transaction status to localized string
     *
     * @param transactionStatus The transaction status to translate
     * @param i18nService The I18nService instance for translation
     * @return Translated transaction status name
     */
    public static String translateTransactionStatus(
            TransactionStatus transactionStatus, I18nService i18nService) {
        Map<String, String> statusKeyMap =
                Map.of(
                        "pending", Constants.TranslationKeys.TRANSACTION_STATUS_PENDING,
                        "confirmed", Constants.TranslationKeys.TRANSACTION_STATUS_CONFIRMED);

        String key = statusKeyMap.getOrDefault(transactionStatus.name().toLowerCase(), null);
        if (key != null) {
            return i18nService.tr(key);
        }

        return transactionStatus.name();
    }

    public static String translateTransactionType(
            TransactionType transactionType, I18nService i18nService) {
        Map<String, String> typeKeyMap =
                Map.of(
                        "income", Constants.TranslationKeys.TRANSACTION_TYPE_INCOMES,
                        "expense", Constants.TranslationKeys.TRANSACTION_TYPE_EXPENSES);

        String key = typeKeyMap.getOrDefault(transactionType.name().toLowerCase(), null);
        if (key != null) {
            return i18nService.tr(key);
        }

        return transactionType.name();
    }

    public static String translateRecurringTransactionStatus(
            RecurringTransactionStatus rtStatus, I18nService i18nService) {
        Map<String, String> statusKeyMap =
                Map.of(
                        "active", Constants.TranslationKeys.RECURRING_TRANSACTION_STATUS_ACTIVE,
                        "inactive",
                                Constants.TranslationKeys.RECURRING_TRANSACTION_STATUS_INACTIVE);

        String key = statusKeyMap.getOrDefault(rtStatus.name().toLowerCase(), null);
        if (key != null) {
            return i18nService.tr(key);
        }

        return rtStatus.name();
    }

    public static String translateRecurringTransactionFrequency(
            RecurringTransactionFrequency frequency, I18nService i18nService) {
        Map<String, String> frequencyKeyMap =
                Map.of(
                        "daily", Constants.TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_DAILY,
                        "weekly", Constants.TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_WEEKLY,
                        "monthly",
                                Constants.TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_MONTHLY,
                        "yearly", Constants.TranslationKeys.RECURRING_TRANSACTION_FREQUENCY_YEARLY);

        String key = frequencyKeyMap.getOrDefault(frequency.name().toLowerCase(), null);
        if (key != null) {
            return i18nService.tr(key);
        }

        return frequency.name();
    }

    public static String translateCalendarEventType(
            CalendarEventType eventType, I18nService i18nService) {
        Map<String, String> eventTypeKeyMap =
                Map.of(
                        "credit_card_statement_closing",
                                Constants.TranslationKeys
                                        .CALENDAR_EVENTTYPE_CREDIT_CARD_STATEMENT_CLOSING,
                        "credit_card_due_date",
                                Constants.TranslationKeys.CALENDAR_EVENTTYPE_CREDIT_CARD_DUE_DATE,
                        "debt_payment_due_date",
                                Constants.TranslationKeys.CALENDAR_EVENTTYPE_DEBT_PAYMENT_DUE_DATE,
                        "income_receipt_date",
                                Constants.TranslationKeys.CALENDAR_EVENTTYPE_INCOME_RECEIPT_DATE);

        String key = eventTypeKeyMap.getOrDefault(eventType.name().toLowerCase(), null);
        if (key != null) {
            return i18nService.tr(key);
        }

        return eventType.getDescription();
    }

    public static String translateTickerType(TickerType tickerType, I18nService i18nService) {
        Map<String, String> tickerKeyMap =
                Map.of(
                        "stock", Constants.TranslationKeys.TICKER_TYPE_STOCK,
                        "fund", Constants.TranslationKeys.TICKER_TYPE_FUND,
                        "cryptocurrency", Constants.TranslationKeys.TICKER_TYPE_CRYPTO);

        String key = tickerKeyMap.getOrDefault(tickerType.name().toLowerCase(), null);
        if (key != null) {
            return i18nService.tr(key);
        }

        return tickerType.name();
    }

    public static String translateCreditCardInvoiceStatus(
            CreditCardInvoiceStatus crcInvoiceStatus, I18nService i18nService) {
        Map<String, String> crcInvoiceStatusKeyMap =
                Map.of(
                        "open", Constants.TranslationKeys.COMMON_CREDIT_CARD_OPEN,
                        "closed", Constants.TranslationKeys.COMMON_CREDIT_CARD_CLOSED);

        String key =
                crcInvoiceStatusKeyMap.getOrDefault(crcInvoiceStatus.name().toLowerCase(), null);
        if (key != null) {
            return i18nService.tr(key);
        }

        return crcInvoiceStatus.name();
    }

    public static String translateCreditCardCreditType(
            CreditCardCreditType crcCreditType, I18nService i18nService) {
        Map<String, String> crcCreditTypeKeyMap =
                Map.of(
                        "cashback", Constants.TranslationKeys.CREDIT_CARD_CREDIT_TYPE_CASHBACK,
                        "refund", Constants.TranslationKeys.CREDIT_CARD_CREDIT_TYPE_REFUND,
                        "reward", Constants.TranslationKeys.CREDIT_CARD_CREDIT_TYPE_REWARD);

        String key = crcCreditTypeKeyMap.getOrDefault(crcCreditType.name().toLowerCase(), null);
        if (key != null) {
            return i18nService.tr(key);
        }

        return crcCreditType.name();
    }

    /**
     * Get virtual wallet info
     *
     * @param wallet The wallet to get the info
     * @param i18nService The I18nService instance for translation
     * @return The virtual wallet info
     */
    public static String getVirtualWalletInfo(Wallet wallet, I18nService i18nService) {
        if (wallet.isMaster()) {
            return i18nService.tr(Constants.TranslationKeys.HOME_WALLET_TOOLTIP_NOT_VIRTUAL_WALLET);
        }

        return MessageFormat.format(
                i18nService.tr(Constants.TranslationKeys.HOME_WALLET_TOOLTIP_IS_VIRTUAL_WALLET),
                wallet.getMasterWallet().getName());
    }

    /**
     * Get a DateTimeFormatter for short month/year format (MMM/yy) with locale
     *
     * @param locale The locale to use for formatting
     * @return DateTimeFormatter configured with the locale
     */
    public static DateTimeFormatter getShortMonthYearFormatter(Locale locale) {
        return DateTimeFormatter.ofPattern("MMM/yy", locale);
    }

    /**
     * Get a DateTimeFormatter for full month and year format (MMMM yyyy) with locale
     *
     * @param locale The locale to use for formatting
     * @return DateTimeFormatter configured with the locale
     */
    public static DateTimeFormatter getFullMonthYearFormatter(Locale locale) {
        return DateTimeFormatter.ofPattern("MMMM yyyy", locale);
    }

    /**
     * Get a DateTimeFormatter for year format (yyyy) with locale
     *
     * @param locale The locale to use for formatting
     * @return DateTimeFormatter configured with the locale
     */
    public static DateTimeFormatter getYearFormatter(Locale locale) {
        return DateTimeFormatter.ofPattern("yyyy", locale);
    }

    /**
     * Format a LocalDateTime to short month/year format with locale
     *
     * @param dateTime The date to format
     * @param i18nService The I18nService instance for locale
     * @return Formatted date string
     */
    public static String formatShortMonthYear(LocalDateTime dateTime, I18nService i18nService) {
        return dateTime.format(getShortMonthYearFormatter(i18nService.getLocale()));
    }

    /**
     * Format a YearMonth to short month/year format with locale
     *
     * @param yearMonth The YearMonth to format
     * @param i18nService The I18nService instance for locale
     * @return Formatted date string
     */
    public static String formatShortMonthYear(YearMonth yearMonth, I18nService i18nService) {
        return yearMonth.format(getShortMonthYearFormatter(i18nService.getLocale()));
    }

    /**
     * Format a YearMonth to full month and year format with locale
     *
     * @param yearMonth The YearMonth to format
     * @param i18nService The I18nService instance for locale
     * @return Formatted date string
     */
    public static String formatFullMonthYear(YearMonth yearMonth, I18nService i18nService) {
        return yearMonth.format(getFullMonthYearFormatter(i18nService.getLocale()));
    }

    /**
     * Format a Year to year format with locale
     *
     * @param year The Year to format
     * @param i18nService The I18nService instance for locale
     * @return Formatted year string
     */
    public static String formatYear(Year year, I18nService i18nService) {
        return year.format(getYearFormatter(i18nService.getLocale()));
    }

    /**
     * Get weekday abbreviations in the current locale
     *
     * @param i18nService The I18nService instance for locale
     * @return Array of weekday abbreviations starting from Sunday
     */
    public static String[] getWeekdayAbbreviations(I18nService i18nService) {
        Locale locale = i18nService.getLocale();
        return new String[] {
            DayOfWeek.SUNDAY.getDisplayName(TextStyle.SHORT, locale),
            DayOfWeek.MONDAY.getDisplayName(TextStyle.SHORT, locale),
            DayOfWeek.TUESDAY.getDisplayName(TextStyle.SHORT, locale),
            DayOfWeek.WEDNESDAY.getDisplayName(TextStyle.SHORT, locale),
            DayOfWeek.THURSDAY.getDisplayName(TextStyle.SHORT, locale),
            DayOfWeek.FRIDAY.getDisplayName(TextStyle.SHORT, locale),
            DayOfWeek.SATURDAY.getDisplayName(TextStyle.SHORT, locale)
        };
    }

    /**
     * Get month display name in the current locale
     *
     * @param month The Month to get display name for
     * @param i18nService The I18nService instance for locale
     * @return Display name of the month
     */
    public static String getMonthDisplayName(Month month, I18nService i18nService) {
        return month.getDisplayName(TextStyle.FULL, i18nService.getLocale());
    }

    /**
     * Get month short display name in the current locale
     *
     * @param month The Month to get short display name for
     * @param i18nService The I18nService instance for locale
     * @return Short display name of the month
     */
    public static String getMonthShortDisplayName(Month month, I18nService i18nService) {
        return month.getDisplayName(TextStyle.SHORT, i18nService.getLocale());
    }

    /**
     * Format a LocalDate for display according to the locale
     * Uses SHORT format style (e.g., dd/MM/yyyy for pt-BR, M/d/yy for en)
     *
     * @param date The LocalDate to format
     * @param i18nService The I18nService instance for locale
     * @return Formatted date string for display
     */
    public static String formatDateForDisplay(LocalDate date, I18nService i18nService) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT)
                        .withLocale(i18nService.getLocale());
        return date.format(formatter);
    }

    /**
     * Format a LocalDateTime for display according to the locale
     * Uses SHORT format style for date (e.g., dd/MM/yyyy for pt-BR, M/d/yy for en)
     *
     * @param dateTime The LocalDateTime to format
     * @param i18nService The I18nService instance for locale
     * @return Formatted date string for display
     */
    public static String formatDateForDisplay(LocalDateTime dateTime, I18nService i18nService) {
        if (dateTime == null) {
            return "";
        }
        DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT)
                        .withLocale(i18nService.getLocale());
        return dateTime.format(formatter);
    }

    /**
     * Format a LocalDateTime with time for display according to the locale
     * Uses SHORT format style (e.g., dd/MM/yyyy HH:mm for pt-BR)
     *
     * @param dateTime The LocalDateTime to format
     * @param i18nService The I18nService instance for locale
     * @return Formatted date and time string for display
     */
    public static String formatDateTimeForDisplay(LocalDateTime dateTime, I18nService i18nService) {
        if (dateTime == null) {
            return "";
        }
        DateTimeFormatter formatter =
                DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)
                        .withLocale(i18nService.getLocale());
        return dateTime.format(formatter);
    }
}
