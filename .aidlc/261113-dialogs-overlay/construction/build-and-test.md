# Build and test — dialogs-overlay

## Resultado

- Comando aprovado: `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew ktlintCheck spotlessCheck test`
- Última execução (2026-11-13, depois da última mudança de código): BUILD SUCCESSFUL; 2288 testes, 0 falhas, 0 erros, 0 ignorados (linha de base antes da intent: verde, tarefas up-to-date).
- Testes novos: `DialogStackTest` (5), `CloseDialogTest` (3), `ToastBoardTest` (5), `ToastHubTest` (2); `CapturePlanTest` ajustado para 34 alvos.
- Capturas de regressão (`scripts/dev/capture-screens.sh`, banco fictício): 34 alvos × 2 temas = 68 imagens, sem alvo que não abriu, sem falha, app encerrada sozinha. Imagens em `construction/screenshots/` (git ignora).

## Requisito → teste

| Requisito | Evidência | Lacuna |
|---|---|---|
| FR-1 overlay em vez de janela | capturas `16`, `23`, `25`, `29`, `19` com o dialog sobre a janela principal | sem teste automatizado de que nenhum `Stage` é criado (a checagem foi por captura e por `grep`) |
| FR-2 scrim e foco | `DialogHost` + captura do protótipo | foco/Tab não exercitados em tela real |
| FR-3 ESC e clique no scrim | implementado em `DialogHost.handleKey`; scrim sem handler de clique | não exercitado manualmente |
| FR-4 pilha | `DialogStackTest`; captura `33-dialog-stack` (3 níveis, profundidade 3, 2, 1, 0) | abrir dialog a partir de dialog de lista em tela real não exercitado |
| FR-5 chamada síncrona, `Boolean` | protótipo (`show` bloqueia e retorna), confirmação `18-alert-confirm` | sem teste automatizado (precisa de toolkit) |
| FR-6 `closeDialog()` | `CloseDialogTest`; 78 arquivos migrados; `grep` sem `scene.window as Stage` nos controllers | |
| FR-7 cartão padrão | capturas dos dialogs nos dois temas | |
| FR-8 confirmação | captura `18-alert-confirm` (Não à esquerda, Sim `danger`); 26 chamadas com `danger = true` | |
| FR-9, FR-10, FR-11 toasts | `ToastBoardTest`, `ToastHubTest`; captura `34-toasts` (erro, sucesso, aviso ×3) | `WindowUtils` não tem teste (toolkit) |
| FR-12 detalhe | `showDetailDialog` migrado | não coberto por captura própria |
| FR-13 rolagem e largura | capturas `23` (detalhe longo) e `29` | janela pequena não testada |
| FR-14 calculadora e calendário | `openPopupWindow` inalterado | abrir a calculadora com overlay aberto não exercitado |
| FR-15 tema e i18n | capturas nos dois temas; nenhuma chave nova | idioma pt-BR não capturado |
| FR-16 capturas | alvos 33 e 34 novos; 16/18/19/23/25/29 adaptados | |
| FR-17 rodapé | varredura dos 61 FXML (sair antes de ação), `accent`/`danger` nos botões, mockup | texto do botão destrutivo é "Sim" |

## Segurança e riscos

- Sem segredos nem dados pessoais nos arquivos; as capturas usam banco fictício e `user.home` temporário.
- Risco residual: o laço de eventos aninhado não pode iniciar durante animação ou layout (mesma regra do `showAndWait`); uma chamada a `showErrorDialog`/`showInformationDialog` a partir de qualquer thread cai em `Platform.runLater`, mas abrir um dialog de uma corrotina retomada por `delay` deve usar `Platform.runLater` (documentado em `CLAUDE.md`).

## Falhas durante a Construction

- Primeira captura do protótipo travou a app: a causa era o protótipo abrir overlays a partir de uma corrotina retomada por animação; corrigido com `Platform.runLater` e o host passou a desfazer o overlay e relançar se o laço não iniciar.
- `IconsTest` reprovou o literal `mdal-warning`; corrigido para `mdmz-warning`.
