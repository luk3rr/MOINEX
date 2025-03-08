/*
 * Filename: AttributeAlreadySetException.java
 * Created on: March  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.exceptions;

/**
 * Exception thrown when the ticker type is invalid
 */
public class AttributeAlreadySetException extends RuntimeException
{
    public AttributeAlreadySetException(String message)
    {
        super(message);
    }
}
