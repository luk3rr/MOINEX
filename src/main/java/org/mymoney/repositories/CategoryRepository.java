/*
 * Filename: CategoryRepository.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.mymoney.repositories;

import java.util.List;

import org.mymoney.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Get all non archived categories
     * @return List of categories
     */
     List<Category> findAllByArchivedFalse();

    /**
     * Check if a category with a given name exists
     * @param name Category name
     * @return True if the category exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Get a category by name
     * @param name Category name
     * @return Category
     */
    Category findByName(String name);

    /**
     * Get the number of associated transactions for a category
     * @param categoryId Category ID
     * @return Number of transactions
     */
    @Query("SELECT COUNT(t) FROM WalletTransaction t WHERE t.category.id = :categoryId")
    Long CountTransactions(@Param("categoryId") Long categoryId);
}
