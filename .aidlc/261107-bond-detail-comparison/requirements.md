# Requirements — bond-detail-comparison

## Original request
Tela de detalhe do título de renda fixa e gráfico de comparação de rendimento entre títulos. Reduzir as colunas da tabela principal de títulos e abrir uma tela com as informações específicas do título ao selecionar; adicionar um gráfico que permita comparar os rendimentos entre os títulos.

Contexto dado pelo dono na conversa (palavras dele): "Talvez dê para reduzir as colunas e permitir uma tela com informações específicas do título e tals. Talvez adicionar um gráfico que permita comparar os rendimentos entre os títulos algo assim".

## 1.1 Intent & stakeholders

**Intenção em termos de negócio.** A tabela principal de renda fixa tem colunas demais e mistura o que ajuda a varrer a carteira (nome, valor, rendimento) com o que só interessa quando se olha um título (identificador, taxa, rendimento do mês, preço unitário). O dono quer varrer a carteira num relance, abrir os dados de um título específico num lugar próprio e comparar o rendimento dos títulos entre si. [src: Original request]

**Pergunta que o gráfico deve responder** (todas as quatro marcadas pelo dono): como cada título se compara ao CDI; qual título rende mais no período; quanto cada título rendeu por mês em reais; bruto contra líquido de imposto. [user]

**O que o detalhe do título deve mostrar** (todos marcados): dados do título e posição, histórico de juros mensal, operações do título, e as ações de comprar, resgatar, editar e arquivar. [user]

**Como o detalhe abre:** o dono quer testar duas formas, uma janela aberta por um menu de opções no botão direito da linha e um painel ao lado da tabela, e decidir olhando mockups. [user] [mem: M-008]

**Colunas da tabela principal depois de enxugar:** Nome, Tipo, Investido, Atual, Rendimento acumulado e Vencimento. [user]

**Stakeholders**

| Papel | Quem | Interesse |
|---|---|---|
| Decide e usa | o dono do app | varrer a carteira, ver o detalhe de um título, comparar rendimentos |
| Afetado | telas de renda fixa existentes (tabela, histórico de juros, operações, arquivados) | continuam funcionando; ações hoje na tela principal mudam de lugar |

**Medidas de sucesso** (mensuráveis)

- SM-1: a tabela principal tem no máximo 6 colunas visíveis.
- SM-2: o detalhe de um título abre com no máximo 2 interações a partir da tabela (clique direito e escolher a opção, ou selecionar a linha para o painel).
- SM-3: o gráfico mostra, para o mesmo período, uma linha por título não arquivado com dados e uma linha de referência do CDI.
- SM-4: o rendimento acumulado de um título no gráfico bate com o histórico de juros mensal do mesmo título com diferença de até R$ 0,01.

**Não objetivos:** mudar a fórmula de juros; novos índices ou fontes de dados; marcação a mercado do Tesouro; exportação; alertas.

**Restrições:** JavaFX com AtlantaFX e os tokens `theme-*` (M-001); `TitledValue` em "Título: valor" (M-010); textos nos dois idiomas; dinheiro em `BigDecimal` (M-017); nenhum dado pessoal do dono em arquivos (M-011).

### Assumptions & open questions

- A1: o gráfico considera os títulos não arquivados com taxa, tipo e histórico do índice; os demais ficam fora e a tela diz quantos. — confirmed: yes (2026-11-07T18:56:55-03:00, gate 1.x)
- A2: o rendimento acumulado de cada linha usa o valor diário do título já calculado para a opção "Incluir renda fixa" da carteira (`BondAccrualCalculator.dailyValues`), sem contar aportes e resgates como ganho. — confirmed: yes (2026-11-07T18:56:55-03:00, gate 1.x)
- A3: o líquido usa o IR regressivo estimado que já existe (`BondTaxEstimator`). — confirmed: yes (2026-11-07T18:56:55-03:00, gate 1.x)
- Q1: o painel lateral e a janela, qual fica? (decidir com mockups)

## 1.3 Feasibility & Constraints

**Veredito: viável, sem dependências novas.**

| O que a feature precisa | O que já existe |
|---|---|
| Valor diário de cada título | `BondAccrualCalculator.dailyValues` (usado na opção "Incluir renda fixa") |
| Retorno acumulado descontando aportes e resgates | `PortfolioPerformanceCalculator.portfolioReturn` com `FixedIncomePosition` |
| Linha do CDI | `PerformanceBenchmarkService`, `BenchmarkChart` |
| Histórico de juros mensal, ajuste manual e recálculo | `BondInterestHistoryController` e `BondInterestCalculationService` |
| Imposto estimado, valor líquido | `BondTaxEstimator`, `BondService.estimateNetInterest` |
| Operações de um título | `BondService.getOperationsByBond`, `BondTransactionsController` |
| Menu de opções no botão direito | `RowContextMenu` (usado no histórico de juros) |

