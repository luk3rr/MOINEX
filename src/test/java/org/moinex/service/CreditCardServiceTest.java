package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.creditcard.*;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.repository.CategoryRepository;
import org.moinex.repository.creditcard.*;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.util.Constants;
import org.moinex.util.enums.CreditCardCreditType;
import org.moinex.util.enums.CreditCardInvoiceStatus;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {

    @Mock private CreditCardRepository creditCardRepository;
    @Mock private CreditCardDebtRepository creditCardDebtRepository;
    @Mock private CreditCardPaymentRepository creditCardPaymentRepository;
    @Mock private CreditCardOperatorRepository creditCardOperatorRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private CreditCardCreditRepository creditCardCreditRepository;

    @InjectMocks private CreditCardService creditCardService;

    private CreditCard creditCard;
    private CreditCardOperator operator;
    private Category category;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        operator = new CreditCardOperator(1, "Test Operator");
        category = Category.builder().id(1).name("Food").build();
        wallet = new Wallet(1, "Test Wallet", new BigDecimal("500.00"));

        creditCard =
                CreditCard.builder()
                        .id(1)
                        .name("Test Card")
                        .billingDueDay(10)
                        .closingDay(1)
                        .maxDebt(new BigDecimal("2000.00"))
                        .lastFourDigits("1234")
                        .operator(operator)
                        .defaultBillingWallet(wallet)
                        .availableRebate(BigDecimal.ZERO)
                        .build();
    }

    @Nested
    @DisplayName("Credit Card Creation (addCreditCard)")
    class AddCreditCardTests {
        @Test
        @DisplayName("Should add credit card successfully")
        void addCreditCard_Success() {
            when(creditCardRepository.existsByName(anyString())).thenReturn(false);
            when(creditCardOperatorRepository.findById(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
            when(creditCardRepository.save(any(CreditCard.class))).thenReturn(creditCard);

            Integer newCardId =
                    creditCardService.addCreditCard(
                            creditCard.getName(),
                            creditCard.getBillingDueDay(),
                            creditCard.getClosingDay(),
                            creditCard.getMaxDebt(),
                            creditCard.getLastFourDigits(),
                            operator.getId(),
                            wallet.getId());

            assertNotNull(newCardId);
            verify(creditCardRepository).save(any(CreditCard.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if card name is blank")
        void addCreditCard_BlankName_ThrowsException() {
            when(creditCardRepository.existsByName(anyString())).thenReturn(false);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    "  ",
                                    creditCard.getBillingDueDay(),
                                    creditCard.getClosingDay(),
                                    creditCard.getMaxDebt(),
                                    creditCard.getLastFourDigits(),
                                    operator.getId()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if billing due day is out of range")
        void addCreditCard_BillingDueDayOutOfRange_ThrowsException() {
            when(creditCardRepository.existsByName(anyString())).thenReturn(false);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    creditCard.getName(),
                                    32,
                                    creditCard.getClosingDay(),
                                    creditCard.getMaxDebt(),
                                    creditCard.getLastFourDigits(),
                                    operator.getId()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if closing day is out of range")
        void addCreditCard_ClosingDayOutOfRange_ThrowsException() {
            when(creditCardRepository.existsByName(anyString())).thenReturn(false);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    creditCard.getName(),
                                    creditCard.getBillingDueDay(),
                                    32,
                                    creditCard.getMaxDebt(),
                                    creditCard.getLastFourDigits(),
                                    operator.getId()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if max debt is negative")
        void addCreditCard_MaxDebtNegative_ThrowsException() {
            when(creditCardRepository.existsByName(anyString())).thenReturn(false);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    creditCard.getName(),
                                    creditCard.getBillingDueDay(),
                                    creditCard.getClosingDay(),
                                    new BigDecimal("-1000.00"),
                                    creditCard.getLastFourDigits(),
                                    operator.getId()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if last four digits are not numeric")
        void addCreditCard_LastFourDigitsNotNumeric_ThrowsException() {
            when(creditCardRepository.existsByName(anyString())).thenReturn(false);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    creditCard.getName(),
                                    creditCard.getBillingDueDay(),
                                    creditCard.getClosingDay(),
                                    creditCard.getMaxDebt(),
                                    "abcd",
                                    operator.getId()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if last four digits are blank")
        void addCreditCard_LastFourDigitsBlank_ThrowsException() {
            when(creditCardRepository.existsByName(anyString())).thenReturn(false);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    creditCard.getName(),
                                    creditCard.getBillingDueDay(),
                                    creditCard.getClosingDay(),
                                    creditCard.getMaxDebt(),
                                    "   ",
                                    operator.getId()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException if last four digits are not 4 digits")
        void addCreditCard_LastFourDigitsNotFourDigits_ThrowsException() {
            when(creditCardRepository.existsByName(anyString())).thenReturn(false);

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    creditCard.getName(),
                                    creditCard.getBillingDueDay(),
                                    creditCard.getClosingDay(),
                                    creditCard.getMaxDebt(),
                                    "12345",
                                    operator.getId()));
        }

        @Test
        @DisplayName("Should throw EntityExistsException if card name already exists")
        void addCreditCard_NameExists_ThrowsException() {
            when(creditCardRepository.existsByName(creditCard.getName())).thenReturn(true);

            assertThrows(
                    EntityExistsException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    creditCard.getName(),
                                    creditCard.getBillingDueDay(),
                                    creditCard.getClosingDay(),
                                    creditCard.getMaxDebt(),
                                    creditCard.getLastFourDigits(),
                                    operator.getId()));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException if operator does not exist")
        void addCreditCard_OperatorNotFound_ThrowsException() {
            when(creditCardRepository.existsByName(anyString())).thenReturn(false);
            when(creditCardOperatorRepository.findById(anyInt())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    creditCard.getName(),
                                    creditCard.getBillingDueDay(),
                                    creditCard.getClosingDay(),
                                    creditCard.getMaxDebt(),
                                    creditCard.getLastFourDigits(),
                                    operator.getId()));
        }
    }

    @Nested
    @DisplayName("Credit Card Update (updateCreditCard)")
    class UpdateCreditCardTests {
        @Test
        @DisplayName("Should update credit card successfully")
        void updateCreditCard_Success() {
            CreditCard updatedCard =
                    CreditCard.builder()
                            .id(creditCard.getId())
                            .name("Updated Card Name")
                            .billingDueDay(15)
                            .closingDay(5)
                            .maxDebt(new BigDecimal("2500.00"))
                            .lastFourDigits("5678")
                            .operator(operator)
                            .build();

            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardOperatorRepository.findById(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(creditCardRepository.findAll()).thenReturn(Collections.singletonList(creditCard));
            when(creditCardPaymentRepository.getAllPendingCreditCardPayments(creditCard.getId()))
                    .thenReturn(Collections.emptyList());

            creditCardService.updateCreditCard(updatedCard);

            ArgumentCaptor<CreditCard> cardCaptor = ArgumentCaptor.forClass(CreditCard.class);
            verify(creditCardRepository).save(cardCaptor.capture());
            CreditCard savedCard = cardCaptor.getValue();

            assertEquals("Updated Card Name", savedCard.getName());
            assertEquals(15, savedCard.getBillingDueDay());
            assertEquals(new BigDecimal("2500.00"), savedCard.getMaxDebt());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException if credit card does not exist")
        void updateCreditCard_NotFound_ThrowsException() {
            when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> creditCardService.updateCreditCard(creditCard));
        }

        @Test
        @DisplayName(
                "Should throw IllegalStateException if new name is already in use by another card")
        void updateCreditCard_NameAlreadyExists_ThrowsException() {
            CreditCard anotherCard = CreditCard.builder().id(2).name("Another Card").build();
            creditCard.setName("Another Card");

            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(new CreditCard()));
            when(creditCardOperatorRepository.findById(anyInt())).thenReturn(Optional.of(operator));
            when(creditCardRepository.findAll()).thenReturn(List.of(creditCard, anotherCard));

            assertThrows(
                    IllegalStateException.class,
                    () -> creditCardService.updateCreditCard(creditCard));
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException if operator does not exist when updating"
                        + " credit card")
        void updateCreditCard_OperatorNotFound_ThrowsException() {
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardOperatorRepository.findById(operator.getId()))
                    .thenReturn(Optional.empty());

            creditCard.setOperator(operator);

            assertThrows(
                    EntityNotFoundException.class,
                    () -> creditCardService.updateCreditCard(creditCard));
        }

        @Test
        @DisplayName("Should update due date of future pending payments when updating credit card")
        void updateCreditCard_UpdatesFuturePendingPaymentsDueDate() {
            CreditCardPayment futurePayment =
                    CreditCardPayment.builder()
                            .id(1)
                            .date(LocalDateTime.now().plusDays(10))
                            .amount(new BigDecimal("100.00"))
                            .build();

            CreditCard updatedCard =
                    CreditCard.builder()
                            .id(creditCard.getId())
                            .name("Updated Card Name")
                            .billingDueDay(15)
                            .closingDay(5)
                            .maxDebt(new BigDecimal("2500.00"))
                            .lastFourDigits("5678")
                            .operator(operator)
                            .build();

            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardOperatorRepository.findById(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(creditCardRepository.findAll()).thenReturn(Collections.singletonList(creditCard));
            when(creditCardPaymentRepository.getAllPendingCreditCardPayments(creditCard.getId()))
                    .thenReturn(List.of(futurePayment));

            creditCardService.updateCreditCard(updatedCard);

            verify(creditCardPaymentRepository).save(any(CreditCardPayment.class));
            assertEquals(15, futurePayment.getDate().getDayOfMonth());
        }
    }

    @Nested
    @DisplayName("Debt Management (addDebt)")
    class AddDebtTests {
        @Test
        @DisplayName("Should add debt and create correct payments for multiple installments")
        void addDebt_Success_MultipleInstallments() {
            BigDecimal debtValue = new BigDecimal("300.00");
            int installments = 3;
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
            when(creditCardPaymentRepository.getTotalPendingPaymentsByYear(creditCard.getId()))
                    .thenReturn(BigDecimal.ZERO);

            creditCardService.addDebt(
                    creditCard.getId(),
                    category,
                    LocalDateTime.now(),
                    YearMonth.now(),
                    debtValue,
                    installments,
                    "Test Debt");

            verify(creditCardDebtRepository).save(any(CreditCardDebt.class));

            ArgumentCaptor<CreditCardPayment> paymentCaptor =
                    ArgumentCaptor.forClass(CreditCardPayment.class);
            verify(creditCardPaymentRepository, times(installments)).save(paymentCaptor.capture());

            List<CreditCardPayment> capturedPayments = paymentCaptor.getAllValues();
            assertEquals(installments, capturedPayments.size());
            assertEquals(new BigDecimal("100.00"), capturedPayments.get(0).getAmount());
            assertEquals(new BigDecimal("100.00"), capturedPayments.get(1).getAmount());
            assertEquals(new BigDecimal("100.00"), capturedPayments.get(2).getAmount());
        }

        @Test
        @DisplayName(
                "Should add remainder to first installment when value is not divisible by"
                        + " installments")
        void addDebt_RemainderAddedToFirstInstallment() {
            BigDecimal debtValue = new BigDecimal("100.01");
            int installments = 3;
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
            when(creditCardPaymentRepository.getTotalPendingPaymentsByYear(creditCard.getId()))
                    .thenReturn(BigDecimal.ZERO);

            creditCardService.addDebt(
                    creditCard.getId(),
                    category,
                    LocalDateTime.now(),
                    YearMonth.now(),
                    debtValue,
                    installments,
                    "Test Debt");

            ArgumentCaptor<CreditCardPayment> paymentCaptor =
                    ArgumentCaptor.forClass(CreditCardPayment.class);
            verify(creditCardPaymentRepository, times(installments)).save(paymentCaptor.capture());
            List<CreditCardPayment> payments = paymentCaptor.getAllValues();

            assertEquals(new BigDecimal("33.35"), payments.get(0).getAmount());
            assertEquals(new BigDecimal("33.33"), payments.get(1).getAmount());
            assertEquals(new BigDecimal("33.33"), payments.get(2).getAmount());
        }

        @Test
        @DisplayName("Should throw InsufficientResourcesException if debt exceeds available credit")
        void addDebt_InsufficientCredit_ThrowsException() {
            BigDecimal debtValue = new BigDecimal("1500.00");
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

            when(creditCardPaymentRepository.getTotalPendingPaymentsByYear(creditCard.getId()))
                    .thenReturn(new BigDecimal("600.00"));

            assertThrows(
                    MoinexException.InsufficientResourcesException.class,
                    () ->
                            creditCardService.addDebt(
                                    creditCard.getId(),
                                    category,
                                    LocalDateTime.now(),
                                    YearMonth.now(),
                                    debtValue,
                                    1,
                                    "Test Debt"));
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException if default billing wallet does not exist")
        void addCreditCard_DefaultBillingWalletNotFound_ThrowsException() {
            when(creditCardRepository.existsByName(creditCard.getName())).thenReturn(false);
            when(creditCardOperatorRepository.findById(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            creditCardService.addCreditCard(
                                    creditCard.getName(),
                                    creditCard.getBillingDueDay(),
                                    creditCard.getClosingDay(),
                                    creditCard.getMaxDebt(),
                                    creditCard.getLastFourDigits(),
                                    operator.getId(),
                                    wallet.getId()));
        }

        @DisplayName("Should throw EntityNotFoundException when credit card does not exist")
        @Test
        void addDebt_CreditCardNotFound_ThrowsException() {
            when(creditCardRepository.findById(anyInt())).thenReturn(Optional.empty());
            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            creditCardService.addDebt(
                                    1,
                                    category,
                                    LocalDateTime.now(),
                                    YearMonth.now(),
                                    BigDecimal.ONE,
                                    1,
                                    "desc"));
        }

        @DisplayName("Should throw EntityNotFoundException when category does not exist")
        @Test
        void addDebt_CategoryNotFound_ThrowsException() {
            when(creditCardRepository.findById(anyInt())).thenReturn(Optional.of(creditCard));
            when(categoryRepository.findById(anyInt())).thenReturn(Optional.empty());
            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            creditCardService.addDebt(
                                    1,
                                    category,
                                    LocalDateTime.now(),
                                    YearMonth.now(),
                                    BigDecimal.ONE,
                                    1,
                                    "desc"));
        }

        @DisplayName("Should throw IllegalArgumentException when value is null")
        @Test
        void addDebt_ValueNull_ThrowsException() {
            when(creditCardRepository.findById(anyInt())).thenReturn(Optional.of(creditCard));
            when(categoryRepository.findById(anyInt())).thenReturn(Optional.of(category));
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addDebt(
                                    1,
                                    category,
                                    LocalDateTime.now(),
                                    YearMonth.now(),
                                    null,
                                    1,
                                    "desc"));
        }

        @DisplayName("Should throw IllegalArgumentException when value is negative")
        @Test
        void addDebt_ValueNegative_ThrowsException() {
            when(creditCardRepository.findById(anyInt())).thenReturn(Optional.of(creditCard));
            when(categoryRepository.findById(anyInt())).thenReturn(Optional.of(category));
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addDebt(
                                    1,
                                    category,
                                    LocalDateTime.now(),
                                    YearMonth.now(),
                                    new BigDecimal("-1"),
                                    1,
                                    "desc"));
        }

        @DisplayName("Should throw IllegalArgumentException when installments are out of range")
        @Test
        void addDebt_InstallmentsOutOfRange_ThrowsException() {
            when(creditCardRepository.findById(anyInt())).thenReturn(Optional.of(creditCard));
            when(categoryRepository.findById(anyInt())).thenReturn(Optional.of(category));
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addDebt(
                                    1,
                                    category,
                                    LocalDateTime.now(),
                                    YearMonth.now(),
                                    BigDecimal.ONE,
                                    0,
                                    "desc"));
        }

        @DisplayName("Should throw IllegalArgumentException when register date is null")
        @Test
        void addDebt_RegisterDateNull_ThrowsException() {
            when(creditCardRepository.findById(anyInt())).thenReturn(Optional.of(creditCard));
            when(categoryRepository.findById(anyInt())).thenReturn(Optional.of(category));
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addDebt(
                                    1, category, null, YearMonth.now(), BigDecimal.ONE, 1, "desc"));
        }

        @DisplayName("Should throw IllegalArgumentException when invoice month is null")
        @Test
        void addDebt_InvoiceMonthNull_ThrowsException() {
            when(creditCardRepository.findById(anyInt())).thenReturn(Optional.of(creditCard));
            when(categoryRepository.findById(anyInt())).thenReturn(Optional.of(category));
            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addDebt(
                                    1,
                                    category,
                                    LocalDateTime.now(),
                                    null,
                                    BigDecimal.ONE,
                                    1,
                                    "desc"));
        }
    }

    @Nested
    @DisplayName("Debt Update (updateCreditCardDebt)")
    class UpdateCreditCardDebtTests {

        private CreditCardDebt oldDebt;
        private List<CreditCardPayment> oldPayments;

        @BeforeEach
        void setup() {
            oldDebt =
                    CreditCardDebt.builder()
                            .id(1)
                            .amount(new BigDecimal("400.00"))
                            .installments(3)
                            .creditCard(creditCard)
                            .category(category)
                            .description("Old Description")
                            .build();

            oldPayments = new ArrayList<>();
            IntStream.range(1, 4)
                    .forEach(
                            i -> {
                                CreditCardPayment payment =
                                        CreditCardPayment.builder()
                                                .id(i)
                                                .creditCardDebt(oldDebt)
                                                .amount(new BigDecimal("100.00"))
                                                .installment(i)
                                                .date(LocalDateTime.now().plusMonths(i))
                                                .build();
                                oldPayments.add(payment);
                            });
        }

        @Test
        @DisplayName(
                "Should update all debt fields successfully when the installments are decreased")
        void updateCreditCardDebt_Success_AllChanges_DecreasedInstallments() {
            CreditCardDebt newDebtData =
                    CreditCardDebt.builder()
                            .id(oldDebt.getId())
                            .amount(new BigDecimal("600.00"))
                            .installments(2)
                            .creditCard(creditCard)
                            .category(new Category(2, "New Category", false))
                            .description("New Description")
                            .build();

            YearMonth newInvoiceMonth = YearMonth.now().plusMonths(1);

            when(creditCardDebtRepository.findById(oldDebt.getId()))
                    .thenReturn(Optional.of(oldDebt));
            when(creditCardRepository.existsById(creditCard.getId())).thenReturn(true);
            when(creditCardPaymentRepository.getPaymentsByDebtId(oldDebt.getId()))
                    .thenReturn(oldPayments);

            when(creditCardPaymentRepository.findById(3))
                    .thenReturn(Optional.of(oldPayments.get(2)));

            creditCardService.updateCreditCardDebt(newDebtData, newInvoiceMonth);

            ArgumentCaptor<CreditCardDebt> debtCaptor =
                    ArgumentCaptor.forClass(CreditCardDebt.class);
            verify(creditCardDebtRepository, atLeastOnce()).save(debtCaptor.capture());
            CreditCardDebt savedDebt = debtCaptor.getValue();

            assertEquals(newDebtData.getDescription(), savedDebt.getDescription());
            assertEquals(newDebtData.getCategory(), savedDebt.getCategory());
            assertEquals(newDebtData.getAmount(), savedDebt.getAmount());
            assertEquals(newDebtData.getInstallments(), savedDebt.getInstallments());

            verify(creditCardPaymentRepository, atLeastOnce()).save(any(CreditCardPayment.class));
            verify(creditCardPaymentRepository, times(1)).delete(any(CreditCardPayment.class));
        }

        @Test
        @DisplayName(
                "Should update all debt fields successfully when the installments are increased")
        void updateCreditCardDebt_Success_AllChanges_IncreasedInstallments() {
            CreditCardDebt newDebtData =
                    CreditCardDebt.builder()
                            .id(oldDebt.getId())
                            .amount(new BigDecimal("600.00"))
                            .installments(10)
                            .creditCard(creditCard)
                            .category(new Category(2, "New Category", false))
                            .description("New Description")
                            .build();

            YearMonth newInvoiceMonth = YearMonth.now().plusMonths(1);

            when(creditCardDebtRepository.findById(oldDebt.getId()))
                    .thenReturn(Optional.of(oldDebt));
            when(creditCardRepository.existsById(creditCard.getId())).thenReturn(true);
            when(creditCardPaymentRepository.getPaymentsByDebtId(oldDebt.getId()))
                    .thenReturn(oldPayments);

            creditCardService.updateCreditCardDebt(newDebtData, newInvoiceMonth);

            ArgumentCaptor<CreditCardDebt> debtCaptor =
                    ArgumentCaptor.forClass(CreditCardDebt.class);
            verify(creditCardDebtRepository, atLeastOnce()).save(debtCaptor.capture());
            CreditCardDebt savedDebt = debtCaptor.getValue();

            assertEquals(newDebtData.getDescription(), savedDebt.getDescription());
            assertEquals(newDebtData.getCategory(), savedDebt.getCategory());
            assertEquals(newDebtData.getAmount(), savedDebt.getAmount());
            assertEquals(newDebtData.getInstallments(), savedDebt.getInstallments());

            verify(creditCardPaymentRepository, atLeastOnce()).save(any(CreditCardPayment.class));
        }

        @Test
        @DisplayName("Should update all debt fields successfully when some payment already paid")
        void updateCreditCardDebt_Success_AllChanges_WithPaidPayment() {
            CreditCardDebt newDebtData =
                    CreditCardDebt.builder()
                            .id(oldDebt.getId())
                            .amount(new BigDecimal("600.00"))
                            .installments(2)
                            .creditCard(creditCard)
                            .category(new Category(2, "New Category", false))
                            .description("New Description")
                            .build();

            YearMonth newInvoiceMonth = YearMonth.now().plusMonths(1);

            // Set the first payment as paid
            oldPayments.getFirst().setWallet(wallet);

            when(creditCardDebtRepository.findById(oldDebt.getId()))
                    .thenReturn(Optional.of(oldDebt));
            when(creditCardRepository.existsById(creditCard.getId())).thenReturn(true);
            when(creditCardPaymentRepository.getPaymentsByDebtId(oldDebt.getId()))
                    .thenReturn(oldPayments);

            when(creditCardPaymentRepository.findById(3))
                    .thenReturn(Optional.of(oldPayments.get(2)));

            creditCardService.updateCreditCardDebt(newDebtData, newInvoiceMonth);

            ArgumentCaptor<CreditCardDebt> debtCaptor =
                    ArgumentCaptor.forClass(CreditCardDebt.class);
            verify(creditCardDebtRepository, atLeastOnce()).save(debtCaptor.capture());
            CreditCardDebt savedDebt = debtCaptor.getValue();

            assertEquals(newDebtData.getDescription(), savedDebt.getDescription());
            assertEquals(newDebtData.getCategory(), savedDebt.getCategory());
            assertEquals(newDebtData.getAmount(), savedDebt.getAmount());
            assertEquals(newDebtData.getInstallments(), savedDebt.getInstallments());

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());
            Wallet updatedWallet = walletCaptor.getValue();

            assertEquals(new BigDecimal("600.00"), updatedWallet.getBalance());

            verify(creditCardPaymentRepository, atLeastOnce()).save(any(CreditCardPayment.class));
            verify(creditCardPaymentRepository, times(1)).delete(any(CreditCardPayment.class));
        }

        @Test
        @DisplayName("Should update debt successfully when the invoice month is changed")
        void updateCreditCardDebt_Success_ChangeInvoiceMonth() {
            CreditCardDebt newDebtData =
                    CreditCardDebt.builder()
                            .id(oldDebt.getId())
                            .amount(new BigDecimal("500.00"))
                            .installments(3)
                            .creditCard(creditCard)
                            .category(category)
                            .description("Updated Description")
                            .build();

            YearMonth newInvoiceMonth = YearMonth.now().plusMonths(2);

            when(creditCardDebtRepository.findById(oldDebt.getId()))
                    .thenReturn(Optional.of(oldDebt));
            when(creditCardRepository.existsById(creditCard.getId())).thenReturn(true);
            when(creditCardPaymentRepository.getPaymentsByDebtId(oldDebt.getId()))
                    .thenReturn(oldPayments);

            creditCardService.updateCreditCardDebt(newDebtData, newInvoiceMonth);

            // check if date of the first payment has changed
            ArgumentCaptor<CreditCardPayment> paymentCaptor =
                    ArgumentCaptor.forClass(CreditCardPayment.class);
            verify(creditCardPaymentRepository, atLeastOnce()).save(paymentCaptor.capture());
            List<CreditCardPayment> savedPayments = paymentCaptor.getAllValues();
            assertEquals(
                    newInvoiceMonth.getMonthValue(),
                    savedPayments.getFirst().getDate().getMonthValue());
            assertEquals(newInvoiceMonth.getYear(), savedPayments.getFirst().getDate().getYear());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when credit card does not exist")
        void updateCreditCardDebt_CreditCardNotFound_ThrowsException() {
            CreditCardDebt newDebtData =
                    CreditCardDebt.builder()
                            .id(oldDebt.getId())
                            .amount(new BigDecimal("400.00"))
                            .installments(2)
                            .creditCard(creditCard)
                            .build();

            when(creditCardDebtRepository.findById(oldDebt.getId()))
                    .thenReturn(Optional.of(oldDebt));
            when(creditCardRepository.existsById(creditCard.getId())).thenReturn(false);

            assertThrows(
                    EntityNotFoundException.class,
                    () -> creditCardService.updateCreditCardDebt(newDebtData, YearMonth.now()));
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when debt does not exist")
        void updateCreditCardDebt_DebtNotFound_ThrowsException() {
            CreditCardDebt nonExistentDebt =
                    CreditCardDebt.builder().id(999).amount(BigDecimal.TEN).build();
            when(creditCardDebtRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> creditCardService.updateCreditCardDebt(nonExistentDebt, YearMonth.now()));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for zero or negative amount")
        void updateCreditCardDebt_InvalidAmount_ThrowsException() {
            CreditCardDebt newDebtData =
                    CreditCardDebt.builder()
                            .id(oldDebt.getId())
                            .amount(BigDecimal.ZERO)
                            .installments(3)
                            .creditCard(creditCard)
                            .build();

            when(creditCardDebtRepository.findById(oldDebt.getId()))
                    .thenReturn(Optional.of(oldDebt));
            when(creditCardRepository.existsById(creditCard.getId())).thenReturn(true);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> creditCardService.updateCreditCardDebt(newDebtData, YearMonth.now()));
        }
    }

    @Nested
    @DisplayName("Debt Management (deleteDebt)")
    class DeleteDebtTests {
        @Test
        @DisplayName("Deletes debt successfully when debt exists")
        void deletesDebtSuccessfully() {
            Integer debtId = 1;
            CreditCardDebt debt =
                    CreditCardDebt.builder().id(debtId).creditCard(creditCard).build();
            CreditCardPayment payment1 =
                    CreditCardPayment.builder().id(1).creditCardDebt(debt).build();
            CreditCardPayment payment2 =
                    CreditCardPayment.builder().id(2).creditCardDebt(debt).build();
            List<CreditCardPayment> payments = List.of(payment1, payment2);

            when(creditCardDebtRepository.findById(debtId)).thenReturn(Optional.of(debt));
            when(creditCardPaymentRepository.getPaymentsByDebtId(debtId)).thenReturn(payments);
            when(creditCardPaymentRepository.findById(payment1.getId()))
                    .thenReturn(Optional.of(payment1));
            when(creditCardPaymentRepository.findById(payment2.getId()))
                    .thenReturn(Optional.of(payment2));

            creditCardService.deleteDebt(debtId);

            verify(creditCardPaymentRepository, times(2)).delete(any(CreditCardPayment.class));
            verify(creditCardDebtRepository).delete(debt);
        }

        @Test
        @DisplayName("Refund wallet when deleting debt with payments already paid")
        void refundsWalletWhenDeletingDebtWithPaidPayments() {
            Integer debtId = 1;
            CreditCardDebt debt =
                    CreditCardDebt.builder().id(debtId).creditCard(creditCard).build();
            CreditCardPayment payment1 =
                    CreditCardPayment.builder()
                            .id(1)
                            .creditCardDebt(debt)
                            .wallet(wallet)
                            .amount(new BigDecimal("100.00"))
                            .build();
            CreditCardPayment payment2 =
                    CreditCardPayment.builder()
                            .id(2)
                            .creditCardDebt(debt)
                            .amount(new BigDecimal("50.00"))
                            .build();
            List<CreditCardPayment> payments = List.of(payment1, payment2);

            when(creditCardDebtRepository.findById(debtId)).thenReturn(Optional.of(debt));
            when(creditCardPaymentRepository.getPaymentsByDebtId(debtId)).thenReturn(payments);
            when(creditCardPaymentRepository.findById(payment1.getId()))
                    .thenReturn(Optional.of(payment1));
            when(creditCardPaymentRepository.findById(payment2.getId()))
                    .thenReturn(Optional.of(payment2));

            creditCardService.deleteDebt(debtId);

            verify(walletRepository).save(any(Wallet.class));
            verify(creditCardPaymentRepository, times(2)).delete(any(CreditCardPayment.class));
            verify(creditCardDebtRepository).delete(debt);
        }

        @Test
        @DisplayName("Throws EntityNotFoundException when payment does not exist")
        void throwsExceptionWhenPaymentDoesNotExist() {
            Integer debtId = 1;
            CreditCardDebt debt =
                    CreditCardDebt.builder().id(debtId).creditCard(creditCard).build();
            CreditCardPayment payment =
                    CreditCardPayment.builder().id(1).creditCardDebt(debt).build();

            when(creditCardDebtRepository.findById(debtId)).thenReturn(Optional.of(debt));
            when(creditCardPaymentRepository.getPaymentsByDebtId(debtId))
                    .thenReturn(List.of(payment));

            assertThrows(EntityNotFoundException.class, () -> creditCardService.deleteDebt(debtId));
        }

        @Test
        @DisplayName("Throws EntityNotFoundException when debt does not exist")
        void throwsExceptionWhenDebtDoesNotExist() {
            Integer debtId = 999;

            when(creditCardDebtRepository.findById(debtId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> creditCardService.deleteDebt(debtId));
        }

        @DisplayName("Deletes debt with no associated payments")
        @Test
        void deletesDebtWithNoAssociatedPayments() {
            Integer debtId = 1;
            CreditCardDebt debt = CreditCardDebt.builder().id(debtId).build();
            List<CreditCardPayment> payments = Collections.emptyList();

            when(creditCardDebtRepository.findById(debtId)).thenReturn(Optional.of(debt));
            when(creditCardPaymentRepository.getPaymentsByDebtId(debtId)).thenReturn(payments);

            creditCardService.deleteDebt(debtId);

            verify(creditCardPaymentRepository, never()).delete(any(CreditCardPayment.class));
            verify(creditCardDebtRepository).delete(debt);
        }
    }

    @Nested
    @DisplayName("Invoice Payment (payInvoice)")
    class PayInvoiceTests {
        @Test
        @DisplayName("Should pay invoice and update wallet balance correctly")
        void payInvoice_Success_NoRebate() {
            CreditCardDebt debt =
                    CreditCardDebt.builder().id(1).installments(2).creditCard(creditCard).build();
            CreditCardPayment payment1 =
                    CreditCardPayment.builder()
                            .id(1)
                            .creditCardDebt(debt)
                            .amount(new BigDecimal("150.00"))
                            .build();
            CreditCardPayment payment2 =
                    CreditCardPayment.builder()
                            .id(2)
                            .creditCardDebt(debt)
                            .amount(new BigDecimal("50.00"))
                            .build();
            List<CreditCardPayment> pendingPayments = List.of(payment1, payment2);

            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardPaymentRepository.getPendingCreditCardPayments(
                            anyInt(), anyInt(), anyInt()))
                    .thenReturn(pendingPayments);

            creditCardService.payInvoice(creditCard.getId(), wallet.getId(), 7, 2025);

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());

            assertEquals(new BigDecimal("300.00"), walletCaptor.getValue().getBalance());

            verify(creditCardPaymentRepository, times(2)).save(any(CreditCardPayment.class));
        }

        @Test
        @DisplayName("Should pay invoice with rebate and update balances correctly")
        void payInvoice_Success_WithRebate() {
            creditCard.setAvailableRebate(new BigDecimal("30.00"));

            CreditCardDebt debt =
                    CreditCardDebt.builder().id(1).installments(1).creditCard(creditCard).build();
            CreditCardPayment payment1 =
                    CreditCardPayment.builder()
                            .id(1)
                            .creditCardDebt(debt)
                            .amount(new BigDecimal("150.00"))
                            .build();
            CreditCardPayment payment2 =
                    CreditCardPayment.builder()
                            .id(2)
                            .creditCardDebt(debt)
                            .amount(new BigDecimal("50.00"))
                            .build();
            List<CreditCardPayment> pendingPayments = List.of(payment1, payment2);

            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardPaymentRepository.getPendingCreditCardPayments(
                            anyInt(), anyInt(), anyInt()))
                    .thenReturn(pendingPayments);

            creditCardService.payInvoice(
                    creditCard.getId(), wallet.getId(), 7, 2025, new BigDecimal("30.00"));

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());
            assertEquals(new BigDecimal("330.00"), walletCaptor.getValue().getBalance());

            ArgumentCaptor<CreditCard> cardCaptor = ArgumentCaptor.forClass(CreditCard.class);
            verify(creditCardRepository).save(cardCaptor.capture());
            assertEquals(
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    cardCaptor.getValue().getAvailableRebate());
        }

        @Test
        @DisplayName("Should cap rebate to pending payments total when rebate exceeds total")
        void payInvoice_RebateExceedsPendingPaymentsTotal_CapsRebate() {
            creditCard.setAvailableRebate(new BigDecimal("500.00"));
            CreditCardDebt debt =
                    CreditCardDebt.builder().id(1).installments(1).creditCard(creditCard).build();
            CreditCardPayment payment1 =
                    CreditCardPayment.builder()
                            .id(1)
                            .creditCardDebt(debt)
                            .amount(new BigDecimal("100.00"))
                            .build();
            CreditCardPayment payment2 =
                    CreditCardPayment.builder()
                            .id(2)
                            .creditCardDebt(debt)
                            .amount(new BigDecimal("50.00"))
                            .build();
            List<CreditCardPayment> pendingPayments = List.of(payment1, payment2);

            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardPaymentRepository.getPendingCreditCardPayments(
                            anyInt(), anyInt(), anyInt()))
                    .thenReturn(pendingPayments);

            creditCardService.payInvoice(
                    creditCard.getId(), wallet.getId(), 7, 2025, new BigDecimal("200.00"));

            ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
            verify(walletRepository).save(walletCaptor.capture());
            assertEquals(new BigDecimal("500.00"), walletCaptor.getValue().getBalance());

            ArgumentCaptor<CreditCard> cardCaptor = ArgumentCaptor.forClass(CreditCard.class);
            verify(creditCardRepository).save(cardCaptor.capture());
            assertEquals(new BigDecimal("350.00"), cardCaptor.getValue().getAvailableRebate());
        }

        @Test
        @DisplayName("Throws EntityNotFoundException when credit card does not exist")
        void throwsExceptionWhenCreditCardDoesNotExist() {
            Integer creditCardId = 999;
            Integer walletId = 1;

            when(creditCardRepository.findById(creditCardId)).thenReturn(Optional.empty());
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

            assertThrows(
                    EntityNotFoundException.class,
                    () -> creditCardService.payInvoice(creditCardId, walletId, 7, 2025));
        }

        @Test
        @DisplayName("Throws EntityNotFoundException when wallet does not exist")
        void throwsExceptionWhenWalletDoesNotExist() {
            Integer creditCardId = 999;

            assertThrows(
                    EntityNotFoundException.class,
                    () -> creditCardService.payInvoice(creditCardId, 999, 7, 2025));
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when rebate is negative")
        void payInvoice_RebateNegative_ThrowsException() {
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.payInvoice(
                                    creditCard.getId(),
                                    wallet.getId(),
                                    7,
                                    2025,
                                    new BigDecimal("-1")));
        }

        @Test
        @DisplayName("Throws InsufficientResourcesException when rebate exceeds available rebate")
        void payInvoice_RebateExceedsAvailable_ThrowsException() {
            creditCard.setAvailableRebate(new BigDecimal("10.00"));
            when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));

            assertThrows(
                    MoinexException.InsufficientResourcesException.class,
                    () ->
                            creditCardService.payInvoice(
                                    creditCard.getId(),
                                    wallet.getId(),
                                    7,
                                    2025,
                                    new BigDecimal("20.00")));
        }
    }

    @Nested
    @DisplayName("Credit Registration (addCredit)")
    class AddCreditTests {

        @DisplayName("Adds credit successfully and updates available rebate")
        @Test
        void addsCreditSuccessfully_UpdatesAvailableRebate() {
            Integer creditCardId = 1;
            BigDecimal amount = new BigDecimal("100.00");
            CreditCardCreditType type = CreditCardCreditType.CASHBACK;
            LocalDateTime date = LocalDateTime.now();
            String description = "Test Credit";

            CreditCard creditCard =
                    CreditCard.builder().id(creditCardId).availableRebate(BigDecimal.ZERO).build();

            when(creditCardRepository.findById(creditCardId)).thenReturn(Optional.of(creditCard));

            creditCardService.addCredit(creditCardId, date, amount, type, description);

            verify(creditCardCreditRepository).save(any(CreditCardCredit.class));
            verify(creditCardRepository).save(creditCard);
            assertEquals(amount, creditCard.getAvailableRebate());
        }

        @DisplayName("Throws EntityNotFoundException when credit card does not exist")
        @Test
        void throwsExceptionWhenCreditCardDoesNotExist() {
            Integer creditCardId = 999;

            when(creditCardRepository.findById(creditCardId)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () ->
                            creditCardService.addCredit(
                                    creditCardId,
                                    LocalDateTime.now(),
                                    BigDecimal.ONE,
                                    CreditCardCreditType.CASHBACK,
                                    "Test Credit"));
        }

        @DisplayName("Throws IllegalArgumentException when credit amount is zero")
        @Test
        void throwsExceptionWhenCreditAmountIsZero() {
            Integer creditCardId = 1;
            CreditCard creditCard = CreditCard.builder().id(creditCardId).build();

            when(creditCardRepository.findById(creditCardId)).thenReturn(Optional.of(creditCard));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addCredit(
                                    creditCardId,
                                    LocalDateTime.now(),
                                    BigDecimal.ZERO,
                                    CreditCardCreditType.CASHBACK,
                                    "Test Credit"));
        }

        @DisplayName("Throws IllegalArgumentException when credit amount is negative")
        @Test
        void throwsExceptionWhenCreditAmountIsNegative() {
            Integer creditCardId = 1;
            CreditCard creditCard = CreditCard.builder().id(creditCardId).build();

            when(creditCardRepository.findById(creditCardId)).thenReturn(Optional.of(creditCard));

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            creditCardService.addCredit(
                                    creditCardId,
                                    LocalDateTime.now(),
                                    new BigDecimal("-1"),
                                    CreditCardCreditType.CASHBACK,
                                    "Test Credit"));
        }
    }

    @Nested
    @DisplayName("Card Lifecycle (delete, archive, unarchive)")
    class CardLifecycleTests {
        @Test
        @DisplayName("Should delete credit card successfully if it has no debts")
        void deleteCreditCard_Success() {
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardDebtRepository.getDebtCountByCreditCard(creditCard.getId()))
                    .thenReturn(0);

            creditCardService.deleteCreditCard(creditCard.getId());

            verify(creditCardRepository).delete(creditCard);
        }

        @Test
        @DisplayName("Should archive credit card successfully if it has no pending payments")
        void archiveCreditCard_Success() {
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(
                            creditCard.getId()))
                    .thenReturn(BigDecimal.ZERO);

            creditCardService.archiveCreditCard(creditCard.getId());

            ArgumentCaptor<CreditCard> cardCaptor = ArgumentCaptor.forClass(CreditCard.class);
            verify(creditCardRepository).save(cardCaptor.capture());
            assertTrue(cardCaptor.getValue().isArchived());
        }

        @Test
        @DisplayName("Should unarchive credit card successfully")
        void unarchiveCreditCard_Success() {
            creditCard.setArchived(true);
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));

            creditCardService.unarchiveCreditCard(creditCard.getId());

            ArgumentCaptor<CreditCard> cardCaptor = ArgumentCaptor.forClass(CreditCard.class);
            verify(creditCardRepository).save(cardCaptor.capture());
            assertFalse(cardCaptor.getValue().isArchived());
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when credit card does not exist")
        void unarchiveCreditCard_NotFound_ThrowsException() {
            when(creditCardRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> creditCardService.unarchiveCreditCard(999));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when deleting a card with debts")
        void deleteCreditCard_WithDebts_ThrowsException() {
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardDebtRepository.getDebtCountByCreditCard(creditCard.getId()))
                    .thenReturn(1);

            assertThrows(
                    IllegalStateException.class,
                    () -> creditCardService.deleteCreditCard(creditCard.getId()));
        }

        @Test
        @DisplayName(
                "Should throw IllegalStateException when archiving a card with pending payments")
        void archiveCreditCard_WithPendingPayments_ThrowsException() {
            when(creditCardRepository.findById(creditCard.getId()))
                    .thenReturn(Optional.of(creditCard));
            when(creditCardPaymentRepository.getTotalPendingPaymentsByCreditCard(
                            creditCard.getId()))
                    .thenReturn(new BigDecimal("100.00"));

            assertThrows(
                    IllegalStateException.class,
                    () -> creditCardService.archiveCreditCard(creditCard.getId()));
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException when deleting a non-existent credit card")
        void deleteCreditCard_NotFound_ThrowsException() {
            when(creditCardRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class, () -> creditCardService.deleteCreditCard(999));
        }

        @Test
        @DisplayName(
                "Should throw EntityNotFoundException when archiving a non-existent credit card")
        void archiveCreditCard_NotFound_ThrowsException() {
            when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class,
                    () -> creditCardService.archiveCreditCard(creditCard.getId()));
        }
    }

    @Nested
    @DisplayName("Getters - getNextInvoiceDate")
    class GetNextInvoiceDateTests {
        CreditCard creditCard;

        @BeforeEach
        void setup() {
            creditCard = CreditCard.builder().id(1).closingDay(15).billingDueDay(20).build();
        }

        @DisplayName("Should return date from repository if it exists")
        @Test
        void getNextInvoiceDate_ReturnsDateFromRepositoryWhenAvailable() {
            String dbDate = "2025-08-25T10:30:00";
            when(creditCardPaymentRepository.getNextInvoiceDate(1)).thenReturn(dbDate);

            LocalDateTime result = creditCardService.getNextInvoiceDate(1);

            assertEquals(LocalDateTime.parse(dbDate, Constants.DB_DATE_FORMATTER), result);
            verify(creditCardRepository, never()).findById(anyInt());
        }

        @DisplayName("Should calculate date in SAME month if current day is BEFORE closing day")
        @Test
        void getNextInvoiceDate_CalculatesForSameMonth_WhenBeforeClosingDay() {
            LocalDateTime now = LocalDateTime.of(2025, 7, 10, 12, 0);
            when(creditCardRepository.findById(1)).thenReturn(Optional.of(creditCard));
            when(creditCardPaymentRepository.getNextInvoiceDate(1)).thenReturn(null);

            try (MockedStatic<LocalDateTime> mockedTime = mockStatic(LocalDateTime.class)) {
                mockedTime.when(LocalDateTime::now).thenReturn(now);

                LocalDateTime result = creditCardService.getNextInvoiceDate(1);
                LocalDateTime expected = now.withDayOfMonth(20);

                assertEquals(expected, result);
            }
        }

        @DisplayName("Should calculate date in NEXT month if current day is AFTER closing day")
        @Test
        void getNextInvoiceDate_CalculatesForNextMonth_WhenAfterClosingDay() {
            LocalDateTime now = LocalDateTime.of(2025, 7, 20, 12, 0);
            when(creditCardRepository.findById(1)).thenReturn(Optional.of(creditCard));
            when(creditCardPaymentRepository.getNextInvoiceDate(1)).thenReturn(null);

            try (MockedStatic<LocalDateTime> mockedTime = mockStatic(LocalDateTime.class)) {
                mockedTime.when(LocalDateTime::now).thenReturn(now);

                LocalDateTime result = creditCardService.getNextInvoiceDate(1);

                LocalDateTime expected =
                        now.plusMonths(1).withDayOfMonth(creditCard.getBillingDueDay());

                assertEquals(expected, result);
            }
        }

        @DisplayName("Should throw EntityNotFoundException when card is not found for calculation")
        @Test
        void getNextInvoiceDate_ThrowsException_WhenCardNotFoundForCalculation() {
            when(creditCardPaymentRepository.getNextInvoiceDate(999)).thenReturn(null);
            when(creditCardRepository.findById(999)).thenReturn(Optional.empty());

            assertThrows(
                    EntityNotFoundException.class, () -> creditCardService.getNextInvoiceDate(999));
        }
    }

    @Nested
    @DisplayName("Invoice Status Tests (getInvoiceStatus)")
    class GetInvoiceStatusTests {
        @Test
        @DisplayName("Should return OPEN when date is after next invoice date")
        void getInvoiceStatus_ReturnsOpen_WhenDateIsAfterNextInvoiceDate() {
            LocalDateTime nextInvoiceDate = LocalDateTime.of(2026, 2, 10, 0, 0);
            when(creditCardPaymentRepository.getNextInvoiceDate(creditCard.getId()))
                    .thenReturn(nextInvoiceDate.format(Constants.DB_DATE_FORMATTER));

            CreditCardInvoiceStatus status =
                    creditCardService.getInvoiceStatus(creditCard.getId(), 3, 2026);

            assertEquals(CreditCardInvoiceStatus.OPEN, status);
        }

        @Test
        @DisplayName("Should return OPEN when date is equal to next invoice date")
        void getInvoiceStatus_ReturnsOpen_WhenDateIsEqualToNextInvoiceDate() {
            LocalDateTime nextInvoiceDate = LocalDateTime.of(2026, 2, 10, 0, 0);
            when(creditCardPaymentRepository.getNextInvoiceDate(creditCard.getId()))
                    .thenReturn(nextInvoiceDate.format(Constants.DB_DATE_FORMATTER));

            CreditCardInvoiceStatus status =
                    creditCardService.getInvoiceStatus(creditCard.getId(), 2, 2026);

            assertEquals(CreditCardInvoiceStatus.OPEN, status);
        }

        @Test
        @DisplayName("Should return CLOSED when date is before next invoice date")
        void getInvoiceStatus_ReturnsClosed_WhenDateIsBeforeNextInvoiceDate() {
            LocalDateTime nextInvoiceDate = LocalDateTime.of(2026, 3, 10, 0, 0);
            when(creditCardPaymentRepository.getNextInvoiceDate(creditCard.getId()))
                    .thenReturn(nextInvoiceDate.format(Constants.DB_DATE_FORMATTER));

            CreditCardInvoiceStatus status =
                    creditCardService.getInvoiceStatus(creditCard.getId(), 2, 2026);

            assertEquals(CreditCardInvoiceStatus.CLOSED, status);
        }

        @Test
        @DisplayName("Should handle invoice status when next invoice date is calculated")
        void getInvoiceStatus_HandlesCalculatedNextInvoiceDate() {
            LocalDateTime futureInvoiceDate =
                    LocalDateTime.now().plusMonths(1).withDayOfMonth(creditCard.getBillingDueDay());
            when(creditCardPaymentRepository.getNextInvoiceDate(creditCard.getId()))
                    .thenReturn(futureInvoiceDate.format(Constants.DB_DATE_FORMATTER));

            CreditCardInvoiceStatus status =
                    creditCardService.getInvoiceStatus(
                            creditCard.getId(),
                            futureInvoiceDate.getMonthValue(),
                            futureInvoiceDate.getYear());

            assertEquals(CreditCardInvoiceStatus.OPEN, status);
        }
    }

    @Nested
    @DisplayName("Payment Date Tests")
    class PaymentDateTests {
        @Test
        @DisplayName("Should return earliest payment date when debts exist")
        void getEarliestPaymentDate_ReturnsDate_WhenDebtsExist() {
            LocalDateTime expectedDate = LocalDateTime.of(2025, 1, 15, 10, 30);
            when(creditCardDebtRepository.findEarliestPaymentDate())
                    .thenReturn(expectedDate.format(Constants.DB_DATE_FORMATTER));

            LocalDateTime result = creditCardService.getEarliestPaymentDate();

            assertEquals(expectedDate, result);
            verify(creditCardDebtRepository).findEarliestPaymentDate();
        }

        @Test
        @DisplayName("Should return current date when no debts exist for earliest date")
        void getEarliestPaymentDate_ReturnsNow_WhenNoDebtsExist() {
            when(creditCardDebtRepository.findEarliestPaymentDate()).thenReturn(null);

            try (MockedStatic<LocalDateTime> mockedDateTime =
                    org.mockito.Mockito.mockStatic(LocalDateTime.class)) {
                LocalDateTime now = LocalDateTime.of(2026, 1, 3, 16, 30);
                mockedDateTime.when(LocalDateTime::now).thenReturn(now);

                LocalDateTime result = creditCardService.getEarliestPaymentDate();

                assertEquals(now, result);
            }
        }

        @Test
        @DisplayName("Should return latest payment date when debts exist")
        void getLatestPaymentDate_ReturnsDate_WhenDebtsExist() {
            LocalDateTime expectedDate = LocalDateTime.of(2026, 12, 25, 14, 45);
            when(creditCardDebtRepository.findLatestPaymentDate())
                    .thenReturn(expectedDate.format(Constants.DB_DATE_FORMATTER));

            LocalDateTime result = creditCardService.getLatestPaymentDate();

            assertEquals(expectedDate, result);
            verify(creditCardDebtRepository).findLatestPaymentDate();
        }

        @Test
        @DisplayName("Should return current date when no debts exist for latest date")
        void getLatestPaymentDate_ReturnsNow_WhenNoDebtsExist() {
            when(creditCardDebtRepository.findLatestPaymentDate()).thenReturn(null);

            try (MockedStatic<LocalDateTime> mockedDateTime =
                    org.mockito.Mockito.mockStatic(LocalDateTime.class)) {
                LocalDateTime now = LocalDateTime.of(2026, 1, 3, 16, 30);
                mockedDateTime.when(LocalDateTime::now).thenReturn(now);

                LocalDateTime result = creditCardService.getLatestPaymentDate();

                assertEquals(now, result);
            }
        }

        @Test
        @DisplayName("Should handle date parsing correctly for earliest date")
        void getEarliestPaymentDate_ParsesDateCorrectly() {
            String dateString = "2025-06-15T08:30:00";
            when(creditCardDebtRepository.findEarliestPaymentDate()).thenReturn(dateString);

            LocalDateTime result = creditCardService.getEarliestPaymentDate();

            assertEquals(LocalDateTime.of(2025, 6, 15, 8, 30, 0), result);
        }

        @Test
        @DisplayName("Should handle date parsing correctly for latest date")
        void getLatestPaymentDate_ParsesDateCorrectly() {
            String dateString = "2027-03-20T18:45:30";
            when(creditCardDebtRepository.findLatestPaymentDate()).thenReturn(dateString);

            LocalDateTime result = creditCardService.getLatestPaymentDate();

            assertEquals(LocalDateTime.of(2027, 3, 20, 18, 45, 30), result);
        }
    }
}
