# Requirements — ticker-refresh-action

## Original request
Quero permitir que cada ação no tablewview tenha a opção de atualizar as informações referentes a ela (preço e histórico)

## 2.3 Requirements
### Functional
| ID | Requirement | Acceptance criteria | Priority | Source |
|---|---|---|---|---|
| FR-1 | O menu de linha (clique direito) da tabela da aba Ações/Fundos ganha a ação "Atualizar dados". | Given um ticker na tabela, When clico direito na linha e escolho "Atualizar dados", Then o preço atual e o histórico diário só desse ticker são atualizados (mesmo comportamento do botão em lote, restrito a ele). | Must | [src: Q-Hist] |
| FR-2 | O mesmo menu ganha a ação "Resetar e buscar novamente". | Given um ticker, When escolho a ação e confirmo, Then todo o histórico de preços armazenado dele é apagado e refeito (janela diária + preços das datas de transação e fins de mês) e o preço atual é atualizado. | Must | [src: Q-Reset] |
| FR-3 | O reset pede confirmação antes de apagar. | Given a ação de reset, When escolho, Then um diálogo de confirmação explica que o histórico do ativo será apagado e refeito; cancelar não altera nada. | Must | derivado de FR-2 |
| FR-4 | Resultado e falha avisados por notificação, como no lote. | Sucesso → notificação SUCCESS; falha do preço ou do histórico → notificação ERROR com o símbolo; a tabela e os indicadores recarregam ao fim. | Must | [src: Q-Feedback] |
| FR-5 | Durante a atualização de um ativo, a UI não permite outra atualização concorrente (por linha ou em lote). | Given uma atualização em curso, When tento iniciar outra, Then é bloqueada (reusa `isUpdatingPortfolioPrices` e o ícone girando). | Must | derivado |
| FR-6 | Se o reset falhar no meio, o histórico anterior do ativo é preservado. | Given falha da API durante o reset, Then o histórico antigo continua intacto (operação transacional ou busca antes de apagar). | Must | risco da escolha "Tudo do ativo" |
### Non-functional
| ID | Category | Target | Verification | Source |
|---|---|---|---|---|
| NFR-1 | Responsividade | Nenhum trabalho de rede na thread de UI (`FxUtils.launchBackgroundThenUI`). | revisão de código | convenção do projeto |
| NFR-2 | i18n | Textos novos em `messages_en` e `messages_pt_BR`, só acrescentando chaves (CLAUDE.md). | revisão | CLAUDE.md |
### Constraints · Out of scope
- Só a tabela da aba Ações/Fundos; tickers arquivados, títulos (bonds) e a janela de detalhe ficam fora.
- Sem mudança de banco/migração.
- Teste de serviço para o reset (preservação em falha); a UI segue sem testes (M-003: checklist manual com `make demo`).

### Assumptions & open questions
- A1: o reset reaproveita `processTicker` (backfill desde a primeira compra) + `syncDaily`; a atomicidade de FR-6 será resolvida no 3.5 (ver risco: `@Transactional` com `suspend`).
