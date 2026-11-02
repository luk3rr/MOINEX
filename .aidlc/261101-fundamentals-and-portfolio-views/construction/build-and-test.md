# Build and Test — fundamentals-and-portfolio-views

Data: 2026-11-01T17:23:15-03:00 · HEAD de partida `c72d94af` (v2.20.2) · JDK 25 · sem commits (tudo no working tree, a pedido do dono)

## Resultado

| Verificação | Comando | Resultado |
|---|---|---|
| Build completo, lint, formatação, testes e cobertura | `./gradlew clean build jacocoTestReport` | **BUILD SUCCESSFUL** (49 s) |
| Testes Kotlin | idem | **1537 testes, 0 falhas, 0 erros, 0 ignorados** (baseline em `c72d94af`: 1411) |
| Testes Python (contrato do script) | `/usr/bin/python3 -m unittest discover -s src/test/python -v` | **36 testes OK** (baseline: não existiam) |
| Cobertura de instruções | JaCoCo | **27,3%** (baseline 25,7%) |
| Comando de verificação aprovado | `./gradlew ktlintCheck spotlessCheck test` + testes Python | verde |

Nenhuma falha pré-existente. Os testes foram contados nos XML de `build/test-results/test`.

## Teste de ponta a ponta com uma cópia do banco de uso real

O app inteiro foi iniciado sob `xvfb` com `-Duser.home` temporário e uma **cópia** do banco de uso real. O arquivo original **não foi modificado**.

- Migration **V035** aplicada na cópia ("now at version v035"), Hibernate validou o esquema.
- A sincronização de inicialização rodou contra Yahoo e BACEN sem nenhum erro (`grep " ERROR "` vazio): preços diários, fechamentos do Ibovespa e meses de IPCA gravados.
- As telas Ações e Fundos, Detalhe do ativo e Desempenho foram capturadas sobre esses dados reais.

### Defeitos que só apareceram com dados reais (todos corrigidos e cobertos por teste)

1. **Variação do dia** usava o último preço salvo mesmo velho (uma variação de dezenas de %) e dava 0,00% aos domingos. Agora só vale um fechamento de até 5 dias, e sem sessão hoje vale a última sessão guardada.
2. **Aba Desempenho abria em 3M** em vez de 12M: o `SegmentedControl` reseleciona o primeiro segmento quando é exibido, o que numa aba ainda não aberta acontece depois da seleção. Corrigido no `SegmentedFilter` (seleciona de novo quando o controle ganha skin).
3. **Operações em dias sem pregão** (fim de semana ou feriado) mudavam a quantidade no pregão seguinte sem registrar o dinheiro movimentado, e apareciam como ganho ou perda. Cada operação agora vale no primeiro pregão em ou depois da data.
4. **Carteira com compras grandes sobre um valor pequeno**: o retorno do dia dividia o lucro por um valor anterior muito pequeno quando havia compras grandes no mesmo dia, e uma diferença de poucos reais entre o preço pago e o fechamento virava dezenas de %, com um buraco no gráfico. O lucro do dia agora é dividido pelo capital empregado no dia (valor anterior mais o que foi comprado), e a curva ficou na escala dos benchmarks.

Além disso, antes do teste com a base real, a base fictícia já tinha revelado: sobreposição do cabeçalho do diálogo fundamentalista, corrida de carregamento no detalhe do ativo e triângulos de marcador pretos.

## Matriz requisito → teste

| Requisito | Testes |
|---|---|
| FR-1 períodos vazios do yfinance | Python `EmptyOldestPeriodTest` (falha no script de `c72d94af`, confirmado) |
| FR-2 piso do crescimento e margem de segurança | Python `GrahamFairValueTest` |
| FR-3 LPA coerente (e VPA) | Python `EpsResolutionTest` |
| FR-4 conversão de moeda | Python `CurrencyConversionTest` (taxa simulada; na prática a VALE3 reporta em BRL) |
| FR-5 motivo do N/A | Python `MissingMetricReasonTest`; Kotlin `FundamentalParserTest`, `FundamentalReasonTextTest` |
| FR-6 contrato do JSON | toda a suíte Python (36 testes) |
| FR-7, FR-8 semáforo e nota | `FundamentalScoringServiceTest`, `FundamentalInsightTextTest` |
| FR-9 FII e ETF | Python `FundAndEtfTest`; `FundamentalParserTest`; `FundamentalAnalysisServiceGetAnalysisTest` (ETF válido) |
| FR-10 comparação com o setor | `FundamentalScoringServiceTest` (mediana, mínimo de 3 pares) |
| FR-11 sparkline | `SparklineGeometryTest` |
| FR-12, FR-14 retorno, peso, DY, filtros e ordenação | `PortfolioAnalyticsServiceTest` |
| FR-13 detalhe do ativo | `PortfolioAnalyticsServiceTest` (`buildDetail`) |
| FR-15 série diária | `TickerPriceHistoryServiceDailySyncTest` |
| FR-16 benchmarks | `PortfolioPerformanceCalculatorTest`, `PerformanceBenchmarkServiceTest`, `BenchmarkServiceTest`, `MarketIndicatorServiceEnsureHistoryTest` |
| FR-17 contribuição | `PortfolioPerformanceCalculatorTest` (`contributions`) |
| FR-18 dividendos | `PortfolioAnalyticsServiceTest` (`buildDividendPanel`) |
| FR-19 rebalanceamento | `PortfolioAnalyticsServiceTest` (`rebalance`, `topPerformers`) |

