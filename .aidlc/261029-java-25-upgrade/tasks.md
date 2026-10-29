# Tasks — java-25-upgrade

## 2.9 Delivery plan

- **Verification command:** `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew ktlintCheck spotlessCheck test` (aprovado em 2026-10-29 pelo usuário). UNIT-1 roda a mesma linha com `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`, porque ainda não migrou de JDK. Complemento manual por Unit: rodar o app (`bootRun`) e conferir o smoke de NFR-3.
- Ordem segue o DAG: UNIT-1 → UNIT-2 → UNIT-3 → UNIT-4 → UNIT-5.
- Branch de trabalho: **`chore/java-25`** (padrão de branch tipada [mem: M-002]). Nenhum commit sem o usuário ver o diff [mem: M-004].
- Walking skeleton: off (não há código novo de produto).

```mermaid
flowchart LR
  U1["UNIT-1 Limpar dependências"] --> U2["UNIT-2 Toolchain JDK 25 (Boot 3.5)"]
  U2 --> U3["UNIT-3 Spring Boot 4"]
  U3 --> U4["UNIT-4 CI, docs e scripts"]
  U4 --> U5["UNIT-5 Verificação final e smoke"]
  U3 -. "bloqueio: ADR e fallback" .-> U2
```
Caption: cada Unit termina verde antes da próxima; o checkpoint do UNIT-2 dá um ponto estável (Java 25 com Boot 3.5.x) caso o Boot 4 trave.

### BOLT-1 — Units: UNIT-1, UNIT-2 — Owner: usuário (aprova diffs), Claude (executa)
- Goal: build rodando em JDK 25 com Gradle 9, Kotlin 2.4, JavaFX 27, ainda em Spring Boot 3.5.x.
- Definition of Done: comando de verificação passa em JDK 25 com 1374+ testes; `bootJar` gera `moinex.jar`; `bootRun` abre a janela principal.
- Confidence hypothesis: alta para JDK/Gradle/JavaFX; média para plugins Gradle e MockK.
- Risks/sequencing: o `org.openjfx.javafxplugin` 0.1.0, Spotless 6.25.0 e ktlint podem exigir versões novas com Gradle 9 (A3).

### BOLT-2 — Units: UNIT-3 — Owner: usuário, Claude
- Goal: Spring Boot 4.x com Hibernate 7, SQLite, Flyway e Kotest funcionando.
- Definition of Done: verificação passa; o app abre uma **cópia** de `~/.moinex/data/moinex.db` sem erro de validação (Flyway e Hibernate) e as telas Home e Carteiras mostram os mesmos saldos.
- Confidence hypothesis: média-baixa (A1): migração grande, dialeto SQLite do Hibernate 7 e extensão Spring do Kotest são os pontos incertos.
- Risks/sequencing: se travar após esforço razoável, registrar ADR e voltar ao checkpoint do UNIT-2 (Boot 3.5.x + Java 25); a decisão é do usuário.

### BOLT-3 — Units: UNIT-4, UNIT-5 — Owner: usuário, Claude
- Goal: CI, docs e instalador alinhados ao Java 25; verificação final.
- Definition of Done: workflows com Temurin 25; nenhuma referência a Java 21; build completo com JaCoCo; checklist de smoke executada pelo usuário.
- Confidence hypothesis: alta, exceto o instalador Windows, só verificável no CI (A2).
- Risks/sequencing: `push` e disparo de workflow são ações externas e pedem o seu "sim" na hora.

## Tasks

