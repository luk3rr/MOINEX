# AI-DLC State — linux-package

- Intent: 261102-linux-package
- Status: COMPLETE (released as v2.22.1)
- Profile: custom (enxuta + Infra/CI)   Depth: Standard   Tests: Standard
- Project type: brownfield   Repos: MOINEX (HEAD efc08298; codekb em 95b19ba7)
- Switches: collaborators=off reviewers=off plan_approval=off walking_skeleton=off autonomy=unset iteration=unit-major
- Verification command: export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew ktlintCheck spotlessCheck test (approved 2026-11-02)
- Language: chat=pt-BR files=pt-BR
- Current: phase=construction stage=3.1 Functional Design   Updated: 2026-11-02T14:49:24-03:00

## Route
Legend: [x] done · [>] current · [ ] pending · [S] skipped (reason) · [!] needs redo
### Inception
- [x] 2.1 Reverse Engineering (delta do codekb)
- [S] 2.2 Team Practices (herdadas da memória M-001..M-012)
- [x] 2.3 Requirements Analysis
- [S] 2.4 User Stories (sem fluxo de usuário novo; critérios ficam nos FR)
- [S] 2.5 Refined Mockups (sem UI)
- [x] 2.6 Domain & Components
- [x] 2.7 Units of Work (4 Units)
- [S] 2.8 Contracts (sem API nova; contratos do pacote ficam em 2.6)
- [x] 2.9 Delivery Plan
### Construction
- [x] 3.1 Functional Design
- [S] 3.2 NFR Requirements
- [S] 3.3 NFR Design
- [x] 3.4 Infrastructure Design
- [x] 3.5 Code Generation (UNIT-1 a UNIT-4)
- [x] 3.6 Build & Test (construction/build-and-test.md)
- [x] 3.7 CI Pipeline (construction/ci-pipeline.md)
### Operations
- (none in this route)

## Units (filled by 2.7)
| Unit | Depends on | Status | Verified |
|---|---|---|---|
| UNIT-1 Python embutido Linux | - | verified | 2026-11-02, script ok + gradle ok 1576 testes |
| UNIT-2 Seleção do interpretador | - | verified (autonomia) | 2026-11-02, gradle ok 1582 testes |
| UNIT-3 Build do pacote | UNIT-1 | verified (autonomia) | 2026-11-02, pacotes gerados + launch xvfb ok |
| UNIT-4 CI e documentação | UNIT-2, UNIT-3 | verified (autonomia) | 2026-11-02, build+validate locais ok; CI não executado |

## Open assumptions
- A1: `jpackage.app-path` aponta para `bin/Moinex` em tempo de execução — confirmed: no (layout do app-image confirmado no spike)
- A2: `jpackage --type deb` funciona no runner — confirmed: no
- A5: `jpackage --type deb --app-image` aceita o app-image do mesmo job — confirmed: no

## Memory candidates