Os gráficos, células e diálogos JavaFX não têm teste automatizado (o projeto não tem testes de UI); a verificação deles é a captura nos dois temas (`construction/screenshots/`) e o teste com a cópia da base real. `CapturePlanTest` garante que os 26 alvos de captura continuam consistentes.

## NFR

| NFR | Situação |
|---|---|
| NFR-1 abrir Ações e Fundos com 50 ativos em ≤ 1 s | Os preços de todos os ativos vêm de **uma consulta**; medida: a consulta única levou poucos milissegundos com milhares de linhas diárias. O tempo total da tela não foi cronometrado de ponta a ponta (a captura inclui carregamento do app); é uma estimativa conservadora. |
| NFR-2 falha do Yahoo não apaga dados | `syncDaily` e `syncIbovespa` só gravam (upsert); testes de falha isolada por ativo e de falha de download sem escrita. |
| NFR-3 tema e identidade | Capturas nos dois temas; cores por tokens `theme-*` ou `ThemeColors` (com teste de paridade contra os tokens). |
| NFR-4 i18n | Chaves novas só acrescentadas nas duas `.properties` (pt-BR e en; 2 mensagens de tipo inválido foram editadas); testes verificam que os textos existem nos dois bundles. As capturas foram só em pt-BR, então o inglês não foi visto na tela. |
| NFR-5 testes unitários | +126 testes Kotlin e +36 Python. |
| NFR-6 migration aditiva, base real intocada | V035 aditiva, aplicada numa cópia; o original não foi modificado. |
| NFR-7 acessibilidade | sinais + e −, rótulos de texto nos selos e legendas; a cor nunca é o único indicador. |

## Riscos residuais

- **Cálculos de retorno com poucos dados**: em uma carteira com posições pequenas compradas quase todas no mesmo dia, o percentual do gráfico é fiel ao método, mas fica ruidoso quando o capital era minúsculo.
- **Uma tabela nova** (`benchmark_price_history`) e a **regra relaxada** de taxa negativa em `MarketIndicatorHistory`: decisões fora do desenho inicial, descritas no resumo da UNIT-9.
- **Python fora do CI**: os 36 testes Python rodam só localmente; incluí-los no pipeline exige sua decisão (M-005).
- **Inglês sem captura**, **sem teste de UI**, e **margem de segurança** exibida como número bruto (−827% em casos extremos).
- Um banco existente não tem as séries diárias, o Ibovespa nem os motivos de N/A até o app ser aberto (sincronização na inicialização) e as análises serem atualizadas.

## Rodada 2: ajustes pedidos após a revisão (2026-11-01T18:35:06-03:00)

Verificação: `./gradlew ktlintCheck spotlessCheck test` verde com **1545 testes Kotlin** (0 falhas) e 36 Python; capturas em `construction/screenshots/round2-*`.

