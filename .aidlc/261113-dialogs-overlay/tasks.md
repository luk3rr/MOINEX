# Tasks — dialogs-overlay

## 2.9 Delivery plan
- **Verification command:** `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew ktlintCheck spotlessCheck test`  (proposto; aguarda aprovação no gate 2.9)
- Trabalho direto na `main`, sem commit antes de o dono ver o diff (M-004, M-014). Validação visual por capturas em `construction/screenshots/` (M-006, M-009) e checklist de telas nos dois temas (M-003).
- Ordem segue o DAG de design.md § 2.7. Autonomia a decidir no início da Construction.
- Walking skeleton: UNIT-1 (cartão sobre o `rootPane`, protótipo do laço aninhado).

### BOLT-1 — Units: UNIT-1, UNIT-2 — Owner: dono + IA
Goal: infraestrutura do overlay e fechamento por `closeDialog()` sem mudar o que o usuário vê.
Definition of Done: verification command passa; protótipo de 3 níveis funciona; nenhum controller de dialog faz cast para `Stage`.
Confidence hypothesis: o laço aninhado se comporta como `showAndWait` em JavaFX 27 (A9). Risk: foco/teclado em níveis aninhados.

### BOLT-2 — Units: UNIT-3, UNIT-4 — Owner: dono + IA
Goal: formulários, listas, detalhe e confirmações em overlay com o rodapé padrão (FR-17).
Definition of Done: verification command passa; nenhuma janela do SO por dialog; checklist de telas dos 64 FXML nos dois temas.
Risk: dialogs altos e FXML com tamanho fixo; rodapés com botões fora do padrão.

### BOLT-3 — Units: UNIT-5, UNIT-6 — Owner: dono + IA
Goal: toasts no lugar de avisos/sucesso/erro, capturas, limpeza e documentação.
Definition of Done: verification command passa; capturas dos novos estilos nos dois temas; `CLAUDE.md` e codekb atualizados.

## Tasks

### UNIT-1 Fundação do overlay   Status: verified (2026-11-13, exit 0)
- [x] T-1.1 `DialogSize`, `DialogCard` (cabeçalho, ✕, corpo em `ScrollPane`, `DialogFooter(start,end)`) — FR-7, FR-13, FR-17 — files: `common/ui/DialogCard.kt` — test: teste de montagem do cartão sem display (estrutura e classes)
- [x] T-1.2 Lógica pura da pilha (`DialogStack`: empilhar, fechar topo, fechar fora de ordem fecha os de cima) — FR-4 — files: `common/ui/DialogStack.kt` — test: `DialogStackTest` (3 níveis, fora de ordem, idempotência)
- [x] T-1.3 `DialogHost` sobre o `rootPane`: scrim por nível, foco inicial e devolução, ESC no topo, Tab preso, laço aninhado — FR-2, FR-3, NFR-2 — files: `common/ui/DialogHost.kt` — test: stub de `FxUtils` (M-013) + captura manual
- [x] T-1.4 `Node.closeDialog()` com fallback para `Stage` — FR-6 — files: `common/ui/DialogExtensions.kt` — test: `CloseDialogTest`
- [x] T-1.5 CSS `dialog-*` e botões `button-primary/secondary/danger` nos tokens dark/light — FR-7, FR-15, FR-17 — files: `resources/css/component/dialog.css`, tokens — test: captura nos dois temas
- [x] T-1.6 Registro do host no `MainController` (Z-order abaixo dos toasts) e chaves i18n `DIALOG_CLOSE` — FR-2 — files: `ui/main/MainController.kt`, bundles (só acrescentar) — test: captura do protótipo
- [x] T-1.7 Protótipo A9: 3 níveis abertos/fechados na captura de tela — NFR-2 — files: `app/CapturePlan.kt` — test: captura
**Unit summary (Construction 3.5):**
- Arquivos novos: `common/ui/DialogStack.kt`, `DialogCard.kt`, `DialogHost.kt`, `DialogExtensions.kt`, `resources/css/component/dialog.css`, testes `DialogStackTest` (5) e `CloseDialogTest` (3). Alterados: `MainController` (cria o `DialogHost` e o registra em `WindowUtils.dialogHost`), `NotificationToastOverlay` (expõe `node`), `Files` (`DIALOG_CSS`), `base.css` (botão `danger`), tokens dark/light (`theme-on-negative`), `CapturePlan`/`ScreenshotRunner`/`CapturePlanTest` (alvo 33 `dialog-stack`).
- Como ver: `CAPTURE_ONLY=dialog-stack bash scripts/dev/capture-screens.sh <dir>` (imagens em `construction/screenshots/`, git ignora).
- Verificação: `ktlintCheck spotlessCheck test` com BUILD SUCCESSFUL em 2026-11-13; captura clara e escura com profundidade `[3, 2, 1, 0]` ao fechar.
- Desvios: (1) sem chave i18n nova: o tooltip do ✕ reusa `dialog.button.close`; (2) `DialogFooter(start, end)` não foi criado: o cartão tem um slot `footer` e os FXML atuais já trazem os botões centralizados juntos no corpo, a padronização de cada rodapé fica em T-3.4; (3) `DialogHost` e `DialogCard` não têm teste unitário (exigem toolkit/display, M-013); cobertos pela captura do protótipo; (4) botão primário usa o `accent` existente e `danger` é novo.
- Achado (A9): `Platform.enterNestedEventLoop` não pode iniciar durante animação/layout (mesma regra do `showAndWait`), e o laço só sai do mais interno por vez. O host libera um laço por vez e, se o laço não iniciar, desfaz o overlay e relança. Corrotinas que retomam de `delay` precisam abrir o dialog por `Platform.runLater`.

