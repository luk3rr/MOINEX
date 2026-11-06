# Verificação — Construction (yfinance-resilience)

## 1. Inventário de artefatos
| Artefato | Presente |
|---|---|
| tasks.md com 24 de 24 tarefas marcadas e resumo por Unit | ✓ |
| design.md §3.1 (contratos dos scripts) | ✓ |
| construction/build-and-test.md | ✓ |
| construction/screenshots/ (ignorada pelo git) | ✓ |
| codekb areas/market-data.md atualizado | ✓ |
| CLAUDE.md atualizado (scripts, gateway, migrations) | ✓ |

## 2. Rastreabilidade Unit → código → testes → resultado

| Unit | Código principal | Testes | Resultado |
|---|---|---|---|
| UNIT-3 | `V037__add_ticker_market_data.sql`, `TickerMarketData`, `TickerMarketDataService`, `Ticker.marketData` | `TickerMarketDataTest`, `TickerMarketDataServiceTest`, `TickerMarketDataMigrationTest` | verificada, aprovada |
| UNIT-2 | `YahooGateway`, `APIUtils`, `YahooCooldownException` | `YahooGatewayTest`, `APIUtilsYahooGatewayTest` | verificada, aprovada |
| UNIT-1 | `yf_resilience.py`, scripts de preço e histórico, demais scripts, `prepareScriptDirectory` | `test_yf_resilience`, `test_get_stock_price`, `test_get_stock_price_history`, `test_scripts_isolated`, `APIUtilsScriptDirectoryTest` | verificada, aprovada |
| UNIT-4 | `MarketDataFreshness`, `TickerService`, `TickerPriceHistoryService`, `MarketService`, `YahooCooldownNotifier`, `AppStartupRunner` | `YahooRequestVolumeTest`, `MarketDataFreshnessTest`, `YahooCooldownNotifierTest`, ampliações dos testes de serviço | verificada, aprovada |
| UNIT-5 | `FundamentalPriceOverlay`, controllers e FXML das telas | `FundamentalPriceOverlayTest`; capturas | verificada, aprovada |

Execução final: `ktlintCheck spotlessCheck test` exit 0 (1893 testes, 0 falhas) e 100 testes Python OK.

## 3. Cobertura de NFR
Todos os NFR têm design e validação: ver a matriz em `construction/build-and-test.md`. NFR-1 medido em teste (10 requisições para 30 tickers velhos; 0 para dados frescos).

## 4. Consistência
- Termos e IDs consistentes entre requirements, design e tasks.
- A premissa A4 (cooldown inicial de 5 min) virou a constante `DEFAULT_BASE_COOLDOWN`; A7 (lote de 8) virou `BATCH_SIZE`; A8 (constantes ajustáveis) cumprida. A5/A6 confirmadas na prática.
- Desvios registrados nos resumos de cada Unit em `tasks.md`.

## 5. Veredito
PASS, com riscos aceitos e registrados em `build-and-test.md`: telas sem teste automatizado (checklist ao dono), Windows e pacotes não executados neste intent, comportamento real do Yahoo em rate limit coberto por simulação.