**RAID**

| ID | Tipo | Item | Mitigação |
|---|---|---|---|
| R-1 | Risco | Título sem histórico do índice não tem série diária | ficar fora do gráfico e avisar na tela |
| R-2 | Risco | Muitos títulos viram linhas demais no gráfico | no máximo uma linha por título; destacar o selecionado; rolagem da legenda |
| R-3 | Risco | Duas formas de navegação dobram o trabalho de interface | decidir por mockups no 2.5 e construir uma só |
| R-4 | Risco | O líquido é estimativa e pode ser lido como número fiscal | rótulo de estimativa, como no histórico de juros |
| R-5 | Risco | A tabela principal perde ações hoje presentes | listar no escopo onde cada ação passa a viver |
| D-1 | Dependência | Histórico de juros recalculado pelo dono depois do intent anterior | o gráfico usa o cálculo diário, que não depende do histórico gravado |

**Recomendação: go.**

### Assumptions & open questions

- A4: o desempenho de desenhar um gráfico com dezenas de linhas diárias é aceitável para o volume do dono (poucos títulos). — confirmed: yes (2026-11-07T18:56:55-03:00, gate 1.x)

## 1.4 Scope

**Dentro**

- Tabela principal com 6 colunas: Nome, Tipo, Investido, Atual, Rendimento acumulado e Vencimento.
- Tela de detalhe do título: dados e posição, histórico de juros mensal, operações do título e as ações de comprar, resgatar, editar e arquivar.
- Abertura do detalhe por um menu no botão direito da linha e por um painel ao lado da tabela; a decisão de qual fica sai dos mockups.
- Gráfico de comparação entre títulos: rendimento acumulado em % no período (3M, 6M, 12M e desde o início) contra o CDI; opção de ver o líquido estimado; vista de rendimento mensal em reais por título.
- Os dois idiomas e os dois temas.

**Fora**

- Mudar fórmulas de juros, IR ou saldo.
- Novos índices ou fontes de dados; marcação a mercado do Tesouro; IOF; exportação; alertas.
- Mudar o diálogo de compra e de resgate além de onde ele é aberto.

**Limite do MVP:** tabela enxuta, detalhe com dados, histórico, operações e ações, e o gráfico de rendimento acumulado contra o CDI. Líquido e rendimento mensal em reais entram se couberem sem reabrir o escopo.

**Backlog priorizado**

| Prioridade | Item |
|---|---|
| Must | tabela enxuta; detalhe com dados e posição, histórico e operações; gráfico % acumulado contra CDI por período |
| Should | ações de comprar, resgatar, editar e arquivar no detalhe; alternância bruto e líquido; vista de rendimento mensal em reais |
| Could | segunda forma de navegação (painel ou janela) se o dono quiser as duas |

### Assumptions & open questions

- A5: as ações hoje na tela principal (comprar, resgatar, histórico, arquivados) continuam acessíveis; o que mudar de lugar será definido nos mockups. — confirmed: yes (2026-11-07T18:56:55-03:00, gate 1.x)

## 2.3 Requirements

Os exemplos usam números fictícios (M-011). A forma de abrir o detalhe (janela, painel ou as duas) fica para o 2.5; os requisitos valem para qualquer uma.

### Functional