### UNIT-2 Fechamento por `closeDialog()`   Status: verified (2026-11-13, exit 0)
- [x] T-2.1 Listar os usos de `scene.window as Stage`, `stage.close()`, `window.hide()` por pacote — FR-6 — files: `construction/close-usages.md` — test: n/a (inventário)
- [x] T-2.2 Trocar por `closeDialog()` em `ui/dialog/**` (lotes por domínio: wallettransaction, creditcard, investment, financialplanning, goal, wishlist, ai) — FR-6, NFR-6 — test: verification command a cada lote
- [x] T-2.3 Trocar em `ui/main/**`, `ui/common/**`, `dataexchange/ui/**` — FR-6 — test: verification command
- [x] T-2.4 Verificar com `grep` que nenhum controller de dialog faz cast para `Stage` — FR-6 — test: `grep` sem resultados fora de `app/`, calculadora e calendário
**Unit summary (Construction 3.5):**
- 78 arquivos de `src/main/kotlin` trocaram `(x.scene.window as Stage).close()`, `x.scene.window.hide()` e `val stage = x.scene.window as Stage; stage.close()` por `x.closeDialog()` (import acrescentado e `javafx.stage.Stage` removido quando ficou sem uso). A app segue abrindo janelas do SO; `closeDialog()` cai no `Stage` fora de overlay.
- Verificação: `ktlintCheck spotlessCheck test` com BUILD SUCCESSFUL em 2026-11-13.
- Sobras de propósito, tratadas na UNIT-3: `DataExchangeDialogs` (3 `stage.close()` e o ESC), `FieldValidation.kt:104`, `AddGoalController.kt:154` e `FundamentalAnalysisController.kt:636` (chamam `sizeToScene()` na janela, que no overlay seria a janela principal), `SettingsController` e `AiChatController` (donos de `FileChooser`, janela principal, ficam).

