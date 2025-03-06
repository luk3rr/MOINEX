/*
 * Filename: CreditCardService.java
 * Created on: September  4, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import lombok.NoArgsConstructor;
import org.moinex.entities.Category;
import org.moinex.entities.CreditCard;
import org.moinex.entities.CreditCardCredit;
import org.moinex.entities.CreditCardDebt;
import org.moinex.entities.CreditCardOperator;
import org.moinex.entities.CreditCardPayment;
import org.moinex.entities.Wallet;
import org.moinex.repositories.CategoryRepository;
import org.moinex.repositories.CreditCardCreditRepository;
import org.moinex.repositories.CreditCardDebtRepository;
import org.moinex.repositories.CreditCardOperatorRepository;
import org.moinex.repositories.CreditCardPaymentRepository;
import org.moinex.repositories.CreditCardRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.util.Constants;
import org.moinex.util.CreditCardCreditType;
import org.moinex.util.CreditCardInvoiceStatus;
import org.moinex.util.LoggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for the business logic Credit Card entities
 */
@Service
@NoArgsConstructor
public class CreditCardService
{
    @Autowired
    private CreditCardDebtRepository creditCardDebtRepository;

    @Autowired
    private CreditCardCreditRepository creditCardCreditRepository;

    @Autowired
    private CreditCardPaymentRepository creditCardPaymentRepository;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private CreditCardOperatorRepository creditCardOperatorRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private static final Logger logger = LoggerConfig.getLogger();

    /**
     * Creates a new credit card
     * @param name The name of the credit card
     * @param billingDueDay The day of the month the credit card bill is due
     * @param closingDay The day of the month the credit card bill is closed
     * @param maxDebt The maximum debt of the credit card
     * @throws RuntimeException If the credit card name is already in use
     * @throws RuntimeException If the billingDueDay is not in the range [1,
     *     Constants.MAX_BILLING_DUE_DAY]
     * @throws RuntimeException If the maxDebt is negative
     * @return The id of the created credit card
     */
    @Transactional
    public Long addCreditCard(String     name,
                              Integer    dueDate,
                              Integer    closingDay,
                              BigDecimal maxDebt,
                              String     lastFourDigits,
                              Long       operatorId)
    {
        return addCreditCard(name,
                             dueDate,
                             closingDay,
                             maxDebt,
                             lastFourDigits,
                             operatorId,
                             null);
    }

    /**
     * Creates a new credit card
     * @param name The name of the credit card
     * @param billingDueDay The day of the month the credit card bill is due
     * @param closingDay The day of the month the credit card bill is closed
     * @param maxDebt The maximum debt of the credit card
     * @throws RuntimeException If the credit card name is already in use
     * @throws RuntimeException If the billingDueDay is not in the range [1,
     *     Constants.MAX_BILLING_DUE_DAY]
     * @throws RuntimeException If the maxDebt is negative
     * @return The id of the created credit card
     */
    @Transactional
    public Long addCreditCard(String     name,
                              Integer    dueDate,
                              Integer    closingDay,
                              BigDecimal maxDebt,
                              String     lastFourDigits,
                              Long       operatorId,
                              Long       defaultBillingWalletId)
    {
        // Remove leading and trailing whitespaces
        name = name.strip();

        if (creditCardRepository.existsByName(name))
        {
            throw new RuntimeException("Credit card with name " + name +
                                       " already exists");
        }

        creditCardBasicChecks(name, dueDate, closingDay, maxDebt, lastFourDigits);

        CreditCardOperator operator =
            creditCardOperatorRepository.findById(operatorId)
                .orElseThrow(
                    ()
                        -> new RuntimeException("Credit card operator with id " +
                                                operatorId + " does not exist"));

        Wallet defaultBillingWallet =
            defaultBillingWalletId != null
                ? walletRepository.findById(defaultBillingWalletId)
                      .orElseThrow(()
                                       -> new RuntimeException("Wallet with id " +
                                                               defaultBillingWalletId +
                                                               " does not exist"))
                : null;

        CreditCard newCreditCard =
            creditCardRepository.save(CreditCard.builder()
                                          .name(name)
                                          .billingDueDay(dueDate)
                                          .closingDay(closingDay)
                                          .maxDebt(maxDebt)
                                          .lastFourDigits(lastFourDigits)
                                          .operator(operator)
                                          .defaultBillingWallet(defaultBillingWallet)
                                          .build());

        logger.info("Credit card " + name + " has created successfully");

        return newCreditCard.getId();
    }

