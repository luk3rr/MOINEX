# Phase check — Construction (dialogs-overlay)

## 1. Inventário

| Artefato | Existe |
|---|---|
| código e testes por Unit (UNIT-1..6) | ✓ |
| tasks.md com tarefas marcadas e resumo por Unit | ✓ |
| design.md § 3.1 (UNIT-1) | ✓ |
| construction/build-and-test.md | ✓ |
| construction/screenshots/ (68 imagens, git ignora) | ✓ |

## 2. Rastreabilidade (unit → código → testes → resultado)

| Unit | Código | Testes | Resultado |
|---|---|---|---|
| UNIT-1 | `common/ui/Dialog{Stack,Card,Host,Extensions}.kt`, `dialog.css` | `DialogStackTest`, `CloseDialogTest`, captura `dialog-stack` | verificada |
| UNIT-2 | 78 controllers | `CloseDialogTest`, compilação, `grep` | verificada |
| UNIT-3 | `WindowUtils`, `DataExchangeDialogs`, `DraftEditorDialog`, 58 FXML | capturas dos dialogs | verificada, com T-3.6 manual pendente |
| UNIT-4 | `WindowUtils.showConfirmationDialog`, 26 chamadas `danger` | captura `alert-confirm` | verificada |
| UNIT-5 | `ToastBoard`, `ToastHub`, `NotificationToastOverlay` | `ToastBoardTest`, `ToastHubTest`, captura `toasts` | verificada |
| UNIT-6 | `CapturePlan`, `ScreenshotRunner`, `CLAUDE.md` | captura completa 68 imagens | verificada |

NFRs: NFR-1 (testes existentes verdes: 2288, 0 falhas); NFR-2 e NFR-3 (foco/Tab e tempo de abertura) sem medição formal, a validar no checklist manual; NFR-4 (lógica testável sem display: `DialogStack`, `ToastBoard`, `ToastHub`) cumprida; NFR-5 (contraste) por inspeção das capturas; NFR-6 (migração em lotes com a app rodando) cumprida.

## 3. Consistência
- Contratos 2.8 respeitados; desvios registrados nos resumos das Units (sem `DialogFooter`, sem chave i18n nova, botão destrutivo com "Sim", toasts com ✕ também nos transitórios).

## 4. Veredito: PASS, com pendências manuais do dono
- Checklist manual (M-003): calculadora e calendário com overlay aberto, ESC e Tab em telas reais, dialog aberto a partir de dialog de lista, janela pequena, idioma pt-BR, toast longo de erro.