### UNIT-3 Formulários, listas e detalhe em overlay   Status: verified (2026-11-13, exit 0)
- [x] T-3.1 `openModalWindow` usa `DialogHost` (título, `onHiddenActions`, tamanho, fallback para `Stage`) — FR-1, FR-5 — files: `WindowUtils.kt` — test: `WindowUtilsOverlayTest` com host falso
- [x] T-3.2 `showDetailDialog` com o cartão largo — FR-12 — test: captura
- [x] T-3.3 `DataExchangeDialogs.newStage` e `DraftEditorDialog` migram para o host — FR-1, US-7 — test: testes existentes de dataexchange + captura
- [x] T-3.4 Padronizar rodapé e botões nos 64 FXML (sair à esquerda; ação à direita; classes de botão), em lotes por domínio — FR-17, FR-13 — test: checklist por domínio nos dois temas
- [x] T-3.7 Remover as chamadas `sizeToScene()` que viram redimensionamento da janela principal (`FieldValidation`, `AddGoalController`, `FundamentalAnalysisController`) — FR-13 — test: captura
- [x] T-3.5 Ajustar tamanhos fixos (`prefWidth/Height`) dos FXML altos/largos para rolar dentro do cartão — FR-13 — test: captura em janela pequena
- [ ] T-3.6 Confirmar calculadora e calendário como janelas livres com um dialog aberto — FR-14 — test: verificação manual
**Unit summary (Construction 3.5):**
- `WindowUtils.openModalWindow` e `showDetailDialog` abrem o `DialogHost` (largura = largura natural do FXML + 18 px de folga da rolagem, mínimo 420; título e ✕ no cabeçalho; `onHiddenActions` ao fechar); sem host cai no `Stage` antigo. `DataExchangeDialogs` (`present`, sem `Stage` quando há host) e `DraftEditorDialog` (`DialogCard` no lugar do `Dialog`) migraram. `Node.refitDialogWindow()` substitui as 3 chamadas de `sizeToScene()` (no overlay não faz nada, para não redimensionar a janela principal). `DialogHost.closeAll()` para as capturas, que agora fotografam a janela principal.
- FXML: 57 botões de ação (`save`, `create`, `confirm`, `add`, `edit`, `rename`, `buy`, `sell`) receberam `styleClass="accent"` e 1 de exclusão `danger`, em `ui/dialog/**`; Fechar passou para a esquerda da ação em `ticker_detail` e `bond_interest_history`. A varredura dos 61 FXML não achou mais sair à direita de ação no rodapé.
- Verificação: `ktlintCheck spotlessCheck test` com BUILD SUCCESSFUL; capturas dos dialogs de lançamento (com erros de validação), rebalanceamento, detalhe do ticker, histórico do título, análise fundamentalista e da pilha, nos dois temas.
- Não verificado nesta Unit (sem exercício manual): T-3.6 (calculadora e calendário abertos com um overlay aberto), ESC/Tab em telas reais, abrir um dialog a partir de outro dialog de lista (coberto só pela pilha do protótipo). Entram no checklist de telas do dono (M-003).
- Desvios: T-3.4 aplicou classes de estilo e a ordem dos botões, mas não moveu os botões de dentro do corpo para o rodapé fixo do cartão (os FXML já os têm centralizados e juntos, que é o padrão pedido); `DialogHost.closeAll` adicionado.

### UNIT-4 Confirmações   Status: verified (2026-11-13, exit 0)
- [x] T-4.1 `ConfirmationCard` (ícone, mensagem, Não à esquerda e primário à direita, perigo, foco em Não, ESC = false) — FR-8, FR-17 — test: `ConfirmationResultTest` (true só por Sim; ESC/fechar = false)
- [x] T-4.2 `showConfirmationDialog` usa o host, com `danger` opcional — FR-8 — files: `WindowUtils.kt` — test: idem
- [x] T-4.3 Marcar `danger = true` nas confirmações de exclusão/descarte entre as 44 chamadas — FR-8 — test: revisão das chamadas + captura
- [x] T-4.4 Confirmação sobre formulário aberto mantém o estado do formulário — FR-4 — test: `DialogStackTest` + verificação manual
**Unit summary (Construction 3.5):**
- `WindowUtils.showConfirmationDialog(header, message, resources, danger = false)` abre um cartão `SMALL` no host: título = header, corpo com ícone (aviso vermelho se `danger`, pergunta se não) e a mensagem, rodapé com Não à esquerda de Sim (primário `accent` ou `danger`), foco inicial em Não, ESC/fechar = `false`, retorno `Boolean` síncrono. Sem host cai no `Alert` antigo. Novos: `Icons.WARNING` (`mdmz-warning`), `Styles.BUTTON_PRIMARY/BUTTON_DANGER/ICON_ACCENT`, `.app-icon.icon-accent`.
- 26 das 44 chamadas ficaram com `danger = true` (chaves de exclusão, remoção e reset de histórico, 20 arquivos); arquivar, desarquivar, reabrir, concluir e marcar como comprado ficam normais.
- Verificação: `ktlintCheck spotlessCheck test` BUILD SUCCESSFUL (o `IconsTest` pegou o literal errado `mdal-warning`, corrigido para `mdmz-warning`); captura `alert-confirm` nos dois temas com Não à esquerda e Sim vermelho à direita.
- Desvios: o texto do botão destrutivo continua "Sim" (chaves existentes), não "Excluir" como no mockup; T-4.4 (confirmação sobre formulário aberto) coberta pelos testes da pilha e pelo protótipo, não por exercício manual.

