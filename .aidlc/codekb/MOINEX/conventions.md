# Conventions — MOINEX

Status: human-reviewed (2026-10-29) · Confidence: medium-high

## Estilo e lint
- ktlint 14.0.1 + Spotless; 4 espaços, linha ≤120, final de arquivo com newline. [src: .editorconfig:1-20] [src: build.gradle.kts:16-17]
- `./gradlew ktlintFormat` / `spotlessApply` corrigem. [src: CLAUDE.md]
- Sem comentários `//` explicativos no código Kotlin; KDoc em classes/funções é aceito. [user] (regra do dono do projeto, registrada em memória do Claude)

## Padrões de código
- Controllers: `@Controller`, `@FXML lateinit var`, dependências por construtor. [src: ui/main/MainController.kt:41-56]
- Constantes centralizadas: `Files.kt` (caminhos), `Styles.kt` (classes CSS e alguns estilos inline), `TranslationKeys.kt` (i18n). [src: common/constant/]
- Cabeçalho de arquivo com `Filename/Created on/Author`. [src: app/App.kt:1-8]
- Cores sempre via tokens `theme-*` no CSS; `Styles.kt` tem alguns `-fx-*` inline (notificação, detalhe). [src: css/theme/tokens.*.css] [src: common/constant/Styles.kt]

## Testes
- Kotest BehaviorSpec (Given/When/Then), MockK, fábricas em `src/test/kotlin/org/moinex/factory/`, H2 em memória. [src: CLAUDE.md]
- 117 arquivos de teste em `service/`, `model/`, `ai/`; **nenhum** em `ui/` e Sonar exclui `ui/`, `common/`, `config/`, `model/`, `repository/` da cobertura. [src: find src/test] [src: sonar-project.properties]

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
