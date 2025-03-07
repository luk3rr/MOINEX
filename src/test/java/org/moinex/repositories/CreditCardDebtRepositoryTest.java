/*
 * Filename: CreditCardDebtRepositoryTest.java
 * Created on: September  5, 2024
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
import org.moinex.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Tests for the CreditCardDebtRepository
 */
@DataJpaTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { MainApplication.class })
@ActiveProfiles("test")
class CreditCardDebtRepositoryTest
{
    @Autowired
    private CreditCardDebtRepository m_creditCardDebtRepository;

    @Autowired
    private CreditCardRepository m_creditCardRepository;

    @Autowired
    private CreditCardOperatorRepository m_creditCardOperatorRepository;

    @Autowired
    private CategoryRepository m_categoryRepository;

    private CreditCard         m_creditCard;
    private CreditCardOperator m_crcOperator;

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
        CreditCardOperator creditCardOperator =
            CreditCardOperator.builder().name(name).icon("").build();
        m_creditCardOperatorRepository.save(creditCardOperator);
        return creditCardOperator;
    }

    private Category createCategory(String name)
    {
        Category category = Category.builder().name(name).build();
        m_categoryRepository.save(category);
        return category;
    }

    private CreditCardDebt createCreditCardDebt(CreditCard    m_creditCard,
                                                BigDecimal    totalAmount,
                                                LocalDateTime date)
    {
        CreditCardDebt creditCardDebt = CreditCardDebt.builder()
                                            .creditCard(m_creditCard)
                                            .installments(1)
                                            .amount(totalAmount)
                                            .date(date)
                                            .category(createCategory("category"))
                                            .build();

        m_creditCardDebtRepository.save(creditCardDebt);
        return creditCardDebt;
    }

    @BeforeEach
    void setUp()
    {
        // Initialize the credit card
        m_crcOperator = createCreditCardOperator("Operator");
        m_creditCard =
            createCreditCard("CreditCard", m_crcOperator, new BigDecimal("1000.0"));
    }

    @Test
    void testNoDebt()
    {
        // No debt yet
        assertEquals(
            0.0,
            m_creditCardDebtRepository.getTotalDebt(m_creditCard.getId()).doubleValue(),
            Constants.EPSILON,
            "Total debt must be 0.0");
    }

    @Test
    void testSingleDebt()
    {
        createCreditCardDebt(m_creditCard,
                             new BigDecimal("1000.0"),
                             LocalDateTime.now().plusDays(10));

        assertEquals(
            1000.0,
            m_creditCardDebtRepository.getTotalDebt(m_creditCard.getId()).doubleValue(),
            Constants.EPSILON,
            "Total debt must be 1000.0");
    }

    @Test
    void testMultipleDebts()
    {
        createCreditCardDebt(m_creditCard,
                             new BigDecimal("1000.0"),
                             LocalDateTime.now().plusDays(10));

        createCreditCardDebt(m_creditCard,
                             new BigDecimal("500.0"),
                             LocalDateTime.now().plusDays(5));

        assertEquals(
            1500.0,
            m_creditCardDebtRepository.getTotalDebt(m_creditCard.getId()).doubleValue(),
            Constants.EPSILON,
            "Total debt must be 1500.0");
    }

    @Test
    void testDebtsForMultipleCreditCards()
    {
        CreditCard creditCard1 =
            createCreditCard("CreditCard1", m_crcOperator, new BigDecimal("1000.0"));

        CreditCard creditCard2 =
            createCreditCard("CreditCard2", m_crcOperator, new BigDecimal("2000.0"));

        createCreditCardDebt(creditCard1,
                             new BigDecimal("1000.0"),
                             LocalDateTime.now().plusDays(10));

        createCreditCardDebt(creditCard2,
                             new BigDecimal("500.0"),
                             LocalDateTime.now().plusDays(5));

        assertEquals(
            1000.0,
            m_creditCardDebtRepository.getTotalDebt(creditCard1.getId()).doubleValue(),
            Constants.EPSILON,
            "Total debt for CreditCard1 must be 1000.0");

        assertEquals(
            500.0,
            m_creditCardDebtRepository.getTotalDebt(creditCard2.getId()).doubleValue(),
            Constants.EPSILON,
            "Total debt for CreditCard2 must be 500.0");
    }
}