### UNIT-1 Limpar dependências sem uso   Status: verified (2026-10-29, exit 0)
Covers: FR-8, NFR-1. Verificação: comando acima com JDK 21.
- [x] T-1.1 Criar a branch `chore/java-25` a partir de `main` — covers NFR-4 — files: (git) — test: `git branch --show-current`
- [x] T-1.2 Remover Lombok (`compileOnly` e `annotationProcessor`) e a entrada em `libs.versions.toml` — covers FR-8 — files: build.gradle.kts, gradle/libs.versions.toml — test: `./gradlew compileKotlin`
- [x] T-1.3 Remover Mockito (`mockito-core`, `mockito-junit-jupiter`) — covers FR-8 — files: build.gradle.kts, gradle/libs.versions.toml — test: `./gradlew compileTestKotlin`
- [x] T-1.4 Remover google-java-format, reflections e commons-beanutils — covers FR-8 — files: build.gradle.kts, gradle/libs.versions.toml — test: `./gradlew compileKotlin compileTestKotlin`
- [x] T-1.5 Remover o plugin `org.beryx.jlink` — covers FR-8 — files: build.gradle.kts — test: `./gradlew bootJar`
- [x] T-1.6 Rodar a verificação com JDK 21 e registrar 1374+ testes sem falhas — covers NFR-1 — test: `./gradlew ktlintCheck spotlessCheck test`

**Unit summary (Construction 3.5):** arquivos alterados: `build.gradle.kts` e `gradle/libs.versions.toml` (24 linhas removidas, nenhuma adicionada). Removidos: plugin `org.beryx.jlink`, Lombok (`compileOnly` + `annotationProcessor`), Mockito (core e junit-jupiter) declarados, google-java-format, reflections e commons-beanutils. Como rodar: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew ktlintCheck spotlessCheck test`. Resultado em 2026-10-29: exit 0, BUILD SUCCESSFUL em 1m17s, 1374 testes, 0 falhas, 0 erros, 0 ignorados; `./gradlew bootJar` OK. Desvio: o Mockito (5.17.0) continua no classpath de teste **de forma transitiva** pelo `spring-boot-starter-test`; só a declaração explícita foi removida. Branch: `chore/java-25`, sem commit (aguarda você ver o diff).

### UNIT-2 Toolchain em JDK 25 (Spring Boot 3.5.x)   Status: verified (2026-10-29, exit 0); T-2.8 aguarda o smoke manual do usuário
Covers: FR-1, FR-2, FR-3, FR-4, FR-7, NFR-1, NFR-3. Depende de: UNIT-1.
- [x] T-2.1 Atualizar o Gradle wrapper para a 9.x estável mais recente e corrigir o que o Gradle 9 quebrar nos scripts — covers FR-2 — files: gradle/wrapper/gradle-wrapper.properties, build.gradle.kts — test: `./gradlew --version`
- [x] T-2.2 Atualizar os plugins Kotlin (`jvm`, `spring`, `jpa`, `allopen`) para 2.4.x e ajustar `jvmTarget`, `release` e `sourceCompatibility` para 25 — covers FR-1, FR-3 — files: build.gradle.kts — test: `./gradlew compileKotlin`
- [x] T-2.3 Subir JavaFX para 27.0.1 e o plugin `org.openjfx.javafxplugin` para a versão compatível — covers FR-4 — files: gradle/libs.versions.toml, build.gradle.kts — test: `./gradlew compileKotlin`
- [x] T-2.4 Subir JaCoCo (≥ 0.8.14), ktlint (plugin e engine) e Spotless para versões compatíveis com Gradle 9 — covers FR-7 — files: gradle/libs.versions.toml, build.gradle.kts — test: `./gradlew ktlintCheck spotlessCheck jacocoTestReport`
- [x] T-2.5 Subir MockK e Kotest para versões que suportem Kotlin 2.4 e JDK 25; corrigir testes que quebrarem — covers FR-7, NFR-1 — files: gradle/libs.versions.toml, src/test — test: `./gradlew test`
- [x] T-2.6 Corrigir erros e deprecações novas do compilador Kotlin 2.4 — covers FR-3 — files: src/main/kotlin — test: `./gradlew build -x test`
- [x] T-2.7 Rodar a verificação com JDK 25 (1374+ testes) e `./gradlew bootJar` — covers NFR-1 — test: comando de verificação
- [ ] T-2.8 Entregar ao usuário a checklist de smoke de NFR-3 e rodar `bootRun` em JDK 25 — covers NFR-3 — test: janela principal abre; navegação, diálogo, troca de tema, cotação e chat de IA funcionam

**Unit summary (Construction 3.5):** arquivos alterados: `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`. Gradle 8.14 → 9.8.2; Kotlin (4 plugins) 2.1.0 → 2.4.21; alvo Java 21 → 25 (`sourceCompatibility`, `targetCompatibility`, `options.release`, Kotlin `jvmTarget`); JavaFX 23.0.1 → 27.0.1 (controls, fxml, web); JaCoCo 0.8.13 → 0.8.16; MockK 1.13.8 → 1.14.11; plugin ktlint Gradle 14.0.1 → 14.2.0 (engine ktlint continua 1.4.1). Como rodar: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew ktlintCheck spotlessCheck test`. Resultado em 2026-10-29: exit 0, BUILD SUCCESSFUL em 45s, 1374 testes, 0 falhas, 0 erros, 0 ignorados; `bootJar` OK com bytecode major 69 (Java 25); `jacocoTestReport` OK; o app abriu a janela "Moinex" e aplicou as 34 migrations num `user.home` descartável (banco real intocado).
Desvios: (1) o ktlint falhava no JDK 25 porque o plugin do Kotlin forçava o `kotlin-compiler-embeddable` 2.4.21 na configuração `ktlint`; corrigido fixando 2.0.21 (o que o ktlint 1.4.1 espera) via nova entrada `org-jetbrains-kotlin-compiler-ktlint` no catálogo e um bloco `resolutionStrategy` em `build.gradle.kts`. O ktlint 1.8.0 foi descartado por acusar dezenas de violações novas no código existente. (2) Spotless 6.25.0 não foi alterado: não há bloco `spotless {}` e o `spotlessCheck` passa. (3) T-2.6: o Kotlin 2.4 emite avisos novos (conversões redundantes, `!!` desnecessário, `CONSTRAINED_RESIZE_POLICY` deprecado no JavaFX), nenhum erro; não alterei código de produção. (4) Premissa corrigida: a máquina só tinha o JRE 25; o usuário instalou `openjdk-25-jdk`. (5) No JDK 25 o JavaFX imprime avisos de "restricted method"; sugestão futura: `--enable-native-access=ALL-UNNAMED` nos lançadores. (6) A primeira execução do smoke encerrou sozinha após ~16s sem erro no log e não se reproduziu na segunda.

