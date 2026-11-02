# Tasks — fundamentals-and-portfolio-views

## 2.9 Delivery plan

### Verification command

Proposto a partir do repositório (aprovação no gate 2.9):

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
./gradlew ktlintCheck spotlessCheck test && /usr/bin/python3 -m unittest discover -s src/test/python -v
```

- Parte Kotlin: lint, formatação e testes (baseline em HEAD: 1411 testes, 0 falhas). [src: codekb/quality-and-risks.md]
- Parte Python: testes de contrato do script com fixtures, sem rede, em `src/test/python/` (criado na UNIT-1). Usa o `/usr/bin/python3` que o app também usa. Fora do CI por enquanto; incluir no `pipeline.yml` exige sim explícito (M-005).
- Validação visual por Unit: `CAPTURE_ONLY=<alvo> CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>` (dados fictícios, tema escuro e claro) e conferência do dono. Alvos novos em `CapturePlan.kt` quando houver tela nova.

### Regras do plano (valem para toda Unit)

- Tudo direto no `main`, um commit pequeno por Unit (ou por parte coerente), só depois de o dono ver o diff; Conventional Commits; `Co-Authored-By` permitido, sem link de sessão. [user] [mem: M-004]
- Sem comentários `//` no Kotlin; KDoc ok. `.properties` só com chaves acrescentadas (pt-BR e en); se a edição falhar, pedir ao usuário. [src: CLAUDE.md]
- O banco de uso real é apenas leitura; nenhum teste, captura ou comando grava nele. [user]
- Cada Unit termina com: verificação verde, capturas nos dois temas (se UI) e checklist de telas para o dono. [mem: M-003]

### Bolts

| Bolt | Units | Definition of Done |
|---|---|---|
| BOLT-1 Dados e N/A | UNIT-1, UNIT-2 | PETR4, VALE3, ITUB4 e WEGE3 com CAGR, valor justo e margem coerentes; LPA e moeda corretos; teste de contrato verde; cards nunca somem e mostram motivo e data |
| BOLT-2 Fundamentalista | UNIT-3, UNIT-4 | Cabeçalho com semáforo e nota; comparação com setor; FII e ETF com abas próprias; textos pt-BR e en |
| BOLT-3 Ações e Fundos | UNIT-5, UNIT-6, UNIT-7, UNIT-8 | Série diária 12m persistida sem alterar os snapshots mensais; tabela com retorno, peso, DY, filtros e sparkline; detalhe do ativo; NFR-1 conferido com 50 ativos |
| BOLT-4 Visão Geral | UNIT-9 a UNIT-12 | Aba Desempenho (benchmarks, contribuição, dividendos); selo de desvio e rebalanceamento na Visão Geral |

Walking skeleton: UNIT-1 (fatia mínima ponta a ponta; FR-1 e FR-2 já no working tree).

### Tasks

Convenção: `T-n.m [ ]` · FR/US servidos · teste.

#### UNIT-1 · Dados do script (FR-1, FR-2, FR-3, FR-4, FR-6 · US-1, US-2, US-4)
- [x] T-1.1 Rever o diff já no working tree (`drop_empty_periods`, piso do crescimento, margem só com valor justo > 0) e criar `src/test/python/` com fixtures do yfinance 1.7.0 (coluna final vazia). FR-1, FR-2, FR-6 · teste: CAGR e valor justo não nulos com fixture; falha se a coluna vazia voltar.
- [x] T-1.2 LPA coerente: `eps_source` e troca por `Diluted EPS` quando divergir > 25%. FR-3 · teste: PETR4 (10,49 vs 1,52) troca; ITUB4 (4,14 vs 4,01) mantém.
- [x] T-1.3 Conversão de moeda `financialCurrency` → `currency`, com `fx_rate` e `fx_date` e falha tratada. FR-4 · teste: fixture VALE3 em USD, taxa mockada.
- [x] T-1.4 Rodar o script real (rede) para PETR4, VALE3, ITUB4, WEGE3 e comparar com os valores esperados; registrar em `construction/`. FR-1..4 · evidência manual.
- [x] T-1.5 Verificação (comando acima) e diff para o dono.

