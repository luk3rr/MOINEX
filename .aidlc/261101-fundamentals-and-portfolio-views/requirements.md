# Requirements — fundamentals-and-portfolio-views

## Original request

Pedido inicial, verbatim:

> Gostaria de ajuda para corrigir um possível problema e aproveitar para pensar maneiras de melhor a parte de visulização das ações e também de análise fundamentalista. Primeiro, o possível problema é que algumas informações como o número de Graham estão NA. Queria entender se alguma coisa na resposta a API ou no código mudou de maneira que esse e outros campos estão ficando NA.
>
> Segundo, seria discutir oportunidades de melhorar a visualização dos ativos/performance tanto na tela de ações e fundos como na visão geral.

### Contexto levantado antes do intent (diagnóstico, [src: investigação 2026-11-01])

- **Causa dos N/A:** o yfinance 1.7.0 passou a devolver uma 5ª coluna (período mais antigo) em `financials`/`balance_sheet`/`cashflow` em que só linhas auxiliares vêm preenchidas e `Total Revenue`, `Net Income`, `Diluted EPS` vêm NaN. `safe_get(..., 0)` converte NaN em 0, então `calculate_revenue_growth` falha em `revenues[-1] != 0`, o CAGR de receita vira `None`, e com ele valor justo de Graham, margem de segurança e demais derivados.
- **Correção já aplicada no working tree (não commitada, aguardando revisão do diff):** `drop_empty_periods` em `get_fundamental_data.py` + piso 0 para o crescimento em `calculate_graham_fair_value` + margem de segurança só se valor justo > 0.
- **Problemas abertos de qualidade de dado:** (a) PETR4 `trailingEps` 10,49 vs `Diluted EPS` do balanço 1,52 → P/L e Graham Number inflados; (b) VALE3 reporta em USD (`financialCurrency`) com preço em BRL → LPA/valor justo na moeda errada; (c) FIIs/ETFs (BOVA11) caem em "Financial data not available".
- **Ideias de visualização propostas:** gráfico por ativo (sparkline + preço com compras/vendas e preço médio); tabela de tickers com retorno total/anualizado, peso, DY on cost, distância ao preço-alvo; contribuição para o retorno (waterfall); carteira vs benchmarks (Ibovespa, CDI, IPCA+); dividendos (mensal empilhado, renda média, projeção 12m); fundamentalista (semáforo de valuation, nota de qualidade/radar, comparação com setor, motivo do N/A, data por card, métricas de FII); visão geral com alertas e rebalanceamento sugerido.

## 2.3 Requirements

Escopo confirmado pelo dono: todos os itens propostos entram; nenhum corte (COULD) aceito. Ordem de entrega: Dados → Fundamentalista → Ações e Fundos → Visão Geral. Prioridade MoSCoW: todos MUST; a ordem é a de entrega. [user: gate 2.3]

### Requisitos funcionais

Bloco A, qualidade dos dados (script `get_fundamental_data.py` e serviço):

- **FR-1** (MUST) Ignorar períodos sem a linha-chave nos demonstrativos do yfinance, para que CAGR de receita, EPS CAGR, PEG, valor justo de Graham e margem de segurança voltem a ser calculados. Já implementado no working tree. [src: diagnóstico 2026-11-01]
  - Aceite: para PETR4.SA, ITUB4.SA e WEGE3.SA o `growth.revenue_growth.cagr` e `valuation.graham_fair_value` não são nulos com 4 anos de dados.
- **FR-2** (MUST) O valor justo de Graham usa crescimento com piso 0 e teto 25%, e a margem de segurança só é calculada quando o valor justo é maior que 0. Já implementado.
  - Aceite: com CAGR negativo (VALE3, PETR4) o valor justo é positivo e a margem de segurança fica entre -∞ e 100% sem valores absurdos como +126857%.
- **FR-3** (MUST) Usar um LPA coerente: quando `trailingEps` diverge do `Diluted EPS` mais recente do balanço além de um limite definido no 2.8, usar o do balanço e registrar a origem no JSON (`eps_source`).
  - Aceite: PETR4.SA passa a ter P/L e Graham Number calculados com LPA ≈ 1,52, e o JSON indica a fonte usada.
- **FR-4** (MUST) Converter valores monetários dos demonstrativos para a moeda do preço quando `financialCurrency` difere de `currency` (ex.: VALE3 em USD), registrando taxa e data usadas.
  - Aceite: VALE3.SA tem LPA e valor justo em BRL comparáveis ao preço; o JSON traz `financial_currency` e `fx_rate`.
- **FR-5** (MUST) Cada métrica sem valor traz um motivo estruturado no JSON (`reason`: prejuízo, histórico insuficiente, não aplicável, dado indisponível) e a UI exibe o motivo e a data do dado em vez de "N/A" ou de esconder o card.
  - Aceite: Graham Number de empresa com LPA ≤ 0 mostra "Não aplicável: LPA negativo"; chave ausente mostra o card com motivo.
