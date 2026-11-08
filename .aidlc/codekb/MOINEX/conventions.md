# Conventions — MOINEX

Status: human-reviewed (2026-11-07) · Confidence: medium-high

## Estilo e lint
- ktlint (plugin 14.2.0, engine 1.4.1, compilador embutido fixado em 2.0.21) + Spotless (plugin aplicado sem bloco de configuração); 4 espaços, linha ≤120, final de arquivo com newline. [src: .editorconfig:1-20] [src: build.gradle.kts:16-17]
- `./gradlew ktlintFormat` / `spotlessApply` corrigem. [src: CLAUDE.md]
- Sem comentários `//` explicativos no código Kotlin; KDoc em classes/funções é aceito. [user] (regra do dono do projeto, registrada em memória do Claude)

## Padrões de código
- Controllers: `@Controller`, `@FXML lateinit var`, dependências por construtor. [src: ui/main/MainController.kt:41-56]
- Constantes centralizadas: `Files.kt` (caminhos), `Styles.kt` (classes CSS e alguns estilos inline), `TranslationKeys.kt` (i18n). [src: common/constant/]
- Cabeçalho de arquivo com `Filename/Created on/Author`. [src: app/App.kt:1-8]
- Cores sempre via tokens `theme-*` no CSS; `Styles.kt` tem alguns `-fx-*` inline (notificação, detalhe). [src: css/theme/tokens.*.css] [src: common/constant/Styles.kt]

## Testes
- Kotest BehaviorSpec (Given/When/Then), MockK, fábricas em `src/test/kotlin/org/moinex/factory/`, H2 em memória. [src: CLAUDE.md]
- 204 arquivos de teste Kotlin (2144 testes em 2026-11-07) e 6 de Python (118 testes); **nenhum** em `ui/` e Sonar exclui `ui/`, `common/`, `config/`, `model/`, `repository/` da cobertura. [src: find src/test] [src: sonar-project.properties]

## i18n
Traduções em `messages_pt_BR.properties` e `messages_en.properties`; nunca reescrever o arquivo todo, só acrescentar/editar chaves. [src: CLAUDE.md] [user]

## Git
- Commits em Conventional Commits (`feat:`, `fix:`, `chore:`), tags anotadas `vX.Y.Z` no `main`, `version` do `build.gradle.kts` sincronizada em commit próprio. [src: CLAUDE.md] [src: git log]
- `Co-Authored-By` permitido; nunca link de sessão. Commit só quando pedido. [user]
- Hook pré-commit: `ktlintCheck` + `test` (`make install-hooks`). [src: .githooks/pre-commit]
- Branches por tipo, no padrão `feat/*`, `refactor/*`, `chore/*`, mergeadas no `main` (ex.: `feat/fundamental-analysis`, `feat/savings-screen`, `refactor/migrate-crc-kotlin`, `chore/style-and-usability-enhancement`); mudanças pequenas vão direto no `main`. Padrão confirmado pelo usuário em 2026-10-29. [user] [src: git branch -a] [src: git log --merges]
- Revisão: o próprio autor revisa o diff antes de commitar; não há template de PR nem CODEOWNERS. [user] [src: .github/]
- Validação de UI: manual, rodando o app e navegando nas telas; não há testes de UI. [user]

## Definition of done
Lint e testes passando no pipeline (build, spotlessCheck/ktlintCheck, test+Jacoco, Sonar). [src: .github/workflows/pipeline.yml:33-39]

## Atualização 2026-11-01: convenções de UI
- Cores em CSS/FXML só de tokens `theme-*`; cores desenhadas em código só de `ThemeColors`; só sombras e scrims podem usar `rgba(0,0,0,α)`. [src: CLAUDE.md] [src: common/ui/ThemeColors.kt]
- Ícones: `FontIcon` com literais de `Icons.kt` e classes `app-icon` + `icon-size-N`. [src: common/ui/Icons.kt]
- Filtros com poucas opções fixas usam `SegmentedFilter`; ações de linha vão no botão direito (`RowContextMenu`); ações de tela vão no rodapé "+" e na busca global via `ScreenActionProvider`. [src: git log bf3b6d11, 135821d3, 1718224c]
- Diálogos: `DialogForm.install` (Enter salva) e `FieldValidation` (validação inline). [src: common/ui/]
- Testes: 128 arquivos; há testes de tokens/contraste de tema, `common/ui`, busca e `CapturePlan`, mas nenhum de controllers, charts ou `ThemeService`. [src: find src/test]
- Antes de comitar, rodar `ktlintCheck` + `test` (hook); i18n em UTF-8 sem `\uXXXX`. [src: CLAUDE.md]

## Atualização 2026-11-02: header de arquivo e TitledValue
- Todo arquivo de código da app (`.kt` em `src/main`, `.py` de scripts) começa com o header `Filename` / `Created on: <Month D, YYYY>` / `Author`, criado junto com o arquivo (`/* */` em Kotlin, `#` em Python); arquivos migrados de Java usam a data do `.java` original. Os 98 arquivos que não tinham foram corrigidos em f426f361; testes não seguem o padrão. [user] [mem: M-012] [src: CLAUDE.md "File header"]
- "<Título>: <Valor>" usa `common/ui/TitledValue`; telas antigas migram quando tocadas. [user] [mem: M-010]

## Atualização 2026-11-07
- Testes Python (`src/test/python/`, unittest) fazem parte do `make test`, do hook de pré-commit e do CI (job `python-tests`). [src: Makefile:47] [src: .github/workflows/pipeline.yml:46]
- Todo arquivo `.sh` novo precisa de `git update-index --chmod=+x` (`core.fileMode` é false), senão o CI falha com "Permission denied". [src: CLAUDE.md]
- Escritor único: `TickerMarketDataService` (preço, momento, site e metadata do ticker), `AiChatHistoryService` (tabelas `ai_chat*`); nenhum outro código grava nessas colunas/tabelas. [src: CLAUDE.md "Database"]
- Atualização manual de um item no Yahoo usa `APIUtils.withoutYahooCooldown { }`; automática e em lote respeita o cooldown. [src: CLAUDE.md]
- O `ER` em `docs/database-erd.md` nunca é editado à mão: `make erd`. Capturas de validação ficam em `.aidlc/<intent>/construction/screenshots/` (ignoradas pelo git). [src: CLAUDE.md]
- Antes de iniciar cada intent AI-DLC, perguntar se usa branch ou `main` (M-014). [mem: M-014]
- Testes que passam por `FxUtils.launchOnFxThread` devem stubar o `FxUtils` (`mockkObject`): sem display o toolkit não inicia e `Platform.runLater` trava para sempre (travou o CI na v2.22.0). [mem: M-013]
