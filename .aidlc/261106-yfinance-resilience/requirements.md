# Requirements — yfinance-resilience

## Original request
Quero otimizar o uso que faço do yfinances. Atualmente caiu muito no caso de rate limit ou muitas falhas. Avalie o que pode ser feito.

## 1.1 Intent & stakeholders

### Intenção reformulada
Fazer a atualização de dados de mercado (yfinance) do MOINEX ser **resiliente e econômica**: um refresh normal quase nunca falha por rate limit e, quando falha, preserva o dado anterior sem corrompê-lo. Além disso, o mesmo dado (principalmente o preço atual de um ativo) deve ter **uma única fonte de verdade** entre as telas. [user]

### Problemas observados
| # | Problema | Evidência |
|---|---|---|
| P1 | Rate limit e falhas frequentes, principalmente na atualização de preços dos tickers e na carga de indicadores de mercado ao abrir o app | [user] |
| P2 | Preço do mesmo ativo diverge entre telas: a análise fundamentalista usa um preço guardado no próprio `dataJson` (cache de 7 dias), as demais telas usam `Ticker.currentUnitValue` | [user] [src: FundamentalAnalysisService.kt:37,63-66,95-102] [src: get_fundamental_data.py:969] |

### Sinais iniciais do código (hipóteses, a confirmar no 2.1)
- `get_stock_price.py` chama `ticker.info` (endpoint pesado) por símbolo só para obter moeda e site; em falha assume `"USD"`, o que pode converter o preço com a moeda errada. [src: get_stock_price.py:fetch_symbol_info]
- `yf.download` que volta vazio é tratado como rate limit, mesmo quando o símbolo é inválido. [src: get_stock_price.py:_download_chunk]
- Cada script tem a própria cópia de `retry_call`; não há cooldown nem cache compartilhado entre execuções. [src: scripts/*.py]
- 6 serviços Kotlin disparam scripts independentes, sem coordenação de cota. [src: APIUtils.kt, services de investimento]

### Escala e sucesso
- Carteira de 15–50 tickers ativos. [user]
- Sucesso: refresh normal quase nunca falha; em falha, o dado antigo é mantido e nada é corrompido. [user]
- Métrica a definir em 2.3 (ex.: nº de chamadas ao Yahoo por refresh, taxa de falha, divergência de preço entre telas = 0).

### Stakeholders
| Papel | Quem |
|---|---|
| Decisor, usuário e mantenedor | Dono do MOINEX |
| Afetado | Telas de Carteira, Desempenho, Análise Fundamentalista, Mercado |
| Dependência externa | Yahoo Finance via yfinance 1.7.0 |

### Fora do escopo declarado
(a fechar em 2.3; trocar a fonte de dados não foi escolhido como objetivo, só como possível fallback a avaliar)

### Assumptions & open questions
- A1: a divergência de preço vem do `dataJson` da análise fundamentalista; confirmar no 2.1 com a lista de telas que mostram preço. Confirmado pelo dono: não (ainda).
- A2: não será trocada a fonte de dados; fallback alternativo é só avaliação. Confirmado pelo dono: não.
## 2.3 Requirements

### Decisões do dono neste estágio [user]
- Escopo aprovado: R1, R2, R3, R4, R5, R6, R7. R8 (fonte externa alternativa, paga ou não) fica para um **próximo intent**, que permitirá ao usuário escolher outro provedor no lugar do yfinance.
- Nova ideia: unificar as informações de um ativo numa **estrutura agregada e extensível (JSON)**, para acrescentar dados no futuro sem novas colunas/chamadas.
- Novo pedido: mostrar, **por ticker, data e hora da última atualização**.
- TTL padrão: 60 minutos. Em rate limit: manter dado antigo, avisar e aguardar cooldown.

### Functional
| ID | Requirement | Rationale | Acceptance criteria | Priority | Source |
|---|---|---|---|---|---|
| FR-1 | Existe uma única fonte do preço atual de cada ticker; todas as telas (Carteira, Visão geral, Detalhe, Desempenho, Análise Fundamentalista, assistente de IA) leem o mesmo valor | P2: preços divergentes entre telas | Given um ticker com preço atualizado, When abro qualquer tela que o mostre, Then o valor e a moeda são idênticos; o `dataJson` da análise não é lido como preço | Must | [user] D1 [src: FundamentalAnalysisController.kt:172] |
| FR-2 | Os dados de mercado de um ticker (preço, moeda, site, e outros que vierem) ficam num registro agregado em JSON por ticker, extensível sem migration a cada novo campo | Facilitar acrescentar informações e manipular tudo de forma agregada | Given um novo campo adicionado ao registro, When o app lê um registro antigo, Then lê com valor ausente sem erro; um teste cobre leitura de JSON antigo/incompleto | Must | [user] |
| FR-3 | Cada ticker mostra a data e hora da última atualização bem-sucedida do preço (e, no registro agregado, de cada grupo de dados quando houver) | Saber a idade do dado | Given um ticker atualizado às HH:MM, When abro a tabela de tickers e o detalhe, Then vejo "dd/MM/yyyy HH:mm" no formato do idioma; Given falha na atualização, Then a hora anterior é mantida | Must | [user] [src: Ticker.kt:32 lastUpdate já existe] |
| FR-4 | O refresh na abertura do app só busca tickers e indicadores com dado mais velho que o TTL (padrão 60 min); o botão manual ignora o TTL | Reduzir volume de requisições | Given 30 tickers atualizados há 20 min, When abro o app, Then 0 chamadas de preço ao Yahoo; Given um com 70 min, Then só esse é buscado; botão manual busca todos | Must | [user] D2 |
| FR-5 | A moeda e o site do ticker são obtidos uma vez e guardados; o refresh de preço não chama `ticker.info` por símbolo | Reduzir chamadas pesadas; evitar fallback USD | Given tickers com moeda conhecida, When atualizo preços, Then nenhuma chamada `info` é feita; Given moeda desconhecida e `info` falhando, Then o preço NÃO é atualizado e o antigo é mantido (nunca assume USD) | Must | [src: get_stock_price.py:fetch_symbol_info] |
| FR-6 | O histórico diário é buscado em lote para vários tickers por processo, só para o intervalo que falta, sem a cascata de `history` até `period=max` | Reduzir processos e requisições | Given 30 tickers, When sincronizo o histórico, Then o número de processos Python é ≤ ceil(30/lote) e nenhuma chamada usa `period=max` salvo no backfill inicial explícito | Must | [src: get_stock_price_history.py:154-215] |
| FR-7 | Os 6 scripts compartilham um único módulo de resiliência (retry com backoff e jitter, detecção de rate limit); resposta vazia não é tratada como rate limit | Remover duplicação e falsos positivos | Given símbolo inválido, When busco preço, Then falha uma vez sem retry; Given HTTP 429, Then retry com backoff até o limite | Should | [src: scripts/*.py retry_call] |
| FR-8 | Todas as chamadas ao Yahoo passam por um limitador global de concorrência e por um cooldown: após rate limit, nenhuma chamada nova ao Yahoo é feita até o fim do cooldown | Parar de insistir quando bloqueado | Given um rate limit, When outra tela pede dados antes do fim do cooldown, Then a chamada não ocorre e o dado antigo é mantido | Must | [user] |
| FR-9 | Em rate limit ou falha parcial, o app mantém o dado antigo, nunca grava preço inválido e avisa o usuário com a hora da próxima tentativa | Falha sem corromper | Given 429, Then os preços antigos permanecem, uma notificação diz que o Yahoo limitou e quando haverá nova tentativa | Must | [user] |

### Non-functional
| ID | Category | Measurable target | Verification | Source |
|---|---|---|---|---|
| NFR-1 | Volume | Abertura com todos os dados frescos faz 0 requisições ao Yahoo; abertura com dados velhos de 30 tickers faz ≤ 15 requisições (antes: ~100–150 estimadas) | Teste que conta chamadas a um fake do cliente Python/`APIUtils` | [src: design.md §2.1 F13] |
| NFR-2 | Integridade | Nenhum cenário de falha altera `currentUnitValue` ou o histórico para um valor errado (zero fallback de moeda) | Testes de falha por cenário (429, vazio, timeout, info ausente) | [user] |
| NFR-3 | Compatibilidade | Migration Flyway sem perda de dados; banco existente abre e é migrado; Hibernate `validate` passa | Teste de migration + `bootRun` com banco fictício (M-006) | [src: CLAUDE.md §Database] |
| NFR-4 | Observabilidade | Cada chamada ao Yahoo registra no log: script, nº de símbolos, duração, resultado (ok/rate limit/erro) | Inspeção do log em teste | [inferred] |
| NFR-5 | Convenções | Código novo segue ktlint/spotless, header de arquivo (M-012), `TitledValue` (M-010), i18n pt-BR/en sem reescrever o arquivo inteiro | `ktlintCheck spotlessCheck test` | [mem: M-010, M-012] |

### Constraints
- yfinance 1.7.0 e `/usr/bin/python3` no Linux; não passar `requests.Session` customizada ao `yf.Ticker`. [src: CLAUDE.md]
- SQLite com 1 conexão; `ddl-auto: validate`; schema só por Flyway.
- Trabalho direto na `main`; sem commit antes de o dono ver o diff (M-004, M-014).

### Out of scope
- Trocar ou adicionar provedor de dados externo (R8): próximo intent.
- Mudar os cálculos de análise fundamental ou de desempenho.
- Rodar contra a internet real em testes automatizados.

### Glossary
| Termo | Significado |
|---|---|
| TTL | Idade máxima para considerar um dado fresco (padrão 60 min no startup) |
| Cooldown | Pausa sem chamadas ao Yahoo depois de um rate limit |
| Registro agregado | JSON por ticker com os dados de mercado (preço, moeda, site, datas de atualização, extensível) |

### Assumptions & open questions
- A4: o cooldown inicial é de 5 minutos, crescendo se o rate limit se repetir; valor ajustável em constante. Confirmado pelo dono: não.
- A5: o registro agregado é guardado numa coluna JSON no próprio `ticker` (ou tabela 1:1 `ticker_market_data`); a escolha vai para o design em 2.7. Confirmado pelo dono: não.
- A6: a atualização da análise fundamentalista (cache de 7 dias) segue com sua regra atual, só sem ser fonte de preço. Confirmado pelo dono: não.

