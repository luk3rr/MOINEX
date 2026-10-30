# Cores fixas fora dos tokens — estado após a UNIT-3 (NFR-4)

Contagem por arquivo de ocorrências de hexadecimal, `rgb(a)` ou cor nomeada fora de `css/theme/tokens.*.css`, ignorando comentários. Cada arquivo tem uma Unit dona que converte para tokens; a UNIT-14 refaz a busca e só admite a exceção aprovada abaixo.

| Arquivo | Ocorrências | Dono |
|---|---|---|
| `component/charts.css` | 75 | UNIT-5 |
| `component/notification.css` | 18 | UNIT-13 |
| `component/timeline-chart.css` | 36 | UNIT-5 |
| `scene/annual_summary.css` | 6 | UNIT-8 |
| `scene/fire_calculator.css` | 4 | UNIT-11 |
| `scene/home.css` | 19 | UNIT-8 |
| `scene/main.css` | 3 | exceção aprovada (sombra neutra rgba(0,0,0,α)) |
| `scene/savings.css` | 10 | UNIT-12 |

**Convertidos na UNIT-3:** `css/scene/wallet.css` (duas cores de barra, agora `theme-negative` e `theme-positive`) e `css/component/progress-bar.css` (hexadecimais e gradientes fixos, agora tokens).

**Exceção aprovada:** sombras neutras `dropshadow(..., rgba(0, 0, 0, α), ...)`, que não pertencem a uma paleta de tema.