### UNIT-5 Toasts   Status: verified (2026-11-13, exit 0)
- [x] T-5.1 `NotificationToastOverlay`: toast sem persistência, `sticky` com ✕, mensagem longa com rolagem — FR-10 — test: captura
- [x] T-5.2 `ToastHub` com agrupamento ×N (4 s) e limite de 5 — FR-11 — files: `ui/common/ToastHub.kt` — test: `ToastHubTest` (agrupamento, limite, erro não removido)
- [x] T-5.3 `showInformationDialog`/`showSuccessDialog`/`showErrorDialog` delegam ao hub; fallback para `Alert` sem hub — FR-9, FR-10 — test: `WindowUtilsToastTest`
- [x] T-5.4 Registrar o hub no `MainController`; chave i18n `TOAST_REPEAT_COUNT` — FR-9 — test: captura
- [x] T-5.5 Garantir que o toast não grava no centro de notificações — FR-9 — test: `ToastHubTest` (nenhuma chamada a `NotificationService`)
**Unit summary (Construction 3.5):**
- Novos: `ui/common/ToastBoard.kt` (agrupamento ×N, limite de 5 sem remover erro, fila), `ToastHub.kt` (`ToastSurface`, `show`, `dismissed`, `clear`), testes `ToastBoardTest` (5) e `ToastHubTest` (2). `NotificationToastOverlay` agora implementa `ToastSurface` (`addToast`, `updateToastCount`, `removeToast`), com contador ×N e erro (`sticky`) sem tempo de saída e sem fechar ao clicar; transitório some em 4 s. `WindowUtils.showInformationDialog` vira toast de aviso, `showSuccessDialog` de sucesso e `showErrorDialog` de erro persistente; sem hub cai no `Alert`. O hub é criado no `MainController` (`WindowUtils.toastHub`) e não usa o `NotificationService`, então nada vai ao centro de notificações.
- Verificação: `ktlintCheck spotlessCheck test` BUILD SUCCESSFUL (7 testes novos); captura `toasts` nos dois temas com erro, sucesso e aviso ×3.
- Desvios: os toasts mantêm o ✕ também nos transitórios (o mockup os mostrava sem ✕); mensagem muito longa de erro quebra linha mas não rola (limite de altura não implementado); o texto do toast não é selecionável para copiar; `WindowUtils` sem teste unitário (precisa de toolkit, M-013), coberto pela captura.

