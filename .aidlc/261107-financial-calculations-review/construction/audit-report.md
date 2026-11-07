# Relatório de auditoria dos cálculos financeiros

Intent `261107-financial-calculations-review` · HEAD de partida `6c94ec03` · escrito em 2026-11-07. O diff das correções está na árvore de trabalho, sem commit.

Este relatório não traz valores, posições nem datas do banco de uso real do dono (regra M-011). As conferências com dados reais seguiram um método: recalcular o resultado gravado com a fórmula do código e comparar.

## Resumo

| Veredito antes da revisão | Quantidade |
|---|---|
| Correto | 14 |
| Com problema ou ausente, corrigido neste intent | 26 |
| Ambíguo ou decisão de modelo (4 mantidos e documentados, 1 melhorado: a idade do FIRE) | 5 |
| Com problema, sem correção (decisão do dono) | 1 |
| Não conferido em profundidade | 2 |
| **Total de cálculos listados** | **48** |

Contagem feita sobre as tabelas abaixo; uma linha com dois vereditos (por exemplo "correto na base e sem capitalização") conta como "com problema".

## Renda fixa

| Cálculo | Veredito | Fórmula de referência | Código | Estado |
|---|---|---|---|---|
| Juro de título pós-fixado, percentual do indexador | **Com problema** (a taxa do título era ignorada quando a operação não tinha spread) | fator do dia = 1 + (índice/100) × (taxa/100) | `BondAccrualCalculator.rowFactor` | Corrigido (UNIT-1). `FLOATING` passa a significar % do indexador e os títulos existentes seguem valendo assim |
| Juro de título "indexador + taxa a.a." | **Com problema** (o spread era somado à taxa diária, o que daria cerca de 2% ao dia) | fator do dia = (1 + índice/100) × (1 + taxa/100)^(1/252) | idem | Corrigido: tipo novo `FLOATING_SPREAD` |
| Unidade de cada índice | **Com problema** (CDI diário, Selic anual e IPCA mensal eram tratados como diários) | CDI: diário; Selic: (1+v)^(1/252); IPCA: mensal | idem | Corrigido |
| Juro prefixado | Correto na base (252 dias úteis) e **sem capitalização** | (1 + taxa/100)^(1/252) por dia útil, sobre o saldo | idem | Corrigido: passa a capitalizar |
| `ZERO_COUPON` | **Com problema** (não rendia nada) | igual ao prefixado, no calendário do CDI se não houver índice | idem | Corrigido, sem teste dedicado (usa a mesma fórmula do prefixado) |
| Contagem de dias úteis | **Com problema** (o dia da operação entrava em dois períodos) | o dia da compra não rende; o do resgate rende | idem | Corrigido |
| Juros sobre juros | **Com problema** (base só do principal) | cada dia rende sobre principal + juro acumulado | idem | Corrigido |
| Acumulado do mês corrente | **Com problema** (cada atualização somava o mês de novo) | acumulado = acumulado do mês anterior + juro do mês − juro resgatado | `BondInterestCalculationService` | Corrigido (FR-28) |
| Resgate e juro realizado | **Com problema** (o juro acumulado nunca diminuía; o lucro era só digitado) | juro do resgate = juro retido × (principal resgatado ÷ principal retido) | `BondService.estimateRedemption` | Corrigido: sugestão editável no diálogo; resgates antigos com lucro 0 não mudam |
| Imposto de renda | **Ausente** (valor sempre bruto) | 22,5% até 180 dias, 20% até 360, 17,5% até 720, 15% acima; LCI e LCA isentos | `BondTaxEstimator` | Novo: juro líquido estimado no diálogo de histórico; IOF e taxa de custódia fora do escopo |
| Preço médio da renda fixa | **Com problema** (exceção quando a divisão não termina) | invested ÷ quantidade em custódia | `BondService.getAverageUnitPrice` | Corrigido |
| Recalcular histórico | **Com problema** (apagava os meses ajustados à mão) | recalcula preservando meses ajustados e mostra antes e depois | `previewRecalculation` | Corrigido |
| Valor da venda fora do par | **Com problema, sem correção** | o diálogo calcula o lucro com a diferença de preço e o serviço a soma de novo ao valor creditado | `BondService.createBondOperation` | Decisão do dono: o que é o preço de venda. Não afeta títulos a preço unitário de R$ 1 |

