/*
 * Filename: CategoryService.java
 * Created on: October  5, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.util.List;
import java.util.logging.Logger;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.repositories.CategoryRepository;
import org.moinex.repositories.CreditCardDebtRepository;
import org.moinex.util.LoggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing the categories
 */
@Service
@NoArgsConstructor
public class CategoryService
{
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CreditCardDebtRepository creditCardDebtRepository;

    private static final Logger logger = LoggerConfig.getLogger();

    /**
     * Add a new category
     * @param name Category name
     * @return Category ID
     * @throws RuntimeException If the category name is empty or already exists
     */
    @Transactional
    public Long addCategory(String name)
    {
        // Remove leading and trailing whitespaces
        name = name.strip();

        if (name.isBlank())
        {
            throw new RuntimeException("Category name cannot be empty");
        }

        if (categoryRepository.existsByName(name))
        {
            throw new RuntimeException("Category with name " + name +
                                       " already exists");
        }

        Category category = Category.builder().name(name).build();

        categoryRepository.save(category);

        logger.info("Category " + name + " added successfully");

        return category.getId();
    }

    /**
     * Delete a category
     * @param id Category ID
     * @throws RuntimeException If the category not found or has associated transactions
     */
    @Transactional
    public void deleteCategory(Long id)
    {
        Category category = categoryRepository.findById(id).orElseThrow(
            () -> new RuntimeException("Category with ID " + id + " not found"));

        if (getCountTransactions(id) > 0)
        {
            throw new RuntimeException(
                "Category " + category.getName() +
                " cannot be deleted because it has associated transactions");
        }

        categoryRepository.delete(category);

        logger.info("Category " + category.getName() + " deleted successfully");
    }

    /**
     * Rename a category
     * @param id Category ID
     * @param newName New category name
     * @throws RuntimeException If the category not found, the new name is empty or
     *     already exists
     */
    @Transactional
    public void renameCategory(Long id, String newName)
    {
        // Remove leading and trailing whitespaces
        newName = newName.strip();

        if (newName.isBlank())
        {
            throw new RuntimeException("Category name cannot be empty");
        }

        if (categoryRepository.existsByName(newName))
        {
            throw new RuntimeException("Category with name " + newName +
                                       " already exists");
        }

        Category category = categoryRepository.findById(id).orElseThrow(
            () -> new RuntimeException("Category with ID " + id + " not found"));

        category.setName(newName);

        categoryRepository.save(category);

        logger.info("Category " + newName + " renamed successfully");
    }

    /**
     * Archive a category
     * @param id Category ID to be archived
     * @throws RuntimeException If the category not found
     */
    @Transactional
    public void archiveCategory(Long id)
    {
        Category category = categoryRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Category with ID " + id +
                                        " not found and cannot be archived"));

        category.setArchived(true);

        categoryRepository.save(category);

        logger.info("Category with id " + id + " was archived");
    }

    /**
     * Unarchive a category
     * @param id Category ID to be unarchived
     * @throws RuntimeException If the category not found
     */
    @Transactional
    public void unarchiveCategory(Long id)
    {
        Category category = categoryRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Category with ID " + id +
                                        " not found and cannot be unarchived"));

        category.setArchived(false);

        categoryRepository.save(category);

        logger.info("Category with id " + id + " was unarchived");
    }

    /**
     * Get a category by its name
     * @param name Category name
     * @return Category
     */
    public Category getCategoryByName(String name)
    {
        return categoryRepository.findByName(name);
    }

    /**
     * Get all categories
     * @return List of categories
     */
    public List<Category> getCategories()
    {
        return categoryRepository.findAll();
    }

    /**
     * Get all non-archived categories ordered by name
     * @return List of categories
     */
    public List<Category> getNonArchivedCategoriesOrderedByName()
    {
        return categoryRepository.findAllByIsArchivedFalseOrderByNameAsc();
    }

    /**
     * Get the number of transactions associated with a category
     * @param categoryId Category ID
     * @return Number of transactions
     */
    public Long getCountTransactions(Long categoryId)
    {
        return categoryRepository.getCountTransactions(categoryId) +
            creditCardDebtRepository.getCountTransactions(categoryId);
    }
}
