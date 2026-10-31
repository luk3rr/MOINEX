# AI-DLC State — java-25-upgrade

- Intent: 261029-java-25-upgrade
- Status: COMPLETE (CI do GitHub e instalador Windows ainda não executados; T-4.5 [S])
- Profile: custom (enxuta)   Depth: Minimal   Tests: Standard
- Project type: brownfield   Repos: MOINEX (HEAD d38e3ee1)
- Switches: collaborators=off reviewers=off plan_approval=off walking_skeleton=off autonomy=gated iteration=unit-major
- Verification command: JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew ktlintCheck spotlessCheck test (approved 2026-10-29; UNIT-1 com JDK 21)
- Language: chat=pt-BR files=pt-BR
- Current: phase=construction stage=3.5 Code Generation (aguardando início da Construction)   Updated: 2026-10-29T19:48:34-03:00

## Route
Legend: [x] done · [>] current · [ ] pending · [S] skipped (reason) · [!] needs redo
### Inception
- [x] 2.1 Reverse Engineering (reuso da codekb, commit d38e3ee1)
- [x] 2.3 Requirements Analysis
- [x] 2.9 Delivery Plan
### Construction
- [x] 3.5 Code Generation
- [x] 3.6 Build & Test (construction/build-and-test.md)
- [x] 3.7 CI Pipeline (construction/ci-pipeline.md; workflows atualizados, não executados)
### Operations
- (none in this route)

## Units (filled by 2.7)
| Unit | Depends on | Status | Verified |
|---|---|---|---|
| UNIT-1 Limpar dependências sem uso | - | verified | 2026-10-29, exit 0 |
| UNIT-2 Toolchain JDK 25 (Boot 3.5.x) | UNIT-1 | verified | 2026-10-29, exit 0 (T-2.8 manual pendente) |
| UNIT-3 Spring Boot 4.x | UNIT-2 | verified | 2026-10-29, exit 0 |
| UNIT-4 CI, docs e scripts | UNIT-3 | verified | 2026-10-29, exit 0 (T-4.5 [S]) |
| UNIT-5 Verificação final e smoke | UNIT-4 | verified | 2026-10-29, build OK + smoke do usuário |

## Open assumptions
- A1 (2.3): Spring Boot 4.x aceita Flyway 11, dialeto SQLite do Hibernate 7 e Kotest spring extension — confirmed: yes (UNIT-3, 2026-10-29: testes e cópia do banco real)
- A2 (2.3): instalador Windows só verificável no CI — confirmed: no
- A4 (3.5): premissa errada no 2.1: a máquina tem só o JRE 25 (sem javac); precisa de `sudo apt install openjdk-25-jdk` — corrigido ao usuário
- A3 (2.3): plugins Gradle (javafx 0.1.0, Spotless 6.25.0, ktlint) funcionam com Gradle 9.x — confirmed: yes (UNIT-2; ktlint exigiu fixar o compilador embutido)

## Memory candidates  (proposed, not yet approved — see protocols.md §Memory)

## Notes
- Mesclado em `main` localmente (5bf86e53) e liberado como v2.19.0 (182ccdae, tag anotada "Upgrade to Java 25, Spring Boot 4 and JavaFX 27"). Nada foi enviado ao remoto.
- Commits na branch chore/java-25: d50e86e6 (UNIT-1), c9f377c5 (UNIT-2), 727688a2 (UNIT-3), 66e1b565 (UNIT-4) e fbb1d063 (CLAUDE.md, UNIT-5). Sem push; main está 5 commits atrás. Pendente do usuário: smoke manual (T-5.2) e decisão sobre push/PR/CI (T-4.5). Hook de pré-commit passou nos dois (JDK 21 e JDK 25). O `Makefile` tem uma alteração não commitada (JAVA_HOME → java-25) que não foi feita pelo Claude; entra na revisão do UNIT-4/T-4.3.
- Pré-requisito do intent 261029-adopt-atlantafx (pausado): AtlantaFX 3.0.0 exige Java 25+ e JavaFX 27+. Quando este intent chegar ao main, retomar aquele com /ai-dlc resume.