## Renda variável e rentabilidade

| Cálculo | Veredito | Fórmula de referência | Código | Estado |
|---|---|---|---|---|
| Lucro e rentabilidade da tela de investimentos | **Correto** | Σ quantidade × (preço atual − preço médio); % sobre o investido; sem dividendos | `SavingsOverviewController` | Mantido; a dica do gráfico deixa claro o que cada métrica inclui |
| Preço médio após venda parcial | **Com problema** (a contagem só crescia) | média das unidades ainda em custódia; a venda não muda o preço médio | `TickerService` | Corrigido (UNIT-3) |
| Peso fantasma no preço médio de ativo criado já com unidades | **Com problema** (contagem inicial 1) | contagem inicial = quantidade | `Ticker` | Corrigido |
| Precisão do preço médio | **Com problema** (2 casas na divisão) | 8 casas guardadas, exibição formatada | `Asset`, `TickerService` | Corrigido |
| Editar e apagar compra ou venda | **Com problema** (não ajustava quantidade nem preço médio) | desfaz o efeito da operação | `TickerService` | Corrigido (FR-27), exato quando desfeito na ordem inversa |
| Integridade das quantidades | **Ausente** | quantidade atual contra compras − vendas | `QuantityIntegrityService` | Novo, em Configurações, só relata |
| Contribuição por ativo | **Correto** | valor ao fim − valor ao início − aportes + dividendos | `PortfolioPerformanceCalculator.contributions` | Mantido |
| Retorno da carteira no gráfico | **Correto**, sem dividendos | retorno composto por dia sobre o capital em jogo | `portfolioReturn` | Mantido; caixa "Incluir dividendos" liga o retorno total |
| Dividend yield por ativo | **Ambíguo** | dividendos de 12 meses ÷ custo da posição atual; os dividendos podem vir de quando a quantidade era outra | `PortfolioAnalyticsService` | Mantido, aproximação documentada |
| Rebalanceamento | **Correto** | metas relativas aos tipos com meta; deltas somam zero | `PortfolioAnalyticsService.rebalance` | Mantido |
| Variação do dia e melhores e piores | **Correto** | variação do último pregão sobre o fechamento anterior | `PortfolioAnalyticsService` | Mantido |

## Desempenho histórico

| Cálculo | Veredito | Fórmula de referência | Código | Estado |
|---|---|---|---|---|
| Ganho mensal | **Com problema** (somava a apreciação e o lucro da venda, contando o realizado duas vezes) | valor ao fim − valor ao início − dinheiro posto no mês + proventos | `InvestmentPerformanceCalculator` | Corrigido (UNIT-4) |
| Apreciação do período | **Com problema** (usava a quantidade do fim do período) | só vale a partir da compra | idem | Corrigido |
| Ganho acumulado | **Com problema** (sem o realizado; renda fixa por um título só) | soma dos ganhos mensais | `InvestmentPerformanceService` | Corrigido |
| Valor investido em meses passados | **Com problema** (preço médio de hoje) | custo vigente na data pelo método do custo médio | `InvestmentPerformanceCalculator` | Corrigido; unidades que já estavam na carteira antes do primeiro registro usam o preço médio atual como custo |

## Patrimônio, carteiras e cartão

