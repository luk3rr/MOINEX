/*
 * Filename: WalletTestDatabase.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.wallet

import io.mockk.mockk
import org.moinex.common.ClockProvider
import org.moinex.common.util.UIUtils
import org.moinex.dataexchange.creditcard.CreditCardCreditSpec
import org.moinex.dataexchange.creditcard.CreditCardDebtSpec
import org.moinex.dataexchange.creditcard.CreditCardOperatorSpec
import org.moinex.dataexchange.creditcard.CreditCardPaymentSpec
import org.moinex.dataexchange.creditcard.CreditCardSpec
import org.moinex.dataexchange.creditcard.RecurringCreditCardDebtSpec
import org.moinex.dataexchange.export.ExportService
import org.moinex.dataexchange.imports.ImportExecutor
import org.moinex.dataexchange.imports.ImportPlanner
import org.moinex.dataexchange.investment.BondOperationSpec
import org.moinex.dataexchange.investment.BondSpec
import org.moinex.dataexchange.investment.CryptoExchangeSpec
import org.moinex.dataexchange.investment.DividendSpec
import org.moinex.dataexchange.investment.InvestmentTargetSpec
import org.moinex.dataexchange.investment.TickerPurchaseSpec
import org.moinex.dataexchange.investment.TickerSaleSpec
import org.moinex.dataexchange.investment.TickerSpec
import org.moinex.dataexchange.investment.TransactionLinker
import org.moinex.dataexchange.planning.BudgetGroupSpec
import org.moinex.dataexchange.planning.FinancialPlanSpec
import org.moinex.dataexchange.planning.WishlistItemLinkSpec
import org.moinex.dataexchange.planning.WishlistItemSpec
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.spec.EntitySpec
import org.moinex.model.Category
import org.moinex.model.Notification
import org.moinex.model.creditcard.CreditCard
import org.moinex.model.creditcard.CreditCardCredit
import org.moinex.model.creditcard.CreditCardDebt
import org.moinex.model.creditcard.CreditCardOperator
import org.moinex.model.creditcard.CreditCardPayment
import org.moinex.model.creditcard.RecurringCreditCardDebt
import org.moinex.model.financialplanning.BudgetGroup
import org.moinex.model.financialplanning.FinancialPlan
import org.moinex.model.goal.Goal
import org.moinex.model.investment.Bond
import org.moinex.model.investment.BondOperation
import org.moinex.model.investment.CryptoExchange
import org.moinex.model.investment.Dividend
import org.moinex.model.investment.InvestmentTarget
import org.moinex.model.investment.Ticker
import org.moinex.model.investment.TickerPurchase
import org.moinex.model.investment.TickerSale
import org.moinex.model.wallettransaction.RecurringTransaction
import org.moinex.model.wallettransaction.Transfer
import org.moinex.model.wallettransaction.Wallet
import org.moinex.model.wallettransaction.WalletTransaction
import org.moinex.model.wallettransaction.WalletType
import org.moinex.model.wishlist.WishlistItem
import org.moinex.model.wishlist.WishlistItemLink
import org.moinex.repository.CategoryRepository
import org.moinex.repository.NotificationRepository
import org.moinex.repository.creditcard.CreditCardCreditRepository
import org.moinex.repository.creditcard.CreditCardDebtRepository
import org.moinex.repository.creditcard.CreditCardOperatorRepository
import org.moinex.repository.creditcard.CreditCardPaymentRepository
import org.moinex.repository.creditcard.CreditCardRepository
import org.moinex.repository.creditcard.RecurringCreditCardDebtRepository
import org.moinex.repository.financialplanning.FinancialPlanRepository
import org.moinex.repository.investment.BondOperationRepository
import org.moinex.repository.investment.BondRepository
import org.moinex.repository.investment.CryptoExchangeRepository
import org.moinex.repository.investment.DividendRepository
import org.moinex.repository.investment.InvestmentTargetRepository
import org.moinex.repository.investment.TickerPurchaseRepository
import org.moinex.repository.investment.TickerRepository
import org.moinex.repository.investment.TickerSaleRepository
import org.moinex.repository.wallettransaction.RecurringTransactionRepository
import org.moinex.repository.wallettransaction.TransferRepository
import org.moinex.repository.wallettransaction.WalletRepository
import org.moinex.repository.wallettransaction.WalletTransactionRepository
import org.moinex.repository.wallettransaction.WalletTypeRepository
import org.moinex.repository.wishlist.WishlistItemLinkRepository
import org.moinex.repository.wishlist.WishlistItemRepository
import org.moinex.service.CategoryService
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.moinex.service.creditcard.CreditCardService
import org.moinex.service.investment.BondInterestCalculationService
import org.moinex.service.investment.BondService
import org.moinex.service.investment.TickerMarketDataService
import org.moinex.service.investment.TickerService
import org.moinex.service.wallet.RecurringTransactionService
import org.moinex.service.wallet.WalletService
import org.moinex.service.wishlist.WishlistService
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.UUID
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = ["org.moinex.repository"],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = [
                "org\\.moinex\\.repository\\.(?!(wallettransaction|creditcard)\\." +
                    "|financialplanning\\.FinancialPlanRepository\$|wishlist\\.|CategoryRepository" +
                    "|NotificationRepository" +
                    "|investment\\.(Ticker|TickerPurchase|TickerSale|Dividend|CryptoExchange|Bond|BondOperation" +
                    "|InvestmentTarget)Repository\$).*",
            ],
        ),
    ],
)
class WalletTestConfig {
    @Bean
    fun dataSource(): DataSource =
        DriverManagerDataSource("jdbc:h2:mem:${UUID.randomUUID()};DB_CLOSE_DELAY=-1", "sa", "")

    @Bean
    fun entityManagerFactory(dataSource: DataSource): LocalContainerEntityManagerFactoryBean =
        LocalContainerEntityManagerFactoryBean().apply {
            this.dataSource = dataSource
            jpaVendorAdapter = HibernateJpaVendorAdapter()
            setManagedTypes(
                PersistenceManagedTypes.of(
                    listOf(
                        Wallet::class,
                        WalletType::class,
                        WalletTransaction::class,
                        Transfer::class,
                        RecurringTransaction::class,
                        Category::class,
                        Notification::class,
                        CreditCard::class,
                        CreditCardOperator::class,
                        CreditCardDebt::class,
                        CreditCardPayment::class,
                        CreditCardCredit::class,
                        RecurringCreditCardDebt::class,
                        Ticker::class,
                        TickerPurchase::class,
                        TickerSale::class,
                        Dividend::class,
                        CryptoExchange::class,
                        Bond::class,
                        BondOperation::class,
                        InvestmentTarget::class,
                        Goal::class,
                        FinancialPlan::class,
                        BudgetGroup::class,
                        WishlistItem::class,
                        WishlistItemLink::class,
                    ).map { it.java.name },
                    emptyList(),
                ),
            )
            setJpaPropertyMap(
                mapOf(
                    "hibernate.hbm2ddl.auto" to "create-drop",
                    "hibernate.auto_quote_keyword" to "true",
                ),
            )
        }

    @Bean
    fun transactionManager(emf: jakarta.persistence.EntityManagerFactory): PlatformTransactionManager =
        JpaTransactionManager(emf)

    @Bean
    fun clockProvider() = ClockProvider()

    @Bean
    fun preferencesService(): PreferencesService = mockk<PreferencesService>(relaxed = true).also { UIUtils(it) }

    @Bean
    fun notificationService(
        repository: NotificationRepository,
        clock: ClockProvider,
    ) = NotificationService(repository, clock)

    @Bean
    fun walletService(
        wallets: WalletRepository,
        transfers: TransferRepository,
        transactions: WalletTransactionRepository,
        types: WalletTypeRepository,
        notifications: NotificationService,
        preferences: PreferencesService,
        clock: ClockProvider,
    ) = WalletService(wallets, transfers, transactions, types, notifications, preferences, clock)

    @Bean
    fun categoryService(
        categories: CategoryRepository,
        walletService: WalletService,
        creditCardService: CreditCardService,
        wishlistService: WishlistService,
        notifications: NotificationService,
        preferences: PreferencesService,
    ) = CategoryService(
        categories,
        creditCardService,
        walletService,
        wishlistService,
        notifications,
        preferences,
    )

    @Bean
    fun creditCardService(
        debts: CreditCardDebtRepository,
        payments: CreditCardPaymentRepository,
        cards: CreditCardRepository,
        operators: CreditCardOperatorRepository,
        wallets: WalletRepository,
        credits: CreditCardCreditRepository,
        notifications: NotificationService,
        preferences: PreferencesService,
        clock: ClockProvider,
    ) = CreditCardService(debts, payments, cards, operators, wallets, credits, notifications, preferences, clock)

    @Bean
    fun operatorSpec(repository: CreditCardOperatorRepository) = CreditCardOperatorSpec(repository)

    @Bean
    fun cardSpec(
        repository: CreditCardRepository,
        operators: CreditCardOperatorRepository,
        wallets: WalletRepository,
        service: CreditCardService,
    ) = CreditCardSpec(repository, operators, wallets, service)

    @Bean
    fun recurringDebtSpec(
        repository: RecurringCreditCardDebtRepository,
        cards: CreditCardRepository,
        categories: CategoryRepository,
    ) = RecurringCreditCardDebtSpec(repository, cards, categories)

    @Bean
    fun debtSpec(
        repository: CreditCardDebtRepository,
        payments: CreditCardPaymentRepository,
        recurring: RecurringCreditCardDebtRepository,
        cards: CreditCardRepository,
        categories: CategoryRepository,
        service: CreditCardService,
    ) = CreditCardDebtSpec(repository, payments, recurring, cards, categories, service)

    @Bean
    fun paymentSpec(
        repository: CreditCardPaymentRepository,
        debts: CreditCardDebtRepository,
        wallets: WalletRepository,
    ) = CreditCardPaymentSpec(repository, debts, wallets)

    @Bean
    fun creditSpec(
        repository: CreditCardCreditRepository,
        cards: CreditCardRepository,
    ) = CreditCardCreditSpec(repository, cards)

    @Bean
    fun wishlistService(
        items: WishlistItemRepository,
        links: WishlistItemLinkRepository,
        walletService: WalletService,
        creditCardService: CreditCardService,
        notifications: NotificationService,
        preferences: PreferencesService,
        clock: ClockProvider,
    ) = WishlistService(items, links, walletService, creditCardService, notifications, preferences, clock)

    @Bean
    fun financialPlanSpec(
        repository: FinancialPlanRepository,
        categories: CategoryRepository,
    ) = FinancialPlanSpec(repository, categories)

    @Bean
    fun budgetGroupSpec(repository: FinancialPlanRepository) = BudgetGroupSpec(repository)

    @Bean
    fun wishlistItemSpec(
        repository: WishlistItemRepository,
        categories: CategoryRepository,
        debts: CreditCardDebtRepository,
        linker: TransactionLinker,
        service: WishlistService,
    ) = WishlistItemSpec(repository, categories, debts, linker, service)

    @Bean
    fun wishlistItemLinkSpec(
        repository: WishlistItemLinkRepository,
        items: WishlistItemRepository,
    ) = WishlistItemLinkSpec(repository, items)

    @Bean
    fun tickerService(
        tickers: TickerRepository,
        purchases: TickerPurchaseRepository,
        sales: TickerSaleRepository,
        dividends: DividendRepository,
        exchanges: CryptoExchangeRepository,
        walletService: WalletService,
        notifications: NotificationService,
        preferences: PreferencesService,
        clock: ClockProvider,
    ) = TickerService(
        tickers,
        purchases,
        sales,
        dividends,
        exchanges,
        walletService,
        notifications,
        preferences,
        clock,
        mockk<TickerMarketDataService>(relaxed = true),
    )

    @Bean
    fun bondService(
        bonds: BondRepository,
        operations: BondOperationRepository,
        walletService: WalletService,
        notifications: NotificationService,
        preferences: PreferencesService,
    ) = BondService(
        bonds,
        operations,
        walletService,
        mockk<BondInterestCalculationService>(relaxed = true),
        notifications,
        preferences,
    )

    @Bean
    fun transactionLinker(repository: WalletTransactionRepository) = TransactionLinker(repository)

    @Bean
    fun tickerSpec(
        repository: TickerRepository,
        service: TickerService,
    ) = TickerSpec(repository, service)

    @Bean
    fun tickerPurchaseSpec(
        repository: TickerPurchaseRepository,
        tickers: TickerRepository,
        linker: TransactionLinker,
    ) = TickerPurchaseSpec(repository, tickers, linker)

    @Bean
    fun tickerSaleSpec(
        repository: TickerSaleRepository,
        tickers: TickerRepository,
        linker: TransactionLinker,
    ) = TickerSaleSpec(repository, tickers, linker)

    @Bean
    fun dividendSpec(
        repository: DividendRepository,
        tickers: TickerRepository,
        linker: TransactionLinker,
    ) = DividendSpec(repository, tickers, linker)

    @Bean
    fun cryptoExchangeSpec(
        repository: CryptoExchangeRepository,
        tickers: TickerRepository,
    ) = CryptoExchangeSpec(repository, tickers)

    @Bean
    fun bondSpec(
        repository: BondRepository,
        service: BondService,
    ) = BondSpec(repository, service)

    @Bean
    fun bondOperationSpec(
        repository: BondOperationRepository,
        bonds: BondRepository,
        linker: TransactionLinker,
    ) = BondOperationSpec(repository, bonds, linker)

    @Bean
    fun investmentTargetSpec(repository: InvestmentTargetRepository) = InvestmentTargetSpec(repository)

    @Bean
    fun recurringTransactionService(
        repository: RecurringTransactionRepository,
        walletService: WalletService,
        notifications: NotificationService,
        preferences: PreferencesService,
        clock: ClockProvider,
    ) = RecurringTransactionService(repository, walletService, notifications, preferences, clock)

    @Bean
    fun walletTypeSpec(repository: WalletTypeRepository) = WalletTypeSpec(repository)

    @Bean
    fun categorySpec(
        repository: CategoryRepository,
        service: CategoryService,
    ) = CategorySpec(repository, service)

    @Bean
    fun walletSpec(
        wallets: WalletRepository,
        types: WalletTypeRepository,
        transactions: WalletTransactionRepository,
        transfers: TransferRepository,
        service: WalletService,
    ) = WalletSpec(wallets, types, transactions, transfers, service)

    @Bean
    fun walletTransactionSpec(
        transactions: WalletTransactionRepository,
        wallets: WalletRepository,
        categories: CategoryRepository,
        service: WalletService,
    ) = WalletTransactionSpec(transactions, wallets, categories, service)

    @Bean
    fun transferSpec(
        transfers: TransferRepository,
        wallets: WalletRepository,
        categories: CategoryRepository,
        service: WalletService,
    ) = TransferSpec(transfers, wallets, categories, service)

    @Bean
    fun recurringTransactionSpec(
        repository: RecurringTransactionRepository,
        wallets: WalletRepository,
        categories: CategoryRepository,
        service: RecurringTransactionService,
    ) = RecurringTransactionSpec(repository, wallets, categories, service)

    @Bean
    fun entityCatalog(specs: ObjectProvider<EntitySpec>) = EntityCatalog(specs)

    @Bean
    fun exportService(catalog: EntityCatalog) = ExportService(catalog)

    @Bean
    fun importPlanner(catalog: EntityCatalog) = ImportPlanner(catalog)

    @Bean
    fun importExecutor(
        catalog: EntityCatalog,
        manager: PlatformTransactionManager,
        notifications: NotificationService,
    ) = ImportExecutor(catalog, manager, notifications)
}

class WalletTestDatabase : AutoCloseable {
    val context = AnnotationConfigApplicationContext(WalletTestConfig::class.java)

    inline fun <reified T : Any> get(): T = context.getBean(T::class.java)

    override fun close() {
        context.close()
    }
}
