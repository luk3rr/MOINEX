# Build & Test — ai-agent-evolution (UNIT-1..8)

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

## UNIT-4 (domínio Lançamentos): resultado e checklist manual

- Resultado (2026-11-05): BUILD SUCCESSFUL, 1709 testes, 0 falhas. Novos: `ReviewSessionMatchTest`, `DuplicateOverrideTest`, `LancamentosOperationsTest`, `CategoryCardDependencyTest`, casos 10 a 12 do `AgentEvaluationSuite`.
- Checklist manual (M-003), no app com seu LLM:
  1. Linha que casa com 2 lançamentos pendentes: o cartão lista os dois com pontuação; escolher o outro e Aprovar confirma o escolhido (e não o primeiro).
  2. "lance a (a) mesmo assim": cartão "Possível duplicata", desmarcado; Aprovar cria um novo e não mexe no existente.
  3. "gastei 80 no pet shop" com categoria que não existe: aparece "Nova categoria: Pets" antes do lançamento; Aprovar só o lançamento mostra "Aprove antes: …" e não grava; Aprovar os dois cria a categoria e depois o lançamento; Rejeitar a categoria e aprovar o lançamento também bloqueia.
  4. "mude o valor do lançamento #N para X": cartão "Editar lançamento" com "amount: antigo → novo"; o status do lançamento não muda; tente em um lançamento com categoria arquivada (deve manter a categoria).
  5. "transfira 500 da carteira A para a B": cartão de transferência (não receita + despesa); origem igual ao destino mostra erro.
  6. Recorrente (mensal com data final), editar só a frequência; carteira (criar, renomear, arquivar, desarquivar); categoria (criar, renomear, arquivar). Nada de excluir.
  7. Editar num cartão de despesa/receita abre a tela real "Adicionar despesa/receita" preenchida (título "Editar proposta"); Salvar atualiza o cartão ("Editada") sem gravar no banco; Cancelar não muda nada. Nos demais tipos (transferência, recorrente, carteira, categoria) abre o editor de reserva; carteira arquivar/desarquivar e categoria arquivar não têm Editar.
  8. Temas claro e escuro; pt-BR e en (rótulos `aioperation.*` e `aidraft.field.*` novos).
  9. Linhas `org.moinex.ai.audit` no `make log` para cada aprovação, rejeição ou erro, inclusive o bloqueio por dependência.

## UNIT-5 a UNIT-8 e fechamento

- Resultado final (2026-11-05): `env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew ktlintCheck spotlessCheck test` → BUILD SUCCESSFUL, 1731 testes, 0 falhas. O contexto Spring sobe com todos os beans novos (captura do chat com o runner de capturas, tema claro e escuro, banco fictício).
- Testes novos: `CreditCardManagementOperationsTest`, `TickerOperationsTest`, `PlanningOperationsTest`, `ReadToolsTest`.
- Matriz requisito → teste: FR-1..8 `ReviewSessionTest`/`ReviewSessionMatchTest`; FR-10..16 `TransactionMatching*`/`DuplicateOverrideTest`; FR-19/20/24 `LancamentosOperationsTest`/`CategoryCardDependencyTest`/`ProposeChangesToolTest`; FR-21 `CreditCardManagementOperationsTest`; FR-22 `TickerOperationsTest`; FR-23 `PlanningOperationsTest`; FR-25 `ReadToolsTest`; FR-29 `AgentEvaluationSuite`; FR-9 `ReadToolsTest` (rótulos); NFR-9 `AiArchitectureTest` e as 30+ operações como classes isoladas.
- Segurança: nenhuma operação de exclusão (o `OperationRegistry` recusa nomes `delete/remove/drop/purge`); o assistente só lê e propõe, e toda gravação passa por `ReviewSession`; log de auditoria por decisão.

## Checklist manual do dono para UNIT-5 a UNIT-8 (M-003)
1. Cartão: "crie o cartão X da operadora Y, fecha dia 5 e vence dia 12, limite 3000"; editar só o limite de um cartão (cartão mostra antes→depois); arquivar um cartão com e sem pagamentos pendentes (o app deve recusar o segundo); "lance um cashback de 15 no cartão X".
2. Ativos: "cadastre o ativo ABC3.SA, ação"; renomear um ativo (quantidade e preço não mudam); "atualize os dados do PETR4" e "resete e busque de novo o histórico do PETR4" (confere preço e histórico na tela do ativo).
3. Planejamento: "crie a meta Viagem de 8000 até <data futura>"; mudar só a data de uma meta (o saldo não muda); arquivar uma meta; criar e editar um item da wishlist (links e status preservados).
4. Leitura: "busque lançamentos com 'mercado' acima de 100 em outubro" (conferir total e páginas; pedir a página 2); "liste minhas carteiras/categorias/cartões/ativos/recorrentes/metas".
5. Indicador: durante uma consulta longa o texto "pensando…" mostra o que está sendo lido (ex.: "lendo lançamentos").
6. Conferir pt-BR e en nos rótulos novos (`aioperation.*`, `aidraft.field.*`, `aitool.label.*`).

## Não feito (consolidado)
Dívida e recorrente de cartão (editar); câmbio de cripto; título (criar/editar/arquivar); edição de compra/venda/dividendo; plano financeiro, grupos de orçamento e preferências; rascunho nos diálogos reais de cartão, investimento, planejamento, transferência, recorrente, carteira e categoria (usam o editor de reserva); chips de ação rápida e links para telas; chamadas paralelas de tools; `use_domain`; impacto no cartão (FR-26); painel abaixo do chat em janela estreita; atalhos de teclado e `accessibleText`.

## Adendo de 2026-11-05 (pedido do dono)
- Entrou: cobrança recorrente no cartão (criar/editar), títulos (criar/editar) e quebra de linha no chat (Enter envia, Shift+Enter quebra; o campo cresce até 5 linhas; a bolha do usuário preserva as quebras). `list_records` ganhou `bonds` e `card_recurring`.
- Verificação: ktlintCheck spotlessCheck test → BUILD SUCCESSFUL, 1739 testes aproximadamente, 0 falhas; novo `RecurringCardAndBondOperationsTest`; captura do chat nos dois temas ok.
- Checklist manual extra: (1) no chat, Shift+Enter quebra a linha, Enter envia, e uma mensagem de 3 linhas aparece com 3 linhas na bolha; (2) "crie uma assinatura de 39,90 todo dia 10 no cartão X, categoria Streaming" gera o cartão (com cartão de categoria se for nova); editar só o valor mantém início e status; (3) "cadastre o CDB Banco Z, vence 2028-05-01, 110% do CDI" e depois mudar só o vencimento.
- Ainda não feito: arquivar título, editar dívida avulsa de cartão, câmbio de cripto, edição de compra/venda/dividendo.
