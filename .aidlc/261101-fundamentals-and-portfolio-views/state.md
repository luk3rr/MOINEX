# AI-DLC State — fundamentals-and-portfolio-views

- Intent: 261101-fundamentals-and-portfolio-views
- Status: COMPLETE (aguardando revisão e commit do dono)
- Profile: classic   Depth: Standard   Tests: Standard
- Project type: brownfield   Repos: MOINEX (HEAD c72d94af, com modificação não commitada em src/main/resources/scripts/get_fundamental_data.py)
- Switches: collaborators=off reviewers=off plan_approval=off walking_skeleton=off autonomy=autonomous (Units; falhas e commit sempre param) iteration=unit-major
- Verification command: export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 && ./gradlew ktlintCheck spotlessCheck test && /usr/bin/python3 -m unittest discover -s src/test/python -v (approved 2026-11-01)
- Language: chat=pt-BR files=pt-BR
- Current: phase=construction concluída; rodada 2 de ajustes feita; nada commitado   Updated: 2026-11-01T18:35:06-03:00

## Route
Legend: [x] done · [>] current · [ ] pending · [S] skipped (reason) · [!] needs redo
### Inception
- [x] 2.1 Reverse Engineering (codekb delta c72d94af, baseline 1411 testes)
- [x] 2.2 Team Practices
- [x] 2.3 Requirements Analysis
- [x] 2.4 User Stories
- [x] 2.5 Refined Mockups (layout B: nova aba Desempenho)
- [x] 2.6 Domain & Components
- [x] 2.7 Units of Work (12 Units)
- [x] 2.8 Contracts
- [x] 2.9 Delivery Plan
### Construction
- [x] 3.1 Functional Design (nota curta por Unit em design.md e contratos do 2.8)
- [S] 3.2 NFR Requirements (coberto por NFR-1..7 globais e DoD das Units)
- [S] 3.3 NFR Design (idem)
- [S] 3.4 Infrastructure Design (app desktop, sem infraestrutura)
- [x] 3.5 Code Generation (UNIT-1 a UNIT-12)
- [x] 3.6 Build & Test (construction/build-and-test.md)
### Operations
- (none in this route)

## Units (filled by 2.7)
| Unit | Depends on | Status | Verified |
|---|---|---|---|
| UNIT-1 Dados do script | - | verified + approved | 2026-11-01, gradle ok + 15 testes Python |
| UNIT-2 Motivo do N/A | UNIT-1 | verified + approved | 2026-11-01, gradle ok 1424 testes + 24 Python |
| UNIT-3 Semáforo, nota, setor | UNIT-2 | verified + approved | 2026-11-01, gradle ok 1449 testes + 24 Python |
| UNIT-4 FII e ETF | UNIT-1, UNIT-2 | verified + approved (autonomia)26-11-01, gradle ok + python ok |
| UNIT-5 Série diária | - | verified + approved (autonomia)26-11-01, gradle ok + python ok |
| UNIT-6 Tabela: retorno, peso, DY, filtros | - | verified + approved (autonomia)26-11-01, gradle ok + python ok |
| UNIT-7 Sparkline | UNIT-5, UNIT-6 | verified + approved (autonomia)26-11-01, gradle ok + python ok |
| UNIT-8 Detalhe do ativo | UNIT-5, UNIT-6 | verified + approved (autonomia)26-11-01, gradle ok + python ok |
| UNIT-9 Aba Desempenho: benchmarks | UNIT-5, UNIT-6 | verified + approved (autonomia)26-11-01, gradle ok + python ok |
| UNIT-10 Contribuição | UNIT-9 | verified + approved (autonomia)26-11-01, gradle ok + python ok |
| UNIT-11 Dividendos | UNIT-9 | verified + approved (autonomia)26-11-01, gradle ok + python ok |
| UNIT-12 Rebalanceamento e alertas | UNIT-6 | verified + approved (autonomia)26-11-01, gradle ok + python ok |

## Open assumptions
- A2 (2.3): Ibovespa histórico via ^BVSP no script de preço; CDI/IPCA via MarketIndicatorHistory — confirmed: yes na prática (Ibovespa foi para tabela própria, V035; CDI/IPCA em MarketIndicatorHistory)
- A3 (2.3): câmbio USD/BRL histórico via USDBRL=X — confirmed: no (não exercido: VALE3 e demais da carteira reportam em BRL)
- A7 (2.5/2.8): semáforo ≥ 20% / ≤ −20% — confirmed: yes
- A11 (2.8): faixas de subnota fixadas na UNIT-3, ajustadas com ativos reais por leitura read-only do banco real, com aval — confirmed: no (conferido com dados reais na rodada 5; só o setor financeiro foi ajustado)
- A12 (2.8): contribuição inclui realizado e dividendos; diferença para o card Lucro/Prejuízo explicada na tela — confirmed: yes
- A4 (2.3): FIIs/ETFs expõem P/VP e DY em ticker.info — confirmed: parcial (DY é calculado pelos proventos; P/VP existe para alguns FIIs, não para todos)
- A5 (2.3): projeção de dividendos 12m = soma dos últimos 12 meses — confirmed: yes (gate 2.8)

## Memory candidates  (proposed, not yet approved)

- C2 (3.6): DESCARTADA pelo usuário em 2026-11-01.

## Notes
