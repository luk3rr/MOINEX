# Requirements — ai-chat-history

## Original request
Estou interessado em armazenar os chats de conversa com a IA

## 1.1 Intent & stakeholders

Guardar as conversas do chat com a IA do MOINEX para retomá-las, consultá-las, buscá-las e rastrear as propostas da IA. Stakeholder único: o dono/usuário do app. [src: Q-objetivo] [user]

## 2.3 Requirements

### Functional

| ID | Requisito | Fonte |
|---|---|---|
| FR-1 | Cada conversa do chat é gravada no banco local assim que o usuário envia uma mensagem e a cada resposta da IA, sem ação manual. | [user] |
| FR-2 | Existem várias conversas. O usuário cria uma nova com "Nova conversa"; a anterior permanece guardada. | [src: Q-modelo] |
| FR-3 | Cada conversa tem título, gerado automaticamente a partir da primeira mensagem do usuário, e o usuário pode renomeá-lo. | [src: Q-modelo] |
| FR-4 | O chat mostra a lista de conversas (mais recente primeiro) com título e data da última atividade; o usuário abre uma delas. | [src: Q-objetivo] |
| FR-5 | Ao abrir uma conversa guardada, as mensagens (usuário e IA) são exibidas como estavam e o usuário pode continuar a conversa, com o contexto completo restaurado para o LLM, inclusive resultados de tools e texto dos anexos. | [src: Q-objetivo, Q-conteúdo] |
| FR-6 | São gravados, por mensagem: papel, conteúdo enviado ao LLM, texto exibido, chamadas e resultados de tools, tempo de resposta e data/hora. | [src: Q-conteúdo] |
| FR-7 | O texto extraído dos anexos (CSV/PDF) e o nome do arquivo são gravados junto da mensagem que os enviou. | [src: Q-conteúdo] |
| FR-8 | O usuário apaga uma conversa (com confirmação) e pode apagar todas. | [src: Q-retenção] |
| FR-9 | O usuário busca texto nas conversas guardadas; o resultado lista as conversas e as mensagens que casam, e abrir um resultado leva à conversa. | [src: Q-objetivo] |
| FR-10 | Para cada resposta que gerou propostas (ChangeSet), a conversa guarda todas as propostas com o conteúdo completo e a decisão tomada em cada uma (pendente, aprovada, rejeitada, erro) com data. | [src: Q-objetivo, change request gate 2.4] |
| FR-11 | Ao reabrir uma conversa, todas as propostas guardadas são redesenhadas no painel de revisão, agrupadas por resposta da IA, cada uma com seu estado. A conversa reaberta se comporta como se nunca tivesse sido fechada: pendentes continuam acionáveis (aceitar, rejeitar, editar), uma rejeitada pode ser desfeita como na tela ao vivo e uma aprovada só pode ser consultada. | [src: change request gate 2.4, revisado no checkpoint da UNIT-4] |
| FR-12 | Em Preferências o usuário define um limite de retenção (número máximo de conversas e/ou dias); o padrão é sem limite. Ao exceder, as conversas mais antigas são apagadas automaticamente. | [src: Q-retenção] |
| FR-13 | Ao iniciar o app, conversas que estouram o limite são removidas; nenhuma conversa é apagada sem limite configurado ou ação do usuário. | [src: Q-retenção] |
| FR-14 | Ao abrir a tela do chat, a conversa mostrada é a mais recente (ou uma nova, se não houver nenhuma). | [inferred] |

### Non-functional

