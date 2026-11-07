# Área: cálculos financeiros

Status: human-reviewed (2026-11-07, gate 2.1); mapa do estado ANTERIOR às correções, HEAD 6c94ec03. O estado atual e os vereditos estão em .aidlc/261107-financial-calculations-review/construction/audit-report.md · Confidence: alta no que o código faz, média nas suspeitas (marcadas `[inferred]`)

Adicionado no intent `261107-financial-calculations-review`. Mapeia onde cada cálculo financeiro mora e o que ele faz. Os veredictos (correto / com problema / ambíguo) ficam no `requirements.md` e no relatório do intent; aqui só há fatos do código e suspeitas a confirmar. Nenhum dado pessoal do dono entra neste arquivo (M-011).

## Cobertura desta passada

| Domínio | Arquivos | Lido nesta passada |
|---|---|---|
| Renda fixa (juros) | `service/investment/BondInterestCalculationService.kt`, `common/extension/InvestmentExtensions.kt`, `service/investment/MarketIndicatorService.kt`, `resources/scripts/get_market_indicator_history.py` | sim, completo |
| Renda fixa (posição) | `service/investment/BondService.kt` | sim |
| Renda variável (preço médio, lucro) | `service/investment/TickerService.kt`, `PortfolioAnalyticsService.kt`, `PortfolioPerformanceCalculator.kt` | parcial |
| Rentabilidade da tela de investimentos | `ui/main/SavingsOverviewController.kt` (`calculateProfitabilityMetricsByType`) | sim |
| FIRE | `service/financialplanning/FIRECalculatorService.kt` | sim |
| Metas | `ui/main/GoalController.kt`, `ui/common/GoalPaneController.kt`, `common/constant/Constants.kt` | sim (partes numéricas) |
| Resumo anual e insights | `service/summary/AnnualSummaryService.kt`, `FinancialInsightGenerator.kt` | só os pontos com divisão |
| Cartão de crédito | `service/creditcard/CreditCardService.kt` | só parcelas e abatimento proporcional |
| Planejamento (orçamento) | `service/financialplanning/FinancialPlanningService.kt` | só `calculateTargetAmount` |
| Patrimônio | `service/networth/NetWorthCalculationService.kt` | sim |
| Desempenho histórico | `service/investment/InvestmentPerformanceService.kt` | sim (cálculos mensais) |
| Saldo de carteira | `service/wallet/WalletService.kt` | sim (movimentação de saldo) |
| Recorrências | `RecurringTransactionService.kt`, `RecurringCreditCardDebtService.kt` | parcial: o serviço gera transações **pendentes** (`createPendingWalletTransaction`); a dívida recorrente de cartão não foi lida |
| Alocação, rebalanceamento | `AllocationDTO.kt`, `PortfolioAnalyticsService.rebalance` | sim |
| Score fundamentalista | `FundamentalScoringService.kt`, `resources/scripts/get_fundamental_data.py` | sim (fórmulas principais) |

## Renda fixa: juros mensais (`BondInterestCalculationService`)

Fatos do código:

- O juro do mês é calculado por períodos entre operações do mês (compra e venda) e fechado no fim do mês. O acumulado é a soma dos juros mensais; o valor final é `investido + acumulado`. [src: BondInterestCalculationService.kt:268-451, 835-866]
- Base de cada período: `quantidade × preço unitário da última compra`. Não há juros sobre juros: o acumulado não entra na base do mês seguinte. [src: BondInterestCalculationService.kt:525, 564]
- `FIXED`: fator anual `1 + taxa/100` elevado a `dias úteis / 252`, em `Double`. Os dias úteis são as linhas do indicador (`interestIndex`) no intervalo. [src: BondInterestCalculationService.kt:557-614]
- `FLOATING`: produto dos fatores diários `1 + (taxa diária do indicador + spread)/100`, onde "spread" é somado, não multiplicado. [src: BondInterestCalculationService.kt:653-663]
- O "spread" usado é o da última operação de compra; se nenhuma tiver, o estado inicial vale zero e a `interestRate` do título **nunca é usada**. [src: InvestmentExtensions.kt:51-68, BondInterestCalculationService.kt:1058-1061]
- `ZERO_COUPON`: `calculatePeriodInterest` devolve zero. [src: BondInterestCalculationService.kt:453-466]
- O intervalo é `BETWEEN` inclusivo nas duas pontas; períodos consecutivos dentro do mês compartilham a data da operação. [src: MarketIndicatorHistoryRepository.kt:36-40]
- Unidades das séries do BACEN: CDI (série 12) é taxa diária em %; Selic meta (série 432) é taxa anual em %; IPCA (série 433) é variação mensal em %. [src: get_market_indicator_history.py:19-26]
- Sem impostos (IR regressivo, IOF) nem taxas no cálculo; o juro é bruto. [src: BondInterestCalculationService.kt, bond_operation.taxes só em `netProfit` de venda]