- **FR-6** (MUST) Existe teste automatizado do contrato do JSON produzido pelo script (fixture com a forma bruta atual do yfinance, incluindo a coluna final vazia).
  - Aceite: o teste falha se uma coluna vazia voltar a anular CAGR e valor justo.

Bloco B, análise fundamentalista (diálogo):

- **FR-7** (MUST) Cabeçalho de valuation com semáforo: preço atual, Graham Number, valor justo e margem de segurança, com estado barato / justo / caro por faixas definidas no 2.8.
- **FR-8** (MUST) Nota de qualidade (0–100 ou radar) a partir de ROE, margem líquida, endividamento, crescimento e geração de caixa, com a composição visível.
- **FR-9** (MUST) Para FII e ETF exibir métricas próprias quando disponíveis (P/VP, dividend yield, histórico de proventos, variação) em vez de apenas "Financial data not available".
  - Aceite: FIIA11.SA e BOVA11.SA abrem com ao menos performance de preço e as métricas de renda disponíveis.
- **FR-10** (MUST) Comparação das métricas do ativo com a mediana do setor/indústria entre os tickers da carteira com análise salva, indicando quando há menos de 3 pares.

Bloco C, tela de Ações e Fundos:

- **FR-11** (MUST) Sparkline de 12 meses por linha da tabela.
- **FR-12** (MUST) Colunas de retorno total (%), lucro/prejuízo (R$), peso na carteira (%) e dividend yield on cost, ordenáveis.
- **FR-13** (MUST) Detalhe do ativo com gráfico de preço (12 meses), linha do preço médio e marcadores de compras e vendas.
- **FR-14** (MUST) Filtros segmentados (`SegmentedFilter`) por tipo e ordenação por retorno, peso e variação do dia.
- **FR-15** (MUST) Persistir preço de fechamento diário dos últimos 12 meses por ativo e atualizá-lo incrementalmente na atualização de preços.
  - Aceite: após a atualização, `ticker_price_history` tem um ponto por dia útil por ativo não arquivado, sem duplicar fim de mês ou datas de operação.

Bloco D, Visão Geral:

- **FR-16** (MUST) Na nova aba Desempenho de Savings, gráfico de performance da carteira sobreposto com Ibovespa, CDI e IPCA (variação acumulada no mesmo período).
- **FR-17** (MUST) Na aba Desempenho, gráfico de contribuição por ativo em R$ (waterfall) para o lucro da carteira.
- **FR-18** (MUST) Na aba Desempenho, painel de dividendos: barras mensais empilhadas por ativo, renda média mensal e projeção de 12 meses.
- **FR-19** (MUST) Na Visão Geral, alertas de desvio em relação às metas de alocação (`InvestmentTarget`) e valor sugerido em R$ para rebalancear.

### Requisitos não funcionais

- **NFR-1** (MUST) Abrir a tela de Ações e Fundos com 50 ativos em ≤ 1 s após o primeiro carregamento, sem rede; consultas à rede só na atualização de preços (por usuário).
- **NFR-2** (MUST) Falha do Yahoo (429, indisponível) nunca apaga dados salvos: manter o último cache e mostrar a data. Retry com backoff já existente (`RetryConfig`).
- **NFR-3** (MUST) Visual coerente com o tema: cores só por tokens `theme-*` ou `ThemeColors`, nos temas claro e escuro; preservar identidade (M-001). [mem: M-001]
- **NFR-4** (MUST) Textos novos nas duas línguas (pt-BR e en), com chaves acrescentadas, sem reescrever o `.properties` inteiro. [src: CLAUDE.md]
- **NFR-5** (MUST) Testes unitários (Kotest/MockK) para cálculos novos (retorno, peso, DY on cost, contribuição, benchmarks, rebalanceamento, nota, semáforo) e para o contrato do script.
- **NFR-6** (MUST) Qualquer migration Flyway é aditiva e compatível com o banco real (o desenho do 2.6 não prevê nenhuma); nada neste trabalho grava no banco real do dono. [user]
- **NFR-7** (MUST) Todo valor derivado informa a data de referência e quando é estimativa (acessibilidade: cor nunca é o único indicador; semáforo tem texto/ícone).

### Restrições

- Python 3 do sistema e yfinance 1.7.0 (`requirements.txt`); não passar `requests.Session` a `yf.Ticker`. [src: CLAUDE.md]
- O banco de uso real é só leitura; testes e validação visual usam H2, `make demo` ou cópia. [user]
- Sem comentários `//` explicativos no Kotlin; commit só após o dono ver o diff; tudo no `main` em commits pequenos. [mem: M-004] [user]
- Nada de CI/push sem sim explícito na hora. [mem: M-005]

### Fora de escopo

Operações com derivativos, bonds/renda fixa, criptomoedas além do que as telas já mostram, novas telas fora de Savings, recomendações de compra/venda, mudança de fonte de dados.

### Glossário

- **Graham Number**: √(22,5 × LPA × VPA). **Valor justo de Graham (revisado)**: LPA × (8,5 + 2g) × 4,4 / Y. **Margem de segurança**: (valor justo − preço) / valor justo.
- **LPA/EPS**: lucro por ação. **VPA**: valor patrimonial por ação. **DY on cost**: proventos de 12 meses / custo médio.

