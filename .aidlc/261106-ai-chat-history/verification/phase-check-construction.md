# Phase check — Construction (261106-ai-chat-history)

## 1. Inventário

| Artefato | Presente |
|---|---|
| Código das 5 Units (migração, entidades, repositórios, serviço, codec, estado da conversa, tela, retenção) | ✓ |
| Testes novos e alterados (ver `construction/build-and-test.md`) | ✓ |
| `construction/build-and-test.md` | ✓ |
| Resumo e `verified` por Unit em `tasks.md` e `state.md` | ✓ (5 de 5) |
| Capturas de tela, tema claro e escuro (não versionadas) | ✓ |

## 2. Rastreabilidade (Unit → código → testes → resultado)

| Unit | Código principal | Testes | Resultado |
|---|---|---|---|
| UNIT-1 | `V038`, `model/ai`, `repository/ai`, `AiChatHistoryService`, `AiChatService.turnMessages` | `AiChatHistoryServiceTest`, `AiChatHistoryMigrationTest`, `AiChatServiceTest` | verified, exit 0 |
| UNIT-2 | `ChatPayloadCodec`, `saveChangeSet`, `updateChange`, `openChat` com propostas | `ChatPayloadCodecTest`, `AiChatHistoryServiceTest` | verified, exit 0 |
| UNIT-3 | `ChatConversationState`, `AiChatController`, `ai_chat.fxml/css` | `ChatConversationStateTest`, captura | verified, exit 0 |
| UNIT-4 | `ReviewSession.load`, `ReviewPanel` | `ReviewSessionStoredTest`, captura | verified, exit 0 (revisão 1 no checkpoint) |
| UNIT-5 | `PreferencesService`, `SettingsController`, `AppStartupRunner` | `PreferencesServiceLimitTest`, `AiChatHistoryServiceTest`, `ChatConversationStateTest` | verified, exit 0 |

Todos os FR e NFR têm verificação na matriz de `build-and-test.md`. FR-14, NFR-1, NFR-6, NFR-7 e NFR-8 dependem de revisão ou checagem manual.

## 3. Consistência

- FR-11 e US-6 foram revisados duas vezes (gate 2.4 e checkpoint da UNIT-4); requirements, design (§2.5) e tasks concordam com o comportamento final: reaberta como se nunca tivesse sido fechada.
- `design.md §2.6` fala em `ChatConversationState` dentro de `ai/chat`; foi para `service/ai` (registrado no resumo da UNIT-3). Contratos de §2.8 seguem corretos (a busca devolve `messageId` nulo para título; `StoredConversation.changeSets`).
- O texto de §2.8 sobre Jackson "versão 2.21.7 no catálogo" virou "versão do BOM do Spring Boot", registrado no resumo da UNIT-2.

## 4. Veredito

**PASS**. Itens em aberto são de validação manual do dono e não bloqueiam o fechamento.
