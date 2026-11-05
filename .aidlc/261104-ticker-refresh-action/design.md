# Design — ticker-refresh-action

## 2.1 Codebase analysis
Delta sobre `.aidlc/codekb/` (HEAD be661aaa), só a área da tabela de ativos (aba Ações/Fundos).

- Tabela: `ui/main/SavingsStocksFundsController.kt` (`stocksFundsTabTickerTable`, `TableView<Ticker>`). O menu de linha é instalado em `initialize()` via `RowContextMenu.install(table, List<RowAction>)` (`common/ui/RowContextMenu.kt`); hoje: Detalhe, Editar, Excluir, Comprar, Vender, Dividendo, Análise fundamentalista.
- Atualização em lote já existe: `handleUpdatePortfolioPrices()` chama `tickerService.updateTickersPriceFromApi(tickers)` (preço atual, domínio, logos; devolve os que falharam) e depois `tickerPriceHistoryService.syncDailyPriceHistory(tickers)` (histórico diário, janela de `DAILY_WINDOW_MONTHS`; erros por ticker só são logados). Ambos já aceitam uma lista, então servem para um ticker só.
- Feedback do lote: notificações SUCCESS / ERROR (todos falharam) / INFO (alguns falharam), com chaves `SAVINGS_STOCKS_FUNDS_DIALOG_UPDATE_PRICES_*`; botão com ícone girando (`setOff/setOnUpdatePortfolioPricesButton`) e flag `isUpdatingPortfolioPrices`.
- Lacuna: `syncDailyPriceHistory` engole a falha por ticker, então a UI não sabe se o histórico falhou.

### Assumptions & open questions
- A1: a ação por linha reaproveita os dois serviços existentes, sem novo código de serviço.