### UNIT-6 Fechamento do intent   Status: verified (2026-11-13, exit 0)
- [x] T-6.1 `CapturePlan`: formulário, confirmação normal e destrutiva, pilha, toasts de aviso/sucesso/erro, nos dois temas — FR-16 — test: `capture-screens.sh`
- [x] T-6.2 Remover o código antigo (`Stage`/`Alert` por dialog) que sobrar além do fallback — NFR-6 — test: verification command
- [x] T-6.3 Atualizar `CLAUDE.md` (padrão de dialogs, rodapé, toasts) e o codekb — US-7 — test: revisão
- [x] T-6.4 Revisão geral do diff e checklist final de telas — NFR-1 — test: verification command
**Unit summary (Construction 3.5):**
- Alvos novos `dialog-stack` (33) e `toasts` (34) no `CapturePlan`; capturas dos dialogs e da confirmação adaptadas ao overlay; captura completa de 34 × 2 sem falhas. `CLAUDE.md` documenta os dialogs em overlay, o padrão de botões e os toasts.
- Código antigo: o `Stage`/`Alert` por dialog permanece só como fallback sem host (inicialização, testes), por decisão de design (ADR-4, contrato 2.8). Não foi removido.
- Desvio: o codekb não foi atualizado (a skill de codekb é interativa); recomendar `/ai-dlc codekb update`.

### UNIT-7 Redimensionar dialogs (acréscimo pedido depois da Construction)   Status: verified (2026-11-13, exit 0)
- [x] T-7.1 `DialogResize` (zonas da borda, cálculo centralizado, limites) — FR-18 — files: `common/ui/DialogResize.kt` — test: `DialogResizeTest` (5)
- [x] T-7.2 `DialogSizeStore` e `PreferencesDialogSizeStore` (chave `dialog.size.<nome>`) — FR-18 — files: `common/ui/DialogSizeStore.kt` — test: `DialogSizeStoreTest` (5)
- [x] T-7.3 Alças no `DialogCard` (cursor, arrasto, duplo clique restaura) e chave por dialog em `WindowUtils`, `DataExchangeDialogs` e `DraftEditorDialog` — FR-18 — test: captura `dialog-resize`
**Unit summary (Construction 3.5):**
- Todos os cartões (formulários, listas, detalhe, confirmação, importação/exportação, rascunho da IA) redimensionam pela borda (6 px) ou canto (14 px); o cartão segue centralizado e a variação é o dobro do arrasto; mínimo 320 × 160, máximo a área útil. Tamanho salvo ao soltar o mouse, em `java.util.prefs`; duplo clique na borda apaga o tamanho salvo.
- Verificação: `ktlintCheck spotlessCheck test` BUILD SUCCESSFUL (2298 testes, 0 falhas). Captura `dialog-resize` (alvo 35) arrasta a borda direita com eventos sintéticos: largura 420 para 580 (esperado +160), tamanho salvo `580 × 466` e reaberto com 580.
- Não verificado: arrasto por mouse real em todos os tipos de dialog; conflito do arrasto na borda direita com a barra de rolagem vertical (a borda de 6 px tem prioridade); cursores nas bordas.

### UNIT-8 Ajustes finais (pedidos do dono depois da UNIT-7)   Status: verified (2026-11-13, exit 0)
- [x] T-8.1 Escurecer o fundo sob o dialog: o `dialog.css` só estava no cartão, então o `.dialog-scrim` não era estilizado; o host agora o aplica também ao scrim — FR-2
- [x] T-8.2 `DialogLayout.stretch`: raiz do FXML preenche o cartão (levanta `maxWidth`/`maxHeight` fixos), campos de texto de linhas com ícone ocupam a largura livre, tabelas e listas ocupam a altura livre; `ScrollPane` com `fitToHeight` — FR-13
- [x] T-8.3 Capturas `layout-add-wallet`, `layout-add-wishlist-item`, `layout-add-crypto-exchange` e `layout-add-income` (alvos 36 a 39) em tamanhos maiores que o padrão
**Unit summary:** verificação `ktlintCheck spotlessCheck test` BUILD SUCCESSFUL (2298 testes, 0 falhas). Capturas: fundo escurecido nos dois temas; campos alinhados ao eixo central, esticados na largura; tabela de links da lista de desejos cresce com a altura. Sem teste unitário de `DialogLayout` (controles exigem toolkit, M-013); os dialogs de edição (renomear carteira, alterar saldo, editar categoria, editar transação, editar item da lista de desejos) seguem a mesma estrutura dos capturados, mas não foram capturados um a um.
