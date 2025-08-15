package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.model.Category;
import org.moinex.model.financialplanning.BudgetGroup;
import org.moinex.model.financialplanning.FinancialPlan;
import org.moinex.repository.CategoryRepository;
import org.moinex.repository.financialplanning.BudgetGroupRepository;
import org.moinex.repository.financialplanning.FinancialPlanRepository;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.util.enums.TransactionType;

@ExtendWith(MockitoExtension.class)
class FinancialPlanningServiceTest {

    @Mock private FinancialPlanRepository financialPlanRepository;
    @Mock private BudgetGroupRepository budgetGroupRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private TransferRepository transferRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private FinancialPlanningService financialPlanningService;

    private Category category1;
    private Category category2;
    private Category category3;
    private BudgetGroup budgetGroup1;
    private BudgetGroup budgetGroup2;
    private FinancialPlan financialPlan;

    @BeforeEach
    void setUp() {
        category1 = Category.builder().id(1).name("Rent").isArchived(false).build();
        category2 = Category.builder().id(2).name("Groceries").isArchived(false).build();
        category3 = Category.builder().id(3).name("Utilities").isArchived(false).build();

        budgetGroup1 =
                BudgetGroup.builder()
                        .name("Essentials")
                        .targetPercentage(new BigDecimal("50.00"))
                        .categories(Set.of(category1, category2))
                        .build();

        budgetGroup2 =
                BudgetGroup.builder()
                        .name("Savings")
                        .targetPercentage(new BigDecimal("50.00"))
                        .categories(Set.of(category3))
                        .build();

        financialPlan =
                FinancialPlan.builder()
                        .id(1)
                        .name("Monthly Budget 2025")
                        .baseIncome(new BigDecimal("3000.00"))
                        .budgetGroups(List.of(budgetGroup1, budgetGroup2))
                        .build();

        budgetGroup1.setPlan(financialPlan);
    }

    @Nested
    @DisplayName("Create Plan Tests")
    class CreatePlanTests {

