/*
 * Filename: CategoryRepository.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package com.mymoney.repositories;

import java.util.List;

import com.mymoney.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    /**
     * Check if a category with a given name exists
     * @param name Category name
     * @return True if the category exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Get all categories
     * @return List of categories
     */
    List<Category> findAll();
}
