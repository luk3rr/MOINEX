# Área: telas de renda fixa e gráficos de comparação

Status: draft (2026-11-07, HEAD 55d07233) · Confidence: alta no código, média nas decisões de interface

Adicionado no intent `261107-bond-detail-comparison`. Mapeia o que existe hoje para a tabela de títulos, os diálogos por título e os gráficos de benchmark, que a feature de detalhe e comparação vai estender. Nenhum dado pessoal do dono entra aqui (M-011).

## Tela principal de títulos

- A aba é `ui/main/SavingsBondsController.kt` com `resources/ui/main/savings_bonds.fxml`: quatro cartões (total investido, valor atual, lucro e juros recebidos) e uma tabela `TableView<Bond>` com 13 colunas, busca por texto e filtro de tipo. [src: SavingsBondsController.kt:63-150]
- As colunas são criadas por `createColumn(chave) { texto }` com um extrator por linha; as de quantidade e preço unitário se escondem sozinhas quando todos os títulos listados são só em reais (`BondTableColumns`). [src: SavingsBondsController.kt:350-410, common/ui/BondTableColumns.kt]
- O clique direito numa linha usa `RowContextMenu.install(tabela, listaDeAcoes)`, hoje com editar, apagar, comprar, resgatar e histórico de juros; as ações da tela (registrar título, arquivados, operações) vêm de `screenActions()`. [src: common/ui/RowContextMenu.kt, SavingsBondsController.kt:96-125]
- Cada linha chama `BondService` célula a célula (quantidade, preço médio, investido, juro acumulado); há dois caches de juro por título. [src: SavingsBondsController.kt:412-440]

## Diálogos por título

| Diálogo | Arquivo | O que faz |
|---|---|---|
| Histórico de juros | `ui/dialog/investment/view/BondInterestHistoryController.kt` | tabela mensal por título, editar juro de um mês, voltar ao automático, recalcular com antes e depois, juro líquido estimado |
| Operações | `BondTransactionsController.kt` | operações de **todos** os títulos (colunas por operação, busca) |
| Arquivados | `ArchivedBondsController.kt` | títulos arquivados, desarquivar |
| Compra e resgate | `create/AddBondPurchaseController.kt`, `AddBondSaleController.kt` | valor em reais para CDB, LCI e LCA, resgate pelo valor bruto com cálculo automático |

- Todos abrem por `WindowUtils.openModalWindow(fxml, título, springContext, { controller -> ... }, listaDeCallbacks)`. [src: SavingsBondsController.kt:327-350]

## Gráficos e dados

- `ui/common/BenchmarkChart.kt` é um `LineChart` de **quatro séries fixas** (carteira, CDI, Ibovespa, IPCA) com cores no `benchmark-chart.css`; o cursor com valores é `ChartCrosshair.wrap(grafico, classe, series)`. Não aceita uma lista de séries de tamanho variável. [src: BenchmarkChart.kt]
- `BenchmarkSeriesDTO` tem os quatro campos fixos. [src: model/dto/BenchmarkSeriesDTO.kt]
- `SavingsPerformanceController` monta o período com `SegmentedFilter` (3M, 6M, 12M) e as caixas "Incluir dividendos" e "Incluir renda fixa". [src: ui/main/SavingsPerformanceController.kt]
- `PerformanceBenchmarkService.loadFixedIncome` (privado) calcula o valor diário de todos os títulos juntos com `BondAccrualCalculator.dailyValues` e os fluxos de compra e resgate; `PortfolioPerformanceCalculator.portfolioReturn` devolve o retorno acumulado % descontando fluxos; `cumulativeFromDailyRates` dá a linha do CDI. [src: PerformanceBenchmarkService.kt, PortfolioPerformanceCalculator.kt]
- Dados por título: `BondService.getOperationsByBond`, `getMonthlyInterestHistory`, `estimateNetInterest`, `isAmountOnly`; `BondInterestCalculationService` guarda o histórico mensal. [src: BondService.kt]

## O que isso significa para a feature

1. O gráfico de comparação **precisa de um componente novo ou de uma generalização do `BenchmarkChart`** para N linhas com paleta.
2. O retorno **por título** exige separar `loadFixedIncome` em uma função por título e chamar `portfolioReturn` com um só `FixedIncomePosition`.
3. A tabela já tem o mecanismo de menu de clique direito e de colunas por extrator, então a tabela enxuta e o item "Detalhes" são pequenos.
4. A janela de detalhe segue o padrão dos diálogos; o painel lateral seria um padrão novo (nenhuma tela hoje divide a área entre tabela e painel de detalhe de linha).
5. As operações de um título hoje só existem na lista de todos os títulos; o detalhe precisa de uma lista filtrada por título.

## Baseline

HEAD 55d07233: ktlintCheck, spotlessCheck e test com 2114 testes Kotlin, 0 falhas; Python 118 OK (resultado do commit anterior, árvore limpa).

## Perguntas em aberto

- O gráfico parte do início do período para todos os títulos, ou cada título parte da sua primeira compra?
- Títulos arquivados entram na comparação?
