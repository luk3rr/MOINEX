# Build and Test — 261102-release-update-notification

## Comando aprovado

`export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && ./gradlew ktlintCheck spotlessCheck test` → exit 0, 1576 testes, 0 falhas (2026-11-02).

## Testes novos (23)

| Teste | Casos | Cobre |
|---|---|---|
| SemanticVersionTest | 3 | FR-3 |
| AppVersionTest | 1 | FR-3 |
| GitHubReleaseClientTest | 7 | FR-1, FR-8, NFR-2, NFR-3, NFR-5 |
| NotificationServiceTest | 2 | FR-5 |
| ReleaseUpdateServiceTest | 7 | FR-3, FR-4, FR-6, FR-7, FR-8 |
| ReleaseUpdateSchedulerTest | 3 | FR-2, NFR-1 (laço resiliente) |

Contagem por casos folha do Kotest, conferida nos XMLs do Gradle.

## Matriz requisito → verificação

| Requisito | Verificação |
|---|---|
| FR-1 | GitHubReleaseClientTest |
| FR-2 | Scheduler: intervalo de 24 h e laço que sobrevive a erro; início no `AppStartupRunner` (revisão de código) |
| FR-3 | SemanticVersionTest, ReleaseUpdateServiceTest |
| FR-4 | ReleaseUpdateServiceTest |
| FR-5 | NotificationServiceTest + botão (validação visual pendente) |
| FR-6 | ReleaseUpdateServiceTest |
| FR-7 | ReleaseUpdateServiceTest (sem chamada ao GitHub) + captura de Configurações |
| FR-8 | GitHubReleaseClientTest, ReleaseUpdateServiceTest, ReleaseUpdateSchedulerTest |
| FR-9 | chaves nos dois bundles; captura em en |
| NFR-1 | chamada em `launchOnBackground` (IO) |
| NFR-2 | GitHubReleaseClientTest (timeout 10 s) |
| NFR-3 | GitHubReleaseClientTest (host externo rejeitado) |
| NFR-4 | Flyway aplicou V036 (36 migrations) e Hibernate `validate` passou ao subir o contexto |
| NFR-5 | HttpClient e versão instalada injetáveis |

## Não verificado automaticamente (riscos)

- Chamada real à API do GitHub pela app (a API respondeu 200 por `curl`, mas o `HttpClient` da app não foi exercitado contra a rede).
- Aparência do botão "Abrir página" na central e no toast, nos dois temas, e o clique abrindo o navegador.
- Fluxo ponta a ponta com uma release maior que a instalada.
