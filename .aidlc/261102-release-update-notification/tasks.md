# Tasks — 261102-release-update-notification

## 2.9 Delivery plan
- **Verification command:** `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && ./gradlew ktlintCheck spotlessCheck test`  (aguardando aprovação do humano)
- Ordem segue o DAG: UNIT-1 → UNIT-2 → UNIT-3. Walking skeleton: off (3 Units pequenas).
### BOLT-1 — Units: UNIT-1, UNIT-2
Goal: base de checagem e notificação com link. DoD: comando passa; testes novos verdes; validação visual da célula/toast.
### BOLT-2 — Units: UNIT-3
Goal: fluxo completo. DoD: comando passa; checklist visual de Configurações; fluxo ponta a ponta com GitHub falso.

## Tasks
### UNIT-1 Checagem de release   Status: verified (2026-11-02, exit 0)
- [x] T-1.1 `SemanticVersion` com parse e comparação — covers FR-3 — files: model/SemanticVersion.kt — test: SemanticVersionTest
- [x] T-1.2 `GitHubReleaseClient` com HttpClient injetável, timeout 10 s, JSON → `LatestRelease` — covers FR-1, FR-8, NFR-2, NFR-5 — files: service/release/GitHubReleaseClient.kt — test: GitHubReleaseClientTest
- [x] T-1.3 Validação do host de `html_url` — covers NFR-3 — test: GitHubReleaseClientTest (URL externa rejeitada)
- [x] T-1.4 Extrair leitura da versão instalada de `MainController` para componente reutilizável — covers FR-3 — files: MainController.kt, common/AppVersion.kt — test: AppVersionTest
**Unit summary (Construction 3.5):** criados `model/SemanticVersion.kt`, `common/util/AppVersion.kt`, `service/release/GitHubReleaseClient.kt` e os testes `SemanticVersionTest` (3), `GitHubReleaseClientTest` (7), `AppVersionTest` (1); `MainController` passou a usar `AppVersion.installedText()`. Verificação: `./gradlew ktlintCheck spotlessCheck test` exit 0, 1564 testes, 0 falhas. Desvio: `GitHubReleaseClient` tem construtor secundário `@Autowired` sem argumentos para o Spring, e o primário recebe o `HttpClient` para teste.

### UNIT-2 Notificação com link   Status: verified (2026-11-02, exit 0)
- [x] T-2.1 Migration V036 e `Notification.actionUrl` — covers NFR-4 — files: db/migration/V036__add_notification_action_url.sql, Notification.kt — test: teste de persistência/migration
- [x] T-2.2 `NotificationService.send(actionUrl)` — covers FR-5 — test: NotificationServiceTest
- [x] T-2.3 Botão "Abrir página" na `NotificationListCell` e no toast, via `WindowUtils.openUrl` — covers FR-5, US-2 — files: NotificationListCell.kt, NotificationToastOverlay.kt, css, i18n — test: validação visual (M-003)
**Unit summary (Construction 3.5):** `V036__add_notification_action_url.sql` (aplicada sobre V034 em SQLite em memória: coluna `action_url TEXT` criada), `Notification.actionUrl`, `NotificationService.send(actionUrl)`, botão "Abrir página" em `NotificationListCell` e `NotificationToastOverlay` (novo parâmetro `openPageLabel`), chave `notification.action.openPage` em en e pt-BR, `NotificationServiceTest` (2). Verificação: exit 0, 1566 testes, 0 falhas. Pendente: validação visual do botão (checklist final, M-003). Os testes do Gradle desligam o Flyway; a migration foi conferida à mão no SQLite.

### UNIT-3 Serviço, agendador e opção   Status: verified (2026-11-02, exit 0)
- [x] T-3.1 Chaves em `PreferencesService` (checkEnabled, lastNotifiedVersion) — covers FR-6, FR-7 — test: PreferencesServiceTest
- [x] T-3.2 `ReleaseUpdateService.checkForUpdate()` — covers FR-3, FR-4, FR-6, FR-7, FR-8 — test: ReleaseUpdateServiceTest
- [x] T-3.3 `ReleaseUpdateScheduler` (inicialização + 24 h, IO) — covers FR-2, NFR-1 — test: teste com relógio/dispatcher injetados
- [x] T-3.4 Interruptor em Configurações (FXML, controller, i18n pt-BR/en) — covers FR-7, FR-9, US-3 — test: validação visual (M-003)

**Unit summary (Construction 3.5):** `service/release/ReleaseUpdateService.kt` e `ReleaseUpdateScheduler.kt`; `PreferencesService.updateCheckEnabled` e `lastNotifiedVersion`; `AppStartupRunner` chama `releaseUpdateScheduler.start()` (fora do modo de captura); `CheckBox` em `settings.fxml` + `SettingsController`; chaves i18n `notification.update.*` e `settings.updates.checkEnabled*` em en e pt-BR. Testes: `ReleaseUpdateServiceTest` (7) e `ReleaseUpdateSchedulerTest` (3). Verificação: exit 0, 1576 testes, 0 falhas. Smoke: `capture-screens.sh` com `CAPTURE_ONLY=settings` subiu o contexto Spring, aplicou V036 (36 migrations) e passou na validação do Hibernate; a captura de Configurações mostra a opção ligada nos temas escuro e claro. Desvios: (1) T-3.1 sem teste automatizado de `PreferencesService`, porque ele grava no nó real de `java.util.prefs` do usuário; a lógica fica coberta pelos testes do serviço com mock. (2) Os serviços usam construtor secundário `@Autowired` para o Spring e primário para teste.
