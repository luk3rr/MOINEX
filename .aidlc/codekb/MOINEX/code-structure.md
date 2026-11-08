# Code structure — MOINEX

Status: human-reviewed (2026-11-07) · Confidence: high

## Mapa (profundidade ≤3)
```
src/main/kotlin/org/moinex/
  app/          App → JavaFXApp → SpringApp, AppStartupRunner, ScreenshotRunner, CapturePlan
  config/       AppConfig, AppDispatchers, FlywayConfig, RetryConfig, BalanceCalculationConfig, AiPromptProperties
  ai/           chat, credentials, ingest (CSV/PDF), provider (Anthropic/Gemini/OpenAI/OpenRouter), tools (leitura + propose_changes), operation (framework de operações por domínio), review, prompt, history, audit
  service/      54 arquivos por domínio (wallet, creditcard, goal, investment, networth, ai, matching, release, ...)
  repository/   41 arquivos (Spring Data JPA; `repository/ai/` novo)
  model/        42 arquivos com @Entity (inclui `model/ai/`), 28 arquivos de enums, dto, SemanticVersion
  ui/main/      21 arquivos
  ui/dialog/    79 arquivos de diálogo (create/update/view/base por domínio)
  ui/common/    painéis, células reutilizáveis e search/ (busca global)
  common/       chart/ (6 gráficos custom + ChartFactory), ui/ (ThemeColors, Icons, SegmentedFilter, RowContextMenu, ...), constant/ (Files, Styles, TranslationKeys), util/, extension/, helper/, retry/
src/main/resources/
  ui/{main,dialog,common}/*.fxml   (95 arquivos)
  css/{theme,common,component,scene}/   (38 arquivos; tokens.dark/light + base.css em theme/)
  icon/ (5 subpastas de PNG remanescentes: app, common, crc_operator, sidebar, wallet_type; demais ícones são Ikonli)   i18n/   db/migration/ (39)   scripts/ (6 Python + yf_resilience.py compartilhado)
src/test/kotlin/org/moinex/   service/, model/, ai/, factory/, common/ui/, ui/theme/, ui/common/search/, app/  (204 arquivos .kt; nenhum teste de controllers) · src/test/python/ (6 arquivos, 118 testes)
```
[src: find em src/main e src/test]

## Pontos de entrada
`App.kt:12 main()` → `JavaFXApp` → `SpringApp.start()`. [src: app/]

## Onde adicionar
| O quê | Onde |
|---|---|
| Nova tela principal | FXML em `resources/ui/main/`, controller em `ui/main/`, registrar botão em `MainController`, constantes em `Files.kt` |
| Novo diálogo | FXML em `resources/ui/dialog/<domínio>/`, controller em `ui/dialog/<domínio>/{create,update,view}`, abrir com `WindowUtils.openModalWindow` |
| Novo estilo | classe em `css/component|scene`, constante em `Styles.kt` / `Files.kt`, cores via tokens |
| Nova string | chave em `messages_en.properties` e `messages_pt_BR.properties` + `TranslationKeys.kt` |
| Nova tabela | migration `V0xx__*.sql` + `@Entity` + repository (Hibernate só valida) |

## Evitar
`build/`, `.gradle/`, `.aidlc/`. Constantes de caminho de CSS/FXML ficam centralizadas em `common/constant/Files.kt` (210+ linhas). [src: common/constant/Files.kt]

## Atualização 2026-11-01: onde mexer neste intent
| O quê | Onde |
|---|---|
| Dados fundamentalistas | `src/main/resources/scripts/get_fundamental_data.py` → `common/util/APIUtils.kt` → `service/investment/FundamentalAnalysisService.kt` → tabela `fundamental_analysis` |
| Tela de análise fundamentalista | `ui/dialog/investment/view/FundamentalAnalysisController.kt` + `ui/common/FundamentalMetricPaneController.kt` + `css/scene/fundamental-*.css` |
| Ações e fundos | `ui/main/SavingsStocksFundsController.kt` + `resources/ui/main/savings_stocks_funds.fxml` |
| Visão geral | `ui/main/SavingsOverviewController.kt` + `savings_overview.fxml` |
| Gráficos | `common/chart/*` + `ChartFactory`; cores via `theme-chart-*` ou `ThemeColors` |
| Rodar com dados fictícios | `make demo`; capturas com `scripts/dev/capture-screens.sh <dir>` (home falso, não toca `~/.moinex`) |

