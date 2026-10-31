# AI-DLC State — adopt-atlantafx

- Intent: 261029-adopt-atlantafx
- Status: COMPLETE
- Profile: classic   Depth: Standard   Tests: Standard
- Project type: brownfield   Repos: MOINEX (HEAD 6201d096; codekb analisada em 08bbf24f)
- Switches: collaborators=off reviewers=off plan_approval=off walking_skeleton=on autonomy=gated (checkpoints automáticos a partir da UNIT-3) iteration=unit-major
- Verification command: JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew ktlintCheck spotlessCheck test (approved 2026-10-29)
- Language: chat=pt-BR files=pt-BR
- Current: intent fechada pelo usuário. Aprovações visuais e smoke manual feitos; Resumo anual sem mudanças; posição do "+" aprovada; créditos de cartão em novo intent. Updated: 2026-10-30

## Route
Legend: [x] done · [>] current · [ ] pending · [S] skipped (reason) · [!] needs redo
### Inception
- [x] 2.1 Reverse Engineering
- [x] 2.2 Team Practices
- [x] 2.3 Requirements Analysis
- [x] 2.4 User Stories
- [x] 2.5 Refined Mockups
- [x] 2.6 Domain & Components
- [x] 2.7 Units of Work
- [x] 2.8 Contracts
- [x] 2.9 Delivery Plan
### Construction
- [x] 3.1 Functional Design (UNIT-1 e UNIT-2 detalhados em design.md; UNIT-3 a UNIT-14 cobertos por 2.5–2.8 e tasks.md)
- [x] 3.2 NFR Requirements (design.md, blocos "NFR requirements" de UNIT-1 e UNIT-2; NFR-1 a NFR-7 em requirements.md)
- [x] 3.3 NFR Design (design.md, blocos "NFR design" de UNIT-1 e UNIT-2; ADR-1 a ADR-7)
- [x] 3.4 Infrastructure Design (sem mudança de infraestrutura; design.md, blocos "Infrastructure design")
- [x] 3.5 Code Generation (UNIT-1 a UNIT-14, VERIFICATION_RUN no audit.md)
- [x] 3.6 Build & Test (construction/build-and-test.md; verification/phase-check-construction.md)
### Operations
- (none in profile classic)

## Units (filled by 2.7)
| Unit | Depends on | Status | Verified |
|---|---|---|---|
| UNIT-1 Capturas e medições de base | - | verified | 2026-10-29, exit 0 |
| UNIT-2 Fundação do tema (walking skeleton) | UNIT-1 | verified (skeleton aprovado) | 2026-10-29, exit 0 |
| UNIT-3 JFoenix e CSS redundante | UNIT-2 | verified | 2026-10-29, exit 0 |
| UNIT-4 Ícones Ikonli | UNIT-3 | verified | 2026-10-29, exit 0 |
| UNIT-5 Gráficos e chat no tema | UNIT-3 | verified | 2026-10-29, exit 0 |
| UNIT-6 Formulários e diálogos | UNIT-3, UNIT-4 | verified | 2026-10-30, exit 0 |
| UNIT-7 Busca global por Ctrl+S | UNIT-2, UNIT-4 | verified | 2026-10-30, exit 0 |
| UNIT-8 Início e Resumo anual | UNIT-4, UNIT-5 | verified (aguarda T-8.6) | 2026-10-30, exit 0 |
| UNIT-9 Carteiras e Cartões | UNIT-4, UNIT-5 | verified (aguarda aprovação visual) | 2026-10-30, exit 0 |
| UNIT-10 Transações | UNIT-4, UNIT-5 | verified (aguarda aprovação visual) | 2026-10-30, exit 0 |
| UNIT-11 Metas, Planos, FIRE e Wishlist | UNIT-4, UNIT-5 | verified (aguarda aprovação visual) | 2026-10-30, exit 0 |
| UNIT-12 Poupança | UNIT-4, UNIT-5 | verified (aguarda aprovação visual) | 2026-10-30, exit 0 |
| UNIT-13 Chat, Configurações e Notificações | UNIT-4, UNIT-5 | verified (aguarda aprovação visual) | 2026-10-30, exit 0 |
| UNIT-14 Fechamento | UNIT-6, UNIT-7, UNIT-8 a UNIT-13 | verified (aguarda aprovação de capturas e smoke) | 2026-10-30, exit 0 |

