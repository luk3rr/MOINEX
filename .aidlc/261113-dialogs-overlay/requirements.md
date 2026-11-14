# Requirements — dialogs-overlay

## Original request
Quero mudar o estilo dos dialogs existente na app. Quero que você mapeie todos. Em geral, eles são novas janelas que tecnicamente bloqueiam o uso da janela principal da app. Quero mudar isso para que eles sejam renderizados sobre a área principal da tela da app, de maneira semelhante ao search. Além disso, quero mudar os dialogs que são apenas informáticos (e.g. sucesso) para notificações. Hoje eles obrigam a pessoa a ler e clicar em ok. Acredito que notificações sejam suficientes. Será que conseguimos definir um estilo novo e padrão para os dialogs? bem como os de confirmação etc.

## 2.3 Requirements

### Functional

| ID | Requirement | Rationale | Acceptance criteria | Priority | Source |
|---|---|---|---|---|---|
| FR-1 | Todo dialog de formulário/lista (as 87 chamadas de `openModalWindow`, `DataExchangeDialogs` e `DraftEditorDialog`) é renderizado como overlay sobre a área da janela principal, sem abrir janela do sistema. | Janelas modais do SO bloqueiam e destoam da app; o dono quer o estilo da busca. | Given um formulário (ex.: Adicionar despesa), When abro, Then ele aparece sobre a janela principal com scrim, e nenhum `Stage` novo é criado. | Must | [src: request] |
| FR-2 | O overlay bloqueia a interação com o conteúdo por baixo (scrim) e devolve o foco ao conteúdo ao fechar. | Mantém a semântica modal atual. | Given um dialog aberto, When clico ou digito atrás, Then nada por baixo reage; ao fechar, o foco volta ao controle anterior. | Must | [src: 2.1] |
| FR-3 | ESC fecha o dialog do topo; clicar no scrim **não** fecha formulários nem confirmações. | Evita perder dados digitados por clique acidental. | Given um formulário com campos preenchidos, When clico no scrim, Then continua aberto; When aperto ESC, Then fecha. | Must | [src: A2] |
| FR-4 | Dialogs podem empilhar: um dialog aberto a partir de outro aparece sobre ele, e fechar o do topo revela o anterior com o estado intacto. | Fluxos atuais abrem confirmações/edições a partir de formulários e listas. | Given uma lista com uma edição aberta, When abro uma confirmação, Then ela cobre a edição; When fecho a confirmação, Then a edição segue como estava. | Must | [src: Q3] |
| FR-5 | A abertura de um dialog continua síncrona para o chamador (retorna quando fecha), e a confirmação continua devolvendo `Boolean`. | Preserva as 87+44 chamadas existentes e os recarregamentos pós-fechamento. | Given uma tela que abre "Adicionar" e atualiza a lista depois, When fecho o dialog, Then a lista atualiza sem alterar o código do chamador além da API. | Must | [src: 2.1] |
| FR-6 | Existe um mecanismo único e padrão para um dialog se fechar sem depender de `Stage` (substitui `(campo.scene.window as Stage).close()`). | 81 arquivos dependem de `Stage`. | Given qualquer controller de dialog migrado, Then não há cast para `Stage` nem `window.hide()` nele. | Must | [src: 2.1] |
| FR-7 | Existe um estilo padrão de cartão de dialog: cabeçalho com título e botão fechar, corpo, rodapé de ações; vale para formulários, listas, confirmação e detalhe. | "Definir um estilo novo e padrão". | Given dois dialogs de tipos diferentes, Then compartilham cabeçalho, margens, cantos, sombra e rodapé conforme os tokens de tema. | Must | [src: request] |
| FR-8 | Confirmações (`showConfirmationDialog`) usam o mesmo cartão, com título, mensagem, botão secundário (Não/Cancelar) e primário (Sim/Confirmar); ação destrutiva usa estilo de perigo. | Pedido de padronizar confirmações. | Given excluir um item, When a confirmação aparece, Then o botão de confirmar tem estilo de perigo e Não é o foco inicial. | Must | [src: request] |
| FR-9 | `showInformationDialog` e `showSuccessDialog` passam a mostrar um **toast transitório** (sem OK, some sozinho), que **não** é gravado no centro de notificações. | O dono não quer obrigar a ler e clicar em OK; avisos de validação não devem poluir o histórico. | Given clico em Editar sem seleção, When o aviso aparece, Then é um toast que some em poucos segundos, sem bloquear, e o centro de notificações não ganha item. | Must | [src: request, Q1] |
| FR-10 | `showErrorDialog` passa a mostrar um toast de erro **que não some sozinho** até ser fechado, com a mensagem completa. | Erro não deve desaparecer antes de ser lido. | Given falha ao salvar, When o erro aparece, Then permanece até eu fechar, sem bloquear a tela. | Must | [src: Q2] |
| FR-11 | Toasts transitórios do mesmo texto em sequência são agrupados (não empilham duplicatas) e a pilha tem limite de itens visíveis. | Validações repetidas (vários cliques) não podem encher a tela. | Given cinco cliques seguidos em Editar sem seleção, Then aparece um único toast (renovado). | Should | [src: inferido] |
| FR-12 | `showDetailDialog` passa a usar o cartão de dialog padrão (overlay) com o `Node` de conteúdo. | Único detalhe existente; segue o padrão. | Given o detalhe aberto, Then aparece como overlay com botão fechar. | Must | [src: 2.1] |
| FR-13 | Dialogs altos ou largos (listas de transações, rebalanceamento, detalhe de ticker) cabem na janela: o cartão limita-se à área útil e o corpo rola quando necessário. | Hoje cada janela tem tamanho próprio; o overlay tem a área da app. | Given janela principal pequena, When abro um dialog alto, Then o cabeçalho e o rodapé ficam visíveis e o corpo rola. | Must | [src: 2.1] |
| FR-14 | Calculadora e Calendário permanecem janelas separadas e não modais. | Ferramentas auxiliares. | Given a calculadora aberta, When abro um dialog overlay, Then a calculadora continua utilizável. | Must | [src: Q4] |
| FR-15 | Todo texto usa o bundle i18n (pt-BR e en) e o tema claro/escuro; sem cor fixa fora dos tokens. | Regras do projeto (CLAUDE.md, M-001). | Given troca de tema ou idioma, Then o overlay e os toasts seguem. | Must | [src: CLAUDE.md] |
| FR-17 | Rodapé padronizado em todos os dialogs: botões de sair (Cancelar, Fechar, Não, Voltar) sempre à esquerda dos botões de ação (Salvar, Sim, Confirmar, Editar, Excluir etc.), que ficam à direita deles. A regra é de posição relativa entre os botões: os botões ficam sempre juntos, lado a lado (gap 8 px), nunca separados nas pontas do cartão; o grupo fica centralizado (padrão) ou à direita. Estilos de botão: primário (roxo, preenchido), secundário (contornado) e perigo (vermelho, para operações destrutivas como Excluir e Descartar). | Consistência pedida pelo dono; estilos iguais ao mockup. | Given qualquer dialog migrado, Then, no grupo de botões, os de sair ficam à esquerda dos de ação (o grupo pode estar centralizado); Given uma operação destrutiva, Then o botão de confirmar é vermelho. | Must | [src: pedido do dono em 2.8] |
| FR-16 | O `ScreenshotRunner`/`CapturePlan` captura os novos dialogs (formulário, confirmação, toast de aviso e de erro) nos dois temas. | Validação visual (M-003, M-006). | Given `capture-screens.sh`, Then geram imagens dos novos estilos. | Should | [src: M-003] |

