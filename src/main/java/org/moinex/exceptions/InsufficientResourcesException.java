/*
 * Filename: InsufficientCreditException.java
 * Created on: March  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.exceptions;

/**
 * Exception thrown when there are not enough resources to perform an operation
 */
public class InsufficientResourcesException extends RuntimeException
{
    public InsufficientResourcesException(String explanation)
    {
        super(explanation);
    }
}
