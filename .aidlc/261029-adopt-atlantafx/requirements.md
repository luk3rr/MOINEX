# Requirements — adopt-atlantafx

## Original request
Quero substituir JavaFX por AtlantaFX

(Leitura confirmada em 2026-10-29: adotar o AtlantaFX 3.0.0 como tema base no lugar do Modena e remover o JFoenix; o JavaFX continua como toolkit, agora na versão 27. Ampliada pelo usuário com migração de ícones e redesenho das telas principais.)

## 2.3 Requirements

Origem: respostas do usuário em 2026-10-29, codekb, memória do projeto. Pré-requisito já entregue: Java 25, JavaFX 27.0.1, Kotlin 2.4.21, Spring Boot 4.1.2 em `main` (v2.19.0). [src: .aidlc/261029-java-25-upgrade]

### Functional
| ID | Requirement | Rationale | Acceptance criteria | Priority | Source |
|---|---|---|---|---|---|
| FR-1 | O app usa o **AtlantaFX 3.0.0** (`io.github.mkpaz:atlantafx-base`) como user-agent stylesheet no lugar do Modena. | Tema moderno e coberto de controles; a 3.0.0 exige Java 25 e JavaFX 27. | Given o app iniciado, When a janela principal abre, Then `Application.userAgentStylesheet` aponta para o tema escolhido e nenhum controle usa o visual Modena. | Must | [user] [src: repo1.maven.org] |
| FR-2 | Tema escuro = `Dracula` do AtlantaFX; tema claro = `PrimerLight`. A troca claro/escuro continua nas Configurações e é persistida como hoje (`PreferencesService`). | Decisão do usuário; mantém o comportamento atual de preferência. | Given o tema escolhido, When reinicio o app, Then o mesmo tema é aplicado. | Must | [user] |
| FR-3 | Os tokens `theme-*` (`tokens.light.css`, `tokens.dark.css`) passam a ser uma camada semântica sobre as variáveis `-color-*` do AtlantaFX; as variáveis Modena (`-fx-base`, `-fx-background`, `-fx-accent`...) deixam de ser a base. | O Dracula/Primer do AtlantaFX não usa `-fx-base`; sem o mapeamento os tokens perdem efeito. | Given qualquer tela, When inspeciono o CSS, Then cores de texto, fundo e destaque derivam de `-color-*` via tokens, sem depender de `-fx-base`. | Must | [src: codekb/quality-and-risks.md] |
| FR-4 | A identidade do MOINEX é preservada nos dois temas: roxo de destaque `#BD93F9`, botões em pílula (raio 20, borda 2px) e sidebar verde (`#0a614e`, hover `#106e5b`). | Identidade definida pelo usuário. | Given qualquer tela nos dois temas, When comparo com o design de referência, Then botões, sidebar e destaque seguem essas cores e formas. | Must | [user] [mem: M-001] |
| FR-5 | O **JFoenix é removido**: dependência, imports e uso de `JFXButton` (81 FXML, 14 arquivos Kotlin) viram `Button`; a regra `.jfx-button` vira `.button` mantendo o visual de FR-4. | Biblioteca sem manutenção usada só para um botão. | Given o projeto, When busco "jfoenix", Then não há resultados em código, FXML, CSS ou build; os botões mantêm o visual. | Must | [src: codekb/technology-stack.md] |
| FR-6 | Os overrides de CSS que só redesenham controles nativos (`combo-box`, `data-picker`, `table-view`, `progress-bar`, `menu-button`, `buttons`; ~400 linhas) são removidos ou reduzidos ao que for identidade ou semântica. | O AtlantaFX já estiliza esses controles. | Given o CSS, When comparo antes/depois, Then cada regra restante tem justificativa (identidade, semântica ou controle custom). | Should | [src: codekb/quality-and-risks.md] |
| FR-7 | A troca de tema atualiza **todas as janelas abertas** (principal, diálogos, janelas de popup), e os `Alert`/diálogos nativos (`WindowUtils.show*Dialog`) passam a usar o tema. | Hoje só o `rootPane` atualiza e os `Alert` ficam sem tema. | Given um diálogo aberto, When alterno o tema, Then o diálogo muda na hora; Given um `Alert`, When ele abre, Then usa o tema atual. | Must | [user] |
| FR-8 | Os **69 ícones de interface** (`icon/common` 48, `icon/sidebar` 12, `icon/wallet_type` 9) migram de PNG para ícones vetoriais Ikonli (`FontIcon`), com cor vinda do tema (sem `ColorAdjust`). Ficam em PNG: os 4 ícones do app, as 9 bandeiras de cartão e os 4.009 emojis do chat de IA. | Decisão do usuário; ícones nítidos e temáveis. | Given as telas e diálogos, When alterno o tema, Then os ícones de interface mudam de cor sem efeito de imagem; os PNGs listados continuam inalterados. | Must | [user] |
| FR-9 | **Redesenho de todas as telas principais** (Home, Resumo anual, Chat de IA, Carteiras, Cartões, Transações, Metas/Planos/FIRE, Wishlist, Poupança — visão geral, ações e fundos, bonds —, Configurações, Central de notificações) com **hierarquia visual e mais respiro**: cards, títulos e espaçamentos consistentes, usando os estilos do AtlantaFX. | Objetivo do usuário. | Given cada tela principal, When aprovo as capturas antes/depois nos dois temas (NFR-3), Then a tela segue o desenho aprovado em 2.5. | Must | [user] |
| FR-10 | **Formulários e diálogos melhores** nos diálogos de criação, edição e visualização (78 arquivos): validação inline, foco inicial no primeiro campo, Enter confirma, Esc cancela, mensagens de erro junto ao campo. | Objetivo do usuário. | Given um diálogo de formulário, When deixo um campo obrigatório vazio e confirmo, Then o erro aparece junto ao campo sem fechar a janela; Enter salva e Esc cancela. | Must | [user] |
| FR-11 | **Navegação e busca**: atalhos de teclado para as telas e para "nova transação", busca global e acesso rápido a nova transação a partir de qualquer tela. O desenho exato sai do 2.5. | Objetivo do usuário. | Given o app aberto, When uso o atalho ou a busca global, Then chego à tela ou ação em até 1 passo (detalhes em 2.4/2.5). | Must | [user] |
| FR-12 | O chat de IA (`WebView`) e os gráficos custom de `common/chart/` (`DoughnutChart`, `SankeyChart`, `CircularProgressBar`, `BudgetGroupTimelineChart`, `BudgetGroupGroupedBarChart`, `NetWorthLineChart`) usam as cores do tema novo e não regridem visualmente. | São componentes com CSS próprio. | Given os gráficos e o chat nos dois temas, When comparo antes/depois, Then cores, legendas e eixos acompanham o tema. | Must | [src: codekb/quality-and-risks.md] |

