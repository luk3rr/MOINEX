# API / interfaces — MOINEX

Status: human-reviewed (2026-11-07) · Confidence: medium

O app não expõe API de rede; as interfaces são internas e de saída.

## Interfaces consumidas
| Interface | Quem usa | Observações |
|---|---|---|
| Scripts Python (6): `get_stock_price`, `get_stock_price_history`, `get_fundamental_data`, `get_market_indicator_history`, `get_brazilian_market_indicators`, `get_stock_logo` | `APIUtils` via `YahooGateway` (12 chamadas em `service/`) | stdout = uma linha JSON, stderr = logs; timeout 300s; retry nos scripts [src: CLAUDE.md] [src: src/main/resources/scripts/] |
| Provedores LLM: Anthropic, Gemini, OpenAI, OpenRouter | `ai/provider/*`, `LlmProviderFactory` | HTTP via `java.net.http`; sem SDK [src: ai/provider/LlmProvider.kt:18] |
| Ferramentas expostas ao LLM | `ai/tools/*`: `QueryFinanceTool`, `ReadTools`, `FindMatchingTransactionsTool`, `ProposeChangesTool` (propõe um `ChangeSet`) | o modelo propõe, o usuário revisa e confirma; as ferramentas `Propose*` antigas foram removidas [src: ai/tools/] [src: areas/ai-agent.md] |
| GitHub Releases | `service/release/GitHubReleaseClient` | consulta a última release para avisar o usuário [src: service/release/] |

## Interfaces internas
- Services (`@Service`) são a API da UI; repositórios têm 133 consultas `@Query` JPQL e nenhuma nativa. [src: grep "@Query" em repository/]
- Eventos de UI via `NotificationService` (central e toasts). [src: service/NotificationService.kt]

## Versionamento e erros
Sem versionamento de API. Exceções de domínio em `org.moinex.exception`. [src: src/main/kotlin/org/moinex/exception/]

## Atualização 2026-11-01: contrato dos scripts Python
Todos escrevem JSON em stdout (exceção: `get_brazilian_market_indicators.py` usa `print(dict)`). Cada métrica de `get_fundamental_data.py` é `{value, type: percent|currency|ratio|number, data_temporality: real_time|historical|calculated, reference_date?}` e as seções são `price_performance`, `profitability`, `growth`, `debt`, `efficiency`, `cash_generation`, `valuation`; sem demonstrativos vem `error: "Financial data not available"`. [src: scripts/get_fundamental_data.py] [src: service/investment/FundamentalAnalysisService.kt:103]

| Script | Chamador |
|---|---|
| get_stock_price.py | APIUtils.fetchStockPrices (TickerService:537, MarketService:117) |
| get_stock_price_history.py | fetchStockPriceHistory (TickerPriceHistoryService:213) |
| get_stock_logo.py | fetchStockLogos (TickerService:669) |
| get_fundamental_data.py | fetchFundamentalAnalysis (FundamentalAnalysisService:81) |
| get_market_indicator_history.py | fetchMarketIndicatorHistory (MarketIndicatorService:165) |
| get_brazilian_market_indicators.py | fetchBrazilianMarketIndicators (MarketService:105) |

## Atualização 2026-11-01
`get_stock_price_history.py` passou a devolver série diária e também `^BVSP`; `get_fundamental_data.py` emite schema v2 com `reason` por métrica e EPS do balanço, e extrai valuation, qualidade e setor para FIIs/ETFs. Os scripts seguem sem sessão `requests` própria no yfinance. [src: src/main/resources/scripts/] [src: src/test/python/]

## Atualização 2026-11-07: contrato dos scripts
Há um sétimo arquivo, `yf_resilience.py`, módulo compartilhado (não é script de entrada). Ele concentra o retry (`retry_call()`: 2 tentativas, 2 s inicial, ×2, jitter, só em rate limit), a classificação de erro e a conversão de moeda; `capture_yfinance_errors()` lê as mensagens do logger `yfinance`, porque `yf.download()` não levanta exceção na 1.7.0. Uma falha por símbolo é `{"error", "error_kind"}` com `error_kind` ∈ `rate_limit | invalid_symbol | currency_unknown | error`; os scripts nunca assumem moeda nem taxa de câmbio: devolvem erro e nenhum preço. `APIUtils` copia o script e `yf_resilience.py` para um diretório temporário privado e cada script põe o próprio diretório no `sys.path`. Não passar `requests.Session` própria ao `yf.Ticker`. [src: src/main/resources/scripts/yf_resilience.py] [src: CLAUDE.md]
