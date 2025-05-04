/*
 * Filename: CreditCardServiceTest.java
 * Created on: September  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.moinex.error.MoinexException;
import org.moinex.model.Category;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.creditcard.CreditCardDebt;
import org.moinex.model.creditcard.CreditCardOperator;
import org.moinex.model.creditcard.CreditCardPayment;
import org.moinex.repository.CategoryRepository;
import org.moinex.repository.creditcard.CreditCardDebtRepository;
import org.moinex.repository.creditcard.CreditCardOperatorRepository;
import org.moinex.repository.creditcard.CreditCardPaymentRepository;
import org.moinex.repository.creditcard.CreditCardRepository;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceTest {
    @Mock private CreditCardDebtRepository creditCardDebtRepository;

    @Mock private CreditCardPaymentRepository creditCardPaymentRepository;

    @Mock private CreditCardRepository creditCardRepository;

    @Mock private CreditCardOperatorRepository creditCardOperatorRepository;

    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private CreditCardService creditCardService;

    private CreditCard creditCard;
    private CreditCardOperator operator;
    private Category category;
    private LocalDateTime registerDate;
    private YearMonth invoiceMonth;
    private String description;

    @BeforeAll
    static void setUp() {
        MockitoAnnotations.openMocks(CreditCardServiceTest.class);
    }

    @BeforeEach
    void beforeEach() {
        String crcLastFourDigits = "1234";
        operator = new CreditCardOperator(1L, "Operator");

        creditCard =
                CreditCard.builder()
                        .name("Credit Card")
                        .billingDueDay(10)
                        .closingDay(4)
                        .maxDebt(new BigDecimal("1000.0"))
                        .lastFourDigits(crcLastFourDigits)
                        .operator(operator)
                        .build();

        category = Category.builder().name("Category").build();
        registerDate = LocalDateTime.now();
        invoiceMonth = YearMonth.now();
        description = "";
    }

    @Test
    @DisplayName("Test if the credit card is created successfully")
    void testCreateCreditCard() {
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(creditCard);

        when(creditCardRepository.existsByName(creditCard.getName())).thenReturn(false);

        when(creditCardOperatorRepository.findById(operator.getId()))
                .thenReturn(Optional.of(operator));

        creditCardService.addCreditCard(
                creditCard.getName(),
                creditCard.getBillingDueDay(),
                creditCard.getClosingDay(),
                creditCard.getMaxDebt(),
                creditCard.getLastFourDigits(),
                creditCard.getOperator().getId());

        // Capture the credit card that was saved and check if it is correct
        ArgumentCaptor<CreditCard> creditCardCaptor = ArgumentCaptor.forClass(CreditCard.class);

        verify(creditCardRepository).save(creditCardCaptor.capture());

        CreditCard ccd = creditCardCaptor.getValue();

        assertEquals(this.creditCard.getName(), ccd.getName());
        assertEquals(this.creditCard.getBillingDueDay(), ccd.getBillingDueDay());
        assertEquals(this.creditCard.getMaxDebt(), ccd.getMaxDebt());
    }

    @Test
    @DisplayName("Test if the credit card is not created when the name is already in use")
    void testCreateCreditCardAlreadyExists() {
        when(creditCardRepository.existsByName(creditCard.getName())).thenReturn(true);

        String creditCardName = creditCard.getName();
        int billingDueDay = creditCard.getBillingDueDay();
        int closingDay = creditCard.getClosingDay();
        BigDecimal maxDebt = creditCard.getMaxDebt();
        String lastFourDigits = creditCard.getLastFourDigits();
        Long operatorId = creditCard.getOperator().getId();

        assertThrows(
                EntityExistsException.class,
                () ->
                        creditCardService.addCreditCard(
                                creditCardName,
                                billingDueDay,
                                closingDay,
                                maxDebt,
                                lastFourDigits,
                                operatorId));

        // Verify that the credit card was not saved
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the credit card is not created when the billing due day is invalid")
    void testCreateCreditCardInvalidDueDate() {
        when(creditCardRepository.existsByName(creditCard.getName())).thenReturn(false);

        // Case when the billing due day is less than 1
        creditCard.setBillingDueDay(0);

        String name = creditCard.getName();
        int billingDueDay = creditCard.getBillingDueDay();
        int closingDay = creditCard.getClosingDay();
        BigDecimal maxDebt = creditCard.getMaxDebt();
        String lastFourDigits = creditCard.getLastFourDigits();
        Long operatorId = creditCard.getOperator().getId();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        creditCardService.addCreditCard(
                                name,
                                billingDueDay,
                                closingDay,
                                maxDebt,
                                lastFourDigits,
                                operatorId));

        // Case when the billing due day is greater than Constants.MAX_BILLING_DUE_DAY
        creditCard.setBillingDueDay(Constants.MAX_BILLING_DUE_DAY + 1);
        int updatedBillingDueDay = creditCard.getBillingDueDay();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        creditCardService.addCreditCard(
                                name,
                                updatedBillingDueDay,
                                closingDay,
                                maxDebt,
                                lastFourDigits,
                                operatorId));

        // Verify that the credit card was not saved
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the credit card is not created when the max debt is negative")
    void testCreateCreditCardNegativeMaxDebt() {
        when(creditCardRepository.existsByName(creditCard.getName())).thenReturn(false);

        // Case when the max debt is negative
        creditCard.setMaxDebt(new BigDecimal("-1.0"));

        String creditCardName = creditCard.getName();
        int billingDueDay = creditCard.getBillingDueDay();
        int closingDay = creditCard.getClosingDay();
        BigDecimal maxDebt = creditCard.getMaxDebt();
        String lastFourDigits = creditCard.getLastFourDigits();
        Long operatorId = creditCard.getOperator().getId();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        creditCardService.addCreditCard(
                                creditCardName,
                                billingDueDay,
                                closingDay,
                                maxDebt,
                                lastFourDigits,
                                operatorId));

        // Verify that the credit card was not saved
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName(
            "Test if the credit card is not when last four digits is blank or not has 4 "
                    + "digits")
    void testCreateCreditCardInvalidLastFourDigits() {
        when(creditCardRepository.existsByName(creditCard.getName())).thenReturn(false);

        // Case when the last four digits is blank
        creditCard.setLastFourDigits("");

        String creditCardName = creditCard.getName();
        int billingDueDay = creditCard.getBillingDueDay();
        int closingDay = creditCard.getClosingDay();
        BigDecimal maxDebt = creditCard.getMaxDebt();
        String emptyLastFourDigits = creditCard.getLastFourDigits();
        Long operatorId = creditCard.getOperator().getId();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        creditCardService.addCreditCard(
                                creditCardName,
                                billingDueDay,
                                closingDay,
                                maxDebt,
                                emptyLastFourDigits,
                                operatorId));

        // Case when the last four digits has less than 4 digits
        creditCard.setLastFourDigits("123");
        String threeNumbersLastFourDigits = creditCard.getLastFourDigits();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        creditCardService.addCreditCard(
                                creditCardName,
                                billingDueDay,
                                closingDay,
                                maxDebt,
                                threeNumbersLastFourDigits,
                                operatorId));

        // Case when the last four digits has more than 4 digits
        creditCard.setLastFourDigits("12345");
        String fiveNumbersLastFourDigits = creditCard.getLastFourDigits();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        creditCardService.addCreditCard(
                                creditCardName,
                                billingDueDay,
                                closingDay,
                                maxDebt,
                                fiveNumbersLastFourDigits,
                                operatorId));

        // Verify that the credit card was not saved
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the credit card is not created when the operator does not exist")
    void testCreateCreditCardOperatorDoesNotExists() {
        when(creditCardRepository.existsByName(creditCard.getName())).thenReturn(false);

        when(creditCardOperatorRepository.findById(operator.getId())).thenReturn(Optional.empty());

        String creditCardName = creditCard.getName();
        int billingDueDay = creditCard.getBillingDueDay();
        int closingDay = creditCard.getClosingDay();
        BigDecimal maxDebt = creditCard.getMaxDebt();
        String lastFourDigits = creditCard.getLastFourDigits();
        Long operatorId = creditCard.getOperator().getId();

        assertThrows(
                EntityNotFoundException.class,
                () ->
                        creditCardService.addCreditCard(
                                creditCardName,
                                billingDueDay,
                                closingDay,
                                maxDebt,
                                lastFourDigits,
                                operatorId));

        // Verify that the credit card was not saved
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the credit card is deleted successfully")
    void testDeleteCreditCard() {
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        creditCardService.deleteCreditCard(creditCard.getId());

        // Verify that the credit card was deleted
        verify(creditCardRepository).delete(creditCard);
    }

    @Test
    @DisplayName("Test if the credit card is not deleted when it does not exist")
    void testDeleteCreditCardDoesNotExists() {
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.empty());

        Long creditCardId = creditCard.getId();

        assertThrows(
                EntityNotFoundException.class,
                () -> creditCardService.deleteCreditCard(creditCardId));

        // Verify that the credit card was not deleted
        verify(creditCardRepository, never()).delete(any(CreditCard.class));
    }

    @Test
    @DisplayName("Test if the available credit is returned correctly when there is no debt")
    void testGetAvailableCredit() {
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(creditCardPaymentRepository.getTotalPendingPayments(creditCard.getId()))
                .thenReturn(new BigDecimal("0.0"));

        BigDecimal availableCredit = creditCardService.getAvailableCredit(creditCard.getId());

        assertEquals(creditCard.getMaxDebt(), availableCredit);
    }

    @Test
    @DisplayName("Test if the available credit is returned correctly when there is a debt")
    void testGetAvailableCreditWithDebt() {
        BigDecimal maxDebt = creditCard.getMaxDebt();
        BigDecimal totalPendingPayments = maxDebt.divide(new BigDecimal("2"), RoundingMode.HALF_UP);
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(creditCardPaymentRepository.getTotalPendingPayments(creditCard.getId()))
                .thenReturn(totalPendingPayments);

        BigDecimal availableCredit = creditCardService.getAvailableCredit(creditCard.getId());

        assertEquals(
                maxDebt.subtract(totalPendingPayments).doubleValue(),
                availableCredit.doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName(
            "Test if the available credit is returned correctly when there is a "
                    + "debt and payments")
    void testGetAvailableCreditWithDebtAndPayments() {
        creditCard.setMaxDebt(new BigDecimal("1000.0"));

        BigDecimal totalPendingPayments = new BigDecimal("200.0");

        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(creditCardPaymentRepository.getTotalPendingPayments(creditCard.getId()))
                .thenReturn(totalPendingPayments);

        BigDecimal availableCredit = creditCardService.getAvailableCredit(creditCard.getId());

        assertEquals(
                new BigDecimal("1000.0").subtract(totalPendingPayments).doubleValue(),
                availableCredit.doubleValue(),
                Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the credit card does not exist")
    void testGetAvailableCreditDoesNotExists() {
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.empty());

        Long creditCardId = creditCard.getId();

        assertThrows(
                EntityNotFoundException.class,
                () -> creditCardService.getAvailableCredit(creditCardId));
    }

    @Test
    @DisplayName("Test if the debt is registered successfully")
    void testRegisterDebt() {
        creditCard.setMaxDebt(new BigDecimal("1000.0"));

        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        when(creditCardPaymentRepository.getTotalPendingPayments(creditCard.getId()))
                .thenReturn(new BigDecimal("0.0"));

        creditCardService.addDebt(
                creditCard.getId(),
                category,
                registerDate,
                invoiceMonth,
                new BigDecimal("100.0"),
                1,
                description);

        // Verify that the debt was registered
        verify(creditCardDebtRepository).save(any(CreditCardDebt.class));

        // Verify that the payments were registered
        verify(creditCardPaymentRepository).save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the credit card does not exist")
    void testRegisterDebtCreditCardDoesNotExists() {
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.empty());

        Long creditCardId = creditCard.getId();
        Category debtCategory = category;
        LocalDateTime debtRegisterDate = registerDate;
        YearMonth debtInvoiceMonth = invoiceMonth;
        BigDecimal debtValue = new BigDecimal("100.0");
        int installments = 1;
        String debtDescription = description;

        assertThrows(
                EntityNotFoundException.class,
                () ->
                        creditCardService.addDebt(
                                creditCardId,
                                debtCategory,
                                debtRegisterDate,
                                debtInvoiceMonth,
                                debtValue,
                                installments,
                                debtDescription));

        // Verify that the debt was not registered
        verify(creditCardDebtRepository, never()).save(any(CreditCardDebt.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the category does not exist")
    void testRegisterDebtCategoryDoesNotExists() {
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

        Long creditCardId = creditCard.getId();
        Category debtCategory = category;
        LocalDateTime debtRegisterDate = registerDate;
        YearMonth debtInvoiceMonth = invoiceMonth;
        BigDecimal debtValue = new BigDecimal("100.0");
        int installments = 1;
        String debtDescription = description;

        assertThrows(
                EntityNotFoundException.class,
                () ->
                        creditCardService.addDebt(
                                creditCardId,
                                debtCategory,
                                debtRegisterDate,
                                debtInvoiceMonth,
                                debtValue,
                                installments,
                                debtDescription));

        // Verify that the debt was not registered
        verify(creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(creditCardPaymentRepository, never()).save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the value is negative")
    void testRegisterDebtNegativeValue() {
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        Long creditCardId = creditCard.getId();
        Category debtCategory = category;
        LocalDateTime debtRegisterDate = registerDate;
        YearMonth debtInvoiceMonth = invoiceMonth;
        BigDecimal debtValue = new BigDecimal("-1.0");
        int installments = 1;
        String debtDescription = description;

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        creditCardService.addDebt(
                                creditCardId,
                                debtCategory,
                                debtRegisterDate,
                                debtInvoiceMonth,
                                debtValue,
                                installments,
                                debtDescription));

        // Verify that the debt was not registered
        verify(creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(creditCardPaymentRepository, never()).save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the installment is less than 1")
    void testRegisterDebtInvalidInstallment() {
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        Long creditCardId = creditCard.getId();
        Category debtCategory = category;
        LocalDateTime debtRegisterDate = registerDate;
        YearMonth debtInvoiceMonth = invoiceMonth;
        BigDecimal debtValue = new BigDecimal("100.0");
        int installments = 0;
        String debtDescription = description;

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        creditCardService.addDebt(
                                creditCardId,
                                debtCategory,
                                debtRegisterDate,
                                debtInvoiceMonth,
                                debtValue,
                                installments,
                                debtDescription));

        // Verify that the debt was not registered
        verify(creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(creditCardPaymentRepository, never()).save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when the installment is greater than "
                    + "Constants.MAX_INSTALLMENTS")
    void testRegisterDebtInvalidInstallment2() {
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        Long creditCardId = creditCard.getId();
        Category debtCategory = category;
        LocalDateTime debtRegisterDate = registerDate;
        YearMonth debtInvoiceMonth = invoiceMonth;
        BigDecimal debtValue = new BigDecimal("100.0");
        int installments = Constants.MAX_INSTALLMENTS + 1;
        String debtDescription = description;

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        creditCardService.addDebt(
                                creditCardId,
                                debtCategory,
                                debtRegisterDate,
                                debtInvoiceMonth,
                                debtValue,
                                installments,
                                debtDescription));

        // Verify that the debt was not registered
        verify(creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(creditCardPaymentRepository, never()).save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName(
            "Test if exception is thrown when the credit card does not have enough "
                    + "credit to register the debt")
    void testRegisterDebtNotEnoughCredit() {
        creditCard.setMaxDebt(new BigDecimal("100.0"));

        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        when(creditCardPaymentRepository.getTotalPendingPayments(creditCard.getId()))
                .thenReturn(new BigDecimal("0.0"));

        assertThrows(
                MoinexException.InsufficientResourcesException.class,
                () ->
                        creditCardService.addDebt(
                                creditCard.getId(),
                                category,
                                registerDate,
                                invoiceMonth,
                                new BigDecimal("200.0"),
                                1,
                                description));

        // Verify that the debt was not registered
        verify(creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(creditCardPaymentRepository, never()).save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName("Test if the payment is registered successfully")
    void testRegisterPayment() {
        // Setup mocks
        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        when(creditCardPaymentRepository.getTotalPendingPayments(creditCard.getId()))
                .thenReturn(new BigDecimal("0.0"));

        // Capture the payment that was saved and check if it is correct
        ArgumentCaptor<CreditCardPayment> paymentCaptor =
                ArgumentCaptor.forClass(CreditCardPayment.class);

        BigDecimal debtValue = new BigDecimal("100.0");

        creditCardService.addDebt(
                creditCard.getId(),
                category,
                registerDate,
                invoiceMonth,
                debtValue,
                5,
                description);

        // Verify if the payment was saved
        verify(creditCardPaymentRepository, times(5)).save(paymentCaptor.capture());

        // Get the captured payments and check if they are correct
        List<CreditCardPayment> capturedPayments = paymentCaptor.getAllValues();

        assertEquals(5, capturedPayments.size(), "The number of payments is incorrect");

        BigDecimal expectedInstallmentValue =
                debtValue.divide(new BigDecimal("5"), RoundingMode.HALF_UP);

        for (int installmentNumber = 0;
                installmentNumber < capturedPayments.size();
                installmentNumber++) {
            CreditCardPayment payment = capturedPayments.get(installmentNumber);

            // Check if the payment amount is correct
            assertEquals(
                    expectedInstallmentValue.doubleValue(),
                    payment.getAmount().doubleValue(),
                    Constants.EPSILON,
                    "The payment amount of installment " + installmentNumber + " is incorrect");

            // Check if the installment number is correct
            assertEquals(
                    installmentNumber + 1,
                    payment.getInstallment(),
                    "The installment number of installment " + installmentNumber + " is incorrect");

            // Check if the payment date is correct
            LocalDateTime expectedPaymentDate =
                    invoiceMonth
                            .plusMonths(installmentNumber)
                            .atDay(creditCard.getBillingDueDay())
                            .atTime(23, 59);

            assertEquals(
                    expectedPaymentDate,
                    payment.getDate(),
                    "The payment date of installment " + installmentNumber + " is incorrect");

            // Check if wallet is set correctly as null
            assertNull(
                    payment.getWallet(),
                    "The wallet of installment " + installmentNumber + " is incorrect");
        }
    }

    @Test
    @DisplayName(
            "Test if the payment is registered correctly when the debt is divided "
                    + "into installments")
    void testRegisterPaymentWithInstallmentsExactDivision() {
        // Case: 120 / 3 = 40
        BigDecimal debtValue = new BigDecimal("120.0");
        Integer installments = 3;

        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        when(creditCardPaymentRepository.getTotalPendingPayments(creditCard.getId()))
                .thenReturn(new BigDecimal("0.0"));

        // Capture the payment that was saved and check if it is correct
        ArgumentCaptor<CreditCardPayment> paymentCaptor =
                ArgumentCaptor.forClass(CreditCardPayment.class);

        creditCardService.addDebt(
                creditCard.getId(),
                category,
                registerDate,
                invoiceMonth,
                debtValue,
                installments,
                description);

        // Verify if the payment was saved
        verify(creditCardPaymentRepository, times(installments)).save(paymentCaptor.capture());

        // Get the captured payments and check if they are correct
        List<CreditCardPayment> capturedPayments = paymentCaptor.getAllValues();

        assertEquals(installments, capturedPayments.size(), "The number of payments is incorrect");

        BigDecimal expectedInstallmentValue =
                debtValue.divide(new BigDecimal(installments), RoundingMode.HALF_UP);

        for (int installmentNumber = 0;
                installmentNumber < capturedPayments.size();
                installmentNumber++) {
            CreditCardPayment payment = capturedPayments.get(installmentNumber);

            // Check if the payment amount is correct
            assertEquals(
                    expectedInstallmentValue.doubleValue(),
                    payment.getAmount().doubleValue(),
                    Constants.EPSILON,
                    "The payment amount of installment " + installmentNumber + " is incorrect");
        }
    }

    @Test
    @DisplayName(
            "Test if the payment is registered correctly when the debt is not "
                    + "divided into installments")
    void testRegisterPaymentWithInstallmentsNotExactDivisionCase1() {
        // 100 / 3 =
        // - 1st: 33.34
        // - 2nd: 33.33
        // - 3rd: 33.33
        BigDecimal debtValue = new BigDecimal("100.0");
        Integer installments = 3;

        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        when(creditCardPaymentRepository.getTotalPendingPayments(creditCard.getId()))
                .thenReturn(new BigDecimal("0.0"));

        // Capture the payment that was saved and check if it is correct
        ArgumentCaptor<CreditCardPayment> paymentCaptor =
                ArgumentCaptor.forClass(CreditCardPayment.class);

        creditCardService.addDebt(
                creditCard.getId(),
                category,
                registerDate,
                invoiceMonth,
                debtValue,
                installments,
                description);

        // Verify if the payment was saved
        verify(creditCardPaymentRepository, times(installments)).save(paymentCaptor.capture());

        // Get the captured payments and check if they are correct
        List<CreditCardPayment> capturedPayments = paymentCaptor.getAllValues();

        assertEquals(installments, capturedPayments.size(), "The number of payments is incorrect");

        assertEquals(
                BigDecimal.valueOf(33.34),
                capturedPayments.get(0).getAmount(),
                "Incorrect value of installment 1");
        assertEquals(
                BigDecimal.valueOf(33.33),
                capturedPayments.get(1).getAmount(),
                "Incorrect value of installment 2");
        assertEquals(
                BigDecimal.valueOf(33.33),
                capturedPayments.get(2).getAmount(),
                "Incorrect value of installment 3");
    }

    @Test
    @DisplayName(
            "Test if the payment is registered correctly when the debt is not "
                    + "divided into installments")
    void testRegisterPaymentWithInstallmentsNotExactDivisionCase2() {
        // 100 / 6 =
        // - 1st: 16.70
        // - 2nd: 16.66
        // - 3rd: 16.66
        // - 5th: 16.66
        // - 6th: 16.66
        // - 7th: 16.66
        BigDecimal debtValue = new BigDecimal("100.0");
        Integer installments = 6;

        when(creditCardRepository.findById(creditCard.getId())).thenReturn(Optional.of(creditCard));

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        when(creditCardPaymentRepository.getTotalPendingPayments(creditCard.getId()))
                .thenReturn(new BigDecimal("0.0"));

        // Capture the payment that was saved and check if it is correct
        ArgumentCaptor<CreditCardPayment> paymentCaptor =
                ArgumentCaptor.forClass(CreditCardPayment.class);

        creditCardService.addDebt(
                creditCard.getId(),
                category,
                registerDate,
                invoiceMonth,
                debtValue,
                installments,
                description);

        // Verify if the payment was saved
        verify(creditCardPaymentRepository, times(installments)).save(paymentCaptor.capture());

        // Get the captured payments and check if they are correct
        List<CreditCardPayment> capturedPayments = paymentCaptor.getAllValues();

        assertEquals(installments, capturedPayments.size(), "The number of payments is incorrect");

        assertEquals(
                new BigDecimal("16.70"),
                capturedPayments.get(0).getAmount(),
                "Incorrect value of installment 1");
        assertEquals(
                BigDecimal.valueOf(16.66),
                capturedPayments.get(1).getAmount(),
                "Incorrect value of installment 2");
        assertEquals(
                BigDecimal.valueOf(16.66),
                capturedPayments.get(2).getAmount(),
                "Incorrect value of installment 3");
        assertEquals(
                BigDecimal.valueOf(16.66),
                capturedPayments.get(3).getAmount(),
                "Incorrect value of installment 4");
        assertEquals(
                BigDecimal.valueOf(16.66),
                capturedPayments.get(4).getAmount(),
                "Incorrect value of installment 5");
        assertEquals(
                BigDecimal.valueOf(16.66),
                capturedPayments.get(5).getAmount(),
                "Incorrect value of installment 6");
    }
}