| ID | Requirement | Rationale | Acceptance criteria | Priority | Source |
|---|---|---|---|---|---|
| FR-1 | A tabela principal de títulos tem 6 colunas: Nome, Tipo, Valor Investido, Valor Atual, Rendimento acumulado e Vencimento. Busca por texto e filtro de tipo continuam. | Varrer a carteira num relance (SM-1). | Given títulos cadastrados, When a aba abre, Then a tabela mostra exatamente essas 6 colunas e nenhuma de identificador, taxa, rendimento do mês, quantidade ou preço unitário. | Must | [user: "Nome, Tipo, Investido, Atual, Rendimento acumulado, Vencimento"] |
| FR-2 | A coluna "Rendimento acumulado" mostra o valor em reais e o percentual sobre o valor investido, em verde se positivo e vermelho se negativo. | Dá noção de quem rende mais sem abrir o gráfico. | Given investido R$ 4.000,00 e rendimento acumulado de R$ 150,20, Then a célula mostra "+ R$ 150,20 (+3,76%)" em verde. Given rendimento de −R$ 10,00 sobre R$ 1.000,00, Then mostra "− R$ 10,00 (−1,00%)" em vermelho. | Must | [user: "Valor em R$ e o percentual sobre o investido, colorido"] |
| FR-3 | A partir de uma linha da tabela o usuário abre o detalhe do título em no máximo 2 interações. | SM-2. | Given um título na tabela, When o usuário usa a forma de abrir escolhida no 2.5, Then o detalhe desse título aparece. | Must | [user: janela por clique direito e painel lateral] |
| FR-4 | O detalhe mostra os dados do título: nome, identificador, tipo, emissor, tipo de juro e taxa (percentual do indexador, indexador mais taxa anual, prefixada), indexador e vencimento. | Esses dados saem da tabela principal. | Given um CDB a 120% do CDI, Then o detalhe mostra "120% do CDI", indexador CDI, emissor e vencimento. | Must | [user: "Dados do título e posição"] |
| FR-5 | O detalhe mostra a posição: valor investido, valor atual, rendimento acumulado, rendimento do mês e o juro líquido estimado (rotulado como estimativa; "—" para tipos sem estimativa). | Hoje só o histórico de juros mostra o líquido. | Given um CDB com juro retido de R$ 100,00 mantido por 100 dias, Then o juro líquido estimado é R$ 77,50. | Must | [user]; [src: BondService.estimateNetInterest] |
| FR-6 | O detalhe tem o histórico de juros mensal do título com as ações já existentes: ajustar o juro de um mês, voltar ao automático e recalcular o histórico com antes e depois. | O diálogo de histórico passa a viver no detalhe. | Given um mês ajustado à mão, When o usuário recalcula, Then o mês ajustado é mantido e os demais mudam como no recálculo atual. | Must | [user: "Histórico de juros mensal"]; [src: BondInterestHistoryController] |
| FR-7 | O detalhe lista as operações só desse título (compras e resgates), com editar e apagar. | Hoje as operações de todos os títulos ficam numa lista única. | Given 2 títulos com 3 e 5 operações, When o detalhe do primeiro abre, Then lista 3 operações. | Must | [user: "Operações do título"] |
| FR-8 | O detalhe tem as ações de comprar, resgatar, editar e arquivar o título. | Ações no contexto do título. | Given o detalhe aberto, When o usuário clica em Comprar, Then abre o diálogo de compra desse título. | Should | [user: "Ações: comprar, resgatar, editar, arquivar"] |
| FR-9 | O detalhe tem um gráfico pequeno do saldo do título nos últimos 12 meses. | Mostra a trajetória sem sair do detalhe. | Given um título com histórico de 12 meses, Then o gráfico mostra o saldo mês a mês; sem histórico, mostra a mensagem de vazio. | Should | [src: mockups A e B] |
| FR-10 | Existe a vista "Comparação" com uma linha de rendimento acumulado em % para cada título ativo com dados e uma linha de referência do CDI, com os períodos 3M, 6M, 12M e Tudo; abre em 12M. "Tudo" vai da primeira compra de qualquer título até hoje. | Responde "qual título rende mais" e "como se compara ao CDI" (SM-3). | Given 3 títulos ativos, When a vista abre, Then o gráfico mostra 3 linhas e a do CDI no período de 12 meses. | Must | [user: as quatro perguntas; "12M ao abrir; opções 3M, 6M, 12M e Tudo"] |
| FR-11 | Todas as linhas partem de 0% no início do período; um título comprado depois entra em 0% na data da sua primeira compra. | Comparar o mesmo intervalo. | Given um título comprado no meio do período, Then sua linha começa em 0% nessa data e termina no mesmo ponto final das outras. | Must | [user: "Todas em 0% no início do período..."] |
| FR-12 | O rendimento acumulado de cada linha não conta aportes nem resgates como ganho e bate com o histórico de juros mensal do título com diferença de até R$ 0,01 por mês. | SM-4. | Given um título com compra de R$ 1.000,00 e juro de R$ 10,00 no período, Then a linha termina em +1,00%; um aporte adicional no meio do período não altera o percentual por si só. | Must | [src: PortfolioPerformanceCalculator] |
| FR-13 | Títulos sem série do índice ficam fora do gráfico e a vista informa quantos. | R-1. | Given 4 títulos e 1 sem histórico do índice, Then o gráfico mostra 3 linhas e o aviso "1 título fora do gráfico". | Must | [src: 1.3] |
| FR-14 | O usuário alterna entre bruto e líquido estimado (IR regressivo) nas linhas de acumulado e no rendimento mensal; a vista rotula o líquido como estimativa. | Pergunta "bruto contra líquido". | Given um CDB de 100 dias com juro de R$ 100,00, When o usuário escolhe líquido, Then o rendimento considerado é R$ 77,50; LCI e LCA não mudam. | Should | [user: "Bruto contra líquido de imposto"]; [src: BondTaxEstimator] |
| FR-15 | A vista "Mensal em R$" mostra, para os últimos 12 meses, barras agrupadas com o rendimento do mês de cada título. | Pergunta "quanto rendeu por mês". | Given 2 títulos com rendimento de R$ 20,00 e R$ 10,00 em um mês, Then as barras mostram 20,00 e 10,00 nesse mês. | Should | [user: "Rendimento mensal em R$ por título"] |
| FR-16 | A legenda mostra o nome e o resultado do período de cada série e liga e desliga a linha com um clique; o cursor mostra os valores de cada série num dia. | Com várias linhas, destacar uma ajuda. | Given 4 linhas, When o usuário desliga uma na legenda, Then a linha some e as outras permanecem. | Should | [src: ChartCrosshair, mockup C] |
| FR-17 | Existe a opção "Incluir arquivados", desligada por padrão, que acrescenta os títulos arquivados à comparação. | Comparar com o que já saiu. | Given um título arquivado, Then ele só aparece quando a opção está ligada. | Should | [user: "Não, só os ativos; uma opção liga os arquivados"] |
| FR-18 | As vistas tratam estados vazios: sem títulos, um só título e sem dados do índice têm mensagem própria. | Estados dos wireframes. | Given nenhum título, Then a vista Comparação mostra "Cadastre um título para comparar". | Must | [src: 1.6] |
| FR-19 | Tudo que for novo existe em português e inglês, nos temas claro e escuro, com as cores das linhas vindas dos tokens do tema. | Padrão do app. | Given o idioma en, Then os textos novos aparecem em inglês; nos dois temas as linhas têm contraste mínimo de 3:1 com o fundo do cartão. | Must | [mem: M-001] |
| FR-20 | Compra, resgate, editar, apagar, histórico de juros, operações, arquivados e a opção "Incluir renda fixa" do gráfico de carteira continuam funcionando como hoje. | Não regressão. | Given as telas existentes, Then os testes e as capturas atuais seguem passando sem mudança de comportamento. | Must | [src: 1.4] |

