/*
 * Filename: WalletService.java
 * Created on: August 31, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.moinex.error.MoinexException;
import org.moinex.model.wallettransaction.Wallet;
import org.moinex.model.wallettransaction.WalletType;
import org.moinex.repository.goal.GoalRepository;
import org.moinex.repository.wallettransaction.TransferRepository;
import org.moinex.repository.wallettransaction.WalletRepository;
import org.moinex.repository.wallettransaction.WalletTransactionRepository;
import org.moinex.repository.wallettransaction.WalletTypeRepository;
import org.moinex.util.UIUtils;
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
public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private WalletRepository walletRepository;
    private TransferRepository transfersRepository;
    private WalletTransactionRepository walletTransactionRepository;
    private WalletTypeRepository walletTypeRepository;
    private GoalRepository goalRepository;

    @Autowired
    public WalletService(
            WalletRepository walletRepository,
            TransferRepository transfersRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletTypeRepository walletTypeRepository,
            GoalRepository goalRepository) {
        this.walletRepository = walletRepository;
        this.transfersRepository = transfersRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletTypeRepository = walletTypeRepository;
        this.goalRepository = goalRepository;
    }

    /**
     * Creates a new wallet
     *
     * @param name       The name of the wallet
     * @param balance    The initial balance of the wallet
     * @param walletType The type of the wallet
     * @return The id of the created wallet
     * @throws IllegalArgumentException If the wallet name is empty
     * @throws EntityExistsException    If the wallet name is already in use
     */
    @Transactional
    public Integer addWallet(String name, BigDecimal balance, WalletType walletType) {
        // Remove leading and trailing whitespaces
        name = name.strip();

        if (name.isBlank()) {
            throw new IllegalArgumentException("Wallet name cannot be empty");
        }

        if (walletRepository.existsByName(name)) {
            throw new EntityExistsException("Wallet with name " + name + " already exists");
        }

        logger.info("Wallet {} created with balance {}", name, balance);

        Wallet wt = Wallet.builder().name(name).balance(balance).type(walletType).build();

        walletRepository.save(wt);

        return wt.getId();
    }

    /**
     * Delete a wallet
     *
     * @param id The id of the wallet to be deleted
     * @throws EntityNotFoundException If the wallet does not exist
     * @throws IllegalStateException   If the wallet has transactions
     */
    @Transactional
    public void deleteWallet(Integer id) {
        Wallet wallet =
                walletRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Wallet with id %d not found and cannot be"
                                                                + " deleted",
                                                        id)));

        if (walletTransactionRepository.getTransactionCountByWallet(id) > 0
                || transfersRepository.getTransferCountByWallet(id) > 0) {
            throw new IllegalStateException(
                    String.format(
                            "Wallet with id %d has transactions and cannot be deleted."
                                    + "Remove the transactions first or archive the wallet",
                            id));
        }

        if (wallet.isMaster()) {
            removeAllVirtualWalletsFromMasterWallet(wallet.getId());
        }

        walletRepository.delete(wallet);

        logger.info("Wallet with id {} was permanently deleted", id);
    }

    public void removeAllVirtualWalletsFromMasterWallet(Integer masterWalletId) {
        List<Wallet> virtualWallets =
                walletRepository.findVirtualWalletsByMasterWallet(masterWalletId);

        if (!virtualWallets.isEmpty()) {
            logger.info(
                    "Removing {} virtual wallets from master wallet with id {}",
                    virtualWallets.size(),
                    masterWalletId);
            virtualWallets.forEach(
                    virtualWallet -> {
                        virtualWallet.setMasterWallet(null);
                        walletRepository.save(virtualWallet);
                        logger.info(
                                "Virtual wallet with id {} unlinked from master wallet",
                                virtualWallet.getId());
                    });
        }
    }

    /**
     * Archive a wallet
     *
     * @param id The id of the wallet to be archived
     * @throws EntityNotFoundException If the wallet does not exist
     * @note This method is used to archive a wallet, which means that the wallet
     * will not be deleted from the database, but it will not be used in the
     * application anymore
     */
    @Transactional
    public void archiveWallet(Integer id) {
        Wallet wallet =
                walletRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Wallet with id %d not found and cannot be"
                                                                + " archived",
                                                        id)));
        wallet.setArchived(true);

        if (wallet.isMaster()) {
            updateWalletBalance(wallet.getId(), getUnallocatedBalance(wallet));
            removeAllVirtualWalletsFromMasterWallet(wallet.getId());
        } else if (wallet.isVirtual()) {
            wallet.setMasterWallet(null);
        }

        walletRepository.save(wallet);

        logger.info("Wallet with id {} was archived", id);
    }

    /**
     * Unarchive a wallet
     *
     * @param id The id of the wallet to be unarchived
     * @throws EntityNotFoundException If the wallet does not exist
     * @note This method is used to unarchive a wallet, which means that the wallet
     * will be used in the application again
     */
    @Transactional
    public void unarchiveWallet(Integer id) {
        Wallet wallet =
                walletRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format(
                                                        "Wallet with id %d not found and cannot be"
                                                                + " unarchived",
                                                        id)));

        wallet.setArchived(false);
        walletRepository.save(wallet);

        logger.info("Wallet with id {} was unarchived", id);
    }

    /**
     * Rename a wallet
     *
     * @param id      The id of the wallet to be renamed
     * @param newName The new name of the wallet
     * @throws IllegalArgumentException If the wallet name is empty
     * @throws EntityNotFoundException  If the wallet does not exist
     * @throws EntityExistsException    If the wallet name is already in use
     */
    @Transactional
    public void renameWallet(Integer id, String newName) {
        // Remove leading and trailing whitespaces
        newName = newName.strip();

        if (newName.isBlank()) {
            throw new IllegalArgumentException("Wallet name cannot be empty");
        }

        Wallet wallet =
                walletRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format("Wallet with id %d not found", id)));

        if (walletRepository.existsByName(newName)) {
            throw new EntityExistsException("Wallet with name " + newName + " already exists");
        }

        wallet.setName(newName);
        walletRepository.save(wallet);

        logger.info("Wallet with id {} renamed to {}", id, newName);
    }

    /**
     * Change the wallet type
     *
     * @param id      The id of the wallet to change the type
     * @param newType The new type of the wallet
     * @throws EntityNotFoundException                      If the wallet does not exist
     * @throws EntityNotFoundException                      If the wallet type does not exist
     * @throws MoinexException.AttributeAlreadySetException If the wallet already has the new type
     */
    @Transactional
    public void changeWalletType(Integer id, WalletType newType) {
        Wallet wallet =
                walletRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format("Wallet with id %d not found", id)));

        if (newType == null || !walletTypeRepository.existsById(newType.getId())) {
            throw new EntityNotFoundException("Wallet type not found");
        }

        if (wallet.getType().getId().equals(newType.getId())) {
            throw new MoinexException.AttributeAlreadySetException(
                    "Wallet with name "
                            + wallet.getName()
                            + " already has type "
                            + newType.getName());
        }

        wallet.setType(newType);
        walletRepository.save(wallet);

        logger.info("Wallet with id {} type changed to {}", id, newType.getName());
    }

    /**
     * Update the balance of a wallet
     *
     * @param id         The id of the wallet
     * @param newBalance The new balance of the wallet
     * @throws EntityNotFoundException If the wallet does not exist
     */
    @Transactional
    public void updateWalletBalance(Integer id, BigDecimal newBalance) {
        Wallet wallet =
                walletRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format("Wallet with id %d not found", id)));

        BigDecimal diff = newBalance.subtract(wallet.getBalance());

        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            updateWalletBalance(id, diff, true);
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            updateWalletBalance(id, diff.negate(), false);
        } else {
            logger.info("Wallet with id {} balance remains unchanged", id);
        }
    }

    @Transactional
    public void incrementWalletBalance(Integer id, BigDecimal amount) {
        updateWalletBalance(id, amount, true);
    }

    @Transactional
    public void decrementWalletBalance(Integer id, BigDecimal amount) {
        updateWalletBalance(id, amount, false);
    }

    private void updateWalletBalance(
            @NonNull Integer id, @NonNull BigDecimal amount, boolean isIncrement) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Wallet wallet =
                walletRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                String.format("Wallet with id %d not found", id)));

        BigDecimal adjustedAmount = isIncrement ? amount : amount.negate();

        if (wallet.isVirtual()) {
            if (wallet.getBalance().add(adjustedAmount).compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalStateException("Virtual wallets cannot have negative balances");
            }

            Wallet master = wallet.getMasterWallet();
            master.setBalance(master.getBalance().add(adjustedAmount));
            walletRepository.save(master);

            logger.info(
                    "Master wallet with id {} balance {} by {}",
                    master.getId(),
                    isIncrement ? "incremented" : "decremented",
                    amount);

        } else if (wallet.isMaster() && !isIncrement) {
            BigDecimal unallocatedBalance = getUnallocatedBalance(wallet);

            if (unallocatedBalance.compareTo(amount) < 0) {
                throw new IllegalStateException(
                        "Master wallet cannot be decremented below the unallocated balance. "
                                + "\nUnallocated balance is "
                                + UIUtils.formatCurrency(unallocatedBalance));
            }
        }

        wallet.setBalance(wallet.getBalance().add(adjustedAmount));
        walletRepository.save(wallet);

        logger.info(
                "Wallet with id {} balance {} by {}",
                wallet.getId(),
                isIncrement ? "incremented" : "decremented",
                amount);
    }

    /**
     * Get wallet by id
     *
     * @param id The id of the wallet
     * @return The wallet with the provided id
     * @throws EntityNotFoundException If the wallet does not exist
     */
    public Wallet getWalletById(Integer id) {
        return walletRepository
                .findById(id)
                .orElseThrow(
                        () -> new EntityNotFoundException("Wallet with id " + id + " not found"));
    }

    /**
     * Get all wallet types
     *
     * @return A list with all wallet types
     */
    public List<WalletType> getAllWalletTypes() {
        return walletTypeRepository.findAllByOrderByNameAsc();
    }

    /**
     * Get all archived wallets
     *
     * @return A list with all archived wallets
     */
    public List<Wallet> getAllArchivedWallets() {
        return walletRepository.findAllByIsArchivedTrue();
    }

    /**
     * Get all wallets that are not archived ordered by name
     *
     * @return A list with all wallets that are not archived
     */
    public List<Wallet> getAllNonArchivedWalletsOrderedByName() {
        return walletRepository.findAllByIsArchivedFalseOrderByNameAsc();
    }

    /**
     * Get all wallets that are not archived ordered descending by the number of
     * transactions
     *
     * @return A list with all wallets that are not archived
     */
    public List<Wallet> getAllNonArchivedWalletsOrderedByTransactionCountDesc() {
        return walletRepository.findAllByIsArchivedFalse().stream()
                .sorted(
                        Comparator.comparingLong(
                                        (Wallet w) ->
                                                walletTransactionRepository
                                                                .getTransactionCountByWallet(
                                                                        w.getId())
                                                        + transfersRepository
                                                                .getTransferCountByWallet(
                                                                        w.getId()))
                                .reversed())
                .toList();
    }

    /**
     * Get the unallocated balance of a master wallet.
     * This is the balance that is not allocated to any goals
     *
     * @param masterWallet The master wallet to check
     * @return The unallocated balance of the master wallet
     */
    public BigDecimal getUnallocatedBalance(@NonNull Wallet masterWallet) {
        BigDecimal allocatedBalance =
                walletRepository.getAllocatedBalanceByMasterWallet(masterWallet.getId());

        return masterWallet.getBalance().subtract(allocatedBalance);
    }

    public Integer getCountOfVirtualWalletsByMasterWalletId(Integer masterWalletId) {
        return walletRepository.getCountOfVirtualWalletsByMasterWalletId(masterWalletId);
    }
}
