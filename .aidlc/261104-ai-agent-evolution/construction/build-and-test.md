# Build & Test — ai-agent-evolution (parcial: UNIT-1..3)

- Comando aprovado: `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew ktlintCheck spotlessCheck test`
- Resultado (2026-11-05): BUILD SUCCESSFUL, 1661 testes, 0 falhas.
- Testes novos relevantes: `TransactionMatchingServiceTest`, `TransactionMatchingPerformanceTest` (200 linhas × 10.000 registros < 1 s), `ProposeChangesToolTest`, `ReviewSessionTest`, `WalletAndCreditCardOperationsTest`, `InvestmentOperationsTest`, `OperationRegistryTest`, `PromptComposerTest`, `ChangeSetAuditLoggerTest`, `AiArchitectureTest`, `AgentEvaluationSuite` (9 casos com LLM simulado).
- Capturas (fora do git): `construction/screenshots/ai-chat/` (chat vazio, layout novo) e `construction/screenshots/review-panel/` (painel com propostas fictícias, temas escuro e claro).

## Checklist manual do dono (M-003), com `make demo` ou o app normal e seu LLM

1. Chat abre sem painel; o aviso e o campo de mensagem aparecem como antes.
2. Digite "gastei 45 no mercado ontem no <sua carteira>": aparece o painel à direita com um cartão "Nova", selecionado.
3. Anexe um extrato que contenha (a) um lançamento já registrado e confirmado com outro nome, (b) um pendente equivalente, (c) uma parcela de compra de cartão já registrada, (d) uma linha nova:
   - (a) e (c) viram um aviso no chat com id e motivo, sem cartão;
   - (b) vira cartão "Casa com #id" e Aprovar confirma o pendente mantendo o nome dele;
   - (d) vira cartão "Nova".
4. Peça "lance a (a) mesmo assim": aparece cartão "Possível duplicata" (não selecionado).
5. Editar abre o editor de reserva com os campos da operação; Salvar rascunho atualiza o cartão ("Editada"); Cancelar não muda nada; campo obrigatório vazio mostra o aviso e não fecha.
6. Aprovar um cartão grava (confira na tela de carteira/cartão/investimentos); "Aprovar selecionadas" aprova só os marcados; Rejeitar tira da fila e "Desfazer rejeição" volta.
7. Um cartão com carteira/cartão/ativo não reconhecido mostra o erro de validação e só aprova depois de editar.
8. Tema claro e escuro; idiomas pt-BR e en; "Recolher" e o botão "Revisão (N)" reabrem o painel.
9. Linha do log `org.moinex.ai.audit` (`make log`) para cada aprovação, rejeição ou erro.
10. Pergunte "já lancei <valor> em <data>?": o agente usa `find_matching_transactions`.

## Pontos conhecidos que não foram feitos nesta rodada
Impacto no cartão (FR-26), painel abaixo do chat em janela estreita, atalhos de teclado e `accessibleText`, `use_domain`, "Ver existente", filtro por tipo de operação.
