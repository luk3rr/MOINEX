/*
 * Filename: CreditCardPaymentRepositoryTest.java
 * Created on: September  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moinex.app.config.AppConfig;
import org.moinex.model.Category;
import org.moinex.model.creditcard.CreditCard;
import org.moinex.model.creditcard.CreditCardDebt;
import org.moinex.model.creditcard.CreditCardOperator;
import org.moinex.model.creditcard.CreditCardPayment;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.repository.creditcard.CreditCardDebtRepository;
import org.moinex.repository.creditcard.CreditCardOperatorRepository;
import org.moinex.repository.creditcard.CreditCardPaymentRepository;
import org.moinex.repository.creditcard.CreditCardRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests for the CreditCardPaymentRepository
 */
@DataJpaTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {AppConfig.class})
@ActiveProfiles("test")
class CreditCardPaymentRepositoryTest {
    @Autowired private CreditCardPaymentRepository creditCardPaymentRepository;

    @Autowired private CreditCardRepository creditCardRepository;

    @Autowired private WalletRepository walletRepository;

    @Autowired private CreditCardDebtRepository creditCardDebtRepository;

    @Autowired private CreditCardOperatorRepository creditCardOperatorRepository;

    @Autowired private CategoryRepository categoryRepository;

    private CreditCard creditCard1;
    private CreditCard creditCard2;
    private Wallet wallet;

    private CreditCard createCreditCard(
            String name, CreditCardOperator operator, BigDecimal maxDebt) {
        CreditCard creditCard =
                CreditCard.builder()
                        .name(name)
                        .maxDebt(maxDebt)
                        .billingDueDay(10)
                        .closingDay(5)
                        .operator(operator)
                        .build();

        creditCardRepository.save(creditCard);
        return creditCard;
    }

    private CreditCardOperator createCreditCardOperator(String name) {
        CreditCardOperator ccdOperator = CreditCardOperator.builder().name(name).icon("").build();
        creditCardOperatorRepository.save(ccdOperator);
        return ccdOperator;
    }

    private Wallet createWallet(String name, BigDecimal balance) {
        Wallet wt = Wallet.builder().name(name).balance(balance).build();
        walletRepository.save(wt);
        return wt;
    }

    private CreditCardDebt createCreditCardDebt(CreditCard creditCard, BigDecimal totalAmount) {
        CreditCardDebt creditCardDebt =
                CreditCardDebt.builder()
                        .installments(1)
                        .amount(totalAmount)
                        .creditCard(creditCard)
                        .date(LocalDateTime.now().plusDays(5))
                        .category(createCategory("category"))
                        .build();

        creditCardDebtRepository.save(creditCardDebt);
        return creditCardDebt;
    }

    private Category createCategory(String name) {
        Category category = Category.builder().name(name).build();
        categoryRepository.save(category);
        return category;
    }

    private void createCreditCardPayment(CreditCardDebt debt, Wallet wallet, BigDecimal amount) {
        CreditCardPayment creditCardPayment =
                CreditCardPayment.builder()
                        .creditCardDebt(debt)
                        .wallet(wallet)
                        .amount(amount)
                        .date(LocalDateTime.now())
                        .installment(1)
                        .build();

        creditCardPaymentRepository.save(creditCardPayment);
    }

    @BeforeEach
    void setUp() {
        // Initialize CreditCard and Wallet
        CreditCardOperator creditCardOperator = createCreditCardOperator("Operator");
        creditCard1 = createCreditCard("CreditCard1", creditCardOperator, new BigDecimal("1000.0"));
        creditCard2 = createCreditCard("CreditCard2", creditCardOperator, new BigDecimal("1000.0"));
        wallet = createWallet("Wallet", new BigDecimal("1000.0"));
    }

    @Test
    void testNoPayments() {
        // No payments yet
        assertEquals(
                0.0,
                creditCardPaymentRepository.getTotalPaidAmount(creditCard1.getId()).doubleValue(),
                Constants.EPSILON,
                "Total paid amount must be 0.0");
    }

    @Test
    void testSinglePayment() {
        // Create CreditCardDebt and Payment
        CreditCardDebt debt = createCreditCardDebt(creditCard1, new BigDecimal("500.0"));

        createCreditCardPayment(debt, wallet, new BigDecimal("100.0"));

        assertEquals(
                100.0,
                creditCardPaymentRepository.getTotalPaidAmount(creditCard1.getId()).doubleValue(),
                Constants.EPSILON,
                "Total paid amount must be 100.0");
    }

    @Test
    void testMultiplePayments() {
        // Create CreditCardDebt and Payments
        CreditCardDebt debt = createCreditCardDebt(creditCard1, new BigDecimal("500.0"));

        createCreditCardPayment(debt, wallet, new BigDecimal("100.0"));
        createCreditCardPayment(debt, wallet, new BigDecimal("200.0"));

        assertEquals(
                300.0,
                creditCardPaymentRepository.getTotalPaidAmount(creditCard1.getId()).doubleValue(),
                Constants.EPSILON,
                "Total paid amount must be 300.0");
    }

    @Test
    void testPaymentsForMultipleCreditCards() {
        // Create CreditCardDebt and Payments for both credit cards
        CreditCardDebt debt1 = createCreditCardDebt(creditCard1, new BigDecimal("500.0"));
        CreditCardDebt debt2 = createCreditCardDebt(creditCard2, new BigDecimal("500.0"));

        createCreditCardPayment(debt1, wallet, new BigDecimal("100.0"));
        createCreditCardPayment(debt1, wallet, new BigDecimal("200.0"));
        createCreditCardPayment(debt2, wallet, new BigDecimal("255.0"));

        assertEquals(
                300.0,
                creditCardPaymentRepository.getTotalPaidAmount(creditCard1.getId()).doubleValue(),
                Constants.EPSILON,
                "Total paid amount must be 300.0");

        assertEquals(
                255.0,
                creditCardPaymentRepository.getTotalPaidAmount(creditCard2.getId()).doubleValue(),
                Constants.EPSILON,
                "Total paid amount must be 255.0");
    }
}
