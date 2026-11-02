# Verificação — Inception (261102-linux-package)

## 1. Inventário

| Artefato | Presente |
|---|---|
| requirements.md (1.1, 1.4, 2.3) | ✓ |
| design.md (2.1, 2.6, 2.7) | ✓ |
| tasks.md (2.9, comando aprovado) | ✓ |
| 2.2, 2.4, 2.5, 2.8 | [S] com motivo em state.md |

## 2. Rastreabilidade (sem histórias na rota)

| Requisito | Unit | Tarefas |
|---|---|---|
| FR-1 | UNIT-3 | T-3.1, T-3.3 |
| FR-2 | UNIT-3 | T-3.1, T-3.2 |
| FR-3 | UNIT-1 | T-1.1, T-1.2 |
| FR-4 | UNIT-2 | T-2.1, T-2.2 |
| FR-5 | UNIT-3 | T-3.3 |
| FR-6 | UNIT-3 | T-3.4 |
| FR-7 | UNIT-3 | T-3.4 |
| FR-8 | UNIT-4 | T-4.1 |
| FR-9 | UNIT-4 | T-4.2 |
| FR-10 | UNIT-4 | T-4.3 |
| NFR-1 | UNIT-4 | T-4.1 |
| NFR-2 | UNIT-3 | T-3.5 |
| NFR-3 | UNIT-1 | T-1.1 |
| NFR-4 | UNIT-1 | T-1.1 |
| NFR-5 | UNIT-2 | T-2.2 |
| NFR-6 | UNIT-3 | T-3.1 |

Sem órfãos; DAG acíclico (U1→U3, U2→U4, U3→U4).

## 3. Consistência
Nome do `.deb`, caminho `lib/python/bin/python3`, `ubuntu-22.04` e Python 3.12 coincidem entre os três arquivos. FR-1 foi ajustado ao nome real gerado pelo jpackage.

## 4. Veredito: PASS
Premissas A1, A2 e A5 seguem abertas e serão fechadas na Construction.
