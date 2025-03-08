/*
 * Filename: SameSourceDestionationException.java
 * Created on: March  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.exceptions;

/**
 * Exception thrown when the source and destination are the same
 */
public class SameSourceDestionationException extends RuntimeException
{
    public SameSourceDestionationException(String explanation)
    {
        super(explanation);
    }
}