Suspeitas a confirmar (`[inferred]`, viram requisitos no 2.3):

1. `interestRate` ignorada em título flutuante sem spread na operação: "110% do CDI" rende como 100% do CDI. O comportamento foi conferido contra o resultado armazenado no banco de uso real do dono (método: recomputar o juro mensal e comparar); detalhes não são registrados aqui.
2. Spread somado ao fator diário: um "CDI + 2% a.a." digitado como `2` produziria cerca de 2% ao dia. Selic (anual) em linhas diárias teria efeito igualmente absurdo; IPCA (mensal) seria somado por mês.
3. Dia da operação contado duas vezes quando há operação no meio do mês, por causa do `BETWEEN` inclusivo.
4. Base de juros usa o preço da última compra, não o custo médio ponderado; com preços unitários diferentes entre compras o resultado diverge.
5. Sem capitalização do acumulado: renda fixa real capitaliza.
6. `ZERO_COUPON` não rende nada.
7. Impostos não considerados: o "rendimento" mostrado é bruto.
8. Fixo calculado em `Double` (`pow`), perda de precisão sem importância prática, mas inconsistente com o resto em `BigDecimal`.

## Renda fixa: posição e rentabilidade (`BondService`)

- `getAverageUnitPrice` divide `totalValue / totalQuantity` sem escala nem `MathContext`: lança `ArithmeticException` quando a divisão não termina (ex.: 100 ÷ 3). Usa só as compras, ignorando vendas. [src: BondService.kt:341-345]
- `getTotalInvestedValue()` = preço médio × quantidade atual. Valor atual da renda fixa na tela de rentabilidade = investido + **juro acumulado desde o início**, sem descontar o juro já sacado em vendas. [src: SavingsOverviewController.kt:865-880, BondService.kt:376-380] `[inferred]` o juro acumulado fica inflado depois de vendas parciais; precisa decidir como a venda consome o juro.
- `getTotalInvestedValueByMonth` ignora meses com venda maior que compra (devolve zero). [src: BondService.kt:429-443]

## Renda variável (`TickerService`)

- Preço médio: média ponderada em `averageUnitValueCount`, arredondada a 2 casas. A contagem só cresce em compras e **nunca diminui em vendas**; logo, depois de vender e recomprar a outro preço, a média inclui unidades já vendidas. [src: TickerService.kt:733-753, 272]
- Arredondamento a 2 casas zera o preço médio de cripto com preço unitário abaixo de um centavo (o banco já guarda valores como 6,75e-06, vindos de outro caminho). `[inferred]`
- O padrão da entidade `Ticker` é `averageUnitValueCount = 1`, enquanto `Asset` usa zero. `[inferred]` uma unidade fantasma no peso da média quando o ticker é criado por esse construtor.

## Rentabilidade da tela de investimentos

- Variável: investido = `Σ preço médio × quantidade`, atual = `Σ quantidade × preço atual`, lucro = diferença, % = lucro/investido; só tickers não arquivados. Dividendos ficam fora do lucro e entram no dividend yield (`dividendos / investido`). [src: SavingsOverviewController.kt:817-870]
- Total = variável + fixa. [src: SavingsOverviewController.kt:885-893]
- Gráfico carteira × benchmark e tabela de contribuição: ver o intent `261101-fundamentals-and-portfolio-views` e `PortfolioPerformanceCalculator.kt`. Retorno composto dia a dia **sem dividendos**; a contribuição **com dividendos**. [src: PortfolioPerformanceCalculator.kt:49-110]

## FIRE (`FIRECalculatorService`)

- Meta = `gasto mensal × 12 / (taxa de retirada/100)`. [src: FIRECalculatorService.kt:79-82]
- Projeção: taxa mensal equivalente `(1+a)^(1/12) − 1`, valor futuro com aporte no fim do mês (anuidade ordinária), teto de 600 meses. Fórmula correta; o caso de taxa zero é linear. [src: FIRECalculatorService.kt:100-142]
- Premissas sem tratamento: retorno nominal e gasto constante (sem inflação); idade no FIRE usa divisão inteira de meses por 12. `[inferred]` decisões de modelo, não erros; confirmar com o dono.

## Metas (`GoalController`, `GoalPaneController`)