### Non-functional

| ID | Category | Measurable target | Verification | Source |
|---|---|---|---|---|
| NFR-1 | Desempenho | O gráfico de comparação para 20 títulos e 12 meses é montado em até 2 s e fora da thread da interface (a tela não congela). | Teste de desempenho do cálculo; verificação manual da tela | [inferred] |
| NFR-2 | Precisão | Valores em reais e percentuais calculados em `BigDecimal`; diferença para o histórico mensal de até R$ 0,01 por mês. | Testes com valor de referência calculado à parte | [mem: M-017] |
| NFR-3 | Acessibilidade | Cada série tem cor e rótulo textual na legenda; contraste das linhas de pelo menos 3:1. | Revisão das capturas nos dois temas | [src: tokens `theme-chart-series-*`] |
| NFR-4 | Testabilidade | Cada FR tem teste automático ou captura de tela com dados fictícios. | `./gradlew test` e capturas | [mem: M-006] |
| NFR-5 | i18n | Chaves novas só ao fim dos dois `.properties`. | Revisão do diff | [feedback salvo] |
| NFR-6 | Privacidade | Nenhum dado, caminho ou valor do banco real em testes, docs ou commits. | Revisão do diff | [mem: M-011] |
| NFR-7 | Consistência | Nenhum número novo contradiz os já mostrados: o rendimento acumulado da tabela e o do detalhe são o mesmo valor. | Teste que compara as duas fontes | [inferred] |

### Constraints · Out of scope · Glossary

- **Restrições:** JavaFX com AtlantaFX, tokens `theme-*`, `Icons.kt`, `TitledValue` em "Título: valor", cabeçalho de arquivo padrão, sem comentário `//` explicativo, ktlint e spotless passando.
- **Fora de escopo:** ver 1.4.
- **Glossário:** *rendimento acumulado* = juro que ainda está no título (já líquido dos resgates de juro); *saldo* = principal mais esse juro; *linha do CDI* = retorno acumulado do CDI no mesmo período; *líquido estimado* = bruto menos o IR regressivo estimado.

