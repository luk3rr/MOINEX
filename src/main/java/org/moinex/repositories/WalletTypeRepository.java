/*
 * Filename: WalletTypeRepository.java
 * Created on: September 29, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repositories;

import org.moinex.entities.WalletType;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletTypeRepository extends JpaRepository<WalletType, Long> {

    /**
     * Find all wallet types ordered by name ascending
     * @return List of wallet types
     */
    List<WalletType> findAllByOrderByNameAsc();

    /**
     * Find a wallet type by its name
     * @param name The name of the wallet type
     * @return The wallet type
     */
    Optional<WalletType> findByName(String name);

    /**
     * Check if a wallet type with the given name exists
     * @param name The name of the wallet type
     * @return True if the wallet type exists, false otherwise
     */
    boolean existsByName(String name);
}
