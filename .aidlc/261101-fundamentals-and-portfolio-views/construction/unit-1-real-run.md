# UNIT-1 · Conferência com dados reais (T-1.4)

Data: 2026-11-01T15:55:14-03:00 · yfinance 1.7.0 · script rodado contra o Yahoo (rede), sem banco.

| Ativo | LPA usado | Fonte | Graham Number | Valor justo | Margem | P/L | PEG | CAGR receita |
|---|---|---|---|---|---|---|---|---|
| PETR4.SA | 1,52 | statement | 14,16 | 5,68 | −827,2% | 34,7 | – (LPA em queda) | −10,5% |
| VALE3.SA | 0,55 | statement | 9,85 | 2,06 | −2915,1% | 112,8 | – (LPA em queda) | −4,3% |
| ITUB4.SA | 4,14 | info | 42,90 | 39,56 | 2,2% | 9,3 | 0,64 | 6,6% |
| WEGE3.SA | 1,49 | info | 12,63 | 19,88 | −143,2% | 32,5 | 2,18 | 10,9% |
| AAPL | 8,72 | info | 38,00 | 46,52 | −640,8% | 39,5 | 5,74 | 1,8% |

Observações:
- Todos têm CAGR, valor justo e margem preenchidos (antes: PETR4, VALE3 e WEGE3 ficavam sem valor justo e sem CAGR).
- `financialCurrency` da VALE3 no Yahoo é **BRL**, igual à moeda do preço: a premissa de erro de moeda para VALE3 (requisito FR-4) não se confirmou. A conversão só age quando as moedas diferem e está coberta por teste com taxa simulada.
- PETR4: LPA do Yahoo (10,49) trocado pelo do balanço (1,52); VPA recalculado como patrimônio / ações diluídas (≈ 5,86) em vez do 37,3 do Yahoo, que não bate com o balanço (ações por classe).
- A margem de segurança é (justo − preço) / justo; com valor justo pequeno ela fica muito negativa. O semáforo da UNIT-3 usa só as faixas, mas a exibição do número bruto deve ser repensada lá.