| ID | Requisito | Verificação |
|---|---|---|
| NFR-1 | Os dados ficam só no banco SQLite local (`~/.moinex/data`); nada é enviado a serviço externo além do que o chat já envia ao provedor de LLM. | Revisão de código: nenhum cliente novo. |
| NFR-2 | A migração Flyway é aditiva (novas tabelas) e não altera dados existentes; `ddl-auto: validate` passa. | Teste de startup + migração em banco de dados fictício. |
| NFR-3 | Abrir a lista de conversas e abrir uma conversa de 200 mensagens leva menos de 500 ms no hardware do dono; gravar uma mensagem não bloqueia a thread JavaFX. | Teste de serviço com dados gerados + checagem manual. |
| NFR-4 | A busca em 5.000 mensagens retorna em menos de 1 s. | Teste com dados gerados. |
| NFR-5 | Falha ao gravar não derruba o chat: o erro é logado, o usuário vê um aviso e a conversa continua em memória. | Teste de serviço com repositório que falha. |
| NFR-6 | Todos os textos novos existem em pt-BR e en. | Revisão das chaves de i18n. |
| NFR-7 | Nenhum dado de conversa, anexo ou chave de API entra em logs em INFO; o conteúdo do anexo gravado não é duplicado no log. | Revisão de código. |
| NFR-8 | O tema claro e o escuro funcionam em todas as telas novas. | Capturas em `construction/screenshots/` ([mem: M-003, M-009]). |

### Constraints · Out of scope · Glossary

- Restrições: SQLite com conexão única; Hibernate só valida o schema; um `@Service` por domínio. [src: CLAUDE.md]
- Fora do escopo: criptografia do banco; sincronização entre dispositivos; exportar conversa para arquivo; busca semântica; sumarização automática de conversas longas; compartilhar conversa; mudar prompts ou tools da IA.
- Glossário: **conversa** = sequência de mensagens com título; **mensagem** = turno USER, ASSISTANT ou TOOL; **ChangeSet** = lote de propostas da IA.

### Assumptions & open questions
- A1: o texto dos anexos é gravado por inteiro, como o usuário escolheu (aviso: extratos ficam no banco); sem limite de tamanho por anexo. — confirmed: não
- A2: a retenção é por número de conversas e/ou dias, ambos opcionais; o padrão é sem limite. — confirmed: não
- A3: a restauração de contexto envia ao LLM todo o histórico guardado (pode estourar a janela do modelo; tratamento de truncamento fica fora do escopo). — confirmed: não

## 2.4 User stories

Persona única: **Dono do app** (usa o chat para perguntar sobre as finanças e para lançar extratos).

| ID | História | FR | Critério de aceite |
|---|---|---|---|
| US-1 | Como dono, quero que minhas conversas sejam salvas sozinhas, para não perder nada ao fechar o app. | FR-1, FR-6, FR-7 | Dado uma conversa com anexo, quando fecho e reabro o app, então mensagens, tools e texto do anexo estão no banco. |
| US-2 | Como dono, quero começar uma nova conversa sem perder as antigas. | FR-2, FR-14 | Dado uma conversa aberta, quando clico em "Nova conversa", então aparece um chat vazio e a anterior segue na lista. |
| US-3 | Como dono, quero ver e reabrir conversas anteriores pela lista, com título que eu possa mudar. | FR-3, FR-4 | Dado 3 conversas, então a lista mostra título e data, mais recente primeiro; renomear atualiza a lista. |
| US-4 | Como dono, quero continuar uma conversa antiga com o LLM lembrando do contexto. | FR-5 | Dado uma conversa reaberta, quando envio uma pergunta que depende de mensagens anteriores, então o LLM recebe todo o histórico guardado. |
| US-5 | Como dono, quero buscar um texto em todas as conversas. | FR-9 | Dado o termo "fatura", então vejo as conversas e trechos que o contêm e abro a conversa pelo resultado. |
| US-6 | Como dono, quero que as sugestões da IA fiquem salvas e voltem na tela, para continuar aceitando ou rejeitando as pendentes e consultar as que já decidi. | FR-10, FR-11 | Dado uma conversa com 3 propostas (1 aprovada, 1 rejeitada, 1 pendente), quando reabro a conversa, então o painel mostra as 3 agrupadas por resposta com seus estados; a pendente pode ser aceita ou rejeitada, a rejeitada pode ser desfeita (como ao vivo) e a aprovada só abre para consulta. |
| US-7 | Como dono, quero apagar conversas e limitar quanto o app guarda. | FR-8, FR-12, FR-13 | Dado limite de 20 conversas e 25 existentes, ao iniciar o app restam as 20 mais recentes; apagar pede confirmação. |

Cobertura: FR-1..FR-14 estão todos em pelo menos uma história; NFR-1..8 vão para a DoD das Units (2.9).

### Assumptions & open questions
Nenhuma nova.
