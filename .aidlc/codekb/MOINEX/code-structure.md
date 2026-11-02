# Code structure — MOINEX

Status: human-reviewed (2026-11-01, delta pós v2.20.2) · Confidence: high

## Mapa (profundidade ≤3)
```
src/main/kotlin/org/moinex/
  app/          App → JavaFXApp → SpringApp, AppStartupRunner, ScreenshotRunner, CapturePlan
  config/       AppConfig, AppDispatchers, FlywayConfig, RetryConfig, BalanceCalculationConfig, AiPromptProperties
  ai/           chat, credentials, ingest (CSV/PDF), provider (Anthropic/Gemini/OpenAI/OpenRouter), tools
  service/      27 arquivos por domínio (wallet, creditcard, goal, investment, networth, ...)
  repository/   36 repositórios Spring Data JPA
  model/        36 @Entity + 30 enums + dto
  ui/main/      19 controllers das telas
  ui/dialog/    78 controllers de diálogo (create/update/view/base por domínio)
  ui/common/    painéis, células reutilizáveis e search/ (busca global)
  common/       chart/ (6 gráficos custom + ChartFactory), ui/ (ThemeColors, Icons, SegmentedFilter, RowContextMenu, ...), constant/ (Files, Styles, TranslationKeys), util/, extension/, helper/, retry/
src/main/resources/
  ui/{main,dialog,common}/*.fxml   (93 arquivos)
  css/{theme,common,component,scene}/   (31 arquivos; tokens.dark/light + base.css em theme/)
  icon/ (5 subpastas de PNG remanescentes: app, common, crc_operator, sidebar, wallet_type; demais ícones são Ikonli)   i18n/   db/migration/ (34)   scripts/ (6 Python)
src/test/kotlin/org/moinex/   service/, model/, ai/, factory/, common/ui/, ui/theme/, ui/common/search/, app/  (128 arquivos; nenhum teste de controllers)
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
| Rodar com dados fictícios | `make demo`; capturas com `scripts/capture-screens.sh <dir>` (home falso, não toca `~/.moinex`) |

## Atualização 2026-11-01 (intent fundamentals-and-portfolio-views, não commitado)
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