| FR-18 | Todo dialog em overlay pode ser redimensionado arrastando a borda ou o canto com o mouse; o cartão continua centralizado e não pode ser movido; o tamanho fica entre um mínimo (320 × 160) e a área útil da janela e é lembrado por dialog entre aberturas; duplo clique na borda volta ao tamanho padrão. | Pedido do dono depois da Construction. | Given um dialog aberto, When arrasto a borda direita 80 px, Then a largura cresce 160 px e, ao reabrir o dialog, o tamanho é o mesmo. | Must | [src: pedido do dono, 2026-11-13] |

### Non-functional

| ID | Category | Measurable target | Verification | Source |
|---|---|---|---|---|
| NFR-1 | Compatibilidade | Zero mudança de regra de negócio; mesmos retornos (`Boolean`, recarregamentos) nos chamadores | testes existentes passam (`ktlintCheck spotlessCheck test`) | [src: 2.1] |
| NFR-2 | Usabilidade/teclado | Foco inicial no primeiro campo (ou no botão seguro da confirmação); Tab não escapa do dialog do topo; ESC fecha o topo | checagem manual guiada + teste do gerenciador de foco/pilha | [src: FR-2, FR-3] |
| NFR-3 | Desempenho | Abrir um dialog leva ≤ 150 ms perceptíveis (sem criar janela do SO), sem travar a UI | medição manual nas telas maiores | [src: inferido] |
| NFR-4 | Testabilidade | A lógica de pilha/fechamento/resultado fica fora do JavaFX o bastante para teste unitário sem display (M-013) | testes unitários passam sem display | [mem: M-013] |
| NFR-5 | Acessibilidade | Contraste dos toasts e botões conforme os tokens existentes nos dois temas | revisão das capturas nos dois temas | [src: FR-15] |
| NFR-6 | Migração segura | Migração por lotes de telas, com a app compilando e rodando a cada lote | verificação por Unit | [src: inferido] |

