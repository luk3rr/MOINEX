/*
 * Filename: CreditCardDebtRepositoryTest.java
 * Created on: September  5, 2024
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
import org.moinex.repository.creditcard.CreditCardDebtRepository;
import org.moinex.repository.creditcard.CreditCardOperatorRepository;
import org.moinex.repository.creditcard.CreditCardRepository;
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
@ContextConfiguration(classes = {AppConfig.class})
@ActiveProfiles("test")
class CreditCardDebtRepositoryTest {
    @Autowired private CreditCardDebtRepository creditCardDebtRepository;

    @Autowired private CreditCardRepository creditCardRepository;

    @Autowired private CreditCardOperatorRepository creditCardOperatorRepository;

    @Autowired private CategoryRepository categoryRepository;

    private CreditCard creditCard;
    private CreditCardOperator creditCardOperator;

    private CreditCard createCreditCard(
            String name, CreditCardOperator operator, BigDecimal maxDebt) {
        CreditCard ccd =
                CreditCard.builder()
                        .name(name)
                        .maxDebt(maxDebt)
                        .billingDueDay(10)
                        .closingDay(5)
                        .operator(operator)
                        .build();

        creditCardRepository.save(ccd);
        return ccd;
    }

    private CreditCardOperator createCreditCardOperator(String name) {
        CreditCardOperator ccdOperator = CreditCardOperator.builder().name(name).icon("").build();
        creditCardOperatorRepository.save(ccdOperator);
        return ccdOperator;
    }

    private Category createCategory(String name) {
        Category category = Category.builder().name(name).build();
        categoryRepository.save(category);
        return category;
    }

    private CreditCardDebt createCreditCardDebt(
            CreditCard creditCard, BigDecimal totalAmount, LocalDateTime date) {
        CreditCardDebt creditCardDebt =
                CreditCardDebt.builder()
                        .creditCard(creditCard)
                        .installments(1)
                        .amount(totalAmount)
                        .date(date)
                        .category(createCategory("category"))
                        .build();

        creditCardDebtRepository.save(creditCardDebt);
        return creditCardDebt;
    }

    @BeforeEach
    void setUp() {
        // Initialize the credit card
        creditCardOperator = createCreditCardOperator("Operator");
        creditCard = createCreditCard("CreditCard", creditCardOperator, new BigDecimal("1000.0"));
    }

    @Test
    void testNoDebt() {
        // No debt yet
        assertEquals(
                0.0,
                creditCardDebtRepository.getTotalDebt(creditCard.getId()).doubleValue(),
                Constants.EPSILON,
                "Total debt must be 0.0");
    }

    @Test
    void testSingleDebt() {
        createCreditCardDebt(
                creditCard, new BigDecimal("1000.0"), LocalDateTime.now().plusDays(10));

        assertEquals(
                1000.0,
                creditCardDebtRepository.getTotalDebt(creditCard.getId()).doubleValue(),
                Constants.EPSILON,
                "Total debt must be 1000.0");
    }

    @Test
    void testMultipleDebts() {
        createCreditCardDebt(
                creditCard, new BigDecimal("1000.0"), LocalDateTime.now().plusDays(10));

        createCreditCardDebt(creditCard, new BigDecimal("500.0"), LocalDateTime.now().plusDays(5));

        assertEquals(
                1500.0,
                creditCardDebtRepository.getTotalDebt(creditCard.getId()).doubleValue(),
                Constants.EPSILON,
                "Total debt must be 1500.0");
    }

    @Test
    void testDebtsForMultipleCreditCards() {
        CreditCard creditCard1 =
                createCreditCard("CreditCard1", creditCardOperator, new BigDecimal("1000.0"));

        CreditCard creditCard2 =
                createCreditCard("CreditCard2", creditCardOperator, new BigDecimal("2000.0"));

        createCreditCardDebt(
                creditCard1, new BigDecimal("1000.0"), LocalDateTime.now().plusDays(10));

        createCreditCardDebt(creditCard2, new BigDecimal("500.0"), LocalDateTime.now().plusDays(5));

        assertEquals(
                1000.0,
                creditCardDebtRepository.getTotalDebt(creditCard1.getId()).doubleValue(),
                Constants.EPSILON,
                "Total debt for CreditCard1 must be 1000.0");

        assertEquals(
                500.0,
                creditCardDebtRepository.getTotalDebt(creditCard2.getId()).doubleValue(),
                Constants.EPSILON,
                "Total debt for CreditCard2 must be 500.0");
    }
}
