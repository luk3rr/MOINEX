/*
 * Filename: CategoryService.java
 * Created on: October  5, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.util.List;
import java.util.logging.Logger;
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
public class CategoryService
{
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CreditCardDebtRepository creditCardDebtRepository;

    private static final Logger m_logger = LoggerConfig.GetLogger();

    public CategoryService() { }

    /**
     * Add a new category
     * @param name Category name
     * @return Category ID
     */
    @Transactional
    public Long AddCategory(String name)
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

        Category category = new Category(name);

        categoryRepository.save(category);

        m_logger.info("Category " + name + " added successfully");

        return category.GetId();
    }

    /**
     * Delete a category
     * @param id Category ID
     */
    @Transactional
    public void DeleteCategory(Long id)
    {
        Category category = categoryRepository.findById(id).orElseThrow(
            () -> new RuntimeException("Category with ID " + id + " not found"));

        if (CountTransactions(id) > 0)
        {
            throw new RuntimeException(
                "Category " + category.GetName() +
                " cannot be deleted because it has associated transactions");
        }

        categoryRepository.delete(category);

        m_logger.info("Category " + category.GetName() + " deleted successfully");
    }

    /**
     * Rename a category
     * @param id Category ID
     * @param newName New category name
     */
    @Transactional
    public void RenameCategory(Long id, String newName)
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

        category.SetName(newName);

        categoryRepository.save(category);

        m_logger.info("Category " + newName + " renamed successfully");
    }

    /**
     * Archive a category
     * @param id Category ID to be archived
     */
    @Transactional
    public void ArchiveCategory(Long id)
    {
        Category category = categoryRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Category with ID " + id +
                                        " not found and cannot be archived"));

        category.SetArchived(true);

        categoryRepository.save(category);

        m_logger.info("Category with id " + id + " was archived");
    }

    /**
     * Unarchive a category
     * @param id Category ID to be unarchived
     */
    @Transactional
    public void UnarchiveCategory(Long id)
    {
        Category category = categoryRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Category with ID " + id +
                                        " not found and cannot be unarchived"));

        category.SetArchived(false);

        categoryRepository.save(category);

        m_logger.info("Category with id " + id + " was unarchived");
    }

    /**
     * Get a category by its name
     * @param name Category name
     * @return Category
     */
    public Category GetCategoryByName(String name)
    {
        return categoryRepository.findByName(name);
    }

    /**
     * Get all categories
     * @return List of categories
     */
    public List<Category> GetCategories()
    {
        return categoryRepository.findAll();
    }

    /**
     * Get all non-archived categories ordered by name
     * @return List of categories
     */
    public List<Category> GetNonArchivedCategoriesOrderedByName()
    {
        return categoryRepository.findAllByArchivedFalseOrderByNameAsc();
    }

    /**
     * Get the number of transactions associated with a category
     * @param categoryId Category ID
     * @return Number of transactions
     */
    public Long CountTransactions(Long categoryId)
    {
        return categoryRepository.CountTransactions(categoryId) +
            creditCardDebtRepository.CountTransactions(categoryId);
    }
}