### Assumptions & open questions

- A9: o rendimento mensal em reais, em líquido, desconta o IR estimado proporcional ao rendimento do mês. — confirmed: yes (2026-11-07T19:10:04-03:00, gate 2.3)
- A10: o percentual da coluna "Rendimento acumulado" é o rendimento acumulado dividido pelo valor investido atual. — confirmed: yes (2026-11-07T19:10:04-03:00, gate 2.3)
- A11: "Tudo" começa na primeira compra entre os títulos listados (ativos, mais arquivados se a opção estiver ligada). — confirmed: yes (2026-11-07T19:10:04-03:00, gate 2.3)

## 2.4 User stories

Persona única: **o dono do app**, que registra e acompanha a própria renda fixa.

### US-1 Varrer a carteira  (FR-1, FR-2)  Persona: dono
Como dono, quero uma tabela enxuta com nome, tipo, investido, atual, rendimento e vencimento, para ver a carteira num relance.
- AC1: Given títulos cadastrados, When abro a aba, Then vejo as 6 colunas e o rendimento em R$ e % colorido.

### US-2 Abrir o detalhe de um título  (FR-3, FR-4, FR-5)  Persona: dono
Como dono, quero abrir os dados de um título, para ver taxa, emissor, vencimento e minha posição sem poluir a tabela.
- AC1: Given um título, When o abro pela forma escolhida, Then vejo dados, posição e juro líquido estimado.

### US-3 Corrigir o histórico de juros do título  (FR-6)  Persona: dono
Como dono, quero ver e ajustar o histórico de juros dentro do detalhe, para corrigir um mês sem abrir outro diálogo.
- AC1: Given um mês, When o ajusto ou recalculo, Then vale o comportamento atual (meses ajustados são mantidos).

### US-4 Ver e agir sobre as operações do título  (FR-7, FR-8)  Persona: dono
Como dono, quero ver só as operações do título e comprar, resgatar, editar ou arquivar a partir dele.
- AC1: Given o detalhe aberto, Then a lista tem só as operações desse título e as ações abrem os diálogos existentes.

### US-5 Ver a trajetória do saldo  (FR-9)  Persona: dono
Como dono, quero um gráfico pequeno do saldo do título, para ver como ele evoluiu em 12 meses.
- AC1: Given um título com histórico, Then o gráfico mostra o saldo mês a mês.

### US-6 Comparar os títulos com o CDI  (FR-10, FR-11, FR-12, FR-13, FR-16, FR-18)  Persona: dono
Como dono, quero um gráfico com uma linha por título e o CDI, para saber qual rende mais e quem está acima do CDI.
- AC1: Given 3 títulos, When abro a comparação, Then vejo 3 linhas e o CDI em 12M, todas partindo de 0%.
- AC2: Given um título sem histórico do índice, Then ele fica fora e a tela diz quantos.
- AC3: Given a legenda, When clico num título, Then a linha dele liga ou desliga.

### US-7 Comparar bruto e líquido  (FR-14)  Persona: dono
Como dono, quero alternar entre bruto e líquido estimado, para comparar o que fica depois do imposto.
- AC1: Given um CDB, When escolho líquido, Then o rendimento descontado do IR estimado é usado; LCI e LCA não mudam.

### US-8 Comparar o rendimento mensal em reais  (FR-15)  Persona: dono
Como dono, quero barras do rendimento de cada mês por título, para ver o dinheiro que cada um rendeu.
- AC1: Given 12 meses, Then cada mês mostra uma barra por título.

### US-9 Incluir os arquivados  (FR-17)  Persona: dono
Como dono, quero ligar uma opção para ver também os arquivados, para comparar com títulos que já saíram.
- AC1: Given a opção desligada, Then só os ativos aparecem; ligada, os arquivados entram.

### US-10 Usar a tela no meu idioma e tema  (FR-19)  Persona: dono
Como dono, quero a tela em português ou inglês e nos temas claro e escuro, com linhas legíveis.
- AC1: Given o idioma en e o tema claro, Then textos em inglês e linhas com contraste mínimo de 3:1.

### US-11 Continuar fazendo o que já faço  (FR-20)  Persona: dono
Como dono, quero que comprar, resgatar, editar, apagar, arquivar e ver o histórico continuem funcionando, para não perder nada com a mudança.
- AC1: Given as telas existentes, Then os testes e as capturas atuais seguem passando.

### Assumptions & open questions

- A12: uma persona basta; não há outros usuários do app. — confirmed: yes (2026-11-07T19:16:02-03:00, gate 2.4)

