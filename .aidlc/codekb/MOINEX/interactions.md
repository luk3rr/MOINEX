# Interactions — MOINEX

Status: human-reviewed (2026-10-29) · Confidence: medium-high

## 1. Inicialização
```mermaid
sequenceDiagram
  participant M as main()
  participant J as JavaFXApp
  participant S as SpringApp
  participant FX as FXMLLoader
  M->>J: Application.launch
  J->>S: init() → start(args)
  S-->>J: ApplicationContext (Flyway, JPA)
  J->>J: start(): splash (UNDECORATED)
  J->>FX: carrega main.fxml (background, controllerFactory = getBean)
  FX-->>J: mainRoot
  J->>J: Scene(mainRoot).show(), fecha splash
  S->>S: AppStartupRunner (recorrentes + mercado em coroutines)
```
[src: app/JavaFXApp.kt:44-110] [src: app/AppStartupRunner.kt:42-120]

## 2. Navegação entre telas
`MainController.loadContent(fxml, ...)` carrega o FXML na `contentArea`, adiciona folhas de estilo da tela e chama `themeService.applyTo(newContent)`. [src: ui/main/MainController.kt:271-295]

## 3. Troca de tema
```mermaid
sequenceDiagram
  participant U as Usuário
  participant T as ThemeService
  participant P as PreferencesService
  U->>T: toggleAndApply()
  T->>P: toggleTheme()
  T->>T: applyTo(mainRoot): troca tokens.* e recolore ícones
```
Janelas já abertas não são atualizadas; novas janelas recebem o tema vigente em `WindowUtils`. [src: service/ThemeService.kt:30-50] [src: common/util/WindowUtils.kt]

## 4. Diálogo modal
`WindowUtils.openModalWindow(fxml, title, ctx, controllerSetup)` → `Stage` APPLICATION_MODAL → `Scene` com tokens + `common-styles.css` → `showAndWait()`. [src: common/util/WindowUtils.kt:140-185]

## 5. Cotações
`TickerService.updateAllNonArchivedTickersPrices` → `APIUtils` → script Python → JSON → persiste preço e histórico. [src: service/investment/TickerService.kt] [inferred]

## 6. Chat de IA
`AiChatService` → `LlmProvider` → resposta markdown (commonmark) renderizada em `WebView` com CSS dependente do tema. [src: ai/chat/MarkdownRenderer.kt] [src: ui/main/AiChatController.kt:549-620]