### Assumptions & open questions

- A2: Ibovespa histórico vem de `get_stock_price_history.py` com `^BVSP`; CDI e IPCA vêm de `MarketIndicatorHistory`. [inferred]
- A3: taxa de câmbio USD/BRL histórica obtida pelo mesmo script de preço (`USDBRL=X`). [inferred]
- A4: FIIs/ETFs expõem P/VP e DY via `ticker.info` do yfinance; a disponibilidade varia por ativo. [inferred]
- A5: projeção de dividendos 12m = média dos últimos 12 meses (método simples) até o 2.8 decidir outro. [inferred]
- Q: nenhuma pendente.

## 2.4 User stories

Persona única: **Investidor-dono** (P1), usuário individual do MOINEX, carteira brasileira de ações, FIIs e ETFs, decide por análise fundamentalista e acompanha performance. [src: codekb/overview.md]

| ID | História | FR |
|---|---|---|
| US-1 | Como investidor, quero que Graham, valor justo, PEG e margem de segurança apareçam para ativos que têm dados, para analisar sem ver N/A indevido. | FR-1, FR-2 |
| US-2 | Como investidor, quero que LPA e valores venham na moeda e no LPA corretos, para que P/L e Graham de PETR4 e VALE3 reflitam a realidade. | FR-3, FR-4 |
| US-3 | Como investidor, quero saber por que uma métrica está indisponível, para distinguir prejuízo de falta de dado. | FR-5 |
| US-4 | Como mantenedor, quero um teste do contrato do script, para que uma mudança do yfinance não volte a quebrar a análise sem aviso. | FR-6 |
| US-5 | Como investidor, quero ver de relance se um ativo está barato ou caro e sua qualidade, para priorizar o estudo. | FR-7, FR-8 |
| US-6 | Como investidor, quero analisar FIIs e ETFs com métricas próprias e comparar um ativo com seu setor, para ter contexto além de ações. | FR-9, FR-10 |
| US-7 | Como investidor, quero ver tendência, retorno, peso e DY on cost de cada ativo na tabela, ordenar e filtrar, para identificar o que pesa na carteira. | FR-11, FR-12, FR-14 |
| US-8 | Como investidor, quero ver o preço do ativo com meu preço médio e minhas compras e vendas, para julgar minhas entradas. | FR-13, FR-15 |
| US-9 | Como investidor, quero comparar a carteira com Ibovespa, CDI e IPCA e ver a contribuição de cada ativo, para saber se meu desempenho foi bom e de onde veio. | FR-16, FR-17 |
| US-10 | Como investidor, quero ver minha renda de dividendos e ser avisado de desvios da alocação com sugestão de rebalanceamento, para agir sobre a carteira. | FR-18, FR-19 |

Cenários de aceite (Given/When/Then), por história:

- **US-1** Given PETR4.SA com 4 anos de demonstrativo e a coluna mais antiga vazia, When abro a análise anual, Then vejo Graham Number e valor justo preenchidos e margem de segurança coerente.
- **US-2** Given VALE3.SA reportando em USD, When abro a análise, Then LPA e valor justo estão em BRL e a fonte e a taxa aparecem no tooltip.
- **US-3** Given ativo com LPA negativo, When abro Valuation, Then o card de Graham mostra "Não aplicável: LPA negativo" com a data do dado.
- **US-4** Given o fixture bruto com coluna final vazia, When o teste roda, Then CAGR e valor justo não são nulos.
- **US-5** Given qualquer ação com dados, When abro a análise, Then o topo mostra o semáforo (texto + cor) e a nota de qualidade com a composição.
- **US-6** Given FIIA11.SA, When abro a análise, Then vejo P/VP e DY; Given 2 pares no setor, When abro a comparação, Then aparece o aviso "menos de 3 pares".
- **US-7** Given 10 ativos, When ordeno por retorno e filtro por tipo, Then a tabela reflete a seleção e cada linha mostra sparkline.
- **US-8** Given ativo com compras, When abro o detalhe, Then o gráfico mostra preço de 12 meses, linha do preço médio e marcadores de operação.
- **US-9** Given carteira com histórico, When abro a Visão Geral, Then o gráfico sobrepõe Ibovespa, CDI e IPCA e o waterfall soma o lucro total.
- **US-10** Given metas de alocação, When um tipo passa da meta, Then aparece alerta com o valor em R$ a vender ou aportar; o painel de dividendos mostra renda mensal e projeção.

Notas por perspectiva: design (estados vazio/erro/carregando, tema claro e escuro, cor nunca é o único sinal); desenvolvedor (US-7 e US-8 dependem de FR-15, série diária); qualidade (testes unitários para cálculos e contrato; validação visual com `make demo`).

Cobertura: FR-1..FR-19 estão cobertos por pelo menos uma história; NFRs se aplicam a todas.

### Assumptions & open questions
- Nenhuma nova.
