/*
 * Filename: CreditCardPaymentRepositoryTest.java
 * Created on: September  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.moinex.app.MainApplication;
import org.moinex.entities.Category;
import org.moinex.entities.CreditCard;
import org.moinex.entities.CreditCardDebt;
import org.moinex.entities.CreditCardOperator;
import org.moinex.entities.CreditCardPayment;
import org.moinex.entities.Wallet;
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
@ContextConfiguration(classes = { MainApplication.class })
@ActiveProfiles("test")
public class CreditCardPaymentRepositoryTest
{
    @Autowired
    private CreditCardPaymentRepository m_creditCardPaymentRepository;

    @Autowired
    private CreditCardRepository m_creditCardRepository;

    @Autowired
    private WalletRepository m_walletRepository;

    @Autowired
    private CreditCardDebtRepository m_creditCardDebtRepository;

    @Autowired
    private CreditCardOperatorRepository m_creditCardOperatorRepository;

    @Autowired
    private CategoryRepository m_categoryRepository;

    private CreditCard         m_creditCard1;
    private CreditCard         m_creditCard2;
    private CreditCardOperator m_crcOperator;
    private Wallet             m_wallet;

    private CreditCard
    createCreditCard(String name, CreditCardOperator operator, BigDecimal maxDebt)
    {
        CreditCard creditCard = CreditCard.builder()
                                    .name(name)
                                    .maxDebt(maxDebt)
                                    .billingDueDay(10)
                                    .closingDay(5)
                                    .operator(operator)
                                    .build();

        m_creditCardRepository.save(creditCard);
        return creditCard;
    }

    private CreditCardOperator createCreditCardOperator(String name)
    {
        CreditCardOperator creditCardOperator = CreditCardOperator.builder().name(name).icon("").build();
        m_creditCardOperatorRepository.save(creditCardOperator);
        return creditCardOperator;
    }

    private Wallet createWallet(String name, BigDecimal balance)
    {
        Wallet m_wallet = Wallet.builder().name(name).balance(balance).build();
        m_walletRepository.save(m_wallet);
        return m_wallet;
    }

    private CreditCardDebt createCreditCardDebt(CreditCard creditCard,
                                                BigDecimal totalAmount)
    {
        CreditCardDebt creditCardDebt = CreditCardDebt.builder()
                                            .installments(1)
                                            .totalAmount(totalAmount)
                                            .creditCard(creditCard)
                                            .date(LocalDateTime.now().plusDays(5))
                                            .category(createCategory("category"))
                                            .build();

        m_creditCardDebtRepository.save(creditCardDebt);
        return creditCardDebt;
    }

    private Category createCategory(String name)
    {
        Category category = Category.builder().name(name).build();
        m_categoryRepository.save(category);
        return category;
    }

    private void
    createCreditCardPayment(CreditCardDebt debt, Wallet wallet, BigDecimal amount)
    {
        CreditCardPayment creditCardPayment = CreditCardPayment.builder()
            .creditCardDebt(debt)
            .wallet(wallet)
            .amount(amount)
            .date(LocalDateTime.now())
            .installment(1)
            .build();

        m_creditCardPaymentRepository.save(creditCardPayment);
    }

    @BeforeEach
    public void setUp()
    {
        // Initialize CreditCard and Wallet
        m_crcOperator = createCreditCardOperator("Operator");
        m_creditCard1 =
            createCreditCard("CreditCard1", m_crcOperator, new BigDecimal("1000.0"));
        m_creditCard2 =
            createCreditCard("CreditCard2", m_crcOperator, new BigDecimal("1000.0"));
        m_wallet = createWallet("Wallet", new BigDecimal("1000.0"));
    }

    @Test
    public void testNoPayments()
    {
        // No payments yet
        assertEquals(
            0.0,
            m_creditCardPaymentRepository.getTotalPaidAmount(m_creditCard1.getId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 0.0");
    }

    @Test
    public void testSinglePayment()
    {
        // Create CreditCardDebt and Payment
        CreditCardDebt debt =
            createCreditCardDebt(m_creditCard1, new BigDecimal("500.0"));

        createCreditCardPayment(debt, m_wallet, new BigDecimal("100.0"));

        assertEquals(
            100.0,
            m_creditCardPaymentRepository.getTotalPaidAmount(m_creditCard1.getId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 100.0");
    }

    @Test
    public void testMultiplePayments()
    {
        // Create CreditCardDebt and Payments
        CreditCardDebt debt =
            createCreditCardDebt(m_creditCard1, new BigDecimal("500.0"));

        createCreditCardPayment(debt, m_wallet, new BigDecimal("100.0"));
        createCreditCardPayment(debt, m_wallet, new BigDecimal("200.0"));

        assertEquals(
            300.0,
            m_creditCardPaymentRepository.getTotalPaidAmount(m_creditCard1.getId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 300.0");
    }

    @Test
    public void testPaymentsForMultipleCreditCards()
    {
        // Create CreditCardDebt and Payments for both credit cards
        CreditCardDebt debt1 =
            createCreditCardDebt(m_creditCard1, new BigDecimal("500.0"));
        CreditCardDebt debt2 =
            createCreditCardDebt(m_creditCard2, new BigDecimal("500.0"));

        createCreditCardPayment(debt1, m_wallet, new BigDecimal("100.0"));
        createCreditCardPayment(debt1, m_wallet, new BigDecimal("200.0"));
        createCreditCardPayment(debt2, m_wallet, new BigDecimal("255.0"));

        assertEquals(
            300.0,
            m_creditCardPaymentRepository.getTotalPaidAmount(m_creditCard1.getId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 300.0");

        assertEquals(
            255.0,
            m_creditCardPaymentRepository.getTotalPaidAmount(m_creditCard2.getId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 255.0");
    }
}
