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
import org.moinex.entities.Category;
import org.moinex.entities.CreditCard;
import org.moinex.entities.CreditCardDebt;
import org.moinex.entities.CreditCardOperator;
import org.moinex.entities.CreditCardPayment;
import org.moinex.entities.Wallet;
import org.moinex.repositories.CategoryRepository;
import org.moinex.repositories.CreditCardDebtRepository;
import org.moinex.repositories.CreditCardOperatorRepository;
import org.moinex.repositories.CreditCardPaymentRepository;
import org.moinex.repositories.CreditCardRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.util.Constants;
import org.moinex.util.CreditCardInvoiceStatus;
import org.moinex.util.LoggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for the business logic Credit Card entities
 */
@Service
public class CreditCardService
{
    @Autowired
    private CreditCardDebtRepository m_creditCardDebtRepository;

    @Autowired
    private CreditCardPaymentRepository m_creditCardPaymentRepository;

    @Autowired
    private CreditCardRepository m_creditCardRepository;

    @Autowired
    private CreditCardOperatorRepository m_creditCardOperatorRepository;

    @Autowired
    private WalletRepository m_walletRepository;

    @Autowired
    private CategoryRepository m_categoryRepository;

    private static final Logger m_logger = LoggerConfig.GetLogger();

    public CreditCardService() { }

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
    public Long CreateCreditCard(String     name,
                                 Integer    dueDate,
                                 Integer    closingDay,
                                 BigDecimal maxDebt,
                                 String     lastFourDigits,
                                 Long       operatorId)
    {
        return CreateCreditCard(name,
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
    public Long CreateCreditCard(String     name,
                                 Integer    dueDate,
                                 Integer    closingDay,
                                 BigDecimal maxDebt,
                                 String     lastFourDigits,
                                 Long       operatorId,
                                 Long       defaultBillingWalletId)
    {
        // Remove leading and trailing whitespaces
        name = name.strip();

        if (m_creditCardRepository.existsByName(name))
        {
            throw new RuntimeException("Credit card with name " + name +
                                       " already exists");
        }

        CreditCardBasicChecks(name, dueDate, closingDay, maxDebt, lastFourDigits);

        CreditCardOperator operator =
            m_creditCardOperatorRepository.findById(operatorId)
                .orElseThrow(
                    ()
                        -> new RuntimeException("Credit card operator with id " +
                                                operatorId + " does not exist"));

        Wallet defaultBillingWallet =
            defaultBillingWalletId != null
                ? m_walletRepository.findById(defaultBillingWalletId)
                      .orElseThrow(()
                                       -> new RuntimeException("Wallet with id " +
                                                               defaultBillingWalletId +
                                                               " does not exist"))
                : null;

        CreditCard newCreditCard =
            m_creditCardRepository.save(new CreditCard(name,
                                                       dueDate,
                                                       closingDay,
                                                       maxDebt,
                                                       lastFourDigits,
                                                       operator,
                                                       defaultBillingWallet));

        m_logger.info("Credit card " + name + " has created successfully");

        return newCreditCard.GetId();
    }

    /**
     * Delete a credit card
     * @param id The id of the credit card
     * @throws RuntimeException If the credit card does not exist
     * @throws RuntimeException If the credit card has debts
     */
    @Transactional
    public void DeleteCreditCard(Long id)
    {
        CreditCard creditCard = m_creditCardRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + id +
                                        " does not exist"));

        if (GetDebtCountByCreditCard(id) > 0)
        {
            throw new RuntimeException("Credit card with id " + id +
                                       " has debts and cannot be deleted");
        }

        m_creditCardRepository.delete(creditCard);

        m_logger.info("Credit card with id " + id + " was permanently deleted");
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
    public void UpdateCreditCard(CreditCard crc)
    {
        CreditCard oldCrc = m_creditCardRepository.findById(crc.GetId())
                                .orElseThrow(()
                                                 -> new RuntimeException(
                                                     "Credit card with id " +
                                                     crc.GetId() + " does not exist"));

        CreditCardOperator operator =
            m_creditCardOperatorRepository.findById(crc.GetOperator().GetId())
                .orElseThrow(()
                                 -> new RuntimeException(
                                     "Credit card operator with id " +
                                     crc.GetOperator().GetId() + " does not exist"));

        // Remove leading and trailing whitespaces
        crc.SetName(crc.GetName().strip());

        if (crc.GetName().isBlank())
        {
            throw new RuntimeException("Credit card name cannot be empty");
        }

        CreditCardBasicChecks(crc.GetName(),
                              crc.GetBillingDueDay(),
                              crc.GetClosingDay(),
                              crc.GetMaxDebt(),
                              crc.GetLastFourDigits());

        List<CreditCard> creditCards = m_creditCardRepository.findAll();

        for (CreditCard creditCard : creditCards)
        {
            if (creditCard.GetName().equals(crc.GetName()) &&
                !creditCard.GetId().equals(crc.GetId()))
            {
                throw new RuntimeException("Credit card with name " + crc.GetName() +
                                           " already exists");
            }
        }

        oldCrc.SetName(crc.GetName());

        oldCrc.SetMaxDebt(crc.GetMaxDebt());
        oldCrc.SetLastFourDigits(crc.GetLastFourDigits());
        oldCrc.SetOperator(operator);
        oldCrc.SetDefaultBillingWallet(crc.GetDefaultBillingWallet());

        // Update bill due day for each pending payment where the payment date is
        // greater than the current day. Overdue payments are not updated
        List<CreditCardPayment> payments =
            GetAllPendingCreditCardPayments(oldCrc.GetId())
                .stream()
                .filter(p -> p.GetDate().isAfter(LocalDateTime.now()))
                .toList();

        for (CreditCardPayment payment : payments)
        {
            payment.SetDate(payment.GetDate().withDayOfMonth(crc.GetBillingDueDay()));
            m_creditCardPaymentRepository.save(payment);
        }

        oldCrc.SetBillingDueDay(crc.GetBillingDueDay());

        oldCrc.SetClosingDay(crc.GetClosingDay());

        m_creditCardRepository.save(oldCrc);

        m_logger.info("Credit card with id " + crc.GetId() + " updated successfully");
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
    public void RegisterDebt(Long          crcId,
                             Category      category,
                             LocalDateTime registerDate,
                             YearMonth     invoiceMonth,
                             BigDecimal    value,
                             Integer       installments,
                             String        description)
    {
        CreditCard creditCard = m_creditCardRepository.findById(crcId).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + crcId +
                                        " does not exist"));

        Category cat =
            m_categoryRepository.findById(category.GetId())
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

        BigDecimal availableCredit = GetAvailableCredit(crcId);

        if (value.compareTo(availableCredit) > 0)
        {
            throw new RuntimeException(
                "Credit card with id " + crcId +
                " does not have enough credit to register debt");
        }

        CreditCardDebt debt = new CreditCardDebt(creditCard,
                                                 cat,
                                                 registerDate,
                                                 installments,
                                                 value,
                                                 description);

        m_creditCardDebtRepository.save(debt);

        m_logger.info("Debit registered on credit card with id " + crcId +
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
                                            .atDay(creditCard.GetBillingDueDay())
                                            .atTime(23, 59);

            CreditCardPayment payment = new CreditCardPayment(debt,
                                                              paymentDate,
                                                              currentInstallmentValue,
                                                              i + 1);

            m_creditCardPaymentRepository.save(payment);

            m_logger.info("Payment of debt " + description +
                          " on credit card with id " + crcId +
                          " registered with value " + currentInstallmentValue +
                          " and due date " + paymentDate);
        }
    }

