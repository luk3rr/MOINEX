# Build and test — 261106-ai-chat-history

## Resultado (2026-11-07)

| Comando | Resultado |
|---|---|
| `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && ./gradlew clean ktlintCheck spotlessCheck test bootJar` | BUILD SUCCESSFUL, 2032 testes, 0 falhas, 0 ignorados |
| `/usr/bin/python3 -m unittest discover -s src/test/python` | 100 testes, OK |
| Início do app com a V038 e o Hibernate em `validate` (`scripts/dev/capture-screens.sh`, home temporário, dados fictícios) | OK; a primeira execução achou `response_time_millis INTEGER` contra `Long`, corrigido para `BIGINT` na V038 |

Linha de base antes das mudanças: `./gradlew test` com exit 0 (resultado reaproveitado pelo Gradle, sem falhas).

## Testes novos e alterados

| Classe | Testes | Cobre |
|---|---|---|
| `AiChatHistoryServiceTest` | 45 | criar, listar, abrir, renomear, apagar, buscar, retenção, título automático, falha de gravação, propostas (salvar, atualizar, reabrir, operação desaparecida) |
| `AiChatHistoryMigrationTest` | 7 | V038 em banco novo e repetida; entidades x colunas; busca em 5.000 mensagens < 1 s; abrir 200 mensagens < 500 ms |
| `ChatPayloadCodecTest` | 44 | ida e volta de **cada** operação do registro; formato de datas e dinheiro; campo extra; operação inexistente; payload ilegível |
| `ChatConversationStateTest` | 13 | gravar por turno, tool calls, recuperação após falha, abrir conversa, retenção ao criar |
| `ReviewSessionStoredTest` | 12 | `load`, ganchos de estado, desfazer rejeição, payload ilegível |
| `PreferencesServiceLimitTest` | 3 | leitura dos campos de limite |
| `AiChatServiceTest` (+2 verificações) | 20 | `turnMessages` |
| `AiArchitectureTest` (existente) | — | continua valendo; guiou a mudança de pacote do `ChatConversationState` |

## Matriz requisito para teste

| Requisito | Verificação |
|---|---|
| FR-1 salvar sozinho | `ChatConversationStateTest` (startTurn, finishTurn), `AiChatHistoryServiceTest` (appendMessages) |
| FR-2 várias conversas | `AiChatHistoryServiceTest` (createChat), `ChatConversationStateTest` (reset) |
| FR-3 título automático e renomear | `AiChatHistoryServiceTest` (título de 60 caracteres, renomear, título em branco) |
| FR-4 lista com título e data | `AiChatHistoryServiceTest` (listChats); captura da tela |
| FR-5 retomar com contexto | `AiChatHistoryServiceTest` (openChat com TOOL e tool calls), `ChatConversationStateTest` (load), `AiChatServiceTest` (turnMessages) |
| FR-6 o que se grava por mensagem | `AiChatHistoryServiceTest` (appendMessages) |
| FR-7 anexos | `AiChatHistoryServiceTest` e `ChatConversationStateTest` (nomes só na primeira mensagem do usuário); texto em `content` |
| FR-8 apagar e apagar todas | `AiChatHistoryServiceTest` (ordem filhos antes de pais) |
| FR-9 buscar | `AiChatHistoryServiceTest` (escape, título antes, trecho), `AiChatHistoryMigrationTest` (SQL real no SQLite) |
| FR-10 propostas guardadas com estado | `AiChatHistoryServiceTest` (saveChangeSet, updateChange), `ChatPayloadCodecTest` |
| FR-11 reabrir como se nunca tivesse fechado | `ReviewSessionStoredTest`, `AiChatHistoryServiceTest` (openChat com estados); captura "Resposta 1" |
| FR-12 e FR-13 retenção | `AiChatHistoryServiceTest` (limites), `ChatConversationStateTest`, `PreferencesServiceLimitTest`; início do app em `AppStartupRunner` (manual) |
| FR-14 abrir a mais recente | revisão do controller + checklist manual |
| NFR-1 só local | revisão: nenhum cliente de rede novo |
| NFR-2 migração aditiva | `AiChatHistoryMigrationTest`, início do app com `validate` |
| NFR-3 desempenho de abrir | `AiChatHistoryMigrationTest` (200 mensagens) |
| NFR-4 busca | `AiChatHistoryMigrationTest` (5.000 mensagens) |
| NFR-5 falha de gravação | `AiChatHistoryServiceTest`, `ChatConversationStateTest` |
| NFR-6 i18n | chaves nos dois `.properties` (revisão); suíte existente de paridade dos bundles passa |
| NFR-7 sem conteúdo em log | revisão: só ids e nomes de classe nos logs novos |
| NFR-8 temas | capturas em `construction/screenshots/` (não versionadas) |

## Segurança e privacidade

- O texto dos anexos (extratos) agora fica no banco local (A1, decisão do dono). Nada sai do computador além do envio ao provedor que o chat já fazia.
- Logs novos não trazem conteúdo de conversa, de anexo nem de payload; só ids, contagens e nomes de classe de erro.
- O codec ignora campos desconhecidos e devolve texto bruto somente leitura para payload ilegível; nada ilegível é aplicado.

## Riscos e pendências

- Verificação manual do dono (listas em `tasks.md`, por Unit), nos dois temas, ainda não feita: coluna recolhida, busca com rolagem, renomear e apagar, propostas reabertas, campos de retenção em Configurações.
- O JDBC do app não liga `foreign_keys`: a exclusão de filhos é explícita no serviço (A8).
- Restaurar mensagens TOOL aumenta o contexto enviado ao LLM em conversas longas (A6); o serviço já corta resultados antigos em 2000 caracteres.
- Os arquivos aparecem no índice do git (staged) sem que eu tenha rodado `git add`; nada foi commitado.