### UNIT-3 Spring Boot 4.x   Status: verified (2026-10-29, exit 0)
Covers: FR-5, FR-6, NFR-1, NFR-2. Depende de: UNIT-2.
- [x] T-3.1 Atualizar o plugin e os starters do Spring Boot para 4.x e o dependency-management; adaptar para os starters modulares (JPA, Flyway, teste) — covers FR-5 — files: build.gradle.kts, gradle/libs.versions.toml — test: `./gradlew compileKotlin`
- [x] T-3.2 Alinhar `hibernate-community-dialects` ao Hibernate 7 e conferir o nome do `SQLiteDialect` em `application.yml` — covers FR-6 — files: gradle/libs.versions.toml, src/main/resources/application.yml — test: o contexto sobe
- [x] T-3.3 Ajustar `FlywayConfig` e a dependência do Flyway para o Boot 4 — covers FR-5, FR-6 — files: src/main/kotlin/org/moinex/config/FlywayConfig.kt — test: migrations validam num banco novo
- [x] T-3.4 Corrigir mudanças de API do Spring 7 e do Spring Data 4 no código (`config/`, `app/`, `repository/`, `service/`) — covers FR-5 — files: src/main/kotlin — test: `./gradlew compileKotlin`
- [x] T-3.5 Corrigir os testes que dependem de Spring (Kotest spring extension) — covers FR-5, NFR-1 — files: src/test — test: `./gradlew test`
- [x] T-3.6 Abrir o app com uma **cópia** de `~/.moinex/data/moinex.db` e comparar saldos de Home e Carteiras — covers FR-6, NFR-2 — test: sem erro de validação do Flyway/Hibernate; saldos iguais
- [x] T-3.7 Rodar a verificação completa e registrar o resultado; se bloqueado, escrever ADR com o motivo e perguntar ao usuário sobre o fallback — covers NFR-1 — test: comando de verificação