### Constraints
- Kotlin + JavaFX + AtlantaFX; sem nova dependência. Cores só por tokens `theme-*`; ícones por `Icons.kt`; identidade visual preservada (M-001).
- Mantém o padrão do overlay existente: o `rootPane` `AnchorPane` do `MainController`; toasts via `NotificationToastOverlay`.
- Trabalho direto na `main`, sem commit antes de o dono ver o diff (M-004, M-014).
- Mockups em HTML na pasta `mockups/` (M-007, M-008).
- i18n: só acrescentar/editar chaves, nunca reescrever o arquivo inteiro.

### Out of scope
- Calculadora e Calendário (continuam janelas).
- `FileChooser`/`DirectoryChooser` nativos, `Popup` de sugestões/tooltips, menus de contexto.
- Alterar o conteúdo ou as regras de validação dos formulários; trocar textos das mensagens (exceto onde o toast exigir encurtar).
- Notificações persistentes do centro de notificações (continuam como estão).

### Glossary
- **Overlay**: camada sobre a área principal da janela (`rootPane`), com scrim, no estilo da busca.
- **Scrim**: fundo escurecido que bloqueia o conteúdo por baixo.
- **Toast transitório**: aviso que some sozinho e não é gravado no banco.
- **Toast persistente de erro**: aviso que fica até o usuário fechar.

### Assumptions & open questions
- A13 (2.3, revisão): "Fechar" de listas/detalhes fica à esquerda; ações como Editar e Excluir de uma lista ficam à direita dele (sempre em posição relativa); ✕ do cabeçalho continua em formulários e listas, no canto direito (confirmado pelo dono; a regra esquerda/direita vale só para o rodapé) — confirmed: no
- A1 (2.3): `showInformationDialog` com mensagem de **ação concluída** e `showSuccessDialog` viram o mesmo toast transitório, com ícone de sucesso só no segundo — confirmed: yes (gate 2.3)
- A2 (2.3): clicar no scrim não fecha nada (nem confirmações), só ESC e os botões — confirmed: yes (gate 2.3)
- A3 (2.3): toast transitório dura ~4 s, o de erro até fechar; tempos e posição finais saem do mockup — confirmed: yes (gate 2.3)
- A4 (2.3): `DataExchangeDialogs` e `DraftEditorDialog` migram junto, usando a mesma infraestrutura — confirmed: yes (gate 2.3)
- A5 (2.3): a migração é toda de uma vez no mesmo intent (todas as chamadas), em Units por grupo de telas — confirmed: yes (gate 2.3)
## 2.4 User stories