**Resumo UNIT-1 (verificada em 2026-11-01):**
- Arquivos: `src/main/resources/scripts/get_fundamental_data.py` (períodos vazios, piso do crescimento, margem só com valor justo > 0, `resolve_eps`, `statement_eps`, `statement_book_value_per_share`, `get_fx_rate`, `scale_monetary_rows`, campos `eps_source`, `financial_currency`, `fx_rate`, `fx_date` em `valuation`); novo `src/test/python/test_get_fundamental_data.py` (15 testes); `construction/unit-1-real-run.md`.
- Como rodar: `/usr/bin/python3 -m unittest discover -s src/test/python -v`.
- Resultado: `./gradlew ktlintCheck spotlessCheck test` BUILD SUCCESSFUL; 15 testes Python OK; os testes de CAGR e valor justo falham no script antigo (confirmado).
- Desvios: (1) FR-3 foi estendido: ao trocar o LPA, o VPA passa a ser patrimônio / ações diluídas, senão o Graham Number da PETR4 continuava errado; (2) FR-4: a VALE3 reporta em BRL no Yahoo, então a conversão de moeda não tem caso real hoje e fica como proteção para ativos com moedas diferentes; (3) para período trimestral o LPA do balanço é a soma de 4 trimestres, só quando todos existem.


#### UNIT-2 · Motivo do N/A (FR-5 · US-3)
- [x] T-2.1 Script: `reason` por métrica sem valor, métrica nunca omitida, `schema_version = 2`, `asset_kind`. FR-5 · teste Python por motivo.
- [x] T-2.2 `FundamentalParser` e modelos (C2), tolerantes a cache antigo. FR-5 · teste Kotest com JSON novo e antigo.
- [x] T-2.3 `FundamentalAnalysisController`/`FundamentalMetricPaneController`: card sempre presente, motivo traduzido, data de referência. FR-5, NFR-7 · teste do mapeamento reason → chave i18n; captura nos dois temas.
- [x] T-2.4 Chaves i18n pt-BR/en. NFR-4.
- [x] T-2.5 Verificação e diff.

**Resumo UNIT-2 (verificada em 2026-11-01):**
- Arquivos: `get_fundamental_data.py` (`METRIC_SPECS`, `missing_reason`, `placeholder_metric`, `fill_missing_metrics`, `schema_version = 2`, `asset_kind`; P/L nulo com LPA ≤ 0 e ROE nulo com patrimônio ≤ 0); `model/dto/FundamentalSnapshot.kt`; `service/investment/FundamentalParser.kt`; `ui/common/FundamentalReasonText.kt`; `FundamentalMetricPaneController.kt` e `FundamentalAnalysisController.kt` (card sempre presente, motivo e data); `css/scene/fundamental-metric-pane.css`; `TranslationKeys.kt` e as duas `.properties` (6 chaves, só acrescentadas); `CapturePlan.kt`, `ScreenshotRunner.kt` e `resources/capture/fundamental-analysis-sample.json` (alvos de captura 19 e 20 do diálogo); testes `FundamentalParserTest`, `FundamentalReasonTextTest`, `CapturePlanTest` ajustado, e 9 testes novos de motivos em `src/test/python/`.
- Como ver: `CAPTURE_ONLY=fundamental-valuation,fundamental-growth CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/`.
- Resultado: `./gradlew ktlintCheck spotlessCheck test` BUILD SUCCESSFUL (1424 testes Kotlin, 0 falhas); 24 testes Python OK.
- Desvios: (1) P/L com LPA ≤ 0 e ROE com patrimônio ≤ 0 passaram a ser nulos com motivo, em vez de números sem sentido (−40 e −392% apareciam antes); (2) a captura do diálogo semeia uma análise de exemplo no banco fictício da captura (nunca no real) e o setor aparece "N/A" porque a análise de exemplo não grava setor; (3) análises salvas antes desta versão (schema 1) continuam abrindo, mas mostram "Dado indisponível" até um refresh.


