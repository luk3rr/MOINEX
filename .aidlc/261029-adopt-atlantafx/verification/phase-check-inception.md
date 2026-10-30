# Verification — Inception — adopt-atlantafx

Data: 2026-10-29 · Verdict: **PASS** (a inconsistência de redação abaixo foi corrigida a pedido do usuário)

## 1. Inventário de artefatos

| Artefato | Estado |
|---|---|
| requirements.md § Original request | ✓ |
| requirements.md § 2.3 (12 FR, 7 NFR, restrições, fora de escopo, glossário, suposições) | ✓ |
| requirements.md § 2.4 (2 personas, 16 histórias, mapa, rastreabilidade) | ✓ |
| design.md § 2.1 (resumo da codekb) e § 2.2 (práticas) | ✓ |
| design.md § 2.5 (mockups) e `mockups/mockups.html` | ✓ |
| design.md § 2.6 (7 componentes, 6 ADRs, anexo de ícones) | ✓ |
| design.md § 2.7 (14 Units, DAG, mapas) e § 2.8 (contratos) | ✓ |
| tasks.md § 2.9 (comando de verificação aprovado, 6 bolts, 133 tarefas) | ✓ |
| state.md (rota, Units, suposições, comando de verificação) e audit.md | ✓ |
| `.aidlc/codekb/MOINEX/` (human-reviewed, delta pós v2.19.0) | ✓ |

## 2. Rastreabilidade requisito → história → Unit → bolt

| Requisito | Histórias | Unit | Bolt |
|---|---|---|---|
| FR-1, FR-2, FR-3 | US-1 | UNIT-2 | BOLT-1 |
| FR-4 | US-2 | UNIT-2 | BOLT-1 |
| FR-5, FR-6 | US-3 | UNIT-3 | BOLT-2 |
| FR-7 | US-4 | UNIT-2 | BOLT-1 |
| FR-8 | US-5 | UNIT-4 | BOLT-2 |
| FR-12 | US-6 | UNIT-5 | BOLT-2 |
| FR-9 | US-7 a US-14 | UNIT-8 a UNIT-13 | BOLT-4 e BOLT-5 |
| FR-10 | US-15 | UNIT-6 | BOLT-3 |
| FR-11 | US-16 | UNIT-7 | BOLT-3 |
| NFR-1, NFR-6 | todas | todas as Units | todos |
| NFR-2 | US-2 | UNIT-2 (teste), UNIT-5 e UNIT-8 a UNIT-13 | BOLT-1, 2, 4 e 5 |
| NFR-3 | — | UNIT-1 (antes) e Units de interface (depois), UNIT-14 | todos |
| NFR-4 | — | UNIT-3, UNIT-14 | BOLT-2, 6 |
| NFR-5 | — | UNIT-1 (base), UNIT-14 (depois) | BOLT-1, 6 |
| NFR-7 | US-15, US-16 | UNIT-6, UNIT-7, UNIT-14 | BOLT-3, 6 |

- Órfãos: nenhum requisito sem história ou Unit; nenhuma história sem requisito; nenhuma tarefa sem requisito ou teste (checagem mecânica sobre as 133 tarefas).
- Cada uma das 16 histórias tem exatamente uma Unit; o DAG é acíclico; cada Unit tem ≥1 tarefa; cada NFR aparece na Definition of Done de uma Unit.
- Desvio registrado: FR-9 é coberto por 6 Units (uma por grupo de telas), porque tem 8 histórias; a regra "um dono por FR" foi aplicada no nível da história.

## 3. Consistência

- Resolvido durante a Inception: FR-11, US-16 e US-17 foram revisados a pedido do usuário (apenas Ctrl+S); a versão anterior está em `archive/`; `design.md` §§ 2.5 a 2.8 e `tasks.md` seguem a versão revista.
- Resolvido: as medições de contraste mudaram a paleta (janela `#21222C` e card `#282A36` no escuro; roxo, aviso e positivo como texto no claro); § 2.5, § 2.8 e a UNIT-2 usam os mesmos valores.
- Corrigido: contagem de usos de ícone em FXML (269) e de diálogos de criar/editar (46).
- **Corrigido em 2026-10-29:** o critério AC1 de US-5 e a descrição de FR-8 dizem que os 69 ícones mudam de cor com o tema, mas o ADR-3 (aprovado) admite que as aproximações recusadas na revisão visual fiquem em PNG. FR-8 e o AC1 de US-5 foram reescritos como "os ícones de interface aprovados mudam de cor; os recusados na revisão ficam em PNG e são listados".

## 4. Riscos e suposições abertas

A3, A4, A5, A11, A12, A13, A14, A15, A16, A18 e A19; todas têm tarefa de verificação (UNIT-1, UNIT-2, UNIT-4, UNIT-6 e UNIT-12) e, se falharem, o caminho é voltar ao 2.6 ou usar a reserva descrita no ADR correspondente.
