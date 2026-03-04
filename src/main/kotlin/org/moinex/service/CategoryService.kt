/*
 * Filename: CategoryService.kt (original filename: CategoryService.java)
 * Created on: October 5, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.service

import org.moinex.common.findByIdOrThrow
import org.moinex.model.Category
import org.moinex.repository.CategoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val creditCardService: CreditCardService,
    private val walletService: WalletService,
) {
    private val logger = LoggerFactory.getLogger(CategoryService::class.java)

    @Transactional
    fun createCategory(category: Category): Int {
        check(!categoryRepository.existsByName(category.name)) {
            "Category '${category.name}' already exists"
        }

        val newCategory = categoryRepository.save(category)

        logger.info("Category '{}' added successfully", newCategory)

        return newCategory.id!!
    }

    @Transactional
    fun deleteCategory(id: Int) {
        val categoryFromDatabase = categoryRepository.findByIdOrThrow(id)

        check(getTransactionCountByCategory(id) == 0) {
            "Category ${categoryFromDatabase.name} cannot be deleted because it has associated transactions"
        }

        categoryRepository.delete(categoryFromDatabase)

        logger.info("Category '{}' deleted successfully", categoryFromDatabase.name)
    }

    @Transactional
    fun renameCategory(updatedCategory: Category) {
        val categoryFromDatabase = categoryRepository.findByIdOrThrow(updatedCategory.id!!)

        if (categoryFromDatabase.name == updatedCategory.name) {
            logger.info("Category name is the same, no need to update")
            return
        }

        check(!categoryRepository.existsByName(updatedCategory.name)) {
            "Category with name '${updatedCategory.name}' already exists"
        }

        categoryFromDatabase.apply {
            name = updatedCategory.name
        }

        logger.info("Category '{}' renamed successfully", categoryFromDatabase)
    }

    @Transactional
    fun archiveCategory(id: Int) {
        val categoryFromDatabase = categoryRepository.findByIdOrThrow(id)

        if (categoryFromDatabase.isArchived) {
            logger.info("Category with id {} is already archived", id)
            return
        }

        categoryFromDatabase.apply {
            isArchived = true
        }

        logger.info("Category with id {} was archived", id)
    }

    @Transactional
    fun unarchiveCategory(id: Int) {
        val categoryFromDatabase = categoryRepository.findByIdOrThrow(id)

        if (!categoryFromDatabase.isArchived) {
            logger.info("Category with id {} is already unarchived", id)
            return
        }

        categoryFromDatabase.apply {
            isArchived = false
        }

        logger.info("Category with id {} was unarchived", id)
    }

    fun getCategories(): List<Category> = categoryRepository.findAll()

    fun getNonArchivedCategoriesOrderedByName(): List<Category> = categoryRepository.findAllByIsArchivedFalseOrderByNameAsc()

    fun getTransactionCountByCategory(categoryId: Int): Int =
        walletService.getWalletTransactionAndTransferCountByCategory(categoryId) +
            creditCardService.getDebtCountByCategory(categoryId)
}