- Depósito mensal sugerido = `(meta − saldo) / meses até a data`, 2 casas, sem rendimento. Meses = `ChronoUnit.MONTHS.between`, inteiro truncado. Com 0 meses, sugere o valor inteiro. [src: GoalController.kt:493-507, 732-741, Constants.kt:192-195]
- Progresso = `saldo / meta` com **2 casas antes de multiplicar por 100**, ou seja, passos de 1 ponto percentual. [src: GoalController.kt:664, GoalPaneController.kt:221-225]

## Cartão de crédito (`CreditCardService`)

- Parcelas: `valor / n` com `RoundingMode.FLOOR` a 2 casas; o resto é guardado à parte (provavelmente somado a uma parcela). [src: CreditCardService.kt:825-832]
- Pagamento parcial distribuído por fração `restante / totalRestante` arredondada a **2 casas**; a última fatura absorve a diferença. Com várias faturas as frações perdem precisão e a última recebe a sobra. [src: CreditCardService.kt:485-503] `[inferred]` imprecisão de centavos até alguns reais.

## Resumo anual e insights

- Percentuais com 6 casas e saída a 2; média mensal = total / `monthCount`. `[inferred]` conferir `monthCount` no modo de período parcial. [src: AnnualSummaryService.kt:156-161, 267-272]
- Insights usam crescimento de renda e taxa de poupança por metades do período. [src: FinancialInsightGenerator.kt:83-153]

## Planejamento

- Valor-alvo de um grupo = `renda base × % alvo / 100`, arredondado. Sem falha aparente. [src: FinancialPlanningService.kt:191-198]

## Patrimônio (`NetWorthCalculationService`)

- Patrimônio do mês = ativos − passivos. Ativos = saldos positivos de carteiras + investimentos + receita recorrente; passivos = dívida de cartão + saldos negativos + despesa recorrente. [src: NetWorthCalculationService.kt:132-160]
- Meses passados: saldo atual revertido pelas transações, pagamentos de fatura e transferências posteriores ao mês. Mês atual: saldo atual mais pendentes e agendadas. Meses futuros: saldo atual mais recorrências futuras. [src: NetWorthCalculationService.kt:168-340]
- Investimento em renda variável: quantidade histórica × **preço atual** (não há preço histórico). [src: NetWorthCalculationService.kt:448-478]
- Renda fixa: `finalValue` do mês (investido + juro acumulado) quando existe cálculo; senão quantidade × último preço de compra. [src: NetWorthCalculationService.kt:480-515]
- Recorrências com data final entram como ativo/passivo em **todo mês** em que há parcela, inclusive meses passados e o mês atual. [src: NetWorthCalculationService.kt:358-419]

Suspeitas (`[inferred]`):

1. Histórico de renda variável muda quando o preço de hoje muda, porque usa o preço atual em todos os meses.
2. Recorrência de mês passado ou corrente pode ser contada duas vezes: a transação gerada (pendente e depois confirmada) já entra no saldo da carteira e a mesma parcela entra de novo como ativo/passivo; no mês atual as pendentes ainda são projetadas sobre o saldo. [src: RecurringTransactionService.kt:254-266, NetWorthCalculationService.kt:215-235, 358-419]
3. Valor da renda fixa herda o problema do juro acumulado depois de vendas (ver seção de renda fixa).

## Desempenho histórico de investimentos (`InvestmentPerformanceService`)

Fatos do código:

- Ganho mensal = dividendos + lucro líquido das vendas de renda fixa + lucro das vendas de ações (`preço − custo médio da venda`) + apreciação do mês + juros mensais da renda fixa. [src: InvestmentPerformanceService.kt:200-243]
- Apreciação do mês: soma, por período entre operações, de `(preço final − preço inicial) × quantidade no fim do período`. [src: InvestmentPerformanceService.kt:376-416]
- Valor investido por mês usa o preço médio **atual** do ativo × quantidade do mês. [src: InvestmentPerformanceService.kt:174-198]
- Ganho acumulado = ganho não realizado (valor no fim do mês − custo com preço médio atual) + dividendos até o mês + juro acumulado da renda fixa. [src: InvestmentPerformanceService.kt:245-310]
- Juro acumulado da renda fixa no ganho acumulado: pega o **último** registro entre todos os títulos, não a soma por título. [src: InvestmentPerformanceService.kt:296-301]

Suspeitas (`[inferred]`):

1. Ganho mensal conta duas vezes o ganho realizado: a apreciação já reconhece a alta mês a mês e a venda soma `preço − custo` de novo; o mesmo vale para o juro da renda fixa mais o lucro líquido da venda.
2. A apreciação usa a quantidade do **fim** do período no período inteiro: compra feita no fim do período valoriza retroativamente, venda faz o contrário.
3. Ganho acumulado não inclui o lucro realizado das vendas de ações, então não é a soma dos ganhos mensais.
4. Com mais de um título de renda fixa o acumulado do `maxByOrNull` devolve só um título. Hoje inócuo se há um único título.
5. `getAllNonArchivedSales` num ponto e `getAllSales` em outro; ativos arquivados somem de parte dos números.
6. Valor investido e custo usam o preço médio de hoje em meses passados.

