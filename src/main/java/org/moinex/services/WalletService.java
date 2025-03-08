/*
 * Filename: WalletService.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.NoArgsConstructor;
import org.moinex.entities.Wallet;
import org.moinex.entities.WalletType;
import org.moinex.exceptions.AttributeAlreadySetException;
import org.moinex.repositories.TransferRepository;
import org.moinex.repositories.WalletRepository;
import org.moinex.repositories.WalletTransactionRepository;
import org.moinex.repositories.WalletTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is responsible for the business logic of the Wallet entity
 */
@Service
@NoArgsConstructor
public class WalletService
{
    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransferRepository transfersRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    /**
     * TODO: Check if a wallet can have no type
     * Creates a new wallet
     * @param name The name of the wallet
     * @param balance The initial balance of the wallet
     * @return The id of the created wallet
     * @throws IllegalArgumentException If the wallet name is empty
     * @throws EntityExistsException If the wallet name is already in use
     */
    @Transactional
    public Long addWallet(String name, BigDecimal balance)
    {
        // Remove leading and trailing whitespaces
        name = name.strip();

        if (name.isBlank())
        {
            throw new IllegalArgumentException("Wallet name cannot be empty");
        }

        if (walletRepository.existsByName(name))
        {
            throw new EntityExistsException("Wallet with name " + name +
                                            " already exists");
        }

        logger.info("Wallet " + name + " created with balance " + balance);

        Wallet wt = Wallet.builder().name(name).balance(balance).build();

        walletRepository.save(wt);

        return wt.getId();
    }

    /**
     * Creates a new wallet
     * @param name The name of the wallet
     * @param balance The initial balance of the wallet
     * @param walletType The type of the wallet
     * @return The id of the created wallet
     * @throws IllegalArgumentException If the wallet name is empty
     * @throws EntityExistsException If the wallet name is already in use
     */
    @Transactional
    public Long addWallet(String name, BigDecimal balance, WalletType walletType)
    {
        // Remove leading and trailing whitespaces
        name = name.strip();

        if (name.isBlank())
        {
            throw new IllegalArgumentException("Wallet name cannot be empty");
        }

        if (walletRepository.existsByName(name))
        {
            throw new EntityExistsException("Wallet with name " + name +
                                            " already exists");
        }

        logger.info("Wallet " + name + " created with balance " + balance);

        Wallet wt =
            Wallet.builder().name(name).balance(balance).type(walletType).build();

        walletRepository.save(wt);

        return wt.getId();
    }

    /**
     * Delete a wallet
     * @param id The id of the wallet to be deleted
     * @throws EntityNotFoundException If the wallet does not exist
     * @throws IllegalStateException If the wallet has transactions
     */
    @Transactional
    public void deleteWallet(Long id)
    {
        Wallet wallet = walletRepository.findById(id).orElseThrow(
            ()
                -> new EntityNotFoundException("Wallet with id " + id +
                                               " not found and cannot be deleted"));

        if (walletTransactionRepository.getTransactionCountByWallet(id) > 0 ||
            transfersRepository.getTransferCountByWallet(id) > 0)
        {
            throw new IllegalStateException(
                "Wallet with id " + id +
                " has transactions and cannot be deleted. Remove "
                + "the transactions first or archive the wallet");
        }

        walletRepository.delete(wallet);

        logger.info("Wallet with id " + id + " was permanently deleted");
    }

    /**
     * Archive a wallet
     * @param id The id of the wallet to be archived
     * @throws EntityNotFoundException If the wallet does not exist
     * @note This method is used to archive a wallet, which means that the wallet
     * will not be deleted from the database, but it will not be used in the
     * application anymore
     */
    @Transactional
    public void archiveWallet(Long id)
    {
        Wallet wallet = walletRepository.findById(id).orElseThrow(
            ()
                -> new EntityNotFoundException("Wallet with id " + id +
                                               " not found and cannot be archived"));

        wallet.setArchived(true);
        walletRepository.save(wallet);

        logger.info("Wallet with id " + id + " was archived");
    }

    /**
     * Unarchive a wallet
     * @param id The id of the wallet to be unarchived
     * @throws EntityNotFoundException If the wallet does not exist
     * @note This method is used to unarchive a wallet, which means that the wallet
     * will be used in the application again
     */
    @Transactional
    public void unarchiveWallet(Long id)
    {
        Wallet wallet = walletRepository.findById(id).orElseThrow(
            ()
                -> new EntityNotFoundException(
                    "Wallet with id " + id + " not found and cannot be unarchived"));

        wallet.setArchived(false);
        walletRepository.save(wallet);

        logger.info("Wallet with id " + id + " was unarchived");
    }

