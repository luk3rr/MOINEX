# Technology stack — MOINEX

Status: human-reviewed (2026-10-29) · Confidence: high

| Item | Versão | Uso |
|---|---|---|
| Kotlin / JVM | 2.1.0 / Java 21 | linguagem principal [src: build.gradle.kts:6,70] |
| Spring Boot | 3.5.11 | injeção, JPA, config [src: build.gradle.kts:9] |
| JavaFX | 23.0.1 (controls, fxml, web) | UI; `web` usado só no chat de IA [src: gradle/libs.versions.toml:20-22] |
| JFoenix | (ver libs.versions.toml) | apenas `JFXButton` (95 usos) [src: grep] |
| SQLite JDBC + Hibernate community dialects | | banco local, `ddl-auto: validate` [src: CLAUDE.md] |
| Flyway | | 34 migrations [src: src/main/resources/db/migration] |
| kotlinx-coroutines (core, reactor, javafx) | 1.9.0 (javafx) | assíncrono [src: gradle/libs.versions.toml:44] |
| commonmark (+gfm tables) | | markdown do chat renderizado em WebView [src: build.gradle.kts] |
| PDFBox, OpenCSV | | importação de extratos [src: ai/ingest/] |
| exp4j | | calculadora [src: build.gradle.kts] |
| Python 3 + yfinance, requests, pandas, numpy | /usr/bin/python3 no Linux | cotações e indicadores [src: CLAUDE.md] |
| Testes | Kotest BehaviorSpec, MockK, AssertJ, H2, Mockito, JUnit5 | [src: build.gradle.kts] |
| Qualidade | ktlint 14.0.1, Spotless 6.25.0, Jacoco, SonarCloud | [src: build.gradle.kts, sonar-project.properties] |
| Empacote | jlink plugin, scripts de instalador Windows | [src: build.gradle.kts:15, .github/workflows/build-windows-installer.yml] |

Observações:
- Mockito e Lombok aparecem como dependência mas o código é Kotlin. [inferred: restos da migração Java→Kotlin]
- `kotlin("plugin.allopen")` cobre `@Entity`/`@MappedSuperclass`/`@Embeddable`. [src: build.gradle.kts:22-26]