## Alocação e rebalanceamento

- Rebalanceamento só considera tipos com meta; sem aporte vende o excesso e compra a falta; com aporte distribui só compras proporcionalmente à falta. Impostos e custos ignorados. Contas fecham (deltas somam zero). [src: PortfolioAnalyticsService.kt:259-318]
- `getAchievementPercentage` = atual / meta. [src: AllocationDTO.kt:30-36]
- Linhas de ativo: rentabilidade e DY sobre o custo da posição atual; os dividendos dos últimos 12 meses incluem pagamentos de quando a quantidade era outra. `[inferred]` aproximação aceitável, só documentar.

## Score fundamentalista e valuation

- Veredito: margem = `(valor justo − preço) / valor justo`; barato ≥ 20%, caro ≤ −20%. [src: FundamentalScoringService.kt:valuation]
- Valor justo = Graham revisado quando existe, senão número de Graham `√(22,5 × LPA × VPA)`. [src: get_fundamental_data.py:409-452]
- Graham revisado: `LPA × (8,5 + 2g) × 4,4 / Y` com `Y` fixo em 10% e `g` = **CAGR da receita** limitado a 0–25%. [src: get_fundamental_data.py:428-452, 1185-1189]
- Score de qualidade = média ponderada de rentabilidade (35), dívida (25), crescimento (20) e caixa (20), por faixas; bancos/seguradoras excluem dívida e caixa. [src: FundamentalScoringService.kt:quality]
- ROIC: `EBIT × (1 − alíquota) / (dívida + patrimônio − caixa)`, alíquota padrão 34% ou `|imposto / EBIT|`. [src: get_fundamental_data.py:253-275, 1074-1079]

Suspeitas (`[inferred]`):

1. `Y = 10%` fixo destoa de uma Selic bem maior: o valor justo fica inflado. Premissa a confirmar.
2. Graham usa crescimento de **lucro** esperado; aqui entra o crescimento da receita.
3. CAGR com `n_anos = len(períodos) − 1` só vale para períodos anuais; no modo trimestral o expoente fica errado.
4. Margem líquida pontuada com as faixas do ROE. Decisão de modelo, não erro.
5. Alíquota efetiva usa imposto sobre EBIT, não sobre lucro antes do imposto.

## Saldo de carteira (`WalletService`)

- Incremento e decremento de saldo, edição de valor, status e transferência, com carteira virtual refletida no saldo da carteira mestre. A movimentação confere (sinais corretos em edição de valor, mudança de status e troca de carteira em transferência). Sem achado. [src: WalletService.kt:573-802]

## Cartão: achado adicional

- A fração `restante / totalRestante` a 2 casas pode superar a realidade: com faturas de valores muito diferentes (ex.: uma grande e duas de R$ 1), um pagamento parcial quase total paga uma parcela **acima do próprio valor** e deixa as outras sem pagar. [src: CreditCardService.kt:485-501] `[inferred]`, reprodutível com teste.
- Dívida em uma data = dívidas criadas − pagamentos pagos (valor cheio, incluindo o abatimento) − pagamentos pendentes dos meses até a data, quando futura. Coerente com o uso no patrimônio. [src: CreditCardService.kt:604-631]

## Perguntas em aberto

- Como o dono cadastra renda fixa flutuante na prática (percentual do CDI ou CDI + taxa)? Decide o modelo certo do spread.
- A renda fixa deve considerar imposto (IR regressivo/IOF) e capitalização?
- Como uma venda de renda fixa consome principal e juro?
- A dívida recorrente de cartão (`RecurringCreditCardDebtService`) não foi lida; entra na leitura do Unit de patrimônio e cartão.
- Qual taxa de juros longa usar no Graham (hoje 10% fixa)?

## Depois do intent 261107-financial-calculations-review

As suspeitas acima foram tratadas nos Units 1 a 6 (corrigidas, documentadas ou levadas ao dono). Pontos que mudaram de arquitetura: juro da renda fixa por `BondAccrualCalculator` (saldo capitalizado), imposto estimado por `BondTaxEstimator`, desempenho histórico por `InvestmentPerformanceCalculator`, repartição de pagamento de fatura por `InvoicePaymentDistribution`, verificação de integridade por `QuantityIntegrityService`, `InterestType.FLOATING_SPREAD`, migração `V039` (apaga o cache de desempenho). Ver o relatório de auditoria para o estado de cada cálculo.
