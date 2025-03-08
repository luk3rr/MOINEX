/*
 * Filename: IncompleteGoalException.java
 * Created on: March  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.exceptions;

/**
 * Exception thrown when there are not enough resources to perform an operation
 */
public class IncompleteGoalException extends RuntimeException
{
    public IncompleteGoalException(String explanation)
    {
        super(explanation);
    }
}
