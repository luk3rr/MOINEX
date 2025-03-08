/*
 * Filename: RecurringTransactionAlreadyStoppedException.java
 * Created on: March  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.exceptions;

/**
 * Exception thrown when a recurring transaction is already stopped
 */
public class RecurringTransactionAlreadyStoppedException extends RuntimeException
{
    public RecurringTransactionAlreadyStoppedException(String explanation)
    {
        super(explanation);
    }
}
