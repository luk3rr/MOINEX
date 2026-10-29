# Data model — MOINEX

Status: human-reviewed (2026-10-29, delta pós v2.19.0) · Confidence: medium

## Armazenamento
- SQLite em `~/.moinex/data/moinex.db` (diretório configurável nas preferências, com fallback para o padrão). [src: app/SpringApp.kt:28-48] [src: CLAUDE.md]
- Pool HikariCP com 1 conexão; Hibernate 7.4 com `ddl-auto: validate` e `SQLiteDialect` do `hibernate-community-dialects`; schema só por Flyway (34 migrations, V001..V034). [src: CLAUDE.md] [src: src/main/resources/db/migration/]
- Preferências do usuário (ex.: tema, pasta do banco) em `java.util.prefs` e `PreferencesService`, fora do banco. [src: app/SpringApp.kt:31-33] [src: service/PreferencesService.kt]

## Entidades (36), por domínio
| Domínio | Entidades |
|---|---|
| Carteiras e transações | Wallet, WalletType, WalletTransaction, Transfer, RecurringTransaction, Category |
| Cartões | CreditCard, CreditCardOperator, CreditCardDebt, CreditCardPayment, CreditCardCredit, RecurringCreditCardDebt |
| Planejamento | FinancialPlan, BudgetGroup, FIRECalculatorSettings, Goal |
| Investimentos | Ticker, TickerPurchase, TickerSale, TickerPriceHistory, Dividend, Bond, BondOperation, BondInterestCalculation, FundamentalAnalysis, InvestmentTarget, InvestmentPerformanceSnapshot, CryptoExchange |
| Mercado | BrazilianMarketIndicators, MarketQuotesAndCommodities, MarketIndicatorHistory |
| Outros | NetWorthSnapshot, Notification, CalendarEvent, WishlistItem, WishlistItemLink |

[src: grep "@Entity" em model/]

## Relações (visão aproximada)
```mermaid
erDiagram
  WALLET ||--o{ WALLET_TRANSACTION : has
  WALLET }o--|| WALLET_TYPE : typed
  WALLET_TRANSACTION }o--|| CATEGORY : categorized
  CREDIT_CARD ||--o{ CREDIT_CARD_DEBT : has
  CREDIT_CARD_DEBT ||--o{ CREDIT_CARD_PAYMENT : paid_by
  TICKER ||--o{ TICKER_PURCHASE : bought
  TICKER ||--o{ TICKER_SALE : sold
  TICKER ||--o{ TICKER_PRICE_HISTORY : priced
  BOND ||--o{ BOND_OPERATION : operations
  FINANCIAL_PLAN ||--o{ BUDGET_GROUP : groups
  WISHLIST_ITEM ||--o{ WISHLIST_ITEM_LINK : links
```
Caption: relações inferidas pelos nomes das entidades; há 35 anotações de relação JPA no total. [inferred] [src: grep em model/]

## Migrations
Sufixo `V0xx__descricao.sql`; `data.sql` semeia dados iniciais (inserção só se a tabela estiver vazia). Ordem de inicialização: o bean manual `flyway` migra e depois executa `data.sql`; o `entityManagerFactory` depende do bean `flyway`, e `spring.sql.init.mode` é `never` de propósito. [src: src/main/resources/data.sql] [src: config/FlywayConfig.kt] [src: src/main/resources/application.yml]

## PII
Dados financeiros pessoais, locais. Credenciais de IA ficam em `AiCredentialStore`; valores não são copiados aqui. [src: ai/credentials/AiCredentialStore.kt]
