# Verificação Inception → Construction — bond-detail-comparison

Data: 2026-11-07T19:22:58-03:00 · Perfil mvp · Profundidade Standard

## 1. Inventário de artefatos

| Artefato | Estado |
|---|---|
| `requirements.md` (1.1, 1.3, 1.4, 2.3 com 20 FR e 7 NFR, 2.4 com 11 histórias) | ✓ |
| `design.md` (1.6, 2.1, 2.2, 2.5, 2.6, 2.7, 2.8) | ✓ |
| `tasks.md` (2.9: verificação aprovada, 2 bolts, 5 Units, 27 tarefas) | ✓ |
| `mockups/layouts.html` (rústico) e `mockups/refined.html` (refinado), em HTML | ✓ |
| `.aidlc/codekb/MOINEX/areas/bond-screens.md` | ✓ (draft, aprovado no gate 2.1) |
| `state.md`, `audit.md` | ✓ |
| 1.2, 1.5, 1.7 | fora da rota do perfil mvp |

## 2. Rastreabilidade (FR/NFR → história → Unit → tarefa)

Conferida por script sobre os três arquivos:
- 20 FR (14 Must e 6 Should), 7 NFR, 11 histórias, 27 tarefas.
- Todos os FR estão em pelo menos uma história e têm um Unit dono; nenhum órfão em nenhum sentido.
- Todo FR tem tarefa; cada NFR aparece em tarefa ou na Definition of Done de um bolt.
- Toda tarefa tem `covers` e teste ou captura.
- DAG acíclico: UNIT-1 e UNIT-2 e UNIT-4 sem dependência; UNIT-3 depende de 1 e 2; UNIT-5 depende de 1 e 4.

## 3. Consistência

- Contagem de Must e Should: o resumo do gate 2.3 dizia 13 e 7; conferido por comando depois, são 14 e 6; registrado no `audit.md`.
- Os mockups rústicos saíram primeiro só em texto, contra a regra M-007; refeitos em HTML e a regra foi endurecida.
- Decisão do dono: janela de detalhe (opção A); o painel lateral não será construído; a tabela mantém 6 colunas (FR-1).
- Termos e IDs concordam entre os arquivos.

## 4. Pontos de atenção para a Construction

- O líquido por dia (UNIT-4) é a conta mais sujeita a erro; precisa de teste de referência calculado à parte.
- O detalhe reaproveita o histórico de juros e as operações, hoje diálogos próprios; extrair sem mudar comportamento.
- Capturas de tela precisam do alvo de captura para os novos diálogos (como foi feito de forma temporária no intent anterior).

## Veredito: PASS
