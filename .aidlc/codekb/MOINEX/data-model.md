# Data model — MOINEX

Status: human-reviewed (2026-11-07) · Confidence: medium

## Armazenamento
- SQLite em `~/.moinex/data/moinex.db` (diretório configurável nas preferências, com fallback para o padrão). [src: app/SpringApp.kt:28-48] [src: CLAUDE.md]
- Pool HikariCP com 1 conexão; Hibernate 7.4 com `ddl-auto: validate` e `SQLiteDialect` do `hibernate-community-dialects`; schema só por Flyway. [src: CLAUDE.md] [src: src/main/resources/db/migration/]
- Preferências do usuário (ex.: tema, pasta do banco) em `java.util.prefs` e `PreferencesService`, fora do banco. [src: app/SpringApp.kt:31-33] [src: service/PreferencesService.kt]

## Entidades, por domínio
| Domínio | Entidades |
|---|---|
| Carteiras e transações | Wallet, WalletType, WalletTransaction, Transfer, RecurringTransaction, Category |
| Cartões | CreditCard, CreditCardOperator, CreditCardDebt, CreditCardPayment, CreditCardCredit, RecurringCreditCardDebt |
| Planejamento | FinancialPlan, BudgetGroup, FIRECalculatorSettings, Goal |
| Investimentos | Ticker, TickerPurchase, TickerSale, TickerPriceHistory, Dividend, Bond, BondOperation, BondInterestCalculation, FundamentalAnalysis, InvestmentTarget, InvestmentPerformanceSnapshot, CryptoExchange |
| Mercado | BrazilianMarketIndicators, MarketQuotesAndCommodities, MarketIndicatorHistory |
| Outros | NetWorthSnapshot, Notification, CalendarEvent, WishlistItem, WishlistItemLink |
| Conversas com o assistente | AiChat, AiChatMessage, AiChatChangeSet, AiChatChange (`model/ai`) |

[src: grep "@Entity" em model/]

## Relações e diagrama ER
O diagrama entidade-relacionamento completo, com colunas, chaves e relações de todas as tabelas, está em `docs/database-erd.md`. Ele é gerado a partir das migrations Flyway (`scripts/dev/generate_erd.py`, `make erd`), então reflete o schema real e não depende desta página. [src: docs/database-erd.md] [src: scripts/dev/generate_erd.py]

Pontos que o diagrama não diz sozinho:
- O JDBC do app não liga `PRAGMA foreign_keys`: as chaves estrangeiras documentam as relações, mas o banco não as impõe, e exclusões em cascata são feitas pelo código (por exemplo, `AiChatHistoryService` apaga propostas, lotes, mensagens e conversa nessa ordem). [src: application.yml (jdbc:sqlite:…?journal_mode=WAL)] [src: service/ai/AiChatHistoryService.kt]
- As tabelas `purchase` e `sale` existem no schema, mas nenhuma entidade JPA as mapeia (`TickerPurchase` e `TickerSale` usam `ticker_purchase` e `ticker_sale`); parecem legado de migrations antigas. [inferred] [src: grep "@Table" em model/]
- Datas são texto ISO (`LocalDateTimeStringConverter`), e valores monetários `numeric`. [src: common/converter/LocalDateTimeStringConverter.kt]

## Migrations
Sufixo `V0xx__descricao.sql`; `data.sql` semeia dados iniciais (inserção só se a tabela estiver vazia). Ordem de inicialização: o bean manual `flyway` migra e depois executa `data.sql`; o `entityManagerFactory` depende do bean `flyway`, e `spring.sql.init.mode` é `never` de propósito. [src: src/main/resources/data.sql] [src: config/FlywayConfig.kt] [src: src/main/resources/application.yml]

## PII
Dados financeiros pessoais, locais. Credenciais de IA ficam em `AiCredentialStore`; valores não são copiados aqui. [src: ai/credentials/AiCredentialStore.kt]

## Atualização 2026-11-01: investimentos
- `Asset` (@MappedSuperclass: name, symbol único, type, currentQuantity, currentUnitValue, averageUnitValue); `Ticker` acrescenta lastUpdate, isArchived, domain (site/logo). [src: model/investment/Asset.kt] [src: model/investment/Ticker.kt]
- `TickerPriceHistory(ticker, priceDate, closingPrice, isMonthEnd)`; `InvestmentPerformanceSnapshot(referenceMonth, investedValue, portfolioValue, accumulatedCapitalGains, monthlyCapitalGains)`. [src: model/investment/]
- `FundamentalAnalysis` (tabela `fundamental_analysis`, único por ticker+período, `data_json` TEXT). [src: model/investment/FundamentalAnalysis.kt]
- DTOs: `InvestmentPerformanceDTO`, `TickerPerformanceDTO`, `ProfitabilityMetricsDTO`, `AllocationDTO`. [src: model/dto/]
- `Dividend` é gerido por `TickerService`; não existe `DividendService`. [src: service/investment/TickerService.kt:333]

