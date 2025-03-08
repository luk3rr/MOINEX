/*
 * Filename: APIFetchException.java
 * Created on: March  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.exceptions;

/**
 * Exception thrown when there is an error fetching data from an API
 */
public class APIFetchException extends RuntimeException
{
    public APIFetchException(String explanation)
    {
        super(explanation);
    }
}