| Cálculo | Veredito | Fórmula de referência | Código | Estado |
|---|---|---|---|---|
| Saldo de carteira (movimentação, edição, transferência) | **Correto** | sinais conferidos nas edições e trocas | `WalletService` | Mantido |
| Patrimônio de meses passados | **Correto** na reversão de saldo; **com problema** na renda variável | saldo atual revertido; ativo pelo fechamento do mês | `NetWorthCalculationService` | Corrigido (preço de fechamento). O gráfico só muda com o botão "Recalcular" |
| Recorrências no patrimônio | **Com problema** (contava parcelas já realizadas) | só parcelas ainda não geradas em meses futuros | idem | Corrigido; resíduo: a parcela de um mês futuro pode pesar no saldo projetado e como passivo ou ativo |
| Dívida de cartão numa data | **Correto** | dívidas criadas − pagamentos − pendentes até a data | `CreditCardService.getDebtAtDate` | Mantido |
| Crédito disponível | **Correto** | limite − pendentes | `getAvailableCredit` | Mantido |
| Valor das parcelas | **Correto** | piso a 2 casas, resto na primeira parcela, soma exata | `calculateInstallmentValues` | Mantido |
| Pagamento parcial e abatimento | **Com problema** (fração a 2 casas, até 0,6% de erro e parcela paga acima do que devia) | repartição proporcional ao centavo, com teto e soma exata | `InvoicePaymentDistribution` | Corrigido (UNIT-5) |
| Dívida recorrente de cartão | **Correto** | ver `audit-notes.md` | `RecurringCreditCardDebtService` | Mantido |

## Projeções, metas e fundamentos

| Cálculo | Veredito | Fórmula de referência | Código | Estado |
|---|---|---|---|---|
| FIRE: meta e valor futuro | **Correto** | 12 × gasto ÷ taxa de retirada; valor futuro com aporte no fim do mês | `FIRECalculatorService` | Mantido; premissas (retorno nominal, gasto constante) agora aparecem na tela |
| FIRE: idade | **Ambíguo** (só anos inteiros) | idade + meses ÷ 12 | idem | Corrigido: anos e meses |
| Depósito mensal sugerido da meta | **Ambíguo** (sem rendimento; meses truncados) | (meta − saldo) ÷ meses | `GoalController` | Mantido, documentado |
| Progresso da meta | **Com problema** (passos de 1 ponto) | saldo ÷ meta com 6 casas | `Goal.progressPercentage` | Corrigido (FR-25) |
| Valor justo de Graham | **Com problema** (rendimento fixo de 10% e crescimento da receita) | LPA × (8,5 + 2g) × 4,4 ÷ Y | `get_fundamental_data.py` | Corrigido: Y = Selic meta mais recente (10% fora do Brasil ou sem dado) e g = CAGR do lucro |
| CAGR em demonstrações trimestrais | **Com problema** (expoente em trimestres) | anos = (períodos − 1) ÷ períodos por ano | idem | Corrigido |
| Número de Graham | **Correto** | √(22,5 × LPA × VPA) | idem | Mantido |
| ROIC | **Ambíguo** | NOPAT ÷ (dívida + patrimônio − caixa); alíquota 34% ou |imposto ÷ EBIT| | idem | Mantido, a alíquota efetiva usa o EBIT |
| Nota de qualidade | **Ambíguo** (decisão de modelo) | faixas por indicador, pesos 35/25/20/20; margem líquida usa as faixas do ROE | `FundamentalScoringService` | Mantido |
| Orçamento por grupo | **Correto** | renda base × % alvo ÷ 100 | `FinancialPlanningService` | Mantido |
| Resumo anual e insights | **Não conferido em profundidade** | percentuais com 6 casas; média mensal ÷ número de meses do período | `AnnualSummaryService`, `FinancialInsightGenerator` | Só os pontos com divisão foram lidos |
| Calculadora | **Não conferido** | utilitário de tela | `CalculatorService` | Fora do escopo financeiro |

## Pendências e ações suas

1. **Recalcular o histórico de juros de cada título** (diálogo de histórico, botão "Recalcular Histórico"): mostra antes e depois e mantém meses ajustados. Até lá, o mês corrente usa a fórmula nova e os anteriores a antiga.
2. **Apertar "Recalcular" do patrimônio** na tela inicial, para o gráfico usar o fechamento de cada mês.
3. **Decidir o valor creditado na venda de renda fixa fora do par** (duplo, descrito acima).
4. **Decidir se a recorrência marcada para o patrimônio deve sair da projeção de saldo** de meses futuros.
5. **Revisar as premissas confirmadas**: o dia da aplicação não rende e o do resgate rende; `ZERO_COUPON` sem índice usa o calendário do CDI.
6. Os índices IPCA e Selic só foram validados em teste de fórmula, sem recalcular um título real.
7. O spread de uma operação só vale quando maior que zero (zero é lido como "não informado").