## Open assumptions
- A1 (2.3): "Substituir JavaFX por AtlantaFX" = adotar o AtlantaFX 3.0.0 como tema base (no lugar do Modena + overrides) e remover o JFoenix; o JavaFX continua como toolkit, na versão 27. — confirmed: yes (o usuário respondeu pedindo Java 25 + AtlantaFX mais recente, o que pressupõe essa leitura)
- A2 (2.3): Verde da sidebar também no tema claro — confirmed: yes

- A3 (2.3): AtlantaFX 3.0.0 (Dracula e PrimerLight) funciona com JavaFX 27.0.1 / Java 25 — confirmed: yes (UNIT-2)
- A4 (2.3): um pacote do Ikonli cobre os 69 ícones de interface — confirmed: no (escolha no 2.6)
- A5 (2.3): limite de +20% de desempenho (NFR-5) é proposta da IA — confirmed: no
- A6 (2.3): "Todas as telas" inclui Chat de IA, Configurações e Notificações — confirmed: yes (o usuário marcou "Todas"; aprovado no gate)
- A7 (2.4): Ctrl+S (único atalho global) não conflita com atalhos existentes — confirmed: yes (grep em 2026-10-29); sem efeito em diálogos modais
- A8 (2.4): o catálogo inicial de ações da busca (definido no 2.5) basta — confirmed: no
- A9 (2.5): no escuro, janela #21222C e card #282A36 (todos os textos passam AA) — confirmed: no
- A10 (2.5): o catálogo de 23 ações da busca basta no começo — confirmed: no
- A11 (2.5): dá para sobrepor o roxo do Dracula nativo por #BD93F9 via -color-accent-* — confirmed: yes, com ajuste (rampa -color-accent-0..9; UNIT-2)
- A12 (2.5): menu por linha nas tabelas de Poupança substitui a fileira de botões sem perder ações — confirmed: no
- A13 (2.6): a camada de tokens por Scene convive com o tema base (sem ThemeManager) — confirmed: yes (UNIT-2)
- A14 (2.6): a API do atlantafx-validation serve para os formulários — confirmed: no (reserva: validação própria)
- A15 (2.6): o listener em Window.getWindows() alcança as janelas dos Alert nativos — confirmed: yes (UNIT-2)
- A4 (atualizada em 2.6): os 69 PNGs têm glifo no Material 2; 11 são aproximações a rever nas capturas — confirmed: parcial
- A16 (2.7): captura via Node.snapshot funciona com WebView e gráficos usando o banco fictício — confirmed: yes (UNIT-1)
- A17 (2.7): a ordem serial das 14 Units não gera conflitos — confirmed: no
- A18 (2.8): atlantafx-validation registra regras sobre ComboBox e DatePicker — confirmed: no (UNIT-6; reserva: validação manual)
- A19 (2.8): o runner de captura abre telas e diálogos sem interação; WebView pode precisar de atraso — confirmed: yes (UNIT-1; diálogo exigiu yield() antes do showAndWait)

## Memory candidates  (proposed, not yet approved — see protocols.md §Memory)

## Notes
- Branch feat/atlantafx: 79a297ea (UNIT-1) e 44defaee (UNIT-2) commitados; UNIT-3 em diante em modo automático (sem push).
- Atualização do pré-requisito (2026-10-29T19:42:57-03:00): o intent 261029-java-25-upgrade tem JDK 25, Gradle 9.8.2, Kotlin 2.4.21, JavaFX 27.0.1 e Spring Boot 4.1.2 prontos e verificados na branch `chore/java-25` (5 commits, último fbb1d063), ainda **não** mergeada no `main`, sem push e sem CI executado. **Atualização 2026-10-29T19:48:34-03:00: o pré-requisito está mesclado na `main` local (5bf86e53) e tagueado v2.19.0; ainda sem push.** Pode retomar este intent com `/ai-dlc resume`.
- PAUSADO em 2026-10-29T18:55:22-03:00: o usuário decidiu que a atualização para Java 25 (Kotlin 2.3+, JavaFX 27, MockK, ktlint, jlink/instalador, workflows) é um **intent separado, feito antes deste**. Este intent só entra em Construction depois dele estar no main. Retomar com `/ai-dlc resume` neste intent, no estágio 2.3 (decisões em requirements.md § 2.3 draft).
- Contexto prévio da conversa: avaliação de AtlantaFX vs Compose Multiplatform; mobile fora do curto prazo; prioridade é interface e usabilidade; manter o caminho livre para Compose no futuro (sem tipos JavaFX em service/model/repository).
