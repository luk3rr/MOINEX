# Phase check — Inception (261106-ai-chat-history)

## 1. Inventário de artefatos

| Artefato | Presente |
|---|---|
| requirements.md (Original request, 1.1, 2.3, 2.4) | ✓ |
| design.md (2.1, 2.2, 2.5, 2.6, 2.7, 2.8) | ✓ |
| tasks.md (2.9: comando de verificação aprovado, bolts, tarefas) | ✓ |
| state.md e audit.md atualizados | ✓ |
| mockups/conversation-list-layouts.html | ✓ |

## 2. Rastreabilidade

| Verificação | Resultado |
|---|---|
| Os 14 FR estão em ≥1 história (US-1..7) | ✓ |
| Cada história liga a ≥1 FR e a ≥1 Unit | ✓ |
| Cada FR e NFR tem dono em uma Unit (design.md §2.7) | ✓ |
| Cada FR e NFR aparece em ≥1 tarefa de tasks.md | ✓ (NFR-1 e NFR-3 ganharam T-1.12 e T-1.11 durante esta verificação) |
| DAG de Units acíclico | ✓ |
| Cada Unit tem ≥1 tarefa com teste | ✓ |
| Cada NFR está no DoD de um bolt ou em tarefa | ✓ |

## 3. Consistência

- IDs e termos concordam entre os três arquivos (conversa, mensagem, ChangeSet, propostas).
- Contradição corrigida: FR-11 foi revisado no gate 2.4 e US-6 e o desenho do painel (§2.5, §2.6) concordam.
- Pendência de confirmação: A1–A9 (premissas) e a versão do Jackson no BOM (T-2.1).
- UNIT-1 usa org.json para tool calls (T-1.5) e UNIT-2 introduz Jackson; o codec da UNIT-2 deve também cobrir `tool_calls_json`, e a UNIT-2 migra essa gravação ao codec (contrato §2.8 lista `toolCallsToJson`). Aceito como retrabalho pequeno.

## 4. Veredito

**PASS** (1 observação: ordem org.json → Jackson para tool calls).
