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
    CreateCreditCard(String name, CreditCardOperator operator, BigDecimal maxDebt)
    {
        CreditCard creditCard = new CreditCard();
        creditCard.SetName(name);
        creditCard.SetMaxDebt(maxDebt);
        creditCard.SetBillingDueDay(10);
        creditCard.SetClosingDay(5);
        creditCard.SetOperator(operator);
        m_creditCardRepository.save(creditCard);
        return creditCard;
    }

    private CreditCardOperator CreateCreditCardOperator(String name)
    {
        CreditCardOperator creditCardOperator = new CreditCardOperator();
        creditCardOperator.SetName(name);
        creditCardOperator.SetIcon("");
        m_creditCardOperatorRepository.save(creditCardOperator);
        return creditCardOperator;
    }

    private Wallet CreateWallet(String name, BigDecimal balance)
    {
        Wallet m_wallet = new Wallet();
        m_wallet.SetName(name);
        m_wallet.SetBalance(balance);
        m_walletRepository.save(m_wallet);
        return m_wallet;
    }

    private CreditCardDebt CreateCreditCardDebt(CreditCard creditCard,
                                                BigDecimal totalAmount)
    {
        CreditCardDebt creditCardDebt = new CreditCardDebt();
        creditCardDebt.SetInstallments(1);
        creditCardDebt.SetTotalAmount(totalAmount);
        creditCardDebt.SetCreditCard(creditCard);
        creditCardDebt.SetDate(LocalDateTime.now().plusDays(5));
        creditCardDebt.SetCategory(CreateCategory("category"));
        m_creditCardDebtRepository.save(creditCardDebt);
        return creditCardDebt;
    }

    private Category CreateCategory(String name)
    {
        Category category = new Category(name);
        m_categoryRepository.save(category);
        return category;
    }

    private void
    CreateCreditCardPayment(CreditCardDebt debt, Wallet wallet, BigDecimal amount)
    {
        CreditCardPayment creditCardPayment = new CreditCardPayment();
        creditCardPayment.SetAmount(amount);
        creditCardPayment.SetCreditCardDebt(debt);
        creditCardPayment.SetWallet(wallet);
        creditCardPayment.SetDate(LocalDateTime.now());
        creditCardPayment.SetInstallment(1);
        m_creditCardPaymentRepository.save(creditCardPayment);
    }

    @BeforeEach
    public void SetUp()
    {
        // Initialize CreditCard and Wallet
        m_crcOperator = CreateCreditCardOperator("Operator");
        m_creditCard1 =
            CreateCreditCard("CreditCard1", m_crcOperator, new BigDecimal("1000.0"));
        m_creditCard2 =
            CreateCreditCard("CreditCard2", m_crcOperator, new BigDecimal("1000.0"));
        m_wallet = CreateWallet("Wallet", new BigDecimal("1000.0"));
    }

    @Test
    public void TestNoPayments()
    {
        // No payments yet
        assertEquals(
            0.0,
            m_creditCardPaymentRepository.GetTotalPaidAmount(m_creditCard1.GetId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 0.0");
    }

    @Test
    public void TestSinglePayment()
    {
        // Create CreditCardDebt and Payment
        CreditCardDebt debt =
            CreateCreditCardDebt(m_creditCard1, new BigDecimal("500.0"));

        CreateCreditCardPayment(debt, m_wallet, new BigDecimal("100.0"));

        assertEquals(
            100.0,
            m_creditCardPaymentRepository.GetTotalPaidAmount(m_creditCard1.GetId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 100.0");
    }

    @Test
    public void TestMultiplePayments()
    {
        // Create CreditCardDebt and Payments
        CreditCardDebt debt =
            CreateCreditCardDebt(m_creditCard1, new BigDecimal("500.0"));

        CreateCreditCardPayment(debt, m_wallet, new BigDecimal("100.0"));
        CreateCreditCardPayment(debt, m_wallet, new BigDecimal("200.0"));

        assertEquals(
            300.0,
            m_creditCardPaymentRepository.GetTotalPaidAmount(m_creditCard1.GetId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 300.0");
    }

    @Test
    public void TestPaymentsForMultipleCreditCards()
    {
        // Create CreditCardDebt and Payments for both credit cards
        CreditCardDebt debt1 =
            CreateCreditCardDebt(m_creditCard1, new BigDecimal("500.0"));
        CreditCardDebt debt2 =
            CreateCreditCardDebt(m_creditCard2, new BigDecimal("500.0"));

        CreateCreditCardPayment(debt1, m_wallet, new BigDecimal("100.0"));
        CreateCreditCardPayment(debt1, m_wallet, new BigDecimal("200.0"));
        CreateCreditCardPayment(debt2, m_wallet, new BigDecimal("255.0"));

        assertEquals(
            300.0,
            m_creditCardPaymentRepository.GetTotalPaidAmount(m_creditCard1.GetId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 300.0");

        assertEquals(
            255.0,
            m_creditCardPaymentRepository.GetTotalPaidAmount(m_creditCard2.GetId())
                .doubleValue(),
            Constants.EPSILON,
            "Total paid amount must be 255.0");
    }
}