1. **Tela de Investimentos lenta.** Medido com uma cópia de um banco real (JVM fria): abrir a tela levava **~1,2 s** (≈ 0,3 s para carregar o FXML das 4 abas e ≈ 0,9 s de CSS e layout). Causas: as 4 abas eram construídas ao abrir, inclusive as escondidas, e a tela de Ações e Fundos repetia a mesma leitura do banco 4 vezes na thread da interface. Correções: as abas carregam na primeira vez em que são mostradas; a tela de Ações e Fundos lê tudo **uma vez em segundo plano** (`portfolioSnapshot`) e filtra, ordena e busca em memória; "Melhores e Piores" na Visão Geral deixou de ler a série de preços. Resultado medido do mesmo jeito: **~0,22 s** (0,15 s de FXML e 0,08 s de CSS/layout) e 10 a 25 ms por aba aberta depois. O seu disco e a sua máquina podem dar números maiores, mas a proporção deve se manter.
2. **Gráficos interativos.** Novo `ChartCrosshair`: linha vertical tracejada que acompanha o mouse e "gruda" no dia mais próximo, ponto colorido em cada série e uma caixa com a data e os valores (carteira, CDI, Ibovespa e IPCA na aba Desempenho; preço, preço médio e as operações do dia no detalhe do ativo). Os triângulos de compra e venda passaram para a mesma camada. Os gráficos de linha deixaram de criar um nó por ponto (menos memória e CSS). A cota "do dia" cai no pregão seguinte quando não há pregão. Lógica de busca coberta por `CrosshairMathTest`.
3. **Pilha de dividendos com tooltip** em cada parte: ativo, mês, valor, percentual do mês e total do mês, no mesmo formato dos gráficos empilhados de Cartões.
4. **Análise fundamentalista:** os dois cartões novos agora têm as mesmas bordas esquerda e direita do cartão de cima; o tooltip do Valuation foi quebrado em linhas; o do cartão de Qualidade explica a nota e cada uma das 4 dimensões tem tooltip próprio dizendo o que mede, o que é 100 e o que é 0 (as faixas vêm das regras de `FundamentalScoringService`). Os tooltips reais foram fotografados para conferir a largura.

## Rodada 3: tooltips por ícone, carregamento e cartões de resumo (2026-11-01T18:48:01-03:00)

Verificação: `./gradlew ktlintCheck spotlessCheck test` verde com 1545 testes Kotlin e 36 Python; capturas em `construction/screenshots/round3-*`.

- **Ícone "i"** (`InfoIcon`, discreto, mais claro ao passar o mouse): os tooltips deixaram de estar no cartão inteiro. Agora ficam no ícone do canto de cada cartão de Valuation e Qualidade, em cada um dos 4 indicadores de qualidade e em cada card das abas.
- **Carregamento** agora fica sobre a área das abas (`StackPane` com o `TabPane`); os cartões de resumo não se mexem e a área das abas não encolhe durante a atualização (conferido clicando em Atualizar Dados na captura).
- **Cartão de Qualidade:** a nota ("58 / 100") e os 4 indicadores ficam centralizados na altura que sobra, e os dois cartões têm a mesma largura.
- **Decisão de qualidade em selo**, igual à do Valuation: Fraca (vermelho), Regular (âmbar), Boa (verde suave) e Excelente (verde cheio); a nota virou número grande ao lado.

## Rodada 4: cards das abas com fundo e borda

Os cards das abas não tinham fundo nem borda (o estilo `pane.css` só vale para o id `#root`). Agora usam `.metric-card` em `fundamental-analysis.css`: fundo `theme-bg-card`, borda `theme-border` e cantos de 6 px, o mesmo visual dos cards de Valuation e Qualidade. Comparei com uma variação de fundo `theme-bg-pane` sem borda: ficou mais destacada no escuro, mas um bloco cinza chapado no claro. Captura em `construction/screenshots/round4-metric-cards-*`. Para trocar de variação basta editar a regra `.metric-card`.

Dois detalhes de método: no FXML as classes de estilo se separam por **vírgula** (`styleClass="pane, metric-card"`; com espaço o JavaFX cria uma classe só), e o CSS no `stylesheets` do próprio nó raiz vale para os filhos, não para ele mesmo.

## Rodada 5: abas Preço e Renda no detalhe do ativo

O detalhe do ativo ganhou duas abas, como no mockup m5b. **Preço** mantém o gráfico e as operações e ganha uma linha de posição (valor, peso na carteira, faixa de 12 meses e distância do topo). **Renda** mostra recebido em 12 meses, recebido no total, último provento, yield sobre o custo e o gráfico de proventos só daquele ativo. `TickerDetailDTO` passou a trazer `position` e `income`, calculados em `PortfolioAnalyticsService.buildDetail` (testado). A seleção 3M/6M/12M vale só para o gráfico de preço. Valem para ações, FIIs, ETFs e cripto, pois todos usam o mesmo diálogo. Capturas: `construction/screenshots/round5-*`; novo alvo de captura 30 (`ticker-detail-income`).

Também: capturas de validação passam a ficar em `construction/screenshots/` (M-009); corrigido o histórico de rendimentos de títulos (campos `editButton`/`resetButton` inexistentes) e o respiro de 10 px no topo da aba Desempenho.
