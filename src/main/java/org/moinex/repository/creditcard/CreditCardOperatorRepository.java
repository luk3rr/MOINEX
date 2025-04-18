/*
 * Filename: CreditCardOperatorRepository.java
 * Created on: September 17, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.creditcard;

import java.util.List;
import org.moinex.model.creditcard.CreditCardOperator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditCardOperatorRepository
    extends JpaRepository<CreditCardOperator, Long> {

    /**
     * Get all credit card operators ordered by name
     * @return List of credit card operators
     */
    List<CreditCardOperator> findAllByOrderByNameAsc();
}