### Non-functional
| ID | Category | Measurable target | Verification | Source |
|---|---|---|---|---|
| NFR-1 | Regressão | Os 1374 testes atuais (ou mais) passam, 0 falhas, em JDK 25. | `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew ktlintCheck spotlessCheck test` | [src: quality-and-risks.md] |
| NFR-2 | Acessibilidade | Contraste de texto **WCAG AA**: 4,5:1 para texto normal e 3:1 para texto grande, nos dois temas, para os pares texto/fundo dos tokens. | Script que calcula o contraste dos pares de tokens e falha abaixo da meta. | [user] |
| NFR-3 | Aceitação visual | Capturas **antes/depois** das telas principais, nos dois temas, aprovadas pelo usuário; mais a checklist manual de telas e janelas. | Capturas geradas na Construction e anexadas ao `construction/`; aprovação no checkpoint. | [user] [mem: M-003] |
| NFR-4 | Consistência de estilo | Nenhuma cor hexadecimal fixa em CSS fora dos arquivos de tokens e dos gráficos justificados. | `grep` por `#[0-9a-fA-F]{3,8}` nos CSS, com lista de exceções aprovadas. | [mem: M-001] [src: conventions.md] |
| NFR-5 | Desempenho | A abertura da janela principal e a navegação entre telas não pioram mais de 20% em relação à medição feita antes da mudança. | Medição do tempo até a janela principal antes e depois (limite proposto, ver A5). | [inferred] |
| NFR-6 | Reversibilidade | Trabalho na branch `feat/atlantafx`; sem commit antes de o usuário ver o diff. | Histórico do git. | [mem: M-002] [mem: M-004] |
| NFR-7 | i18n | Textos novos entram em `messages_pt_BR.properties` e `messages_en.properties` só com acréscimos de chaves; nunca reescrever o arquivo inteiro. | Revisão do diff dos `.properties`. | [src: CLAUDE.md] |

### Constraints · Out of scope · Glossary
- **Constraints:** JDK 25, JavaFX 27.0.1, Spring Boot 4.1.2 (já em `main`); camadas `service`, `repository` e banco não mudam; sem alterações de schema; preferência de tema persistida como hoje; `AtlantaFX` 3.0.0 usa módulos `atlantafx.base` (e, se necessário, `atlantafx.decorations`/`spins`/`validation`, a decidir no 2.6).
- **Out of scope:** reduzir cliques nas tarefas frequentes (não escolhido), oferecer outros temas do AtlantaFX (Nord, Cupertino) nas Configurações, migração para Compose Multiplatform ou mobile, novas funcionalidades de negócio, trocar os PNGs do app, das bandeiras de cartão e dos emojis.
- **Glossary:** *user-agent stylesheet* = folha de estilo base do JavaFX (hoje Modena); *token* = variável de cor semântica `theme-*`; *FontIcon* = nó do Ikonli que desenha um glifo vetorial; *WCAG AA* = nível de contraste mínimo de acessibilidade web.

### Assumptions & open questions
- A1 (confirmada): "substituir JavaFX por AtlantaFX" = AtlantaFX 3.0.0 como tema + remover JFoenix. [user]
- A2 (confirmada): sidebar verde nos dois temas. [user]
- A3 — confirmed: no: o AtlantaFX 3.0.0 (Dracula e PrimerLight) funciona com JavaFX 27.0.1 e Java 25 no MOINEX sem ajustes além do mapeamento de tokens. Verificar na primeira Unit (spike).
- A4 — confirmed: no: um pacote do Ikonli cobre os 69 ícones de interface; os que não tiverem equivalente ficam em PNG e são listados. Pacote a escolher no 2.6.
- A5 — confirmed: no: o limite de +20% em NFR-5 é uma proposta minha; o usuário pode trocar ou removê-lo no gate.
- A6 — confirmed: no: "todas as telas principais" inclui Chat de IA, Configurações e Central de notificações (o usuário marcou "Todas").
- Aberta para 2.4/2.5: forma exata da busca global e dos atalhos (FR-11), e o desenho das telas (FR-9) saem dos mockups.

## 2.4 User stories
