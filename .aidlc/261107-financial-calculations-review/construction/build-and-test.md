# Build and Test — financial-calculations-review

Data: 2026-11-07T17:47:09-03:00 · HEAD de partida 6c94ec03 · árvore de trabalho sem commit.

## Resultado da verificação aprovada

Comando: `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && ./gradlew ktlintCheck spotlessCheck test && /usr/bin/python3 -m unittest discover -s src/test/python`

| Parte | Resultado |
|---|---|
| ktlintCheck e spotlessCheck | BUILD SUCCESSFUL |
| Testes Kotlin (`cleanTest test`) | 2114 testes, 0 falhas, 0 erros (baseline: 2041); 2109 antes dos ajustes pós-Construction |
| Testes Python | 118 testes, OK (baseline: 109) |
| Migração V039 | aplicada num banco fictício pela captura de tela (schema na versão 039) |

Estratégia de testes do perfil: padrão (unitário por componente, integração nas fronteiras dos Units, critério de aceite por requisito). Falhas antigas: nenhuma.

## Requisito → teste

| Requisito | Testes |
|---|---|
| FR-1, FR-21 | `construction/audit-report.md`, `construction/audit-notes.md` (documentos) |
| FR-2 | `BondAccrualCalculatorTest` (110% do CDI, CDI + 2%, IPCA + 6%, Selic) |
| FR-3 | `BondInterestCalculationServiceAccrualTest` (taxa da compra e herdada) |
| FR-4 | `BondAccrualCalculatorTest` (venda rende no dia, compra não), `...AccrualTest` (compra e venda no mês) |
| FR-5 | `BondAccrualCalculatorTest` (252 dias), `...AccrualTest` (meses compostos) |
| FR-6 | `...AccrualTest` (resgate com juro), `BondServicePositionTest` (juro do resgate) |
| FR-7 | `BondTaxEstimatorTest`, `BondServicePositionTest` |
| FR-8 | cobertura indireta: usa a fórmula do prefixado (`BondAccrualCalculatorTest`, R$ 100,00); sem teste dedicado |
| FR-9 | `BondServicePositionTest` (R$ 1,33 sem exceção) |
| FR-10 | `...RecalculateAllMonthlyInterestTest`, `...AccrualTest` (pré-visualização) |
| FR-11, FR-27 | `TickerServiceAveragePriceTest` |
| FR-12 | `QuantityIntegrityServiceTest` |
| FR-13, FR-14, FR-15, FR-16 | `InvestmentPerformanceCalculatorTest`, `InvestmentPerformanceServiceTest` |
| FR-17 | `PortfolioPerformanceCalculatorTest` (retorno total); rótulos por dica, sem teste automático |
| FR-18, FR-19 | `NetWorthCalculationServiceHistoryTest` |
| FR-20 | `InvoicePaymentDistributionTest`, `CreditCardServicePayInvoiceTest` |
| FR-22, FR-23, FR-24 | `test_get_fundamental_data.py` (`ReferenceYieldAndGrowthTest`); a escolha EPS ou receita para o crescimento não tem teste dedicado |
| FR-25 | `GoalProgressTest` |
| FR-26 | `FIRECalculatorServiceTest` (idade em anos e meses); premissas na tela por captura |
| FR-28 | `...AccrualTest` (cálculo em dois dias diferentes) |

## NFR

| NFR | Validação |
|---|---|
| NFR-1 precisão | `BondAccrualCalculator` usa `BigDecimal`; só a raiz fracionária do fator de cada dia passa por `Math.pow` (15 dígitos), com testes contra valores de 40 dígitos |
| NFR-2 testabilidade | tabela acima |
| NFR-3 privacidade | busca no diff e nos arquivos novos por tickers, caminhos e nomes do banco real: nenhum achado |
| NFR-4 desempenho | `BondAccrualCalculatorTest` (5 anos de CDI diário, 60 aportes, abaixo de 5 s) |
| NFR-5 compatibilidade | sem migração de modelo (valor novo no enum); V039 apaga só o cache de desempenho |
| NFR-6 i18n | chaves novas só ao fim dos dois `.properties`; edições de valores existentes apenas nos textos que ficaram falsos (confirmação do recálculo, tipo de juro, dica do Graham, nota do tipo flutuante) |

## Segurança e falhas

Sem dados pessoais nos arquivos. Nenhuma falha de teste aberta. Lacunas conhecidas: FR-8, FR-17 (rótulos) e a escolha do crescimento do Graham sem teste automático dedicado; telas dos diálogos de venda e de cadastro de título não foram abertas em captura.
