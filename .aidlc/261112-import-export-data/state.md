# AI-DLC State — import-export-data

- Intent: 261112-import-export-data
- Status: COMPLETE (revisado e commitado pelo dono em main; Operations fora da rota)
- Profile: classic   Depth: Standard   Tests: Standard
- Project type: brownfield   Repos: MOINEX (HEAD 8c32f394; codekb em 95b19ba7, defasado)
- Switches: collaborators=off reviewers=off plan_approval=off walking_skeleton=off autonomy=autonomous (aprovado em bloco em 2026-11-13: Units 3 a 7 e 3.6; falhas, verificação e commits param; sem commit) iteration=unit-major
- Verification command: export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && env -u DISPLAY -u WAYLAND_DISPLAY ./gradlew ktlintCheck spotlessCheck test (approved 2026-11-12)
- Branch: main (direto, por escolha do dono; M-014)
- Language: chat=pt-BR files=pt-BR
- Current: construction concluída; commitado   Updated: 2026-11-13T14:07:47.044423-03:00

## Route
Legend: [x] done · [>] current · [ ] pending · [S] skipped (reason) · [!] needs redo
### Inception
- [x] 2.1 Reverse Engineering (delta; codekb aefe5133 reusado)
- [S] 2.2 Team Practices (herdadas da memória M-003..M-017)
- [x] 2.3 Requirements Analysis
- [x] 2.4 User Stories
- [x] 2.5 Refined Mockups (mockups/refined.html; layout D2)
- [x] 2.6 Domain & Components
- [x] 2.7 Units of Work
- [x] 2.8 Contracts
- [x] 2.9 Delivery Plan
### Construction
- [x] 3.1 Functional Design (UNIT-1..7)
- [S] 3.2 NFR Design (NFRs tratadas na Inception e nos testes)
- [S] 3.3 Infrastructure Design (app desktop)
- [S] 3.4 CI Pipeline (a pipeline existente já roda ktlint, spotless e test)
- [x] 3.5 Code Generation (UNIT-1..7)
- [x] 3.6 Build & Test (construction/build-and-test.md)

## Units (filled by 2.7)
| Unit | Depends on | Status | Verified |
|---|---|---|---|
| UNIT-1 Fundação | — | verified | yes (2026-11-12) |
| UNIT-2 Carteiras | UNIT-1 | verified | yes (2026-11-13) |
| UNIT-3 Interface | UNIT-2 | verified | yes (2026-11-13) |
| UNIT-4 Cartões | UNIT-2 | verified | yes (2026-11-13) |
| UNIT-5 Investimentos | UNIT-2 | verified | yes (2026-11-13) |
| UNIT-6 Planejamento | UNIT-2, UNIT-4 | verified | yes (2026-11-13) |
| UNIT-7 Fechamento | UNIT-3..6 | verified | yes (2026-11-13) |

## Open assumptions
- A1 (2.1): codekb de aefe5133 ainda vale para as áreas tocadas — confirmed: yes (gate 2.1)
- A2 (2.3): chave natural de entidades sem nome único = conjunto de campos (definir em 2.8); idênticas legítimas no mesmo arquivo são distintas — confirmed: yes (gate 2.3)
- A3 (2.3): campo layout_version=1 no arquivo — confirmed: yes (gate 2.3)
- A4 (2.3): export inclui arquivadas/inativas; import recria como arquivadas — confirmed: yes (gate 2.3)
- A5 (2.3): FR-15 (modelo vazio) só entra se couber — confirmed: yes (gate 2.3)
- A6 (2.4): uma única persona — confirmed: yes (gate 2.4)
- A7..A13 (2.5-2.9): limites de 50 MB/200 entradas, arquivadas exportadas, ADR-1 revalidado no início da Construction, balance de carteira a fechar no 3.1 do UNIT-2, autonomia a decidir no 3.x — confirmed: yes (gates 2.5-2.9)

## Memory candidates
