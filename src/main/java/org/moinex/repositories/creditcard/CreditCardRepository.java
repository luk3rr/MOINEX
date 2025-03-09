/*
 * Filename: CreditCardRepository.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repositories.creditcard;

import java.util.List;
import org.moinex.entities.creditcard.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    /**
     * Check if a credit card with the given name exists
     * @param name The name of the credit card
     * @return True if a credit card with the given name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Get a credit card by its name
     * @param name The name of the credit card
     * @return The credit card with the given name
     */
    CreditCard findByName(String name);

    /**
     * Get all credit cards that are archived
     * @return A list with all credit cards that are archived
     */
    List<CreditCard> findAllByIsArchivedTrue();

    /**
     * Get all credit cards that are not archived
     * @return A list with all credit cards that are not archived
     */
    List<CreditCard> findAllByIsArchivedFalse();

    /**
     * Get all credit cards ordered by name
     * @return A list with all credit cards ordered by name
     */
    List<CreditCard> findAllByOrderByNameAsc();

    /**
     * Get all credit cards are not archived ordered by name
     * @return A list with all credit cards that are not archived ordered by name
     */
    List<CreditCard> findAllByIsArchivedFalseOrderByNameAsc();
}