        @Test
        @DisplayName("Should create a financial plan successfully")
        void createPlan_Success() {
            when(financialPlanRepository.existsByName(anyString())).thenReturn(false);
            when(categoryRepository.existsById(anyInt())).thenReturn(true);
            when(financialPlanRepository.save(any(FinancialPlan.class))).thenReturn(financialPlan);

            Integer planId =
                    financialPlanningService.createPlan(
                            financialPlan.getName(),
                            financialPlan.getBaseIncome(),
                            financialPlan.getBudgetGroups());

            assertNotNull(planId);
            ArgumentCaptor<FinancialPlan> planCaptor = ArgumentCaptor.forClass(FinancialPlan.class);
            verify(financialPlanRepository).save(planCaptor.capture());

            FinancialPlan savedPlan = planCaptor.getValue();
            assertEquals("Monthly Budget 2025", savedPlan.getName());
            assertEquals(2, savedPlan.getBudgetGroups().size());
            assertEquals(
                    financialPlan.getName(),
                    savedPlan.getBudgetGroups().getFirst().getPlan().getName());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for an empty plan name")
        void createPlan_EmptyName_ThrowsException() {
            String emptyName = "   ";

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    emptyName,
                                    financialPlan.getBaseIncome(),
                                    financialPlan.getBudgetGroups()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for a plan with an existing name")
        void createPlan_NameExists_ThrowsException() {
            when(financialPlanRepository.existsByName(financialPlan.getName())).thenReturn(true);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    financialPlan.getBaseIncome(),
                                    financialPlan.getBudgetGroups()));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException for a plan with zero or negative base"
                        + " income")
        void createPlan_ZeroOrNegativeIncome_ThrowsException() {
            BigDecimal zeroIncome = BigDecimal.ZERO;
            BigDecimal negativeIncome = new BigDecimal("-1000.00");

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    zeroIncome,
                                    financialPlan.getBudgetGroups()));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    negativeIncome,
                                    financialPlan.getBudgetGroups()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for a plan with no budget groups")
        void createPlan_NoBudgetGroups_ThrowsException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    financialPlan.getBaseIncome(),
                                    Collections.emptyList()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for a budget group with an empty name")
        void createPlan_EmptyGroupName_ThrowsException() {
            budgetGroup1.setName("   ");

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    financialPlan.getBaseIncome(),
                                    List.of(budgetGroup1)));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException for a budget group with an invalid"
                        + " percentage")
        void createPlan_InvalidPercentage_ThrowsException() {
            budgetGroup1.setTargetPercentage(new BigDecimal("101.00"));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    financialPlan.getBaseIncome(),
                                    List.of(budgetGroup1)));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for a budget group with no categories")
        void createPlan_NoCategories_ThrowsException() {
            budgetGroup1.setCategories(Collections.emptySet());

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    financialPlan.getBaseIncome(),
                                    List.of(budgetGroup1)));
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException for a non-existent saved category in a group")
        void createPlan_CategoryNotFound_ThrowsException() {
            when(categoryRepository.existsById(anyInt())).thenReturn(false);

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    financialPlan.getBaseIncome(),
                                    List.of(budgetGroup1)));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException for a budget group with any archived"
                        + " categories")
        void createPlan_ArchivedCategory_ThrowsException() {
            Category archivedCategory =
                    Category.builder().id(3).name("Archived Category").isArchived(true).build();
            budgetGroup1.setCategories(Set.of(category1, archivedCategory));

            when(categoryRepository.existsById(anyInt())).thenReturn(true);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    financialPlan.getBaseIncome(),
                                    List.of(budgetGroup1)));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException if total budget percentage exceeds 100%")
        void createPlan_TotalPercentageExceeds100_ThrowsException() {
            BudgetGroup invalidGroup =
                    BudgetGroup.builder()
                            .name("Invalid Group")
                            .targetPercentage(new BigDecimal("60.00"))
                            .categories(Set.of(category1))
                            .build();

            List<BudgetGroup> groups = List.of(budgetGroup1, invalidGroup);

            when(categoryRepository.existsById(anyInt())).thenReturn(true);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    financialPlan.getBaseIncome(),
                                    groups));
        }

        @Test
        @DisplayName(
                "Should throw IllegalArgumentException if total budget percentage is less than"
                        + " 100%")
        void createPlan_TotalPercentageLessThan100_ThrowsException() {
            BudgetGroup invalidGroup =
                    BudgetGroup.builder()
                            .name("Invalid Group")
                            .targetPercentage(new BigDecimal("30.00"))
                            .categories(Set.of(category1))
                            .build();

            List<BudgetGroup> groups = List.of(budgetGroup1, invalidGroup);

            when(categoryRepository.existsById(anyInt())).thenReturn(true);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            financialPlanningService.createPlan(
                                    financialPlan.getName(),
                                    financialPlan.getBaseIncome(),
                                    groups));
        }

        @Test
        @DisplayName("Should archive old plan when creating a new one")
        void createPlan_ArchivesOldPlan() {
            FinancialPlan oldPlan =
                    FinancialPlan.builder()
                            .id(1)
                            .name("Old Plan")
                            .baseIncome(new BigDecimal("2000.00"))
                            .budgetGroups(List.of(budgetGroup1))
                            .archived(false)
                            .build();

            when(financialPlanRepository.findByArchivedFalse()).thenReturn(Optional.of(oldPlan));
            when(financialPlanRepository.existsByName(anyString())).thenReturn(false);
            when(categoryRepository.existsById(anyInt())).thenReturn(true);
            when(financialPlanRepository.save(any(FinancialPlan.class))).thenReturn(financialPlan);

            financialPlanningService.createPlan(
                    financialPlan.getName(),
                    financialPlan.getBaseIncome(),
                    financialPlan.getBudgetGroups());

            verify(financialPlanRepository).save(argThat(plan -> plan.isArchived()));
            verify(financialPlanRepository)
                    .save(argThat(plan -> plan.getName().equals(financialPlan.getName())));
            verify(financialPlanRepository, times(1)).findByArchivedFalse();
            verify(financialPlanRepository, times(1)).existsByName(financialPlan.getName());
            verify(categoryRepository, atLeastOnce()).existsById(anyInt());
            verify(financialPlanRepository, times(2)).save(any(FinancialPlan.class));
            assertTrue(oldPlan.isArchived(), "Old plan should be archived");
            assertFalse(financialPlan.isArchived(), "New plan should not be archived");
        }
    }

    @Nested
    @DisplayName("Update Plan Tests")
    class UpdatePlanTests {
        FinancialPlan existingPlan;

        @BeforeEach
        void setup() {
            BudgetGroup existingGroupA =
                    BudgetGroup.builder()
                            .name("Essentials")
                            .targetPercentage(new BigDecimal("50.00"))
                            .categories(Set.of(category1))
                            .build();

            BudgetGroup existingGroupB =
                    BudgetGroup.builder()
                            .name("Savings")
                            .targetPercentage(new BigDecimal("50.00"))
                            .categories(Set.of(category2))
                            .build();

            existingPlan =
                    FinancialPlan.builder()
                            .id(1)
                            .name("Original Plan")
                            .baseIncome(new BigDecimal("3000.00"))
                            .budgetGroups(new ArrayList<>(List.of(existingGroupA, existingGroupB)))
                            .build();

            existingGroupA.setPlan(existingPlan);
            existingGroupB.setPlan(existingPlan);
        }

        @Test
        @DisplayName("Should update a financial plan successfully")
        void updatePlan_Success() {
            BudgetGroup budgetGroupC =
                    BudgetGroup.builder()
                            .name("Entertainment")
                            .targetPercentage(BigDecimal.valueOf(80))
                            .categories(Set.of(category1))
                            .build();

            BudgetGroup budgetGroupD =
                    BudgetGroup.builder()
                            .name("Health")
                            .targetPercentage(BigDecimal.valueOf(20))
                            .categories(Set.of(category2))
                            .build();

            FinancialPlan updatedPlan =
                    FinancialPlan.builder()
                            .id(existingPlan.getId())
                            .name(existingPlan.getName() + " Updated")
                            .baseIncome(existingPlan.getBaseIncome().add(BigDecimal.ONE))
                            .budgetGroups(List.of(budgetGroupC, budgetGroupD))
                            .build();

            when(financialPlanRepository.findById(anyInt())).thenReturn(Optional.of(existingPlan));

            when(categoryRepository.existsById(anyInt())).thenReturn(true);
            when(financialPlanRepository.existsByName(anyString())).thenReturn(false);
            when(financialPlanRepository.save(any(FinancialPlan.class))).thenReturn(updatedPlan);

            financialPlanningService.updatePlan(updatedPlan);

            ArgumentCaptor<FinancialPlan> planCaptor = ArgumentCaptor.forClass(FinancialPlan.class);
            verify(financialPlanRepository).save(planCaptor.capture());
            FinancialPlan savedPlan = planCaptor.getValue();
            assertEquals(updatedPlan.getName(), savedPlan.getName());
            assertEquals(updatedPlan.getBudgetGroups().size(), savedPlan.getBudgetGroups().size());
            assertEquals(updatedPlan.getBudgetGroups().size(), savedPlan.getBudgetGroups().size());
            assertEquals(
                    updatedPlan.getName(),
                    savedPlan.getBudgetGroups().getFirst().getPlan().getName());
            assertEquals(
                    updatedPlan.getBudgetGroups().getFirst().getName(),
                    savedPlan.getBudgetGroups().getFirst().getName());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when updating a non-existent plan")
        void updatePlan_NotFound_ThrowsException() {
            when(financialPlanRepository.findById(999)).thenReturn(Optional.empty());
            FinancialPlan nonExistentPlan = FinancialPlan.builder().id(999).build();

            assertThrows(
                    EntityNotFoundException.class,
                    () -> financialPlanningService.updatePlan(nonExistentPlan));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for a blank name")
        void updatePlan_BlankName_ThrowsException() {
            FinancialPlan updatedPlan =
                    FinancialPlan.builder()
                            .id(existingPlan.getId())
                            .name("    ")
                            .baseIncome(existingPlan.getBaseIncome())
                            .budgetGroups(existingPlan.getBudgetGroups())
                            .build();

            when(financialPlanRepository.findById(existingPlan.getId()))
                    .thenReturn(Optional.of(financialPlan));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> financialPlanningService.updatePlan(updatedPlan));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for a zero base income")
        void updatePlan_ZeroIncome_ThrowsException() {
            existingPlan.setBaseIncome(BigDecimal.ZERO);
            when(financialPlanRepository.findById(existingPlan.getId()))
                    .thenReturn(Optional.of(new FinancialPlan()));

            assertThrows(
                    IllegalArgumentException.class,
                    () -> financialPlanningService.updatePlan(existingPlan));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when renaming to an existing plan name")
        void updatePlan_DuplicateName_ThrowsException() {
            String existingName = "Another Plan";

            FinancialPlan updatedPlan =
                    FinancialPlan.builder()
                            .id(existingPlan.getId())
                            .name(existingName)
                            .baseIncome(existingPlan.getBaseIncome())
                            .budgetGroups(existingPlan.getBudgetGroups())
                            .build();

            when(financialPlanRepository.findById(existingPlan.getId()))
                    .thenReturn(Optional.of(existingPlan));
            when(financialPlanRepository.existsByName(existingName)).thenReturn(true);

            assertThrows(
                    IllegalStateException.class,
                    () -> financialPlanningService.updatePlan(updatedPlan));
        }
    }

    @Nested
    @DisplayName("Get Plan Status Tests")
    class GetPlanStatusTests {
        @Test
        @DisplayName("Should calculate plan status correctly")
        void getPlanStatus_Success() {
            YearMonth period = YearMonth.of(2025, 7);
            BigDecimal expectedSpentAmount = new BigDecimal("1250.50");

            when(financialPlanRepository.findById(financialPlan.getId()))
                    .thenReturn(Optional.of(financialPlan));
            when(walletTransactionRepository.getSumAmountByCategoriesAndDateBetween(
                            anyList(), any(TransactionType.class), anyString(), anyString()))
                    .thenReturn(expectedSpentAmount);
            when(transferRepository.getSumAmountByCategoriesAndDateBetween(
                            anyList(), anyString(), anyString()))
                    .thenReturn(BigDecimal.ZERO);

            List<FinancialPlanningService.PlanStatusDTO> statusList =
                    financialPlanningService.getPlanStatus(financialPlan.getId(), period);

            assertNotNull(statusList);
            assertEquals(2, statusList.size());

            FinancialPlanningService.PlanStatusDTO status = statusList.get(0);
            assertEquals(budgetGroup1, status.group());
            assertEquals(0, expectedSpentAmount.compareTo(status.spentAmount()));
        }

        @Test
        @DisplayName("Should return zero spent amount for a group with no categories")
        void getPlanStatus_GroupWithNoCategories() {
            budgetGroup1.setCategories(Collections.emptySet());
            financialPlan.setBudgetGroups(List.of(budgetGroup1));

            YearMonth period = YearMonth.of(2025, 7);

            when(financialPlanRepository.findById(financialPlan.getId()))
                    .thenReturn(Optional.of(financialPlan));

            List<FinancialPlanningService.PlanStatusDTO> statusList =
                    financialPlanningService.getPlanStatus(financialPlan.getId(), period);

            assertEquals(1, statusList.size());
            assertEquals(0, BigDecimal.ZERO.compareTo(statusList.getFirst().spentAmount()));
            verify(walletTransactionRepository, never())
                    .getSumAmountByCategoriesAndDateBetween(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when plan is not found")
        void getPlanStatus_PlanNotFound_ThrowsException() {
            when(financialPlanRepository.findById(anyInt())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> financialPlanningService.getPlanStatus(999, YearMonth.now()));
        }
    }
}
