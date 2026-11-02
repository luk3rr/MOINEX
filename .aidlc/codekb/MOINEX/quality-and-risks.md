# Quality and risks — MOINEX

Status: human-reviewed (2026-11-02, HEAD 95b19ba7) · Confidence: medium-high

## Baseline de testes
Atualização pós v2.19.0 (HEAD 08bbf24f): `./gradlew clean build jacocoTestReport` com JDK 25 → BUILD SUCCESSFUL em 50s, 1374 testes, 0 falhas; cobertura de instruções 25,7%. Smoke manual feito pelo usuário; CI do GitHub passou (informado pelo usuário). [user] [src: .aidlc/261029-java-25-upgrade/construction/build-and-test.md]

Baseline original: `./gradlew test` em 2026-10-29, commit d38e3ee1, `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`: BUILD SUCCESSFUL em 1m01s. 1374 testes, 0 falhas, 0 erros, 0 ignorados. Sem falhas pré-existentes. [src: build/test-results/test/*.xml] Rodado com autorização do usuário. [user]

## Pontos quentes (12 meses)
Mais alterados: arquivos i18n (`messages_*.properties`), `TranslationKeys.kt`, `HomeController.kt`, `WalletController.kt`, `SavingsController`. [src: git log --since 12 months]

## Riscos relevantes para a troca de tema (histórico, tema já migrado em 2026-10-30; ver riscos atuais abaixo)
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

## Atualização 2026-11-01 (HEAD c72d94af, v2.20.2)
**Baseline de testes (HEAD c72d94af, 2026-11-01):** `./gradlew test` com JDK 25 → BUILD SUCCESSFUL em 42s, 1411 testes, 0 falhas, 0 erros, 0 ignorados; sem falhas pré-existentes. Rodado com OK do usuário; working tree com a correção do script Python não commitada (não afeta testes Kotlin). [user] [src: build/test-results/test/*.xml]

**Riscos atuais relevantes ao intent**
1. Dependência do formato bruto do yfinance: a 1.7.0 passou a devolver o período mais antigo como coluna só com NaN em linhas-chave, o que anulou CAGR, valor justo de Graham e derivados (corrigido em f426f361). [src: investigação 2026-11-01] [src: scripts/get_fundamental_data.py]
2. `trailingEps` do Yahoo diverge do `Diluted EPS` do balanço (PETR4: 10,49 vs 1,52) e empresas que reportam em USD (VALE3) são comparadas a preço em BRL; sem normalização de moeda. [src: investigação 2026-11-01]
3. (parcialmente resolvido em f426f361) FIIs/ETFs agora têm cartão de renda; P/VP existe para alguns FIIs, não para todos. [src: scripts/get_fundamental_data.py] [src: service/investment/FundamentalAnalysisService.kt:103]
4. (resolvido em f426f361) N/A agora mostra o motivo (`MetricReason`). [src: ui/dialog/investment/view/FundamentalAnalysisController.kt:652]
5. Nenhum teste cobre `FundamentalAnalysisController`, `Savings*Controller`, `APIUtils` ou o script Python; não há teste de contrato do JSON do script. [src: find src/test]
6. (resolvido em f426f361) série diária de 12 meses em `TickerPriceHistory`. [src: model/investment/TickerPriceHistory.kt]
7. `Ticker` não tem preço-alvo, moeda nem dividend yield. [src: model/investment/Ticker.kt]
8. Cache de `get_brazilian_market_indicators.py` usa `print(dict)` em vez de JSON. [src: scripts/get_brazilian_market_indicators.py]

## Atualização 2026-11-01 (fim do intent fundamentals-and-portfolio-views, commitado em f426f361)
**Baseline:** `./gradlew ktlintCheck spotlessCheck test` → 1553 testes Kotlin, 0 falhas; 36 testes Python, OK. Cobertura 27,3% (base 25,7%).
**Riscos novos:** (a) as faixas da nota de qualidade foram conferidas com uma cópia de um banco real: bancos não têm dívida/caixa pontuados (setor Financial Services); o veredito de Graham ainda marca "caro" para quase tudo e não foi alterado; (b) os testes Python não rodam no CI; (c) telas novas só têm captura em pt-BR e no tema escuro; (d) só o tema escuro e pt-BR foram vistos nas abas novas do detalhe do ativo. [src: .aidlc/261101-fundamentals-and-portfolio-views/state.md]