    @Transactional
    public void DeleteDebt(Long debtId)
    {
        CreditCardDebt debt = m_creditCardDebtRepository.findById(debtId).orElseThrow(
            () -> new RuntimeException("Debt with id " + debtId + " not found"));

        // Delete all payments associated with the debt
        List<CreditCardPayment> payments = GetPaymentsByDebtId(debtId);

        for (CreditCardPayment payment : payments)
        {
            DeletePayment(payment.GetId());
        }

        m_creditCardDebtRepository.delete(debt);

        m_logger.info("Debt with id " + debtId + " deleted");
    }

    /**
     * Archive a credit card
     * @param id The id of the credit card
     * @throws RuntimeException If the credit card does not exist
     * @throws RuntimeException If the credit card has pending payments
     */
    @Transactional
    public void ArchiveCreditCard(Long id)
    {
        CreditCard creditCard = m_creditCardRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + id +
                                        " not found and cannot be archived"));

        if (GetTotalPendingPayments(id).compareTo(BigDecimal.ZERO) > 0)
        {
            throw new RuntimeException(
                "Credit card with id " + id +
                " has pending payments and cannot be archived");
        }

        creditCard.SetArchived(true);
        m_creditCardRepository.save(creditCard);

        m_logger.info("Credit card with id " + id + " was archived");
    }

    /**
     * Unarchive a credit card
     * @param id The id of the credit card
     * @throws RuntimeException If the credit card does not exist
     */
    @Transactional
    public void UnarchiveCreditCard(Long id)
    {
        CreditCard creditCard = m_creditCardRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + id +
                                        " not found and cannot be unarchived"));

        creditCard.SetArchived(false);
        m_creditCardRepository.save(creditCard);

        m_logger.info("Credit card with id " + id + " was unarchived");
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
    public void UpdateCreditCardDebt(CreditCardDebt debt, YearMonth invoiceMonth)
    {
        CreditCardDebt oldDebt =
            m_creditCardDebtRepository.findById(debt.GetId())
                .orElseThrow(()
                                 -> new RuntimeException("Debt with id " +
                                                         debt.GetId() +
                                                         " does not exist"));

        m_creditCardRepository.findById(debt.GetCreditCard().GetId())
            .orElseThrow(()
                             -> new RuntimeException("Credit card with id " +
                                                     debt.GetCreditCard().GetId() +
                                                     " does not exist"));

        if (debt.GetTotalAmount().compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new RuntimeException("Total amount must be greater than zero");
        }

        // Complex update
        ChangeInvoiceMonth(oldDebt, invoiceMonth);
        ChangeDebtTotalAmount(oldDebt, debt.GetTotalAmount());
        ChangeDebtInstallments(oldDebt, debt.GetInstallments());

        // Trivial update
        oldDebt.SetCreditCard(debt.GetCreditCard());
        oldDebt.SetCategory(debt.GetCategory());
        oldDebt.SetDescription(debt.GetDescription());

        m_creditCardDebtRepository.save(oldDebt);

        m_logger.info("Debt with id " + debt.GetId() + " updated successfully");
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
    public void PayInvoice(Long crcId, Long walletId, Integer month, Integer year)
    {
        Wallet wallet = m_walletRepository.findById(walletId).orElseThrow(
            ()
                -> new RuntimeException("Wallet with id " + walletId +
                                        " does not exist"));

        m_creditCardRepository.findById(crcId).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + crcId +
                                        " does not exist"));

        List<CreditCardPayment> pendingPayments =
            GetPendingCreditCardPayments(crcId, month, year);

        BigDecimal pendingPaymentsTotal = pendingPayments.stream()
                                              .map(CreditCardPayment::GetAmount)
                                              .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (CreditCardPayment payment : pendingPayments)
        {
            payment.SetWallet(wallet);
            m_creditCardPaymentRepository.save(payment);

            m_logger.info(
                "Payment number " + payment.GetInstallment() + " of debt with id " +
                payment.GetCreditCardDebt().GetId() + " on credit card with id " +
                payment.GetCreditCardDebt().GetCreditCard().GetId() + " paid");
        }

        // Subtract the total of pending payments from the wallet balance
        wallet.SetBalance(wallet.GetBalance().subtract(pendingPaymentsTotal));
        m_walletRepository.save(wallet);
    }

    /**
     * Get all credit cards
     * @return A list with all credit cards
     */
    public List<CreditCard> GetAllCreditCards()
    {
        return m_creditCardRepository.findAll();
    }

    /**
     * Get all archived credit cards
     * @return A list with all archived credit cards
     */
    public List<CreditCard> GetAllArchivedCreditCards()
    {
        return m_creditCardRepository.findAllByArchivedTrue();
    }

    /**
     * Get all credit cards ordered by name
     * @return A list with all credit cards ordered by name
     */
    public List<CreditCard> GetAllCreditCardsOrderedByName()
    {
        return m_creditCardRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get all credit cards are not archived ordered by name
     * @return A list with all credit cards that are not archived ordered by name
     */
    public List<CreditCard> GetAllNonArchivedCreditCardsOrderedByName()
    {
        return m_creditCardRepository.findAllByArchivedFalseOrderByNameAsc();
    }

    /**
     * Get all credit cards are not archived ordered descending by the number of
     * transactions
     * @return A list with all credit cards that are not archived ordered by transaction
     *    count
     */
    public List<CreditCard> GetAllNonArchivedCreditCardsOrderedByTransactionCountDesc()
    {
        return m_creditCardRepository.findAllByArchivedFalse()
            .stream()
            .sorted(Comparator
                        .comparingLong(
                            (CreditCard c)
                                -> m_creditCardDebtRepository.GetDebtCountByCreditCard(
                                    c.GetId()))
                        .reversed())
            .toList();
    }

    /**
     * Get all credit card operators ordered by name
     * @return A list with all credit card operators ordered by name
     */
    public List<CreditCardOperator> GetAllCreditCardOperatorsOrderedByName()
    {
        return m_creditCardOperatorRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get available credit of a credit card
     * @param id The id of the credit card
     * @return The available credit of the credit card
     * @throws RuntimeException If the credit card does not exist
     */
    public BigDecimal GetAvailableCredit(Long id)
    {
        CreditCard creditCard = m_creditCardRepository.findById(id).orElseThrow(
            ()
                -> new RuntimeException("Credit card with id " + id +
                                        " does not exist"));

        BigDecimal totalPendingPayments =
            m_creditCardPaymentRepository.GetTotalPendingPayments(id);

        return creditCard.GetMaxDebt().subtract(totalPendingPayments);
    }

    /**
     * Get credit card payments in a month and year
     * @param month The month
     * @param year The year
     * @return A list with all credit card payments in a month and year
     */
    public List<CreditCardPayment> GetCreditCardPayments(Integer month, Integer year)
    {
        return m_creditCardPaymentRepository.GetCreditCardPayments(month, year);
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
    GetCreditCardPayments(Long crcId, Integer month, Integer year)
    {
        return m_creditCardPaymentRepository.GetCreditCardPayments(crcId, month, year);
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
    GetPendingCreditCardPayments(Long crcId, Integer month, Integer year)
    {
        return m_creditCardPaymentRepository.GetPendingCreditCardPayments(crcId,
                                                                          month,
                                                                          year);
    }

    /**
     * Get all pending credit card payments
     * @param crcId The id of the credit card
     * @return A list with all pending credit card payments
     */
    public List<CreditCardPayment> GetAllPendingCreditCardPayments(Long crcId)
    {
        return m_creditCardPaymentRepository.GetAllPendingCreditCardPayments(crcId);
    }

    /**
     * Get payments by debt id
     * @param debtId The debt id
     * @return A list with all credit card payments by debt id
     */
    public List<CreditCardPayment> GetPaymentsByDebtId(Long debtId)
    {
        return m_creditCardPaymentRepository.GetPaymentsByDebtId(debtId);
    }

    /**
     * Get all paid payments of all credit cards in a month and year
     * @param month The month
     * @param year The year
     * @return A list with all paid payments of all credit cards in a month and year
     */
    public List<CreditCardPayment> GetAllPaidPaymentsByMonth(Integer month,
                                                             Integer year)
    {
        return m_creditCardPaymentRepository.GetAllPaidPaymentsByMonth(month, year);
    }

    /**
     * Get the total debt amount of all credit cards in a month and year
     * @param month The month
     * @param year The year
     * @return The total debt amount of all credit cards in a month and year
     */
    public BigDecimal GetTotalDebtAmount(Integer month, Integer year)
    {
        return m_creditCardPaymentRepository.GetTotalDebtAmount(month, year);
    }

    /**
     * Get the total debt amount of all credit cards in a year
     * @param year The year
     * @return The total debt amount of all credit cards in a year
     */
    public BigDecimal GetTotalDebtAmount(Integer year)
    {
        return m_creditCardPaymentRepository.GetTotalDebtAmount(year);
    }

    /**
     * Get the total of all pending payments of all credit cards from a specified month
     * and year onward, including future months and the current month
     * @param month The starting month (inclusive)
     * @param year The starting year (inclusive)
     * @return The total of all pending payments of all credit cards from the specified
     *     month and year onward
     */
    public BigDecimal GetTotalPendingPayments(Integer month, Integer year)
    {
        return m_creditCardPaymentRepository.GetTotalPendingPayments(month, year);
    }

    /**
     * Get the total of all paid payments of all credit cards from a specified month
     * and year
     * @param month The month
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *   month and year
     */
    public BigDecimal GetPaidPaymentsByMonth(Integer month, Integer year)
    {
        return m_creditCardPaymentRepository.GetPaidPaymentsByMonth(month, year);
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
    public BigDecimal GetPaidPaymentsByMonth(Long walletId, Integer month, Integer year)
    {
        return m_creditCardPaymentRepository.GetPaidPaymentsByMonth(walletId,
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
    public BigDecimal GetPendingPaymentsByMonth(Integer month, Integer year)
    {
        return m_creditCardPaymentRepository.GetPendingPaymentsByMonth(month, year);
    }

    /**
     * Get the total of all pending payments of all credit cards from a specified year
     * onward, including future years and the current year
     * @param year The starting year (inclusive)
     * @return The total of all pending payments of all credit cards from the specified
     *    year onward
     */
    public BigDecimal GetTotalPendingPayments(Integer year)
    {
        return m_creditCardPaymentRepository.GetTotalPendingPayments(year);
    }

    /**
     * Get the total of all paid payments of all credit cards from a specified year
     * @param year The year
     * @return The total of all paid payments of all credit cards from the specified
     *     year
     */
    public BigDecimal GetPaidPaymentsByYear(Integer year)
    {
        return m_creditCardPaymentRepository.GetPaidPaymentsByYear(year);
    }

    /**
     * Get the total of all pending payments of all credit cards from a specified year
     * @param year The year
     * @return The total of all pending payments of all credit cards from the specified
     *     year
     */
    public BigDecimal GetPendingPaymentsByYear(Integer year)
    {
        return m_creditCardPaymentRepository.GetPendingPaymentsByYear(year);
    }

    /**
     * Get the total of all pending payments of a credit card
     * @return The total of all pending payments of all credit cards
     */
    public BigDecimal GetTotalPendingPayments(Long crcId)
    {
        return m_creditCardPaymentRepository.GetTotalPendingPayments(crcId);
    }

    /**
     * Get the total of all pending payments of all credit cards
     * @return The total of all pending payments of all credit cards
     */
    public BigDecimal GetTotalPendingPayments()
    {
        return m_creditCardPaymentRepository.GetTotalPendingPayments();
    }

    /**
     * Get the remaining debt of a purchase
     * @param debtId The id of the debt
     * @return The remaining debt of the purchase
     */
    public BigDecimal GetRemainingDebt(Long debtId)
    {
        return m_creditCardPaymentRepository.GetRemainingDebt(debtId);
    }

    /**
     * Get the invoice amount of a credit card in a specified month and year
     * @param creditCardId The credit card id
     * @param month The month
     * @param year The year
     * @return The invoice amount of the credit card in the specified month and year
     */
    public BigDecimal GetInvoiceAmount(Long crcId, Integer month, Integer year)
    {
        return m_creditCardPaymentRepository.GetInvoiceAmount(crcId, month, year);
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
    GetInvoiceStatus(Long crcId, Integer month, Integer year)
    {
        LocalDateTime nextInvoiceDate = GetNextInvoiceDate(crcId);
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
    public LocalDateTime GetNextInvoiceDate(Long crcId)
    {
        String nextInvoiceDate =
            m_creditCardPaymentRepository.GetNextInvoiceDate(crcId);

        // If there is no next invoice date, calculate it
        // If the current day is greater than the closing day, the next invoice date is
        // billingDueDay of the next month
        // Otherwise, the next invoice date is billingDueDay of the current month
        if (nextInvoiceDate == null)
        {
            LocalDateTime now = LocalDateTime.now();

            CreditCard creditCard = m_creditCardRepository.findById(crcId).orElseThrow(
                ()
                    -> new RuntimeException("Credit card with id " + crcId +
                                            " does not exist"));

            Integer currentDay = now.getDayOfMonth();
            Integer closingDay = creditCard.GetClosingDay();

            if (currentDay > closingDay)
            {
                return now.plusMonths(1).withDayOfMonth(creditCard.GetBillingDueDay());
            }

            return now.withDayOfMonth(creditCard.GetBillingDueDay());
        }

        return LocalDateTime.parse(nextInvoiceDate, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the date of the latest payment
     * @return The date of the latest payment or the current date if there are no debts
     */
    public LocalDateTime GetEarliestPaymentDate()
    {
        String date = m_creditCardDebtRepository.FindEarliestPaymentDate();

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
    public LocalDateTime GetLatestPaymentDate()
    {
        String date = m_creditCardDebtRepository.FindLatestPaymentDate();

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
    public Long GetDebtCountByCreditCard(Long id)
    {

        return m_creditCardDebtRepository.GetDebtCountByCreditCard(id);
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
    private void CreditCardBasicChecks(String     name,
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
    private void DeletePayment(Long id)
    {
        CreditCardPayment payment =
            m_creditCardPaymentRepository.findById(id).orElseThrow(
                () -> new RuntimeException("Payment with id " + id + " not found"));

        // If payment was made with a wallet, add the amount back to the
        // wallet balance
        if (payment.GetWallet() != null)
        {
            payment.GetWallet().SetBalance(
                payment.GetWallet().GetBalance().add(payment.GetAmount()));

            m_logger.info("Payment number " + payment.GetInstallment() +
                          " of debt with id " + payment.GetCreditCardDebt().GetId() +
                          " on credit card with id " +
                          payment.GetCreditCardDebt().GetCreditCard().GetId() +
                          " deleted and added to wallet with id " +
                          payment.GetWallet().GetId());

            m_walletRepository.save(payment.GetWallet());
        }

        m_creditCardPaymentRepository.delete(payment);

        m_logger.info("Payment number " + payment.GetInstallment() +
                      " of debt with id " + payment.GetCreditCardDebt().GetId() +
                      " on credit card with id " +
                      payment.GetCreditCardDebt().GetCreditCard().GetId() + " deleted");
    }

    /**
     * Update invoice month of a debt
     * @param debt The debt to be updated
     * @param invoiceMonth The new invoice month
     */
    private void ChangeInvoiceMonth(CreditCardDebt oldDebt, YearMonth invoice)
    {
        List<CreditCardPayment> payments = GetPaymentsByDebtId(oldDebt.GetId());

        CreditCardPayment firstPayment = payments.getFirst();

        // If the first payment is in the same month and year of the invoice, do not
        // update
        if (firstPayment == null ||
            (firstPayment.GetDate().getMonth() == invoice.getMonth() &&
             firstPayment.GetDate().getYear() == invoice.getYear()))
        {
            return;
        }

        for (Integer i = 0; i < oldDebt.GetInstallments(); i++)
        {
            CreditCardPayment payment = payments.get(i);

            // Calculate the payment date
            LocalDateTime paymentDate =
                invoice.plusMonths(i)
                    .atDay(oldDebt.GetCreditCard().GetBillingDueDay())
                    .atTime(23, 59);

            payment.SetDate(paymentDate);
            m_creditCardPaymentRepository.save(payment);

            m_logger.info("Payment number " + payment.GetInstallment() +
                          " of debt with id " + oldDebt.GetId() +
                          " on credit card with id " +
                          oldDebt.GetCreditCard().GetId() + " updated with due date " +
                          paymentDate);
        }
    }

    /**
     * Change the number of installments of a debt
     * @param debt The debt to be updated
     * @param newInstallments The new number of installments
     */
    private void ChangeDebtInstallments(CreditCardDebt oldDebt, Integer newInstallments)
    {
        if (oldDebt.GetInstallments() == newInstallments)
        {
            return;
        }

        List<CreditCardPayment> payments = GetPaymentsByDebtId(oldDebt.GetId());

        BigDecimal value = oldDebt.GetTotalAmount();

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
        if (newInstallments < oldDebt.GetInstallments())
        {
            for (Integer i = 0; i < oldDebt.GetInstallments(); i++)
            {
                CreditCardPayment payment = payments.get(i);

                // If the payment is greater than the new number of installments, delete
                // it
                if (payment.GetInstallment() > newInstallments)
                {
                    DeletePayment(payment.GetId());
                }
                // If the payment is less or equal than the new number of installments,
                // update it
                else
                {
                    payment.SetAmount(i == 0 ? firstInstallment : installmentValue);
                    m_creditCardPaymentRepository.save(payment);

                    m_logger.info("Payment number " + payment.GetInstallment() +
                                  " of debt with id " + oldDebt.GetId() +
                                  " on credit card with id " +
                                  oldDebt.GetCreditCard().GetId() +
                                  " updated with value " +
                                  (i == 0 ? firstInstallment : installmentValue));
                }
            }
        }
        else // Insert and update payments
        {
            for (Integer i = 1; i <= newInstallments; i++)
            {
                if (i > oldDebt.GetInstallments())
                {
                    CreditCardPayment lastPayment = payments.getLast();

                    // Calculate the payment date
                    LocalDateTime paymentDate = lastPayment.GetDate().plusMonths(1);

                    CreditCardPayment payment = new CreditCardPayment(oldDebt,
                                                                      paymentDate,
                                                                      installmentValue,
                                                                      i);

                    m_creditCardPaymentRepository.save(payment);

                    m_logger.info("Payment number " + i + " of debt with id " +
                                  oldDebt.GetId() + " on credit card with id " +
                                  oldDebt.GetCreditCard().GetId() +
                                  " registered with value " + installmentValue +
                                  " and due date " + paymentDate);

                    // Add new payment to the list
                    payments.add(payment);
                }
                else
                {
                    CreditCardPayment payment = payments.get(i - 1);

                    payment.SetAmount(i == 0 ? firstInstallment : installmentValue);
                    m_creditCardPaymentRepository.save(payment);

                    m_logger.info("Payment number " + payment.GetInstallment() +
                                  " of debt with id " + oldDebt.GetId() +
                                  " on credit card with id " +
                                  oldDebt.GetCreditCard().GetId() +
                                  " updated with value " +
                                  (i == 0 ? firstInstallment : installmentValue));
                }
            }
        }

        // Update the number of installments
        oldDebt.SetInstallments(newInstallments);
        m_creditCardDebtRepository.save(oldDebt);
    }

    /**
     * Change the total amount of a debt
     * @param oldDebt The debt to be updated
     * @param newAmount The new total amount
     */
    private void ChangeDebtTotalAmount(CreditCardDebt oldDebt, BigDecimal newAmount)
    {
        if (oldDebt.GetTotalAmount().equals(newAmount))
        {
            return;
        }

        List<CreditCardPayment> payments = GetPaymentsByDebtId(oldDebt.GetId());

        Integer installments = oldDebt.GetInstallments();

        // Divide the value exactly, with full precision
        BigDecimal exactInstallmentValue =
            newAmount.divide(new BigDecimal(installments), 2, RoundingMode.FLOOR);

        // Calculate the remainder
        BigDecimal remainder = newAmount.subtract(
            exactInstallmentValue.multiply(new BigDecimal(installments)));

        // Update payments
        for (Integer i = 0; i < oldDebt.GetInstallments(); i++)
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
            if (payment.GetWallet() != null)
            {
                BigDecimal diff = currentInstallmentValue.subtract(payment.GetAmount());

                payment.GetWallet().SetBalance(
                    payment.GetWallet().GetBalance().add(diff));

                m_logger.info("Payment number " + payment.GetInstallment() +
                              " of debt with id " + oldDebt.GetId() +
                              " on credit card with id " +
                              oldDebt.GetCreditCard().GetId() +
                              " updated and added to wallet with id " +
                              payment.GetWallet().GetId());

                m_walletRepository.save(payment.GetWallet());
            }

            payment.SetAmount(currentInstallmentValue);
            m_creditCardPaymentRepository.save(payment);

            m_logger.info("Payment number " + payment.GetInstallment() +
                          " of debt with id " + oldDebt.GetId() +
                          " on credit card with id " +
                          oldDebt.GetCreditCard().GetId() + " updated with value " +
                          currentInstallmentValue);
        }

        // Update the total amount
        oldDebt.SetTotalAmount(newAmount);
        m_creditCardDebtRepository.save(oldDebt);
    }
}
