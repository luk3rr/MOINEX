# Technology stack — MOINEX

Status: human-reviewed (2026-10-29, delta pós v2.19.0) · Confidence: high

| Item | Versão | Uso |
|---|---|---|
| Kotlin / JVM | 2.4.21 / Java 25 (`release` 25) | linguagem principal [src: build.gradle.kts:7-10,89-91] |
| Gradle | 9.8.2 | build; só roda no Java 25 a partir da 9.1 [src: gradle/wrapper/gradle-wrapper.properties:3] |
| Spring Boot | 4.1.2 (Spring Framework 7.0.10, Spring Data JPA 4.1.2) | injeção, JPA, config [src: build.gradle.kts:11] [src: gradle/libs.versions.toml] |
| JavaFX | 27.0.1 (controls, fxml, web) | UI; `web` usado só no chat de IA [src: gradle/libs.versions.toml:20-22] |
| JFoenix | (ver libs.versions.toml) | apenas `JFXButton` (95 usos) [src: grep] |
| SQLite JDBC + Hibernate 7.4.13 (core e community dialects, versão gerida pelo BOM do Boot) | | banco local, `ddl-auto: validate` [src: CLAUDE.md] |
| Flyway | 11.3.4 | 34 migrations; bean manual em `FlywayConfig` [src: src/main/resources/db/migration] [src: config/FlywayConfig.kt] |
| kotlinx-coroutines (core, reactor, javafx) | 1.9.0 (javafx) | assíncrono [src: gradle/libs.versions.toml:44] |
| commonmark (+gfm tables) | | markdown do chat renderizado em WebView [src: build.gradle.kts] |
| PDFBox, OpenCSV | | importação de extratos [src: ai/ingest/] |
| exp4j | | calculadora [src: build.gradle.kts] |
| Python 3 + yfinance, requests, pandas, numpy | /usr/bin/python3 no Linux | cotações e indicadores [src: CLAUDE.md] |
| Testes | Kotest BehaviorSpec, MockK, AssertJ, H2, JUnit5 (Mockito só transitivo via spring-boot-starter-test), MockK 1.14.11 | [src: build.gradle.kts] |
| Qualidade | ktlint 14.0.1, Spotless 6.25.0, Jacoco, SonarCloud | [src: build.gradle.kts, sonar-project.properties] |
| Empacote | scripts de instalador Windows (`jpackage`) | [src: build.gradle.kts:15, .github/workflows/build-windows-installer.yml] |

Observações:
- Lombok, Mockito (declarado), google-java-format, reflections, commons-beanutils e o plugin jlink foram removidos em 2026-10-29 (v2.19.0). [src: git show d50e86e6]
- ktlint: plugin Gradle 14.2.0 com engine 1.4.1; o `kotlin-compiler-embeddable` da configuração `ktlint` é fixado em 2.0.21, senão o plugin do Kotlin força 2.4.21 e o ktlint falha. [src: build.gradle.kts] [src: gradle/libs.versions.toml]
- JaCoCo 0.8.16. [src: gradle/libs.versions.toml]
- `kotlin("plugin.allopen")` cobre `@Entity`/`@MappedSuperclass`/`@Embeddable`. [src: build.gradle.kts:22-26]