#### UNIT-3 · Semáforo, nota e setor (FR-7, FR-8, FR-10 · US-5, US-6)
- [x] T-3.1 `FundamentalScoringService`: `valuation`, `quality`, `sectorMedian` (C3, C4). FR-7, FR-8, FR-10 · teste: faixas do semáforo, reponderação sem dimensão, < 3 pares.
- [x] T-3.2 (feito na rodada 5, com cópia do banco de uso real; ajuste: setor financeiro sem dívida e caixa) Faixas de subnota ajustadas com ativos reais lidos do banco real em modo somente leitura (com aval do dono antes de abrir). A11 · evidência manual.
- [x] T-3.3 Cabeçalho do diálogo (semáforo e nota com composição) e comparação com o setor nos cards. M5 · captura nos dois temas.
- [x] T-3.4 i18n e verificação e diff.

**Resumo UNIT-3 (verificada em 2026-11-01):**
- Arquivos: `model/dto/FundamentalScoring.kt` (veredito, nota, comparação com o setor); `service/investment/FundamentalScoringService.kt` (`valuation`, `quality`, `labelFor`, `sectorComparisons`, `median`); `ui/common/FundamentalInsightsView.kt` e `FundamentalInsightText.kt` (cartões Valuation e Qualidade); `FundamentalMetricPaneController.kt` (linha "Setor: mediana (n pares)"); `FundamentalAnalysisController.kt` (calcula os insights em segundo plano e mostra os cartões); `fundamental_analysis.fxml` (caixa `insightsBox`, altura mínima 659 → 780); `fundamental-analysis.css` (cartões e selos com tokens do tema); 17 chaves i18n em pt-BR e en (só acrescentadas); `CapturePlan` (alvo 21) e `ScreenshotRunner` (semeia 4 análises de exemplo no banco fictício da captura); testes `FundamentalScoringServiceTest` e `FundamentalInsightTextTest`.
- Como ver: `CAPTURE_ONLY=fundamental-valuation,fundamental-profitability CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/unit-3-*`.
- Resultado: `./gradlew ktlintCheck spotlessCheck test` BUILD SUCCESSFUL (1449 testes Kotlin, 0 falhas); 24 testes Python OK.
- Desvios: (1) a dimensão Caixa usa FCF/lucro e FCF yield, porque a margem de FCF não existe no JSON (C4 previa a margem); (2) o cartão de Valuation não repete o Graham Number, que continua na aba Valuation; (3) o diálogo ficou mais alto (780 px) para caber os cartões; (4) a margem de segurança pode ser muito negativa (−827% na PETR4), exibida como está; o veredito usa só as faixas; (5) o cartão não aparece para FII/ETF (UNIT-4).
- Bug de layout achado pela captura e corrigido: os cartões cobriam o fim do cabeçalho do diálogo.


#### UNIT-4 · FII e ETF (FR-9 · US-6)
- [x] T-4.1 Script: `asset_kind`, seção `income`, `price_to_book`, `dividends_12m`; sem erro "Financial data not available" para fundos. FR-9 · teste Python com fixtures de FII e ETF.
- [x] T-4.2 Diálogo: abas Preço, Renda e Valor para FII/ETF e aviso. M5b · captura nos dois temas.
- [x] T-4.3 Teste real FIIA11.SA e BOVA11.SA (rede); i18n; verificação e diff.

**Resumo UNIT-4 (verificada em 2026-11-01):**
- Arquivos: `get_fundamental_data.py` (`classify_asset`, `monthly_dividends`, `build_fund_result`, `fund_missing_reason`, `FUND_METRIC_SPECS`; fundos e ETFs deixam de cair em "Financial data not available" e ganham `income` e `price_to_book`); `FundamentalSnapshot.kt` e `FundamentalParser.kt` (seção `income` e histórico de proventos); `FundamentalAnalysisService.kt` (ETF vira tipo válido); `FundamentalAnalysisController.kt` (abas Desempenho de Preço, Renda e Valor para fundos e aviso no lugar dos cartões); `DividendHistoryChart.kt`; `FundamentalInsightsView.kt` (`notice`); i18n (9 chaves novas e 2 mensagens de tipo inválido editadas em pt-BR e en); `CapturePlan` (alvo 22) e `ScreenshotRunner`; testes Python (12 novos), `FundamentalParserTest` e `FundamentalAnalysisServiceGetAnalysisTest` (ETF).
- Como ver: `CAPTURE_ONLY=fundamental-fund-income CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/unit-4-*`.
- Dados reais (rede): FIIs trouxeram DY e, em geral, P/VP; um deles sem P/VP no Yahoo; ETFs de índice sem proventos e ETFs que pagam proventos com DY calculado; ações seguem como empresa.
- Desvios: (1) FIIB11 (um FII) não tem preço nem proventos no Yahoo, então continua com "Financial data not available" em vez de virar um ETF vazio; (2) a captura usa um ticker fictício (MGLU3) como hospedeiro do JSON de fundo; (3) o gráfico de proventos fica abaixo dos cards e exige rolar o diálogo; (4) a verificação com uma cópia de banco real confirmou: análises ANNUAL antigas de ações tinham valor justo e CAGR nulos, e vários FIIs caíam em "Financial data not available".