    /**
     * Delete a credit card
     * @param id The id of the credit card
     * @throws RuntimeException If the credit card does not exist
     * @throws RuntimeException If the credit card has debts
     */
    @Transactional
    public void deleteCreditCard(Long id)
    {
        CreditCard creditCard = creditCardRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + id +
                                        " does not exist"));

        if (getDebtCountByCreditCard(id) > 0)
        {
            throw new RuntimeException("Credit card with id " + id +
                                       " has debts and cannot be deleted");
        }

        creditCardRepository.delete(creditCard);

        logger.info("Credit card with id " + id + " was permanently deleted");
    }

    /**
     * Update a credit card
     * @param crc The credit card to be updated
     * @throws RuntimeException If the credit card does not exist
     * @throws RuntimeException If the credit card name is empty
     * @throws RuntimeException If the billingDueDay is not in the range [1,
     *     Constants.MAX_BILLING_DUE_DAY]
     * @throws RuntimeException If the maxDebt is negative
     * @throws RuntimeException If the lastFourDigits is empty or has length different
     *     from 4
     */
    @Transactional
    public void updateCreditCard(CreditCard crc)
    {
        CreditCard oldCrc = creditCardRepository.findById(crc.getId())
                                .orElseThrow(()
                                                 -> new RuntimeException(
                                                     "Credit card with id " +
                                                     crc.getId() + " does not exist"));

        CreditCardOperator operator =
            creditCardOperatorRepository.findById(crc.getOperator().getId())
                .orElseThrow(()
                                 -> new RuntimeException(
                                     "Credit card operator with id " +
                                     crc.getOperator().getId() + " does not exist"));

        // Remove leading and trailing whitespaces
        crc.setName(crc.getName().strip());

        if (crc.getName().isBlank())
        {
            throw new RuntimeException("Credit card name cannot be empty");
        }

        creditCardBasicChecks(crc.getName(),
                              crc.getBillingDueDay(),
                              crc.getClosingDay(),
                              crc.getMaxDebt(),
                              crc.getLastFourDigits());

        List<CreditCard> creditCards = creditCardRepository.findAll();

        for (CreditCard creditCard : creditCards)
        {
            if (creditCard.getName().equals(crc.getName()) &&
                !creditCard.getId().equals(crc.getId()))
            {
                throw new RuntimeException("Credit card with name " + crc.getName() +
                                           " already exists");
            }
        }

        oldCrc.setName(crc.getName());

        oldCrc.setMaxDebt(crc.getMaxDebt());
        oldCrc.setLastFourDigits(crc.getLastFourDigits());
        oldCrc.setOperator(operator);
        oldCrc.setDefaultBillingWallet(crc.getDefaultBillingWallet());

        // Update bill due day for each pending payment where the payment date is
        // greater than the current day. Overdue payments are not updated
        List<CreditCardPayment> payments =
            getAllPendingCreditCardPayments(oldCrc.getId())
                .stream()
                .filter(p -> p.getDate().isAfter(LocalDateTime.now()))
                .toList();

        for (CreditCardPayment payment : payments)
        {
            payment.setDate(payment.getDate().withDayOfMonth(crc.getBillingDueDay()));
            creditCardPaymentRepository.save(payment);
        }

        oldCrc.setBillingDueDay(crc.getBillingDueDay());

        oldCrc.setClosingDay(crc.getClosingDay());

        creditCardRepository.save(oldCrc);

        logger.info("Credit card with id " + crc.getId() + " updated successfully");
    }

    /**
     * Register a debt on the credit card and its respective future payment
     * @param creditCardName The name of the credit card
     * @param category The category of the debt
     * @param registerDate The date the debt was registered
     * @param invoiceMonth The month of the invoice
     * @param value The value of the debt
     * @param installments The number of installments of the debt
     * @param description The description of the debt
     * @throws RuntimeException If the credit card does not exist
     * @throws RuntimeException If the category does not exist
     * @throws RuntimeException If the value is negative
     * @throws RuntimeException If the installments is not in range [1,
     *     Constants.MAX_INSTALLMENTS]
     * @throws RuntimeException If the credit card does not have enough credit
     */
    @Transactional
    public void addDebt(Long          crcId,
                        Category      category,
                        LocalDateTime registerDate,
                        YearMonth     invoiceMonth,
                        BigDecimal    value,
                        Integer       installments,
                        String        description)
    {
        CreditCard creditCard = creditCardRepository.findById(crcId).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + crcId +
                                        " does not exist"));

        Category cat =
            categoryRepository.findById(category.getId())
                .orElseThrow(()
                                 -> new RuntimeException("Category with name " +
                                                         category + " does not exist"));

        if (value == null)
        {
            throw new RuntimeException("Value cannot be null");
        }

        if (value.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new RuntimeException("Value must be non-negative");
        }

        if (installments < 1 || installments > Constants.MAX_INSTALLMENTS)
        {
            throw new RuntimeException("Installment must be in the range [1, " +
                                       Constants.MAX_INSTALLMENTS + "]");
        }

        if (registerDate == null)
        {
            throw new RuntimeException("Register date cannot be null");
        }

        if (invoiceMonth == null)
        {
            throw new RuntimeException("Invoice month cannot be null");
        }

        BigDecimal availableCredit = getAvailableCredit(crcId);

        if (value.compareTo(availableCredit) > 0)
        {
            throw new RuntimeException(
                "Credit card with id " + crcId +
                " does not have enough credit to register debt");
        }

        CreditCardDebt debt = CreditCardDebt.builder()
                                  .creditCard(creditCard)
                                  .category(cat)
                                  .date(registerDate)
                                  .amount(value)
                                  .installments(installments)
                                  .description(description)
                                  .build();

        creditCardDebtRepository.save(debt);

        logger.info("Debit registered on credit card with id " + crcId +
                    " with value " + value + " and description " + description);

        // Divide the value exactly, with full precision
        BigDecimal exactInstallmentValue =
            value.divide(new BigDecimal(installments), 2, RoundingMode.FLOOR);

        // Calculate the remainder
        BigDecimal remainder = value.subtract(
            exactInstallmentValue.multiply(new BigDecimal(installments)));

        // Iterate through the installments
        for (Integer i = 0; i < installments; i++)
        {
            // If there is a remainder, add it to the first installment
            BigDecimal currentInstallmentValue = exactInstallmentValue;
            if (remainder.compareTo(BigDecimal.ZERO) > 0 && i == 0)
            {
                currentInstallmentValue = currentInstallmentValue.add(remainder);
            }

            // Calculate the date and save the payment
            LocalDateTime paymentDate = invoiceMonth.plusMonths(i)
                                            .atDay(creditCard.getBillingDueDay())
                                            .atTime(23, 59);

            CreditCardPayment payment = CreditCardPayment.builder()
                                            .creditCardDebt(debt)
                                            .date(paymentDate)
                                            .amount(currentInstallmentValue)
                                            .installment(i + 1)
                                            .build();

            creditCardPaymentRepository.save(payment);

            logger.info("Payment of debt " + description + " on credit card with id " +
                        crcId + " registered with value " + currentInstallmentValue +
                        " and due date " + paymentDate);
        }
    }

    /**
     * Delete a debt
     * @param debtId The id of the debt
     * @throws RuntimeException If the debt does not exist
     */
    @Transactional
    public void deleteDebt(Long debtId)
    {
        CreditCardDebt debt = creditCardDebtRepository.findById(debtId).orElseThrow(
            () -> new RuntimeException("Debt with id " + debtId + " not found"));

        // Delete all payments associated with the debt
        List<CreditCardPayment> payments = getPaymentsByDebtId(debtId);

        for (CreditCardPayment payment : payments)
        {
            deletePayment(payment.getId());
        }

        creditCardDebtRepository.delete(debt);

        logger.info("Debt with id " + debtId + " deleted");
    }

    /**
     * Register a credit on the credit card
     * @param crcId The id of the credit card
     * @param date The date of the credit
     * @param amount The amount of the credit
     * @param type The type of the credit
     * @param description The description of the credit
     */
    @Transactional
    public void addCredit(Long                 crcId,
                          LocalDateTime        date,
                          BigDecimal           amount,
                          CreditCardCreditType type,
                          String               description)
    {
        CreditCard creditCard = creditCardRepository.findById(crcId).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + crcId +
                                        " does not exist"));

        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Amount must be greater than zero");
        }

        CreditCardCredit credit = CreditCardCredit.builder()
                                      .creditCard(creditCard)
                                      .amount(amount)
                                      .date(date)
                                      .description(description)
                                      .type(type)
                                      .build();

        // Update credit card available rebate
        creditCard.setAvailableRebate(creditCard.getAvailableRebate().add(amount));

        creditCardCreditRepository.save(credit);

        creditCardRepository.save(creditCard);

        logger.info("Credit of " + amount + " added to credit card with id " + crcId);
    }

    /**
     * Delete a credit
     * @param creditId The id of the credit
     * @throws RuntimeException If the credit does not exist
     */
    @Transactional
    public void deleteCredit(Long creditId)
    {
        CreditCardCredit credit =
            creditCardCreditRepository.findById(creditId).orElseThrow(
                ()
                    -> new RuntimeException("Credit with id " + creditId +
                                            " not found"));

        // creditCardCreditRepository.delete(credit);
        //  TODO: Tratar a exclusão de créditos

        logger.info("Credit with id " + creditId + " deleted");
    }

    /**
     * Archive a credit card
     * @param id The id of the credit card
     * @throws RuntimeException If the credit card does not exist
     * @throws RuntimeException If the credit card has pending payments
     */
    @Transactional
    public void archiveCreditCard(Long id)
    {
        CreditCard creditCard = creditCardRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + id +
                                        " not found and cannot be archived"));

        if (getTotalPendingPayments(id).compareTo(BigDecimal.ZERO) > 0)
        {
            throw new RuntimeException(
                "Credit card with id " + id +
                " has pending payments and cannot be archived");
        }

        creditCard.setIsArchived(true);
        creditCardRepository.save(creditCard);

        logger.info("Credit card with id " + id + " was archived");
    }

    /**
     * Unarchive a credit card
     * @param id The id of the credit card
     * @throws RuntimeException If the credit card does not exist
     */
    @Transactional
    public void unarchiveCreditCard(Long id)
    {
        CreditCard creditCard = creditCardRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + id +
                                        " not found and cannot be unarchived"));

        creditCard.setIsArchived(false);
        creditCardRepository.save(creditCard);

        logger.info("Credit card with id " + id + " was unarchived");
    }

    /**
     * Update the debt of a credit card
     * @param debt The debt to be updated
     * @param invoiceMonth The month of the invoice
     * @throws RuntimeException If the debt does not exist
     * @throws RuntimeException If the credit card does not exist
     * @throws RuntimeException If the total amount of the debt is less than or equal to
     *     zero
     */
    @Transactional
    public void updateCreditCardDebt(CreditCardDebt debt, YearMonth invoiceMonth)
    {
        CreditCardDebt oldDebt =
            creditCardDebtRepository.findById(debt.getId())
                .orElseThrow(()
                                 -> new RuntimeException("Debt with id " +
                                                         debt.getId() +
                                                         " does not exist"));

        creditCardRepository.findById(debt.getCreditCard().getId())
            .orElseThrow(()
                             -> new RuntimeException("Credit card with id " +
                                                     debt.getCreditCard().getId() +
                                                     " does not exist"));

        if (debt.getAmount().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Total amount must be greater than zero");
        }

        // Complex update
        changeInvoiceMonth(oldDebt, invoiceMonth);
        changeDebtTotalAmount(oldDebt, debt.getAmount());
        changeDebtInstallments(oldDebt, debt.getInstallments());

        // Trivial update
        oldDebt.setCreditCard(debt.getCreditCard());
        oldDebt.setCategory(debt.getCategory());
        oldDebt.setDescription(debt.getDescription());

        creditCardDebtRepository.save(oldDebt);

        logger.info("Debt with id " + debt.getId() + " updated successfully");
    }

    /**
     * Update the credit of a credit card
     * @param credit The credit to be updated
     */
    @Transactional
    public void updateCreditCardCredit(CreditCardCredit credit)
    {
        CreditCardCredit oldCredit =
            creditCardCreditRepository.findById(credit.getId())
                .orElseThrow(()
                                 -> new RuntimeException("Credit with id " +
                                                         credit.getId() +
                                                         " does not exist"));

        // TODO: Tratar a atualização de créditos
        credit.setCreditCard(oldCredit.getCreditCard());
        creditCardCreditRepository.save(credit);

        logger.info("Credit with id " + credit.getId() + " updated successfully");
    }

    /**
     * Pay a credit card invoice
     * @param crcId The id of the credit card to pay the invoice
     * @param walletId The id of the wallet to register the payment
     * @param month The month of the invoice
     * @param year The year of the invoice
     * @param rebate The rebate amount
     * @throws RuntimeException If the credit card does not exist
     * @throws RuntimeException If the wallet does not exist
     */
    @Transactional
    public void payInvoice(Long       crcId,
                           Long       walletId,
                           Integer    month,
                           Integer    year,
                           BigDecimal rebate)
    {
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(
            ()
                -> new RuntimeException("Wallet with id " + walletId +
                                        " does not exist"));

        CreditCard creditCard = creditCardRepository.findById(crcId).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + crcId +
                                        " does not exist"));

        if (rebate.compareTo(BigDecimal.ZERO) < 0)
        {
            throw new RuntimeException("Rebate must be non-negative");
        }

        if (creditCard.getAvailableRebate().compareTo(rebate) < 0)
        {
            throw new RuntimeException(
                "Credit card with id " + crcId +
                " does not have enough rebate to pay the invoice");
        }

        List<CreditCardPayment> pendingPayments =
            getPendingCreditCardPayments(crcId, month, year);

        BigDecimal pendingPaymentsTotal = pendingPayments.stream()
                                              .map(CreditCardPayment::getAmount)
                                              .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingPaymentsTotalWithRebate =
            pendingPaymentsTotal.subtract(rebate);

        // If the discount value is greater than the total of pending payments, then use
        // only the necessary to pay the pending payments
        if (pendingPaymentsTotalWithRebate.compareTo(BigDecimal.ZERO) < 0)
        {
            pendingPaymentsTotalWithRebate = BigDecimal.ZERO;
            rebate = pendingPaymentsTotal;
        }

        for (CreditCardPayment payment : pendingPayments)
        {
            payment.setWallet(wallet);
            creditCardPaymentRepository.save(payment);

            logger.info("Payment number " + payment.getInstallment() +
                        " of debt with id " + payment.getCreditCardDebt().getId() +
                        " on credit card with id " +
                        payment.getCreditCardDebt().getCreditCard().getId() + " paid");
        }

        // Subtract the total of pending payments from the wallet balance
        wallet.setBalance(wallet.getBalance().subtract(pendingPaymentsTotalWithRebate));
        walletRepository.save(wallet);

        creditCard.setAvailableRebate(creditCard.getAvailableRebate().subtract(rebate));
        creditCardRepository.save(creditCard);
    }

    /**
     * Pay a credit card invoice
     * @param crcId The id of the credit card to pay the invoice
     * @param walletId The id of the wallet to register the payment
     * @param month The month of the invoice
     * @param year The year of the invoice
     * @throws RuntimeException If the credit card does not exist
     * @throws RuntimeException If the wallet does not exist
     */
    @Transactional
    public void payInvoice(Long crcId, Long walletId, Integer month, Integer year)
    {
        payInvoice(crcId, walletId, month, year, BigDecimal.ZERO);
    }

    /**
     * Get all credit cards
     * @return A list with all credit cards
     */
    public List<CreditCard> getAllCreditCards()
    {
        return creditCardRepository.findAll();
    }

    /**
     * Get all archived credit cards
     * @return A list with all archived credit cards
     */
    public List<CreditCard> getAllArchivedCreditCards()
    {
        return creditCardRepository.findAllByIsArchivedTrue();
    }

    /**
     * Get all credit cards ordered by name
     * @return A list with all credit cards ordered by name
     */
    public List<CreditCard> getAllCreditCardsOrderedByName()
    {
        return creditCardRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get all credit cards are not archived ordered by name
     * @return A list with all credit cards that are not archived ordered by name
     */
    public List<CreditCard> getAllNonArchivedCreditCardsOrderedByName()
    {
        return creditCardRepository.findAllByIsArchivedFalseOrderByNameAsc();
    }

    /**
     * Get all credit cards are not archived ordered descending by the number of
     * transactions
     * @return A list with all credit cards that are not archived ordered by transaction
     *    count
     */
    public List<CreditCard> getAllNonArchivedCreditCardsOrderedByTransactionCountDesc()
    {
        return creditCardRepository.findAllByIsArchivedFalse()
            .stream()
            .sorted(Comparator
                        .comparingLong(
                            (CreditCard c)
                                -> creditCardDebtRepository.getDebtCountByCreditCard(
                                    c.getId()))
                        .reversed())
            .toList();
    }

    /**
     * Get all credit card operators ordered by name
     * @return A list with all credit card operators ordered by name
     */
    public List<CreditCardOperator> getAllCreditCardOperatorsOrderedByName()
    {
        return creditCardOperatorRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get available credit of a credit card
     * @param id The id of the credit card
     * @return The available credit of the credit card
     * @throws RuntimeException If the credit card does not exist
     */
    public BigDecimal getAvailableCredit(Long id)
    {
        CreditCard creditCard = creditCardRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + id +
                                        " does not exist"));

        BigDecimal totalPendingPayments =
            creditCardPaymentRepository.getTotalPendingPayments(id);

        return creditCard.getMaxDebt().subtract(totalPendingPayments);
    }

    /**
     * Get credit card payments in a month and year
     * @param month The month
     * @param year The year
     * @return A list with all credit card payments in a month and year
     */
    public List<CreditCardPayment> getCreditCardPayments(Integer month, Integer year)
    {
        return creditCardPaymentRepository.getCreditCardPayments(month, year);
    }

    /**
     * Get credit card payments in a month and year by credit card id
     * @param crcId The id of the credit card
     * @param month The month
     * @param year The year
     * @return A list with all credit card payments in a month and year by credit card
     *     id
     */
    public List<CreditCardPayment>
    getCreditCardPayments(Long crcId, Integer month, Integer year)
    {
        return creditCardPaymentRepository.getCreditCardPayments(crcId, month, year);
    }

    /**
     * Get credit card pending payments in a month and year by credit card id
     * @param crcId The id of the credit card
     * @param month The month
     * @param year The year
     * @return A list with all credit card pending payments in a month and year by
     *     credit card id
     */
    public List<CreditCardPayment>
    getPendingCreditCardPayments(Long crcId, Integer month, Integer year)
    {
        return creditCardPaymentRepository.getPendingCreditCardPayments(crcId,
                                                                        month,
                                                                        year);
    }

    /**
     * Get all pending credit card payments
     * @param crcId The id of the credit card
     * @return A list with all pending credit card payments
     */
    public List<CreditCardPayment> getAllPendingCreditCardPayments(Long crcId)
    {
        return creditCardPaymentRepository.getAllPendingCreditCardPayments(crcId);
    }

    /**
     * Get payments by debt id
     * @param debtId The debt id
     * @return A list with all credit card payments by debt id
     */
    public List<CreditCardPayment> getPaymentsByDebtId(Long debtId)
    {
        return creditCardPaymentRepository.getPaymentsByDebtId(debtId);
    }

    /**
     * Get all paid payments of all credit cards in a month and year
     * @param month The month
     * @param year The year
     * @return A list with all paid payments of all credit cards in a month and year
     */
    public List<CreditCardPayment> getAllPaidPaymentsByMonth(Integer month,
                                                             Integer year)
    {
        return creditCardPaymentRepository.getAllPaidPaymentsByMonth(month, year);
    }

    /**
     * Get the total debt amount of all credit cards in a month and year
     * @param month The month
     * @param year The year
     * @return The total debt amount of all credit cards in a month and year
     */
    public BigDecimal getTotalDebtAmount(Integer month, Integer year)
    {
        return creditCardPaymentRepository.getTotalDebtAmount(month, year);
    }

    /**
     * Get the total debt amount of all credit cards in a year
     * @param year The year
     * @return The total debt amount of all credit cards in a year
     */
    public BigDecimal getTotalDebtAmount(Integer year)
    {
        return creditCardPaymentRepository.getTotalDebtAmount(year);
    }

    /**
     * Get the total of all pending payments of all credit cards from a specified month
     * and year onward, including future months and the current month
     * @param month The starting month (inclusive)
     * @param year The starting year (inclusive)
     * @return The total of all pending payments of all credit cards from the specified
     *     month and year onward
     */
    public BigDecimal getTotalPendingPayments(Integer month, Integer year)
    {
        return creditCardPaymentRepository.getTotalPendingPayments(month, year);
    }

    /**
     * Get the total of all paid payments of all credit cards from a specified month
     * and year
     * @param month The month
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *   month and year
     */
    public BigDecimal getPaidPaymentsByMonth(Integer month, Integer year)
    {
        return creditCardPaymentRepository.getPaidPaymentsByMonth(month, year);
    }

    /**
     * Get the total of all paid payments of all credit cards from a specified month and
     * year by a wallet
     * @param walletId The wallet id
     * @param month The month
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *   month and year by a wallet
     */
    public BigDecimal getPaidPaymentsByMonth(Long walletId, Integer month, Integer year)
    {
        return creditCardPaymentRepository.getPaidPaymentsByMonth(walletId,
                                                                  month,
                                                                  year);
    }

    /**
     * Get the total of all pending payments of all credit cards from a specified month
     * and year
     * @param month The month
     * @param year The year
     * @return The total of all pending payments of all credit cards from the specified
     *    month and year
     */
    public BigDecimal getPendingPaymentsByMonth(Integer month, Integer year)
    {
        return creditCardPaymentRepository.getPendingPaymentsByMonth(month, year);
    }

    /**
     * Get the total of all pending payments of all credit cards from a specified year
     * onward, including future years and the current year
     * @param year The starting year (inclusive)
     * @return The total of all pending payments of all credit cards from the specified
     *    year onward
     */
    public BigDecimal getTotalPendingPayments(Integer year)
    {
        return creditCardPaymentRepository.getTotalPendingPayments(year);
    }

    /**
     * Get the total of all paid payments of all credit cards from a specified year
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *     year
     */
    public BigDecimal getPaidPaymentsByYear(Integer year)
    {
        return creditCardPaymentRepository.getPaidPaymentsByYear(year);
    }

    /**
     * Get the total of all pending payments of all credit cards from a specified year
     * @param year The year
     * @return The total of all pending payments of all credit cards from the specified
     *     year
     */
    public BigDecimal getPendingPaymentsByYear(Integer year)
    {
        return creditCardPaymentRepository.getPendingPaymentsByYear(year);
    }

    /**
     * Get the total of all pending payments of a credit card
     * @return The total of all pending payments of all credit cards
     */
    public BigDecimal getTotalPendingPayments(Long crcId)
    {
        return creditCardPaymentRepository.getTotalPendingPayments(crcId);
    }

    /**
     * Get the total of all pending payments of all credit cards
     * @return The total of all pending payments of all credit cards
     */
    public BigDecimal getTotalPendingPayments()
    {
        return creditCardPaymentRepository.getTotalPendingPayments();
    }

    /**
     * Get the remaining debt of a purchase
     * @param debtId The id of the debt
     * @return The remaining debt of the purchase
     */
    public BigDecimal getRemainingDebt(Long debtId)
    {
        return creditCardPaymentRepository.getRemainingDebt(debtId);
    }

    /**
     * Get the invoice amount of a credit card in a specified month and year
     * @param creditCardId The credit card id
     * @param month The month
     * @param year The year
     * @return The invoice amount of the credit card in the specified month and year
     */
    public BigDecimal getInvoiceAmount(Long crcId, Integer month, Integer year)
    {
        return creditCardPaymentRepository.getInvoiceAmount(crcId, month, year);
    }

    /**
     * Get credits card credits in a month and year
     * @param crcId The id of the credit card
     * @param month The month
     * @param year The year
     */
    public List<CreditCardCredit>
    getCreditCardCreditsByMonth(Long crcId, Integer month, Integer year)
    {
        return creditCardCreditRepository.findCreditCardCreditsByMonth(crcId,
                                                                       month,
                                                                       year);
    }

    /**
     * Get the total of all credit card credits in a month and year
     * @param crcId The id of the credit card
     * @param month The month
     * @param year The year
     * @return The total of all credit card credits in a month and year
     */
    public BigDecimal
    getTotalCreditCardCreditsByMonth(Long crcId, Integer month, Integer year)
    {
        return creditCardCreditRepository.getTotalCreditCardCreditsByMonth(crcId,
                                                                           month,
                                                                           year);
    }

    /**
     * Get all credit card credits
     * @return A list with all credit card credits
     */
    public List<CreditCardCredit> getAllCreditCardCredits()
    {
        return creditCardCreditRepository.findAll();
    }

    /**
     * Get the invoice status of a credit card in a specified month and year
     * The invoice status can be either 'Open' or 'Closed'
     * @param creditCardId The credit card id
     * @param month The month
     * @param year The year
     * @return The invoice status of the credit card in the specified month and year
     * @throws RuntimeException If the credit card does not exist
     */
    public CreditCardInvoiceStatus
    getInvoiceStatus(Long crcId, Integer month, Integer year)
    {
        LocalDateTime nextInvoiceDate = getNextInvoiceDate(crcId);
        nextInvoiceDate               = nextInvoiceDate.withHour(0).withMinute(0);

        LocalDateTime dateToCompare =
            LocalDateTime.of(year, month, nextInvoiceDate.getDayOfMonth(), 23, 59);

        if (dateToCompare.isAfter(nextInvoiceDate) ||
            dateToCompare.isEqual(nextInvoiceDate))
        {
            return CreditCardInvoiceStatus.OPEN;
        }

        return CreditCardInvoiceStatus.CLOSED;
    }

    /**
     * Get next invoice date of a credit card
     * @param crcId The id of the credit card
     * @return The next invoice date of the credit card
     * @throws RuntimeException If the credit card does not exist
     */
    public LocalDateTime getNextInvoiceDate(Long crcId)
    {
        String nextInvoiceDate = creditCardPaymentRepository.getNextInvoiceDate(crcId);

        // If there is no next invoice date, calculate it
        // If the current day is greater than the closing day, the next invoice date is
        // billingDueDay of the next month
        // Otherwise, the next invoice date is billingDueDay of the current month
        if (nextInvoiceDate == null)
        {
            LocalDateTime now = LocalDateTime.now();

            CreditCard creditCard = creditCardRepository.findById(crcId).orElseThrow(
                ()
                    -> new RuntimeException("Credit card with id " + crcId +
                                            " does not exist"));

            Integer currentDay = now.getDayOfMonth();
            Integer closingDay = creditCard.getClosingDay();

            if (currentDay > closingDay)
            {
                return now.plusMonths(1).withDayOfMonth(creditCard.getBillingDueDay());
            }

            return now.withDayOfMonth(creditCard.getBillingDueDay());
        }

        return LocalDateTime.parse(nextInvoiceDate, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the date of the latest payment
     * @return The date of the latest payment or the current date if there are no debts
     */
    public LocalDateTime getEarliestPaymentDate()
    {
        String date = creditCardDebtRepository.findEarliestPaymentDate();

        if (date == null)
        {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the date of the latest payment
     * @return The date of the latest payment or the current date if there are no debts
     */
    public LocalDateTime getLatestPaymentDate()
    {
        String date = creditCardDebtRepository.findLatestPaymentDate();

        if (date == null)
        {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get count of debts by credit card
     * @param id The id of the credit card
     * @return The count of debts by credit card
     */
    public Long getDebtCountByCreditCard(Long id)
    {

        return creditCardDebtRepository.getDebtCountByCreditCard(id);
    }

    /**
     * Get credit card debt suggestions
     * @return A list with credit card debt suggestions
     */
    public List<CreditCardDebt> getCreditCardDebtSuggestions()
    {
        return creditCardDebtRepository.findSuggestions();
    }

    /**
     * Get credit card credit suggestions
     * @return A list with credit card credit suggestions
     */
    public List<CreditCardCredit> getCreditCardCreditSuggestions()
    {
        return creditCardCreditRepository.findSuggestions();
    }

    /**
     * Basic checks for credit card creation or update
     * @param name The name of the credit card
     * @param dueDate The day of the month the credit card bill is due
     * @param closingDay The day of the month the credit card bill is closed
     * @param maxDebt The maximum debt of the credit card
     * @param lastFourDigits The last four digits of the credit card
     * @throws RuntimeException If the credit card name is empty
     * @throws RuntimeException If the credit card name is already in use
     * @throws RuntimeException If the billingDueDay is not in the range [1,
     *     Constants.MAX_BILLING_DUE_DAY]
     * @throws RuntimeException If the maxDebt is negative
     * @throws RuntimeException If the lastFourDigits is empty or has length different
     *     from 4
     */
    private void creditCardBasicChecks(String     name,
                                       Integer    dueDate,
                                       Integer    closingDay,
                                       BigDecimal maxDebt,
                                       String     lastFourDigits)
    {
        if (name.isBlank())
        {
            throw new RuntimeException("Credit card name cannot be empty");
        }

        if (dueDate < 1 || dueDate > Constants.MAX_BILLING_DUE_DAY)
        {
            throw new RuntimeException("Billing due day must be in the range [1, " +
                                       Constants.MAX_BILLING_DUE_DAY + "]");
        }

        if (closingDay < 1 || closingDay > Constants.MAX_BILLING_DUE_DAY)
        {
            throw new RuntimeException("Closing day must be in the range [1, " +
                                       Constants.MAX_BILLING_DUE_DAY + "]");
        }

        if (maxDebt.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Max debt must be positive");
        }

        if (lastFourDigits.isBlank() || lastFourDigits.length() != 4)
        {
            throw new RuntimeException("Last four digits must have length 4");
        }
    }

    /**
     * Delete a payment of a debt
     * @param id The id of the payment
     * @throws RuntimeException If the payment does not exist
     * @note WARNING: The data in CreditCardDebt is not updated when a payment is
     *   deleted
     */
    private void deletePayment(Long id)
    {
        CreditCardPayment payment =
            creditCardPaymentRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Payment with id " + id + " not found"));

        // If payment was made with a wallet, add the amount back to the
        // wallet balance
        if (payment.getWallet() != null)
        {
            payment.getWallet().setBalance(
                payment.getWallet().getBalance().add(payment.getAmount()));

            logger.info("Payment number " + payment.getInstallment() +
                        " of debt with id " + payment.getCreditCardDebt().getId() +
                        " on credit card with id " +
                        payment.getCreditCardDebt().getCreditCard().getId() +
                        " deleted and added to wallet with id " +
                        payment.getWallet().getId());

            walletRepository.save(payment.getWallet());
        }

        creditCardPaymentRepository.delete(payment);

        logger.info("Payment number " + payment.getInstallment() + " of debt with id " +
                    payment.getCreditCardDebt().getId() + " on credit card with id " +
                    payment.getCreditCardDebt().getCreditCard().getId() + " deleted");
    }

    /**
     * Update invoice month of a debt
     * @param debt The debt to be updated
     * @param invoiceMonth The new invoice month
     */
    private void changeInvoiceMonth(CreditCardDebt oldDebt, YearMonth invoice)
    {
        List<CreditCardPayment> payments = getPaymentsByDebtId(oldDebt.getId());

        CreditCardPayment firstPayment = payments.getFirst();

        // If the first payment is in the same month and year of the invoice, do not
        // update
        if (firstPayment == null ||
            (firstPayment.getDate().getMonth() == invoice.getMonth() &&
             firstPayment.getDate().getYear() == invoice.getYear()))
        {
            return;
        }

        for (Integer i = 0; i < oldDebt.getInstallments(); i++)
        {
            CreditCardPayment payment = payments.get(i);

            // Calculate the payment date
            LocalDateTime paymentDate =
                invoice.plusMonths(i)
                    .atDay(oldDebt.getCreditCard().getBillingDueDay())
                    .atTime(23, 59);

            payment.setDate(paymentDate);
            creditCardPaymentRepository.save(payment);

            logger.info("Payment number " + payment.getInstallment() +
                        " of debt with id " + oldDebt.getId() +
                        " on credit card with id " + oldDebt.getCreditCard().getId() +
                        " updated with due date " + paymentDate);
        }
    }

    /**
     * Change the number of installments of a debt
     * @param debt The debt to be updated
     * @param newInstallments The new number of installments
     */
    private void changeDebtInstallments(CreditCardDebt oldDebt, Integer newInstallments)
    {
        if (oldDebt.getInstallments() == newInstallments)
        {
            return;
        }

        List<CreditCardPayment> payments = getPaymentsByDebtId(oldDebt.getId());

        BigDecimal value = oldDebt.getAmount();

        // Calculate the new installment value
        // If the division is not exact, the first installment absorbs the remainder
        BigDecimal installmentValue =
            value.divide(new BigDecimal(newInstallments), 2, RoundingMode.HALF_UP);

        BigDecimal totalCalculated = installmentValue.multiply(
            new BigDecimal(newInstallments)); // Total without remainder

        BigDecimal remainder = value.subtract(totalCalculated);

        // First installment absorbs the remainder
        BigDecimal firstInstallment = installmentValue.add(remainder);

        // Delete and update payments
        if (newInstallments < oldDebt.getInstallments())
        {
            for (Integer i = 0; i < oldDebt.getInstallments(); i++)
            {
                CreditCardPayment payment = payments.get(i);

                // If the payment is greater than the new number of installments, delete
                // it
                if (payment.getInstallment() > newInstallments)
                {
                    deletePayment(payment.getId());
                }
                // If the payment is less or equal than the new number of installments,
                // update it
                else
                {
                    payment.setAmount(i == 0 ? firstInstallment : installmentValue);
                    creditCardPaymentRepository.save(payment);

                    logger.info("Payment number " + payment.getInstallment() +
                                " of debt with id " + oldDebt.getId() +
                                " on credit card with id " +
                                oldDebt.getCreditCard().getId() +
                                " updated with value " +
                                (i == 0 ? firstInstallment : installmentValue));
                }
            }
        }
        else // Insert and update payments
        {
            for (Integer i = 1; i <= newInstallments; i++)
            {
                if (i > oldDebt.getInstallments())
                {
                    CreditCardPayment lastPayment = payments.getLast();

                    // Calculate the payment date
                    LocalDateTime paymentDate = lastPayment.getDate().plusMonths(1);

                    CreditCardPayment payment = CreditCardPayment.builder()
                                                    .creditCardDebt(oldDebt)
                                                    .date(paymentDate)
                                                    .amount(installmentValue)
                                                    .installment(i)
                                                    .build();

                    creditCardPaymentRepository.save(payment);

                    logger.info("Payment number " + i + " of debt with id " +
                                oldDebt.getId() + " on credit card with id " +
                                oldDebt.getCreditCard().getId() +
                                " registered with value " + installmentValue +
                                " and due date " + paymentDate);

                    // Add new payment to the list
                    payments.add(payment);
                }
                else
                {
                    CreditCardPayment payment = payments.get(i - 1);

                    payment.setAmount(i == 0 ? firstInstallment : installmentValue);
                    creditCardPaymentRepository.save(payment);

                    logger.info("Payment number " + payment.getInstallment() +
                                " of debt with id " + oldDebt.getId() +
                                " on credit card with id " +
                                oldDebt.getCreditCard().getId() +
                                " updated with value " +
                                (i == 0 ? firstInstallment : installmentValue));
                }
            }
        }

        // Update the number of installments
        oldDebt.setInstallments(newInstallments);
        creditCardDebtRepository.save(oldDebt);
    }

    /**
     * Change the total amount of a debt
     * @param oldDebt The debt to be updated
     * @param newAmount The new total amount
     */
    private void changeDebtTotalAmount(CreditCardDebt oldDebt, BigDecimal newAmount)
    {
        if (oldDebt.getAmount().equals(newAmount))
        {
            return;
        }

        List<CreditCardPayment> payments = getPaymentsByDebtId(oldDebt.getId());

        Integer installments = oldDebt.getInstallments();

        // Divide the value exactly, with full precision
        BigDecimal exactInstallmentValue =
            newAmount.divide(new BigDecimal(installments), 2, RoundingMode.FLOOR);

        // Calculate the remainder
        BigDecimal remainder = newAmount.subtract(
            exactInstallmentValue.multiply(new BigDecimal(installments)));

        // Update payments
        for (Integer i = 0; i < oldDebt.getInstallments(); i++)
        {
            CreditCardPayment payment = payments.get(i);

            // If there is a remainder, add it to the first installment
            BigDecimal currentInstallmentValue = exactInstallmentValue;
            if (remainder.compareTo(BigDecimal.ZERO) > 0 && i == 0)
            {
                currentInstallmentValue = currentInstallmentValue.add(remainder);
            }

            // If the payment was made with a wallet, add the amount difference back to
            // the wallet balance
            if (payment.getWallet() != null)
            {
                BigDecimal diff = currentInstallmentValue.subtract(payment.getAmount());

                payment.getWallet().setBalance(
                    payment.getWallet().getBalance().add(diff));

                logger.info("Payment number " + payment.getInstallment() +
                            " of debt with id " + oldDebt.getId() +
                            " on credit card with id " +
                            oldDebt.getCreditCard().getId() +
                            " updated and added to wallet with id " +
                            payment.getWallet().getId());

                walletRepository.save(payment.getWallet());
            }

            payment.setAmount(currentInstallmentValue);
            creditCardPaymentRepository.save(payment);

            logger.info("Payment number " + payment.getInstallment() +
                        " of debt with id " + oldDebt.getId() +
                        " on credit card with id " + oldDebt.getCreditCard().getId() +
                        " updated with value " + currentInstallmentValue);
        }

        // Update the total amount
        oldDebt.setAmount(newAmount);
        creditCardDebtRepository.save(oldDebt);
    }
}
