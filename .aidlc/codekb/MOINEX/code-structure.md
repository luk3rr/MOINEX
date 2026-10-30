# Code structure — MOINEX

Status: human-reviewed (2026-10-29, delta pós v2.19.0) · Confidence: high

## Mapa (profundidade ≤3)
```
src/main/kotlin/org/moinex/
  app/          App → JavaFXApp → SpringApp, AppStartupRunner
  config/       AppConfig, AppDispatchers, FlywayConfig, RetryConfig, BalanceCalculationConfig, AiPromptProperties
  ai/           chat, credentials, ingest (CSV/PDF), provider (Anthropic/Gemini/OpenAI/OpenRouter), tools
  service/      27 arquivos por domínio (wallet, creditcard, goal, investment, networth, ...)
  repository/   36 repositórios Spring Data JPA
  model/        36 @Entity + 30 enums + dto
  ui/main/      19 controllers das telas
  ui/dialog/    78 controllers de diálogo (create/update/view/base por domínio)
  ui/common/    14 painéis e células reutilizáveis
  common/       chart/ (7 gráficos custom), constant/ (Files, Styles, TranslationKeys), util/, extension/, helper/, retry/
src/main/resources/
  ui/{main,dialog,common}/*.fxml   (93 arquivos)
  css/{theme,common,component,scene}/   (33 arquivos, ~2.200 linhas)
  icon/ (82 PNGs)   i18n/   db/migration/ (34)   scripts/ (6 Python)
src/test/kotlin/org/moinex/   service/, model/, ai/, factory/  (nenhum teste em ui/)
```
[src: find em src/main e src/test]

## Pontos de entrada
`App.kt:12 main()` → `JavaFXApp` → `SpringApp.start()`. [src: app/]

## Onde adicionar
| O quê | Onde |
|---|---|
| Nova tela principal | FXML em `resources/ui/main/`, controller em `ui/main/`, registrar botão em `MainController`, constantes em `Files.kt` |
| Novo diálogo | FXML em `resources/ui/dialog/<domínio>/`, controller em `ui/dialog/<domínio>/{create,update,view}`, abrir com `WindowUtils.openModalWindow` |
| Novo estilo | classe em `css/component|scene`, constante em `Styles.kt` / `Files.kt`, cores via tokens |
| Nova string | chave em `messages_en.properties` e `messages_pt_BR.properties` + `TranslationKeys.kt` |
| Nova tabela | migration `V0xx__*.sql` + `@Entity` + repository (Hibernate só valida) |

## Evitar
`build/`, `.gradle/`, `.aidlc/`. Constantes de caminho de CSS/FXML ficam centralizadas em `common/constant/Files.kt` (210+ linhas). [src: common/constant/Files.kt]
