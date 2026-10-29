# Quality and risks — MOINEX

Status: human-reviewed (2026-10-29, delta pós v2.19.0) · Confidence: medium-high

## Baseline de testes
Atualização pós v2.19.0 (HEAD 08bbf24f): `./gradlew clean build jacocoTestReport` com JDK 25 → BUILD SUCCESSFUL em 50s, 1374 testes, 0 falhas; cobertura de instruções 25,7%. Smoke manual feito pelo usuário; CI do GitHub passou (informado pelo usuário). [user] [src: .aidlc/261029-java-25-upgrade/construction/build-and-test.md]

Baseline original: `./gradlew test` em 2026-10-29, commit d38e3ee1, `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`: BUILD SUCCESSFUL em 1m01s. 1374 testes, 0 falhas, 0 erros, 0 ignorados. Sem falhas pré-existentes. [src: build/test-results/test/*.xml] Rodado com autorização do usuário. [user]

## Pontos quentes (12 meses)
Mais alterados: arquivos i18n (`messages_*.properties`), `TranslationKeys.kt`, `HomeController.kt`, `WalletController.kt`, `SavingsController`. [src: git log --since 12 months]

## Riscos relevantes para a troca de tema
1. **Sem testes de UI**: regressões visuais só aparecem rodando o app. [src: find src/test → 0 em ui/]
2. **Tokens assumem o Modena**: `.root` define `-fx-base`, `-fx-background`, `-fx-accent`, etc. [src: css/theme/tokens.dark.css:7-16] Trocar o user-agent stylesheet muda a base de cálculo dessas variáveis.
3. **`Alert` nativos sem tema**: `WindowUtils.show*Dialog` não aplica `ThemeService`. [src: common/util/WindowUtils.kt:60-130]
4. **Troca de tema não atualiza janelas abertas** (só `rootPane` registrado). [src: service/ThemeService.kt:30-40]
5. **Estilos inline em Kotlin**: 14 `setStyle` e constantes `-fx-*` em `Styles.kt`; 17 `style=` em FXML. [src: grep]
6. **Ícones PNG com `ColorAdjust`** para modo escuro: não escalam e dependem de efeito por nó. [src: common/extension/JavaFxUtils.kt:8-10]
7. **Gráficos custom** (7 classes, `charts.css` 232 linhas, `timeline-chart.css` 286) e `WebView` do chat têm CSS próprio. [src: common/chart/] [src: css/component/]
8. **JFoenix** sem manutenção ativa, usado só por `JFXButton`. [src: grep]
9. **Folhas de estilo repetidas por nó em FXML** (ex.: `combo-box.css` 51+25+22+19 vezes): mudança de caminho exige editar muitos FXML. [src: grep stylesheets=]

## Dívida e legado
Dependências sem uso (Lombok, Mockito, google-java-format, reflections, commons-beanutils) foram removidas em v2.19.0. `.gitlab-ci.yml` legado, atualizado para Temurin 25. [src: git show d50e86e6] [src: CLAUDE.md]

## Segurança
Sem superfície de rede de entrada. Credenciais de IA em store local. Scripts Python executados como subprocesso com argumentos do app. [src: common/util/APIUtils.kt]

## Perguntas em aberto
Nenhuma levantada no walkthrough; o usuário confirmou os 9 riscos sem acréscimos. [user]