#### UNIT-5 · Série diária (FR-15 · US-8)
- [x] T-5.1 `get_stock_price_history.py --daily` com `is_month_end` no último pregão do mês. FR-15 · teste Python.
- [x] T-5.2 `TickerPriceHistoryService.syncDaily` (incremental, upsert, falha por ativo não bloqueia os demais, cache nunca apagado) e chamada na atualização de preços. FR-15, NFR-2 · teste Kotest com MockK e H2.
- [x] T-5.3 Teste de regressão de `InvestmentPerformanceService` e snapshots mensais com linhas diárias presentes. · teste Kotest.
- [x] T-5.4 Verificação e diff.

**Resumo UNIT-5 (verificada em 2026-11-01):**
- Arquivos: `TickerPriceHistoryService.kt` (`syncDailyPriceHistory`, `syncDaily`, `getDailyPrices`; janela de 12 meses, incremental quando a série está densa, falha por ativo isolada, upsert sem apagar nada); `TickerPriceHistoryRepository.kt` (`findLatestPriceByTicker`, `countByTickerIdAndDateRange`, `findByTickerIdAndDateRange`); `AppStartupRunner.kt` (sincroniza depois do histórico existente, na mesma etapa); `SavingsStocksFundsController.kt` (o botão Atualizar Preços também sincroniza a série diária); teste novo `TickerPriceHistoryServiceDailySyncTest` (9 cenários).
- Resultado: `./gradlew ktlintCheck spotlessCheck test` BUILD SUCCESSFUL; 36 testes Python OK.
- Desvios: (1) o modo diário NÃO exigiu flag nova no script: `get_stock_price_history.py` já devolve todos os pregões quando não recebe datas específicas, então o contrato C5 (`--daily`) ficou desnecessário; (2) o fim de data enviado é amanhã, porque o yfinance trata o fim como exclusivo e assim o dia de hoje entra; (3) `is_month_end` só vale para meses já encerrados, como no fluxo antigo, então `InvestmentPerformanceService` e os snapshots mensais enxergam as mesmas linhas de fim de mês (não há migration); (4) a verificação de regressão de `InvestmentPerformanceService` com linhas diárias foi por raciocínio sobre as consultas (`findMostRecentPriceBeforeDate` e `findMonthEndPricesByTickerAndDateRange`), não por teste de integração com banco, porque o projeto não tem testes JPA.


#### UNIT-6 · Tabela: retorno, peso, DY, filtros (FR-12, FR-14 · US-7)
- [x] T-6.1 `PortfolioAnalyticsService.tickerRows` e DTOs (retorno, lucro, peso, DY sobre custo, variação do dia). FR-12 · teste Kotest.
- [x] T-6.2 Colunas novas e ordenáveis em `SavingsStocksFundsController` e FXML; `SegmentedFilter` de tipo; ordenação por retorno, peso, dia. FR-12, FR-14, NFR-1 · captura nos dois temas; medição com 50 ativos fictícios.
- [x] T-6.3 i18n, verificação e diff.

