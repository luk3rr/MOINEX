/*
 * Filename: ResourceAlreadyUpdatingException.java
 * Created on: March  7, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.exceptions;

/**
 * Exception thrown when a resource is already being updated
 */
public class ResourceAlreadyUpdatingException extends RuntimeException
{
    public ResourceAlreadyUpdatingException(String explanation)
    {
        super(explanation);
    }
}
