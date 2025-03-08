/*
 * Filename: CalendarEventType.java
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util.enums;

/**
 * Represents different types of events in the calendar
 */
public enum CalendarEventType
{
    CREDIT_CARD_STATEMENT_CLOSING("Credit Card Statement Closing", "#FF9800"),
    CREDIT_CARD_DUE_DATE("Credit Card Due Date", "#F44336"),
    DEBT_PAYMENT_DUE_DATE("Debt Payment Due Date", "#3F51B5"),
    INCOME_RECEIPT_DATE("Income Receipt Date", "#4CAF50");

    private final String description;
    private final String colorHex;

    CalendarEventType(String description, String colorHex)
    {
        this.description = description;
        this.colorHex    = colorHex;
    }

    public String getDescription()
    {
        return description;
    }

    public String getColorHex()
    {
        return colorHex;
    }
}
