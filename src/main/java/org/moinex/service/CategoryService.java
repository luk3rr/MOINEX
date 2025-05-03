/*
 * Filename: CategoryService.java
 * Created on: October  5, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import lombok.NoArgsConstructor;
import org.moinex.model.Category;
import org.moinex.repository.CategoryRepository;
import org.moinex.repository.creditcard.CreditCardDebtRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for managing the categories
 */
@Service
@NoArgsConstructor
public class CategoryService {
    private CategoryRepository categoryRepository;

    private CreditCardDebtRepository creditCardDebtRepository;

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    @Autowired
    public CategoryService(
            CategoryRepository categoryRepository,
            CreditCardDebtRepository creditCardDebtRepository) {
        this.categoryRepository = categoryRepository;
        this.creditCardDebtRepository = creditCardDebtRepository;
    }

    /**
     * Add a new category
     * @param name Category name
     * @return Category ID
     * @throws EntityExistsException If the category already exists
     * @throws IllegalArgumentException If the category name is empty
     */
    @Transactional
    public Long addCategory(String name) {
        // Remove leading and trailing whitespaces
        name = name.strip();

        if (name.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        if (categoryRepository.existsByName(name)) {
            throw new EntityExistsException(String.format("Category '%s' already exists", name));
        }

        Category category = Category.builder().name(name).build();

        categoryRepository.save(category);

        logger.info("Category '{}' added successfully", name);

        return category.getId();
    }

    /**
     * Delete a category
     * @param id Category ID
     * @throws EntityNotFoundException If the category not found
     * @throws IllegalStateException If the category has associated transactions
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Category with ID %d not found", id)));

        if (getCountTransactions(id) > 0) {
            throw new IllegalStateException(
                    "Category "
                            + category.getName()
                            + " cannot be deleted because it has associated transactions");
        }

        categoryRepository.delete(category);

        logger.info("Category '{}' deleted successfully", category.getName());
    }

    /**
     * Rename a category
     * @param id Category ID
     * @param newName New category name
     * @throws EntityNotFoundException If the category not found
     * @throws EntityExistsException If the new name already exists
     * @throws IllegalArgumentException If the new name is empty
     */
    @Transactional
    public void renameCategory(Long id, String newName) {
        // Remove leading and trailing whitespaces
        newName = newName.strip();

        if (newName.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }

        if (categoryRepository.existsByName(newName)) {
            throw new EntityExistsException("Category with name " + newName + " already exists");
        }

        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Category with ID %d not found", id)));

        category.setName(newName);

        categoryRepository.save(category);

        logger.info("Category '{}' renamed successfully", newName);
    }

    /**
     * Archive a category
     * @param id Category ID to be archived
     * @throws EntityNotFoundException If the category not found
     */
    @Transactional
    public void archiveCategory(Long id) {
        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Category with ID "
                                                        + id
                                                        + " not found and cannot be archived"));

        category.setArchived(true);

        categoryRepository.save(category);

        logger.info("Category with id {} was archived", id);
    }

    /**
     * Unarchive a category
     * @param id Category ID to be unarchived
     * @throws EntityNotFoundException If the category not found
     */
    @Transactional
    public void unarchiveCategory(Long id) {
        Category category =
                categoryRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Category with ID "
                                                        + id
                                                        + " not found and cannot be unarchived"));

        category.setArchived(false);

        categoryRepository.save(category);

        logger.info("Category with id {} was unarchived", id);
    }

    /**
     * Get all categories
     * @return List of categories
     */
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Get all non-archived categories ordered by name
     * @return List of categories
     */
    public List<Category> getNonArchivedCategoriesOrderedByName() {
        return categoryRepository.findAllByIsArchivedFalseOrderByNameAsc();
    }

    /**
     * Get the number of transactions associated with a category
     * @param categoryId Category ID
     * @return Number of transactions
     */
    public Long getCountTransactions(Long categoryId) {
        return categoryRepository.getCountTransactions(categoryId)
                + creditCardDebtRepository.getCountTransactions(categoryId);
    }
}
