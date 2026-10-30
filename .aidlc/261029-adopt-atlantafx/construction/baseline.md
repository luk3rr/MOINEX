# Baseline — adopt-atlantafx (UNIT-1)

Data: 2026-10-29 · Commit de base: `c89959eb` (main, v2.19.0 + documentação) · JDK 25.0.4.1, JavaFX 27.0.1, Spring Boot 4.1.2

## Tempo até a janela principal (NFR-5)

Medido por `scripts/measure-startup.sh 5`: o script sobe o jar com um `user.home` descartável (banco novo) e mede o tempo até a janela "Moinex" aparecer (`xwininfo`).

| Execução | Segundos |
|---|---|
| 1 | 7,69 |
| 2 | 7,75 |
| 3 | 7,79 |
| 4 | 7,79 |
| 5 | 7,79 |

**Média: 7,76 s.** O limite de NFR-5 (A5, proposta de 20%) é, portanto, **9,31 s** na medição final da UNIT-14. A medida inclui o `delay(1000)` fixo do `JavaFXApp` e a subida do Spring (~5 s); ela é feita com banco novo, não com um banco grande.

## Capturas "antes"

- Geradas por `scripts/capture-screens.sh` com banco fictício de `scripts/generate_dummy_data.py`, janela de 1440 × 900, escala 1x, nos temas claro e escuro.
- 32 PNGs (16 telas × 2 temas) em `.aidlc/261029-adopt-atlantafx/construction/screenshots/before/` (local, ignorado pelo git). A busca (17) e o `Alert` (18) não existiam ainda.
- `16-dialog-add-expense-error` mostra o diálogo de despesa **vazio**: o erro inline só existe depois da UNIT-6.

## Observações das capturas

- Home e Carteiras/Cartões usam **cartões coloridos com gradiente** (verde para carteiras, azul e roxo para cartões de crédito), que o mockup do § 2.5 simplificou.
- A sidebar fica **recolhida só com ícones** por padrão (o botão de menu expande); o mockup mostra a versão expandida com rótulos.
- No tema claro a sidebar é um teal claro (`#1ABC9C`, hover `#48C9B0`) com ícones escuros; no escuro é `#0A614E`. O contraste de texto branco sobre `#1ABC9C` seria 2,26:1 (reprova); com texto `#1F2328` é 6,56:1.
- O chat de IA aparece sem mensagens (conversa vazia).

## Medição final (UNIT-14, 2026-10-30)

Cinco execuções com `scripts/measure-startup.sh 5`: 8,38 · 8,31 · 8,14 · 8,00 · 8,02 s. **Média: 8,17 s**, abaixo do limite de 9,31 s (+20% sobre a base de 7,76 s). Medido com banco novo, como na base.
