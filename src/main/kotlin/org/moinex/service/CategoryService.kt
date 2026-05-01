/*
 * Filename: CategoryService.kt (original filename: CategoryService.java)
 * Created on: October 5, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/03/2026
 */

package org.moinex.service

import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.model.Category
import org.moinex.model.enums.NotificationType
import org.moinex.repository.CategoryRepository
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.wallet.WalletService
import org.moinex.service.wishlist.WishlistService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.MessageFormat

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val creditCardService: CreditCardService,
    private val walletService: WalletService,
    private val wishlistService: WishlistService,
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
) {
    private val logger = LoggerFactory.getLogger(CategoryService::class.java)

    @Transactional
    fun createCategory(category: Category): Int {
        check(!categoryRepository.existsByName(category.name)) {
            "Category '${category.name}' already exists"
        }

        val newCategory = categoryRepository.save(category)

        logger.info("$newCategory added successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_CATEGORY_ADDED_TITLE),
            message =
                MessageFormat.format(
                    preferencesService
                        .translate(TranslationKeys.CATEGORY_DIALOG_CATEGORY_ADDED_MESSAGE),
                    newCategory.name,
                ),
            relatedEntityId = newCategory.id!!,
        )

        return newCategory.id!!
    }

    @Transactional
    fun deleteCategory(id: Int) {
        val categoryFromDatabase = categoryRepository.findByIdOrThrow(id)

        check(getTransactionCountByCategory(id) == 0) {
            "Category ${categoryFromDatabase.name} cannot be deleted because it has associated transactions, debts, or wishlist items"
        }

        categoryRepository.delete(categoryFromDatabase)

        logger.info("$categoryFromDatabase deleted successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_DELETE_CATEGORY_TITLE),
            message =
                MessageFormat.format(
                    preferencesService
                        .translate(TranslationKeys.CATEGORY_DIALOG_DELETE_CATEGORY_MESSAGE),
                    categoryFromDatabase.name,
                ),
            relatedEntityId = categoryFromDatabase.id!!,
        )
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

        logger.info("$categoryFromDatabase renamed successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_CATEGORY_UPDATED_TITLE),
            message =
                preferencesService.translate(
                    TranslationKeys.CATEGORY_DIALOG_CATEGORY_NAME_UPDATED_MESSAGE,
                ),
            relatedEntityId = categoryFromDatabase.id!!,
        )
    }

    @Transactional
    fun archiveCategory(id: Int) {
        val categoryFromDatabase = categoryRepository.findByIdOrThrow(id)

        if (categoryFromDatabase.isArchived) {
            logger.info("$categoryFromDatabase is already archived")
            return
        }

        categoryFromDatabase.apply {
            isArchived = true
        }

        logger.info("$categoryFromDatabase was archived")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_CATEGORY_UPDATED_TITLE),
            message =
                preferencesService.translate(
                    TranslationKeys.CATEGORY_DIALOG_CATEGORY_ARCHIVED_UPDATED_MESSAGE,
                ),
            relatedEntityId = categoryFromDatabase.id!!,
        )
    }

    @Transactional
    fun unarchiveCategory(id: Int) {
        val categoryFromDatabase = categoryRepository.findByIdOrThrow(id)

        if (!categoryFromDatabase.isArchived) {
            logger.info("$categoryFromDatabase is already unarchived")
            return
        }

        categoryFromDatabase.apply {
            isArchived = false
        }

        logger.info("$categoryFromDatabase was unarchived")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.CATEGORY_DIALOG_CATEGORY_UPDATED_TITLE),
            message =
                preferencesService.translate(
                    TranslationKeys.CATEGORY_DIALOG_CATEGORY_UNARCHIVED_UPDATED_MESSAGE,
                ),
            relatedEntityId = categoryFromDatabase.id!!,
        )
    }

    fun existsById(id: Int): Boolean = categoryRepository.existsById(id)

    fun getCategories(): List<Category> = categoryRepository.findAll()

    fun getNonArchivedCategoriesOrderedByName(): List<Category> =
        categoryRepository.findAllByIsArchivedFalseOrderByNameAsc()

    fun getTransactionCountByCategory(categoryId: Int): Int =
        walletService.getWalletTransactionAndTransferCountByCategory(categoryId) +
            creditCardService.getDebtCountByCategory(categoryId) +
            wishlistService.getItemCountByCategory(categoryId)
}
