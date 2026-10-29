# Verification — Inception — java-25-upgrade

Data: 2026-10-29 · Verdict: **PASS**

## 1. Inventário de artefatos
| Artefato | Estado |
|---|---|
| requirements.md § Original request | ✓ |
| requirements.md § 2.3 (10 FR, 4 NFR, restrições, suposições) | ✓ |
| design.md § 2.1 (reuso da codekb + pontos de build) | ✓ |
| tasks.md § 2.9 (comando de verificação aprovado, 3 bolts, 5 Units, 30 tarefas) | ✓ |
| state.md (rota, Units, comando de verificação) | ✓ |
| audit.md (gates 2.1, 2.3, 2.9) | ✓ |
| Estágios fora da rota: 2.2, 2.4–2.8 | n/a (perfil enxuto; práticas do time e histórias não se aplicam a atualização de plataforma) |

## 2. Rastreabilidade FR/NFR → Unit → tarefa → bolt
| Requisito | Unit | Tarefas | Bolt |
|---|---|---|---|
| FR-1 JDK 25 | UNIT-2 | T-2.2 | BOLT-1 |
| FR-2 Gradle 9.x | UNIT-2 | T-2.1 | BOLT-1 |
| FR-3 Kotlin 2.4.x | UNIT-2 | T-2.2, T-2.6 | BOLT-1 |
| FR-4 JavaFX 27.0.1 | UNIT-2 | T-2.3 | BOLT-1 |
| FR-5 Spring Boot 4.x | UNIT-3 | T-3.1, T-3.3, T-3.4, T-3.5 | BOLT-2 |
| FR-6 JPA/SQLite/Hibernate 7 | UNIT-3 | T-3.2, T-3.3, T-3.6 | BOLT-2 |
| FR-7 Ferramentas de qualidade | UNIT-2 | T-2.4, T-2.5 | BOLT-1 |
| FR-8 Remover dependências sem uso | UNIT-1 | T-1.2 a T-1.5 | BOLT-1 |
| FR-9 CI Temurin 25 / instalador | UNIT-4 | T-4.1, T-4.2, T-4.5 | BOLT-3 |
| FR-10 Docs e scripts | UNIT-4, UNIT-5 | T-4.3, T-4.4, T-5.3 | BOLT-3 |
| NFR-1 1374+ testes | UNIT-1, 2, 3, 5 | T-1.6, T-2.5, T-2.7, T-3.5, T-3.7, T-5.1 | todos |
| NFR-2 Compat. de dados | UNIT-3 | T-3.6 | BOLT-2 |
| NFR-3 Smoke manual | UNIT-2, UNIT-5 | T-2.8, T-5.2 | BOLT-1, BOLT-3 |
| NFR-4 Reversibilidade | UNIT-1, UNIT-5 | T-1.1, T-5.4 | BOLT-1, BOLT-3 |

Órfãos: nenhum requisito sem tarefa; nenhuma tarefa sem requisito. Histórias (2.4) não estão na rota, então não há mapa história→Unit.
Cada Unit tem ≥1 tarefa; o DAG UNIT-1→2→3→4→5 é acíclico. Cada NFR aparece na Definition of Done de um bolt.

## 3. Consistência
- Inconsistência encontrada e corrigida: o cabeçalho do UNIT-5 listava NFR-2 sem tarefa que o cobrisse; passou a listar NFR-1, NFR-3, NFR-4 e FR-10. NFR-2 fica no UNIT-3 (T-3.6).
- Termos e IDs (FR-, NFR-, UNIT-, BOLT-, T-) coerentes entre requirements.md, design.md e tasks.md.
- Um número informado no gate 2.9 estava errado (33 tarefas); o correto é 30. Os arquivos nunca citaram o total.

## 4. Riscos aceitos e abertos
A1 (Boot 4 aceita Flyway, dialeto SQLite do Hibernate 7 e Kotest spring extension), A2 (instalador Windows só no CI) e A3 (plugins Gradle com Gradle 9) seguem abertas; têm tarefa de verificação e fallback no UNIT-3 (ADR + checkpoint do UNIT-2).
