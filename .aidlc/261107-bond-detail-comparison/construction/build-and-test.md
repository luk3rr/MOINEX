# Build and Test — 261107-bond-detail-comparison

Data: 2026-11-07T22:01:42-03:00. Comando aprovado: `ktlintCheck spotlessCheck cleanTest test` e `unittest` Python.

## Resultado
- Kotlin: BUILD SUCCESSFUL, 2144 testes, 0 falhas, 0 erros.
- Python: 118 testes, OK.

## Requisitos e testes
| Requisito | Teste |
|---|---|
| Rendimento acumulado em R$ e % na tabela | BondServiceYieldTest, BondYieldTextTest |
| Dados do detalhe (investido, atual, mês, líquido estimado, saldo mensal) | BondServiceDetailTest |
| Texto da taxa contratada | BondRateTextTest |
| Retorno acumulado por título contra o CDI, bruto e líquido, arquivados, períodos, mensal, 20 títulos em menos de 2 s | BondComparisonServiceTest |
| Aporte de renda fixa não rende no dia | BondComparisonServiceTest, PortfolioPerformanceCalculator (testes existentes) |

## Sem teste automático (conferido só por captura ou pelo dono)
- Janela de detalhe (abas, cartões, botões, filtro de ano, gráfico de saldo com crosshair) e a aba Comparação (carrossel, legenda, tooltips, ticks com ano): o dono usou na própria base e pediu os ajustes que foram feitos; não há teste de interface.
- Aba Operações e efeitos de comprar, vender e editar de dentro do detalhe.

## Riscos
- O gráfico "Carteira vs benchmarks" com renda fixa mudou levemente (dinheiro que entra não rende no dia).
- Outras 8 mensagens de `messages_pt_BR.properties` têm `'{0}'` com aspas simples, que o MessageFormat não substitui; não foram verificadas.
- A chave `bond.detail.year.label` ficou sem uso.