**Resumo UNIT-6 (verificada em 2026-11-01):**
- Arquivos: `model/dto/TickerRowDTO.kt` (`TickerRowDTO`, `TickerSort`, `DailyPricePointDTO`); `service/investment/PortfolioAnalyticsService.kt` (`tickerRows`, `buildRows`, `filterAndSort`); `TickerPriceHistoryService.getPricePoints` e a consulta em lote no repositório; `SavingsStocksFundsController.kt` e `savings_stocks_funds.fxml` (filtro de tipo e de ordenação como `SegmentedFilter`, colunas Retorno, Dia, Peso e DY s/ Custo ordenáveis também pelo cabeçalho); 9 chaves i18n; teste novo `PortfolioAnalyticsServiceTest` (retorno, peso, DY sobre custo, variação do dia, filtro e 4 ordenações).
- Como ver: `CAPTURE_ONLY=savings-stocks-funds CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/unit-6-*`.
- NFR-1: a tela lê os preços de todos os ativos em uma única consulta; medida com milhares de linhas diárias numa cópia de um banco real, a consulta levou poucos milissegundos.
- Achado com dados reais (cópia de um banco real): a variação do dia usava o último preço salvo mesmo quando ele era de semanas atrás (aparecia uma variação de dezenas de %). Agora só vale um fechamento de até 5 dias antes; senão mostra N/A. Teste adicionado.
- Desvios: (1) o filtro de tipo ficou com 6 segmentos (Todos, Ações, FIIs, ETFs, Fundos, Criptomoedas), mais que os 2 a 3 dos outros filtros; (2) a tela passou a ter 13 colunas, com cabeçalhos cortados em alguns casos (Quantidade Possuída); (3) enquanto a série diária não for sincronizada na sua base, a coluna Dia aparece como N/A para quase todos; (4) a busca por texto continua no controller, só tipo e ordenação foram para o serviço.


#### UNIT-7 · Sparkline (FR-11 · US-7)
- [x] T-7.1 `SparklineCell` (Canvas, `ThemeColors`) e coluna Tendência 12m; "—" sem série. FR-11, ADR-7 · teste da normalização dos pontos; captura nos dois temas.
- [x] T-7.2 Leitura das séries em lote (sem N+1) para a tabela. NFR-1 · teste de consulta.
- [x] T-7.3 Verificação e diff.

**Resumo UNIT-7 (verificada em 2026-11-01):**
- Arquivos: `common/ui/SparklineGeometry.kt` (pontos e tendência, sem JavaFX), `common/ui/SparklineCell.kt` (célula com Canvas, verde se subiu e vermelho se caiu no período, traço se faltam dados); `ThemeColors.kt` (positivo e negativo, com teste de paridade contra os tokens); `SavingsStocksFundsController.kt` (coluna "12 meses" ordenável pela variação do período, 100 px fixos); `PortfolioAnalyticsService.kt` (variação do dia reescrita); `scripts/generate_dummy_data.py` (agora gera um ano de fechamentos diários por ticker ativo, o que também vale para `make demo` e para as capturas); testes `SparklineGeometryTest` e `ThemeColorsTest` ampliado, e `PortfolioAnalyticsServiceTest` ajustado.
- Como ver: `CAPTURE_ONLY=savings-stocks-funds CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/unit-7-*`.
- Leitura em lote: a série vem da consulta única da UNIT-6, sem N+1; NFR-1 conferido lá.
- Achado com dados reais: a variação do dia dava 0,00% aos domingos e feriados (comparava o preço com o fechamento do próprio último pregão). Agora, sem sessão hoje, vale a variação da última sessão guardada; testes de fim de semana, defasagem e lacuna adicionados.
- Desvios: (1) cabeçalhos "12 meses" e "DY/Custo" abreviados porque a tabela já tem 14 colunas; (2) a coluna de tendência é fixa em 100 px; (3) a cor de tendência segue primeiro e último preço do período, não máximos e mínimos; (4) a série sintética usada na cópia da base real não vai para o repositório.


#### UNIT-8 · Detalhe do ativo (FR-13 · US-8)
- [x] T-8.1 `TickerDetailController` e FXML: gráfico de preço 3M/6M/12M, linha do preço médio, marcadores de compra e venda com tooltip. FR-13, M2 · captura nos dois temas.
- [x] T-8.2 Abertura por duplo clique, menu de contexto e busca global (`ScreenActionProvider`). FR-13 · teste do catálogo de ações.
- [x] T-8.3 Verificação e diff.