**Unit summary (Construction 3.5):** arquivos alterados: `build.gradle.kts` (plugin `org.springframework.boot` 3.5.11 → 4.1.2), `gradle/libs.versions.toml` (starters 4.1.2; `hibernate-community-dialects` sem versão fixa, gerida pelo BOM), `src/main/kotlin/org/moinex/config/AppConfig.kt` (`EntityScan` agora em `org.springframework.boot.persistence.autoconfigure`), `src/main/kotlin/org/moinex/common/extension/JpaExtensions.kt` (`T : Any` exigido pelo Spring Data 4), `src/main/kotlin/org/moinex/config/FlywayConfig.kt` (o bean `flyway` executa o `data.sql` após `migrate()`; novo `BeanFactoryPostProcessor` faz o `entityManagerFactory` depender do `flyway`), `src/main/resources/application.yml` (`spring.sql.init.mode: always` → `never`). Versões resolvidas: Spring Boot 4.1.2, Spring Framework 7.0.10, Spring Data JPA 4.1.2, Hibernate 7.4.13.Final (core e community dialects), HikariCP 7.0.2, Flyway 11.3.4. Como rodar: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew ktlintCheck spotlessCheck test`. Resultado em 2026-10-29: exit 0, BUILD SUCCESSFUL, 1374 testes, 0 falhas, 0 erros, 0 ignorados.
Runtime (A1 resolvida): (1) banco novo: 34 migrations aplicadas antes do Hibernate validar o schema, 15 categorias semeadas pelo `data.sql`; (2) **cópia** do banco real: Flyway validou 34 migrations ("up to date"), Hibernate 7 validou o schema, pipeline de inicialização completo, janela "Moinex" aberta, contagem de linhas igual em 43 tabelas antes e depois, banco real intocado (modificado pela última vez em 20/09); a cópia foi apagada ao final.
Desvios: (1) O Boot 4 expôs duas ordenações que só funcionavam por acaso no 3.5: `data.sql` antes do Flyway (`no such table: category`) e validação do Hibernate antes do Flyway (`missing table [bond]`); ambas corrigidas no `FlywayConfig`. (2) Mantive o bean `flyway` manual em vez de migrar para o autoconfig `spring-boot-starter-flyway`; é uma alternativa possível, fora do escopo. (3) `spring.flyway.*` no `application.yml` segue sem efeito (já era assim). (4) Comparei as telas só por contagem de linhas; saldos nas telas Home e Carteiras dependem do smoke manual da UNIT-5. (5) Observação: em 2 de ~9 execuções do jar iniciadas pelo shell da ferramenta o app encerrou sozinho ~5-16s depois, sem erro no log (uma delas ainda com Boot 3.5); em 5 execuções seguidas o app ficou aberto. Não reproduzi a causa.

### UNIT-4 CI, docs e scripts   Status: verified (2026-10-29, exit 0); T-4.5 adiada [S]
Covers: FR-9, FR-10. Depende de: UNIT-3 (ou UNIT-2, se houver fallback).
- [x] T-4.1 `pipeline.yml`: `java-version: '25'` (Temurin) — covers FR-9 — files: .github/workflows/pipeline.yml — test: leitura do YAML
- [x] T-4.2 `build-windows-installer.yml`: Temurin 25 e `jpackage` do JDK 25; ajustar a mensagem "Java 21+" do `.bat` — covers FR-9 — files: .github/workflows/build-windows-installer.yml, scripts/build-windows-installer.bat — test: leitura; execução só no CI
- [x] T-4.3 `Makefile:4`, `CLAUDE.md:32` e `scripts/install.sh:171` para Java 25 — covers FR-10 — files: Makefile, CLAUDE.md, scripts/install.sh — test: `grep -rn "java-21\|java 21\|Java 21"` sem resultados
- [x] T-4.4 Buscar outras referências à plataforma antiga (README, docs, scripts) e corrigir — covers FR-10 — test: grep amplo
- [S] T-4.5 (adiada: o usuário respondeu "Nada agora" em 2026-10-29) Com o seu "sim" na hora: fazer push da branch para o CI rodar `pipeline.yml`; instalador via `workflow_dispatch` só com novo "sim" — covers FR-9 — test: pipeline verde (ação externa)

**Unit summary (Construction 3.5):** arquivos alterados: `.github/workflows/pipeline.yml` e `.github/workflows/build-windows-installer.yml` (Temurin 25), `.gitlab-ci.yml` (imagem `eclipse-temurin:25-jdk`, pipeline legado mantido consistente), `scripts/build-windows-installer.bat` (exige Java 25+ e mensagens), `scripts/install.sh` (mensagem), `scripts/run.sh` (lançador do app instalado: exige Java 25 e procura `/usr/lib/jvm/java-25-openjdk`), `CLAUDE.md` (`JAVA_HOME` do Java 25). O `Makefile` já foi atualizado no commit da UNIT-3. A busca por Java 21 não encontra mais nada fora de `gradlew`. `bash -n` OK em `run.sh` e `install.sh`. Desvios: (1) o plano listava `Makefile`, `CLAUDE.md` e `install.sh`; a busca revelou também `scripts/run.sh` (crítico: com o jar compilado para 25 o lançador antigo recusaria o Java 25 ou tentaria o 21), o `.gitlab-ci.yml` e as checagens do `.bat`. (2) YAML dos workflows só revisado visualmente (PyYAML ausente). (3) O pipeline do GitHub só dispara em push/PR para `main`; verificar o CI exige abrir um PR. O instalador Windows só roda por release ou `workflow_dispatch`.

### UNIT-5 Verificação final e smoke   Status: verified (2026-10-29): build limpo OK e smoke manual feito pelo usuário
Covers: NFR-1, NFR-3, NFR-4, FR-10. Depende de: UNIT-4.
- [x] T-5.1 Rodar `./gradlew clean build jacocoTestReport` em JDK 25 — covers NFR-1 — test: BUILD SUCCESSFUL, relatório gerado
- [x] T-5.2 Entregar a checklist de smoke completa e registrar o resultado informado pelo usuário — covers NFR-3 — test: checklist marcada pelo usuário
- [x] T-5.3 Atualizar `CLAUDE.md` (versões e comandos) para refletir JDK 25, Gradle 9, Kotlin 2.4, Spring Boot 4 — covers FR-10 — files: CLAUDE.md — test: leitura
- [x] T-5.4 Registrar no `state.md` do intent 261029-adopt-atlantafx que o pré-requisito foi atendido (sugestão: `/ai-dlc resume` naquele intent) — covers NFR-4 — test: leitura

### Dependências externas e riscos
- JDK 25 já instalado em `/usr/lib/jvm/java-25-openjdk-amd64`. [src: ls /usr/lib/jvm]
- Rede para baixar Gradle 9, Kotlin 2.4, JavaFX 27, Spring Boot 4 (Maven Central e plugins.gradle.org).
- Python 3 e yfinance continuam como estão (fora do escopo).

### Assumptions & open questions
- A1–A3 do `requirements.md § 2.3` continuam abertas até a Construction.
- Qual Spring Boot 4.x: a mais recente estável no momento da execução (hoje 4.1.2 no Maven Central). [src: repo1.maven.org]