Persona única: **Dono/usuário** da app, que usa a app no desktop para registrar e consultar suas finanças. [src: A6]

### US-1 Abrir formulários sem sair da janela  (FR-1, FR-2, FR-3, FR-13, FR-15)
Como usuário, quero que os formulários e listas abram sobre a janela principal, para não perder o contexto nem lidar com janelas soltas do sistema.
- AC1: Given qualquer tela, When abro Adicionar/Editar/Lista, Then aparece sobre a área principal com scrim e sem nova janela do SO.
- AC2: Given o dialog aberto, When clico no scrim, Then nada fecha; When aperto ESC, Then fecha e o foco volta ao controle anterior.
- AC3: Given janela pequena e dialog alto, Then cabeçalho e rodapé ficam visíveis e o corpo rola.
- AC4: Given troca de tema ou idioma, Then o dialog segue.

### US-2 Fluxos com dialog sobre dialog  (FR-4, FR-5, FR-6)
Como usuário, quero abrir uma edição ou confirmação a partir de outro dialog e voltar ao anterior intacto, para seguir os fluxos de hoje sem perder o que digitei.
- AC1: Given uma lista com edição aberta, When abro uma confirmação e a fecho, Then a edição continua com os mesmos valores.
- AC2: Given um dialog fechado (salvar ou cancelar), Then a tela de origem atualiza como hoje.

### US-3 Confirmações claras e padronizadas  (FR-7, FR-8, FR-17)
Como usuário, quero confirmações no mesmo estilo dos demais dialogs, com a ação destrutiva destacada, para decidir com segurança.
- AC1: Given excluir um item, Then o botão de confirmar tem estilo de perigo e o foco inicial fica em Não.
- AC3: Given qualquer dialog, Then Cancelar/Não/Fechar ficam à esquerda de Sim/Salvar/Excluir, que ficam à direita deles (posição relativa, o par pode estar centralizado); ação destrutiva em vermelho.
- AC2: Given Sim ou Não, Then o chamador recebe `true`/`false` como hoje.

### US-4 Avisos que não exigem clique  (FR-9, FR-11)
Como usuário, quero que avisos e sucessos apareçam como toast que some sozinho, para não precisar clicar em OK.
- AC1: Given Editar sem seleção, Then aparece um toast de aviso que some em poucos segundos e o centro de notificações não ganha item.
- AC2: Given vários cliques seguidos, Then há um único toast do mesmo texto.

### US-5 Erros que não somem antes de eu ler  (FR-10)
Como usuário, quero que o erro fique visível até eu fechá-lo, sem bloquear a tela, para ler e copiar a mensagem.
- AC1: Given falha ao salvar, Then o toast de erro permanece até eu fechar.

### US-6 Ferramentas auxiliares continuam livres  (FR-14)
Como usuário, quero continuar usando a calculadora e o calendário ao lado da app, para consultar valores enquanto preencho um formulário.
- AC1: Given um dialog overlay aberto, When uso a calculadora, Then ela responde.

### US-7 Detalhe e demais dialogs no mesmo padrão  (FR-7, FR-12, FR-16)
Como dono, quero que detalhe, importação/exportação e o editor de rascunho da IA tenham o mesmo cartão, e que as capturas de tela os mostrem, para validar o visual.
- AC1: Given `capture-screens.sh`, Then geram imagens do formulário, da confirmação, do toast de aviso e do de erro nos dois temas.

### Mapa FR → US

| FR | US |
|---|---|
| FR-1, FR-2, FR-3, FR-13, FR-15 | US-1 |
| FR-4, FR-5, FR-6 | US-2 |
| FR-7, FR-8, FR-17 | US-3, US-7 |
| FR-9, FR-11 | US-4 |
| FR-10 | US-5 |
| FR-12, FR-16 | US-7 |
| FR-14 | US-6 |

### Assumptions & open questions
- A6 (2.4): uma única persona (o dono) — confirmed: no
