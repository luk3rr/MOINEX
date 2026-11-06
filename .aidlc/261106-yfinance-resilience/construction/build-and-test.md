# Build and Test — yfinance-resilience

Data: 2026-11-06 · Comando aprovado: `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && ./gradlew ktlintCheck spotlessCheck test && /usr/bin/python3 -m unittest discover -s src/test/python`

## Resultado (execução final, com `cleanTest`)

| Verificação | Resultado |
|---|---|
| `ktlintCheck` e `spotlessCheck` | exit 0 |
| Testes Kotlin | 1893 testes, 0 falhas, 0 erros, 0 ignorados (baseline antes do intent: Gradle exit 0, contagem não registrada) |
| Testes Python | 100 testes, OK (baseline: 36) |
| Boot com banco fictício (`capture-screens.sh`) | Flyway validou 37 migrations e o Hibernate subiu em `validate`; beans novos montados |
| Capturas de tela | carteira, detalhe do ticker e análise fundamentalista, tema escuro e claro, pt-BR, em `construction/screenshots/` (ignorada pelo git) |
| Checagem manual contra o yfinance real | 3 chamadas com tickers genéricos: preço com moeda conhecida (sem `info`), sem moeda (uma busca), símbolo inexistente (`invalid_symbol`); histórico em lote; moedas dos 10 símbolos de mercado |

## Estratégia de testes: Standard

Unitários por componente, integração nas fronteiras entre Units e checagens de aceitação por requisito.

## Matriz requisito → teste

| Requisito | Testes |
|---|---|
| FR-1 preço único | `FundamentalPriceOverlayTest`; `TickerMarketDataServiceTest`; captura da Análise Fundamentalista |
| FR-2 registro agregado | `TickerMarketDataTest` (JSON legado, incompleto, chaves desconhecidas em `metadata`); `TickerMarketDataMigrationTest` |
| FR-3 última atualização | `TickerMarketDataServiceTest`; `TickerServiceUpdatePricesTest` (momento gravado; falha mantém o anterior); capturas (UI sem teste automatizado) |
| FR-4 TTL | `MarketDataFreshnessTest`; `TickerServiceUpdatePricesTest`; `MarketServiceTest`; `TickerPriceHistoryServiceDailySyncTest`; `YahooRequestVolumeTest` |
| FR-5 sem `info` nem USD | `test_get_stock_price.py`; `TickerServiceUpdatePricesTest`; `YahooRequestVolumeTest` |
| FR-6 histórico em lote | `test_get_stock_price_history.py`; `TickerPriceHistoryServiceDailySyncTest`; `YahooRequestVolumeTest` |
| FR-7 módulo único | `test_yf_resilience.py`; `test_scripts_isolated.py`; `APIUtilsScriptDirectoryTest` |
| FR-8 limite e cooldown | `YahooGatewayTest`; `APIUtilsYahooGatewayTest` |
| FR-9 aviso e dado antigo | `YahooCooldownNotifierTest`; `TickerServiceUpdatePricesTest`; `TickerPriceHistoryServiceDailySyncTest` (batch recusado em cooldown) |
| NFR-1 volume | `YahooRequestVolumeTest`: 30 tickers velhos + cotações velhas = 10 requisições; tudo fresco = 0; moeda aprendida uma vez |
| NFR-2 integridade | testes de falha dos scripts (moeda desconhecida, câmbio indisponível, símbolo inválido) e dos serviços (dado antigo mantido) |
| NFR-3 compatibilidade | `TickerMarketDataMigrationTest` (SQL real em SQLite); boot com banco fictício |
| NFR-4 observabilidade | `YahooGatewayTest` (log por chamada com script, símbolos, duração, resultado) |
| NFR-5 convenções | `ktlintCheck`, `spotlessCheck`; i18n só acrescentada; header de arquivo; `TitledValue` |

## O que mudou no volume (NFR-1)

Estimativa original para 30 tickers na abertura: 100 a 150 requisições (estimativa do 2.1, não medida). Medido em teste, contando um download por 8 símbolos mais uma consulta `info` por símbolo sem moeda guardada:

| Cenário | Requisições |
|---|---|
| 30 tickers velhos com moeda conhecida + cotações velhas | 10 (preços 4, histórico 4, cotações 2) |
| Tudo atualizado há menos de 60 min | 0 |
| 30 tickers sem moeda guardada, primeira vez | 34, depois 4 |

O Ibovespa (`BenchmarkService`, já limitado a uma busca a cada 3 dias) soma no máximo 1.

## Falhas durante a construção

- Nenhuma falha ficou sem causa. Falhas intermediárias foram de teste ou descobriram bugs corrigidos na hora: `yf.shared` inexistente no yfinance 1.7.0 e `retry_call` prendendo `time.sleep` na definição.
- Um `java.io.EOFException` do worker do Gradle apareceu uma vez, sem teste falhando, e não se repetiu em três execuções seguintes. Registrado, causa não investigada.

## Segurança

- Os scripts rodam de um diretório temporário privado (permissão restrita no POSIX) criado por execução e removido depois; antes cada execução deixava um arquivo temporário.
- O argumento `--batch` é um JSON passado por `ProcessBuilder` sem shell, então símbolos e datas não são interpretados por um shell.
- Nenhum endpoint novo, nenhum segredo, nenhuma credencial; nada de dado pessoal nos testes ou nos arquivos (M-011).

## Riscos e o que não foi coberto

- Telas não têm teste automatizado (não há suíte de UI): validação por capturas e pelo checklist abaixo (M-003).
- O comportamento real do Yahoo em rate limit não é reproduzível em teste; foi coberto por simulação do logger e das exceções do yfinance.
- Windows com Python embutido: o módulo compartilhado é encontrado por `sys.path.insert` do diretório do script (não depende do `sys.path` padrão), mas não foi executado no instalador do Windows.
- `scripts/linux/validate-package.sh` e o build dos pacotes não foram rodados neste intent; `yf_resilience.py` é um recurso novo em `resources/scripts` e deve ir junto automaticamente.
- `get_fundamental_data.py` não reduz o número de chamadas por ticker; só unifica o retry e marca `error_kind`.

## Checklist de telas para o dono (M-003), nos dois temas

1. **Portfólio**: a última coluna é "Última atualização" com data e hora; clique em "Atualizar Preços" e confira que a hora muda para todos os tickers.
2. **Detalhe do ticker**: a linha "Última atualização" aparece abaixo de Quantidade, Preço médio e Retorno.
3. **Análise Fundamentalista**: o "Preço Atual" do cartão, do veredito de valuation e da aba de desempenho é o mesmo da tabela, com data e hora na referência.
4. **Abrir o app duas vezes em menos de 1 hora** e acompanhar com `make log`: na segunda, as mensagens `fresh, skipping` aparecem e nenhuma linha `[yahoo]` de preço.
5. **Linha `[yahoo]` no log**: cada chamada registra script, nº de símbolos, duração e resultado.
6. **Rate limit** (quando acontecer): notificação "O Yahoo Finance limitou as requisições" com a hora da próxima tentativa; os preços antigos permanecem.
7. **Windows** (antes de uma release): abrir o instalador e conferir que os preços atualizam.
