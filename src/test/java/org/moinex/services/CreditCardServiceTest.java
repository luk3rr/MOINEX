/*
 * Filename: CreditCardServiceTest.java
 * Created on: September  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.moinex.entities.Category;
import org.moinex.entities.CreditCard;
import org.moinex.entities.CreditCardDebt;
import org.moinex.entities.CreditCardOperator;
import org.moinex.entities.CreditCardPayment;
import org.moinex.repositories.CategoryRepository;
import org.moinex.repositories.CreditCardDebtRepository;
import org.moinex.repositories.CreditCardOperatorRepository;
import org.moinex.repositories.CreditCardPaymentRepository;
import org.moinex.repositories.CreditCardRepository;
import org.moinex.util.Constants;

@ExtendWith(MockitoExtension.class)
public class CreditCardServiceTest
{
    @Mock
    private CreditCardDebtRepository m_creditCardDebtRepository;

    @Mock
    private CreditCardPaymentRepository m_creditCardPaymentRepository;

    @Mock
    private CreditCardRepository m_creditCardRepository;

    @Mock
    private CreditCardOperatorRepository m_creditCardOperatorRepository;

    @Mock
    private CategoryRepository m_categoryRepository;

    @InjectMocks
    private CreditCardService m_creditCardService;

    private CreditCard         m_creditCard;
    private CreditCardOperator m_operator;
    private Category           m_category;
    private LocalDateTime      m_registerDate;
    private YearMonth          m_invoiceMonth;
    private String             m_description;
    private String             m_crcLastFourDigits;

    @BeforeAll
    public static void SetUp()
    {
        MockitoAnnotations.openMocks(CreditCardServiceTest.class);
    }

    @BeforeEach
    public void BeforeEach()
    {
        m_crcLastFourDigits = "1234";
        m_operator          = new CreditCardOperator(1L, "Operator");
        m_creditCard        = new CreditCard("Credit Card",
                                      10,
                                      4,
                                      new BigDecimal("1000.0"),
                                      m_crcLastFourDigits,
                                      m_operator);

        m_category     = new Category("Category");
        m_registerDate = LocalDateTime.now();
        m_invoiceMonth = YearMonth.now();
        m_description  = "";
    }

    @Test
    @DisplayName("Test if the credit card is created successfully")
    public void TestCreateCreditCard()
    {
        when(m_creditCardRepository.save(any(CreditCard.class)))
            .thenReturn(m_creditCard);

        when(m_creditCardRepository.existsByName(m_creditCard.GetName()))
            .thenReturn(false);

        when(m_creditCardOperatorRepository.findById(m_operator.GetId()))
            .thenReturn(Optional.of(m_operator));

        m_creditCardService.CreateCreditCard(m_creditCard.GetName(),
                                             m_creditCard.GetBillingDueDay(),
                                             m_creditCard.GetClosingDay(),
                                             m_creditCard.GetMaxDebt(),
                                             m_creditCard.GetLastFourDigits(),
                                             m_creditCard.GetOperator().GetId());

        // Capture the credit card that was saved and check if it is correct
        ArgumentCaptor<CreditCard> creditCardCaptor =
            ArgumentCaptor.forClass(CreditCard.class);

        verify(m_creditCardRepository).save(creditCardCaptor.capture());

        CreditCard creditCard = creditCardCaptor.getValue();

        assertEquals(m_creditCard.GetName(), creditCard.GetName());
        assertEquals(m_creditCard.GetBillingDueDay(), creditCard.GetBillingDueDay());
        assertEquals(m_creditCard.GetMaxDebt(), creditCard.GetMaxDebt());
    }

    @Test
    @DisplayName(
        "Test if the credit card is not created when the name is already in use")
    public void
    TestCreateCreditCardAlreadyExists()
    {
        when(m_creditCardRepository.existsByName(m_creditCard.GetName()))
            .thenReturn(true);

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.CreateCreditCard(
                             m_creditCard.GetName(),
                             m_creditCard.GetBillingDueDay(),
                             m_creditCard.GetClosingDay(),
                             m_creditCard.GetMaxDebt(),
                             m_creditCard.GetLastFourDigits(),
                             m_creditCard.GetOperator().GetId()));

        // Verify that the credit card was not saved
        verify(m_creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "Test if the credit card is not created when the billing due day is invalid")
    public void
    TestCreateCreditCardInvalidDueDate()
    {
        when(m_creditCardRepository.existsByName(m_creditCard.GetName()))
            .thenReturn(false);

        // Case when the billing due day is less than 1
        m_creditCard.SetBillingDueDay(0);

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.CreateCreditCard(
                             m_creditCard.GetName(),
                             m_creditCard.GetBillingDueDay(),
                             m_creditCard.GetClosingDay(),
                             m_creditCard.GetMaxDebt(),
                             m_creditCard.GetLastFourDigits(),
                             m_creditCard.GetOperator().GetId()));

        // Case when the billing due day is greater than Constants.MAX_BILLING_DUE_DAY
        m_creditCard.SetBillingDueDay(Constants.MAX_BILLING_DUE_DAY + 1);

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.CreateCreditCard(
                             m_creditCard.GetName(),
                             m_creditCard.GetBillingDueDay(),
                             m_creditCard.GetClosingDay(),
                             m_creditCard.GetMaxDebt(),
                             m_creditCard.GetLastFourDigits(),
                             m_creditCard.GetOperator().GetId()));

        // Verify that the credit card was not saved
        verify(m_creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the credit card is not created when the max debt is negative")
    public void TestCreateCreditCardNegativeMaxDebt()
    {
        when(m_creditCardRepository.existsByName(m_creditCard.GetName()))
            .thenReturn(false);

        // Case when the max debt is negative
        m_creditCard.SetMaxDebt(new BigDecimal("-1.0"));

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.CreateCreditCard(
                             m_creditCard.GetName(),
                             m_creditCard.GetBillingDueDay(),
                             m_creditCard.GetClosingDay(),
                             m_creditCard.GetMaxDebt(),
                             m_creditCard.GetLastFourDigits(),
                             m_creditCard.GetOperator().GetId()));

        // Verify that the credit card was not saved
        verify(m_creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "Test if the credit card is not when last four digits is blank or not has 4 "
        + "digits")
    public void
    TestCreateCreditCardInvalidLastFourDigits()
    {
        when(m_creditCardRepository.existsByName(m_creditCard.GetName()))
            .thenReturn(false);

        // Case when the last four digits is blank
        m_creditCard.SetLastFourDigits("");

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.CreateCreditCard(
                             m_creditCard.GetName(),
                             m_creditCard.GetBillingDueDay(),
                             m_creditCard.GetClosingDay(),
                             m_creditCard.GetMaxDebt(),
                             m_creditCard.GetLastFourDigits(),
                             m_creditCard.GetOperator().GetId()));

        // Case when the last four digits has less than 4 digits
        m_creditCard.SetLastFourDigits("123");

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.CreateCreditCard(
                             m_creditCard.GetName(),
                             m_creditCard.GetBillingDueDay(),
                             m_creditCard.GetClosingDay(),
                             m_creditCard.GetMaxDebt(),
                             m_creditCard.GetLastFourDigits(),
                             m_creditCard.GetOperator().GetId()));

        // Case when the last four digits has more than 4 digits
        m_creditCard.SetLastFourDigits("12345");

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.CreateCreditCard(
                             m_creditCard.GetName(),
                             m_creditCard.GetBillingDueDay(),
                             m_creditCard.GetClosingDay(),
                             m_creditCard.GetMaxDebt(),
                             m_creditCard.GetLastFourDigits(),
                             m_creditCard.GetOperator().GetId()));

        // Verify that the credit card was not saved
        verify(m_creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName(
        "Test if the credit card is not created when the operator does not exist")
    public void
    TestCreateCreditCardOperatorDoesNotExist()
    {
        when(m_creditCardRepository.existsByName(m_creditCard.GetName()))
            .thenReturn(false);

        when(m_creditCardOperatorRepository.findById(m_operator.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.CreateCreditCard(
                             m_creditCard.GetName(),
                             m_creditCard.GetBillingDueDay(),
                             m_creditCard.GetClosingDay(),
                             m_creditCard.GetMaxDebt(),
                             m_creditCard.GetLastFourDigits(),
                             m_creditCard.GetOperator().GetId()));

        // Verify that the credit card was not saved
        verify(m_creditCardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test if the credit card is deleted successfully")
    public void TestDeleteCreditCard()
    {
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        m_creditCardService.DeleteCreditCard(m_creditCard.GetId());

        // Verify that the credit card was deleted
        verify(m_creditCardRepository).delete(m_creditCard);
    }

    @Test
    @DisplayName("Test if the credit card is not deleted when it does not exist")
    public void TestDeleteCreditCardDoesNotExist()
    {
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     () -> m_creditCardService.DeleteCreditCard(m_creditCard.GetId()));

        // Verify that the credit card was not deleted
        verify(m_creditCardRepository, never()).delete(any(CreditCard.class));
    }

    @Test
    @DisplayName(
        "Test if the available credit is returned correctly when there is no debt")
    public void
    TestGetAvailableCredit()
    {
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(
            m_creditCardPaymentRepository.GetTotalPendingPayments(m_creditCard.GetId()))
            .thenReturn(new BigDecimal("0.0"));

        BigDecimal availableCredit =
            m_creditCardService.GetAvailableCredit(m_creditCard.GetId());

        assertEquals(m_creditCard.GetMaxDebt(), availableCredit);
    }

    @Test
    @DisplayName(
        "Test if the available credit is returned correctly when there is a debt")
    public void
    TestGetAvailableCreditWithDebt()
    {
        BigDecimal maxDebt              = m_creditCard.GetMaxDebt();
        BigDecimal totalPendingPayments = maxDebt.divide(new BigDecimal("2"));

        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(
            m_creditCardPaymentRepository.GetTotalPendingPayments(m_creditCard.GetId()))
            .thenReturn(totalPendingPayments);

        BigDecimal availableCredit =
            m_creditCardService.GetAvailableCredit(m_creditCard.GetId());

        assertEquals(maxDebt.subtract(totalPendingPayments).doubleValue(),
                     availableCredit.doubleValue(),
                     Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if the available credit is returned correctly when there is a "
                 + "debt and payments")
    public void
    TestGetAvailableCreditWithDebtAndPayments()
    {
        m_creditCard.SetMaxDebt(new BigDecimal("1000.0"));

        BigDecimal totalPendingPayments = new BigDecimal("200.0");

        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(
            m_creditCardPaymentRepository.GetTotalPendingPayments(m_creditCard.GetId()))
            .thenReturn(totalPendingPayments);

        BigDecimal availableCredit =
            m_creditCardService.GetAvailableCredit(m_creditCard.GetId());

        assertEquals(
            new BigDecimal("1000.0").subtract(totalPendingPayments).doubleValue(),
            availableCredit.doubleValue(),
            Constants.EPSILON);
    }

    @Test
    @DisplayName("Test if exception is thrown when the credit card does not exist")
    public void TestGetAvailableCreditDoesNotExist()
    {
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(
            RuntimeException.class,
            () -> m_creditCardService.GetAvailableCredit(m_creditCard.GetId()));
    }

    @Test
    @DisplayName("Test if the debt is registered successfully")
    public void TestRegisterDebt()
    {
        m_creditCard.SetMaxDebt(new BigDecimal("1000.0"));

        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.of(m_category));

        when(
            m_creditCardPaymentRepository.GetTotalPendingPayments(m_creditCard.GetId()))
            .thenReturn(new BigDecimal("0.0"));

        m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                         m_category,
                                         m_registerDate,
                                         m_invoiceMonth,
                                         new BigDecimal("100.0"),
                                         1,
                                         m_description);

        // Verify that the debt was registered
        verify(m_creditCardDebtRepository).save(any(CreditCardDebt.class));

        // Verify that the payments were registered
        verify(m_creditCardPaymentRepository).save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the credit card does not exist")
    public void TestRegisterDebtCreditCardDoesNotExist()
    {
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                                             m_category,
                                                             m_registerDate,
                                                             m_invoiceMonth,
                                                             new BigDecimal("100.0"),
                                                             1,
                                                             m_description));

        // Verify that the debt was not registered
        verify(m_creditCardDebtRepository, never()).save(any(CreditCardDebt.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the category does not exist")
    public void TestRegisterDebtCategoryDoesNotExist()
    {
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                                             m_category,
                                                             m_registerDate,
                                                             m_invoiceMonth,
                                                             new BigDecimal("100.0"),
                                                             1,
                                                             m_description));

        // Verify that the debt was not registered
        verify(m_creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(m_creditCardPaymentRepository, never())
            .save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the value is negative")
    public void TestRegisterDebtNegativeValue()
    {
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.of(m_category));

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                                             m_category,
                                                             m_registerDate,
                                                             m_invoiceMonth,
                                                             new BigDecimal("-1.0"),
                                                             1,
                                                             m_description));

        // Verify that the debt was not registered
        verify(m_creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(m_creditCardPaymentRepository, never())
            .save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the installment is less than 1")
    public void TestRegisterDebtInvalidInstallment()
    {
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.of(m_category));

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                                             m_category,
                                                             m_registerDate,
                                                             m_invoiceMonth,
                                                             new BigDecimal("100.0"),
                                                             0,
                                                             m_description));

        // Verify that the debt was not registered
        verify(m_creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(m_creditCardPaymentRepository, never())
            .save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName("Test if exception is thrown when the installment is greater than "
                 + "Constants.MAX_INSTALLMENTS")
    public void
    TestRegisterDebtInvalidInstallment2()
    {
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.of(m_category));

        assertThrows(
            RuntimeException.class,
            ()
                -> m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                                    m_category,
                                                    m_registerDate,
                                                    m_invoiceMonth,
                                                    new BigDecimal("100.0"),
                                                    Constants.MAX_INSTALLMENTS + 1,
                                                    m_description));

        // Verify that the debt was not registered
        verify(m_creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(m_creditCardPaymentRepository, never())
            .save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName(
        "Test if exception is thrown when the credit card does not have enough "
        + "credit to register the debt")
    public void
    TestRegisterDebtNotEnoughCredit()
    {
        m_creditCard.SetMaxDebt(new BigDecimal("100.0"));

        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.of(m_category));

        assertThrows(RuntimeException.class,
                     ()
                         -> m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                                             m_category,
                                                             m_registerDate,
                                                             m_invoiceMonth,
                                                             new BigDecimal("200.0"),
                                                             1,
                                                             m_description));

        // Verify that the debt was not registered
        verify(m_creditCardDebtRepository, never()).save(any(CreditCardDebt.class));

        // Verify that the payments were not registered
        verify(m_creditCardPaymentRepository, never())
            .save(any(CreditCardPayment.class));
    }

    @Test
    @DisplayName("Test if the payment is registered successfully")
    public void TestRegisterPayment()
    {
        // Setup mocks
        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.of(m_category));

        when(
            m_creditCardPaymentRepository.GetTotalPendingPayments(m_creditCard.GetId()))
            .thenReturn(new BigDecimal("0.0"));

        // Capture the payment that was saved and check if it is correct
        ArgumentCaptor<CreditCardPayment> paymentCaptor =
            ArgumentCaptor.forClass(CreditCardPayment.class);

        BigDecimal debtValue = new BigDecimal("100.0");

        m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                         m_category,
                                         m_registerDate,
                                         m_invoiceMonth,
                                         debtValue,
                                         5,
                                         m_description);

        // Verify if the payment was saved
        verify(m_creditCardPaymentRepository, times(5)).save(paymentCaptor.capture());

        // Get the captured payments and check if they are correct
        List<CreditCardPayment> capturedPayments = paymentCaptor.getAllValues();

        assertEquals(5, capturedPayments.size(), "The number of payments is incorrect");

        BigDecimal expectedInstallmentValue = debtValue.divide(new BigDecimal("5"));

        for (Integer installmentNumber = 0; installmentNumber < capturedPayments.size();
             installmentNumber++)
        {
            CreditCardPayment payment = capturedPayments.get(installmentNumber);

            // Check if the payment amount is correct
            assertEquals(expectedInstallmentValue.doubleValue(),
                         payment.GetAmount().doubleValue(),
                         Constants.EPSILON,
                         "The payment amount of installment " + installmentNumber +
                             " is incorrect");

            // Check if the installment number is correct
            assertEquals(installmentNumber + 1,
                         payment.GetInstallment(),
                         "The installment number of installment " + installmentNumber +
                             " is incorrect");

            // Check if the payment date is correct
            LocalDateTime expectedPaymentDate =
                m_invoiceMonth.plusMonths(installmentNumber)
                    .atDay(m_creditCard.GetBillingDueDay())
                    .atTime(23, 59);

            assertEquals(expectedPaymentDate,
                         payment.GetDate(),
                         "The payment date of installment " + installmentNumber +
                             " is incorrect");

            // Check if wallet is set correctly as null
            assertEquals(null,
                         payment.GetWallet(),
                         "The wallet of installment " + installmentNumber +
                             " is incorrect");
        }
    }

    @Test
    @DisplayName("Test if the payment is registered correctly when the debt is divided "
                 + "into installments")
    public void
    TestRegisterPaymentWithInstallmentsExactDivision()
    {
        // Case: 120 / 3 = 40
        BigDecimal debtValue    = new BigDecimal("120.0");
        Integer    installments = 3;

        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.of(m_category));

        when(
            m_creditCardPaymentRepository.GetTotalPendingPayments(m_creditCard.GetId()))
            .thenReturn(new BigDecimal("0.0"));

        // Capture the payment that was saved and check if it is correct
        ArgumentCaptor<CreditCardPayment> paymentCaptor =
            ArgumentCaptor.forClass(CreditCardPayment.class);

        m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                         m_category,
                                         m_registerDate,
                                         m_invoiceMonth,
                                         debtValue,
                                         installments,
                                         m_description);

        // Verify if the payment was saved
        verify(m_creditCardPaymentRepository, times(installments))
            .save(paymentCaptor.capture());

        // Get the captured payments and check if they are correct
        List<CreditCardPayment> capturedPayments = paymentCaptor.getAllValues();

        assertEquals(installments,
                     capturedPayments.size(),
                     "The number of payments is incorrect");

        BigDecimal expectedInstallmentValue =
            debtValue.divide(new BigDecimal(installments));

        for (Integer installmentNumber = 0; installmentNumber < capturedPayments.size();
             installmentNumber++)
        {
            CreditCardPayment payment = capturedPayments.get(installmentNumber);

            // Check if the payment amount is correct
            assertEquals(expectedInstallmentValue.doubleValue(),
                         payment.GetAmount().doubleValue(),
                         Constants.EPSILON,
                         "The payment amount of installment " + installmentNumber +
                             " is incorrect");
        }
    }

    @Test
    @DisplayName("Test if the payment is registered correctly when the debt is not "
                 + "divided into installments")
    public void
    TestRegisterPaymentWithInstallmentsNotExactDivisionCase1()
    {
        // 100 / 3 =
        // - 1st: 33.34
        // - 2nd: 33.33
        // - 3rd: 33.33
        BigDecimal debtValue    = new BigDecimal("100.0");
        Integer    installments = 3;

        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.of(m_category));

        when(
            m_creditCardPaymentRepository.GetTotalPendingPayments(m_creditCard.GetId()))
            .thenReturn(new BigDecimal("0.0"));

        // Capture the payment that was saved and check if it is correct
        ArgumentCaptor<CreditCardPayment> paymentCaptor =
            ArgumentCaptor.forClass(CreditCardPayment.class);

        m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                         m_category,
                                         m_registerDate,
                                         m_invoiceMonth,
                                         debtValue,
                                         installments,
                                         m_description);

        // Verify if the payment was saved
        verify(m_creditCardPaymentRepository, times(installments))
            .save(paymentCaptor.capture());

        // Get the captured payments and check if they are correct
        List<CreditCardPayment> capturedPayments = paymentCaptor.getAllValues();

        assertEquals(installments,
                     capturedPayments.size(),
                     "The number of payments is incorrect");

        assertEquals(BigDecimal.valueOf(33.34),
                     capturedPayments.get(0).GetAmount(),
                     "Incorrent value of installment 1");
        assertEquals(BigDecimal.valueOf(33.33),
                     capturedPayments.get(1).GetAmount(),
                     "Incorrent value of installment 2");
        assertEquals(BigDecimal.valueOf(33.33),
                     capturedPayments.get(2).GetAmount(),
                     "Incorrent value of installment 3");
    }

    @Test
    @DisplayName("Test if the payment is registered correctly when the debt is not "
                 + "divided into installments")
    public void
    TestRegisterPaymentWithInstallmentsNotExactDivisionCase2()
    {
        // 100 / 6 =
        // - 1st: 16.70
        // - 2nd: 16.66
        // - 3rd: 16.66
        // - 5th: 16.66
        // - 6th: 16.66
        // - 7th: 16.66
        BigDecimal debtValue    = new BigDecimal("100.0");
        Integer    installments = 6;

        when(m_creditCardRepository.findById(m_creditCard.GetId()))
            .thenReturn(Optional.of(m_creditCard));

        when(m_categoryRepository.findById(m_category.GetId()))
            .thenReturn(Optional.of(m_category));

        when(
            m_creditCardPaymentRepository.GetTotalPendingPayments(m_creditCard.GetId()))
            .thenReturn(new BigDecimal("0.0"));

        // Capture the payment that was saved and check if it is correct
        ArgumentCaptor<CreditCardPayment> paymentCaptor =
            ArgumentCaptor.forClass(CreditCardPayment.class);

        m_creditCardService.RegisterDebt(m_creditCard.GetId(),
                                         m_category,
                                         m_registerDate,
                                         m_invoiceMonth,
                                         debtValue,
                                         installments,
                                         m_description);

        // Verify if the payment was saved
        verify(m_creditCardPaymentRepository, times(installments))
            .save(paymentCaptor.capture());

        // Get the captured payments and check if they are correct
        List<CreditCardPayment> capturedPayments = paymentCaptor.getAllValues();

        assertEquals(installments,
                     capturedPayments.size(),
                     "The number of payments is incorrect");

        assertEquals(new BigDecimal("16.70"),
                     capturedPayments.get(0).GetAmount(),
                     "Incorrent value of installment 1");
        assertEquals(BigDecimal.valueOf(16.66),
                     capturedPayments.get(1).GetAmount(),
                     "Incorrent value of installment 2");
        assertEquals(BigDecimal.valueOf(16.66),
                     capturedPayments.get(2).GetAmount(),
                     "Incorrent value of installment 3");
        assertEquals(BigDecimal.valueOf(16.66),
                     capturedPayments.get(3).GetAmount(),
                     "Incorrent value of installment 4");
        assertEquals(BigDecimal.valueOf(16.66),
                     capturedPayments.get(4).GetAmount(),
                     "Incorrent value of installment 5");
        assertEquals(BigDecimal.valueOf(16.66),
                     capturedPayments.get(5).GetAmount(),
                     "Incorrent value of installment 6");
    }
}