**Resumo UNIT-8 (verificada em 2026-11-01):**
- Arquivos: `model/dto/TickerRowDTO.kt` (`TickerDetailDTO`, `TradeMarkerDTO`, `TradeSide`); `PortfolioAnalyticsService.kt` (`tickerDetail`, `buildDetail`, janela de 3, 6 ou 12 meses, compras e vendas em ordem); `ui/dialog/investment/view/TickerDetailController.kt` e `ticker_detail.fxml` (cabeçalho com preço e variação do dia, quantidade, preço médio, retorno, seletor 3M/6M/12M, gráfico, tabela de operações e botão de análise fundamentalista); `ui/common/TickerPriceChart.kt` (linha de preço, linha tracejada do preço médio, triângulos verdes de compra e vermelhos de venda com dica); `css/component/ticker-price-chart.css`; abertura por duplo clique, menu de contexto e busca global (ação de tela) em `SavingsStocksFundsController.kt`; 14 chaves i18n; `CapturePlan` (alvo 23) e `ScreenshotRunner` (captura de diálogo modal extraída para um helper comum); testes de `buildDetail` em `PortfolioAnalyticsServiceTest`.
- Como ver: `CAPTURE_ONLY=ticker-detail CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/unit-8-*`.
- Bugs achados pela captura e corrigidos: (1) condição de corrida: o `SegmentedControl` reseleciona 3M ao abrir e a resposta atrasada de 3M sobrescrevia a de 12M; agora respostas antigas são descartadas; (2) o `XYChart` sobrescreve as classes CSS do nó de marcador, então o triângulo saía preto; agora ele vai dentro de um `Group`.
- Desvios: (1) o botão de análise fundamentalista só aparece para tipos válidos (ação, FII e ETF); (2) a tabela de operações mostra todas as do período, mais recentes primeiro; (3) o gráfico usa o preço da operação, não o fechamento do dia, então um marcador pode ficar fora da linha; (4) sem série diária mostra um aviso para usar Atualizar Preços; (5) o gráfico em si não tem teste automatizado, só captura.


#### UNIT-9 · Aba Desempenho: benchmarks (FR-16 · US-9)
- [x] T-9.1 Ingestão do Ibovespa (`^BVSP`) em `market_indicator_history` como `IBOVESPA`, com KDoc da exceção do nome do campo (ADR-5). FR-16 · teste Kotest.
- [x] T-9.2 `PortfolioAnalyticsService.benchmarkSeries` (carteira, CDI, Ibovespa, IPCA, normalizadas). FR-16 · teste Kotest com séries fixas.
- [x] T-9.3 Aba Desempenho (`savings_performance.fxml`, `SavingsPerformanceController`), filtro de período, gráfico via `ChartFactory`, registro em `Savings` e alvo em `CapturePlan`. FR-16, ADR-6 · captura nos dois temas.
- [x] T-9.4 i18n, verificação e diff.

