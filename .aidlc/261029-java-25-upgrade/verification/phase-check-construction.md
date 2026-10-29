# Verification — Construction — java-25-upgrade

Data: 2026-10-29 · Verdict: **PASS COM PENDÊNCIAS** (3 itens dependem do usuário; nenhuma falha de build ou teste)

## 1. Inventário
| Artefato | Estado |
|---|---|
| tasks.md com Units 1–5, tarefas marcadas e resumos por Unit | ✓ |
| construction/build-and-test.md | ✓ |
| construction/ci-pipeline.md | ✓ |
| Commits na branch chore/java-25 | d50e86e6 (UNIT-1), c9f377c5 (UNIT-2), 727688a2 (UNIT-3), 66e1b565 (UNIT-4); `CLAUDE.md` da UNIT-5 ainda sem commit |

## 2. Unit → código → testes → resultado
| Unit | Arquivos principais | Teste/verificação | Resultado |
|---|---|---|---|
| UNIT-1 | build.gradle.kts, libs.versions.toml | comando de verificação (JDK 21) | exit 0, 1374 testes |
| UNIT-2 | build.gradle.kts, libs.versions.toml, gradle-wrapper.properties | comando de verificação (JDK 25), bootJar, jacoco, smoke | exit 0, 1374 testes, major 69 |
| UNIT-3 | AppConfig.kt, JpaExtensions.kt, FlywayConfig.kt, application.yml, build/catalog, Makefile | comando de verificação, smoke com banco novo e com cópia do banco real | exit 0, 1374 testes, contagens iguais em 43 tabelas |
| UNIT-4 | workflows, .gitlab-ci.yml, scripts/*.sh/.bat, CLAUDE.md | busca por Java 21, `bash -n`, hook | exit 0; workflows não executados |
| UNIT-5 | CLAUDE.md; build limpo | `clean build jacocoTestReport` | exit 0, 1374 testes |

## 3. Cobertura de NFR
| NFR | Validação | Estado |
|---|---|---|
| NFR-1 | 1374 testes em todas as Units | ✓ |
| NFR-2 | contagens iguais; saldos nas telas a conferir | parcial |
| NFR-3 | janela abre; checklist manual pendente | pendente |
| NFR-4 | branch tipada; commits só com aprovação | ✓ |

## 4. Consistência
- IDs e termos coerentes entre requirements.md, design.md e tasks.md.
- Decisão fora do plano original: `scripts/run.sh`, `.gitlab-ci.yml` e checagens do `.bat` também atualizados (descobertos pela busca).
- Premissa corrigida: o 2.1 dizia "JDK 25 já instalado"; só havia o JRE.

## 5. Pendências (aceitas como risco até o usuário agir)
1. T-5.2 / T-2.8: smoke manual completo (navegação, diálogo, troca de tema, cotação, chat de IA, saldos).
2. T-4.5: CI do GitHub e instalador Windows não executados (adiado pelo usuário).
3. Commit do `CLAUDE.md` da UNIT-5 (aguarda decisão).
