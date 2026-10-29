# API / interfaces — MOINEX

Status: human-reviewed (2026-10-29) · Confidence: medium

O app não expõe API de rede; as interfaces são internas e de saída.

## Interfaces consumidas
| Interface | Quem usa | Observações |
|---|---|---|
| Scripts Python (6): `get_stock_price`, `get_stock_price_history`, `get_fundamental_data`, `get_market_indicator_history`, `get_brazilian_market_indicators`, `get_stock_logo` | `APIUtils` (12 chamadas em `service/`) | stdout = uma linha JSON, stderr = logs; timeout 300s; retry nos scripts [src: CLAUDE.md] [src: src/main/resources/scripts/] |
| Provedores LLM: Anthropic, Gemini, OpenAI, OpenRouter | `ai/provider/*`, `LlmProviderFactory` | HTTP via `java.net.http`; sem SDK [src: ai/provider/LlmProvider.kt:18] |
| Ferramentas expostas ao LLM | `ai/tools/*` (`QueryFinanceTool`, `ProposeTransactionTool`, `ProposeInvestmentOperationTool`) | o modelo propõe, o usuário confirma [inferred] |

## Interfaces internas
- Services (`@Service`) são a API da UI; repositórios têm 133 consultas `@Query` JPQL e nenhuma nativa. [src: grep "@Query" em repository/]
- Eventos de UI via `NotificationService` (central e toasts). [src: service/NotificationService.kt]

## Versionamento e erros
Sem versionamento de API. Exceções de domínio em `org.moinex.exception`. [src: src/main/kotlin/org/moinex/exception/]
