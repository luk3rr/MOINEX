# Requirements — java-25-upgrade

## Original request
Acho que o ideal seria então bumpar para java 25 e usar a versão mais recente do atlantafx.
(Contexto: pedido feito durante o estágio 2.3 do intent 261029-adopt-atlantafx. O usuário decidiu: "Intent separado, antes deste". Este intent entrega a atualização de plataforma; o AtlantaFX em si é do outro intent.)

## 2.3 Requirements

Origem: pedido do usuário e respostas de 2026-10-29. Comportamento do app **não muda**; é atualização de plataforma.

### Functional
| ID | Requirement | Rationale | Acceptance criteria | Priority | Source |
|---|---|---|---|---|---|
| FR-1 | O projeto compila e executa com **JDK 25** (`sourceCompatibility`, `options.release` e Kotlin `jvmTarget` = 25). | Pré-requisito do AtlantaFX 3.0.0; Java 25 é LTS. | Given o JDK 25 em `JAVA_HOME`, When rodo `./gradlew build`, Then compila sem erro e o jar roda em Java 25. | Must | [user] |
| FR-2 | O Gradle wrapper usa a versão **9.x** mínima exigida pelo Java 25 (≥ 9.1.0), a mais recente compatível com todos os plugins. | Gradle 8.14 não roda no Java 25. | Given o wrapper atualizado, When rodo `./gradlew --version`, Then mostra 9.x em JVM 25. | Must | [src: docs.gradle.org] |
| FR-3 | **Kotlin 2.4.x** (mais recente) em todos os plugins Kotlin (`jvm`, `spring`, `jpa`, `allopen`). | Escolha do usuário; Kotlin 2.1.0 não gera bytecode 25. | Given Kotlin 2.4.x, When compilo, Then sem erro e sem warnings novos que quebrem o CI. | Must | [user] |
| FR-4 | **JavaFX 27.0.1** (`javafx.controls/fxml/web`) com plugin `org.openjfx.javafxplugin` compatível. | Pré-requisito do AtlantaFX 3.0.0. | Given JavaFX 27, When rodo `./gradlew bootRun`, Then a splash e a janela principal abrem. | Must | [src: repo1.maven.org] |
| FR-5 | **Spring Boot 4.x** (mais recente estável), com Spring Framework 7 e Hibernate 7; o código de configuração e as dependências são ajustados para a modularização do Boot 4. | Escolha do usuário. | Given Boot 4.x, When o app sobe, Then o contexto Spring, o Flyway e o JPA inicializam e as telas carregam dados. | Must | [user] |
| FR-6 | JPA continua usando SQLite com `ddl-auto: validate`, o dialeto SQLite do `hibernate-community-dialects` na versão do Hibernate 7 e o pool de 1 conexão. | Restrição do projeto. | Given um `moinex.db` existente, When o app abre, Then a validação do schema passa sem alterar tabelas. | Must | [src: CLAUDE.md] |
| FR-7 | Ferramentas de qualidade compatíveis com JDK 25: JaCoCo ≥ 0.8.14, MockK atual, Kotest atual, ktlint e Spotless funcionando. | Sem isso o CI quebra. | Given as versões novas, When rodo `./gradlew ktlintCheck spotlessCheck test jacocoTestReport`, Then terminam com sucesso. | Must | [src: jacoco.org] |
| FR-8 | Remover do build as dependências sem uso: Lombok (e `annotationProcessor`), Mockito (core e junit-jupiter), google-java-format, reflections, commons-beanutils; e o plugin `org.beryx.jlink` (sem bloco `jlink {}`). | Menos superfície de atualização; Lombok pode quebrar no JDK 25. | Given as remoções, When compilo e testo, Then nenhuma classe falha por import ausente. | Must | [user] [src: grep] |
| FR-9 | Os dois workflows do GitHub Actions (`pipeline.yml`, `build-windows-installer.yml`) usam Temurin **25**; o instalador Windows (`jpackage` + Python embutido) é gerado com o JDK 25. | O CI precisa refletir a plataforma. | Given os workflows, When rodam, Then build, lint, test e geração do instalador terminam com sucesso. | Must | [src: .github/workflows/] |
| FR-10 | Docs e scripts que fixam Java 21 passam a Java 25: `Makefile:4`, `CLAUDE.md:32`, `scripts/install.sh:171`, `scripts/build-windows-installer.bat` (mensagem "Java 21+"). | Consistência. | Given a busca por "21"/"java-21", When procuro, Then não restam referências à plataforma antiga. | Should | [src: grep] |

### Non-functional
| ID | Category | Measurable target | Verification | Source |
|---|---|---|---|---|
| NFR-1 | Regressão | Os 1374 testes atuais (ou mais) passam, 0 falhas, no JDK 25. | `./gradlew ktlintCheck test` | [src: quality-and-risks.md] |
| NFR-2 | Compatibilidade de dados | Um `moinex.db` existente (cópia) abre sem erro de validação nem perda de dados. | Abrir o app com uma cópia do banco e conferir saldos nas telas Home e Carteiras. | [inferred] |
| NFR-3 | Smoke manual | Startup, navegação pelas telas principais, um diálogo, troca de tema, atualização de cotação via Python e chat de IA (WebView) funcionam. | Checklist rodando o app (não há testes de UI). | [user] [mem: M-003] |
| NFR-4 | Reversibilidade | Trabalho em branch tipada (`chore/java-25`); sem commit antes de o usuário ver o diff. | Verificação do histórico. | [mem: M-002] [mem: M-004] |

### Constraints · Out of scope · Glossary
- **Constraints:** Python continua em `/usr/bin/python3` no Linux; banco SQLite com Flyway (34 migrations, sem novas); Hibernate só valida o schema. [src: CLAUDE.md]
- **Out of scope:** adoção do AtlantaFX, remoção do JFoenix, ícones, redesenho (todos no intent 261029-adopt-atlantafx); novas funcionalidades; mudança de schema.
- **Glossary:** LTS = versão de suporte longo do Java; `release` = flag do compilador que fixa a versão da API/bytecode.

### Assumptions & open questions
- A1: Spring Boot 4.x aceita as demais bibliotecas atuais (Flyway 11, `hibernate-community-dialects` com SQLiteDialect no Hibernate 7, Kotest spring extension). — confirmed: no (verificar na Construction; se for bloqueio, fallback ADR: Boot 3.5.x + Java 25).
- A2: O JDK 25 instalado no Linux é suficiente para a verificação; o instalador Windows só pode ser verificado no CI. — confirmed: no
- A3: Gradle 9.x exige ajustes nos plugins `org.openjfx.javafxplugin` 0.1.0, Spotless 6.25.0 e ktlint plugin. — confirmed: no (verificar).