**Resumo UNIT-9 (verificada em 2026-11-01):**
- Arquivos: migration `V035__create_benchmark_price_history.sql`, `BenchmarkPrice` e `BenchmarkPriceRepository`, `BenchmarkService` (sincroniza o Ibovespa, `^BVSP`, 12 meses, incremental); `MarketIndicatorService.ensureHistory` (CDI e IPCA mesmo sem títulos que os usem); `MarketIndicatorHistory` (taxa negativa agora aceita); `PortfolioPerformanceCalculator` (retorno ponderado pelo tempo da carteira, CDI diário, IPCA mensal, níveis do Ibovespa); `PerformanceBenchmarkService`; `BenchmarkSeriesDTO`; `BenchmarkChart`, `SavingsPerformanceController`, `savings_performance.fxml`, `benchmark-chart.css`; aba Desempenho como 2ª aba de Savings (`savings.fxml`, `SavingsController`), com ação na busca global (24 ações) e índices de abas ajustados em `ScreenActionEntries`; `AppStartupRunner` (atualiza benchmarks na inicialização); i18n (10 chaves); `CapturePlan` (alvo 24); testes `PortfolioPerformanceCalculatorTest`, `PerformanceBenchmarkServiceTest`, `BenchmarkServiceTest`, `MarketIndicatorServiceEnsureHistoryTest`.
- Como ver: `CAPTURE_ONLY=savings-performance CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/unit-9-*`.
- Desvios importantes: (1) **houve migration** (V035), contrariando o ADR-5: o Ibovespa não cabe em `market_indicator_history` porque a coluna é o enum `InterestIndex`, que também alimenta os combos de títulos; a tabela nova é aditiva. (2) **mudei uma regra existente**: `MarketIndicatorHistory` rejeitava taxa negativa, o que fazia a sincronização do IPCA inteira falhar em mês de deflação (−0,32% em agosto); o teste antigo que exigia o erro foi trocado por um que aceita a taxa negativa. (3) períodos 3M, 6M e 12M, sem "Desde o início" do mockup, porque a série diária guarda só 12 meses. (4) a carteira da linha é só renda variável de tickers não arquivados com histórico diário. (5) o serviço novo é `PerformanceBenchmarkService`, não `benchmarkSeries` dentro de `PortfolioAnalyticsService` como no contrato. (6) a aba é a 2ª, então Ações e Fundos virou a 3ª e Renda Fixa a 4ª (índices atualizados). (7) o IPCA conta um mês só depois que ele termina; meses que começaram antes do primeiro dia do período não entram.
- Correções posteriores (estágio 3.6, com uma cópia de um banco real): `SegmentedFilter` passou a selecionar de novo quando o controle ganha skin (a aba abria em 3M); operações em dias sem pregão passaram a valer no pregão seguinte; o retorno do dia passou a ser dividido pelo capital empregado no dia (antes aparecia um buraco no gráfico). Detalhes em `construction/build-and-test.md`.

#### UNIT-10 · Contribuição (FR-17 · US-9)
- [x] T-10.1 `contribution(period)` com realizado e dividendos (C4). FR-17 · teste: soma bate com o Total.
- [x] T-10.2 Gráfico de barras com Total, tooltip e rótulo dos componentes. · captura nos dois temas; i18n; verificação e diff.

**Resumo UNIT-10 (verificada em 2026-11-01):**
- Arquivos: `PortfolioPerformanceCalculator.contributions`, `ContributionDTO`, `PerformanceBenchmarkService.contributions` (e `loadPositions` compartilhado com a linha da carteira); `ui/common/ContributionBars.kt`, `contribution-bars.css`; a aba Desempenho ganhou o cartão "Contribuição por ativo" à direita (`savings_performance.fxml`, `SavingsPerformanceController`); 4 chaves i18n; 5 testes novos no `PortfolioPerformanceCalculatorTest`.
- Como ver: `CAPTURE_ONLY=savings-performance CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/unit-10-*`.
- Desvio importante em relação ao contrato C4: a contribuição de cada ativo é o **lucro no período escolhido (3M, 6M ou 12M)**: valor final menos valor inicial, menos o que foi comprado (vendas contam como dinheiro retirado), mais os proventos pagos no período, tudo pela série diária. O contrato falava em lucro não realizado de sempre mais realizado e proventos, o que misturava janelas e não bateria com o gráfico ao lado. Como consequência o Total não é o card "Lucro/Prejuízo" da tela de Ações e Fundos (que é desde a compra), e a nota no cartão explica o cálculo.
- Outros: (1) barras são linhas de HTML-like em JavaFX (`Region`), não um gráfico de cascata; a ordem é do maior lucro para a maior perda; (2) tickers com contribuição menor que R$ 0,005 não aparecem; (3) o `ScrollPane` cobre listas longas.


#### UNIT-11 · Dividendos (FR-18 · US-10)
- [x] T-11.1 `monthlyDividends` com média e projeção = soma de 12 meses. FR-18 · teste Kotest.
- [x] T-11.2 Barras mensais empilhadas por ativo, linha da média e card "Projeção 12m (estimativa)". · captura nos dois temas; i18n; verificação e diff.

