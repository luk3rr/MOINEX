/*
 * Filename: MoinexException.kt (original filename: MoinexException.java)
 * Created on: [Original date]
 * Author: [Original author]
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.exception

sealed class MoinexException(
    message: String,
) : RuntimeException(message) {
    /** Exception thrown when there is an exception fetching data from an API */
    class APIFetchException(
        message: String,
    ) : MoinexException(message)

    /** Exception thrown when the application is shutting down */
    class ApplicationShuttingDownException(
        message: String,
    ) : MoinexException(message)

    /** Exception thrown when an attribute is already set */
    class AttributeAlreadySetException(
        message: String,
    ) : MoinexException(message)

    /** Exception thrown when there are not enough resources to perform an operation */
    class IncompleteGoalException(
        message: String,
    ) : MoinexException(message)

    /** Exception thrown when there are not enough resources to perform an operation */
    class InsufficientResourcesException(
        message: String,
    ) : MoinexException(message)

    /** Exception thrown when the ticker type is invalid */
    class InvalidTickerTypeException(
        message: String,
    ) : MoinexException(message)

    /** Exception thrown when the source and destination are the same */
    class SameSourceDestinationException(
        message: String,
    ) : MoinexException(message)

    /** Exception thrown when a script is not found */
    class ScriptNotFoundException(
        message: String,
    ) : MoinexException(message)

    /** Exception thrown when user tries to transfer money from a master wallet to its virtual wallet */
    class TransferFromMasterToVirtualWalletException(
        message: String,
    ) : MoinexException(message)
}
