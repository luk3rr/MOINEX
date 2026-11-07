# Verificação Inception → Construction — financial-calculations-review

Data: 2026-11-07T16:35:17-03:00 · Perfil custom (2.1, 2.3, 2.7, 2.9) · Profundidade Standard

## 1. Inventário de artefatos

| Artefato | Estado |
|---|---|
| `requirements.md` (1.1, 1.4, 2.3 com 26 FR, 6 NFR e 5 premissas) | ✓ |
| `design.md` (§ 2.1 resumo e § 2.7 com 7 Units e DAG) | ✓ |
| `tasks.md` (§ 2.9 com verificação aprovada, 3 bolts, 35 tarefas) | ✓ |
| `state.md`, `audit.md` | ✓ |
| `.aidlc/codekb/MOINEX/areas/financial-calculations.md` (human-reviewed) | ✓ |
| § 2.8 Contratos em `design.md` | não aplicável (sem API externa nova; contrato de dados é a migração V039) |
| Histórias de usuário (2.4) | fora da rota do perfil |

## 2. Rastreabilidade (FR/NFR → Unit → tarefa)

Conferida por script sobre os três arquivos:
- 26 FR, todos com Unit em `design.md` e pelo menos uma tarefa em `tasks.md`; nenhum órfão.
- 6 NFR, todos citados no `design.md` (Definition of Done de Unit); NFR-4 e NFR-5 têm tarefa própria (T-1.6, T-1.2).
- 35 tarefas, todas com `covers`; nenhuma sem requisito.
- DAG acíclico: UNIT-3 e UNIT-1 e UNIT-6 sem dependência; UNIT-2 → UNIT-1; UNIT-4 → UNIT-1 e UNIT-3; UNIT-5 → UNIT-1; UNIT-7 → todos.

## 3. Consistência

- Contagens: 17 Must e 9 Should (o resumo apresentado no gate 2.3 dizia 20 e 6; o arquivo foi corrigido), 35 tarefas (o resumo do gate 2.9 dizia 38; corrigido). Ambos os gates foram aprovados com esses números errados no resumo, e as correções foram registradas no `audit.md`.
- O escopo (§ 1.4) citava "dois campos novos"; reescrito para listar os controles novos de verdade.
- Os exemplos numéricos dos critérios de aceite foram recalculados por script (1,65; 0,58; 1.134,25; 100,00; 3,74; 2,49) e dois valores iniciais estavam errados (Graham e capitalização), já corrigidos.
- Termos e IDs concordam entre os arquivos.

## 4. Pontos de atenção para a Construction

- Baseline: 2041 testes Kotlin e 109 Python passam hoje (resultado reaproveitado pelo Gradle, tarefas `UP-TO-DATE`); o primeiro `test` do UNIT-3 é uma execução de fato.
- Premissas A1 a A5 confirmadas pelo dono no gate 2.3; A2 (títulos flutuantes atuais como percentual do índice) é a que mais merece conferência visual depois do UNIT-1.
- Nenhum commit sem o dono ver o diff (M-004); trabalho direto na `main` (M-014).

## Veredito: PASS
