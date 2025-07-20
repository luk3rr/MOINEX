package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.model.Category;
import org.moinex.repository.CategoryRepository;
import org.moinex.repository.creditcard.CreditCardDebtRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;

    @Mock private CreditCardDebtRepository creditCardDebtRepository;

    @InjectMocks private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(1).name("Food").isArchived(false).build();
    }

    @Test
    @DisplayName("Should add category successfully when name is valid")
    void addCategory_Success() {
        String categoryName = "  Groceries  ";
        when(categoryRepository.existsByName(categoryName.strip())).thenReturn(false);
        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);

        categoryService.addCategory(categoryName);

        verify(categoryRepository).save(categoryCaptor.capture());
        assertEquals(categoryName.strip(), categoryCaptor.getValue().getName());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when adding a category with a blank name")
    void addCategory_BlankName_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> categoryService.addCategory("   "));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityExistsException when adding a category that already exists")
    void addCategory_AlreadyExists_ThrowsException() {
        when(categoryRepository.existsByName(category.getName())).thenReturn(true);

        assertThrows(
                EntityExistsException.class, () -> categoryService.addCategory(category.getName()));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete category successfully when it has no transactions")
    void deleteCategory_Success() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.getCountTransactions(category.getId())).thenReturn(0);
        when(creditCardDebtRepository.getCountTransactions(category.getId())).thenReturn(0);

        categoryService.deleteCategory(category.getId());

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when deleting a non-existent category")
    void deleteCategory_NotFound_ThrowsException() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.deleteCategory(category.getId()));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName(
            "Should throw IllegalStateException when deleting a category with associated"
                    + " transactions")
    void deleteCategory_WithTransactions_ThrowsException() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.getCountTransactions(category.getId())).thenReturn(1);
        when(creditCardDebtRepository.getCountTransactions(category.getId())).thenReturn(0);

        assertThrows(
                IllegalStateException.class,
                () -> categoryService.deleteCategory(category.getId()));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should rename category successfully")
    void renameCategory_Success() {
        String newName = "Groceries";
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryRepository.existsByName(newName)).thenReturn(false);

        categoryService.renameCategory(category.getId(), newName);

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertEquals(newName, categoryCaptor.getValue().getName());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when renaming with a blank name")
    void renameCategory_BlankName_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.renameCategory(category.getId(), "  "));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityExistsException when renaming to an existing name")
    void renameCategory_NewNameExists_ThrowsException() {
        String existingName = "Health";
        when(categoryRepository.existsByName(existingName)).thenReturn(true);

        assertThrows(
                EntityExistsException.class,
                () -> categoryService.renameCategory(category.getId(), existingName));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when renaming a non-existent category")
    void renameCategory_NotFound_ThrowsException() {
        String newName = "Groceries";
        when(categoryRepository.existsByName(newName)).thenReturn(false);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.renameCategory(category.getId(), newName));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should archive category successfully")
    void archiveCategory_Success() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        categoryService.archiveCategory(category.getId());

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertTrue(categoryCaptor.getValue().isArchived());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when archiving a non-existent category")
    void archiveCategory_NotFound_ThrowsException() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.archiveCategory(category.getId()));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should unarchive category successfully")
    void unarchiveCategory_Success() {
        category.setArchived(true);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        categoryService.unarchiveCategory(category.getId());

        ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(categoryCaptor.capture());
        assertFalse(categoryCaptor.getValue().isArchived());
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when unarchiving a non-existent category")
    void unarchiveCategory_NotFound_ThrowsException() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> categoryService.unarchiveCategory(category.getId()));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get all categories")
    void getCategories_Success() {
        when(categoryRepository.findAll()).thenReturn(Collections.singletonList(category));

        List<Category> categories = categoryService.getCategories();

        assertNotNull(categories);
        assertEquals(1, categories.size());
        verify(categoryRepository).findAll();
    }

    @Test
    @DisplayName("Should get non-archived categories ordered by name")
    void getNonArchivedCategoriesOrderedByName_Success() {
        when(categoryRepository.findAllByIsArchivedFalseOrderByNameAsc())
                .thenReturn(Collections.singletonList(category));

        List<Category> categories = categoryService.getNonArchivedCategoriesOrderedByName();

        assertNotNull(categories);
        assertEquals(1, categories.size());
        verify(categoryRepository).findAllByIsArchivedFalseOrderByNameAsc();
    }

    @Test
    @DisplayName("Should correctly sum transaction counts from both repositories")
    void getCountTransactions_Success() {
        when(categoryRepository.getCountTransactions(category.getId())).thenReturn(5);
        when(creditCardDebtRepository.getCountTransactions(category.getId())).thenReturn(3);

        Integer totalCount = categoryService.getCountTransactions(category.getId());

        assertEquals(8, totalCount);
    }
}