## Atualização 2026-11-01 (intent fundamentals-and-portfolio-views, commitado em f426f361)
| Assunto | Onde |
|---|---|
| Números da carteira (linhas da tabela, detalhe, proventos, rebalanceamento, maiores variações) | `service/investment/PortfolioAnalyticsService.kt` |
| Retorno diário da carteira e contribuição por ativo | `PortfolioPerformanceCalculator.kt` + `PerformanceBenchmarkService.kt` (CDI/IPCA via `MarketIndicatorService.ensureHistory`, Ibovespa via `BenchmarkService`) |
| Veredito de valuation (±20%) e nota de qualidade (pesos 35/25/20/20) | `FundamentalScoringService.kt`; leitura do JSON em `FundamentalParser.kt` |
| Aba Desempenho | `ui/main/SavingsPerformanceController.kt` + `ui/main/savings_performance.fxml` (carregamento preguiçoso das abas em `SavingsController`) |
| Detalhe do ativo (abas Preço e Renda) | `ui/dialog/investment/view/TickerDetailController.kt` + `ticker_detail.fxml` + `css/scene/ticker-detail.css` |
| Rebalanceamento | `RebalanceController.kt` + `rebalance.fxml` |
| Gráficos novos | `ui/common/`: `BenchmarkChart`, `TickerPriceChart`, `DividendChart`, `DividendHistoryChart`, `ContributionBars`; `common/ui/`: `ChartCrosshair`, `CrosshairMath`, `SparklineCell`, `SparklineGeometry` |
| Cartões de valuation/qualidade e ícone (i) | `ui/common/FundamentalInsightsView.kt`, `FundamentalInsightText.kt`, `FundamentalReasonText.kt`, `InfoIcon.kt` |
| Testes dos scripts Python | `src/test/python/` (36 testes, fora do Gradle e do CI) |
| Capturas de tela | `app/CapturePlan.kt` (30 alvos) + `app/ScreenshotRunner.kt` |

## Atualização 2026-11-07 (v2.21.0 → v2.25.0): onde mexer
| Assunto | Onde |
|---|---|
| Acesso ao Yahoo (limite de 2 simultâneas, cooldown 5→60 min, exceção `YahooCooldownException`) | `common/util/YahooGateway.kt`, `APIUtils.withoutYahooCooldown { }`, `service/investment/YahooCooldown*.kt`; detalhes em `areas/market-data.md` |
| Preço/moeda/histórico/site de um ticker (um escritor só) | `service/investment/TickerMarketDataService.kt`, coluna JSON `ticker.metadata` (`model/dto/TickerMetadata.kt`) |
| Assistente de IA: operações por domínio, revisão, auditoria | `ai/operation/**` (registry, `ChangeSet`, `DraftSpec`), `ai/tools/ProposeChangesTool.kt` + `ReadTools.kt`, `ai/review/ReviewSession.kt`, `ai/audit/ChangeSetAuditLogger.kt`; ver `areas/ai-agent.md` |
| Histórico de conversas do assistente | `service/ai/AiChatHistoryService.kt`, `ai/history/ChatPayloadCodec.kt`, `model/ai/*`, `repository/ai/*`; ver `areas/ai-chat-history.md` |
| Casamento determinístico de lançamentos duplicados | `service/matching/` (`TransactionMatchingService`, `DescriptionSimilarity`) |
| Renda fixa: detalhe, comparação, juros | `service/investment/Bond*.kt`, `ui/dialog/investment/view/` (detalhe), `common/ui/Bond*.kt`; ver `areas/bond-screens.md` e `areas/financial-calculations.md` |
| Aviso de nova versão (GitHub Releases) | `service/release/` + `Notification.action_url` (V036) |
| Pacotes Linux, scripts por plataforma | `scripts/linux/`, `scripts/windows/`, `packaging/linux/Moinex.desktop`, `common/util/EmbeddedPython.kt` |
| ER diagram gerado | `scripts/dev/generate_erd.py` → `docs/database-erd.md` (`make erd`) |
| Crosshair nos gráficos de FIRE e patrimônio | `common/ui/ChartCrosshair.kt`, `CrosshairMath.kt`, `common/chart/NetWorthLineChart.kt` |