**Resumo UNIT-11 (verificada em 2026-11-01):**
- Arquivos: `PortfolioAnalyticsService.buildDividendPanel` e `dividendPanel`, `DividendPanelDTO` e `DividendSeriesDTO`; `ui/common/DividendChart.kt` (barras empilhadas por mês, uma série por ticker, legenda à direita), `dividend-chart.css`; painel "Dividendos (12 meses)" embaixo da aba Desempenho com média mensal e projeção (`savings_performance.fxml`, `SavingsPerformanceController`); 6 chaves i18n; `scripts/generate_dummy_data.py` agora também gera proventos fictícios (alimenta `make demo` e as capturas); 5 testes novos em `PortfolioAnalyticsServiceTest`.
- Como ver: `CAPTURE_ONLY=savings-performance CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/unit-11-*`.
- Regras: janela de 12 meses de calendário terminando no mês atual; até 6 ativos com série própria (os maiores pagadores) e o resto somado em "Outros"; média mensal = total dos 12 meses dividido por 12; projeção = o total dos 12 meses repetido, rotulada como estimativa e com nota no tooltip (decisão do dono no gate 2.8).
- Desvios: (1) cores das séries escolhidas entre os tokens `theme-chart-series-*` com matizes bem separados (00, 02, 07, 12, 17, 21, 23), pois as primeiras séries do token são dois verdes e dois azuis quase iguais; (2) o painel soma os proventos de todos os tickers, inclusive arquivados; (3) o mês corrente aparece mesmo sem provento; (4) não há teste do gráfico em si, só captura.


#### UNIT-12 · Rebalanceamento e alertas (FR-19 · US-10)
- [x] T-12.1 `rebalance(contribution)` (C4). FR-19 · teste: sem aporte, com aporte só compras, sem metas.
- [x] T-12.2 Selo de desvio no plano de alocação e botão Rebalancear (oculto sem metas); `RebalanceController` (M4). FR-19 · captura nos dois temas.
- [x] T-12.3 Bloco "Piores" lista só retornos negativos. · teste de `calculateTopPerformers` ou extração testável.
- [x] T-12.4 i18n, verificação final de todo o intent e diff.

### Assumptions & open questions
- A13: o teste Python roda só localmente; incluí-lo no CI é decisão futura do dono. [inferred]

**Resumo UNIT-12 (verificada em 2026-11-01):**
- Arquivos: `PortfolioAnalyticsService.rebalance` (compras e vendas sugeridas por tipo, com ou sem aporte) e `topPerformers` (melhores só com ganho e piores só com perda, agora testáveis fora da tela), `model/dto/RebalanceDTO.kt`; `ui/dialog/investment/view/RebalanceController.kt` e `rebalance.fxml` (campo de aporte, tabela Tipo, Atual, Meta, Desvio e Ação sugerida, nota); botão "Rebalancear" ao lado de "Editar" no plano de alocação, oculto quando não há metas (`savings_overview.fxml`, `SavingsOverviewController`); 13 chaves i18n; `CapturePlan` (alvo 25) e `ScreenshotRunner` (abre o diálogo clicando no botão); 8 testes novos em `PortfolioAnalyticsServiceTest`.
- Como ver: `CAPTURE_ONLY=rebalance-dialog,savings-overview CAPTURE_LANGUAGE=pt CAPTURE_COUNTRY=BR bash scripts/capture-screens.sh <dir>`; capturas em `construction/screenshots/unit-12-*`.
- Regras do rebalanceamento: só entram os tipos com meta, e as metas são normalizadas pela soma; sem aporte, vende o excesso e compra a falta no mesmo valor; com aporte, só compra, repartido pelo tamanho da falta de cada tipo depois do aporte; não inclui impostos nem custos.
- Desvios: (1) o painel de alocação já mostrava o desvio como texto ("Muito acima em 45,30 %"), então o selo novo do mockup não foi criado; a entrega é o botão e o diálogo; (2) "Melhores" também passou a listar só ativos com ganho, para ficar simétrico com "Piores" (o pedido era só sobre os piores); (3) os percentuais do diálogo são os da tela (sobre o total, incluindo tipos sem meta), mas os valores sugeridos são calculados só sobre os tipos com meta; (4) a verificação visual do diálogo usou o clique real no botão; (5) um erro meu na posição do alvo de captura (inserido no meio da lista) foi pego pelo `CapturePlanTest` e corrigido.