## Atualização 2026-11-01 (intent fundamentals-and-portfolio-views, commitado em f426f361)
- `V035__create_benchmark_price_history.sql` cria `benchmark_price_history` (entidade `BenchmarkPrice`, repositório `BenchmarkPriceRepository`): fechamento diário do Ibovespa (`^BVSP`) para a comparação de desempenho. [src: db/migration/V035__create_benchmark_price_history.sql] [src: model/investment/BenchmarkPrice.kt]
- `TickerPriceHistory` agora também guarda série diária de 12 meses (`TickerPriceHistoryService.syncDaily`), além do fim de mês. [src: service/investment/TickerPriceHistoryService.kt]
- `FundamentalAnalysis.data_json` passa ao schema v2 com `reason` por métrica (por que é N/A); lido por `FundamentalParser` em `FundamentalSnapshot`. [src: service/investment/FundamentalParser.kt] [src: model/dto/FundamentalSnapshot.kt]
- DTOs novos: `BenchmarkSeriesDTO`/`DividendPanelDTO`/`ContributionDTO`, `TickerRowDTO`/`TickerDetailDTO` (com `TickerPositionDTO` e `TickerIncomeDTO`), `RebalanceDTO`, `FundamentalScoring` (veredito e nota de qualidade). [src: model/dto/]

## Atualização 2026-11-06 (intent yfinance-resilience, UNIT-3)
- (SUBSTITUÍDO pela atualização seguinte: a coluna é `ticker.metadata` e não repete preço, site nem data) `ticker.market_data` (TEXT, JSON, V037): registro agregado de dados de mercado do ticker (`schema_version`, `price`, `currency`, `website`, `updated_at`; chaves desconhecidas são preservadas). `current_unit_value`, `last_update` e `domain` continuam como colunas espelhadas; o único escritor é `TickerMarketDataService.record`. [src: V037__add_ticker_market_data.sql] [src: TickerMarketDataService.kt]

## Atualização 2026-11-06 (revisão do `ticker.metadata`)
- A V037 cria `ticker.metadata` (JSON, nulo até o primeiro uso) e guarda só o que não tem coluna: `schema_version`, `currency`, `history_updated_at` e chaves futuras (preservadas em `TickerMetadata.other`). Preço, momento e site seguem em `current_unit_value`, `last_update` e `domain`; o JSON não repete esses dados. [src: V037__add_ticker_metadata.sql] [src: TickerMetadata.kt]

## Atualização 2026-11-07 (intent 261106-ai-chat-history): conversas com o assistente
- `V038__create_ai_chat_history.sql` cria 4 tabelas: `ai_chat` (título, criação, última atividade), `ai_chat_message` (papel, `content` enviado ao modelo, `display_text`, nomes de anexos, chamadas de ferramenta em JSON, tempo de resposta; único por `chat_id`+`position`), `ai_chat_change_set` (lote de propostas ligado à mensagem de resposta) e `ai_chat_change` (cada proposta: operação, payload original e atual em JSON, selo, estado, mensagem, data da decisão). [src: db/migration/V038__create_ai_chat_history.sql] [src: model/ai/]
- Único escritor: `AiChatHistoryService`; o JSON dos payloads é feito por `ChatPayloadCodec`. Detalhes em `areas/ai-chat-history.md`. [src: service/ai/AiChatHistoryService.kt] [src: ai/history/ChatPayloadCodec.kt]
- Preferências novas (fora do banco): `ai.chat.sidebarCollapsed`, `ai.history.maxChats`, `ai.history.maxDays`. [src: service/PreferencesService.kt]
- O texto dos anexos (extratos) fica em `ai_chat_message.content`: é dado financeiro pessoal no banco local, por decisão do dono. [user]

## Atualização 2026-11-07: migrations V035–V039
V035 `benchmark_price_history` (série do Ibovespa); V036 `notification.action_url`; V037 `ticker.metadata` (JSON para o que não tem coluna: moeda, momento da sincronização de histórico etc.; preço, momento e site seguem em `current_unit_value`, `last_update` e `domain`, escritos só por `TickerMarketDataService`); V038 `ai_chat`, `ai_chat_message`, `ai_chat_change_set`, `ai_chat_change` (só `AiChatHistoryService` grava); V039 limpa o cache `investment_performance_snapshot` para ser reconstruído com as fórmulas corrigidas. Entidade nova do domínio de investimentos: nenhuma além das de IA; `model/dto` ganhou `BondDetailDTO`, `BondComparisonDTO`, `BondRedemptionEstimateDTO`, `InterestRecalculationRowDTO`, `QuantityIntegrityIssueDTO`, `TickerMetadata`. [src: db/migration/V035–V039] [src: CLAUDE.md "Database"]