    /**
     * Rename a wallet
     * @param id The id of the wallet to be renamed
     * @param newName The new name of the wallet
     * @throws IllegalArgumentException If the wallet name is empty
     * @throws EntityNotFoundException If the wallet does not exist
     * @throws EntityExistsException If the wallet name is already in use
     */
    @Transactional
    public void renameWallet(Long id, String newName)
    {
        // Remove leading and trailing whitespaces
        newName = newName.strip();

        if (newName.isBlank())
        {
            throw new IllegalArgumentException("Wallet name cannot be empty");
        }

        Wallet wallet = walletRepository.findById(id).orElseThrow(
            () -> new EntityNotFoundException("Wallet with id " + id + " not found"));

        if (walletRepository.existsByName(newName))
        {
            throw new EntityExistsException("Wallet with name " + newName +
                                            " already exists");
        }

        wallet.setName(newName);
        walletRepository.save(wallet);

        logger.info("Wallet with id " + id + " renamed to " + newName);
    }

    /**
     * Change wallet type
     * @param id The id of the wallet to change the type
     * @param newType The new type of the wallet
     * @throws EntityNotFoundException If the wallet does not exist
     * @throws EntityNotFoundException If the wallet type does not exist
     * @throws AttributeAlreadySetException If the wallet already has the new type
     */
    @Transactional
    public void changeWalletType(Long id, WalletType newType)
    {
        Wallet wallet = walletRepository.findById(id).orElseThrow(
            () -> new EntityNotFoundException("Wallet with id " + id + " not found"));

        if (newType == null || !walletTypeRepository.existsById(newType.getId()))
        {
            throw new EntityNotFoundException("Wallet type not found");
        }

        if (wallet.getType().getId().equals(newType.getId()))
        {
            throw new AttributeAlreadySetException(
                "Wallet with name " + wallet.getName() + " already has type " +
                newType.getName());
        }

        wallet.setType(newType);
        walletRepository.save(wallet);

        logger.info("Wallet with id " + id + " type changed to " + newType.getName());
    }

    /**
     * Update the balance of a wallet
     * @param id The id of the wallet
     * @param newBalance The new balance of the wallet
     * @throws EntityNotFoundException If the wallet does not exist
     */
    @Transactional
    public void updateWalletBalance(Long id, BigDecimal newBalance)
    {
        Wallet wallet = walletRepository.findById(id).orElseThrow(
            () -> new EntityNotFoundException("Wallet with id " + id + " not found"));

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        logger.info("Wallet with id " + id + " balance updated to " + newBalance);
    }

    /**
     * Get all wallets
     * @return A list with all wallets
     */
    public List<Wallet> getAllWallets()
    {
        return walletRepository.findAll();
    }

    /**
     * Get all wallets ordered by name
     * @return A list with all wallets ordered by name
     */
    public List<Wallet> getAllWalletsOrderedByName()
    {
        return walletRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get wallet by name
     * @param name The name of the wallet
     * @return The wallet with the provided name
     * @throws EntityNotFoundException If the wallet does not exist
     */
    public Wallet getWalletByName(String name)
    {
        return walletRepository.findByName(name).orElseThrow(
            ()
                -> new EntityNotFoundException("Wallet with name " + name +
                                               " not found"));
    }

    /**
     * Get wallet by id
     * @param id The id of the wallet
     * @return The wallet with the provided id
     * @throws EntityNotFoundException If the wallet does not exist
     */
    public Wallet getWalletById(Long id)
    {
        return walletRepository.findById(id).orElseThrow(
            () -> new EntityNotFoundException("Wallet with id " + id + " not found"));
    }

    /**
     * Get all wallet types
     * @return A list with all wallet types
     */
    public List<WalletType> getAllWalletTypes()
    {
        return walletTypeRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get all archived wallets
     * @return A list with all archived wallets
     */
    public List<Wallet> getAllArchivedWallets()
    {
        return walletRepository.findAllByIsArchivedTrue();
    }

    /**
     * Get all wallets that are not archived ordered by name
     * @return A list with all wallets that are not archived
     */
    public List<Wallet> getAllNonArchivedWalletsOrderedByName()
    {
        return walletRepository.findAllByIsArchivedFalseOrderByNameAsc();
    }

    /**
     * Get all wallets that are not archived ordered descending by the number of
     * transactions
     * @return A list with all wallets that are not archived
     */
    public List<Wallet> getAllNonArchivedWalletsOrderedByTransactionCountDesc()
    {
        return walletRepository.findAllByIsArchivedFalse()
            .stream()
            .sorted(
                Comparator
                    .comparingLong(
                        (Wallet w)
                            -> walletTransactionRepository.getTransactionCountByWallet(
                                   w.getId()) +
                                   transfersRepository.getTransferCountByWallet(
                                       w.getId()))
                    .reversed())
            .toList();
    }
}
