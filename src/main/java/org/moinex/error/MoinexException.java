package org.moinex.error;

import lombok.Getter;

@Getter
public sealed class MoinexException extends RuntimeException {
    public MoinexException(String message) {
        super(message);
    }

    /**
     * Exception thrown when there is an error fetching data from an API
     */
    public static final class APIFetchException extends MoinexException
    {
        public APIFetchException(String message)
        {
            super(message);
        }
    }

    /**
     * Exception thrown when the ticker type is invalid
     */
    public static final class ApplicationShuttingDownException extends MoinexException
    {
        public ApplicationShuttingDownException(String message)
        {
            super(message);
        }
    }

    /**
     * Exception thrown when the ticker type is invalid
     */
    public static final class AttributeAlreadySetException extends MoinexException
    {
        public AttributeAlreadySetException(String message)
        {
            super(message);
        }
    }

    /**
     * Exception thrown when there are not enough resources to perform an operation
     */
    public static final class IncompleteGoalException extends MoinexException
    {
        public IncompleteGoalException(String message)
        {
            super(message);
        }
    }

    /**
     * Exception thrown when there are not enough resources to perform an operation
     */
    public static final class InsufficientResourcesException extends MoinexException
    {
        public InsufficientResourcesException(String message)
        {
            super(message);
        }
    }

    /**
     * Exception thrown when the ticker type is invalid
     */
    public static final class InvalidTickerTypeException extends MoinexException
    {
        public InvalidTickerTypeException(String message)
        {
            super(message);
        }
    }

    /**
     * Exception thrown when a recurring transaction is already stopped
     */
    public static final class RecurringTransactionAlreadyStoppedException extends MoinexException
    {
        public RecurringTransactionAlreadyStoppedException(String message)
        {
            super(message);
        }
    }

    /**
     * Exception thrown when a resource is already being updated
     */
    public static final class ResourceAlreadyUpdatingException extends MoinexException
    {
        public ResourceAlreadyUpdatingException(String message)
        {
            super(message);
        }
    }

    /**
     * Exception thrown when the source and destination are the same
     */
    public static final class SameSourceDestinationException extends MoinexException
    {
        public SameSourceDestinationException(String message)
        {
            super(message);
        }
    }

    /**
     * Exception thrown when the ticker type is invalid
     */
    public static final class ScriptNotFoundException extends MoinexException
    {
        public ScriptNotFoundException(String message)
        {
            super(message);
        }
    }
}
