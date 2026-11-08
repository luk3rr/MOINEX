# Quality and risks — MOINEX

Status: human-reviewed (2026-11-07) · Confidence: medium-high

Os 9 riscos da antiga troca de tema (Alert sem tema, JFoenix, ícones PNG com ColorAdjust etc.) foram retirados em 2026-11-07 a pedido do usuário: o tema já foi migrado. [user]

## Baseline de testes
Atualização pós v2.19.0 (HEAD 08bbf24f): `./gradlew clean build jacocoTestReport` com JDK 25 → BUILD SUCCESSFUL em 50s, 1374 testes, 0 falhas; cobertura de instruções 25,7%. Smoke manual feito pelo usuário; CI do GitHub passou (informado pelo usuário). [user] [src: .aidlc/261029-java-25-upgrade/construction/build-and-test.md]

Baseline original: `./gradlew test` em 2026-10-29, commit d38e3ee1, `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`: BUILD SUCCESSFUL em 1m01s. 1374 testes, 0 falhas, 0 erros, 0 ignorados. Sem falhas pré-existentes. [src: build/test-results/test/*.xml] Rodado com autorização do usuário. [user]

## Pontos quentes (12 meses)
Mais alterados: arquivos i18n (`messages_*.properties`), `TranslationKeys.kt`, `HomeController.kt`, `WalletController.kt`, `SavingsController`. [src: git log --since 12 months]

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

## Baseline 2026-11-07 (intent 261107-financial-calculations-review)
- HEAD 6c94ec03: Kotlin 2041 testes, 0 falhas, 0 erros (resultado reaproveitado pelo Gradle, tarefas `UP-TO-DATE`); ktlint e spotless sem pendências; Python 109 testes, OK. Sem falhas pré-existentes.

## Baseline 2026-11-07 (HEAD aefe5133, update do KB)
- `./gradlew test`: BUILD SUCCESSFUL, 2144 testes Kotlin, 0 falhas, 0 erros, 0 ignorados (as tarefas estavam `UP-TO-DATE`, então o resultado é o do último run do Gradle, não uma execução nova); `make test-python`: 118 testes, OK (uma mensagem `[RETRY] 503` é esperada, vem de um teste de retry). Rodado com OK do usuário. [user] [src: build/test-results/test/*.xml]
- Cobertura não remedida; último valor 27,3% (2026-11-01).

## Riscos atuais (confirmados pelo usuário em 2026-11-07)
1. **Yahoo é ponto único de falha:** todo preço, histórico, fundamentalista e logo vem do Yahoo via `YahooGateway`; um rate limit bloqueia as atualizações automáticas por 5 a 60 min e não há fonte alternativa de cotação. [src: common/util/YahooGateway.kt] [user]
2. **Pacote Linux depende do CI e de passos manuais:** `build-package.sh`/`validate-package.sh` só rodam no workflow; `test-package-containers.sh` exige docker e uso manual no artefato do CI antes de um release. [src: CLAUDE.md "Linux packages"] [user]
3. **Sem chave estrangeira imposta e sem teste de UI:** o JDBC não liga `PRAGMA foreign_keys` (cascatas feitas no código, por exemplo em `AiChatHistoryService`); continua sem teste de controllers, charts ou `ThemeService`. [src: service/ai/AiChatHistoryService.kt] [src: find src/test] [user]

## Riscos anteriores ao delta, ainda abertos ou resolvidos
Os riscos 1–8 da atualização de 2026-11-01 (formato bruto do yfinance, EPS divergente, moeda USD vs BRL etc.) não foram reconferidos neste update; a moeda e a conversão passaram a ser tratadas por `yf_resilience.py` (v2.24.0) e o cálculo de juros/rentabilidade foi revisado em `areas/financial-calculations.md`. [src: areas/market-data.md] [inferred]
